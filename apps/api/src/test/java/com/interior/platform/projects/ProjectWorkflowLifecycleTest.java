package com.interior.platform.projects;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.dto.MediaPresentationDto;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.projects.domain.*;
import com.interior.platform.projects.dto.*;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.projects.service.ProjectService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectWorkflowLifecycleTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private MediaService mediaService;
    @Mock
    private StudioRepository studioRepository;

    private AuthorizationService authorizationService;
    private ProjectService projectService;

    private UUID userId;
    private UUID studioAId;
    private UUID studioBId;
    private UUID projectId;

    private ActorContext studioAAdmin;
    private ActorContext studioAMember;
    private ActorContext studioBAdmin;
    private ActorContext customerActor;
    private UserRecord activeUser;
    private StudioMemberRecord studioAMembership;
    private StudioMemberRecord studioAMemberRole;
    private StudioDetailRecord studioADetail;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        projectService = new ProjectService(
                projectRepository,
                securityRepository,
                authorizationService,
                auditService,
                mediaService
        );
        projectService.setStudioRepository(studioRepository);

        userId = UuidV7.randomUuid();
        studioAId = UuidV7.randomUuid();
        studioBId = UuidV7.randomUuid();
        projectId = UuidV7.randomUuid();

        studioAAdmin = new ActorContext(
                userId, "Studio A Admin", "admin@studioa.com",
                Set.of("DESIGNER"), studioAId, "DESIGNER_ADMIN", true
        );
        studioAMember = new ActorContext(
                userId, "Studio A Member", "member@studioa.com",
                Set.of("DESIGNER_TEAM"), studioAId, "MEMBER", true
        );
        studioBAdmin = new ActorContext(
                userId, "Studio B Admin", "admin@studiob.com",
                Set.of("DESIGNER"), studioBId, "DESIGNER_ADMIN", true
        );
        customerActor = new ActorContext(
                userId, "Customer", "customer@example.com",
                Set.of("CUSTOMER"), null, null, true
        );

        activeUser = new UserRecord(
                userId, "Studio A Admin", "admin@studioa.com", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        studioAMembership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioAId, "Studio A", "studio-a", userId, "DESIGNER_ADMIN", Instant.now()
        );
        studioAMemberRole = new StudioMemberRecord(
                UuidV7.randomUuid(), studioAId, "Studio A", "studio-a", userId, "MEMBER", Instant.now()
        );
        studioADetail = new StudioDetailRecord(
                studioAId,
                "Studio A",
                "studio-a",
                userId,
                "ACTIVE",
                "INTERIOR_STUDIO",
                "Principal Architect",
                "Crafting bespoke residential spaces",
                2018,
                "5-10",
                "15L-35L",
                "123 Lake Road",
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "560001",
                "IN",
                true,
                false,
                null,
                "PUBLISHED",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private void mockStudioASecurity() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(studioAMembership));
    }

    private StudioProjectRecord buildSampleProject(ProjectStatus status, VisibilityStatus visibility, Instant archivedAt) {
        return new StudioProjectRecord(
                projectId,
                studioAId,
                "luxury-villa",
                "Luxury Villa Residency",
                "A grand 5BHK bespoke residential development in Bengaluru.",
                "Full comprehensive interior design description here...",
                ProjectCategory.COMPLETE_HOME_INTERIOR,
                status,
                visibility,
                false,
                0,
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "IN",
                PropertyType.VILLA,
                ProjectScope.FULL_INTERIOR,
                2024,
                BudgetVisibility.HIDDEN,
                null,
                null,
                "INR",
                ClientNameVisibility.HIDDEN,
                null,
                new BigDecimal("5200"),
                AreaUnit.SQ_FT,
                "Internal notes",
                ProjectPresentationMode.STANDARD,
                1L,
                userId,
                Instant.now(),
                Instant.now(),
                archivedAt
        );
    }

    @Test
    @DisplayName("Publish Gate: Accurately reports public media availability status")
    void testPublishCheckReportsPublicMediaPresence() {
        mockStudioASecurity();
        StudioProjectRecord project = buildSampleProject(ProjectStatus.READY, VisibilityStatus.PRIVATE, null);

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(Optional.of(project));
        when(studioRepository.findStudioById(studioAId)).thenReturn(Optional.of(studioADetail));
        when(mediaService.getProjectMediaPresentation(studioAId, projectId)).thenReturn(Collections.emptyList());

        ProjectPublishCheckResponse check = projectService.checkPublishability(studioAAdmin, studioAId, projectId);

        assertTrue(check.isPublishable());
        assertFalse(check.hasPublicMedia());
        assertTrue(check.isProjectReady());
        assertTrue(check.isStudioActive());
        assertTrue(check.blockers().isEmpty());
    }

    @Test
    @DisplayName("Publish Gate: Rejects publishing when project is in incomplete DRAFT status")
    void testPublishCheckFailsWhenProjectNotReady() {
        mockStudioASecurity();
        // Missing short description and city
        StudioProjectRecord incompleteProject = new StudioProjectRecord(
                projectId, studioAId, "incomplete-project", "AB", null, null,
                null, ProjectStatus.DRAFT, VisibilityStatus.PRIVATE, false, 0,
                null, null, null, "IN", null, null, null,
                BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                ProjectPresentationMode.STANDARD, 1L, userId, Instant.now(), Instant.now(), null
        );

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(Optional.of(incompleteProject));
        when(studioRepository.findStudioById(studioAId)).thenReturn(Optional.of(studioADetail));
        when(mediaService.getProjectMediaPresentation(studioAId, projectId)).thenReturn(List.of(mock(MediaPresentationDto.class)));

        ProjectPublishCheckResponse check = projectService.checkPublishability(studioAAdmin, studioAId, projectId);

        assertFalse(check.isPublishable());
        assertFalse(check.isProjectReady());
        assertTrue(check.blockers().stream().anyMatch(b -> b.contains("PROJECT_NOT_READY")));
    }

    @Test
    @DisplayName("Publish Gate: Rejects publishing when project is archived")
    void testPublishCheckFailsWhenArchived() {
        mockStudioASecurity();
        StudioProjectRecord archivedProject = buildSampleProject(ProjectStatus.ARCHIVED, VisibilityStatus.PRIVATE, Instant.now());

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(Optional.of(archivedProject));
        when(studioRepository.findStudioById(studioAId)).thenReturn(Optional.of(studioADetail));
        when(mediaService.getProjectMediaPresentation(studioAId, projectId)).thenReturn(List.of(mock(MediaPresentationDto.class)));

        ProjectPublishCheckResponse check = projectService.checkPublishability(studioAAdmin, studioAId, projectId);

        assertFalse(check.isPublishable());
        assertTrue(check.blockers().stream().anyMatch(b -> b.contains("PROJECT_ARCHIVED")));
    }

    @Test
    @DisplayName("Publish Gate: Succeeds and publishes when all requirements met, incrementing version")
    void testPublishSuccess() {
        mockStudioASecurity();
        StudioProjectRecord draftProject = buildSampleProject(ProjectStatus.READY, VisibilityStatus.PRIVATE, null);

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(
                Optional.of(draftProject),
                Optional.of(buildSampleProject(ProjectStatus.READY, VisibilityStatus.PORTFOLIO, null))
        );
        when(studioRepository.findStudioById(studioAId)).thenReturn(Optional.of(studioADetail));
        when(mediaService.getProjectMediaPresentation(studioAId, projectId)).thenReturn(List.of(mock(MediaPresentationDto.class)));
        when(projectRepository.updateProject(any(), anyList(), eq(1L))).thenReturn(1);

        ProjectDetailResponse res = projectService.publishProject(studioAAdmin, studioAId, projectId, 1L);

        assertNotNull(res);
        assertEquals(VisibilityStatus.PORTFOLIO, res.visibilityStatus());
        assertEquals(ProjectStatus.READY, res.projectStatus());
        verify(auditService).record(eq(userId), eq(studioAId), eq("PROJECT_PUBLISHED"), eq("PROJECT"), anyString(), anyMap(), any(), any());
    }

    @Test
    @DisplayName("Publish Idempotency: Publishing an already-published project returns safely without duplicate audit")
    void testPublishIdempotency() {
        mockStudioASecurity();
        StudioProjectRecord alreadyPublished = buildSampleProject(ProjectStatus.READY, VisibilityStatus.PORTFOLIO, null);

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(Optional.of(alreadyPublished));

        ProjectDetailResponse res = projectService.publishProject(studioAAdmin, studioAId, projectId, 1L);

        assertEquals(VisibilityStatus.PORTFOLIO, res.visibilityStatus());
        verify(projectRepository, never()).updateProject(any(), anyList(), anyLong());
        verify(auditService, never()).record(any(), any(), eq("PROJECT_PUBLISHED"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Unpublish: Reverts project visibility to PRIVATE without deleting data")
    void testUnpublishSuccess() {
        mockStudioASecurity();
        StudioProjectRecord publishedProject = buildSampleProject(ProjectStatus.READY, VisibilityStatus.PORTFOLIO, null);

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(
                Optional.of(publishedProject),
                Optional.of(buildSampleProject(ProjectStatus.READY, VisibilityStatus.PRIVATE, null))
        );
        when(projectRepository.updateProject(any(), anyList(), eq(1L))).thenReturn(1);

        ProjectDetailResponse res = projectService.unpublishProject(studioAAdmin, studioAId, projectId, 1L);

        assertEquals(VisibilityStatus.PRIVATE, res.visibilityStatus());
        verify(auditService).record(eq(userId), eq(studioAId), eq("PROJECT_UNPUBLISHED"), eq("PROJECT"), anyString(), anyMap(), any(), any());
    }

    @Test
    @DisplayName("Unpublish Idempotency: Unpublishing an already-private project returns safely")
    void testUnpublishIdempotency() {
        mockStudioASecurity();
        StudioProjectRecord alreadyPrivate = buildSampleProject(ProjectStatus.READY, VisibilityStatus.PRIVATE, null);

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(Optional.of(alreadyPrivate));

        ProjectDetailResponse res = projectService.unpublishProject(studioAAdmin, studioAId, projectId, 1L);

        assertEquals(VisibilityStatus.PRIVATE, res.visibilityStatus());
        verify(projectRepository, never()).updateProject(any(), anyList(), anyLong());
    }

    @Test
    @DisplayName("Security: Regular studio MEMBER without admin role cannot publish or archive project")
    void testMemberCannotPublishOrArchive() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(studioAMemberRole));

        assertThrows(AccessDeniedException.class, () ->
                projectService.publishProject(studioAMember, studioAId, projectId, 1L)
        );
        assertThrows(AccessDeniedException.class, () ->
                projectService.archiveProject(studioAMember, studioAId, projectId, 1L)
        );
    }

    @Test
    @DisplayName("Security: CUSTOMER role is denied all project CMS operations")
    void testCustomerDeniedProjectCms() {
        assertThrows(AccessDeniedException.class, () ->
                projectService.listProjects(customerActor, studioAId, null, null, null, null, false)
        );
        assertThrows(AccessDeniedException.class, () ->
                projectService.createProject(customerActor, studioAId, mock(CreateProjectRequest.class))
        );
    }

    @Test
    @DisplayName("Multi-Studio Tenant Isolation: Studio A admin cannot access or modify Studio B projects")
    void testCrossStudioAccessDenied() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(studioAMembership));

        // Attempting to access studio B with studio A credentials
        assertThrows(AccessDeniedException.class, () ->
                projectService.getProject(studioAAdmin, studioBId, projectId)
        );
        assertThrows(AccessDeniedException.class, () ->
                projectService.publishProject(studioAAdmin, studioBId, projectId, 1L)
        );
    }

    @Test
    @DisplayName("Concurrency: ConflictException raised when concurrent modification alters expected version")
    void testOptimisticConcurrencyConflict() {
        mockStudioASecurity();
        StudioProjectRecord draftProject = buildSampleProject(ProjectStatus.READY, VisibilityStatus.PRIVATE, null);

        when(projectRepository.findProjectById(studioAId, projectId)).thenReturn(Optional.of(draftProject));
        when(studioRepository.findStudioById(studioAId)).thenReturn(Optional.of(studioADetail));
        when(mediaService.getProjectMediaPresentation(studioAId, projectId)).thenReturn(List.of(mock(MediaPresentationDto.class)));
        // 0 rows updated signals version mismatch
        when(projectRepository.updateProject(any(), anyList(), eq(1L))).thenReturn(0);

        assertThrows(ConflictException.class, () ->
                projectService.publishProject(studioAAdmin, studioAId, projectId, 1L)
        );
    }
}
