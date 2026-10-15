package com.quickflow.common;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T09:00:00Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(NOW, ZoneOffset.UTC);
        ObjectMapper bootLikeMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler(fixed))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(bootLikeMapper))
                .build();
    }

    @Test
    void validationErrorIsA400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.path", is("/test/items")))
                .andExpect(jsonPath("$.timestamp", is("2026-10-15T09:00:00Z")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("title")));
    }

    @Test
    void notFoundIsA404WithErrorBody() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Item 7 not found")))
                .andExpect(jsonPath("$.path", is("/test/missing")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void conflictIsA409WithErrorBody() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Already done")))
                .andExpect(jsonPath("$.path", is("/test/conflict")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void badRequestCarriesFieldErrors() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Time zone is unknown")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("timeZone")));
    }

    @RestController
    static class SampleController {

        @PostMapping("/test/items")
        void create(@Valid @RequestBody SampleBody body) {
        }

        @GetMapping("/test/missing")
        void missing() {
            throw new NotFoundException("Item 7 not found");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Already done");
        }

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new BadRequestException("Time zone is unknown",
                    java.util.List.of(new FieldError("timeZone", "unknown zone")));
        }
    }

    record SampleBody(@NotBlank String title) {
    }
}
