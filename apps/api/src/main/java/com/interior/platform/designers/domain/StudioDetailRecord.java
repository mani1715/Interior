package com.interior.platform.designers.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioDetailRecord(
        UUID id,
        String name,
        String slug,
        UUID ownerId,
        String status,
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
        String gstNumber,
        String publicationStatus,
        Instant onboardingCompletedAt,
        Instant createdAt,
        Instant updatedAt,
        List<StudioContactItem> contacts,
        List<StudioServiceItem> services,
        List<StudioServiceAreaItem> serviceAreas
) {
    public record StudioContactItem(String kind, String value, boolean publicConsent, int sortOrder) {}
    public record StudioServiceItem(String serviceCode, String serviceName) {}
    public record StudioServiceAreaItem(String cityName, String locality) {}
}
