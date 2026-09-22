package com.interior.platform.ai.web;

import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.EditingMode;
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
    // PRECISION MASK EDITING ENDPOINTS
    // ============================================================================

    @PostMapping(value = "/masks", consumes = "multipart/form-data")
    @Operation(summary = "Upload precision editing mask", description = "Uploads a client-generated region selection mask PNG for a specific input media asset.")
    public ResponseEntity<UploadMaskResponse> uploadMask(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @RequestParam("inputMediaId") UUID inputMediaId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        UploadMaskResponse res = aiVisualizerService.uploadMask(actor, studioId, inputMediaId, file);
        return createPrivateNoCacheResponse(res, HttpStatus.CREATED);
    }

    @GetMapping(value = "/jobs/{jobId}/mask", produces = "image/png")
    @Operation(summary = "Get precision editing mask preview", description = "Fetches the raw private PNG mask for a precision editing job.")
    public ResponseEntity<byte[]> getJobMask(
            HttpServletRequest request,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        byte[] maskBytes = aiVisualizerService.getJobMask(actor, studioId, jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_TYPE, "image/png")
                .body(maskBytes);
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

    // ============================================================================
    // VARIATIONS & HISTORY ENDPOINTS
    // ============================================================================

    @PostMapping("/jobs/{jobId}/variations")
    @Operation(summary = "Create variation from existing AI job", description = "Generates a variation of an existing concept (refine original or evolve concept).")
    public ResponseEntity<AiJobDetailResponse> createVariation(
            HttpServletRequest request,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateVariationRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobDetailResponse res = aiVisualizerService.createVariation(actor, studioId, jobId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.ACCEPTED);
    }

    @PostMapping("/jobs/{jobId}/shortlist")
    @Operation(summary = "Toggle concept shortlist status", description = "Shortlists or removes concept from shortlist.")
    public ResponseEntity<AiJobDetailResponse> toggleShortlist(
            HttpServletRequest request,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobDetailResponse res = aiVisualizerService.toggleShortlist(actor, studioId, jobId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @PostMapping("/jobs/{jobId}/select")
    @Operation(summary = "Toggle studio selection status", description = "Marks or unmarks concept as the studio's primary selection.")
    public ResponseEntity<AiJobDetailResponse> toggleStudioSelected(
            HttpServletRequest request,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobDetailResponse res = aiVisualizerService.toggleStudioSelected(actor, studioId, jobId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @GetMapping("/history")
    @Operation(summary = "List AI generation history", description = "Returns filterable paginated history of AI jobs including lineage and shortlist status.")
    public ResponseEntity<AiJobHistoryResponse> listHistory(
            HttpServletRequest request,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "editingMode", required = false) EditingMode editingMode,
            @RequestParam(value = "status", required = false) AiJobStatus status,
            @RequestParam(value = "shortlistedOnly", required = false) Boolean shortlistedOnly,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        AiJobHistoryResponse res = aiVisualizerService.listJobHistory(actor, studioId, projectId, editingMode, status, shortlistedOnly, page, limit);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    // ============================================================================
    // CLIENT REVIEW STUDIO ENDPOINTS
    // ============================================================================

    @PostMapping("/client-reviews")
    @Operation(summary = "Create client review link", description = "Packages shortlisted AI concepts into a shareable client review link.")
    public ResponseEntity<CreateClientReviewResponse> createClientReview(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateClientReviewRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        CreateClientReviewResponse res = aiVisualizerService.createClientReview(actor, studioId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.CREATED);
    }

    @GetMapping("/client-reviews")
    @Operation(summary = "List studio client reviews", description = "Returns client review links created for the studio or project.")
    public ResponseEntity<List<ClientReviewDetailResponse>> listClientReviews(
            HttpServletRequest request,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        List<ClientReviewDetailResponse> res = aiVisualizerService.listStudioReviews(actor, studioId, projectId, page, limit);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @GetMapping("/client-reviews/{reviewId}")
    @Operation(summary = "Get studio client review detail", description = "Fetches review details including all items, decisions, and comments.")
    public ResponseEntity<ClientReviewDetailResponse> getClientReviewDetail(
            HttpServletRequest request,
            @PathVariable("reviewId") UUID reviewId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        ClientReviewDetailResponse res = aiVisualizerService.getStudioReviewDetail(actor, studioId, reviewId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @PostMapping("/client-reviews/{reviewId}/close")
    @Operation(summary = "Close client review", description = "Closes review link, preventing further decisions or comments.")
    public ResponseEntity<Void> closeReview(
            HttpServletRequest request,
            @PathVariable("reviewId") UUID reviewId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        aiVisualizerService.closeReview(actor, studioId, reviewId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    @PostMapping("/client-reviews/{reviewId}/revoke")
    @Operation(summary = "Revoke client review", description = "Revokes review link and invalidates all active sessions immediately.")
    public ResponseEntity<Void> revokeReview(
            HttpServletRequest request,
            @PathVariable("reviewId") UUID reviewId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        aiVisualizerService.revokeReview(actor, studioId, reviewId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    @PostMapping("/client-reviews/{reviewId}/rotate-token")
    @Operation(summary = "Rotate client review token", description = "Generates a new access token for the review and invalidates previous sessions.")
    public ResponseEntity<CreateClientReviewResponse> rotateReviewToken(
            HttpServletRequest request,
            @PathVariable("reviewId") UUID reviewId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        CreateClientReviewResponse res = aiVisualizerService.rotateReviewToken(actor, studioId, reviewId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
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
