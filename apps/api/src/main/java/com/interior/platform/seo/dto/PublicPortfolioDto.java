package com.interior.platform.seo.dto;

import java.util.List;

public record PublicPortfolioDto(
        String templateKey,
        String headline,
        String subheadline,
        String bio,
        String designPhilosophy,
        Integer yearsOfExperience,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String fontPairing,
        List<PublicSectionDto> visibleSections
) {
    public record PublicSectionDto(
            String sectionType,
            int displayOrder,
            String contentJson
    ) {}
}
