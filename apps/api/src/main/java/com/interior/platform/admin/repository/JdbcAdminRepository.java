package com.interior.platform.admin.repository;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcAdminRepository implements AdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object obj = rs.getObject(column);
        if (obj == null) return null;
        if (obj instanceof UUID u) return u;
        if (obj instanceof String s) return UUID.fromString(s);
        return UUID.fromString(obj.toString());
    }

    private Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }

    private long countQuery(String sql) {
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class);
            return val != null ? val : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) return "***";
        if (recipient.contains("@")) {
            String[] parts = recipient.split("@", 2);
            String name = parts[0];
            String maskedName = name.length() <= 2 ? name.charAt(0) + "***" : name.substring(0, 2) + "***";
            return maskedName + "@" + parts[1];
        }
        if (recipient.length() > 6) {
            return recipient.substring(0, 3) + "***" + recipient.substring(recipient.length() - 2);
        }
        return "***";
    }

    @Override
    public AdminDashboardMetrics getDashboardMetrics() {
        long totalStudios = countQuery("SELECT COUNT(*) FROM designer_studios");
        long activeStudios = countQuery("SELECT COUNT(*) FROM designer_studios WHERE status = 'ACTIVE'");
        long suspendedStudios = countQuery("SELECT COUNT(*) FROM designer_studios WHERE status = 'SUSPENDED'");
        long publishedStudios = countQuery("SELECT COUNT(*) FROM designer_studios WHERE publication_status = 'PUBLISHED'");

        long totalUsers = countQuery("SELECT COUNT(*) FROM users");
        long activeUsers = countQuery("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
        long suspendedUsers = countQuery("SELECT COUNT(*) FROM users WHERE status = 'SUSPENDED'");

        long totalProjects = countQuery("SELECT COUNT(*) FROM studio_projects");
        long publicProjects = countQuery("SELECT COUNT(*) FROM studio_projects WHERE visibility_status = 'PORTFOLIO'");

        long totalLeads = countQuery("SELECT COUNT(*) FROM studio_leads");
        long pendingVerifications = countQuery("SELECT COUNT(*) FROM studio_verifications WHERE status = 'PENDING'");
        long pendingReviewReports = countQuery("SELECT COUNT(*) FROM review_reports WHERE status = 'PENDING'");

        long totalMediaAssets = countQuery("SELECT COUNT(*) FROM media_assets");
        long totalAiGenerations = countQuery("SELECT COUNT(*) FROM ai_generation_jobs");

        List<AdminAuditLogDto> recentActivity = listAuditLogs(10, 0, null, null);

        return new AdminDashboardMetrics(
                totalStudios,
                activeStudios,
                suspendedStudios,
                publishedStudios,
                totalUsers,
                activeUsers,
                suspendedUsers,
                totalProjects,
                publicProjects,
                totalLeads,
                pendingVerifications,
                pendingReviewReports,
                totalMediaAssets,
                totalAiGenerations,
                recentActivity
        );
    }

    @Override
    public List<AdminUserSummaryDto> listUsers(int limit, int offset, String statusFilter) {
        return listUsers(limit, offset, statusFilter, null);
    }

    @Override
    public List<AdminUserSummaryDto> listUsers(int limit, int offset, String statusFilter, String query) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        String pattern = (query != null && !query.isBlank()) ? "%" + query.trim().toLowerCase() + "%" : null;

        String sql = """
            SELECT id, display_name, email, phone, status, created_at FROM users
            WHERE (? IS NULL OR status = ?)
              AND (? IS NULL OR LOWER(display_name) LIKE ? OR LOWER(email) LIKE ?)
            ORDER BY created_at DESC LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID userId = getUuid(rs, "id");
            String displayName = rs.getString("display_name");
            String email = rs.getString("email");
            String phone = rs.getString("phone");
            String status = rs.getString("status");
            Instant createdAt = getInstant(rs, "created_at");

            // Roles
            String rolesSql = "SELECT r.code FROM identity_roles r " +
                              "JOIN identity_user_roles ur ON ur.role_id = r.id " +
                              "WHERE ur.user_id = ? AND ur.revoked_at IS NULL";
            List<String> rolesList = jdbcTemplate.query(rolesSql, (rrs, rNum) -> rrs.getString("code"), userId);
            Set<String> roles = new HashSet<>(rolesList);

            // Studio Memberships
            String studiosSql = "SELECT s.name FROM designer_studios s " +
                                "JOIN studio_members sm ON sm.studio_id = s.id " +
                                "WHERE sm.user_id = ?";
            List<String> studioNames = jdbcTemplate.query(studiosSql, (srs, sNum) -> srs.getString("name"), userId);

            return new AdminUserSummaryDto(userId, displayName, email, phone, status, createdAt, roles, studioNames);
        }, statusFilter, statusFilter, pattern, pattern, pattern, boundedLimit, boundedOffset);
    }

    @Override
    public Optional<AdminUserDetailDto> getUserDetail(UUID userId) {
        String sql = "SELECT id, display_name, email, phone, status, suspension_reason, created_at FROM users WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String displayName = rs.getString("display_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String status = rs.getString("status");
                String reason = rs.getString("suspension_reason");
                Instant createdAt = getInstant(rs, "created_at");

                String rolesSql = "SELECT r.code FROM identity_roles r " +
                                  "JOIN identity_user_roles ur ON ur.role_id = r.id " +
                                  "WHERE ur.user_id = ? AND ur.revoked_at IS NULL";
                Set<String> roles = new HashSet<>(jdbcTemplate.query(rolesSql, (rrs, rNum) -> rrs.getString("code"), userId));

                String studiosSql = "SELECT s.name FROM designer_studios s " +
                                    "JOIN studio_members sm ON sm.studio_id = s.id " +
                                    "WHERE sm.user_id = ?";
                List<String> studioNames = jdbcTemplate.query(studiosSql, (srs, sNum) -> srs.getString("name"), userId);

                String sessionsSql = "SELECT COUNT(*) FROM identity_sessions WHERE user_id = ? AND revoked_at IS NULL AND idle_expires_at > now() AND absolute_expires_at > now()";
                Long activeSessions = jdbcTemplate.queryForObject(sessionsSql, Long.class, userId);

                return new AdminUserDetailDto(
                        userId,
                        displayName,
                        email,
                        phone,
                        status,
                        reason,
                        createdAt,
                        roles,
                        studioNames,
                        activeSessions != null ? activeSessions.intValue() : 0
                );
            }, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void updateUserStatus(UUID userId, String status) {
        updateUserStatus(userId, status, null);
    }

    @Override
    public void updateUserStatus(UUID userId, String status, String reason) {
        String sql = "UPDATE users SET status = ?, suspension_reason = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, reason, userId);
    }

    @Override
    public long countActiveSuperAdmins() {
        String sql = """
            SELECT COUNT(DISTINCT u.id)
            FROM users u
            JOIN identity_user_roles ur ON ur.user_id = u.id
            JOIN identity_roles r ON r.id = ur.role_id
            WHERE r.code = 'SUPER_ADMIN' AND ur.revoked_at IS NULL AND u.status = 'ACTIVE'
        """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public void updateUserRole(UUID userId, String roleCode) {
        // Revoke current active roles
        jdbcTemplate.update("UPDATE identity_user_roles SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL", userId);

        // Find role_id
        UUID roleId = jdbcTemplate.queryForObject("SELECT id FROM identity_roles WHERE code = ?", (rs, rowNum) -> getUuid(rs, "id"), roleCode);
        if (roleId != null) {
            int updated = jdbcTemplate.update(
                    "UPDATE identity_user_roles SET revoked_at = NULL, granted_at = now() WHERE user_id = ? AND role_id = ?",
                    userId, roleId
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO identity_user_roles (id, user_id, role_id, granted_at, revoked_at) VALUES (?, ?, ?, now(), NULL)",
                        com.interior.platform.common.util.UuidV7.randomUuid(), userId, roleId
                );
            }
        }
    }

    @Override
    public void revokeUserSessions(UUID userId) {
        jdbcTemplate.update("UPDATE identity_sessions SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL", userId);
    }

    @Override
    public List<AdminStudioSummaryDto> listStudios(int limit, int offset, String statusFilter) {
        return listStudios(limit, offset, statusFilter, null);
    }

    @Override
    public List<AdminStudioSummaryDto> listStudios(int limit, int offset, String statusFilter, String query) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        String pattern = (query != null && !query.isBlank()) ? "%" + query.trim().toLowerCase() + "%" : null;

        String sql = """
            SELECT s.id, s.name, s.slug, s.owner_id, u.email as owner_email, s.status, s.publication_status,
            COALESCE(v.status, 'NOT_SUBMITTED') as verification_status,
            (SELECT COUNT(*) FROM studio_projects p WHERE p.studio_id = s.id) as project_count,
            (SELECT COUNT(*) FROM studio_projects p WHERE p.studio_id = s.id AND p.visibility_status = 'PORTFOLIO') as public_project_count,
            s.created_at
            FROM designer_studios s
            LEFT JOIN users u ON u.id = s.owner_id
            LEFT JOIN studio_verifications v ON v.studio_id = s.id
            WHERE (? IS NULL OR s.status = ?)
              AND (? IS NULL OR LOWER(s.name) LIKE ? OR LOWER(s.slug) LIKE ?)
            ORDER BY s.created_at DESC LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminStudioSummaryDto(
                getUuid(rs, "id"),
                rs.getString("name"),
                rs.getString("slug"),
                getUuid(rs, "owner_id"),
                rs.getString("owner_email"),
                rs.getString("status"),
                rs.getString("publication_status"),
                rs.getString("verification_status"),
                rs.getLong("project_count"),
                rs.getLong("public_project_count"),
                getInstant(rs, "created_at")
        ), statusFilter, statusFilter, pattern, pattern, pattern, boundedLimit, boundedOffset);
    }

    @Override
    public Optional<AdminStudioDetailDto> getStudioDetail(UUID studioId) {
        String sql = """
            SELECT s.id, s.name, s.slug, s.owner_id, u.email as owner_email, s.status, s.suspension_reason, s.publication_status,
            COALESCE(v.status, 'NOT_SUBMITTED') as verification_status,
            COALESCE((SELECT bp.code FROM studio_subscriptions sub JOIN billing_plans bp ON bp.id = sub.plan_id WHERE sub.studio_id = s.id AND sub.status = 'ACTIVE' LIMIT 1), 'STANDARD') as plan_code,
            (SELECT COUNT(*) FROM studio_projects p WHERE p.studio_id = s.id) as project_count,
            (SELECT COUNT(*) FROM studio_projects p WHERE p.studio_id = s.id AND p.visibility_status = 'PORTFOLIO') as public_project_count,
            (SELECT COUNT(*) FROM media_assets ma WHERE ma.studio_id = s.id AND ma.deleted_at IS NULL) as photo_count,
            COALESCE((SELECT SUM(ma.file_size) FROM media_assets ma WHERE ma.studio_id = s.id AND ma.deleted_at IS NULL), 0) as storage_bytes,
            (SELECT COUNT(*) FROM upload_intents ui WHERE ui.studio_id = s.id AND ui.status = 'PENDING') as pending_intents,
            (SELECT COUNT(*) FROM communication_deliveries cd WHERE cd.studio_id = s.id AND cd.status = 'FAILED') as failed_communications,
            s.created_at
            FROM designer_studios s
            LEFT JOIN users u ON u.id = s.owner_id
            LEFT JOIN studio_verifications v ON v.studio_id = s.id
            WHERE s.id = ?
        """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new AdminStudioDetailDto(
                    getUuid(rs, "id"),
                    rs.getString("name"),
                    rs.getString("slug"),
                    getUuid(rs, "owner_id"),
                    rs.getString("owner_email"),
                    rs.getString("status"),
                    rs.getString("suspension_reason"),
                    rs.getString("publication_status"),
                    rs.getString("verification_status"),
                    rs.getString("plan_code"),
                    rs.getLong("project_count"),
                    rs.getLong("public_project_count"),
                    rs.getLong("photo_count"),
                    rs.getLong("storage_bytes"),
                    rs.getLong("pending_intents"),
                    rs.getLong("failed_communications"),
                    getInstant(rs, "created_at")
            ), studioId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void updateStudioStatus(UUID studioId, String status) {
        updateStudioStatus(studioId, status, null);
    }

    @Override
    public void updateStudioStatus(UUID studioId, String status, String reason) {
        String sql = "UPDATE designer_studios SET status = ?, suspension_reason = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, reason, studioId);
    }

    @Override
    public void updateStudioPlan(UUID studioId, String planCode) {
        UUID planId = jdbcTemplate.queryForObject(
                "SELECT id FROM billing_plans WHERE code = ?",
                (rs, rowNum) -> getUuid(rs, "id"),
                planCode
        );
        if (planId != null) {
            int updated = jdbcTemplate.update(
                    "UPDATE studio_subscriptions SET plan_id = ?, updated_at = now() WHERE studio_id = ? AND status = 'ACTIVE'",
                    planId, studioId
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO studio_subscriptions (id, studio_id, plan_id, status, provider, created_at, updated_at, version) VALUES (?, ?, ?, 'ACTIVE', 'ADMIN_OVERRIDE', now(), now(), 1)",
                        com.interior.platform.common.util.UuidV7.randomUuid(), studioId, planId
                );
            }
        }
    }

    @Override
    public List<AdminVerificationSummaryDto> listVerificationRequests(int limit, int offset, String statusFilter) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        String sql = """
            SELECT v.id, v.studio_id, s.name as studio_name, s.slug as studio_slug, v.business_name,
            v.professional_type, v.status, v.registration_number, v.gst_number, v.website_domain,
            v.notes, v.decision_reason, v.verified_at, v.expires_at, v.created_at,
            (SELECT COUNT(*) FROM studio_verification_documents d WHERE d.verification_id = v.id) as doc_count
            FROM studio_verifications v
            JOIN designer_studios s ON s.id = v.studio_id
            WHERE (? IS NULL OR v.status = ?)
            ORDER BY (CASE WHEN v.status = 'PENDING' THEN 0 ELSE 1 END), v.created_at DESC
            LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminVerificationSummaryDto(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("studio_name"),
                rs.getString("studio_slug"),
                rs.getString("business_name"),
                rs.getString("professional_type"),
                rs.getString("status"),
                rs.getString("registration_number"),
                rs.getString("gst_number"),
                rs.getString("website_domain"),
                rs.getString("notes"),
                rs.getString("decision_reason"),
                getInstant(rs, "verified_at"),
                getInstant(rs, "expires_at"),
                getInstant(rs, "created_at"),
                rs.getInt("doc_count")
        ), statusFilter, statusFilter, boundedLimit, boundedOffset);
    }

    @Override
    public List<AdminReviewSummaryDto> listReviewsForModeration(int limit, int offset, String statusFilter) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        String sql = """
            SELECT r.id, r.studio_id, s.name as studio_name, r.lead_id, r.rating, r.title,
            r.review_text, r.reviewer_display_name, r.display_name_mode, r.status,
            r.studio_response_text, r.studio_response_at, r.submitted_at, r.published_at,
            (SELECT COUNT(*) FROM review_reports rep WHERE rep.review_id = r.id) as report_count
            FROM studio_reviews r
            JOIN designer_studios s ON s.id = r.studio_id
            WHERE (? IS NULL OR r.status = ?)
            ORDER BY (CASE WHEN r.status = 'FLAGGED' THEN 0 ELSE 1 END), r.submitted_at DESC
            LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminReviewSummaryDto(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("studio_name"),
                getUuid(rs, "lead_id"),
                rs.getInt("rating"),
                rs.getString("title"),
                rs.getString("review_text"),
                rs.getString("reviewer_display_name"),
                rs.getString("display_name_mode"),
                rs.getString("status"),
                rs.getString("studio_response_text"),
                getInstant(rs, "studio_response_at"),
                getInstant(rs, "submitted_at"),
                getInstant(rs, "published_at"),
                rs.getInt("report_count")
        ), statusFilter, statusFilter, boundedLimit, boundedOffset);
    }

    @Override
    public void updateReviewStatus(UUID reviewId, String status) {
        String sql = "UPDATE studio_reviews SET status = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, reviewId);
    }

    @Override
    public List<AdminProjectSummaryDto> listProjectsForModeration(int limit, int offset, String moderationStatus, String query) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        String pattern = (query != null && !query.isBlank()) ? "%" + query.trim().toLowerCase() + "%" : null;

        String sql = """
            SELECT p.id, p.studio_id, s.name as studio_name, s.slug as studio_slug, p.title, p.slug,
            p.category_code, p.project_status, p.visibility_status, p.moderation_status, p.moderation_reason, p.created_at
            FROM studio_projects p
            JOIN designer_studios s ON s.id = p.studio_id
            WHERE (? IS NULL OR p.moderation_status = ?)
              AND (? IS NULL OR LOWER(p.title) LIKE ? OR LOWER(s.name) LIKE ?)
            ORDER BY (CASE WHEN p.moderation_status = 'FLAGGED' THEN 0 ELSE 1 END), p.created_at DESC
            LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminProjectSummaryDto(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("studio_name"),
                rs.getString("studio_slug"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("category_code"),
                rs.getString("project_status"),
                rs.getString("visibility_status"),
                rs.getString("moderation_status"),
                rs.getString("moderation_reason"),
                getInstant(rs, "created_at")
        ), moderationStatus, moderationStatus, pattern, pattern, pattern, boundedLimit, boundedOffset);
    }

    @Override
    public void updateProjectModerationStatus(UUID projectId, String moderationStatus, String reason) {
        String sql = "UPDATE studio_projects SET moderation_status = ?, moderation_reason = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, moderationStatus, reason, projectId);
    }

    @Override
    public List<AdminAuditLogDto> listAuditLogs(int limit, int offset, String actionFilter) {
        return listAuditLogs(limit, offset, actionFilter, null);
    }

    @Override
    public List<AdminAuditLogDto> listAuditLogs(int limit, int offset, String actionFilter, String resourceTypeFilter) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        String sql = """
            SELECT a.id, a.studio_id, a.actor_id, u.email as actor_email, a.action, a.resource_type,
            a.resource_id, a.request_id, a.details, a.timestamp
            FROM audit_events a
            LEFT JOIN users u ON u.id = a.actor_id
            WHERE (? IS NULL OR a.action = ?)
              AND (? IS NULL OR a.resource_type = ?)
            ORDER BY a.timestamp DESC LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminAuditLogDto(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                getUuid(rs, "actor_id"),
                rs.getString("actor_email"),
                rs.getString("action"),
                rs.getString("resource_type"),
                getUuid(rs, "resource_id"),
                rs.getString("request_id"),
                rs.getString("details"),
                getInstant(rs, "timestamp")
        ), actionFilter, actionFilter, resourceTypeFilter, resourceTypeFilter, boundedLimit, boundedOffset);
    }

    @Override
    public List<AdminCommunicationDeliveryDto> listCommunicationDeliveries(int limit, int offset, String statusFilter, String channelFilter) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        String sql = """
            SELECT id, studio_id, channel, event_type, recipient, subject_or_summary, status, provider,
            attempt_count, max_attempts, last_error, created_at
            FROM communication_deliveries
            WHERE (? IS NULL OR status = ?)
              AND (? IS NULL OR channel = ?)
            ORDER BY created_at DESC LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminCommunicationDeliveryDto(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("channel"),
                rs.getString("event_type"),
                maskRecipient(rs.getString("recipient")),
                rs.getString("subject_or_summary"),
                rs.getString("status"),
                rs.getString("provider"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"),
                rs.getString("last_error"),
                getInstant(rs, "created_at")
        ), statusFilter, statusFilter, channelFilter, channelFilter, boundedLimit, boundedOffset);
    }

    @Override
    public void updateCommunicationDeliveryStatus(UUID deliveryId, String status, int attemptCount, String lastError) {
        String sql = "UPDATE communication_deliveries SET status = ?, attempt_count = ?, last_error = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, attemptCount, lastError, deliveryId);
    }

    @Override
    public AdminMediaDiagnosticsDto getMediaDiagnostics() {
        long committedBytes = countQuery("SELECT COALESCE(SUM(file_size), 0) FROM media_assets WHERE deleted_at IS NULL");
        long committedPhotos = countQuery("SELECT COUNT(*) FROM media_assets WHERE deleted_at IS NULL");
        long pendingIntents = countQuery("SELECT COUNT(*) FROM upload_intents WHERE status = 'PENDING'");
        long expiredIntents = countQuery("SELECT COUNT(*) FROM upload_intents WHERE status = 'EXPIRED'");

        return new AdminMediaDiagnosticsDto(committedBytes, committedPhotos, pendingIntents, expiredIntents);
    }

    @Override
    public AdminAiDiagnosticsDto getAiDiagnostics() {
        long totalJobs = countQuery("SELECT COUNT(*) FROM ai_generation_jobs");
        long queuedJobs = countQuery("SELECT COUNT(*) FROM ai_generation_jobs WHERE status = 'QUEUED'");
        long processingJobs = countQuery("SELECT COUNT(*) FROM ai_generation_jobs WHERE status = 'PROCESSING'");
        long completedJobs = countQuery("SELECT COUNT(*) FROM ai_generation_jobs WHERE status = 'COMPLETED'");
        long failedJobs = countQuery("SELECT COUNT(*) FROM ai_generation_jobs WHERE status = 'FAILED'");

        Instant tenMinutesAgo = Instant.now().minus(java.time.Duration.ofMinutes(10));
        String stuckSql = "SELECT COUNT(*) FROM ai_generation_jobs WHERE status = 'PROCESSING' AND created_at < ?";
        Long stuck = jdbcTemplate.queryForObject(stuckSql, Long.class, Timestamp.from(tenMinutesAgo));
        long stuckJobs = stuck != null ? stuck : 0L;

        return new AdminAiDiagnosticsDto(totalJobs, queuedJobs, processingJobs, completedJobs, failedJobs, stuckJobs, "DEV_MOCK_ACTIVE");
    }
}
