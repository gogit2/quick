package com.quickflow.hardening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * One test per business rule of PRD section 7 (BR1-BR14), through the HTTP layer. Each asserts the status and the
 * ApiError body. The clock starts at 2026-10-15 09:00 UTC; tests that depend on time set it themselves.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/business-rules-sweep-${random.uuid}")
class BusinessRulesSweepTest {

    private static final String PAST_DAY = "2026-10-10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    /** BR1: a task title is 1 to 200 characters. */
    @Test
    void br1TaskTitleLimits() throws Exception {
        postJson("/api/tasks", "{\"title\":\"" + "t".repeat(200) + "\"}")
                .andExpect(status().isCreated());
        postJson("/api/tasks", "{\"title\":\"" + "t".repeat(201) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")));
        postJson("/api/tasks", "{\"title\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")));
    }

    /** BR2: a task description is at most 2000 characters. */
    @Test
    void br2TaskDescriptionLimits() throws Exception {
        postJson("/api/tasks", "{\"title\":\"Long notes\",\"description\":\"" + "d".repeat(2000) + "\"}")
                .andExpect(status().isCreated());
        postJson("/api/tasks", "{\"title\":\"Longer notes\",\"description\":\"" + "d".repeat(2001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("description")));
    }

    /** BR3: enum values outside the allowed set are rejected with the field name, in a body and in a query. */
    @Test
    void br3UnsupportedEnumValuesAreRejected() throws Exception {
        postJson("/api/tasks", "{\"title\":\"Enum\",\"priority\":\"URGENT\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("priority")));
        postJson("/api/tasks", "{\"title\":\"Enum\",\"status\":\"LATER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("status")));
        mockMvc.perform(get("/api/tasks").param("priority", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("priority")));
    }

    /** BR4: archived tasks are listed only with archived=true and never in the default list. */
    @Test
    void br4ArchivedTasksAreExcludedFromTheDefaultList() throws Exception {
        long id = create("/api/tasks", "{\"title\":\"Archive me BR4\"}");
        mockMvc.perform(post("/api/tasks/" + id + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived", is(true)));

        assertThat(ids(json(get("/api/tasks")))).doesNotContain(id);
        assertThat(ids(json(get("/api/tasks").param("archived", "true")))).contains(id);
    }

    /** BR5: completing a task sets its status to DONE. */
    @Test
    void br5CompleteSetsStatusDone() throws Exception {
        long id = create("/api/tasks", "{\"title\":\"Finish BR5\"}");
        mockMvc.perform(post("/api/tasks/" + id + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    /** BR6: a habit name is 1 to 150 characters. */
    @Test
    void br6HabitNameLimits() throws Exception {
        postJson("/api/habits", "{\"name\":\"" + "h".repeat(150) + "\",\"frequency\":\"DAILY\"}")
                .andExpect(status().isCreated());
        postJson("/api/habits", "{\"name\":\"" + "h".repeat(151) + "\",\"frequency\":\"DAILY\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
    }

    /** BR7: one completion per habit and date; the second one is a 409 with the ApiError body. */
    @Test
    void br7DuplicateHabitCompletionIsA409() throws Exception {
        long id = create("/api/habits", "{\"name\":\"Stretch BR7\",\"frequency\":\"DAILY\"}");
        String completion = "{\"completionDate\":\"" + PAST_DAY + "\"}";
        postJson("/api/habits/" + id + "/completions", completion)
                .andExpect(status().isCreated());
        postJson("/api/habits/" + id + "/completions", completion)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.path", is("/api/habits/" + id + "/completions")));
    }

    /** BR8: a learning card title is 1 to 200 characters. */
    @Test
    void br8LearningCardTitleLimits() throws Exception {
        postJson("/api/learning-cards", "{\"title\":\"" + "c".repeat(200) + "\"}")
                .andExpect(status().isCreated());
        postJson("/api/learning-cards", "{\"title\":\"" + "c".repeat(201) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")));
        postJson("/api/learning-cards", "{\"title\":\"\"}")
                .andExpect(status().isBadRequest());
    }

    /** BR9: a milestone belongs to one card; using it through another card is a 404. */
    @Test
    void br9MilestoneMustBelongToTheCard() throws Exception {
        long cardA = create("/api/learning-cards", "{\"title\":\"Card A BR9\"}");
        long cardB = create("/api/learning-cards", "{\"title\":\"Card B BR9\"}");
        long milestoneOfA = create("/api/learning-cards/" + cardA + "/milestones", "{\"title\":\"Step A\"}");

        mockMvc.perform(put("/api/learning-cards/" + cardB + "/milestones/" + milestoneOfA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Step A\",\"done\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
        mockMvc.perform(delete("/api/learning-cards/" + cardB + "/milestones/" + milestoneOfA))
                .andExpect(status().isNotFound());
    }

    /** BR10: a plan needs at least one item. */
    @Test
    void br10PlanWithoutItemsIsA400() throws Exception {
        postJson("/api/plans", planBody("Empty BR10", "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z", "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("items")));
    }

    /** BR11: the end must be after the start. Equal times are rejected too. */
    @Test
    void br11PlanEndMustBeAfterStart() throws Exception {
        long task = create("/api/tasks", "{\"title\":\"Window BR11\"}");
        String items = "[{\"sourceType\":\"TASK\",\"sourceId\":" + task + "}]";
        postJson("/api/plans", planBody("Backwards BR11", "2026-10-15T11:00:00Z", "2026-10-15T10:00:00Z", items))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("endDateTime")));
        postJson("/api/plans", planBody("Same BR11", "2026-10-15T11:00:00Z", "2026-10-15T11:00:00Z", items))
                .andExpect(status().isBadRequest());
    }

    /** BR12: rest time is shown only while the plan is In Progress, and counts down to the end. */
    @Test
    void br12RestTimeOnlyInsideTheWindow() throws Exception {
        long task = create("/api/tasks", "{\"title\":\"Window BR12\"}");
        String items = "[{\"sourceType\":\"TASK\",\"sourceId\":" + task + "}]";
        long plan = create("/api/plans", planBody("Rest BR12", "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z", items));

        clock.set(Instant.parse("2026-10-15T09:00:00Z"));
        mockMvc.perform(get("/api/plans/" + plan))
                .andExpect(jsonPath("$.status", is("NOT_STARTED")))
                .andExpect(jsonPath("$.restSeconds", nullValue()));

        clock.set(Instant.parse("2026-10-15T10:30:00Z"));
        mockMvc.perform(get("/api/plans/" + plan))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.restSeconds", is(1800)));

        clock.set(Instant.parse("2026-10-15T11:00:00Z"));
        mockMvc.perform(get("/api/plans/" + plan))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.restSeconds", nullValue()));
    }

    /** BR13: ticking a task item sets the task DONE; ticking a habit item changes only the plan item. */
    @Test
    void br13TickSideEffectsOnlyForTaskItems() throws Exception {
        clock.set(Instant.parse("2026-10-15T09:00:00Z"));
        long task = create("/api/tasks", "{\"title\":\"Side effect BR13\"}");
        long habit = create("/api/habits", "{\"name\":\"Side effect habit BR13\",\"frequency\":\"DAILY\"}");
        String items = "[{\"sourceType\":\"TASK\",\"sourceId\":" + task + "},"
                + "{\"sourceType\":\"HABIT\",\"sourceId\":" + habit + "}]";
        long plan = create("/api/plans", planBody("Ticks BR13", "2026-10-15T08:00:00Z", "2026-10-15T12:00:00Z", items));
        long taskItem = itemId(plan, "TASK");
        long habitItem = itemId(plan, "HABIT");

        putJson("/api/plans/" + plan + "/items/" + taskItem, "{\"done\":true}")
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/" + task))
                .andExpect(jsonPath("$.status", is("DONE")));

        putJson("/api/plans/" + plan + "/items/" + habitItem, "{\"done\":true}")
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/habits/" + habit))
                .andExpect(jsonPath("$.doneForCurrentPeriod", is(false)))
                .andExpect(jsonPath("$.totalCompletions", is(0)));

        putJson("/api/plans/" + plan + "/items/" + taskItem, "{\"done\":false}")
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/" + task))
                .andExpect(jsonPath("$.status", is("TODO")));
    }

    /** BR14: a deleted task, habit or card is never returned again, and its plan items are removed. */
    @Test
    void br14DeletedSourcesAreNeverReturned() throws Exception {
        clock.set(Instant.parse("2026-10-15T09:00:00Z"));
        long task = create("/api/tasks", "{\"title\":\"Gone BR14\"}");
        long habit = create("/api/habits", "{\"name\":\"Gone habit BR14\",\"frequency\":\"DAILY\"}");
        long card = create("/api/learning-cards", "{\"title\":\"Gone card BR14\"}");
        String items = "[{\"sourceType\":\"TASK\",\"sourceId\":" + task + "},"
                + "{\"sourceType\":\"HABIT\",\"sourceId\":" + habit + "},"
                + "{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":" + card + "}]";
        long plan = create("/api/plans", planBody("Sources BR14", "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z", items));

        mockMvc.perform(delete("/api/tasks/" + task)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/habits/" + habit)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/learning-cards/" + card)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + task)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/habits/" + habit)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/learning-cards/" + card)).andExpect(status().isNotFound());
        assertThat(ids(json(get("/api/tasks")))).doesNotContain(task);
        assertThat(ids(json(get("/api/habits")))).doesNotContain(habit);
        assertThat(ids(json(get("/api/learning-cards")))).doesNotContain(card);
        // D-5: the plan is deleted when its last item goes
        mockMvc.perform(get("/api/plans/" + plan)).andExpect(status().isNotFound());
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putJson(String path, String json) throws Exception {
        return mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** POST that must return 201 and gives back the new id. */
    private long create(String path, String json) throws Exception {
        return JsonPath.parse(body(postJson(path, json).andExpect(status().isCreated()))).read("$.id", Long.class);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    /** Performs a GET (or any request) and returns the response body. */
    private String json(MockHttpServletRequestBuilder request) throws Exception {
        return body(mockMvc.perform(request));
    }

    private long itemId(long planId, String sourceType) throws Exception {
        String json = json(get("/api/plans/" + planId));
        List<Number> found = JsonPath.parse(json).read("$.items[?(@.sourceType == '" + sourceType + "')].id");
        return found.get(0).longValue();
    }

    private static List<Long> ids(String json) {
        List<Number> found = JsonPath.parse(json).read("$[*].id");
        return found.stream().map(Number::longValue).toList();
    }

    private static String planBody(String title, String start, String end, String items) {
        return """
                {"title":"%s","estimatedDurationMinutes":60,"startDateTime":"%s","endDateTime":"%s",
                 "priorityOrder":1,"items":%s}""".formatted(title, start, end, items);
    }
}
