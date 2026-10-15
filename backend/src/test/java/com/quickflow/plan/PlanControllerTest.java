package com.quickflow.plan;

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
import com.quickflow.support.MutableClockTestConfig;
import com.quickflow.task.TaskCreate;
import com.quickflow.task.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP level of /api/plans: status codes, the error body and the query parameter. Clock is fixed at 09:00 UTC. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/plan-controller-${random.uuid}")
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskService taskService;

    @Test
    void createReturns201NotStartedWithItems() throws Exception {
        long taskId = taskService.create(new TaskCreate("Write report", null, null, null, null)).id();

        mockMvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(taskId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("NOT_STARTED")))
                .andExpect(jsonPath("$.restSeconds", nullValue()))
                .andExpect(jsonPath("$.items[0].title", is("Write report")));
    }

    @Test
    void planWithoutItemsIsA400WithErrorBody() throws Exception {
        mockMvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/plans")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("items")));
    }

    @Test
    void endBeforeStartIsA400() throws Exception {
        mockMvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backwards","estimatedDurationMinutes":30,
                                 "startDateTime":"2026-10-15T10:00:00Z","endDateTime":"2026-10-15T09:00:00Z",
                                 "priorityOrder":1,"items":[{"sourceType":"TASK","sourceId":1}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("endDateTime")));
    }

    @Test
    void unknownPlanIsA404() throws Exception {
        mockMvc.perform(get("/api/plans/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
        mockMvc.perform(put("/api/plans/999999/items/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void acknowledgeStartAndDeleteOfAPlan() throws Exception {
        long taskId = taskService.create(new TaskCreate("Stand up", null, null, null, null)).id();
        String body = mockMvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(taskId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long planId = JsonPath.parse(body).read("$.id", Long.class);

        mockMvc.perform(post("/api/plans/" + planId + "/acknowledge-start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startNotified", is(true)));

        mockMvc.perform(delete("/api/plans/" + planId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/plans/" + planId))
                .andExpect(status().isNotFound());
    }

    @Test
    void groupFilterRejectsUnknownValue() throws Exception {
        mockMvc.perform(get("/api/plans").param("group", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    /** A plan from 2026-10-15 11:00 to 12:00 UTC (after the fixed clock); items null gives an empty items array. */
    private static String validBody(Long taskId) {
        String items = taskId == null ? "[]" : "[{\"sourceType\":\"TASK\",\"sourceId\":" + taskId + "}]";
        return """
                {"title":"Focused morning","estimatedDurationMinutes":60,
                 "startDateTime":"2026-10-15T11:00:00Z","endDateTime":"2026-10-15T12:00:00Z",
                 "priorityOrder":1,"items":%s}""".formatted(items);
    }
}
