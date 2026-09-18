package com.interior.platform.workspace.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkspaceBusinessProfileResponse(
        UUID id,
        String name,
        String slug,
        String professionalType,
        String professionalTitle,
        String tagline,
        Integer experienceSinceYear,
        String teamSize,
        String budgetRange,
        String addressLine,
        String city,
        String district,
        String state,
        String postalCode,
        String country,
        boolean travelAvailable,
        boolean gstRegistered,
        String gstNumber, // Sensitive business data; exposed only to authenticated studio member/owner
        String operationalStatus,
        String publicationStatus,
        String roleInStudio,
        Instant onboardingCompletedAt,
        List<BusinessContactItemDto> contacts,
        List<BusinessServiceItemDto> services,
        List<BusinessSpecialtyItemDto> specialties,
        List<BusinessServiceAreaItemDto> serviceAreas
) {
    public record BusinessContactItemDto(
            String kind,
            String value,
            boolean publicConsent,
            String visibilityLabel, // "Public when portfolio published" or "Private / Internal only"
            int sortOrder
    ) {}

    public record BusinessServiceItemDto(String code, String name) {}
    public record BusinessSpecialtyItemDto(String code, String name) {}
    public record BusinessServiceAreaItemDto(String city, String locality) {}
}
