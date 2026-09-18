package com.interior.platform.designers.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.designers.domain.OnboardingDraftRecord;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.dto.OnboardingDraftDto;
import com.interior.platform.designers.dto.OnboardingStatusResponse;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.SessionSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProfessionalOnboardingServiceTest {

    private StudioRepository studioRepository;
    private SecurityRepository securityRepository;
    private SlugValidationService slugValidationService;
    private SessionSecurityService sessionSecurityService;
    private AuditService auditService;
    private ProfessionalOnboardingService onboardingService;

    private UUID userId;
    private ActorContext customerActor;
    private UserRecord activeUser;

    @BeforeEach
    void setUp() {
        studioRepository = mock(StudioRepository.class);
        securityRepository = mock(SecurityRepository.class);
        slugValidationService = new SlugValidationService(studioRepository);
        sessionSecurityService = new SessionSecurityService(
                new com.interior.platform.security.config.AuthSecurityProperties(),
                securityRepository,
                java.time.Clock.fixed(java.time.Instant.parse("2026-09-18T10:00:00Z"), java.time.ZoneOffset.UTC)
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
        customerActor = new ActorContext(
                userId,
                "Ramesh Kumar",
                "ramesh@example.com",
                Set.of("CUSTOMER"),
                null,
                null,
                true
        );

        activeUser = new UserRecord(
                userId,
                "Ramesh Kumar",
                "ramesh@example.com",
                "+919876543210",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L
        );

        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
    }

    private OnboardingCompletionRequest createValidRequest() {
        return new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Srinivasa Interiors",
                "srinivasa-interiors",
                "Principal Architect",
                "Crafting soulful living spaces",
                2018,
                "5-10",
                "15L-35L",
                "Brodipet 4th Line",
                "Guntur",
                "Guntur",
                "Andhra Pradesh",
                "522002",
                "IN",
                true,
                true,
                "37AAAAA0000A1Z5",
                List.of("Modular Kitchen", "Living Room", "Full Home Interior"),
                List.of("Modern", "Warm / Natural"),
                List.of("Guntur", "Vijayawada", "Amaravati"),
                "+919876543210",
                "+919876543210",
                "contact@srinivasainteriors.com",
                "https://srinivasainteriors.com",
                "https://instagram.com/srinivasa_interiors",
                true,
                true
        );
    }

    @Test
    @DisplayName("Successful onboarding creates studio, claims slug, assigns OWNER, and grants DESIGNER role")
    void testSuccessfulOnboarding() {
        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.empty());
        when(studioRepository.isSlugClaimed("srinivasa-interiors")).thenReturn(false);
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        var result = onboardingService.completeOnboarding(customerActor, createValidRequest(), req, res);

        assertNotNull(result);
        assertNotNull(result.studio());
        assertEquals("Srinivasa Interiors", result.studio().name());
        assertEquals("srinivasa-interiors", result.studio().slug());
        assertEquals("ACTIVE", result.studio().status());
        assertEquals("UNPUBLISHED", result.studio().publicationStatus());
        assertEquals("OWNER", result.studio().role());

        // Verify studio created with operational status ACTIVE and publication status UNPUBLISHED
        ArgumentCaptor<StudioDetailRecord> studioCaptor = ArgumentCaptor.forClass(StudioDetailRecord.class);
        verify(studioRepository).createStudio(studioCaptor.capture());
        assertEquals("ACTIVE", studioCaptor.getValue().status());
        assertEquals("UNPUBLISHED", studioCaptor.getValue().publicationStatus());
        assertEquals(userId, studioCaptor.getValue().ownerId());

        // Verify slug claimed with state CURRENT
        verify(studioRepository).claimSlug(any(), eq("srinivasa-interiors"), eq("CURRENT"));

        // Verify contacts, services, service areas persisted
        verify(studioRepository, atLeast(1)).addStudioContact(any(), eq("PHONE"), eq("+919876543210"), eq(true), anyInt());
        verify(studioRepository, atLeast(1)).addStudioService(any(), any(), eq("Modular Kitchen"));
        verify(studioRepository, atLeast(1)).addStudioServiceArea(any(), eq("Guntur"), isNull());

        // Verify studio owner membership created
        verify(securityRepository).addStudioMember(any(), any(), eq(userId), eq("OWNER"));

        // Verify DESIGNER platform role granted
        verify(securityRepository).assignUserRole(any(), eq(userId), eq("DESIGNER"), any());

        // Verify draft marked completed
        verify(studioRepository).markDraftCompleted(userId);

        // Verify audit event recorded
        verify(securityRepository).recordAuditEvent(any(), any(), eq(userId), eq("PROFESSIONAL_ONBOARDING_COMPLETED"), eq("STUDIO"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Idempotent completion: double submission returns existing studio without duplicate creation")
    void testIdempotentCompletion() {
        UUID existingStudioId = UUID.randomUUID();
        StudioDetailRecord existingStudio = new StudioDetailRecord(
                existingStudioId,
                "Srinivasa Interiors",
                "srinivasa-interiors",
                userId,
                "ACTIVE",
                "INTERIOR_STUDIO",
                "Principal Architect",
                "Tagline",
                2018,
                "5-10",
                "15L-35L",
                "Address",
                "Guntur",
                "Guntur",
                "Andhra Pradesh",
                "522002",
                "IN",
                true,
                false,
                null,
                "UNPUBLISHED",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                List.of(),
                List.of(),
                List.of()
        );

        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.of(existingStudio));

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        var result = onboardingService.completeOnboarding(customerActor, createValidRequest(), req, res);

        assertNotNull(result);
        assertEquals(existingStudioId, result.studio().id());
        assertTrue(result.message().contains("already completed"));

        // Verify NO duplicate creation or role assignment calls
        verify(studioRepository, never()).createStudio(any());
        verify(securityRepository, never()).assignUserRole(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Unauthenticated actor is rejected with UnauthorizedException")
    void testUnauthenticatedDenied() {
        ActorContext anon = ActorContext.anonymous();
        assertThrows(UnauthorizedException.class, () -> onboardingService.getStatus(anon));
        assertThrows(UnauthorizedException.class, () -> onboardingService.completeOnboarding(anon, createValidRequest(), null, null));
    }

    @Test
    @DisplayName("Inactive or suspended user is rejected")
    void testInactiveUserDenied() {
        UserRecord suspended = new UserRecord(userId, "Suspended", "s@test.com", null, "SUSPENDED", Instant.now(), Instant.now(), 0L);
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(suspended));

        var status = onboardingService.getStatus(customerActor);
        assertEquals("BLOCKED", status.status());

        assertThrows(AccessDeniedException.class, () -> onboardingService.completeOnboarding(customerActor, createValidRequest(), null, null));
    }

    @Test
    @DisplayName("Duplicate or taken slug throws ConflictException (HTTP 409)")
    void testSlugConflictThrows409() {
        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.empty());
        when(studioRepository.isSlugClaimed("srinivasa-interiors")).thenReturn(true);

        assertThrows(ConflictException.class, () -> onboardingService.completeOnboarding(customerActor, createValidRequest(), null, null));
    }

    @Test
    @DisplayName("Reserved platform slug throws ConflictException")
    void testReservedSlugThrowsConflict() {
        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.empty());

        OnboardingCompletionRequest reservedReq = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO", "Admin Studio", "admin", null, null, null, null, null,
                null, "Hyderabad", null, "Telangana", null, "IN", false, false, null,
                List.of("Living Room"), List.of(), List.of(), "+919999999999", null, "admin@test.com",
                null, null, true, true
        );

        assertThrows(ConflictException.class, () -> onboardingService.completeOnboarding(customerActor, reservedReq, null, null));
    }

    @Test
    @DisplayName("Malicious URL schemes (javascript:, data:) are rejected")
    void testMaliciousUrlRejected() {
        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.empty());

        OnboardingCompletionRequest evilUrlReq = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO", "Evil Studio", "evil-studio", null, null, null, null, null,
                null, "Hyderabad", null, "Telangana", null, "IN", false, false, null,
                List.of("Living Room"), List.of(), List.of(), "+919999999999", null, "evil@test.com",
                "javascript:alert(document.cookie)", null, true, true
        );

        assertThrows(BadRequestException.class, () -> onboardingService.completeOnboarding(customerActor, evilUrlReq, null, null));
    }

    @Test
    @DisplayName("HTML script tags in studio name and title are sanitized")
    void testHtmlSanitization() {
        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.empty());
        when(studioRepository.isSlugClaimed("clean-studio")).thenReturn(false);
        when(securityRepository.getUserRoles(userId)).thenReturn(Set.of("CUSTOMER"));

        OnboardingCompletionRequest scriptReq = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "<script>alert('xss')</script>Clean Studio",
                "clean-studio",
                "<b>Lead Architect</b>",
                null, null, null, null, null, "Hyderabad", null, "Telangana", null, "IN",
                false, false, null, List.of("Living Room"), List.of(), List.of(),
                "+919999999999", null, "clean@test.com", null, null, true, true
        );

        onboardingService.completeOnboarding(customerActor, scriptReq, null, null);

        ArgumentCaptor<StudioDetailRecord> captor = ArgumentCaptor.forClass(StudioDetailRecord.class);
        verify(studioRepository).createStudio(captor.capture());

        assertEquals("Clean Studio", captor.getValue().name());
        assertEquals("Lead Architect", captor.getValue().professionalTitle());
        assertFalse(captor.getValue().name().contains("<script>"));
    }

    @Test
    @DisplayName("Invalid Indian GST format is rejected")
    void testInvalidGstRejected() {
        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.empty());

        OnboardingCompletionRequest badGstReq = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO", "Studio A", "studio-a", null, null, null, null, null,
                null, "Hyderabad", null, "Telangana", null, "IN", false, true, "INVALID_GST_123",
                List.of("Living Room"), List.of(), List.of(), "+919999999999", null, "a@test.com",
                null, null, true, true
        );

        assertThrows(BadRequestException.class, () -> onboardingService.completeOnboarding(customerActor, badGstReq, null, null));
    }

    @Test
    @DisplayName("Draft payload exceeds size limit throws BadRequestException")
    void testDraftPayloadSizeLimit() {
        String hugePayload = "x".repeat(70000);
        OnboardingDraftDto hugeDto = new OnboardingDraftDto(2, hugePayload);

        assertThrows(BadRequestException.class, () -> onboardingService.saveDraft(customerActor, hugeDto));
    }

    @Test
    @DisplayName("Draft is saved and retrieved successfully")
    void testDraftSaveAndRetrieve() {
        when(studioRepository.findStudioByOwnerId(userId)).thenReturn(Optional.empty());

        OnboardingDraftDto dto = new OnboardingDraftDto(3, "{\"step\":3,\"city\":\"Guntur\"}");
        onboardingService.saveDraft(customerActor, dto);

        verify(studioRepository).saveDraft(userId, 3, "{\"step\":3,\"city\":\"Guntur\"}", "IN_PROGRESS");

        OnboardingDraftRecord record = new OnboardingDraftRecord(UUID.randomUUID(), userId, 3, "{\"step\":3,\"city\":\"Guntur\"}", "IN_PROGRESS", Instant.now(), Instant.now());
        when(studioRepository.findDraftByUserId(userId)).thenReturn(Optional.of(record));

        OnboardingStatusResponse status = onboardingService.getStatus(customerActor);
        assertEquals("IN_PROGRESS", status.status());
        assertEquals(3, status.currentStep());
        assertEquals("{\"step\":3,\"city\":\"Guntur\"}", status.draftPayload());
    }
}
