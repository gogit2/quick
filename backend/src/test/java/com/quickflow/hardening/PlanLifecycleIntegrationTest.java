package com.quickflow.hardening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.quickflow.QuickFlowApplication;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Plan lifecycle across the clock and a restart. The application is started twice on the same H2 file: the plan's
 * computed status, rest time and item progress must be identical after the restart (NFR-02, FR-08, D-2, BR12).
 */
class PlanLifecycleIntegrationTest {

    @TempDir
    Path dir;

    @Test
    void notStartedThenInProgressThenCompletedSurvivesARestart() throws Exception {
        String db = dir.resolve("lifecycle").toString();

        ConfigurableApplicationContext first = start(db);
        MockMvc mvc = mvc(first);
        MutableClock clock = first.getBean(MutableClock.class);
        clock.set(Instant.parse("2026-10-15T09:00:00Z"));
        long taskA = createTask(mvc, "Lifecycle A");
        long taskB = createTask(mvc, "Lifecycle B");
        long plan = createPlan(mvc, "Lifecycle plan", "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z", taskA, taskB);

        mvc.perform(get("/api/plans/" + plan))
                .andExpect(jsonPath("$.status", is("NOT_STARTED")))
                .andExpect(jsonPath("$.restSeconds", nullValue()));

        // the start instant itself is already In Progress
        clock.set(Instant.parse("2026-10-15T10:00:00Z"));
        mvc.perform(get("/api/plans/" + plan))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.restSeconds", is(3600)));

        clock.set(Instant.parse("2026-10-15T10:30:00Z"));
        tickItem(mvc, plan, taskA, true);
        Map<String, Object> before = planState(mvc, plan);
        assertThat(before).containsEntry("status", "IN_PROGRESS").containsEntry("restSeconds", 1800)
                .containsEntry("completionPercent", 50);
        first.close();

        ConfigurableApplicationContext second = start(db);
        try {
            MockMvc restarted = mvc(second);
            second.getBean(MutableClock.class).set(Instant.parse("2026-10-15T10:30:00Z"));
            Map<String, Object> after = planState(restarted, plan);
            assertThat(after).isEqualTo(before);

            // FR-08: the item change is visible on the dashboard while the plan is In Progress
            restarted.perform(get("/api/dashboard"))
                    .andExpect(jsonPath("$.plans.items[?(@.id == " + plan + ")].completionPercent", is(java.util.List.of(50))));

            tickItem(restarted, plan, taskB, true);
            restarted.perform(get("/api/plans/" + plan))
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.completionPercent", is(100)))
                    .andExpect(jsonPath("$.restSeconds", nullValue()));
            restarted.perform(get("/api/dashboard"))
                    .andExpect(jsonPath("$.plans.items[?(@.id == " + plan + ")]", is(java.util.List.of())));
            restarted.perform(get("/api/tasks/" + taskB))
                    .andExpect(jsonPath("$.status", is("DONE")));
        } finally {
            second.close();
        }
    }

    @Test
    void planThatEndsWithOpenItemsIsCompletedAndKeepsItsPercentageAfterARestart() throws Exception {
        String db = dir.resolve("ended").toString();

        ConfigurableApplicationContext first = start(db);
        MockMvc mvc = mvc(first);
        MutableClock clock = first.getBean(MutableClock.class);
        clock.set(Instant.parse("2026-10-15T09:00:00Z"));
        long taskA = createTask(mvc, "Ended A");
        long taskB = createTask(mvc, "Ended B");
        long plan = createPlan(mvc, "Ended plan", "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z", taskA, taskB);
        clock.set(Instant.parse("2026-10-15T10:15:00Z"));
        tickItem(mvc, plan, taskA, true);

        clock.set(Instant.parse("2026-10-15T11:00:00Z"));
        Map<String, Object> before = planState(mvc, plan);
        assertThat(before).containsEntry("status", "COMPLETED").containsEntry("restSeconds", null)
                .containsEntry("completionPercent", 50);
        first.close();

        ConfigurableApplicationContext second = start(db);
        try {
            MockMvc restarted = mvc(second);
            second.getBean(MutableClock.class).set(Instant.parse("2026-10-15T11:30:00Z"));
            assertThat(planState(restarted, plan)).isEqualTo(before);
        } finally {
            second.close();
        }
    }

    private static ConfigurableApplicationContext start(String db) {
        return new SpringApplicationBuilder(QuickFlowApplication.class, MutableClockTestConfig.class)
                .web(WebApplicationType.SERVLET)
                .properties("qf.db.path=" + db, "server.port=0")
                .run();
    }

    private static MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
    }

    private static long createTask(MockMvc mvc, String title) throws Exception {
        String json = mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(json).read("$.id", Long.class);
    }

    private static long createPlan(MockMvc mvc, String title, String start, String end, long taskA, long taskB)
            throws Exception {
        String body = """
                {"title":"%s","estimatedDurationMinutes":60,"startDateTime":"%s","endDateTime":"%s",
                 "priorityOrder":1,"items":[{"sourceType":"TASK","sourceId":%d},{"sourceType":"TASK","sourceId":%d}]}"""
                .formatted(title, start, end, taskA, taskB);
        String json = mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(json).read("$.id", Long.class);
    }

    /** Ticks the plan item of a task (which also sets the task DONE or TODO, BR13). */
    private static void tickItem(MockMvc mvc, long plan, long taskId, boolean done) throws Exception {
        String json = mvc.perform(get("/api/plans/" + plan)).andReturn().getResponse().getContentAsString();
        java.util.List<Number> itemIds = JsonPath.parse(json)
                .read("$.items[?(@.sourceId == " + taskId + " && @.sourceType == 'TASK')].id");
        mvc.perform(put("/api/plans/" + plan + "/items/" + itemIds.get(0).longValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"done\":" + done + "}"))
                .andExpect(status().isOk());
    }

    /** The figures that must not change across a restart. */
    private static Map<String, Object> planState(MockMvc mvc, long plan) throws Exception {
        String json = mvc.perform(get("/api/plans/" + plan)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // a HashMap, because restSeconds is null once the plan is not In Progress
        Map<String, Object> state = new HashMap<>();
        state.put("status", JsonPath.parse(json).read("$.status", String.class));
        state.put("restSeconds", JsonPath.parse(json).read("$.restSeconds"));
        state.put("completionPercent", JsonPath.parse(json).read("$.completionPercent", Integer.class));
        return state;
    }
}
