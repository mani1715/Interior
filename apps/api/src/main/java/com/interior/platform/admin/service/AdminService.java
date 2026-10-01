package com.interior.platform.admin.service;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
import com.interior.platform.admin.repository.AdminRepository;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;

    public AdminService(
            AdminRepository adminRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            RateLimiterService rateLimiterService
    ) {
        this.adminRepository = adminRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
    }

    private void requireAdmin(ActorContext actor) {
        authorizationService.requirePlatformRole(actor, "ADMIN");
    }

    private void requireModeratorOrAdmin(ActorContext actor) {
        authorizationService.requireAuthenticated(actor);
        if (!actor.hasRole("ADMIN") && !actor.hasRole("SUPER_ADMIN") && !actor.hasRole("MODERATOR")) {
            throw new AccessDeniedException("Platform administrator or moderator privileges required");
        }
    }

    public AdminDashboardMetrics getDashboardMetrics(ActorContext actor) {
        requireAdmin(actor);
        return adminRepository.getDashboardMetrics();
    }

    public List<AdminUserSummaryDto> listUsers(ActorContext actor, int limit, int offset, String statusFilter) {
        requireAdmin(actor);
        return adminRepository.listUsers(limit, offset, statusFilter);
    }

    @Transactional
    public void updateUserStatus(ActorContext actor, UUID userId, UpdateUserStatusRequest req, String ip, String ua) {
        requireAdmin(actor);
        rateLimiterService.acquire("admin:user_status:" + actor.userId(), 30, Duration.ofMinutes(1));

        adminRepository.updateUserStatus(userId, req.status());

        auditService.record(
                actor.userId(),
                null,
                "USER_STATUS_CHANGED",
                "USER",
                userId != null ? userId.toString() : null,
                Map.of("newStatus", req.status(), "reason", req.reason() != null ? req.reason() : ""),
                ip,
                ua
        );
    }

    public List<AdminStudioSummaryDto> listStudios(ActorContext actor, int limit, int offset, String statusFilter) {
        requireAdmin(actor);
        return adminRepository.listStudios(limit, offset, statusFilter);
    }

    @Transactional
    public void updateStudioStatus(ActorContext actor, UUID studioId, UpdateStudioStatusRequest req, String ip, String ua) {
        requireAdmin(actor);
        rateLimiterService.acquire("admin:studio_status:" + actor.userId(), 30, Duration.ofMinutes(1));

        adminRepository.updateStudioStatus(studioId, req.status());

        auditService.record(
                actor.userId(),
                studioId,
                "STUDIO_STATUS_CHANGED",
                "STUDIO",
                studioId != null ? studioId.toString() : null,
                Map.of("newStatus", req.status(), "reason", req.reason() != null ? req.reason() : ""),
                ip,
                ua
        );
    }

    public List<AdminVerificationSummaryDto> listVerificationRequests(ActorContext actor, int limit, int offset, String statusFilter) {
        requireAdmin(actor);
        return adminRepository.listVerificationRequests(limit, offset, statusFilter);
    }

    public List<AdminReviewSummaryDto> listReviewsForModeration(ActorContext actor, int limit, int offset, String statusFilter) {
        requireModeratorOrAdmin(actor);
        return adminRepository.listReviewsForModeration(limit, offset, statusFilter);
    }

    @Transactional
    public void moderateReview(ActorContext actor, UUID reviewId, ModerateReviewRequest req, String ip, String ua) {
        requireModeratorOrAdmin(actor);
        rateLimiterService.acquire("admin:review_moderation:" + actor.userId(), 30, Duration.ofMinutes(1));

        adminRepository.updateReviewStatus(reviewId, req.status());

        auditService.record(
                actor.userId(),
                null,
                "REVIEW_MODERATION",
                "REVIEW",
                reviewId != null ? reviewId.toString() : null,
                Map.of("newStatus", req.status(), "reason", req.reason() != null ? req.reason() : ""),
                ip,
                ua
        );
    }

    public List<AdminAuditLogDto> listAuditLogs(ActorContext actor, int limit, int offset, String actionFilter) {
        requireAdmin(actor);
        return adminRepository.listAuditLogs(limit, offset, actionFilter);
    }
}
