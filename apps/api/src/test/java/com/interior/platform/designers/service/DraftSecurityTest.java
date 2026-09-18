package com.interior.platform.designers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.designers.domain.OnboardingDraftRecord;
import com.interior.platform.designers.dto.OnboardingDraftDto;
import com.interior.platform.designers.dto.OnboardingStatusResponse;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.SessionSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DraftSecurityTest {

    private StudioRepository studioRepository;
    private SecurityRepository securityRepository;
    private ProfessionalOnboardingService onboardingService;
    private ObjectMapper objectMapper;

    private UUID userAId;
    private UUID userBId;
    private ActorContext actorA;
    private ActorContext actorB;

    @BeforeEach
    void setUp() {
        studioRepository = mock(StudioRepository.class);
        securityRepository = mock(SecurityRepository.class);
        SlugValidationService slugValidationService = new SlugValidationService(studioRepository);
        SessionSecurityService sessionSecurityService = new SessionSecurityService(
                new com.interior.platform.security.config.AuthSecurityProperties(),
                securityRepository,
                java.time.Clock.fixed(java.time.Instant.parse("2026-09-18T10:00:00Z"), java.time.ZoneOffset.UTC)
        );
        AuditService auditService = new AuditService(securityRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        objectMapper = new ObjectMapper();

        onboardingService = new ProfessionalOnboardingService(
                studioRepository,
                securityRepository,
                slugValidationService,
                sessionSecurityService,
                auditService
        );

        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();

        actorA = new ActorContext(userAId, "User A", "userA@example.com", Set.of("CUSTOMER"), null, null, true);
        actorB = new ActorContext(userBId, "User B", "userB@example.com", Set.of("CUSTOMER"), null, null, true);

        UserRecord activeUserA = new UserRecord(userAId, "User A", "userA@example.com", null, "ACTIVE", Instant.now(), Instant.now(), 0L);
        UserRecord activeUserB = new UserRecord(userBId, "User B", "userB@example.com", null, "ACTIVE", Instant.now(), Instant.now(), 0L);

        when(securityRepository.findUserById(userAId)).thenReturn(Optional.of(activeUserA));
        when(securityRepository.findUserById(userBId)).thenReturn(Optional.of(activeUserB));
    }

    @Test
    @DisplayName("User A cannot read User B's draft - query is scoped strictly to authenticated actor")
    void testDraftIsolationBetweenUsers() {
        // Mock User B has an existing draft
        OnboardingDraftRecord draftB = new OnboardingDraftRecord(
                UUID.randomUUID(),
                userBId,
                4,
                "{\"studioName\":\"Secret Studio B\",\"city\":\"Hyderabad\"}",
                "IN_PROGRESS",
                Instant.now(),
                Instant.now()
        );
        when(studioRepository.findDraftByUserId(userBId)).thenReturn(Optional.of(draftB));
        when(studioRepository.findDraftByUserId(userAId)).thenReturn(Optional.empty());

        // When Actor A requests onboarding status, it must only query User A's draft
        OnboardingStatusResponse statusA = onboardingService.getStatus(actorA);

        assertEquals("NOT_STARTED", statusA.status());
        assertNull(statusA.draftPayload());

        // Verify repository was strictly queried with User A's ID
        verify(studioRepository).findDraftByUserId(userAId);
        verify(studioRepository, never()).findDraftByUserId(userBId);
    }

    @Test
    @DisplayName("Injected security fields in draft payload are stripped on save")
    void testInjectedSecurityFieldsAreStripped() throws Exception {
        String maliciousPayload = """
            {
                "studioName": "Legit Studio",
                "city": "Vijayawada",
                "userId": "00000000-0000-0000-0000-000000000001",
                "ownerUserId": "00000000-0000-0000-0000-000000000001",
                "studioId": "11111111-1111-1111-1111-111111111111",
                "tenantId": "22222222-2222-2222-2222-222222222222",
                "role": "ADMIN",
                "roles": ["ADMIN", "PLATFORM_OPERATOR"],
                "permissions": ["ALL"],
                "publicationStatus": "PUBLISHED",
                "status": "VERIFIED",
                "verified": true,
                "isAdmin": true
            }
            """;

        OnboardingDraftDto dto = new OnboardingDraftDto(2, maliciousPayload);
        onboardingService.saveDraft(actorA, dto);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(studioRepository).saveDraft(eq(userAId), eq(2), payloadCaptor.capture(), eq("IN_PROGRESS"));

        String savedJson = payloadCaptor.getValue();
        JsonNode root = objectMapper.readTree(savedJson);

        // Verify legitimate business fields remain
        assertEquals("Legit Studio", root.get("studioName").asText());
        assertEquals("Vijayawada", root.get("city").asText());

        // Verify ALL privileged/security fields were stripped
        assertFalse(root.has("userId"), "userId must be stripped");
        assertFalse(root.has("ownerUserId"), "ownerUserId must be stripped");
        assertFalse(root.has("studioId"), "studioId must be stripped");
        assertFalse(root.has("tenantId"), "tenantId must be stripped");
        assertFalse(root.has("role"), "role must be stripped");
        assertFalse(root.has("roles"), "roles must be stripped");
        assertFalse(root.has("permissions"), "permissions must be stripped");
        assertFalse(root.has("publicationStatus"), "publicationStatus must be stripped");
        assertFalse(root.has("status"), "status must be stripped");
        assertFalse(root.has("verified"), "verified must be stripped");
        assertFalse(root.has("isAdmin"), "isAdmin must be stripped");
    }

    @Test
    @DisplayName("Invalid JSON draft payload throws BadRequestException")
    void testInvalidJsonDraftRejected() {
        OnboardingDraftDto invalidDto = new OnboardingDraftDto(2, "{not-valid-json: true");
        assertThrows(BadRequestException.class, () -> onboardingService.saveDraft(actorA, invalidDto));
        verify(studioRepository, never()).saveDraft(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Empty or null draft payload throws BadRequestException")
    void testEmptyDraftRejected() {
        assertThrows(BadRequestException.class, () -> onboardingService.saveDraft(actorA, new OnboardingDraftDto(1, "")));
        assertThrows(BadRequestException.class, () -> onboardingService.saveDraft(actorA, new OnboardingDraftDto(1, "   ")));
        assertThrows(BadRequestException.class, () -> onboardingService.saveDraft(actorA, new OnboardingDraftDto(1, null)));
    }

    @Test
    @DisplayName("Unauthenticated actor cannot save draft")
    void testUnauthenticatedCannotSaveDraft() {
        ActorContext anon = ActorContext.anonymous();
        OnboardingDraftDto dto = new OnboardingDraftDto(1, "{\"studioName\":\"Test\"}");
        assertThrows(UnauthorizedException.class, () -> onboardingService.saveDraft(anon, dto));
    }
}
