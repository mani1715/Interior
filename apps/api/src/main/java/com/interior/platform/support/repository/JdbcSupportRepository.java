package com.interior.platform.support.repository;

import com.interior.platform.support.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSupportRepository implements SupportRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSupportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SupportRequestRecord> requestRowMapper = (rs, rowNum) -> {
        Timestamp resolvedTs = rs.getTimestamp("resolved_at");
        return new SupportRequestRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getObject("studio_id", UUID.class),
                SupportCategory.valueOf(rs.getString("category")),
                rs.getString("subject"),
                SupportStatus.valueOf(rs.getString("status")),
                SupportPriority.valueOf(rs.getString("priority")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                resolvedTs != null ? resolvedTs.toInstant() : null
        );
    };

    private final RowMapper<SupportMessageRecord> messageRowMapper = (rs, rowNum) -> new SupportMessageRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("request_id", UUID.class),
            rs.getObject("author_user_id", UUID.class),
            SupportAuthorType.valueOf(rs.getString("author_type")),
            rs.getString("body"),
            rs.getBoolean("is_internal"),
            rs.getTimestamp("created_at").toInstant()
    );

    @Override
    public SupportRequestRecord createRequest(SupportRequestRecord req) {
        String sql = """
            INSERT INTO support_requests (id, user_id, studio_id, category, subject, status, priority, created_at, updated_at, resolved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                req.id(),
                req.userId(),
                req.studioId(),
                req.category().name(),
                req.subject(),
                req.status().name(),
                req.priority().name(),
                Timestamp.from(req.createdAt()),
                Timestamp.from(req.updatedAt()),
                req.resolvedAt() != null ? Timestamp.from(req.resolvedAt()) : null
        );
        return req;
    }

    @Override
    public Optional<SupportRequestRecord> findRequestById(UUID requestId) {
        String sql = "SELECT * FROM support_requests WHERE id = ?";
        List<SupportRequestRecord> list = jdbcTemplate.query(sql, requestRowMapper, requestId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<SupportRequestRecord> findRequestsByUserId(UUID userId, int limit, int offset) {
        String sql = "SELECT * FROM support_requests WHERE user_id = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, requestRowMapper, userId, limit, offset);
    }

    @Override
    public List<SupportRequestRecord> findRequestsByStudioId(UUID studioId, int limit, int offset) {
        String sql = "SELECT * FROM support_requests WHERE studio_id = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, requestRowMapper, studioId, limit, offset);
    }

    @Override
    public List<SupportRequestRecord> listAllRequests(int limit, int offset, SupportStatus status, SupportCategory category, SupportPriority priority) {
        StringBuilder sql = new StringBuilder("SELECT * FROM support_requests WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append("AND status = ? ");
            params.add(status.name());
        }
        if (category != null) {
            sql.append("AND category = ? ");
            params.add(category.name());
        }
        if (priority != null) {
            sql.append("AND priority = ? ");
            params.add(priority.name());
        }

        sql.append("ORDER BY updated_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), requestRowMapper, params.toArray());
    }

    @Override
    public void updateRequestStatus(UUID requestId, SupportStatus status, SupportPriority priority, Instant resolvedAt) {
        StringBuilder sql = new StringBuilder("UPDATE support_requests SET updated_at = now() ");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(", status = ? ");
            params.add(status.name());
        }
        if (priority != null) {
            sql.append(", priority = ? ");
            params.add(priority.name());
        }
        if (resolvedAt != null) {
            sql.append(", resolved_at = ? ");
            params.add(Timestamp.from(resolvedAt));
        } else if (status == SupportStatus.OPEN || status == SupportStatus.IN_PROGRESS || status == SupportStatus.WAITING_USER) {
            sql.append(", resolved_at = NULL ");
        }

        sql.append("WHERE id = ?");
        params.add(requestId);

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    @Override
    public SupportMessageRecord createMessage(SupportMessageRecord msg) {
        String sql = """
            INSERT INTO support_messages (id, request_id, author_user_id, author_type, body, is_internal, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                msg.id(),
                msg.requestId(),
                msg.authorUserId(),
                msg.authorType().name(),
                msg.body(),
                msg.isInternal(),
                Timestamp.from(msg.createdAt())
        );
        // Also touch updated_at on the parent ticket
        jdbcTemplate.update("UPDATE support_requests SET updated_at = ? WHERE id = ?", Timestamp.from(msg.createdAt()), msg.requestId());
        return msg;
    }

    @Override
    public List<SupportMessageRecord> findMessagesByRequestId(UUID requestId, boolean includeInternal) {
        String sql;
        if (includeInternal) {
            sql = "SELECT * FROM support_messages WHERE request_id = ? ORDER BY created_at ASC";
            return jdbcTemplate.query(sql, messageRowMapper, requestId);
        } else {
            sql = "SELECT * FROM support_messages WHERE request_id = ? AND is_internal = false ORDER BY created_at ASC";
            return jdbcTemplate.query(sql, messageRowMapper, requestId);
        }
    }

    @Override
    public int countUserRequestsLastHour(UUID userId) {
        String sql = "SELECT COUNT(*) FROM support_requests WHERE user_id = ? AND created_at >= (now() - interval '1 hour')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    @Override
    public int countRecentUserMessages(UUID userId, Instant since) {
        String sql = "SELECT COUNT(*) FROM support_messages WHERE author_user_id = ? AND created_at >= ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, Timestamp.from(since));
        return count != null ? count : 0;
    }
}
