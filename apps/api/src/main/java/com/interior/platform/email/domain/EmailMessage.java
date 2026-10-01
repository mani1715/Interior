package com.interior.platform.email.domain;

import java.util.Map;

public record EmailMessage(
        String recipientEmail,
        String subject,
        String textContent,
        String htmlContent,
        Map<String, String> metadata
) {
    public EmailMessage(String recipientEmail, String subject, String textContent) {
        this(recipientEmail, subject, textContent, null, Map.of());
    }
}
