package com.interior.platform.admin.web;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
import com.interior.platform.admin.service.AdminService;
import com.interior.platform.media.dto.StorageReconciliationReport;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@Tag(name = "Platform Administration", description = "Endpoints for platform administrators and content moderators")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform operational metrics", description = "Aggregated counts of users, studios, projects, verifications, and recent audit activity.")
    public ResponseEntity<AdminDashboardMetrics> getDashboard(HttpServletRequest request) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.getDashboardMetrics(actor));
    }

    // --- Users ---

    @GetMapping("/users")
    @Operation(summary = "List users for platform administration")
    public ResponseEntity<List<AdminUserSummaryDto>> listUsers(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "query", required = false) String query
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listUsers(actor, limit, offset, status, query));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get detailed user metadata")
    public ResponseEntity<AdminUserDetailDto> getUserDetail(
            HttpServletRequest request,
            @PathVariable("userId") UUID userId
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.getUserDetail(actor, userId));
    }

    @PostMapping("/users/{userId}/status")
    @Operation(summary = "Update user status (ACTIVE, SUSPENDED, DELETED)")
    public ResponseEntity<Void> updateUserStatus(
            HttpServletRequest request,
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest req
    ) {
        ActorContext actor = extractActor(request);
        adminService.updateUserStatus(actor, userId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/role")
    @Operation(summary = "Modify user platform role (SUPER_ADMIN only)")
    public ResponseEntity<Void> updateUserRole(
            HttpServletRequest request,
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest req
    ) {
        ActorContext actor = extractActor(request);
        adminService.updateUserRole(actor, userId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/revoke-sessions")
    @Operation(summary = "Revoke all active sessions for user (SUPER_ADMIN only)")
    public ResponseEntity<Void> revokeUserSessions(
            HttpServletRequest request,
            @PathVariable("userId") UUID userId
    ) {
        ActorContext actor = extractActor(request);
        adminService.revokeUserSessions(actor, userId, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    // --- Studios ---

    @GetMapping("/studios")
    @Operation(summary = "List studios for platform administration")
    public ResponseEntity<List<AdminStudioSummaryDto>> listStudios(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "query", required = false) String query
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listStudios(actor, limit, offset, status, query));
    }

    @GetMapping("/studios/{studioId}")
    @Operation(summary = "Get detailed studio operational profile")
    public ResponseEntity<AdminStudioDetailDto> getStudioDetail(
            HttpServletRequest request,
            @PathVariable("studioId") UUID studioId
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.getStudioDetail(actor, studioId));
    }

    @PostMapping("/studios/{studioId}/status")
    @Operation(summary = "Update studio status (ACTIVE, SUSPENDED)")
    public ResponseEntity<Void> updateStudioStatus(
            HttpServletRequest request,
            @PathVariable("studioId") UUID studioId,
            @Valid @RequestBody UpdateStudioStatusRequest req
    ) {
        ActorContext actor = extractActor(request);
        adminService.updateStudioStatus(actor, studioId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/studios/{studioId}/plan")
    @Operation(summary = "Manually update studio subscription plan (SUPER_ADMIN only)")
    public ResponseEntity<Void> updateStudioPlan(
            HttpServletRequest request,
            @PathVariable("studioId") UUID studioId,
            @Valid @RequestBody UpdateStudioPlanRequest req
    ) {
        ActorContext actor = extractActor(request);
        adminService.updateStudioPlan(actor, studioId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    // --- Verification ---

    @GetMapping("/verification")
    @Operation(summary = "List verification requests for review")
    public ResponseEntity<List<AdminVerificationSummaryDto>> listVerificationRequests(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) String status
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listVerificationRequests(actor, limit, offset, status));
    }

    // --- Reviews ---

    @GetMapping("/reviews")
    @Operation(summary = "List reviews for moderation")
    public ResponseEntity<List<AdminReviewSummaryDto>> listReviews(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) String status
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listReviewsForModeration(actor, limit, offset, status));
    }

    @PostMapping("/reviews/{reviewId}/status")
    @Operation(summary = "Moderate review status (PUBLISHED, FLAGGED, REMOVED)")
    public ResponseEntity<Void> moderateReview(
            HttpServletRequest request,
            @PathVariable("reviewId") UUID reviewId,
            @Valid @RequestBody ModerateReviewRequest req
    ) {
        ActorContext actor = extractActor(request);
        adminService.moderateReview(actor, reviewId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    // --- Projects ---

    @GetMapping("/projects")
    @Operation(summary = "List projects for content moderation")
    public ResponseEntity<List<AdminProjectSummaryDto>> listProjects(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "moderationStatus", required = false) String moderationStatus,
            @RequestParam(name = "query", required = false) String query
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listProjectsForModeration(actor, limit, offset, moderationStatus, query));
    }

    @PostMapping("/projects/{projectId}/moderation")
    @Operation(summary = "Moderate project status (APPROVED, FLAGGED, HIDDEN)")
    public ResponseEntity<Void> moderateProject(
            HttpServletRequest request,
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody ModerateProjectRequest req
    ) {
        ActorContext actor = extractActor(request);
        adminService.moderateProject(actor, projectId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    // --- Audit Logs ---

    @GetMapping("/audit-logs")
    @Operation(summary = "List admin audit log entries")
    public ResponseEntity<List<AdminAuditLogDto>> listAuditLogs(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "resourceType", required = false) String resourceType
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listAuditLogs(actor, limit, offset, action, resourceType));
    }

    // --- Operational Control Plane ---

    @GetMapping("/operations/health")
    @Operation(summary = "Get safe operational infrastructure health summary")
    public ResponseEntity<OperationalHealthDto> getHealth(HttpServletRequest request) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.getOperationalHealth(actor));
    }

    @GetMapping("/operations/communications")
    @Operation(summary = "List communication deliveries with masked recipient PII")
    public ResponseEntity<List<AdminCommunicationDeliveryDto>> listCommunications(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "channel", required = false) String channel
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listCommunicationDeliveries(actor, limit, offset, status, channel));
    }

    @PostMapping("/operations/communications/{deliveryId}/retry")
    @Operation(summary = "Retry a transiently failed communication delivery")
    public ResponseEntity<Void> retryCommunication(
            HttpServletRequest request,
            @PathVariable("deliveryId") UUID deliveryId
    ) {
        ActorContext actor = extractActor(request);
        adminService.retryCommunicationDelivery(actor, deliveryId, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/operations/media")
    @Operation(summary = "Get media storage diagnostics")
    public ResponseEntity<AdminMediaDiagnosticsDto> getMediaDiagnostics(HttpServletRequest request) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.getMediaDiagnostics(actor));
    }

    @PostMapping("/operations/media/reconcile")
    @Operation(summary = "Trigger media storage reconciliation for a studio")
    public ResponseEntity<StorageReconciliationReport> reconcileMedia(
            HttpServletRequest request,
            @RequestParam(name = "studioId") UUID studioId
    ) {
        ActorContext actor = extractActor(request);
        StorageReconciliationReport report = adminService.reconcileMediaStorage(actor, studioId, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/operations/ai")
    @Operation(summary = "Get AI jobs operational diagnostics")
    public ResponseEntity<AdminAiDiagnosticsDto> getAiDiagnostics(HttpServletRequest request) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.getAiDiagnostics(actor));
    }

    @PostMapping("/operations/ai/reconcile")
    @Operation(summary = "Reconcile stuck AI processing jobs")
    public ResponseEntity<Map<String, Object>> reconcileAi(HttpServletRequest request) {
        ActorContext actor = extractActor(request);
        int recovered = adminService.reconcileStuckAiJobs(actor, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("recoveredCount", recovered));
    }
}
