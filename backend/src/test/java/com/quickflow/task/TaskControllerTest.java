package com.quickflow.task;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP level of /api/tasks: validation errors, not found, the lifecycle operations and the query parameters. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "qf.db.path=./target/test-data/task-controller-${random.uuid}")
class TaskControllerTest {

    private static final String VALID = """
            {"title":"Write report","priority":"HIGH","dueDate":"2026-10-20"}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createWithTitleOnlyReturns201WithDefaults() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Only a title\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.priority", is("MEDIUM")))
                .andExpect(jsonPath("$.archived", is(false)))
                .andExpect(jsonPath("$.overdue", is(false)));
    }

    @Test
    void blankTitleIsA400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/tasks")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")));
    }

    @Test
    void titleOf201CharactersIsA400ButTitleOf200IsAccepted() throws Exception {
        String tooLong = "a".repeat(201);
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")));

        String maxLength = "a".repeat(200);
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + maxLength + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void descriptionOf2001CharactersIsA400WithFieldError() throws Exception {
        String body = "{\"title\":\"Ok\",\"description\":\"" + "d".repeat(2001) + "\"}";

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("description")));
    }

    @Test
    void invalidStatusInBodyIsA400() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ok\",\"status\":\"LATER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void invalidFilterValueIsA400WithFieldError() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "LATER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("status")));
    }

    @Test
    void unknownTaskIsA404WithApiError() throws Exception {
        mockMvc.perform(get("/api/tasks/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Task 987654 not found")))
                .andExpect(jsonPath("$.path", is("/api/tasks/987654")));
    }

    @Test
    void updateReturnsTheNewValues() throws Exception {
        long id = createTask(VALID);

        mockMvc.perform(put("/api/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\",\"status\":\"IN_PROGRESS\",\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Renamed")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.priority", is("LOW")))
                .andExpect(jsonPath("$.dueDate", nullValue()));
    }

    @Test
    void completeArchiveRestoreAndDeleteFollowTheLifecycle() throws Exception {
        long id = createTask(VALID);

        mockMvc.perform(post("/api/tasks/" + id + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));

        mockMvc.perform(post("/api/tasks/" + id + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived", is(true)));

        mockMvc.perform(post("/api/tasks/" + id + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived", is(false)));

        mockMvc.perform(delete("/api/tasks/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchAndFilterParametersAreApplied() throws Exception {
        createTask("{\"title\":\"Uniquely Named Search Target\",\"priority\":\"HIGH\"}");

        mockMvc.perform(get("/api/tasks").param("q", "uniquely named"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", is("Uniquely Named Search Target")));

        mockMvc.perform(get("/api/tasks").param("priority", "HIGH").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void apiDocsListTaskOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/tasks")));
    }

    private long createTask(String body) throws Exception {
        String response = mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(response).read("$.id", Long.class);
    }
}
