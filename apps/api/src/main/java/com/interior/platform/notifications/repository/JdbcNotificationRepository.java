package com.interior.platform.notifications.repository;

import com.interior.platform.notifications.domain.NotificationPreferencesRecord;
import com.interior.platform.notifications.domain.NotificationRecord;
import com.interior.platform.notifications.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class JdbcNotificationRepository implements NotificationRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcNotificationRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private Boolean isPostgres;

    public JdbcNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private synchronized boolean isPostgreSql() {
        if (isPostgres == null) {
            try {
                String dbProduct = jdbcTemplate.execute((java.sql.Connection conn) -> conn.getMetaData().getDatabaseProductName());
                isPostgres = dbProduct != null && dbProduct.toLowerCase().contains("postgresql");
            } catch (Exception e) {
                isPostgres = false;
            }
        }
        return Boolean.TRUE.equals(isPostgres);
    }

    private Object toJsonbObject(String jsonString) {
        if (jsonString == null) return null;
        if (!isPostgreSql()) {
            return jsonString;
        }
        try {
            Class<?> clazz = Class.forName("org.postgresql.util.PGobject");
            Object pgo = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setType", String.class).invoke(pgo, "jsonb");
            clazz.getMethod("setValue", String.class).invoke(pgo, jsonString);
            return pgo;
        } catch (Exception ignored) {
            return jsonString;
        }
    }

    private final RowMapper<NotificationRecord> notificationRowMapper = (rs, rowNum) -> {
        String metadataStr = rs.getString("metadata");
        Timestamp readAtTs = rs.getTimestamp("read_at");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        UUID studioId = rs.getObject("studio_id") != null ? rs.getObject("studio_id", UUID.class) : null;

        return new NotificationRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            studioId,
            NotificationType.valueOf(rs.getString("type")),
            rs.getString("title"),
            rs.getString("message"),
            rs.getString("action_url"),
            readAtTs != null ? readAtTs.toInstant() : null,
            createdAtTs != null ? createdAtTs.toInstant() : Instant.now(),
            metadataStr
        );
    };

    private final RowMapper<NotificationPreferencesRecord> preferencesRowMapper = (rs, rowNum) -> {
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        return new NotificationPreferencesRecord(
            rs.getObject("user_id", UUID.class),
            rs.getBoolean("in_app_enabled"),
            rs.getBoolean("email_enabled"),
            rs.getBoolean("whatsapp_enabled"),
            rs.getBoolean("lead_notifications"),
            rs.getBoolean("review_notifications"),
            rs.getBoolean("ai_notifications"),
            rs.getBoolean("system_notifications"),
            updatedAtTs != null ? updatedAtTs.toInstant() : Instant.now()
        );
    };

    @Override
    public void createNotification(NotificationRecord notification) {
        String sql = """
            INSERT INTO notifications (id, user_id, studio_id, type, title, message, action_url, read_at, created_at, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
            sql,
            notification.id(),
            notification.userId(),
            notification.studioId(),
            notification.type().name(),
            notification.title(),
            notification.message(),
            notification.actionUrl(),
            notification.readAt() != null ? Timestamp.from(notification.readAt()) : null,
            Timestamp.from(notification.createdAt()),
            toJsonbObject(notification.metadata())
        );
    }

    @Override
    public List<NotificationRecord> findByUserId(UUID userId, int limit, int offset) {
        String sql = """
            SELECT id, user_id, studio_id, type, title, message, action_url, read_at, created_at, metadata
            FROM notifications
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(sql, notificationRowMapper, userId, limit, offset);
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count != null ? count : 0L;
    }

    @Override
    public Optional<NotificationRecord> findByIdAndUserId(UUID id, UUID userId) {
        String sql = """
            SELECT id, user_id, studio_id, type, title, message, action_url, read_at, created_at, metadata
            FROM notifications
            WHERE id = ? AND user_id = ?
        """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, notificationRowMapper, id, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void markAsRead(UUID id, UUID userId, Instant readAt) {
        String sql = "UPDATE notifications SET read_at = ? WHERE id = ? AND user_id = ? AND read_at IS NULL";
        jdbcTemplate.update(sql, Timestamp.from(readAt), id, userId);
    }

    @Override
    public void markAllAsRead(UUID userId, Instant readAt) {
        String sql = "UPDATE notifications SET read_at = ? WHERE user_id = ? AND read_at IS NULL";
        jdbcTemplate.update(sql, Timestamp.from(readAt), userId);
    }

    @Override
    public NotificationPreferencesRecord getPreferences(UUID userId) {
        String sql = """
            SELECT user_id, in_app_enabled, email_enabled, whatsapp_enabled,
                   lead_notifications, review_notifications, ai_notifications, system_notifications, updated_at
            FROM notification_preferences
            WHERE user_id = ?
        """;
        try {
            return jdbcTemplate.queryForObject(sql, preferencesRowMapper, userId);
        } catch (EmptyResultDataAccessException e) {
            // Return default preferences
            return NotificationPreferencesRecord.defaultForUser(userId);
        }
    }

    @Override
    public void savePreferences(NotificationPreferencesRecord prefs) {
        String sql = """
            INSERT INTO notification_preferences (
                user_id, in_app_enabled, email_enabled, whatsapp_enabled,
                lead_notifications, review_notifications, ai_notifications, system_notifications, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id) DO UPDATE SET
                in_app_enabled = EXCLUDED.in_app_enabled,
                email_enabled = EXCLUDED.email_enabled,
                whatsapp_enabled = EXCLUDED.whatsapp_enabled,
                lead_notifications = EXCLUDED.lead_notifications,
                review_notifications = EXCLUDED.review_notifications,
                ai_notifications = EXCLUDED.ai_notifications,
                system_notifications = EXCLUDED.system_notifications,
                updated_at = EXCLUDED.updated_at
        """;
        jdbcTemplate.update(
            sql,
            prefs.userId(),
            prefs.inAppEnabled(),
            prefs.emailEnabled(),
            prefs.whatsappEnabled(),
            prefs.leadNotifications(),
            prefs.reviewNotifications(),
            prefs.aiNotifications(),
            prefs.systemNotifications(),
            Timestamp.from(prefs.updatedAt())
        );
    }
}
