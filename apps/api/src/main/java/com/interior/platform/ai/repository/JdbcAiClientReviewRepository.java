package com.interior.platform.ai.repository;

import com.interior.platform.ai.domain.AiClientReviewCommentRecord;
import com.interior.platform.ai.domain.AiClientReviewDecisionRecord;
import com.interior.platform.ai.domain.AiClientReviewItemRecord;
import com.interior.platform.ai.domain.AiClientReviewRecord;
import com.interior.platform.ai.domain.AiClientReviewSessionRecord;
import com.interior.platform.ai.domain.ClientReviewDecisionType;
import com.interior.platform.ai.domain.ClientReviewStatus;
import com.interior.platform.ai.domain.CommentAuthorType;
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
public class JdbcAiClientReviewRepository implements AiClientReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiClientReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AiClientReviewRecord> reviewMapper = (rs, rowNum) -> new AiClientReviewRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "project_id"),
            rs.getString("title"),
            rs.getString("custom_message"),
            rs.getBytes("token_hash"),
            ClientReviewStatus.valueOf(rs.getString("status")),
            rs.getBoolean("include_original"),
            toInstant(rs.getTimestamp("expires_at")),
            getUuid(rs, "current_approved_job_id"),
            getUuid(rs, "created_by"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version")
    );

    private final RowMapper<AiClientReviewSessionRecord> sessionMapper = (rs, rowNum) -> new AiClientReviewSessionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "review_id"),
            rs.getBytes("session_token_hash"),
            rs.getBytes("csrf_token_hash"),
            toInstant(rs.getTimestamp("expires_at")),
            toInstant(rs.getTimestamp("revoked_at")),
            toInstant(rs.getTimestamp("created_at"))
    );

    private final RowMapper<AiClientReviewItemRecord> itemMapper = (rs, rowNum) -> new AiClientReviewItemRecord(
            getUuid(rs, "id"),
            getUuid(rs, "review_id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "job_id"),
            getUuid(rs, "media_id"),
            rs.getString("display_label"),
            rs.getInt("display_order"),
            toInstant(rs.getTimestamp("created_at"))
    );

    private final RowMapper<AiClientReviewDecisionRecord> decisionMapper = (rs, rowNum) -> new AiClientReviewDecisionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "review_id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "job_id"),
            ClientReviewDecisionType.valueOf(rs.getString("decision")),
            rs.getString("client_name"),
            rs.getString("feedback"),
            rs.getBoolean("is_current"),
            toInstant(rs.getTimestamp("created_at"))
    );

    private final RowMapper<AiClientReviewCommentRecord> commentMapper = (rs, rowNum) -> new AiClientReviewCommentRecord(
            getUuid(rs, "id"),
            getUuid(rs, "review_id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "job_id"),
            CommentAuthorType.valueOf(rs.getString("author_type")),
            rs.getString("author_name"),
            rs.getString("comment_text"),
            toInstant(rs.getTimestamp("created_at"))
    );

    @Override
    public void createReview(AiClientReviewRecord review) {
        String sql = "INSERT INTO ai_client_reviews (" +
                "id, studio_id, project_id, title, custom_message, token_hash, status, " +
                "include_original, expires_at, current_approved_job_id, created_by, created_at, updated_at, version" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                review.id(),
                review.studioId(),
                review.projectId(),
                review.title(),
                review.customMessage(),
                review.tokenHash(),
                review.status().name(),
                review.includeOriginal(),
                Timestamp.from(review.expiresAt()),
                review.currentApprovedJobId(),
                review.createdBy(),
                Timestamp.from(review.createdAt()),
                Timestamp.from(review.updatedAt()),
                review.version()
        );
    }

    @Override
    public Optional<AiClientReviewRecord> findReviewById(UUID studioId, UUID reviewId) {
        String sql = "SELECT * FROM ai_client_reviews WHERE studio_id = ? AND id = ?";
        List<AiClientReviewRecord> list = jdbcTemplate.query(sql, reviewMapper, studioId, reviewId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<AiClientReviewRecord> findReviewByIdGlobal(UUID reviewId) {
        String sql = "SELECT * FROM ai_client_reviews WHERE id = ?";
        List<AiClientReviewRecord> list = jdbcTemplate.query(sql, reviewMapper, reviewId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<AiClientReviewRecord> findReviewByTokenHash(byte[] tokenHash) {
        String sql = "SELECT * FROM ai_client_reviews WHERE token_hash = ?";
        List<AiClientReviewRecord> list = jdbcTemplate.query(sql, reviewMapper, tokenHash);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<AiClientReviewRecord> listReviewsByStudio(UUID studioId, UUID projectId, int limit, int offset) {
        if (projectId != null) {
            String sql = "SELECT * FROM ai_client_reviews WHERE studio_id = ? AND project_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
            return jdbcTemplate.query(sql, reviewMapper, studioId, projectId, limit, offset);
        }
        String sql = "SELECT * FROM ai_client_reviews WHERE studio_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, reviewMapper, studioId, limit, offset);
    }

    @Override
    public int countReviewsByStudio(UUID studioId, UUID projectId) {
        if (projectId != null) {
            String sql = "SELECT count(*) FROM ai_client_reviews WHERE studio_id = ? AND project_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId, projectId);
            return count != null ? count : 0;
        }
        String sql = "SELECT count(*) FROM ai_client_reviews WHERE studio_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return count != null ? count : 0;
    }

    @Override
    public void updateReviewStatus(UUID studioId, UUID reviewId, ClientReviewStatus status) {
        String sql = "UPDATE ai_client_reviews SET status = ?, updated_at = now() WHERE studio_id = ? AND id = ?";
        jdbcTemplate.update(sql, status.name(), studioId, reviewId);
    }

    @Override
    public void updateTokenHash(UUID studioId, UUID reviewId, byte[] newTokenHash) {
        String sql = "UPDATE ai_client_reviews SET token_hash = ?, status = 'OPEN', updated_at = now() WHERE studio_id = ? AND id = ?";
        jdbcTemplate.update(sql, newTokenHash, studioId, reviewId);
    }

    @Override
    public void updateReviewCurrentApprovedJob(UUID reviewId, UUID jobId) {
        String sql = "UPDATE ai_client_reviews SET current_approved_job_id = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, jobId, reviewId);
    }

    @Override
    public void createReviewSession(AiClientReviewSessionRecord session) {
        String sql = "INSERT INTO ai_client_review_sessions (" +
                "id, review_id, session_token_hash, csrf_token_hash, expires_at, revoked_at, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                session.id(),
                session.reviewId(),
                session.sessionTokenHash(),
                session.csrfTokenHash(),
                Timestamp.from(session.expiresAt()),
                session.revokedAt() != null ? Timestamp.from(session.revokedAt()) : null,
                Timestamp.from(session.createdAt())
        );
    }

    @Override
    public Optional<AiClientReviewSessionRecord> findReviewSessionByTokenHash(byte[] sessionTokenHash) {
        String sql = "SELECT * FROM ai_client_review_sessions WHERE session_token_hash = ?";
        List<AiClientReviewSessionRecord> list = jdbcTemplate.query(sql, sessionMapper, sessionTokenHash);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public void revokeAllSessionsForReview(UUID reviewId) {
        String sql = "UPDATE ai_client_review_sessions SET revoked_at = now() WHERE review_id = ? AND revoked_at IS NULL";
        jdbcTemplate.update(sql, reviewId);
    }

    @Override
    public void createReviewItems(List<AiClientReviewItemRecord> items) {
        if (items.isEmpty()) return;
        String sql = "INSERT INTO ai_client_review_items (" +
                "id, review_id, studio_id, job_id, media_id, display_label, display_order, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        for (AiClientReviewItemRecord item : items) {
            jdbcTemplate.update(sql,
                    item.id(),
                    item.reviewId(),
                    item.studioId(),
                    item.jobId(),
                    item.mediaId(),
                    item.displayLabel(),
                    item.displayOrder(),
                    Timestamp.from(item.createdAt())
            );
        }
    }

    @Override
    public List<AiClientReviewItemRecord> findItemsByReviewId(UUID reviewId) {
        String sql = "SELECT * FROM ai_client_review_items WHERE review_id = ? ORDER BY display_order ASC";
        return jdbcTemplate.query(sql, itemMapper, reviewId);
    }

    @Override
    public Optional<AiClientReviewItemRecord> findItemByReviewAndJob(UUID reviewId, UUID jobId) {
        String sql = "SELECT * FROM ai_client_review_items WHERE review_id = ? AND job_id = ?";
        List<AiClientReviewItemRecord> list = jdbcTemplate.query(sql, itemMapper, reviewId, jobId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public boolean isMediaInReview(UUID reviewId, UUID mediaId) {
        String sql = "SELECT count(*) FROM ai_client_review_items WHERE review_id = ? AND media_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, reviewId, mediaId);
        return count != null && count > 0;
    }

    @Override
    public void createDecision(AiClientReviewDecisionRecord decision) {
        String sql = "INSERT INTO ai_client_review_decisions (" +
                "id, review_id, studio_id, job_id, decision, client_name, feedback, is_current, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                decision.id(),
                decision.reviewId(),
                decision.studioId(),
                decision.jobId(),
                decision.decision().name(),
                decision.clientName(),
                decision.feedback(),
                decision.isCurrent(),
                Timestamp.from(decision.createdAt())
        );
    }

    @Override
    public void clearCurrentDecisionsForReview(UUID reviewId) {
        String sql = "UPDATE ai_client_review_decisions SET is_current = false WHERE review_id = ? AND is_current = true";
        jdbcTemplate.update(sql, reviewId);
    }

    @Override
    public List<AiClientReviewDecisionRecord> findDecisionsByReviewId(UUID reviewId) {
        String sql = "SELECT * FROM ai_client_review_decisions WHERE review_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, decisionMapper, reviewId);
    }

    @Override
    public Optional<AiClientReviewDecisionRecord> findCurrentDecisionForJob(UUID reviewId, UUID jobId) {
        String sql = "SELECT * FROM ai_client_review_decisions WHERE review_id = ? AND job_id = ? AND is_current = true";
        List<AiClientReviewDecisionRecord> list = jdbcTemplate.query(sql, decisionMapper, reviewId, jobId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public boolean hasAnyClientInteraction(UUID reviewId) {
        String decisionSql = "SELECT count(*) FROM ai_client_review_decisions WHERE review_id = ?";
        Integer decisionCount = jdbcTemplate.queryForObject(decisionSql, Integer.class, reviewId);
        if (decisionCount != null && decisionCount > 0) return true;

        String commentSql = "SELECT count(*) FROM ai_client_review_comments WHERE review_id = ? AND author_type = 'CLIENT'";
        Integer commentCount = jdbcTemplate.queryForObject(commentSql, Integer.class, reviewId);
        return commentCount != null && commentCount > 0;
    }

    @Override
    public void createComment(AiClientReviewCommentRecord comment) {
        String sql = "INSERT INTO ai_client_review_comments (" +
                "id, review_id, studio_id, job_id, author_type, author_name, comment_text, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                comment.id(),
                comment.reviewId(),
                comment.studioId(),
                comment.jobId(),
                comment.authorType().name(),
                comment.authorName(),
                comment.commentText(),
                Timestamp.from(comment.createdAt())
        );
    }

    @Override
    public List<AiClientReviewCommentRecord> findCommentsByReviewId(UUID reviewId) {
        String sql = "SELECT * FROM ai_client_review_comments WHERE review_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, commentMapper, reviewId);
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
