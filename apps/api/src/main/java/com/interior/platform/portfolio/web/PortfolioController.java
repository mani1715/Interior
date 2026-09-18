package com.interior.platform.portfolio.web;

import com.interior.platform.portfolio.dto.CreateVersionSnapshotRequest;
import com.interior.platform.portfolio.dto.InitializePortfolioRequest;
import com.interior.platform.portfolio.dto.PortfolioDetailResponse;
import com.interior.platform.portfolio.dto.PortfolioPreviewResponse;
import com.interior.platform.portfolio.dto.ReorderSectionsRequest;
import com.interior.platform.portfolio.dto.RestoreVersionRequest;
import com.interior.platform.portfolio.dto.SwitchTemplateRequest;
import com.interior.platform.portfolio.dto.UpdatePortfolioRequest;
import com.interior.platform.portfolio.dto.UpdateSectionRequest;
import com.interior.platform.portfolio.service.PortfolioService;
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

import java.util.UUID;

@RestController
@RequestMapping("/portfolio")
@Tag(name = "Portfolio Builder Engine", description = "Endpoints for professional portfolio website configuration, sections management, versioning, and private preview")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    @Operation(summary = "Get portfolio details", description = "Fetch portfolio configuration, sections, and recent version history. Accessible by authorized studio members.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio details retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found")
    })
    public ResponseEntity<PortfolioDetailResponse> getPortfolio(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioDetailResponse response = portfolioService.getPortfolio(actor, requestedStudioId);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Initialize portfolio", description = "Idempotent creation of primary portfolio with default sections for the studio. Restricted to OWNER and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio initialized or already exists"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PortfolioDetailResponse> initializePortfolio(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @RequestBody(required = false) InitializePortfolioRequest initRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioDetailResponse response = portfolioService.initializePortfolio(actor, requestedStudioId, initRequest);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @PutMapping
    @Operation(summary = "Update portfolio settings", description = "Update top-level portfolio attributes with optimistic concurrency control. Publication status rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed or publication attempt rejected"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<PortfolioDetailResponse> updatePortfolio(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody UpdatePortfolioRequest updateRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioDetailResponse response = portfolioService.updatePortfolio(actor, requestedStudioId, updateRequest);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @PutMapping("/sections/{sectionId}")
    @Operation(summary = "Update portfolio section", description = "Update section visibility or JSON content with schema validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Section updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Section not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<PortfolioDetailResponse> updateSection(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("sectionId") UUID sectionId,
            @Valid @RequestBody UpdateSectionRequest sectionRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioDetailResponse response = portfolioService.updateSection(actor, requestedStudioId, sectionId, sectionRequest);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @PostMapping("/sections/reorder")
    @Operation(summary = "Reorder portfolio sections", description = "Reorders sections according to provided list of section IDs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sections reordered"),
            @ApiResponse(responseCode = "400", description = "Invalid section IDs"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<PortfolioDetailResponse> reorderSections(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody ReorderSectionsRequest reorderRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioDetailResponse response = portfolioService.reorderSections(actor, requestedStudioId, reorderRequest);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @PostMapping("/switch-template")
    @Operation(summary = "Switch portfolio template", description = "Switches presentation template key while strictly preserving all content.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template switched"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<PortfolioDetailResponse> switchTemplate(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody SwitchTemplateRequest switchRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioDetailResponse response = portfolioService.switchTemplate(actor, requestedStudioId, switchRequest);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @PostMapping("/versions")
    @Operation(summary = "Create version snapshot", description = "Creates an immutable snapshot of current portfolio configuration and sections.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Snapshot created"),
            @ApiResponse(responseCode = "400", description = "Validation failed or size limit exceeded"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<PortfolioDetailResponse> createVersion(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateVersionSnapshotRequest snapshotRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioDetailResponse response = portfolioService.createVersionSnapshot(actor, requestedStudioId, snapshotRequest);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @PostMapping("/versions/{versionNumber}/restore")
    @Operation(summary = "Restore version snapshot", description = "Restores portfolio configuration and sections from a historical version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version restored"),
            @ApiResponse(responseCode = "400", description = "Corrupt snapshot"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Version snapshot not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public ResponseEntity<PortfolioDetailResponse> restoreVersion(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("versionNumber") int versionNumber,
            @Valid @RequestBody RestoreVersionRequest restoreRequest
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        RestoreVersionRequest validatedRequest = new RestoreVersionRequest(versionNumber, restoreRequest.version());
        PortfolioDetailResponse response = portfolioService.restoreVersion(actor, requestedStudioId, validatedRequest);
        return createPrivateNoCacheResponse(response, HttpStatus.OK);
    }

    @GetMapping("/preview")
    @Operation(summary = "Get private portfolio preview", description = "Fetch normalized preview data with strict privacy filters (public consent contacts only).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preview generated"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found")
    })
    public ResponseEntity<PortfolioPreviewResponse> getPortfolioPreview(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        PortfolioPreviewResponse preview = portfolioService.getPortfolioPreview(actor, requestedStudioId);
        return createPrivateNoCacheResponse(preview, HttpStatus.OK);
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
