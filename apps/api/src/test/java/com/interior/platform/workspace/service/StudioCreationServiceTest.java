package com.interior.platform.workspace.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.SlugCheckResponse;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.designers.service.SlugValidationService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.workspace.dto.CreateStudioRequest;
import com.interior.platform.workspace.dto.CreateStudioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudioCreationServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private SlugValidationService slugValidationService;

    @Mock
    private AuditService auditService;

    private AuthorizationService authorizationService;
    private StudioCreationService studioCreationService;

    private UUID userId;
    private ActorContext actor;
    private UserRecord activeUser;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        studioCreationService = new StudioCreationService(
                securityRepository,
                studioRepository,
                slugValidationService,
                authorizationService,
                auditService
        );

        userId = UuidV7.randomUuid();
        actor = new ActorContext(
                userId,
                "Aarav Patel",
                "aarav@example.com",
                Set.of("CUSTOMER"),
                Set.of("project:read"),
                null,
                null,
                "PASSWORD",
                true
        );

        activeUser = new UserRecord(
                userId,
                "Aarav Patel",
                "aarav@example.com",
                "+919876543210",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                0L
        );
    }

    @Test
    @DisplayName("Successfully creates additional studio with auto-generated slug")
    void createStudio_success_generatedSlug() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(slugValidationService.generateSlug("Studio Aura")).thenReturn("studio-aura");
        when(slugValidationService.checkAvailability("studio-aura")).thenReturn(SlugCheckResponse.available("studio-aura"));

        CreateStudioRequest request = new CreateStudioRequest(
                "Studio Aura",
                "INTERIOR_STUDIO",
                "Mumbai",
                "Maharashtra",
                null
        );

        CreateStudioResponse response = studioCreationService.createStudio(actor, request, null);

        assertNotNull(response);
        assertNotNull(response.studioId());
        assertEquals("Studio Aura", response.name());
        assertEquals("studio-aura", response.slug());
        assertEquals("INTERIOR_STUDIO", response.professionalType());
        assertEquals("DESIGNER_ADMIN", response.role());

        verify(studioRepository).createStudio(argThat(s ->
                s.name().equals("Studio Aura") &&
                s.slug().equals("studio-aura") &&
                s.status().equals("ACTIVE") &&
                s.publicationStatus().equals("UNPUBLISHED") &&
                s.ownerId().equals(userId) &&
                s.city().equals("Mumbai") &&
                s.state().equals("Maharashtra")
        ));

        verify(studioRepository).claimSlug(eq(response.studioId()), eq("studio-aura"), eq("CURRENT"));
        verify(securityRepository).addStudioMember(any(UUID.class), eq(response.studioId()), eq(userId), eq("DESIGNER_ADMIN"));
        verify(auditService).record(eq(userId), eq(response.studioId()), eq("STUDIO_CREATED"), eq("STUDIO"), eq(response.studioId().toString()), anyMap(), any(), any());

        // Crucial requirement: No global role mutation
        verify(securityRepository, never()).assignUserRole(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Successfully creates additional studio with custom requested slug")
    void createStudio_success_customSlug() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(slugValidationService.checkAvailability("custom-aura")).thenReturn(SlugCheckResponse.available("custom-aura"));

        CreateStudioRequest request = new CreateStudioRequest(
                "Studio Aura",
                "ARCHITECT",
                "Bengaluru",
                "Karnataka",
                "custom-aura"
        );

        CreateStudioResponse response = studioCreationService.createStudio(actor, request, null);

        assertNotNull(response);
        assertEquals("custom-aura", response.slug());
        assertEquals("ARCHITECT", response.professionalType());
        verify(studioRepository).claimSlug(eq(response.studioId()), eq("custom-aura"), eq("CURRENT"));
    }

    @Test
    @DisplayName("Rejects custom slug when unavailable with ConflictException")
    void createStudio_customSlugTaken_throwsConflict() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(slugValidationService.checkAvailability("taken-slug"))
                .thenReturn(SlugCheckResponse.unavailable("taken-slug", "This handle is already taken", "taken-slug-2"));

        CreateStudioRequest request = new CreateStudioRequest(
                "Studio Aura",
                "INTERIOR_STUDIO",
                "Mumbai",
                "Maharashtra",
                "taken-slug"
        );

        ConflictException ex = assertThrows(ConflictException.class, () ->
                studioCreationService.createStudio(actor, request, null)
        );
        assertTrue(ex.getMessage().contains("already taken"));
        verify(studioRepository, never()).createStudio(any());
    }

    @Test
    @DisplayName("Rejects unauthenticated actor with UnauthorizedException")
    void createStudio_unauthenticated_throwsUnauthorized() {
        ActorContext anon = ActorContext.anonymous();
        CreateStudioRequest request = new CreateStudioRequest(
                "Studio Aura",
                "INTERIOR_STUDIO",
                "Mumbai",
                "Maharashtra",
                null
        );

        assertThrows(UnauthorizedException.class, () ->
                studioCreationService.createStudio(anon, request, null)
        );
    }

    @Test
    @DisplayName("Rejects inactive user account with AccessDeniedException")
    void createStudio_suspendedUser_throwsAccessDenied() {
        UserRecord suspendedUser = new UserRecord(
                userId,
                "Aarav Patel",
                "aarav@example.com",
                "+919876543210",
                "SUSPENDED",
                Instant.now(),
                Instant.now(),
                0L
        );
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(suspendedUser));

        CreateStudioRequest request = new CreateStudioRequest(
                "Studio Aura",
                "INTERIOR_STUDIO",
                "Mumbai",
                "Maharashtra",
                null
        );

        assertThrows(AccessDeniedException.class, () ->
                studioCreationService.createStudio(actor, request, null)
        );
    }

    @Test
    @DisplayName("Validates missing or invalid name with BadRequestException")
    void createStudio_invalidName_throwsBadRequest() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));

        CreateStudioRequest request = new CreateStudioRequest(
                "   ",
                "INTERIOR_STUDIO",
                "Mumbai",
                "Maharashtra",
                null
        );

        assertThrows(BadRequestException.class, () ->
                studioCreationService.createStudio(actor, request, null)
        );
    }

    @Test
    @DisplayName("Validates missing city or state with BadRequestException")
    void createStudio_invalidLocation_throwsBadRequest() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));

        CreateStudioRequest noCity = new CreateStudioRequest(
                "Studio Aura",
                "INTERIOR_STUDIO",
                "",
                "Maharashtra",
                null
        );
        assertThrows(BadRequestException.class, () ->
                studioCreationService.createStudio(actor, noCity, null)
        );

        CreateStudioRequest noState = new CreateStudioRequest(
                "Studio Aura",
                "INTERIOR_STUDIO",
                "Mumbai",
                "",
                null
        );
        assertThrows(BadRequestException.class, () ->
                studioCreationService.createStudio(actor, noState, null)
        );
    }
}
