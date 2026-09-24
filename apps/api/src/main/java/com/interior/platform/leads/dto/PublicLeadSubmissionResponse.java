package com.interior.platform.leads.dto;

public record PublicLeadSubmissionResponse(
    String referenceNumber,
    String message,
    String studioName
) {}
