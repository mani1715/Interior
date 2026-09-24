package com.interior.platform.leads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicWhatsAppHandoffRequest(
    @NotBlank(message = "Target studio slug is required")
    String targetStudioSlug,
    String targetProjectSlug,
    @Size(max = 100, message = "Visitor name must not exceed 100 characters")
    String visitorName,
    @Size(max = 25, message = "Visitor phone must not exceed 25 characters")
    String visitorPhone,
    @Size(max = 500, message = "Message must not exceed 500 characters")
    String customMessage
) {}
