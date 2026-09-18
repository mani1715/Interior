package com.interior.platform.designers.service;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.designers.domain.CanonicalSpecialty;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.SessionSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SpecialtyValidationTest {

    private StudioRepository studioRepository;
    private SecurityRepository securityRepository;
    private SlugValidationService slugValidationService;
    private SessionSecurityService sessionSecurityService;
    private AuditService auditService;
    private ProfessionalOnboardingService onboardingService;

    private UUID userId;
    private ActorContext customerActor;

    @BeforeEach
    void setUp() {
        studioRepository = mock(StudioRepository.class);
        securityRepository = mock(SecurityRepository.class);
        slugValidationService = new SlugValidationService(studioRepository);
        sessionSecurityService = new SessionSecurityService(
                new com.interior.platform.security.config.AuthSecurityProperties(),
                securityRepository,
                Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC)
        );
        auditService = new AuditService(securityRepository, new com.fasterxml.jackson.databind.ObjectMapper());

        onboardingService = new ProfessionalOnboardingService(
                studioRepository,
                securityRepository,
                slugValidationService,
                sessionSecurityService,
                auditService
        );

        userId = UUID.randomUUID();
        customerActor = new ActorContext(userId, "Test User", "test@test.com", Set.of("CUSTOMER"), null, null, true);

        UserRecord activeUser = new UserRecord(userId, "Test User", "test@test.com", "+919876543210", "ACTIVE", Instant.now(), Instant.now(), 0L);
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(studioRepository.findInitialOnboardingStudioId(userId)).thenReturn(Optional.empty());
        when(studioRepository.isSlugClaimed(any())).thenReturn(false);
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));
    }

    private OnboardingCompletionRequest requestWithSpecialties(List<String> specialties) {
        return new OnboardingCompletionRequest(
                "INTERIOR_STUDIO", "Specialty Studio", "specialty-studio",
                "Lead Designer", "Crafting homes", 2020, "2-5", "15L-35L",
                "Main Road", "Hyderabad", "Hyderabad", "Telangana", "500001", "IN",
                true, false, null,
                List.of("Modular Kitchen"),
                specialties,
                List.of("Hyderabad"),
                "+919876543210", "+919876543210", "contact@specialtystudio.com",
                null, null, true, true
        );
    }

    @Test
    @DisplayName("Valid canonical specialties are persisted to studio_specialties with controlled codes")
    void testValidSpecialtiesPersisted() {
        var req = requestWithSpecialties(List.of("Modern Minimalist", "Warm Contemporary", "SCANDINAVIAN"));

        onboardingService.completeOnboarding(customerActor, req, null, null);

        verify(studioRepository).addStudioSpecialty(any(), eq("MODERN_MINIMALIST"), eq("Modern Minimalist"));
        verify(studioRepository).addStudioSpecialty(any(), eq("WARM_CONTEMPORARY"), eq("Warm Contemporary"));
        verify(studioRepository).addStudioSpecialty(any(), eq("SCANDINAVIAN"), eq("Scandinavian Natural"));
    }

    @Test
    @DisplayName("Invalid or unapproved specialty string is rejected with BadRequestException")
    void testInvalidSpecialtyRejected() {
        var req = requestWithSpecialties(List.of("Modern Minimalist", "Random Unapproved Style 123"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                onboardingService.completeOnboarding(customerActor, req, null, null)
        );

        assertTrue(ex.getMessage().contains("Invalid design specialty"));
        assertTrue(ex.getMessage().contains("Random Unapproved Style 123"));
    }

    @Test
    @DisplayName("Duplicate specialties in request do not create duplicate rows")
    void testDuplicateSpecialtiesDeduplicated() {
        var req = requestWithSpecialties(List.of("Modern Minimalist", "MODERN_MINIMALIST", "modern-minimalist"));

        onboardingService.completeOnboarding(customerActor, req, null, null);

        // Should only be called once for MODERN_MINIMALIST
        verify(studioRepository, times(1)).addStudioSpecialty(any(), eq("MODERN_MINIMALIST"), eq("Modern Minimalist"));
    }

    @Test
    @DisplayName("CanonicalSpecialty enum maps display names, codes, and hyphenated variations correctly")
    void testCanonicalSpecialtyLookup() {
        assertEquals(CanonicalSpecialty.MODERN_MINIMALIST, CanonicalSpecialty.from("Modern Minimalist"));
        assertEquals(CanonicalSpecialty.MODERN_MINIMALIST, CanonicalSpecialty.from("MODERN_MINIMALIST"));
        assertEquals(CanonicalSpecialty.MODERN_MINIMALIST, CanonicalSpecialty.from("modern-minimalist"));

        assertEquals(CanonicalSpecialty.INDIAN_TRADITIONAL, CanonicalSpecialty.from("Indian Traditional / Chettinad"));
        assertEquals(CanonicalSpecialty.NEO_CLASSICAL, CanonicalSpecialty.from("Neo-Classical & Heritage"));
        assertEquals(CanonicalSpecialty.BIOPHILIC, CanonicalSpecialty.from("Biophilic & Sustainable"));

        assertThrows(BadRequestException.class, () -> CanonicalSpecialty.from("non-existent"));
        assertThrows(BadRequestException.class, () -> CanonicalSpecialty.from(""));
        assertThrows(BadRequestException.class, () -> CanonicalSpecialty.from(null));
    }
}
