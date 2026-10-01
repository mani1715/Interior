package com.interior.platform.admin.repository;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;

import java.util.List;
import java.util.UUID;

public interface AdminRepository {
    AdminDashboardMetrics getDashboardMetrics();

    List<AdminUserSummaryDto> listUsers(int limit, int offset, String statusFilter);
    void updateUserStatus(UUID userId, String status);

    List<AdminStudioSummaryDto> listStudios(int limit, int offset, String statusFilter);
    void updateStudioStatus(UUID studioId, String status);

    List<AdminVerificationSummaryDto> listVerificationRequests(int limit, int offset, String statusFilter);

    List<AdminReviewSummaryDto> listReviewsForModeration(int limit, int offset, String statusFilter);
    void updateReviewStatus(UUID reviewId, String status);

    List<AdminAuditLogDto> listAuditLogs(int limit, int offset, String actionFilter);
}
