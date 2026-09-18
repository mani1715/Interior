package com.interior.platform.designers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnboardingCompletionRequest(
        String professionalType,
        String studioName,
        String slug,
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
        List<String> services,
        List<String> specialties,
        List<String> serviceAreas,
        String businessPhone,
        String whatsappNumber,
        String businessEmail,
        String websiteUrl,
        String instagramUrl,
        boolean confirmedAccuracy,
        boolean confirmedContentOwnership
) {}
