package com.interior.platform.email.domain;

public record EmailSendResult(
        boolean success,
        String messageId,
        String status,
        String errorMessage
) {
    public static EmailSendResult success(String messageId) {
        return new EmailSendResult(true, messageId, "SENT", null);
    }

    public static EmailSendResult notConfigured(String reason) {
        return new EmailSendResult(false, null, "NOT_CONFIGURED", reason);
    }

    public static EmailSendResult failure(String errorMessage) {
        return new EmailSendResult(false, null, "FAILED", errorMessage);
    }
}
