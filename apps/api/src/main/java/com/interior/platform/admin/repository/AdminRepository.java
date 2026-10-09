package com.interior.platform.admin.repository;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminRepository {
    AdminDashboardMetrics getDashboardMetrics();

    List<AdminUserSummaryDto> listUsers(int limit, int offset, String statusFilter);
    List<AdminUserSummaryDto> listUsers(int limit, int offset, String statusFilter, String query);
    Optional<AdminUserDetailDto> getUserDetail(UUID userId);
    void updateUserStatus(UUID userId, String status);
    void updateUserStatus(UUID userId, String status, String reason);
    long countActiveSuperAdmins();
    void updateUserRole(UUID userId, String roleCode);
    void revokeUserSessions(UUID userId);

    List<AdminStudioSummaryDto> listStudios(int limit, int offset, String statusFilter);
    List<AdminStudioSummaryDto> listStudios(int limit, int offset, String statusFilter, String query);
    Optional<AdminStudioDetailDto> getStudioDetail(UUID studioId);
    void updateStudioStatus(UUID studioId, String status);
    void updateStudioStatus(UUID studioId, String status, String reason);
    void updateStudioPlan(UUID studioId, String planCode);

    List<AdminVerificationSummaryDto> listVerificationRequests(int limit, int offset, String statusFilter);

    List<AdminReviewSummaryDto> listReviewsForModeration(int limit, int offset, String statusFilter);
    void updateReviewStatus(UUID reviewId, String status);

    List<AdminProjectSummaryDto> listProjectsForModeration(int limit, int offset, String moderationStatus, String query);
    void updateProjectModerationStatus(UUID projectId, String moderationStatus, String reason);

    List<AdminAuditLogDto> listAuditLogs(int limit, int offset, String actionFilter);
    List<AdminAuditLogDto> listAuditLogs(int limit, int offset, String actionFilter, String resourceTypeFilter);

    List<AdminCommunicationDeliveryDto> listCommunicationDeliveries(int limit, int offset, String statusFilter, String channelFilter);
    void updateCommunicationDeliveryStatus(UUID deliveryId, String status, int attemptCount, String lastError);

    AdminMediaDiagnosticsDto getMediaDiagnostics();
    AdminAiDiagnosticsDto getAiDiagnostics();
}
