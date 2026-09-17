package com.interior.platform.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    String requestId,
    Instant timestamp,
    List<FieldErrorDetail> fieldErrors
) {
    public record FieldErrorDetail(String field, String message) {}

    public static ApiErrorResponse of(String code, String message, String requestId) {
        return new ApiErrorResponse(code, message, requestId, Instant.now(), List.of());
    }

    public static ApiErrorResponse of(String code, String message, String requestId, List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(code, message, requestId, Instant.now(), fieldErrors);
    }
}
