package com.interior.platform.security.repository;

import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SecurityRepository {
    void createSession(SessionRecord session);
    Optional<SessionRecord> findSessionByTokenHash(byte[] tokenHash);
    Optional<SessionRecord> findSessionById(UUID sessionId);
    void updateSessionLastSeen(UUID sessionId, Instant lastSeenAt, Instant idleExpiresAt);
    void updateSessionCsrfHash(UUID sessionId, byte[] csrfHash);
    void revokeSession(UUID sessionId, Instant revokedAt);
    void revokeAllUserSessions(UUID userId, Instant revokedAt);

    Optional<UserRecord> findUserById(UUID userId);
    Optional<UserRecord> findUserByEmail(String email);
    Optional<UserRecord> findUserByExternalIdentity(String issuer, String subject);
    void linkExternalIdentity(UUID id, UUID userId, String issuer, String subject, Instant linkedAt);
    void createUser(UserRecord user);

    Set<String> getUserRoles(UUID userId);
    void assignUserRole(UUID id, UUID userId, String roleCode, Instant grantedAt);
    List<StudioMemberRecord> getStudioMemberships(UUID userId);

    void createStudio(UUID id, String name, String slug, UUID ownerId, String status);
    UUID findStudioIdBySlug(String slug);
    void addStudioMember(UUID id, UUID studioId, UUID userId, String role);

    void recordAuditEvent(UUID id, UUID studioId, UUID actorId, String action, String resourceType,
                          UUID resourceId, String requestId, String details, Instant timestamp);
}
