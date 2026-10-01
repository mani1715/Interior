package com.interior.platform.admin.web;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
import com.interior.platform.admin.service.AdminService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/users")
    @Operation(summary = "List users for platform administration")
    public ResponseEntity<List<AdminUserSummaryDto>> listUsers(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) String status
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listUsers(actor, limit, offset, status));
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

    @GetMapping("/studios")
    @Operation(summary = "List studios for platform administration")
    public ResponseEntity<List<AdminStudioSummaryDto>> listStudios(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) String status
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listStudios(actor, limit, offset, status));
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

    @GetMapping("/audit-logs")
    @Operation(summary = "List admin audit log entries")
    public ResponseEntity<List<AdminAuditLogDto>> listAuditLogs(
            HttpServletRequest request,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "action", required = false) String action
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(adminService.listAuditLogs(actor, limit, offset, action));
    }
}
