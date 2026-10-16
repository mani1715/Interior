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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.interior.platform.common.metrics.PlatformMetrics platformMetrics;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        List<ApiErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiErrorResponse.FieldErrorDetail(err.getField(), err.getDefaultMessage()))
                .toList();
        recordMetric("VALIDATION_FAILED");
        ApiErrorResponse body = ApiErrorResponse.of("VALIDATION_FAILED", "Invalid request parameters", requestId, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(com.interior.platform.security.mfa.MfaStepUpRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaStepUpRequired(com.interior.platform.security.mfa.MfaStepUpRequiredException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("MFA_STEP_UP_REQUIRED");
        ApiErrorResponse body = ApiErrorResponse.of("MFA_STEP_UP_REQUIRED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(com.interior.platform.security.mfa.MfaRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaRequired(com.interior.platform.security.mfa.MfaRequiredException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("MFA_REQUIRED");
        ApiErrorResponse body = ApiErrorResponse.of("MFA_REQUIRED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(com.interior.platform.security.mfa.MfaEnrollmentRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaEnrollmentRequired(com.interior.platform.security.mfa.MfaEnrollmentRequiredException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("MFA_ENROLLMENT_REQUIRED");
        ApiErrorResponse body = ApiErrorResponse.of("MFA_ENROLLMENT_REQUIRED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(com.interior.platform.security.mfa.MfaLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaLocked(com.interior.platform.security.mfa.MfaLockedException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("MFA_LOCKED_TEMPORARILY");
        ApiErrorResponse body = ApiErrorResponse.of("MFA_LOCKED_TEMPORARILY", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("ACCESS_DENIED");
        ApiErrorResponse body = ApiErrorResponse.of("ACCESS_DENIED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("NOT_FOUND");
        ApiErrorResponse body = ApiErrorResponse.of("NOT_FOUND", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("UNAUTHORIZED");
        ApiErrorResponse body = ApiErrorResponse.of("UNAUTHORIZED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("TOO_MANY_REQUESTS");
        ApiErrorResponse body = ApiErrorResponse.of("TOO_MANY_REQUESTS", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("CONFLICT");
        ApiErrorResponse body = ApiErrorResponse.of("CONFLICT", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("BAD_REQUEST");
        ApiErrorResponse body = ApiErrorResponse.of("BAD_REQUEST", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AiProviderNotConfiguredException.class)
    public ResponseEntity<ApiErrorResponse> handleAiProviderNotConfigured(AiProviderNotConfiguredException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("PROVIDER_NOT_CONFIGURED");
        ApiErrorResponse body = ApiErrorResponse.of("PROVIDER_NOT_CONFIGURED", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("INVALID_STATE");
        ApiErrorResponse body = ApiErrorResponse.of("INVALID_STATE", ex.getMessage(), requestId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        recordMetric("INTERNAL_ERROR");
        log.error("Unhandled exception [requestId={}]: {}", requestId, ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.of("INTERNAL_ERROR", "An unexpected internal error occurred", requestId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private void recordMetric(String code) {
        if (platformMetrics != null) {
            try {
                platformMetrics.recordError(code);
            } catch (Exception ignored) {
            }
        }
    }

    private String getRequestId(HttpServletRequest request) {
        Object reqId = request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE);
        return reqId != null ? reqId.toString() : "unknown";
    }
}
