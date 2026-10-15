package com.quickflow.common;

import java.time.Instant;
import java.util.List;

/** Error body of every 4xx/5xx response (contract schema ApiError). */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {
}
