package com.interior.platform.email.domain;

public record EmailSendResult(
        boolean success,
        String messageId,
        String status,
        String errorCode,
        String errorMessage,
        boolean retryable
) {
    public static EmailSendResult success(String messageId) {
        return new EmailSendResult(true, messageId, "SENT", null, null, false);
    }

    public static EmailSendResult notConfigured(String reason) {
        return new EmailSendResult(false, null, "NOT_CONFIGURED", "NOT_CONFIGURED", reason, false);
    }

    public static EmailSendResult permanentFailure(String errorCode, String errorMessage) {
        return new EmailSendResult(false, null, "FAILED", errorCode, errorMessage, false);
    }

    public static EmailSendResult transientFailure(String errorCode, String errorMessage) {
        return new EmailSendResult(false, null, "FAILED", errorCode, errorMessage, true);
    }

    public static EmailSendResult failure(String errorMessage) {
        return new EmailSendResult(false, null, "FAILED", "DELIVERY_ERROR", errorMessage, false);
    }
}
