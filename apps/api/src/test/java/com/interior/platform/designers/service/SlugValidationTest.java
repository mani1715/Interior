package com.interior.platform.designers.service;

import com.interior.platform.designers.dto.SlugCheckResponse;
import com.interior.platform.designers.repository.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SlugValidationTest {

    private StudioRepository studioRepository;
    private SlugValidationService slugValidationService;

    @BeforeEach
    void setUp() {
        studioRepository = mock(StudioRepository.class);
        slugValidationService = new SlugValidationService(studioRepository);
    }

    @Test
    @DisplayName("Valid alphanumeric slug with hyphens is accepted")
    void testValidSlug() {
        when(studioRepository.isSlugClaimed("srinivasa-interiors")).thenReturn(false);

        SlugCheckResponse response = slugValidationService.checkAvailability("srinivasa-interiors");
        assertTrue(response.available());
        assertEquals("srinivasa-interiors", response.slug());
    }

    @Test
    @DisplayName("Reserved keywords are rejected with suggestion")
    void testReservedSlugsRejected() {
        SlugCheckResponse resAdmin = slugValidationService.checkAvailability("admin");
        assertFalse(resAdmin.available());
        assertTrue(resAdmin.reason().contains("reserved"));
        assertEquals("admin-studio", resAdmin.suggestedSlug());

        SlugCheckResponse resDashboard = slugValidationService.checkAvailability("dashboard");
        assertFalse(resDashboard.available());
        assertEquals("dashboard-studio", resDashboard.suggestedSlug());

        SlugCheckResponse resApi = slugValidationService.checkAvailability("api");
        assertFalse(resApi.available());
    }

    @Test
    @DisplayName("Already claimed slug is rejected with alternative suggestion")
    void testClaimedSlugRejected() {
        when(studioRepository.isSlugClaimed("atelier-design")).thenReturn(true);
        when(studioRepository.isSlugClaimed("atelier-design-2")).thenReturn(false);

        SlugCheckResponse res = slugValidationService.checkAvailability("atelier-design");
        assertFalse(res.available());
        assertTrue(res.reason().contains("already taken"));
        assertEquals("atelier-design-2", res.suggestedSlug());
    }

    @Test
    @DisplayName("Slug generation normalizes business names with spaces, accents, and special characters")
    void testSlugGeneration() {
        assertEquals("srinivasa-interiors", slugValidationService.generateSlug("Srinivasa Interiors"));
        assertEquals("aditya-associates", slugValidationService.generateSlug("Aditya & Associates"));
        assertEquals("atelier-d-art", slugValidationService.generateSlug("Atelier d'Art"));
        assertEquals("decor-studio", slugValidationService.generateSlug("Décor Studio"));
    }

    @Test
    @DisplayName("Slug generation creates deterministic ASCII fallback for non-Latin scripts")
    void testNonLatinScriptFallback() {
        String teluguSlug = slugValidationService.generateSlug("శ్రీనివాస ఇంటీరియర్స్");
        assertTrue(teluguSlug.startsWith("studio-"));
        assertTrue(teluguSlug.length() >= 8);

        String hindiSlug = slugValidationService.generateSlug("श्रीनिवास इंटीरियर्स");
        assertTrue(hindiSlug.startsWith("studio-"));
    }

    @Test
    @DisplayName("Slug length constraints are enforced (min 3, max 64)")
    void testSlugLengthConstraints() {
        assertFalse(slugValidationService.checkAvailability("ab").available());
        assertTrue(slugValidationService.checkAvailability("abc").available());

        String veryLong = "a".repeat(70);
        assertFalse(slugValidationService.checkAvailability(veryLong).available());
    }
}
