package com.interior.platform.team.repository;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.team.domain.StudioMemberDetails;
import com.interior.platform.team.domain.StudioMemberInvitationRecord;
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
public class JdbcStudioTeamRepository implements StudioTeamRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcStudioTeamRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<StudioMemberDetails> MEMBER_DETAILS_MAPPER = (rs, rowNum) ->
            new StudioMemberDetails(
                    getUuid(rs, "membership_id"),
                    getUuid(rs, "studio_id"),
                    getUuid(rs, "user_id"),
                    rs.getString("display_name"),
                    rs.getString("email"),
                    rs.getString("role"),
                    getInstant(rs, "granted_at")
            );

    private static final RowMapper<StudioMemberInvitationRecord> INVITATION_MAPPER = (rs, rowNum) ->
            new StudioMemberInvitationRecord(
                    getUuid(rs, "id"),
                    getUuid(rs, "studio_id"),
                    rs.getString("invited_email"),
                    rs.getString("role"),
                    rs.getBytes("token_hash"),
                    getUuid(rs, "invited_by_user_id"),
                    rs.getString("status"),
                    getInstant(rs, "expires_at"),
                    getInstant(rs, "accepted_at"),
                    getUuid(rs, "accepted_by_user_id"),
                    getInstant(rs, "revoked_at"),
                    getInstant(rs, "created_at"),
                    getInstant(rs, "updated_at")
            );

    @Override
    public List<StudioMemberDetails> getStudioMembersWithUserDetails(UUID studioId) {
        String sql = """
                SELECT sm.id as membership_id, sm.studio_id, sm.user_id, u.display_name, u.email, sm.role, sm.granted_at
                FROM studio_members sm
                JOIN users u ON u.id = sm.user_id
                WHERE sm.studio_id = ?
                ORDER BY sm.granted_at ASC
                """;
        return jdbcTemplate.query(sql, MEMBER_DETAILS_MAPPER, studioId);
    }

    @Override
    public Optional<StudioMemberDetails> findMembershipById(UUID membershipId) {
        String sql = """
                SELECT sm.id as membership_id, sm.studio_id, sm.user_id, u.display_name, u.email, sm.role, sm.granted_at
                FROM studio_members sm
                JOIN users u ON u.id = sm.user_id
                WHERE sm.id = ?
                """;
        List<StudioMemberDetails> results = jdbcTemplate.query(sql, MEMBER_DETAILS_MAPPER, membershipId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<StudioMemberDetails> findMembershipByStudioAndUser(UUID studioId, UUID userId) {
        String sql = """
                SELECT sm.id as membership_id, sm.studio_id, sm.user_id, u.display_name, u.email, sm.role, sm.granted_at
                FROM studio_members sm
                JOIN users u ON u.id = sm.user_id
                WHERE sm.studio_id = ? AND sm.user_id = ?
                """;
        List<StudioMemberDetails> results = jdbcTemplate.query(sql, MEMBER_DETAILS_MAPPER, studioId, userId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public int countAdmins(UUID studioId) {
        String sql = """
                SELECT COUNT(*) FROM studio_members
                WHERE studio_id = ? AND UPPER(role) IN ('OWNER', 'ADMIN', 'DESIGNER_ADMIN')
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return count != null ? count : 0;
    }

    @Override
    public void updateMemberRole(UUID membershipId, String newRole) {
        String sql = "UPDATE studio_members SET role = ? WHERE id = ?";
        jdbcTemplate.update(sql, newRole, membershipId);
    }

    @Override
    public void removeMember(UUID membershipId) {
        String sql = "DELETE FROM studio_members WHERE id = ?";
        jdbcTemplate.update(sql, membershipId);
    }

    @Override
    public void saveInvitation(StudioMemberInvitationRecord inv) {
        String sql = """
                INSERT INTO studio_member_invitations (
                    id, studio_id, invited_email, role, token_hash, invited_by_user_id,
                    status, expires_at, accepted_at, accepted_by_user_id, revoked_at,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                inv.id(),
                inv.studioId(),
                inv.invitedEmail(),
                inv.role(),
                inv.tokenHash(),
                inv.invitedByUserId(),
                inv.status(),
                Timestamp.from(inv.expiresAt()),
                inv.acceptedAt() != null ? Timestamp.from(inv.acceptedAt()) : null,
                inv.acceptedByUserId(),
                inv.revokedAt() != null ? Timestamp.from(inv.revokedAt()) : null,
                Timestamp.from(inv.createdAt()),
                Timestamp.from(inv.updatedAt())
        );
    }

    @Override
    public Optional<StudioMemberInvitationRecord> findInvitationById(UUID invitationId) {
        String sql = "SELECT * FROM studio_member_invitations WHERE id = ?";
        List<StudioMemberInvitationRecord> list = jdbcTemplate.query(sql, INVITATION_MAPPER, invitationId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<StudioMemberInvitationRecord> findInvitationByTokenHash(byte[] tokenHash) {
        String sql = "SELECT * FROM studio_member_invitations WHERE token_hash = ?";
        List<StudioMemberInvitationRecord> list = jdbcTemplate.query(sql, INVITATION_MAPPER, tokenHash);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<StudioMemberInvitationRecord> findPendingInvitations(UUID studioId, Instant now) {
        String sql = """
                SELECT * FROM studio_member_invitations
                WHERE studio_id = ? AND status = 'PENDING' AND expires_at > ?
                ORDER BY created_at DESC
                """;
        return jdbcTemplate.query(sql, INVITATION_MAPPER, studioId, Timestamp.from(now));
    }

    @Override
    public Optional<StudioMemberInvitationRecord> findPendingInvitationByEmail(UUID studioId, String email, Instant now) {
        String sql = """
                SELECT * FROM studio_member_invitations
                WHERE studio_id = ? AND lower(invited_email) = lower(?) AND status = 'PENDING' AND expires_at > ?
                ORDER BY created_at DESC
                """;
        List<StudioMemberInvitationRecord> list = jdbcTemplate.query(sql, INVITATION_MAPPER, studioId, email.trim(), Timestamp.from(now));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public void updateInvitationStatus(UUID invitationId, String status, Instant timestamp, UUID acceptedByUserId) {
        String sql = """
                UPDATE studio_member_invitations
                SET status = ?,
                    accepted_at = CASE WHEN ? = 'ACCEPTED' THEN ? ELSE accepted_at END,
                    accepted_by_user_id = CASE WHEN ? = 'ACCEPTED' THEN ? ELSE accepted_by_user_id END,
                    revoked_at = CASE WHEN ? = 'REVOKED' THEN ? ELSE revoked_at END,
                    updated_at = ?
                WHERE id = ?
                """;
        Timestamp ts = Timestamp.from(timestamp);
        jdbcTemplate.update(sql, status, status, ts, status, acceptedByUserId, status, ts, ts, invitationId);
    }

    @Override
    public void revokeInvitation(UUID invitationId, Instant revokedAt) {
        String sql = "UPDATE studio_member_invitations SET status = 'REVOKED', revoked_at = ?, updated_at = ? WHERE id = ?";
        Timestamp ts = Timestamp.from(revokedAt);
        jdbcTemplate.update(sql, ts, ts, invitationId);
    }

    @Override
    public String findStudioName(UUID studioId) {
        String sql = "SELECT name FROM designer_studios WHERE id = ?";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"), studioId);
        return list.isEmpty() ? "Studio" : list.get(0);
    }

    @Override
    public void addStudioMember(UUID id, UUID studioId, UUID userId, String role) {
        String sql = "INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, studioId, userId, role, Timestamp.from(Instant.now()));
    }

    @Override
    public boolean hasPlatformRole(UUID userId, String roleCode) {
        String sql = """
                SELECT COUNT(*) FROM identity_user_roles ur
                JOIN identity_roles r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.code = ? AND ur.revoked_at IS NULL
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, roleCode);
        return count != null && count > 0;
    }

    @Override
    public void assignPlatformRole(UUID userId, String roleCode) {
        String findRoleIdSql = "SELECT id FROM identity_roles WHERE code = ?";
        List<UUID> roleIds = jdbcTemplate.query(findRoleIdSql, (rs, rowNum) -> getUuid(rs, "id"), roleCode);
        if (!roleIds.isEmpty()) {
            UUID roleId = roleIds.get(0);
            String insertSql = "INSERT INTO identity_user_roles (id, user_id, role_id, granted_at) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertSql, UuidV7.randomUuid(), userId, roleId, Timestamp.from(Instant.now()));
        }
    }

    private static UUID getUuid(ResultSet rs, String col) throws SQLException {
        Object val = rs.getObject(col);
        if (val == null) {
            return null;
        }
        if (val instanceof UUID u) {
            return u;
        }
        return UUID.fromString(val.toString());
    }

    private static Instant getInstant(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts != null ? ts.toInstant() : null;
    }
}
