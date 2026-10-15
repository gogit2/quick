package com.quickflow.learning;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/learning-controller-${random.uuid}")
class LearningControllerTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearningCardRepository cards;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetState() {
        cards.deleteAll();
        clock.set(START);
    }

    @Test
    void createCardReturns201NotStartedWithEmptyChildren() throws Exception {
        mockMvc.perform(post("/api/learning-cards").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Kotlin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("NOT_STARTED")))
                .andExpect(jsonPath("$.milestones", hasSize(0)))
                .andExpect(jsonPath("$.notes", hasSize(0)));
    }

    @Test
    void blankCardTitleIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/learning-cards").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCardIsNotFound() throws Exception {
        mockMvc.perform(get("/api/learning-cards/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankMilestoneTitleIsRejectedWith400() throws Exception {
        long cardId = createCard();
        mockMvc.perform(post("/api/learning-cards/" + cardId + "/milestones").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completingAllMilestonesMakesTheCardCompleted() throws Exception {
        long cardId = createCard();
        long first = addMilestone(cardId, "Basics");
        long second = addMilestone(cardId, "Coroutines");

        mockMvc.perform(put("/api/learning-cards/" + cardId + "/milestones/" + first).contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Basics\",\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt", is("2026-10-15T09:00:00Z")));
        mockMvc.perform(get("/api/learning-cards/" + cardId))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        mockMvc.perform(put("/api/learning-cards/" + cardId + "/milestones/" + second).contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Coroutines\",\"done\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/learning-cards/" + cardId))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void statusInPutWhileMilestonesExistIs409() throws Exception {
        long cardId = createCard();
        addMilestone(cardId, "Basics");

        mockMvc.perform(put("/api/learning-cards/" + cardId).contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Kotlin\",\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void statusInPutWithoutMilestonesIsAccepted() throws Exception {
        long cardId = createCard();

        mockMvc.perform(put("/api/learning-cards/" + cardId).contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Kotlin\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    void milestoneOfAnotherCardIsNotFound() throws Exception {
        long cardId = createCard();
        long otherCardId = createCard();
        long milestoneId = addMilestone(otherCardId, "Ownership");

        mockMvc.perform(put("/api/learning-cards/" + cardId + "/milestones/" + milestoneId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Ownership\",\"done\":true}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/learning-cards/" + cardId + "/milestones/" + milestoneId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAndDeleteNote() throws Exception {
        long cardId = createCard();

        String body = mockMvc.perform(post("/api/learning-cards/" + cardId + "/notes")
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Read chapter 3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text", is("Read chapter 3")))
                .andExpect(jsonPath("$.createdAt", is("2026-10-15T09:00:00Z")))
                .andReturn().getResponse().getContentAsString();
        long noteId = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(delete("/api/learning-cards/" + cardId + "/notes/" + noteId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/learning-cards/" + cardId))
                .andExpect(jsonPath("$.notes", hasSize(0)));
    }

    @Test
    void deleteCardReturns204ThenNotFoundAndRemovesChildren() throws Exception {
        long cardId = createCard();
        addMilestone(cardId, "Basics");

        mockMvc.perform(delete("/api/learning-cards/" + cardId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/learning-cards/" + cardId))
                .andExpect(status().isNotFound());
    }

    private long createCard() throws Exception {
        String body = mockMvc.perform(post("/api/learning-cards").contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Kotlin\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long addMilestone(long cardId, String title) throws Exception {
        String body = mockMvc.perform(post("/api/learning-cards/" + cardId + "/milestones")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
