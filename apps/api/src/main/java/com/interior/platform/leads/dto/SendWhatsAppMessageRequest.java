package com.interior.platform.leads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendWhatsAppMessageRequest(
    @NotBlank(message = "Message body is required")
    @Size(max = 4000, message = "Message body must not exceed 4000 characters")
    String body,
    String idempotencyKey
) {}
