package com.quickflow.hardening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.quickflow.common.GlobalExceptionHandler;
import com.quickflow.common.RequestLogFilter;
import com.quickflow.support.MutableClockTestConfig;
import com.quickflow.task.TaskCreate;
import com.quickflow.task.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Error shape and API hygiene: every error is an ApiError (no stack trace), unknown routes and malformed bodies are
 * 4xx, one INFO line per request, and a typical request answers in under 500 ms with 200 tasks (NFR-01).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/error-handling-${random.uuid}")
class ErrorHandlingTest {

    private static final long MAX_MILLIS = 500;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskService tasks;

    @Test
    void unknownRouteReturnsTheApiErrorBody() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.path", is("/api/does-not-exist")))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void wrongMethodReturnsTheApiErrorBody() throws Exception {
        mockMvc.perform(delete("/api/dashboard"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.path", is("/api/dashboard")));
    }

    @Test
    void malformedJsonIsA400WithTheApiErrorBody() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Malformed request body")))
                .andExpect(jsonPath("$.path", is("/api/tasks")));
    }

    @Test
    void unsupportedEnumInBodyNamesTheField() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Enum\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("priority")));
    }

    /** An exception nobody expected: 500, the ApiError shape, and no internal detail in the body. */
    @Test
    void unexpectedExceptionIs500WithoutStackTrace() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-15T09:00:00Z"), ZoneOffset.UTC);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(fixed);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");

        ResponseEntity<com.quickflow.common.ApiError> response =
                handler.handleUnexpected(new IllegalStateException("secret database detail"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        com.quickflow.common.ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("Unexpected error");
        assertThat(body.path()).isEqualTo("/api/tasks");
        assertThat(body.fieldErrors()).isEmpty();
        assertThat(body.toString()).doesNotContain("secret database detail").doesNotContain("IllegalStateException");
    }

    /** One INFO line per request with method, path and status; the query string is not logged. */
    @Test
    void oneInfoLinePerRequest() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLogFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            mockMvc.perform(get("/api/dashboard").param("secret", "hidden")).andExpect(status().isOk());
        } finally {
            logger.detachAppender(appender);
        }

        List<ILoggingEvent> lines = appender.list;
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getLevel()).isEqualTo(Level.INFO);
        String line = lines.get(0).getFormattedMessage();
        assertThat(line).startsWith("GET /api/dashboard 200 ");
        assertThat(line).doesNotContain("secret").doesNotContain("hidden");
    }

    /** NFR-01: with 200 seeded tasks a typical read answers in under 500 ms. */
    @Test
    void typicalRequestsAnswerUnder500MsWith200Tasks() throws Exception {
        for (int i = 0; i < 200; i++) {
            tasks.create(new TaskCreate("Seeded task " + i, "Seeded description " + i, null, null, null));
        }
        // the first request also warms up the JSON and JPA paths; it is not timed
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isOk());

        assertThat(timeMillis(get("/api/dashboard"))).isLessThan(MAX_MILLIS);
        assertThat(timeMillis(get("/api/tasks"))).isLessThan(MAX_MILLIS);
    }

    private long timeMillis(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        long start = System.nanoTime();
        mockMvc.perform(request).andExpect(status().isOk());
        return (System.nanoTime() - start) / 1_000_000;
    }
}
