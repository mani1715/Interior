package com.interior.platform.verification.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.verification.domain.VerificationDocumentType;
import com.interior.platform.verification.dto.*;
import com.interior.platform.verification.service.StudioVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/verification")
@Tag(name = "Studio Verification", description = "Endpoints for professional studio business verification submission and private document evidence management")
public class StudioVerificationController {

    private final StudioVerificationService verificationService;

    public StudioVerificationController(StudioVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/studio")
    @Operation(summary = "Get studio verification status", description = "Returns verification status, submitted business details, private documents list, and audit events.")
    public ResponseEntity<StudioVerificationDto> getVerification(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam, actor);

        StudioVerificationDto dto = verificationService.getStudioVerification(actor, studioId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(dto);
    }

    @PostMapping("/studio/submit")
    @Operation(summary = "Submit studio verification request", description = "Submits business identity details (registration, GST, domain) for platform verification.")
    public ResponseEntity<StudioVerificationDto> submitVerification(
            HttpServletRequest request,
            @Valid @RequestBody SubmitVerificationRequest body,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam, actor);

        StudioVerificationDto dto = verificationService.submitVerification(actor, studioId, body);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(dto);
    }

    @PostMapping(value = "/studio/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload private verification evidence document", description = "Uploads a PDF, JPEG, or PNG business document. Strictly private storage; never exposed on public CDN.")
    public ResponseEntity<VerificationDocumentDto> uploadDocument(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") VerificationDocumentType documentType,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) throws IOException {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam, actor);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        VerificationDocumentDto doc = verificationService.uploadDocument(
                actor,
                studioId,
                documentType,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(doc);
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private UUID resolveRequestedStudioId(String header, UUID param, ActorContext actor) {
        if (param != null) return param;
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (actor != null && actor.activeStudioId() != null) {
            return actor.activeStudioId();
        }
        return null;
    }
}
