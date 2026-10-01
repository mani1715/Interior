package com.interior.platform.admin.domain;

import com.interior.platform.admin.dto.AdminAuditLogDto;
import java.util.List;

public record AdminDashboardMetrics(
        long totalStudios,
        long activeStudios,
        long suspendedStudios,
        long publishedStudios,
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long totalProjects,
        long publicProjects,
        long totalLeads,
        long pendingVerifications,
        long pendingReviewReports,
        long totalMediaAssets,
        long totalAiGenerations,
        List<AdminAuditLogDto> recentActivity
) {}
