package com.interior.platform.ai.web;

import com.interior.platform.ai.domain.ReferencePurpose;
import com.interior.platform.ai.dto.*;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI Visualizer", description = "Endpoints for AI concept generation, status polling, and visualizer workflows")
public class AiController {

    private final AiVisualizerService aiVisualizerService;

    public AiController(AiVisualizerService aiVisualizerService) {
        this.aiVisualizerService = aiVisualizerService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get studio AI status and quota", description = "Checks whether AI provider is configured and returns today's usage/quota.")
    public ResponseEntity<AiStudioStatusResponse> getStudioStatus(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiStudioStatusResponse res = aiVisualizerService.getStudioStatus(actor, studioId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @PostMapping("/jobs")
    @Operation(summary = "Submit AI concept generation job", description = "Creates a durable queued job to transform an unfinished room image into an AI concept.")
    public ResponseEntity<AiJobDetailResponse> createGenerationJob(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateAiJobRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobDetailResponse res = aiVisualizerService.createGenerationJob(actor, studioId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.ACCEPTED);
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get AI job detail", description = "Polls or fetches progress and status of a generation job.")
    public ResponseEntity<AiJobDetailResponse> getJobDetail(
            HttpServletRequest request,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobDetailResponse res = aiVisualizerService.getJobDetail(actor, studioId, jobId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @Operation(summary = "Cancel AI job", description = "Cancels a pending or processing AI concept generation job.")
    public ResponseEntity<AiJobDetailResponse> cancelJob(
            HttpServletRequest request,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobDetailResponse res = aiVisualizerService.cancelJob(actor, studioId, jobId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @GetMapping("/jobs")
    @Operation(summary = "List studio AI jobs", description = "Returns paginated list of recent AI generation jobs for the studio.")
    public ResponseEntity<AiJobListResponse> listJobs(
            HttpServletRequest request,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobListResponse res = aiVisualizerService.listJobs(actor, studioId, projectId, limit, offset);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    // ============================================================================
    // REFERENCE LIBRARY ENDPOINTS
    // ============================================================================

    @PostMapping("/references")
    @Operation(summary = "Create reference library entry", description = "Registers an uploaded reference media asset into the studio reference library.")
    public ResponseEntity<ReferenceDetailResponse> createReference(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateReferenceRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        ReferenceDetailResponse res = aiVisualizerService.createReference(actor, studioId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.CREATED);
    }

    @GetMapping("/references")
    @Operation(summary = "List reference library entries", description = "Fetches reference library items for the studio, optionally filtered by project or purpose.")
    public ResponseEntity<List<ReferenceDetailResponse>> listReferences(
            HttpServletRequest request,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "purpose", required = false) ReferencePurpose purpose,
            @RequestParam(value = "includeArchived", required = false, defaultValue = "false") boolean includeArchived,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        List<ReferenceDetailResponse> res = aiVisualizerService.listReferences(actor, studioId, projectId, purpose, includeArchived);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @GetMapping("/references/{referenceId}")
    @Operation(summary = "Get reference library entry", description = "Fetches a single reference entry by ID.")
    public ResponseEntity<ReferenceDetailResponse> getReference(
            HttpServletRequest request,
            @PathVariable("referenceId") UUID referenceId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        ReferenceDetailResponse res = aiVisualizerService.getReference(actor, studioId, referenceId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @PatchMapping("/references/{referenceId}")
    @Operation(summary = "Update reference library entry", description = "Updates purpose, label, default instructions, or associated project.")
    public ResponseEntity<ReferenceDetailResponse> updateReference(
            HttpServletRequest request,
            @PathVariable("referenceId") UUID referenceId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody UpdateReferenceRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        ReferenceDetailResponse res = aiVisualizerService.updateReference(actor, studioId, referenceId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @DeleteMapping("/references/{referenceId}")
    @Operation(summary = "Archive reference library entry", description = "Archives a reference item without modifying any historical job snapshots.")
    public ResponseEntity<Void> archiveReference(
            HttpServletRequest request,
            @PathVariable("referenceId") UUID referenceId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        aiVisualizerService.archiveReference(actor, studioId, referenceId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
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
