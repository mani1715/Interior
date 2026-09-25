package com.interior.platform.reviews.repository;

import com.interior.platform.reviews.domain.*;
import com.interior.platform.reviews.dto.PublicStudioReviewDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcReviewRepository implements ReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ReviewInvitationRecord> invitationMapper = (rs, rowNum) -> new ReviewInvitationRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "lead_id"),
            getUuid(rs, "project_id"),
            rs.getBytes("token_hash"),
            ReviewInvitationStatus.valueOf(rs.getString("status")),
            toInstant(rs.getTimestamp("expires_at")),
            getUuid(rs, "created_by"),
            toInstant(rs.getTimestamp("revoked_at")),
            toInstant(rs.getTimestamp("used_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version")
    );

    private final RowMapper<ReviewInvitationSessionRecord> sessionMapper = (rs, rowNum) -> new ReviewInvitationSessionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "invitation_id"),
            getUuid(rs, "studio_id"),
            rs.getBytes("session_token_hash"),
            rs.getBytes("csrf_token_hash"),
            toInstant(rs.getTimestamp("expires_at")),
            toInstant(rs.getTimestamp("revoked_at")),
            toInstant(rs.getTimestamp("created_at"))
    );

    private final RowMapper<StudioReviewRecord> reviewMapper = (rs, rowNum) -> new StudioReviewRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "lead_id"),
            getUuid(rs, "project_id"),
            getUuid(rs, "review_invitation_id"),
            rs.getInt("rating"),
            rs.getString("title"),
            rs.getString("review_text"),
            rs.getString("reviewer_display_name"),
            DisplayNameMode.valueOf(rs.getString("display_name_mode")),
            ReviewStatus.valueOf(rs.getString("status")),
            rs.getString("studio_response_text"),
            toInstant(rs.getTimestamp("studio_response_at")),
            toInstant(rs.getTimestamp("flagged_at")),
            toInstant(rs.getTimestamp("removed_at")),
            toInstant(rs.getTimestamp("submitted_at")),
            toInstant(rs.getTimestamp("published_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version")
    );

    private final RowMapper<ReviewReportRecord> reportMapper = (rs, rowNum) -> new ReviewReportRecord(
            getUuid(rs, "id"),
            getUuid(rs, "review_id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "reporter_user_id"),
            rs.getString("reporter_ip"),
            ReportReason.valueOf(rs.getString("reason")),
            rs.getString("details"),
            ReportStatus.valueOf(rs.getString("status")),
            toInstant(rs.getTimestamp("created_at"))
    );

    @Override
    public ReviewInvitationRecord createInvitation(ReviewInvitationRecord invitation) {
        String sql = """
            INSERT INTO review_invitations (
                id, studio_id, lead_id, project_id, token_hash, status,
                expires_at, created_by, created_at, updated_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                invitation.id(),
                invitation.studioId(),
                invitation.leadId(),
                invitation.projectId(),
                invitation.tokenHash(),
                invitation.status().name(),
                Timestamp.from(invitation.expiresAt()),
                invitation.createdBy(),
                Timestamp.from(invitation.createdAt()),
                Timestamp.from(invitation.updatedAt()),
                invitation.version()
        );
        return invitation;
    }

    @Override
    public Optional<ReviewInvitationRecord> findInvitationById(UUID id) {
        String sql = "SELECT * FROM review_invitations WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, invitationMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ReviewInvitationRecord> findInvitationByLeadId(UUID studioId, UUID leadId) {
        String sql = "SELECT * FROM review_invitations WHERE studio_id = ? AND lead_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, invitationMapper, studioId, leadId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ReviewInvitationRecord> findInvitationByTokenHash(byte[] tokenHash) {
        String sql = "SELECT * FROM review_invitations WHERE token_hash = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, invitationMapper, (Object) tokenHash));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ReviewInvitationRecord> listInvitations(UUID studioId, int limit, int offset) {
        String sql = "SELECT * FROM review_invitations WHERE studio_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, invitationMapper, studioId, limit, offset);
    }

    @Override
    public int countInvitations(UUID studioId) {
        String sql = "SELECT count(*) FROM review_invitations WHERE studio_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return count != null ? count : 0;
    }

    @Override
    public void revokeInvitation(UUID studioId, UUID invitationId) {
        String sql = "UPDATE review_invitations SET status = 'REVOKED', revoked_at = now(), updated_at = now() WHERE studio_id = ? AND id = ?";
        jdbcTemplate.update(sql, studioId, invitationId);
    }

    @Override
    public void markInvitationUsed(UUID studioId, UUID invitationId, Instant usedAt) {
        String sql = "UPDATE review_invitations SET status = 'USED', used_at = ?, updated_at = now() WHERE studio_id = ? AND id = ?";
        jdbcTemplate.update(sql, Timestamp.from(usedAt), studioId, invitationId);
    }

    @Override
    public ReviewInvitationSessionRecord createSession(ReviewInvitationSessionRecord session) {
        String sql = """
            INSERT INTO review_invitation_sessions (
                id, invitation_id, studio_id, session_token_hash, csrf_token_hash,
                expires_at, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                session.id(),
                session.invitationId(),
                session.studioId(),
                session.sessionTokenHash(),
                session.csrfTokenHash(),
                Timestamp.from(session.expiresAt()),
                Timestamp.from(session.createdAt())
        );
        return session;
    }

    @Override
    public Optional<ReviewInvitationSessionRecord> findSessionByTokenHash(byte[] sessionTokenHash) {
        String sql = "SELECT * FROM review_invitation_sessions WHERE session_token_hash = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, sessionMapper, (Object) sessionTokenHash));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revokeSessionsForInvitation(UUID invitationId) {
        String sql = "UPDATE review_invitation_sessions SET revoked_at = now() WHERE invitation_id = ? AND revoked_at IS NULL";
        jdbcTemplate.update(sql, invitationId);
    }

    @Override
    public StudioReviewRecord createReview(StudioReviewRecord review) {
        String sql = """
            INSERT INTO studio_reviews (
                id, studio_id, lead_id, project_id, review_invitation_id,
                rating, title, review_text, reviewer_display_name, display_name_mode,
                status, submitted_at, published_at, updated_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                review.id(),
                review.studioId(),
                review.leadId(),
                review.projectId(),
                review.reviewInvitationId(),
                review.rating(),
                review.title(),
                review.reviewText(),
                review.reviewerDisplayName(),
                review.displayNameMode().name(),
                review.status().name(),
                Timestamp.from(review.submittedAt()),
                review.publishedAt() != null ? Timestamp.from(review.publishedAt()) : Timestamp.from(Instant.now()),
                Timestamp.from(review.updatedAt()),
                review.version()
        );
        return review;
    }

    @Override
    public Optional<StudioReviewRecord> findReviewById(UUID id) {
        String sql = "SELECT * FROM studio_reviews WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, reviewMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<StudioReviewRecord> findReviewByInvitationId(UUID invitationId) {
        String sql = "SELECT * FROM studio_reviews WHERE review_invitation_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, reviewMapper, invitationId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<StudioReviewRecord> findReviewByLeadId(UUID studioId, UUID leadId) {
        String sql = "SELECT * FROM studio_reviews WHERE studio_id = ? AND lead_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, reviewMapper, studioId, leadId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<StudioReviewRecord> listStudioReviews(UUID studioId, int limit, int offset) {
        String sql = "SELECT * FROM studio_reviews WHERE studio_id = ? ORDER BY submitted_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, reviewMapper, studioId, limit, offset);
    }

    @Override
    public int countStudioReviews(UUID studioId) {
        String sql = "SELECT count(*) FROM studio_reviews WHERE studio_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return count != null ? count : 0;
    }

    @Override
    public List<PublicStudioReviewDto> listPublishedReviews(UUID studioId, int limit, int offset) {
        String sql = """
            SELECT r.id, r.rating, r.title, r.review_text, r.reviewer_display_name,
                   r.display_name_mode, r.published_at, r.studio_response_text,
                   r.studio_response_at,
                   CASE WHEN p.id IS NOT NULL AND p.project_status = 'READY' AND p.visibility_status = 'PORTFOLIO' AND p.archived_at IS NULL THEN p.id ELSE NULL END AS project_id,
                   CASE WHEN p.id IS NOT NULL AND p.project_status = 'READY' AND p.visibility_status = 'PORTFOLIO' AND p.archived_at IS NULL THEN p.title ELSE NULL END AS project_title
            FROM studio_reviews r
            LEFT JOIN studio_projects p ON r.project_id = p.id AND r.studio_id = p.studio_id
            WHERE r.studio_id = ? AND r.status = 'PUBLISHED'
            ORDER BY r.published_at DESC
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new PublicStudioReviewDto(
                getUuid(rs, "id"),
                rs.getInt("rating"),
                rs.getString("title"),
                rs.getString("review_text"),
                rs.getString("reviewer_display_name"),
                DisplayNameMode.valueOf(rs.getString("display_name_mode")),
                toInstant(rs.getTimestamp("published_at")),
                rs.getString("studio_response_text"),
                toInstant(rs.getTimestamp("studio_response_at")),
                getUuid(rs, "project_id"),
                rs.getString("project_title")
        ), studioId, limit, offset);
    }

    @Override
    public int countPublishedReviews(UUID studioId) {
        String sql = "SELECT count(*) FROM studio_reviews WHERE studio_id = ? AND status = 'PUBLISHED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return count != null ? count : 0;
    }

    @Override
    public ReviewAggregate getReviewAggregate(UUID studioId) {
        String sql = """
            SELECT
                COALESCE(AVG(rating), 0.0) AS avg_rating,
                COUNT(*) AS total_count,
                COUNT(*) FILTER (WHERE rating = 1) AS count_1,
                COUNT(*) FILTER (WHERE rating = 2) AS count_2,
                COUNT(*) FILTER (WHERE rating = 3) AS count_3,
                COUNT(*) FILTER (WHERE rating = 4) AS count_4,
                COUNT(*) FILTER (WHERE rating = 5) AS count_5
            FROM studio_reviews
            WHERE studio_id = ? AND status = 'PUBLISHED'
        """;
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                double avg = rs.getDouble("avg_rating");
                int total = rs.getInt("total_count");
                int[] dist = new int[]{
                        rs.getInt("count_1"),
                        rs.getInt("count_2"),
                        rs.getInt("count_3"),
                        rs.getInt("count_4"),
                        rs.getInt("count_5")
                };
                // Round avg to 1 decimal place
                double roundedAvg = Math.round(avg * 10.0) / 10.0;
                return new ReviewAggregate(roundedAvg, total, dist);
            }, studioId);
        } catch (EmptyResultDataAccessException e) {
            return ReviewAggregate.empty();
        }
    }

    @Override
    public void updateStudioResponse(UUID studioId, UUID reviewId, String responseText, Instant responseAt) {
        String sql = """
            UPDATE studio_reviews
            SET studio_response_text = ?, studio_response_at = ?, updated_at = now()
            WHERE studio_id = ? AND id = ?
        """;
        jdbcTemplate.update(sql, responseText, Timestamp.from(responseAt), studioId, reviewId);
    }

    @Override
    public ReviewReportRecord createReport(ReviewReportRecord report) {
        String sql = """
            INSERT INTO review_reports (
                id, review_id, studio_id, reporter_user_id, reporter_ip,
                reason, details, status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                report.id(),
                report.reviewId(),
                report.studioId(),
                report.reporterUserId(),
                report.reporterIp(),
                report.reason().name(),
                report.details(),
                report.status().name(),
                Timestamp.from(report.createdAt())
        );
        return report;
    }

    @Override
    public void updateReviewStatus(UUID reviewId, ReviewStatus status, Instant flaggedAt, Instant removedAt) {
        String sql = """
            UPDATE studio_reviews
            SET status = ?,
                flagged_at = COALESCE(?, flagged_at),
                removed_at = COALESCE(?, removed_at),
                updated_at = now()
            WHERE id = ?
        """;
        jdbcTemplate.update(sql,
                status.name(),
                flaggedAt != null ? Timestamp.from(flaggedAt) : null,
                removedAt != null ? Timestamp.from(removedAt) : null,
                reviewId
        );
    }

    private static UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val instanceof UUID u) return u;
        if (val instanceof String s) return UUID.fromString(s);
        return null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
