package com.interior.platform.projects;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.projects.domain.AreaUnit;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectScope;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.PropertyType;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.ProjectDetailResponse;
import com.interior.platform.projects.dto.ProjectPresentationDto;
import com.interior.platform.projects.dto.ProjectSummaryResponse;
import com.interior.platform.projects.dto.ReorderProjectsRequest;
import com.interior.platform.projects.dto.UpdateProjectRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private AuditService auditService;

    private AuthorizationService authorizationService;
    private ProjectService projectService;

    private UUID userId;
    private UUID studioId;
    private ActorContext ownerActor;
    private ActorContext memberActor;
    private UserRecord activeUser;
    private StudioMemberRecord ownerMembership;
    private StudioMemberRecord regularMembership;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        projectService = new ProjectService(
                projectRepository,
                securityRepository,
                authorizationService,
                auditService
        );

        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();

        ownerActor = new ActorContext(
                userId, "Owner User", "owner@studio.com",
                Set.of("DESIGNER"), studioId, "OWNER", true
        );

        memberActor = new ActorContext(
                userId, "Member User", "member@studio.com",
                Set.of("DESIGNER"), studioId, "MEMBER", true
        );

        activeUser = new UserRecord(
                userId, "Owner User", "owner@studio.com", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );

        ownerMembership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Studio Name", "studio-name", userId, "OWNER", Instant.now()
        );

        regularMembership = new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Studio Name", "studio-name", userId, "MEMBER", Instant.now()
        );
    }

    private void mockOwnerSecurity() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(ownerMembership));
    }

    private void mockMemberSecurity() {
        when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(regularMembership));
    }

    @Test
    @DisplayName("1. OWNER can create project with server-derived READY status when required fields present")
    void testCreateProjectReadyStatus() {
        mockOwnerSecurity();

        when(projectRepository.existsBySlug(eq(studioId), eq("emerald-penthouse"), any())).thenReturn(false);
        when(projectRepository.getNextDisplayOrder(studioId)).thenReturn(0);
        when(projectRepository.createProject(any(), anyList())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectRequest request = new CreateProjectRequest(
                "Emerald Penthouse",
                ProjectCategory.COMPLETE_HOME_INTERIOR,
                "A luxurious 4BHK apartment in Bengaluru with Italian marble.",
                "Full comprehensive interior design description here...",
                PropertyType.APARTMENT,
                ProjectScope.FULL_INTERIOR,
                List.of(ProjectStyle.WARM_CONTEMPORARY, ProjectStyle.MODERN_MINIMALIST),
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "IN",
                2025,
                BudgetVisibility.RANGE,
                new BigDecimal("2500000"),
                new BigDecimal("3500000"),
                "INR",
                ClientNameVisibility.DISPLAY,
                "Sharma Residence",
                new BigDecimal("3200"),
                AreaUnit.SQ_FT,
                VisibilityStatus.PORTFOLIO,
                true,
                "Design award candidate"
        );

        ProjectDetailResponse res = projectService.createProject(ownerActor, studioId, request);

        assertNotNull(res);
        assertEquals("emerald-penthouse", res.slug());
        assertEquals(ProjectStatus.READY, res.projectStatus());
        assertTrue(res.isReady());
        assertTrue(res.missingReadinessFields().isEmpty());
        assertTrue(res.featured());
        assertEquals(VisibilityStatus.PORTFOLIO, res.visibilityStatus());
        assertEquals("Sharma Residence", res.clientDisplayName());
        assertEquals(2, res.styleCodes().size());

        verify(auditService).record(eq(userId), eq(studioId), eq("PROJECT_CREATED"), eq("PROJECT"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("2. Project status derives to DRAFT when required readiness fields are missing")
    void testCreateProjectDraftStatusWhenIncomplete() {
        mockOwnerSecurity();

        when(projectRepository.existsBySlug(eq(studioId), eq("minimal-kitchen"), any())).thenReturn(false);
        when(projectRepository.getNextDisplayOrder(studioId)).thenReturn(1);
        when(projectRepository.createProject(any(), anyList())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectRequest request = new CreateProjectRequest(
                "Minimal Kitchen",
                ProjectCategory.MODULAR_KITCHEN,
                null, // missing shortDescription
                null,
                null,
                null,
                null,
                null, // missing city
                null,
                null, // missing state
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ProjectDetailResponse res = projectService.createProject(ownerActor, studioId, request);

        assertNotNull(res);
        assertEquals(ProjectStatus.DRAFT, res.projectStatus());
        assertFalse(res.isReady());
        assertFalse(res.missingReadinessFields().isEmpty());
        assertEquals(VisibilityStatus.PRIVATE, res.visibilityStatus()); // defaults to PRIVATE
    }

    @Test
    @DisplayName("3. Slug generation resolves collisions by appending incremental suffix")
    void testSlugCollisionResolution() {
        mockOwnerSecurity();

        when(projectRepository.existsBySlug(studioId, "lake-view-villa", null)).thenReturn(true);
        when(projectRepository.existsBySlug(studioId, "lake-view-villa-2", null)).thenReturn(true);
        when(projectRepository.existsBySlug(studioId, "lake-view-villa-3", null)).thenReturn(false);
        when(projectRepository.getNextDisplayOrder(studioId)).thenReturn(2);
        when(projectRepository.createProject(any(), anyList())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectRequest request = new CreateProjectRequest(
                "Lake View Villa",
                ProjectCategory.COMPLETE_HOME_INTERIOR,
                "A waterfront villa with panoramic scenic views.",
                null,
                PropertyType.VILLA,
                ProjectScope.TURNKEY,
                null,
                "Udaipur",
                null,
                "Rajasthan",
                "IN",
                2024,
                BudgetVisibility.HIDDEN,
                null,
                null,
                "INR",
                ClientNameVisibility.HIDDEN,
                null,
                null,
                null,
                VisibilityStatus.PRIVATE,
                false,
                null
        );

        ProjectDetailResponse res = projectService.createProject(ownerActor, studioId, request);
        assertEquals("lake-view-villa-3", res.slug());
    }

    @Test
    @DisplayName("4. MEMBER cannot create or update projects (AccessDeniedException)")
    void testMemberCannotCreateProject() {
        mockMemberSecurity();

        CreateProjectRequest request = new CreateProjectRequest(
                "Unauthorized Project",
                ProjectCategory.LIVING_ROOM,
                "Short description here for living room project",
                null, null, null, null, "Delhi", null, "Delhi", "IN",
                null, null, null, null, null, null, null, null, null, null, null, null
        );

        assertThrows(AccessDeniedException.class, () ->
                projectService.createProject(memberActor, studioId, request)
        );
    }

    @Test
    @DisplayName("5. Update project throws ConflictException on version mismatch (optimistic locking)")
    void testUpdateOptimisticLocking() {
        mockOwnerSecurity();
        UUID projectId = UuidV7.randomUuid();

        StudioProjectRecord existing = new StudioProjectRecord(
                projectId, studioId, "project-slug", "Old Title", "Old description...",
                null, ProjectCategory.BEDROOM, ProjectStatus.DRAFT, VisibilityStatus.PRIVATE,
                false, 0, "Mumbai", null, "Maharashtra", "IN", null, null, null,
                BudgetVisibility.HIDDEN, null, null, "INR", ClientNameVisibility.HIDDEN, null,
                null, null, null, 1L, userId, Instant.now(), Instant.now(), null
        );

        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(existing));
        when(projectRepository.updateProject(any(), anyList(), eq(1L))).thenReturn(0); // 0 rows updated -> conflict

        UpdateProjectRequest updateReq = new UpdateProjectRequest(
                1L,
                "New Title",
                ProjectCategory.BEDROOM,
                "New short description...",
                null, null, null, null,
                "Mumbai", null, "Maharashtra", "IN", null,
                null, null, null, null, null, null, null, null, null, null, null
        );

        assertThrows(ConflictException.class, () ->
                projectService.updateProject(ownerActor, studioId, projectId, updateReq)
        );
    }

    @Test
    @DisplayName("6. Budget validation rejects minimum budget exceeding maximum budget")
    void testBudgetValidation() {
        mockOwnerSecurity();

        CreateProjectRequest request = new CreateProjectRequest(
                "Invalid Budget Project",
                ProjectCategory.LIVING_ROOM,
                "Short summary of project here...",
                null, null, null, null, "Pune", null, "Maharashtra", "IN",
                null, BudgetVisibility.RANGE,
                new BigDecimal("5000000"), // min > max
                new BigDecimal("2000000"),
                "INR", ClientNameVisibility.HIDDEN, null, null, null, null, null, null
        );

        assertThrows(BadRequestException.class, () ->
                projectService.createProject(ownerActor, studioId, request)
        );
    }

    @Test
    @DisplayName("7. Client privacy requires client name when visibility is DISPLAY")
    void testClientNameValidationWhenDisplay() {
        mockOwnerSecurity();

        CreateProjectRequest request = new CreateProjectRequest(
                "Client Display Project",
                ProjectCategory.LIVING_ROOM,
                "Short summary of project here...",
                null, null, null, null, "Pune", null, "Maharashtra", "IN",
                null, BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.DISPLAY, // DISPLAY but no clientDisplayName
                "", null, null, null, null, null
        );

        assertThrows(BadRequestException.class, () ->
                projectService.createProject(ownerActor, studioId, request)
        );
    }

    @Test
    @DisplayName("8. Reordering projects delegates display order updates")
    void testReorderProjects() {
        mockOwnerSecurity();
        UUID p1 = UuidV7.randomUuid();
        UUID p2 = UuidV7.randomUuid();

        when(projectRepository.listProjects(eq(studioId), any(), any(), any(), any(), eq(false))).thenReturn(List.of());

        ReorderProjectsRequest reorderReq = new ReorderProjectsRequest(List.of(p2, p1));
        List<ProjectSummaryResponse> list = projectService.reorderProjects(ownerActor, studioId, reorderReq);

        assertNotNull(list);
        verify(projectRepository).updateDisplayOrders(studioId, List.of(p2, p1));
        verify(auditService).record(eq(userId), eq(studioId), eq("PROJECTS_REORDERED"), eq("PROJECT"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("9. Archive project sets status to ARCHIVED and hides from portfolio")
    void testArchiveProject() {
        mockOwnerSecurity();
        UUID projectId = UuidV7.randomUuid();

        when(projectRepository.archiveProject(studioId, projectId, 2L)).thenReturn(1);
        StudioProjectRecord archived = new StudioProjectRecord(
                projectId, studioId, "project-slug", "Archived Project", "Summary...",
                null, ProjectCategory.BEDROOM, ProjectStatus.ARCHIVED, VisibilityStatus.PRIVATE,
                false, 0, "Mumbai", null, "Maharashtra", "IN", null, null, null,
                BudgetVisibility.HIDDEN, null, null, "INR", ClientNameVisibility.HIDDEN, null,
                null, null, null, 3L, userId, Instant.now(), Instant.now(), Instant.now()
        );
        when(projectRepository.findProjectById(studioId, projectId)).thenReturn(Optional.of(archived));

        ProjectDetailResponse res = projectService.archiveProject(ownerActor, studioId, projectId, 2L);

        assertEquals(ProjectStatus.ARCHIVED, res.projectStatus());
        assertEquals(VisibilityStatus.PRIVATE, res.visibilityStatus());
        assertFalse(res.featured());
        verify(auditService).record(eq(userId), eq(studioId), eq("PROJECT_ARCHIVED"), eq("PROJECT"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("10. Public-safe ProjectPresentationDto respects privacy filters (hidden client name, hidden budget)")
    void testPublicSafeProjectPresentation() {
        UUID projectId = UuidV7.randomUuid();
        StudioProjectRecord project = new StudioProjectRecord(
                projectId, studioId, "luxury-suite", "Luxury Suite", "A boutique master bedroom suite.",
                "Full description...", ProjectCategory.BEDROOM, ProjectStatus.READY, VisibilityStatus.PORTFOLIO,
                true, 0, "Chennai", null, "Tamil Nadu", "IN", PropertyType.APARTMENT, ProjectScope.FULL_INTERIOR,
                2024, BudgetVisibility.HIDDEN, new BigDecimal("1500000"), new BigDecimal("2000000"), "INR",
                ClientNameVisibility.HIDDEN, "Secret VIP Client", new BigDecimal("1400"), AreaUnit.SQ_FT,
                "Strict NDA on client identity", 1L, userId, Instant.now(), Instant.now(), null
        );

        when(projectRepository.findPortfolioProjects(studioId)).thenReturn(List.of(project));
        when(projectRepository.findStylesByProjectIds(List.of(projectId))).thenReturn(
                Map.of(projectId, List.of(ProjectStyle.NEO_CLASSICAL))
        );

        List<ProjectPresentationDto> presentations = projectService.getPortfolioProjects(studioId);

        assertEquals(1, presentations.size());
        ProjectPresentationDto dto = presentations.get(0);
        assertEquals("luxury-suite", dto.slug());
        assertEquals("Luxury Suite", dto.title());
        assertNull(dto.clientName(), "Client name must be null when clientNameVisibility is HIDDEN");
        assertNull(dto.budgetFormatted(), "Budget must be null when budgetVisibility is HIDDEN");
        assertEquals("1,400 sq ft", dto.areaFormatted());
        assertEquals("Chennai, Tamil Nadu", dto.location());
        assertEquals("Neo Classical", dto.styleDisplayNames().get(0));
    }
}
