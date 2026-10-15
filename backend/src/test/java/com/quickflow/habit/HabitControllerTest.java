package com.quickflow.habit;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP level of /api/habits: validation, not found, completions and the rules that answer 400 or 409. Today is 2026-10-15. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/habit-controller-${random.uuid}")
class HabitControllerTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HabitRepository habits;

    @Autowired
    private HabitCompletionRepository completions;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetState() {
        completions.deleteAll();
        habits.deleteAll();
        clock.set(START);
    }

    @Test
    void createDailyHabitReturns201WithDerivedFields() throws Exception {
        mockMvc.perform(post("/api/habits").contentType(JSON)
                        .content("{\"name\":\"Morning run\",\"frequency\":\"DAILY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.doneForCurrentPeriod", is(false)))
                .andExpect(jsonPath("$.currentStreak", is(0)))
                .andExpect(jsonPath("$.totalCompletions", is(0)))
                .andExpect(jsonPath("$.periodStart", is("2026-10-15")))
                .andExpect(jsonPath("$.periodEnd", is("2026-10-15")));
    }

    @Test
    void createWeeklyHabitHasTheMondayToSundayPeriod() throws Exception {
        mockMvc.perform(post("/api/habits").contentType(JSON)
                        .content("{\"name\":\"Weekly review\",\"frequency\":\"WEEKLY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frequency", is("WEEKLY")))
                .andExpect(jsonPath("$.periodStart", is("2026-10-12")))
                .andExpect(jsonPath("$.periodEnd", is("2026-10-18")));
    }

    @Test
    void blankNameIsA400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/habits").contentType(JSON)
                        .content("{\"name\":\"   \",\"frequency\":\"DAILY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/habits")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
    }

    @Test
    void nameOf151CharactersIsA400ButNameOf150IsAccepted() throws Exception {
        mockMvc.perform(post("/api/habits").contentType(JSON)
                        .content("{\"name\":\"" + "a".repeat(151) + "\",\"frequency\":\"DAILY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
        mockMvc.perform(post("/api/habits").contentType(JSON)
                        .content("{\"name\":\"" + "a".repeat(150) + "\",\"frequency\":\"DAILY\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void missingFrequencyIsA400() throws Exception {
        mockMvc.perform(post("/api/habits").contentType(JSON).content("{\"name\":\"No frequency\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("frequency")));
    }

    @Test
    void unknownHabitIs404WithApiError() throws Exception {
        mockMvc.perform(get("/api/habits/987654321"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Habit 987654321 not found")));
    }

    @Test
    void completingTheSameDateTwiceIs409() throws Exception {
        long id = createHabit("Read", "DAILY");
        mockMvc.perform(post("/api/habits/" + id + "/completions").contentType(JSON)
                        .content("{\"completionDate\":\"2026-10-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completionDate", is("2026-10-15")));

        mockMvc.perform(post("/api/habits/" + id + "/completions").contentType(JSON)
                        .content("{\"completionDate\":\"2026-10-15\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Habit already completed for 2026-10-15")));
    }

    @Test
    void completionWithoutBodyIsForToday() throws Exception {
        long id = createHabit("Read", "DAILY");

        mockMvc.perform(post("/api/habits/" + id + "/completions"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completionDate", is("2026-10-15")));
    }

    @Test
    void futureDateIs400() throws Exception {
        long id = createHabit("Read", "DAILY");

        mockMvc.perform(post("/api/habits/" + id + "/completions").contentType(JSON)
                        .content("{\"completionDate\":\"2026-10-16\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("completionDate")));
    }

    @Test
    void secondWeeklyCompletionInTheSameWeekIs409() throws Exception {
        long id = createHabit("Review", "WEEKLY");
        mockMvc.perform(post("/api/habits/" + id + "/completions").contentType(JSON)
                        .content("{\"completionDate\":\"2026-10-13\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/habits/" + id + "/completions").contentType(JSON)
                        .content("{\"completionDate\":\"2026-10-15\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void completingADeactivatedHabitIs400() throws Exception {
        long id = createHabit("Read", "DAILY");
        mockMvc.perform(put("/api/habits/" + id).contentType(JSON)
                        .content("{\"name\":\"Read\",\"frequency\":\"DAILY\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        mockMvc.perform(post("/api/habits/" + id + "/completions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Habit is inactive")));
    }

    @Test
    void undoReturns204ThenNotFound() throws Exception {
        long id = createHabit("Read", "DAILY");
        mockMvc.perform(post("/api/habits/" + id + "/completions"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/habits/" + id + "/completions/2026-10-15"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/habits/" + id + "/completions/2026-10-15"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidDateInThePathIs400() throws Exception {
        long id = createHabit("Read", "DAILY");

        mockMvc.perform(delete("/api/habits/" + id + "/completions/not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCompletionsIsNewestFirstWithRange() throws Exception {
        long id = createHabit("Read", "DAILY");
        for (String date : new String[] {"2026-10-10", "2026-10-13", "2026-10-15"}) {
            mockMvc.perform(post("/api/habits/" + id + "/completions").contentType(JSON)
                            .content("{\"completionDate\":\"" + date + "\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/habits/" + id + "/completions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completionDate", is("2026-10-15")))
                .andExpect(jsonPath("$[2].completionDate", is("2026-10-10")));
        mockMvc.perform(get("/api/habits/" + id + "/completions?from=2026-10-11&to=2026-10-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].completionDate", is("2026-10-13")));
    }

    @Test
    void listFiltersByActive() throws Exception {
        createHabit("Active", "DAILY");
        long paused = createHabit("Paused", "DAILY");
        mockMvc.perform(put("/api/habits/" + paused).contentType(JSON)
                        .content("{\"name\":\"Paused\",\"frequency\":\"DAILY\",\"active\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/habits?active=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].name", is("Paused")));
    }

    @Test
    void deleteReturns204ThenNotFound() throws Exception {
        long id = createHabit("Gone", "DAILY");
        mockMvc.perform(post("/api/habits/" + id + "/completions"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/habits/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/habits/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/habits/" + id + "/completions")).andExpect(status().isNotFound());
    }

    private long createHabit(String name, String frequency) throws Exception {
        String body = mockMvc.perform(post("/api/habits").contentType(JSON)
                        .content("{\"name\":\"" + name + "\",\"frequency\":\"" + frequency + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }
}
