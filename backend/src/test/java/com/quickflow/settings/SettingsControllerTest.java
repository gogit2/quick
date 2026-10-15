package com.quickflow.settings;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP level of /api/settings. Each test gets a fresh database file under target/ (never ./data/dev),
 * so the tests do not depend on each other's saves.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "qf.db.path=./target/test-data/settings-${random.uuid}")
class SettingsControllerTest {

    private static final String VALID = """
            {"displayName":"Alex","email":"alex@example.com","timeZone":"Europe/Berlin",
             "notificationsEnabled":false,"defaultStartPage":"TASKS"}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getReturnsDefaultsOnFreshDatabase() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("QuickFlow User")))
                .andExpect(jsonPath("$.timeZone", is("UTC")))
                .andExpect(jsonPath("$.notificationsEnabled", is(true)))
                .andExpect(jsonPath("$.defaultStartPage", is("DASHBOARD")))
                .andExpect(jsonPath("$.configured", is(false)));
    }

    @Test
    void putSavesAndGetShowsTheValuesAsConfigured() throws Exception {
        mockMvc.perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured", is(true)));

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("Alex")))
                .andExpect(jsonPath("$.email", is("alex@example.com")))
                .andExpect(jsonPath("$.timeZone", is("Europe/Berlin")))
                .andExpect(jsonPath("$.notificationsEnabled", is(false)))
                .andExpect(jsonPath("$.defaultStartPage", is("TASKS")))
                .andExpect(jsonPath("$.configured", is(true)));
    }

    @Test
    void invalidTimeZoneIsA400WithFieldError() throws Exception {
        String body = VALID.replace("Europe/Berlin", "Mars/Olympus");

        mockMvc.perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/settings")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("timeZone")));
    }

    @Test
    void blankDisplayNameIsA400WithFieldError() throws Exception {
        String body = VALID.replace("\"Alex\"", "\"  \"");

        mockMvc.perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("displayName")));
    }

    @Test
    void invalidEmailIsA400WithFieldError() throws Exception {
        String body = VALID.replace("alex@example.com", "not-an-email");

        mockMvc.perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
    }

    @Test
    void apiDocsListSettingsOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/settings")));
    }
}
