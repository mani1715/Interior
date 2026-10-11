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
    void updateUserProfile(UUID userId, String displayName, String phone, String avatarUrl);
    void deactivateUser(UUID userId, Instant deactivatedAt);
    void requestUserDeletion(UUID userId, Instant requestedAt);
    List<SessionRecord> findActiveSessionsByUserId(UUID userId, Instant now);
    void savePlatformFeedback(UUID id, UUID userId, String category, String message, String contactEmail, Instant createdAt);

    Set<String> getUserRoles(UUID userId);
    void assignUserRole(UUID id, UUID userId, String roleCode, Instant grantedAt);
    List<StudioMemberRecord> getStudioMemberships(UUID userId);

    void createStudio(UUID id, String name, String slug, UUID ownerId, String status);
    UUID findStudioIdBySlug(String slug);
    void addStudioMember(UUID id, UUID studioId, UUID userId, String role);

    void saveOidcTransaction(com.interior.platform.security.domain.OidcTransaction transaction);
    Optional<com.interior.platform.security.domain.OidcTransaction> consumeOidcTransaction(String state, Instant now);

    void recordAuditEvent(UUID id, UUID studioId, UUID actorId, String action, String resourceType,
                          UUID resourceId, String requestId, String details, Instant timestamp);

    void updateSessionMfaVerified(UUID sessionId, Instant mfaVerifiedAt, String assurance);
    void revokeAllOtherUserSessions(UUID userId, UUID currentSessionId, Instant revokedAt);

    Optional<com.interior.platform.security.mfa.MfaMethodRecord> findMfaMethodByUserId(UUID userId, String methodType);
    void saveMfaMethod(com.interior.platform.security.mfa.MfaMethodRecord record);
    void confirmMfaMethod(UUID userId, String methodType, Instant confirmedAt);
    void disableMfaMethod(UUID userId, String methodType);
    void recordFailedMfaAttempt(UUID userId, String methodType, int failedAttempts, Instant lockedUntil);
    void resetFailedMfaAttempts(UUID userId, String methodType, long lastUsedCounter);

    void saveRecoveryCodes(UUID userId, List<String> codeHashes);
    int countUnusedRecoveryCodes(UUID userId);
    boolean redeemRecoveryCode(UUID userId, String codeHash, Instant usedAt);
    void deleteRecoveryCodes(UUID userId);
}
