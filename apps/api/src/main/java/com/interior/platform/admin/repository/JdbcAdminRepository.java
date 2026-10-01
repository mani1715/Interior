package com.interior.platform.admin.repository;

import com.interior.platform.admin.domain.AdminDashboardMetrics;
import com.interior.platform.admin.dto.*;
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
        long totalAiGenerations = countQuery("SELECT COUNT(*) FROM ai_generation_requests");

        List<AdminAuditLogDto> recentActivity = listAuditLogs(10, 0, null);

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
        String sql = "SELECT id, display_name, email, phone, status, created_at FROM users " +
                     "WHERE (? IS NULL OR status = ?) " +
                     "ORDER BY created_at DESC LIMIT ? OFFSET ?";

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
        }, statusFilter, statusFilter, limit, offset);
    }

    @Override
    public void updateUserStatus(UUID userId, String status) {
        String sql = "UPDATE users SET status = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, userId);
    }

    @Override
    public List<AdminStudioSummaryDto> listStudios(int limit, int offset, String statusFilter) {
        String sql = "SELECT s.id, s.name, s.slug, s.owner_id, u.email as owner_email, s.status, s.publication_status, " +
                     "COALESCE(v.status, 'NOT_SUBMITTED') as verification_status, " +
                     "(SELECT COUNT(*) FROM studio_projects p WHERE p.studio_id = s.id) as project_count, " +
                     "(SELECT COUNT(*) FROM studio_projects p WHERE p.studio_id = s.id AND p.visibility_status = 'PORTFOLIO') as public_project_count, " +
                     "s.created_at " +
                     "FROM designer_studios s " +
                     "LEFT JOIN users u ON u.id = s.owner_id " +
                     "LEFT JOIN studio_verifications v ON v.studio_id = s.id " +
                     "WHERE (? IS NULL OR s.status = ?) " +
                     "ORDER BY s.created_at DESC LIMIT ? OFFSET ?";

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
        ), statusFilter, statusFilter, limit, offset);
    }

    @Override
    public void updateStudioStatus(UUID studioId, String status) {
        String sql = "UPDATE designer_studios SET status = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, studioId);
    }

    @Override
    public List<AdminVerificationSummaryDto> listVerificationRequests(int limit, int offset, String statusFilter) {
        String sql = "SELECT v.id, v.studio_id, s.name as studio_name, s.slug as studio_slug, v.business_name, " +
                     "v.professional_type, v.status, v.registration_number, v.gst_number, v.website_domain, " +
                     "v.notes, v.decision_reason, v.verified_at, v.expires_at, v.created_at, " +
                     "(SELECT COUNT(*) FROM studio_verification_documents d WHERE d.verification_id = v.id) as doc_count " +
                     "FROM studio_verifications v " +
                     "JOIN designer_studios s ON s.id = v.studio_id " +
                     "WHERE (? IS NULL OR v.status = ?) " +
                     "ORDER BY (CASE WHEN v.status = 'PENDING' THEN 0 ELSE 1 END), v.created_at DESC " +
                     "LIMIT ? OFFSET ?";

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
        ), statusFilter, statusFilter, limit, offset);
    }

    @Override
    public List<AdminReviewSummaryDto> listReviewsForModeration(int limit, int offset, String statusFilter) {
        String sql = "SELECT r.id, r.studio_id, s.name as studio_name, r.lead_id, r.rating, r.title, " +
                     "r.review_text, r.reviewer_display_name, r.display_name_mode, r.status, " +
                     "r.studio_response_text, r.studio_response_at, r.submitted_at, r.published_at, " +
                     "(SELECT COUNT(*) FROM review_reports rep WHERE rep.review_id = r.id) as report_count " +
                     "FROM studio_reviews r " +
                     "JOIN designer_studios s ON s.id = r.studio_id " +
                     "WHERE (? IS NULL OR r.status = ?) " +
                     "ORDER BY (CASE WHEN r.status = 'FLAGGED' THEN 0 ELSE 1 END), r.submitted_at DESC " +
                     "LIMIT ? OFFSET ?";

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
        ), statusFilter, statusFilter, limit, offset);
    }

    @Override
    public void updateReviewStatus(UUID reviewId, String status) {
        String sql = "UPDATE studio_reviews SET status = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, reviewId);
    }

    @Override
    public List<AdminAuditLogDto> listAuditLogs(int limit, int offset, String actionFilter) {
        String sql = "SELECT a.id, a.studio_id, a.actor_id, u.email as actor_email, a.action, a.resource_type, " +
                     "a.resource_id, a.request_id, a.details, a.timestamp " +
                     "FROM audit_events a " +
                     "LEFT JOIN users u ON u.id = a.actor_id " +
                     "WHERE (? IS NULL OR a.action = ?) " +
                     "ORDER BY a.timestamp DESC LIMIT ? OFFSET ?";

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
        ), actionFilter, actionFilter, limit, offset);
    }
}
