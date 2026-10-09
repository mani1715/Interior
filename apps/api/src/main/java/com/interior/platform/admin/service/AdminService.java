package com.interior.platform.admin.service;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
import com.interior.platform.admin.repository.AdminRepository;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.repository.CommunicationDeliveryRepository;
import com.interior.platform.email.service.TransactionalEmailService;
import com.interior.platform.media.dto.StorageReconciliationReport;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;
    private final SecurityRepository securityRepository;
    private final MediaService mediaService;
    private final AiVisualizerService aiVisualizerService;
    private final CommunicationDeliveryRepository communicationDeliveryRepository;
    private final TransactionalEmailService transactionalEmailService;
    private final JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminService(
            AdminRepository adminRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            RateLimiterService rateLimiterService,
            SecurityRepository securityRepository,
            MediaService mediaService,
            AiVisualizerService aiVisualizerService,
            CommunicationDeliveryRepository communicationDeliveryRepository,
            TransactionalEmailService transactionalEmailService,
            JdbcTemplate jdbcTemplate
    ) {
        this.adminRepository = adminRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
        this.securityRepository = securityRepository;
        this.mediaService = mediaService;
        this.aiVisualizerService = aiVisualizerService;
        this.communicationDeliveryRepository = communicationDeliveryRepository;
        this.transactionalEmailService = transactionalEmailService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminService(
            AdminRepository adminRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            RateLimiterService rateLimiterService
    ) {
        this(adminRepository, authorizationService, auditService, rateLimiterService, null, null, null, null, null, null);
    }

    private void requireAdmin(ActorContext actor) {
        authorizationService.requirePlatformRole(actor, "ADMIN");
    }

    private void requireSuperAdmin(ActorContext actor) {
        authorizationService.requireSuperAdmin(actor);
    }

    private void requireModeratorOrAdmin(ActorContext actor) {
        authorizationService.requireAuthenticated(actor);
        authorizationService.requireActiveUser(actor);
        if (!actor.hasRole("ADMIN") && !actor.hasRole("SUPER_ADMIN") && !actor.hasRole("MODERATOR")) {
            throw new AccessDeniedException("Platform administrator or moderator privileges required");
        }
    }

    // 1. Dashboard
    public AdminDashboardMetrics getDashboardMetrics(ActorContext actor) {
        requireAdmin(actor);
        return adminRepository.getDashboardMetrics();
    }

    // 2. Users
    public List<AdminUserSummaryDto> listUsers(ActorContext actor, int limit, int offset, String statusFilter, String query) {
        requireAdmin(actor);
        return adminRepository.listUsers(limit, offset, statusFilter, query);
    }

    public List<AdminUserSummaryDto> listUsers(ActorContext actor, int limit, int offset, String statusFilter) {
        return listUsers(actor, limit, offset, statusFilter, null);
    }

    public AdminUserDetailDto getUserDetail(ActorContext actor, UUID userId) {
        requireAdmin(actor);
        return adminRepository.getUserDetail(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public void updateUserStatus(ActorContext actor, UUID userId, UpdateUserStatusRequest req, String ip, String ua) {
        requireAdmin(actor);
        rateLimiterService.acquire("admin:user_status:" + actor.userId(), 30, Duration.ofMinutes(1));

        // Self-suspension protection
        if (actor.userId() != null && actor.userId().equals(userId)) {
            throw new IllegalArgumentException("Cannot suspend own account via administrative console");
        }

        AdminUserDetailDto target = getUserDetail(actor, userId);

        // Only SUPER_ADMIN can suspend another platform administrator
        if (target.roles().contains("ADMIN") || target.roles().contains("SUPER_ADMIN")) {
            requireSuperAdmin(actor);
        }

        // Last SUPER_ADMIN lockout protection
        if (target.roles().contains("SUPER_ADMIN") && !"ACTIVE".equalsIgnoreCase(req.status())) {
            if (adminRepository.countActiveSuperAdmins() <= 1) {
                throw new IllegalStateException("Cannot suspend or delete the final active SUPER_ADMIN");
            }
        }

        adminRepository.updateUserStatus(userId, req.status(), req.reason());

        // Invalidate active sessions immediately upon suspension
        if ("SUSPENDED".equalsIgnoreCase(req.status()) || "DELETED".equalsIgnoreCase(req.status())) {
            securityRepository.revokeAllUserSessions(userId, Instant.now());
        }

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

    @Transactional
    public void updateUserRole(ActorContext actor, UUID userId, UpdateUserRoleRequest req, String ip, String ua) {
        requireSuperAdmin(actor);
        rateLimiterService.acquire("admin:user_role:" + actor.userId(), 20, Duration.ofMinutes(1));

        // Self-role modification protection
        if (actor.userId() != null && actor.userId().equals(userId)) {
            throw new IllegalArgumentException("Cannot alter own platform role");
        }

        AdminUserDetailDto target = getUserDetail(actor, userId);

        // Last SUPER_ADMIN demotion protection
        if (target.roles().contains("SUPER_ADMIN") && !"SUPER_ADMIN".equalsIgnoreCase(req.role())) {
            if (adminRepository.countActiveSuperAdmins() <= 1) {
                throw new IllegalStateException("Cannot demote the final active SUPER_ADMIN");
            }
        }

        adminRepository.updateUserRole(userId, req.role());

        // Revoke active sessions so role permissions re-evaluate upon next login
        securityRepository.revokeAllUserSessions(userId, Instant.now());

        auditService.record(
                actor.userId(),
                null,
                "ADMIN_ROLE_CHANGED",
                "USER",
                userId.toString(),
                Map.of("oldRoles", target.roles(), "newRole", req.role()),
                ip,
                ua
        );
    }

    @Transactional
    public void revokeUserSessions(ActorContext actor, UUID userId, String ip, String ua) {
        requireSuperAdmin(actor);
        rateLimiterService.acquire("admin:revoke_sessions:" + actor.userId(), 20, Duration.ofMinutes(1));

        getUserDetail(actor, userId); // validates existence

        securityRepository.revokeAllUserSessions(userId, Instant.now());

        auditService.record(
                actor.userId(),
                null,
                "SESSION_REVOKED",
                "USER",
                userId.toString(),
                Map.of("revokedAll", true),
                ip,
                ua
        );
    }

    // 3. Studios
    public List<AdminStudioSummaryDto> listStudios(ActorContext actor, int limit, int offset, String statusFilter, String query) {
        requireAdmin(actor);
        return adminRepository.listStudios(limit, offset, statusFilter, query);
    }

    public List<AdminStudioSummaryDto> listStudios(ActorContext actor, int limit, int offset, String statusFilter) {
        return listStudios(actor, limit, offset, statusFilter, null);
    }

    public AdminStudioDetailDto getStudioDetail(ActorContext actor, UUID studioId) {
        requireAdmin(actor);
        return adminRepository.getStudioDetail(studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found: " + studioId));
    }

    @Transactional
    public void updateStudioStatus(ActorContext actor, UUID studioId, UpdateStudioStatusRequest req, String ip, String ua) {
        requireAdmin(actor);
        rateLimiterService.acquire("admin:studio_status:" + actor.userId(), 30, Duration.ofMinutes(1));

        getStudioDetail(actor, studioId); // validates existence

        adminRepository.updateStudioStatus(studioId, req.status(), req.reason());

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

    @Transactional
    public void updateStudioPlan(ActorContext actor, UUID studioId, UpdateStudioPlanRequest req, String ip, String ua) {
        requireSuperAdmin(actor);
        rateLimiterService.acquire("admin:studio_plan:" + actor.userId(), 20, Duration.ofMinutes(1));

        getStudioDetail(actor, studioId); // validates existence

        adminRepository.updateStudioPlan(studioId, req.planCode());

        auditService.record(
                actor.userId(),
                studioId,
                "SUBSCRIPTION_OVERRIDE",
                "STUDIO_SUBSCRIPTION",
                studioId.toString(),
                Map.of("newPlan", req.planCode()),
                ip,
                ua
        );
    }

    // 4. Verifications
    public List<AdminVerificationSummaryDto> listVerificationRequests(ActorContext actor, int limit, int offset, String statusFilter) {
        requireAdmin(actor);
        return adminRepository.listVerificationRequests(limit, offset, statusFilter);
    }

    // 5. Review Moderation
    public List<AdminReviewSummaryDto> listReviewsForModeration(ActorContext actor, int limit, int offset, String statusFilter) {
        requireModeratorOrAdmin(actor);
        return adminRepository.listReviewsForModeration(limit, offset, statusFilter);
    }

    @Transactional
    public void moderateReview(ActorContext actor, UUID reviewId, ModerateReviewRequest req, String ip, String ua) {
        requireModeratorOrAdmin(actor);
        rateLimiterService.acquire("admin:review_moderation:" + actor.userId(), 30, Duration.ofMinutes(1));

        if ("REMOVED".equalsIgnoreCase(req.status()) && (req.reason() == null || req.reason().isBlank())) {
            throw new IllegalArgumentException("Reason is required when removing a review");
        }

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

    // 6. Project Moderation
    public List<AdminProjectSummaryDto> listProjectsForModeration(ActorContext actor, int limit, int offset, String moderationStatus, String query) {
        requireModeratorOrAdmin(actor);
        return adminRepository.listProjectsForModeration(limit, offset, moderationStatus, query);
    }

    @Transactional
    public void moderateProject(ActorContext actor, UUID projectId, ModerateProjectRequest req, String ip, String ua) {
        requireModeratorOrAdmin(actor);
        rateLimiterService.acquire("admin:project_moderation:" + actor.userId(), 30, Duration.ofMinutes(1));

        adminRepository.updateProjectModerationStatus(projectId, req.moderationStatus(), req.reason());

        auditService.record(
                actor.userId(),
                null,
                "PROJECT_MODERATION",
                "STUDIO_PROJECT",
                projectId.toString(),
                Map.of("newStatus", req.moderationStatus(), "reason", req.reason() != null ? req.reason() : ""),
                ip,
                ua
        );
    }

    // 7. Audit Logs
    public List<AdminAuditLogDto> listAuditLogs(ActorContext actor, int limit, int offset, String actionFilter, String resourceTypeFilter) {
        requireAdmin(actor);
        return adminRepository.listAuditLogs(limit, offset, actionFilter, resourceTypeFilter);
    }

    public List<AdminAuditLogDto> listAuditLogs(ActorContext actor, int limit, int offset, String actionFilter) {
        return listAuditLogs(actor, limit, offset, actionFilter, null);
    }

    // 8. Operational Health & Diagnostics
    public OperationalHealthDto getOperationalHealth(ActorContext actor) {
        requireAdmin(actor);

        String dbStatus = "UP";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            dbStatus = "DOWN";
        }

        String emailStatus = transactionalEmailService.isConfigured() ? transactionalEmailService.getProviderName() : "NOT_CONFIGURED";

        return new OperationalHealthDto(
                dbStatus,
                "LOCAL_DISK",
                "DEV_MOCK",
                emailStatus,
                "DIRECT_WAME",
                Instant.now()
        );
    }

    public List<AdminCommunicationDeliveryDto> listCommunicationDeliveries(ActorContext actor, int limit, int offset, String statusFilter, String channelFilter) {
        requireAdmin(actor);
        return adminRepository.listCommunicationDeliveries(limit, offset, statusFilter, channelFilter);
    }

    @Transactional
    public void retryCommunicationDelivery(ActorContext actor, UUID deliveryId, String ip, String ua) {
        requireAdmin(actor);
        rateLimiterService.acquire("admin:comm_retry:" + actor.userId(), 30, Duration.ofMinutes(1));

        CommunicationDeliveryRecord record = communicationDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Communication delivery record not found: " + deliveryId));

        if (record.channel() == DeliveryChannel.EMAIL && !transactionalEmailService.isConfigured()) {
            throw new IllegalStateException("Cannot retry delivery: Email provider is currently NOT_CONFIGURED");
        }

        adminRepository.updateCommunicationDeliveryStatus(deliveryId, "PENDING", record.attemptCount() + 1, null);

        auditService.record(
                actor.userId(),
                record.studioId(),
                "COMMUNICATION_RETRY",
                "COMMUNICATION_DELIVERY",
                deliveryId.toString(),
                Map.of("channel", record.channel().name(), "attemptCount", record.attemptCount() + 1),
                ip,
                ua
        );
    }

    public AdminMediaDiagnosticsDto getMediaDiagnostics(ActorContext actor) {
        requireAdmin(actor);
        return adminRepository.getMediaDiagnostics();
    }

    @Transactional
    public StorageReconciliationReport reconcileMediaStorage(ActorContext actor, UUID studioId, String ip, String ua) {
        requireAdmin(actor);
        rateLimiterService.acquire("admin:media_reconcile:" + actor.userId(), 10, Duration.ofMinutes(1));

        if (studioId == null) {
            throw new BadRequestException("Target studioId is required for media reconciliation");
        }

        StorageReconciliationReport report = mediaService.reconcileStorageForAdmin(actor, studioId);

        auditService.record(
                actor.userId(),
                studioId,
                "MEDIA_RECONCILIATION",
                "MEDIA_STORAGE",
                studioId.toString(),
                Map.of("cleanedQuarantines", report.expiredIntentsCount(), "reclaimedBytes", report.pendingStorageBytes()),
                ip,
                ua
        );

        return report;
    }

    public AdminAiDiagnosticsDto getAiDiagnostics(ActorContext actor) {
        requireAdmin(actor);
        return adminRepository.getAiDiagnostics();
    }

    @Transactional
    public int reconcileStuckAiJobs(ActorContext actor, String ip, String ua) {
        requireAdmin(actor);
        rateLimiterService.acquire("admin:ai_reconcile:" + actor.userId(), 10, Duration.ofMinutes(1));

        int recoveredCount = aiVisualizerService.reconcileStuckProcessingJobs(Duration.ofMinutes(10));

        auditService.record(
                actor.userId(),
                null,
                "AI_RECONCILIATION",
                "AI_JOBS",
                "PLATFORM",
                Map.of("recoveredCount", recoveredCount),
                ip,
                ua
        );

        return recoveredCount;
    }
}
