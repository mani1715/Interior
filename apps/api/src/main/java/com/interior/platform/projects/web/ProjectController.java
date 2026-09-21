package com.interior.platform.projects.web;

import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.ProjectActionRequest;
import com.interior.platform.projects.dto.ProjectDetailResponse;
import com.interior.platform.projects.dto.ProjectPresentationDto;
import com.interior.platform.projects.dto.ProjectSummaryResponse;
import com.interior.platform.projects.dto.ReorderProjectsRequest;
import com.interior.platform.projects.dto.UpdateProjectRequest;
import com.interior.platform.projects.service.ProjectService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@Tag(name = "Project CMS", description = "Endpoints for professional interior project creation, editing, reordering, archiving, and portfolio visibility")
public class ProjectController {

    private final ProjectService projectService;
    private final com.interior.platform.media.service.MediaService mediaService;

    public ProjectController(
            ProjectService projectService,
            com.interior.platform.media.service.MediaService mediaService
    ) {
        this.projectService = projectService;
        this.mediaService = mediaService;
    }

    @GetMapping
    @Operation(summary = "List studio projects", description = "List interior design projects for the studio with optional filtering. Accessible by authorized studio members.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<ProjectSummaryResponse>> listProjects(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "category", required = false) ProjectCategory category,
            @RequestParam(value = "visibility", required = false) VisibilityStatus visibility,
            @RequestParam(value = "featured", required = false) Boolean featured,
            @RequestParam(value = "includeArchived", required = false, defaultValue = "false") boolean includeArchived
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        List<ProjectSummaryResponse> list = projectService.listProjects(
                actor,
                requestedStudioId,
                status,
                category,
                visibility,
                featured,
                includeArchived
        );
        return createPrivateNoCacheResponse(list, HttpStatus.OK);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project details", description = "Fetch complete project details including internal notes and readiness checklist. Accessible by authorized studio members.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project details retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ResponseEntity<ProjectDetailResponse> getProject(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("projectId") UUID projectId
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        ProjectDetailResponse project = projectService.getProject(actor, requestedStudioId, projectId);
        return createPrivateNoCacheResponse(project, HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create project", description = "Create a new project in the studio CMS. Restricted to elevated studio roles (OWNER and ADMIN).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ProjectDetailResponse> createProject(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateProjectRequest createRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        ProjectDetailResponse created = projectService.createProject(actor, requestedStudioId, createRequest);
        return createPrivateNoCacheResponse(created, HttpStatus.CREATED);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update project", description = "Update project attributes with optimistic concurrency control. Restricted to OWNER and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<ProjectDetailResponse> updateProject(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody UpdateProjectRequest updateRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        ProjectDetailResponse updated = projectService.updateProject(actor, requestedStudioId, projectId, updateRequest);
        return createPrivateNoCacheResponse(updated, HttpStatus.OK);
    }

    @PostMapping("/reorder")
    @Operation(summary = "Reorder projects", description = "Update the display order of projects for the studio. Restricted to OWNER and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projects reordered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid project IDs"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<ProjectSummaryResponse>> reorderProjects(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody ReorderProjectsRequest reorderRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        List<ProjectSummaryResponse> list = projectService.reorderProjects(actor, requestedStudioId, reorderRequest);
        return createPrivateNoCacheResponse(list, HttpStatus.OK);
    }

    @PostMapping("/{projectId}/archive")
    @Operation(summary = "Archive project", description = "Archive a project and hide it from portfolio. Restricted to OWNER and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project archived"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<ProjectDetailResponse> archiveProject(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody ProjectActionRequest actionRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        ProjectDetailResponse archived = projectService.archiveProject(actor, requestedStudioId, projectId, actionRequest.version());
        return createPrivateNoCacheResponse(archived, HttpStatus.OK);
    }

    @PostMapping("/{projectId}/restore")
    @Operation(summary = "Restore project", description = "Restore an archived project to DRAFT/READY. Restricted to OWNER and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project restored"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<ProjectDetailResponse> restoreProject(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody ProjectActionRequest actionRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        ProjectDetailResponse restored = projectService.restoreProject(actor, requestedStudioId, projectId, actionRequest.version());
        return createPrivateNoCacheResponse(restored, HttpStatus.OK);
    }

    @GetMapping("/{projectId}/media")
    @Operation(summary = "List project media", description = "Lists media assets associated with a specific project.")
    public ResponseEntity<List<com.interior.platform.media.dto.MediaDetailResponse>> listProjectMedia(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("projectId") UUID projectId
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        List<com.interior.platform.media.dto.MediaDetailResponse> list = mediaService.listProjectMedia(actor, requestedStudioId, projectId);
        return createPrivateNoCacheResponse(list, HttpStatus.OK);
    }

    @PostMapping("/{projectId}/media/reorder")
    @Operation(summary = "Reorder project media", description = "Reorders media assets within a project.")
    public ResponseEntity<Void> reorderProjectMedia(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody com.interior.platform.media.dto.ReorderMediaRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        mediaService.reorderMedia(actor, requestedStudioId, projectId, req);
        return ResponseEntity.noContent().build();
    }

    private <T> ResponseEntity<T> createPrivateNoCacheResponse(T body, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private UUID resolveRequestedStudioId(String header, UUID param) {
        if (param != null) {
            return param;
        }
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
