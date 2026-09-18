package com.interior.platform.security.repository;

import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JdbcSecurityRepository implements SecurityRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSecurityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SessionRecord> sessionRowMapper = (rs, rowNum) -> new SessionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "user_id"),
            rs.getBytes("token_hash"),
            rs.getBytes("csrf_hash"),
            rs.getTimestamp("auth_time").toInstant(),
            rs.getString("assurance"),
            rs.getTimestamp("last_seen_at").toInstant(),
            rs.getTimestamp("idle_expires_at").toInstant(),
            rs.getTimestamp("absolute_expires_at").toInstant(),
            rs.getTimestamp("revoked_at") != null ? rs.getTimestamp("revoked_at").toInstant() : null,
            rs.getString("device_label")
    );

    private final RowMapper<UserRecord> userRowMapper = (rs, rowNum) -> new UserRecord(
            getUuid(rs, "id"),
            rs.getString("display_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            rs.getLong("version")
    );

    private static UUID getUuid(ResultSet rs, String columnLabel) throws SQLException {
        Object obj = rs.getObject(columnLabel);
        if (obj == null) {
            return null;
        }
        if (obj instanceof UUID u) {
            return u;
        }
        return UUID.fromString(obj.toString());
    }

    @Override
    public void createSession(SessionRecord session) {
        String sql = "INSERT INTO identity_sessions (id, user_id, token_hash, csrf_hash, auth_time, assurance, last_seen_at, idle_expires_at, absolute_expires_at, revoked_at, device_label) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                session.id(),
                session.userId(),
                session.tokenHash(),
                session.csrfHash(),
                Timestamp.from(session.authTime()),
                session.assurance(),
                Timestamp.from(session.lastSeenAt()),
                Timestamp.from(session.idleExpiresAt()),
                Timestamp.from(session.absoluteExpiresAt()),
                session.revokedAt() != null ? Timestamp.from(session.revokedAt()) : null,
                session.deviceLabel()
        );
    }

    @Override
    public Optional<SessionRecord> findSessionByTokenHash(byte[] tokenHash) {
        String sql = "SELECT * FROM identity_sessions WHERE token_hash = ?";
        try {
            SessionRecord session = jdbcTemplate.queryForObject(sql, sessionRowMapper, tokenHash);
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<SessionRecord> findSessionById(UUID sessionId) {
        String sql = "SELECT * FROM identity_sessions WHERE id = ?";
        try {
            SessionRecord session = jdbcTemplate.queryForObject(sql, sessionRowMapper, sessionId);
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void updateSessionLastSeen(UUID sessionId, Instant lastSeenAt, Instant idleExpiresAt) {
        String sql = "UPDATE identity_sessions SET last_seen_at = ?, idle_expires_at = ? WHERE id = ? AND revoked_at IS NULL";
        jdbcTemplate.update(sql, Timestamp.from(lastSeenAt), Timestamp.from(idleExpiresAt), sessionId);
    }

    @Override
    public void updateSessionCsrfHash(UUID sessionId, byte[] csrfHash) {
        String sql = "UPDATE identity_sessions SET csrf_hash = ? WHERE id = ? AND revoked_at IS NULL";
        jdbcTemplate.update(sql, csrfHash, sessionId);
    }

    @Override
    public void revokeSession(UUID sessionId, Instant revokedAt) {
        String sql = "UPDATE identity_sessions SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL";
        jdbcTemplate.update(sql, Timestamp.from(revokedAt), sessionId);
    }

    @Override
    public void revokeAllUserSessions(UUID userId, Instant revokedAt) {
        String sql = "UPDATE identity_sessions SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL";
        jdbcTemplate.update(sql, Timestamp.from(revokedAt), userId);
    }

    @Override
    public Optional<UserRecord> findUserById(UUID userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            UserRecord user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserRecord> findUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            UserRecord user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserRecord> findUserByExternalIdentity(String issuer, String subject) {
        String sql = "SELECT u.* FROM users u " +
                     "JOIN identity_external_identities ei ON ei.user_id = u.id " +
                     "WHERE ei.issuer = ? AND ei.subject = ?";
        try {
            UserRecord user = jdbcTemplate.queryForObject(sql, userRowMapper, issuer, subject);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void linkExternalIdentity(UUID id, UUID userId, String issuer, String subject, Instant linkedAt) {
        String sql = "INSERT INTO identity_external_identities (id, user_id, issuer, subject, linked_at) " +
                     "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, userId, issuer, subject, Timestamp.from(linkedAt));
    }

    @Override
    public void createUser(UserRecord user) {
        String sql = "INSERT INTO users (id, display_name, email, phone, status, created_at, updated_at, version) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                user.id(),
                user.displayName(),
                user.email(),
                user.phone(),
                user.status(),
                Timestamp.from(user.createdAt()),
                Timestamp.from(user.updatedAt()),
                user.version()
        );
    }

    @Override
    public Set<String> getUserRoles(UUID userId) {
        String sql = "SELECT r.code FROM identity_roles r " +
                     "JOIN identity_user_roles ur ON ur.role_id = r.id " +
                     "WHERE ur.user_id = ? AND ur.revoked_at IS NULL";
        List<String> codes = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("code"), userId);
        return new HashSet<>(codes);
    }

    @Override
    public void assignUserRole(UUID id, UUID userId, String roleCode, Instant grantedAt) {
        String findRoleIdSql = "SELECT id FROM identity_roles WHERE code = ?";
        UUID roleId = jdbcTemplate.queryForObject(findRoleIdSql, (rs, rowNum) -> getUuid(rs, "id"), roleCode);
        if (roleId != null) {
            String insertSql = "INSERT INTO identity_user_roles (id, user_id, role_id, granted_at) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertSql, id, userId, roleId, Timestamp.from(grantedAt));
        }
    }

    @Override
    public List<StudioMemberRecord> getStudioMemberships(UUID userId) {
        String sql = "SELECT sm.id, sm.studio_id, s.name as studio_name, s.slug as studio_slug, sm.user_id, sm.role, sm.granted_at " +
                     "FROM studio_members sm " +
                     "JOIN designer_studios s ON s.id = sm.studio_id " +
                     "WHERE sm.user_id = ? AND s.status = 'ACTIVE'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioMemberRecord(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("studio_name"),
                rs.getString("studio_slug"),
                getUuid(rs, "user_id"),
                rs.getString("role"),
                rs.getTimestamp("granted_at").toInstant()
        ), userId);
    }

    @Override
    public void createStudio(UUID id, String name, String slug, UUID ownerId, String status) {
        String sql = "INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, name, slug, ownerId, status);
    }

    @Override
    public UUID findStudioIdBySlug(String slug) {
        String sql = "SELECT id FROM designer_studios WHERE slug = ?";
        List<UUID> ids = jdbcTemplate.query(sql, (rs, rowNum) -> getUuid(rs, "id"), slug);
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Override
    public void addStudioMember(UUID id, UUID studioId, UUID userId, String role) {
        String sql = "INSERT INTO studio_members (id, studio_id, user_id, role) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, studioId, userId, role);
    }

    @Override
    public void saveOidcTransaction(com.interior.platform.security.domain.OidcTransaction transaction) {
        String sql = "INSERT INTO auth_oidc_transactions (id, state, nonce, code_verifier, provider_id, return_url, intent_role, created_at, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                com.interior.platform.common.util.UuidV7.randomUuid(),
                transaction.state(),
                transaction.nonce(),
                transaction.codeVerifier(),
                transaction.providerId(),
                transaction.returnUrl(),
                transaction.intentRole(),
                Timestamp.from(transaction.createdAt()),
                Timestamp.from(transaction.expiresAt())
        );
    }

    @Override
    public Optional<com.interior.platform.security.domain.OidcTransaction> consumeOidcTransaction(String state, Instant now) {
        // Atomic one-time consumption: only updates if not yet consumed and not expired
        String updateSql = "UPDATE auth_oidc_transactions SET consumed_at = ? " +
                           "WHERE state = ? AND consumed_at IS NULL AND expires_at > ?";
        int updated = jdbcTemplate.update(updateSql, Timestamp.from(now), state, Timestamp.from(now));
        if (updated == 0) {
            return Optional.empty();
        }

        String querySql = "SELECT state, nonce, code_verifier, provider_id, return_url, intent_role, created_at, expires_at " +
                          "FROM auth_oidc_transactions WHERE state = ?";
        List<com.interior.platform.security.domain.OidcTransaction> results = jdbcTemplate.query(querySql, (rs, rowNum) -> new com.interior.platform.security.domain.OidcTransaction(
                rs.getString("state"),
                rs.getString("nonce"),
                rs.getString("code_verifier"),
                rs.getString("provider_id"),
                rs.getString("return_url"),
                rs.getString("intent_role"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant()
        ), state);

        return results.stream().findFirst();
    }

    @Override
    public void recordAuditEvent(UUID id, UUID studioId, UUID actorId, String action, String resourceType,
                                 UUID resourceId, String requestId, String details, Instant timestamp) {
        String sql = "INSERT INTO audit_events (id, studio_id, actor_id, action, resource_type, resource_id, request_id, details, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, studioId, actorId, action, resourceType, resourceId, requestId, details, Timestamp.from(timestamp));
    }
}
