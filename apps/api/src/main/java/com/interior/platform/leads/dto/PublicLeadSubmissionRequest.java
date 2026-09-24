package com.interior.platform.leads.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicLeadSubmissionRequest(
    String targetStudioSlug,
    String targetProjectSlug,
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,
    @NotBlank(message = "Phone number is required")
    @Size(max = 25, message = "Phone number must not exceed 25 characters")
    String phone,
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,
    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,
    @Size(max = 50, message = "Category must not exceed 50 characters")
    String projectCategory,
    @Size(max = 50, message = "Budget range must not exceed 50 characters")
    String budgetRange,
    @NotBlank(message = "Requirement message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    String message,
    String preferredContactChannel,
    @AssertTrue(message = "Contact consent is required")
    boolean contactConsent,
    boolean whatsappConsent,
    String idempotencyKey,
    String website_hp // Honeypot
) {}
