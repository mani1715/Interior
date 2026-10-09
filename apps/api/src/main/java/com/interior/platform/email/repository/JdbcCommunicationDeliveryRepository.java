package com.interior.platform.email.repository;

import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.domain.DeliveryChannel;
import com.interior.platform.email.domain.DeliveryStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCommunicationDeliveryRepository implements CommunicationDeliveryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCommunicationDeliveryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<CommunicationDeliveryRecord> rowMapper = (rs, rowNum) -> {
        Timestamp nextRetryTs = rs.getTimestamp("next_retry_at");
        Timestamp deliveredTs = rs.getTimestamp("delivered_at");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        UUID studioId = rs.getObject("studio_id") != null ? rs.getObject("studio_id", UUID.class) : null;
        UUID recipientUserId = rs.getObject("recipient_user_id") != null ? rs.getObject("recipient_user_id", UUID.class) : null;

        return new CommunicationDeliveryRecord(
                rs.getObject("id", UUID.class),
                studioId,
                recipientUserId,
                DeliveryChannel.valueOf(rs.getString("channel")),
                rs.getString("event_type"),
                rs.getString("recipient"),
                rs.getString("subject_or_summary"),
                DeliveryStatus.valueOf(rs.getString("status")),
                rs.getString("provider"),
                rs.getString("provider_message_id"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"),
                rs.getString("last_error"),
                nextRetryTs != null ? nextRetryTs.toInstant() : null,
                rs.getString("idempotency_key"),
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                updatedTs != null ? updatedTs.toInstant() : Instant.now(),
                deliveredTs != null ? deliveredTs.toInstant() : null
        );
    };

    @Override
    public void save(CommunicationDeliveryRecord record) {
        String sql = """
            INSERT INTO communication_deliveries (
                id, studio_id, recipient_user_id, channel, event_type, recipient,
                subject_or_summary, status, provider, provider_message_id, attempt_count,
                max_attempts, last_error, next_retry_at, idempotency_key, created_at, updated_at, delivered_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                record.id(),
                record.studioId(),
                record.recipientUserId(),
                record.channel().name(),
                record.eventType(),
                record.recipient(),
                record.subjectOrSummary(),
                record.status().name(),
                record.provider(),
                record.providerMessageId(),
                record.attemptCount(),
                record.maxAttempts(),
                record.lastError(),
                record.nextRetryAt() != null ? Timestamp.from(record.nextRetryAt()) : null,
                record.idempotencyKey(),
                Timestamp.from(record.createdAt()),
                Timestamp.from(record.updatedAt()),
                record.deliveredAt() != null ? Timestamp.from(record.deliveredAt()) : null
        );
    }

    @Override
    public Optional<CommunicationDeliveryRecord> findById(UUID id) {
        String sql = "SELECT * FROM communication_deliveries WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CommunicationDeliveryRecord> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) return Optional.empty();
        String sql = "SELECT * FROM communication_deliveries WHERE idempotency_key = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, idempotencyKey));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<CommunicationDeliveryRecord> findByStudioId(UUID studioId, int limit, int offset) {
        String sql = """
            SELECT * FROM communication_deliveries
            WHERE studio_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(sql, rowMapper, studioId, limit, offset);
    }

    @Override
    public void updateStatus(
            UUID id,
            DeliveryStatus status,
            String providerMessageId,
            int attemptCount,
            String lastError,
            Instant nextRetryAt,
            Instant deliveredAt
    ) {
        String sql = """
            UPDATE communication_deliveries
            SET status = ?,
                provider_message_id = COALESCE(?, provider_message_id),
                attempt_count = ?,
                last_error = ?,
                next_retry_at = ?,
                delivered_at = COALESCE(?, delivered_at),
                updated_at = ?
            WHERE id = ?
        """;
        jdbcTemplate.update(
                sql,
                status.name(),
                providerMessageId,
                attemptCount,
                lastError,
                nextRetryAt != null ? Timestamp.from(nextRetryAt) : null,
                deliveredAt != null ? Timestamp.from(deliveredAt) : null,
                Timestamp.from(Instant.now()),
                id
        );
    }
}
