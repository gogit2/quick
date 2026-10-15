package com.quickflow.support;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the production Clock bean in tests. Import it with @Import(MutableClockTestConfig.class)
 * and autowire MutableClock to move time.
 */
@TestConfiguration
public class MutableClockTestConfig {

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-10-15T09:00:00Z"));
    }
}
