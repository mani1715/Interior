package com.interior.platform.common.exception;

import com.interior.platform.common.dto.ApiErrorResponse;
import com.interior.platform.common.filter.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        List<ApiErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiErrorResponse.FieldErrorDetail(err.getField(), err.getDefaultMessage()))
                .toList();
        ApiErrorResponse body = ApiErrorResponse.of("VALIDATION_FAILED", "Invalid request parameters", requestId, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("ACCESS_DENIED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("NOT_FOUND", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("UNAUTHORIZED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("TOO_MANY_REQUESTS", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("CONFLICT", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("BAD_REQUEST", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AiProviderNotConfiguredException.class)
    public ResponseEntity<ApiErrorResponse> handleAiProviderNotConfigured(AiProviderNotConfiguredException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("PROVIDER_NOT_CONFIGURED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ApiErrorResponse body = ApiErrorResponse.of("INVALID_STATE", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        log.error("Unhandled exception [requestId={}]: {}", requestId, ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.of("INTERNAL_ERROR", "An unexpected internal error occurred", requestId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getRequestId(HttpServletRequest request) {
        Object reqId = request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE);
        return reqId != null ? reqId.toString() : "unknown";
    }
}
