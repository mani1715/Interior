package com.interior.platform.privacy.web;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.privacy.dto.DataExportResponse;
import com.interior.platform.privacy.dto.PrivacyRequestResponse;
import com.interior.platform.privacy.service.PrivacyExportService;
import com.interior.platform.privacy.service.PrivacyService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/privacy", "/api/v1/privacy"})
@Tag(name = "Privacy & Data Rights", description = "User data export, studio data export, and legal privacy controls")
public class PrivacyController {

    private final PrivacyService privacyService;
    private final PrivacyExportService privacyExportService;

    public PrivacyController(PrivacyService privacyService, PrivacyExportService privacyExportService) {
        this.privacyService = privacyService;
        this.privacyExportService = privacyExportService;
    }

    private ActorContext getActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return actor;
    }

    @GetMapping("/requests")
    @Operation(summary = "List privacy requests", description = "Retrieves current user's privacy requests history")
    public ResponseEntity<List<PrivacyRequestResponse>> getMyRequests(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        return ResponseEntity.ok(privacyService.getRequestsForUser(actor.userId()));
    }

    @PostMapping("/export/user")
    @Operation(summary = "Trigger user data export", description = "Creates a secure package containing personal profile, inquiries, and collections")
    public ResponseEntity<DataExportResponse> exportUserData(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        DataExportResponse response = privacyExportService.triggerUserExport(actor.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/export/studio/{studioId}")
    @Operation(summary = "Trigger studio data export", description = "Creates a secure package containing studio business data, projects, and team members")
    public ResponseEntity<DataExportResponse> exportStudioData(
            @PathVariable UUID studioId,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        if (actor.activeStudioId() == null || !actor.activeStudioId().equals(studioId)) {
            throw new AccessDeniedException("Active studio membership required");
        }
        DataExportResponse response = privacyExportService.triggerStudioExport(studioId, actor.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/download/{jobId}")
    @Operation(summary = "Download data export package", description = "Downloads compiled ZIP package via one-time secure download token")
    public ResponseEntity<byte[]> downloadExport(
            @PathVariable UUID jobId,
            @RequestParam String token
    ) {
        byte[] packageBytes = privacyExportService.downloadExport(jobId, token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"elegance-data-export-" + jobId + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(packageBytes);
    }

    @PostMapping("/close-account")
    @Operation(summary = "Self-service account closure", description = "Immediately suspends account, revokes active sessions, and records closure request")
    public ResponseEntity<PrivacyRequestResponse> closeAccount(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        PrivacyRequestResponse response = privacyService.closeAccount(actor.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-deletion")
    @Operation(summary = "Execute account deletion", description = "Anonymizes identity, purges credentials and personal collections")
    public ResponseEntity<PrivacyRequestResponse> deleteAccount(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        PrivacyRequestResponse response = privacyService.deleteAccount(actor.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/studio/{studioId}/close")
    @Operation(summary = "Close designer studio", description = "Suspends studio publication, archives active projects, and unpublishes public profile")
    public ResponseEntity<PrivacyRequestResponse> closeStudio(
            @PathVariable UUID studioId,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        if (actor.activeStudioId() == null || !actor.activeStudioId().equals(studioId)) {
            throw new AccessDeniedException("Active studio membership required");
        }
        PrivacyRequestResponse response = privacyService.closeStudio(studioId, actor.userId());
        return ResponseEntity.ok(response);
    }
}
