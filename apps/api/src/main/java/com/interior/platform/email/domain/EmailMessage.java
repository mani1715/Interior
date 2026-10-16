package com.interior.platform.email.domain;

import java.util.Map;

public record EmailMessage(
        String recipientEmail,
        String subject,
        String textContent,
        String htmlContent,
        String fromAddress,
        String replyTo,
        EmailTemplateType templateType,
        Map<String, String> metadata
) {
    public EmailMessage(String recipientEmail, String subject, String textContent, String htmlContent, Map<String, String> metadata) {
        this(recipientEmail, subject, textContent, htmlContent, null, null, null, metadata != null ? metadata : Map.of());
    }

    public EmailMessage(String recipientEmail, String subject, String textContent) {
        this(recipientEmail, subject, textContent, null, null, null, null, Map.of());
    }

    public EmailMessage(String recipientEmail, String subject, String textContent, String htmlContent, EmailTemplateType templateType, Map<String, String> metadata) {
        this(recipientEmail, subject, textContent, htmlContent, null, null, templateType, metadata != null ? metadata : Map.of());
    }
}
