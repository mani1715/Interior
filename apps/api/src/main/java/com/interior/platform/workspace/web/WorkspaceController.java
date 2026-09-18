package com.interior.platform.workspace.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.workspace.dto.WorkspaceBusinessProfileResponse;
import com.interior.platform.workspace.dto.WorkspaceSummaryResponse;
import com.interior.platform.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/workspace")
@Tag(name = "Professional Workspace", description = "Endpoints for the authenticated interior professional workspace, readiness metrics, and business profile inspection")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get professional workspace summary", description = "Authoritative workspace summary for authenticated DESIGNER / DESIGNER_TEAM accounts. Never public-cacheable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workspace summary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied: insufficient role, suspended account, or tenant boundary violation"),
            @ApiResponse(responseCode = "404", description = "Studio not found")
    })
    public ResponseEntity<WorkspaceSummaryResponse> getWorkspaceSummary(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        WorkspaceSummaryResponse summary = workspaceService.getWorkspaceSummary(actor, requestedStudioId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(summary);
    }

    @GetMapping("/business")
    @Operation(summary = "Get professional business profile", description = "Detailed business profile for authorized studio members, distinguishing public and private contact visibility.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Business profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied: insufficient role or tenant boundary violation"),
            @ApiResponse(responseCode = "404", description = "Studio not found")
    })
    public ResponseEntity<WorkspaceBusinessProfileResponse> getBusinessProfile(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        WorkspaceBusinessProfileResponse profile = workspaceService.getBusinessProfile(actor, requestedStudioId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(profile);
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
                // Let service reject invalid context if needed
            }
        }
        return null;
    }
}
