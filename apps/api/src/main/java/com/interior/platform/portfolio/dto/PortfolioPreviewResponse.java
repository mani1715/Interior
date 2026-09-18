package com.interior.platform.portfolio.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.domain.SectionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PortfolioPreviewResponse(
        UUID portfolioId,
        UUID studioId,
        String studioName,
        String studioSlug,
        String professionalType,
        String professionalTitle,
        String studioCity,
        String studioState,
        PortfolioTemplateKey templateKey,
        PortfolioStatus status,
        String headline,
        String subheadline,
        String bio,
        String designPhilosophy,
        Integer yearsOfExperience,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        FontPairing fontPairing,
        List<PreviewContactDto> publicContacts,
        List<PreviewServiceDto> canonicalServices,
        List<PreviewSpecialtyDto> canonicalSpecialties,
        List<PreviewServiceAreaDto> canonicalServiceAreas,
        List<PreviewSectionDto> visibleSections,
        Instant previewGeneratedAt
) {
    public record PreviewContactDto(String kind, String contactValue) {}
    public record PreviewServiceDto(String serviceCode, String serviceName) {}
    public record PreviewSpecialtyDto(String specialtyCode, String specialtyName) {}
    public record PreviewServiceAreaDto(String cityName, String locality) {}
    public record PreviewSectionDto(
            UUID sectionId,
            SectionType sectionType,
            int displayOrder,
            int schemaVersion,
            JsonNode content
    ) {}
}
