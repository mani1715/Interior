package com.interior.platform.media.web;

import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.dto.*;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.media.storage.StorageService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/media")
@Tag(name = "Media Engine", description = "Endpoints for direct storage upload intents, processing, derivatives, watermarks, and media library")
public class MediaController {

    private final MediaService mediaService;
    private final StorageService storageService;

    public MediaController(MediaService mediaService, StorageService storageService) {
        this.mediaService = mediaService;
        this.storageService = storageService;
    }

    @PostMapping("/upload-intent")
    @Operation(summary = "Create upload intent", description = "Initializes an upload session and generates a quarantine storage key for direct client upload.")
    public ResponseEntity<UploadIntentResponse> createUploadIntent(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateUploadIntentRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        UploadIntentResponse res = mediaService.createUploadIntent(actor, studioId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.CREATED);
    }

    @PutMapping("/upload/{uploadIntentId}")
    @Operation(summary = "Direct upload to quarantine", description = "Local/dev direct upload target for upload intents.")
    public ResponseEntity<Map<String, Object>> uploadQuarantine(
            @PathVariable("uploadIntentId") UUID uploadIntentId,
            @RequestBody byte[] content,
            @RequestHeader(value = "Content-Type", required = false) String contentType
    ) {
        mediaService.handleQuarantineUpload(uploadIntentId, content, contentType);
        return ResponseEntity.ok(Map.of("status", "uploaded", "uploadIntentId", uploadIntentId));
    }

    @PostMapping("/commit")
    @Operation(summary = "Commit uploaded media", description = "Verifies quarantined file, promotes to canonical private original, and generates responsive watermarked derivatives.")
    public ResponseEntity<MediaDetailResponse> commitUpload(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CommitUploadRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        MediaDetailResponse res = mediaService.commitUpload(actor, studioId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List studio media", description = "Lists all media assets for the active studio with optional filtering.")
    public ResponseEntity<List<MediaDetailResponse>> listStudioMedia(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "mediaType", required = false) MediaType mediaType,
            @RequestParam(value = "visibility", required = false) MediaVisibility visibility,
            @RequestParam(value = "status", required = false) MediaProcessingStatus status
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        List<MediaDetailResponse> list = mediaService.listStudioMedia(actor, studioId, projectId, mediaType, visibility, status);
        return createPrivateNoCacheResponse(list, HttpStatus.OK);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List project media", description = "Lists media assets associated with a specific project.")
    public ResponseEntity<List<MediaDetailResponse>> listProjectMedia(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("projectId") UUID projectId
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        List<MediaDetailResponse> list = mediaService.listProjectMedia(actor, studioId, projectId);
        return createPrivateNoCacheResponse(list, HttpStatus.OK);
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details", description = "Fetches metadata and derivative links for a media asset.")
    public ResponseEntity<MediaDetailResponse> getMedia(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("mediaId") UUID mediaId
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        MediaDetailResponse res = mediaService.getMedia(actor, studioId, mediaId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @PatchMapping("/{mediaId}")
    @Operation(summary = "Update media metadata", description = "Updates caption, alt text, cover status, visibility, and watermark preference.")
    public ResponseEntity<MediaDetailResponse> updateMedia(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("mediaId") UUID mediaId,
            @Valid @RequestBody UpdateMediaRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        MediaDetailResponse res = mediaService.updateMedia(actor, studioId, mediaId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media asset", description = "Soft-deletes a media asset and revokes public derivative access.")
    public ResponseEntity<Void> deleteMedia(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("mediaId") UUID mediaId
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        mediaService.deleteMedia(actor, studioId, mediaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/watermark-settings")
    @Operation(summary = "Get studio watermark settings", description = "Retrieves position, opacity, logo, and fallback text configuration.")
    public ResponseEntity<WatermarkSettingsResponse> getWatermarkSettings(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        WatermarkSettingsResponse res = mediaService.getWatermarkSettings(actor, studioId);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @PutMapping("/watermark-settings")
    @Operation(summary = "Update studio watermark settings", description = "Updates studio watermark configuration.")
    public ResponseEntity<WatermarkSettingsResponse> updateWatermarkSettings(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody WatermarkSettingsRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        WatermarkSettingsResponse res = mediaService.updateWatermarkSettings(actor, studioId, req);
        return createPrivateNoCacheResponse(res, HttpStatus.OK);
    }

    @GetMapping("/{mediaId}/preview")
    @Operation(summary = "Authenticated workspace media preview", description = "Serves private authenticated preview with burned-in disclosure/watermark. Never exposes original raw master.")
    public ResponseEntity<byte[]> getMediaPreview(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @PathVariable("mediaId") UUID mediaId
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);
        byte[] bytes = mediaService.getAuthenticatedMediaPreview(actor, studioId, mediaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    @GetMapping("/public/**")
    @Operation(summary = "Public derivative image CDN delivery", description = "Serves public optimized, watermarked derivatives with long-term immutable caching.")
    public ResponseEntity<byte[]> getPublicDerivative(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        // Strip prefix up to /media/public/
        int idx = fullPath.indexOf("/media/public/");
        if (idx == -1) {
            return ResponseEntity.notFound().build();
        }
        String storageKey = fullPath.substring(idx + "/media/public/".length());

        // Defense-in-depth: only keys starting with "public/" are served
        if (!storageKey.startsWith("public/")) {
            storageKey = "public/" + storageKey;
        }

        if (!mediaService.isDerivativePubliclyDeliverable(storageKey)) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = storageService.load(storageKey);
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.http.MediaType contentType = org.springframework.http.MediaType.IMAGE_JPEG;
        if (storageKey.endsWith(".png")) {
            contentType = org.springframework.http.MediaType.IMAGE_PNG;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentType(contentType)
                .body(bytes);
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
