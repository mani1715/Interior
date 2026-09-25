package com.interior.platform.discovery.dto;

import java.util.List;
import java.util.UUID;

public record DiscoveryProfessionalCardDto(
        UUID id,
        String slug,
        String name,
        String professionalType,
        String professionalTypeLabel,
        String professionalTitle,
        String tagline,
        String city,
        String state,
        Integer experienceSinceYear,
        List<String> services,
        List<String> specialties,
        int projectCount,
        List<String> sampleProjectCoverUrls,
        boolean verified,
        Double reviewAverage,
        int reviewCount
) {
    public DiscoveryProfessionalCardDto(
            UUID id,
            String slug,
            String name,
            String professionalType,
            String professionalTypeLabel,
            String professionalTitle,
            String tagline,
            String city,
            String state,
            Integer experienceSinceYear,
            List<String> services,
            List<String> specialties,
            int projectCount,
            List<String> sampleProjectCoverUrls
    ) {
        this(id, slug, name, professionalType, professionalTypeLabel, professionalTitle,
             tagline, city, state, experienceSinceYear, services, specialties,
             projectCount, sampleProjectCoverUrls, false, null, 0);
    }
}
