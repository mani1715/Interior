package com.interior.platform.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionSecurityServiceTest {

    private SessionSecurityService sessionSecurityService;
    private com.interior.platform.security.repository.SecurityRepository securityRepository;
    private java.time.Clock clock;
    private com.interior.platform.security.config.AuthSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new com.interior.platform.security.config.AuthSecurityProperties();
        securityRepository = org.mockito.Mockito.mock(com.interior.platform.security.repository.SecurityRepository.class);
        clock = java.time.Clock.fixed(java.time.Instant.parse("2026-09-18T10:00:00Z"), java.time.ZoneOffset.UTC);
        sessionSecurityService = new SessionSecurityService(properties, securityRepository, clock);
    }

    @Test
    void testOpaqueSessionTokenGeneration() {
        String token1 = sessionSecurityService.generateOpaqueSessionToken();
        String token2 = sessionSecurityService.generateOpaqueSessionToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() >= 32);
    }

    @Test
    void testTokenHashingIsNotPlaintext() {
        String rawToken = sessionSecurityService.generateOpaqueSessionToken();
        byte[] hash1 = sessionSecurityService.hashToken(rawToken);
        byte[] hash2 = sessionSecurityService.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(32, hash1.length); // SHA-256 is 32 bytes
        assertArrayEquals(hash1, hash2);
    }

    @Test
    void testDifferentTokensProduceDifferentHashes() {
        String token1 = "session-token-alpha";
        String token2 = "session-token-beta";

        byte[] hash1 = sessionSecurityService.hashToken(token1);
        byte[] hash2 = sessionSecurityService.hashToken(token2);

        assertFalse(java.util.Arrays.equals(hash1, hash2));
    }

    @Test
    void testCsrfGenerationAndConstantTimeVerification() {
        String csrfToken = sessionSecurityService.generateCsrfToken();
        assertNotNull(csrfToken);
        byte[] csrfHash = sessionSecurityService.hashToken(csrfToken);

        assertTrue(sessionSecurityService.verifyCsrfToken(csrfToken, csrfHash));
        assertFalse(sessionSecurityService.verifyCsrfToken("tampered-token", csrfHash));
        assertFalse(sessionSecurityService.verifyCsrfToken(null, csrfHash));
    }

    @Test
    void testSessionCreationCalculatesCorrectExpiry() {
        java.util.UUID userId = java.util.UUID.randomUUID();
        var result = sessionSecurityService.createAndPersistSession(userId, "MFA", "TestDevice", null);

        assertNotNull(result.rawSessionToken());
        assertNotNull(result.rawCsrfToken());
        assertEquals(userId, result.sessionRecord().userId());
        assertEquals("MFA", result.sessionRecord().assurance());

        // Default idle is 1800s, absolute is 43200s
        assertEquals(clock.instant().plusSeconds(1800), result.sessionRecord().idleExpiresAt());
        assertEquals(clock.instant().plusSeconds(43200), result.sessionRecord().absoluteExpiresAt());
    }

    @Test
    void testValidateSessionRejectsExpiredSession() {
        String rawToken = "sample-test-token";
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        java.util.UUID userId = java.util.UUID.randomUUID();

        // Expired 10 minutes ago
        com.interior.platform.security.domain.SessionRecord expiredSession = new com.interior.platform.security.domain.SessionRecord(
                java.util.UUID.randomUUID(),
                userId,
                tokenHash,
                new byte[32],
                clock.instant().minusSeconds(3600),
                "PASSWORD",
                clock.instant().minusSeconds(1000),
                clock.instant().minusSeconds(600), // expired idle
                clock.instant().plusSeconds(3600),
                null,
                "Device"
        );

        org.mockito.Mockito.when(securityRepository.findSessionByTokenHash(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(expiredSession));

        var validated = sessionSecurityService.validateSession(rawToken);
        assertTrue(validated.isEmpty());
    }

    @Test
    void testValidateSessionRejectsRevokedSession() {
        String rawToken = "sample-revoked-token";
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        java.util.UUID userId = java.util.UUID.randomUUID();

        com.interior.platform.security.domain.SessionRecord revokedSession = new com.interior.platform.security.domain.SessionRecord(
                java.util.UUID.randomUUID(),
                userId,
                tokenHash,
                new byte[32],
                clock.instant().minusSeconds(3600),
                "PASSWORD",
                clock.instant().minusSeconds(100),
                clock.instant().plusSeconds(1800),
                clock.instant().plusSeconds(43200),
                clock.instant().minusSeconds(50), // Revoked 50 seconds ago
                "Device"
        );

        org.mockito.Mockito.when(securityRepository.findSessionByTokenHash(tokenHash))
                .thenReturn(java.util.Optional.of(revokedSession));

        var validated = sessionSecurityService.validateSession(rawToken);
        assertTrue(validated.isEmpty());
    }

    @Test
    void testCanonicalAccountStatesEnforcement() {
        String rawToken = "sample-status-token";
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        java.util.UUID userId = java.util.UUID.randomUUID();

        com.interior.platform.security.domain.SessionRecord session = new com.interior.platform.security.domain.SessionRecord(
                java.util.UUID.randomUUID(),
                userId,
                tokenHash,
                new byte[32],
                clock.instant().minusSeconds(60),
                "PASSWORD",
                clock.instant().minusSeconds(30),
                clock.instant().plusSeconds(1800),
                clock.instant().plusSeconds(43200),
                null,
                "Device"
        );

        org.mockito.Mockito.when(securityRepository.findSessionByTokenHash(tokenHash))
                .thenReturn(java.util.Optional.of(session));

        // 1. ACTIVE state -> allowed
        var activeUser = new com.interior.platform.security.domain.UserRecord(userId, "Active", "active@test.com", null, "ACTIVE", clock.instant(), clock.instant(), 0L);
        org.mockito.Mockito.when(securityRepository.findUserById(userId)).thenReturn(java.util.Optional.of(activeUser));
        assertTrue(sessionSecurityService.validateSession(rawToken).isPresent());

        // 2. PENDING state -> rejected
        var pendingUser = new com.interior.platform.security.domain.UserRecord(userId, "Pending", "pending@test.com", null, "PENDING", clock.instant(), clock.instant(), 0L);
        org.mockito.Mockito.when(securityRepository.findUserById(userId)).thenReturn(java.util.Optional.of(pendingUser));
        assertTrue(sessionSecurityService.validateSession(rawToken).isEmpty());

        // 3. SUSPENDED state -> rejected
        var suspendedUser = new com.interior.platform.security.domain.UserRecord(userId, "Suspended", "suspended@test.com", null, "SUSPENDED", clock.instant(), clock.instant(), 0L);
        org.mockito.Mockito.when(securityRepository.findUserById(userId)).thenReturn(java.util.Optional.of(suspendedUser));
        assertTrue(sessionSecurityService.validateSession(rawToken).isEmpty());

        // 4. DELETED state -> rejected
        var deletedUser = new com.interior.platform.security.domain.UserRecord(userId, "Deleted", "deleted@test.com", null, "DELETED", clock.instant(), clock.instant(), 0L);
        org.mockito.Mockito.when(securityRepository.findUserById(userId)).thenReturn(java.util.Optional.of(deletedUser));
        assertTrue(sessionSecurityService.validateSession(rawToken).isEmpty());
    }

    @Test
    void testLastSeenWriteThrottlingPreventsWriteAmplification() {
        String rawToken = "sample-throttling-token";
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        java.util.UUID userId = java.util.UUID.randomUUID();
        java.util.UUID sessionId = java.util.UUID.randomUUID();

        // Last seen 60 seconds ago (configured touch interval is 300s)
        com.interior.platform.security.domain.SessionRecord session = new com.interior.platform.security.domain.SessionRecord(
                sessionId,
                userId,
                tokenHash,
                new byte[32],
                clock.instant().minusSeconds(600),
                "PASSWORD",
                clock.instant().minusSeconds(60), // 60s ago < 300s interval
                clock.instant().plusSeconds(1800),
                clock.instant().plusSeconds(43200),
                null,
                "Device"
        );

        org.mockito.Mockito.when(securityRepository.findSessionByTokenHash(tokenHash))
                .thenReturn(java.util.Optional.of(session));
        var user = new com.interior.platform.security.domain.UserRecord(userId, "User", "u@test.com", null, "ACTIVE", clock.instant(), clock.instant(), 0L);
        org.mockito.Mockito.when(securityRepository.findUserById(userId)).thenReturn(java.util.Optional.of(user));

        // Read 1: within interval -> should NOT write to DB
        sessionSecurityService.validateSession(rawToken);
        org.mockito.Mockito.verify(securityRepository, org.mockito.Mockito.never())
                .updateSessionLastSeen(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        // Read 2: another read within interval -> still NO write
        sessionSecurityService.validateSession(rawToken);
        org.mockito.Mockito.verify(securityRepository, org.mockito.Mockito.never())
                .updateSessionLastSeen(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        // Now advance session last_seen beyond the 300s interval (e.g. 350s ago)
        com.interior.platform.security.domain.SessionRecord agedSession = new com.interior.platform.security.domain.SessionRecord(
                sessionId,
                userId,
                tokenHash,
                new byte[32],
                clock.instant().minusSeconds(1000),
                "PASSWORD",
                clock.instant().minusSeconds(350), // 350s >= 300s
                clock.instant().plusSeconds(1800),
                clock.instant().plusSeconds(43200),
                null,
                "Device"
        );
        org.mockito.Mockito.when(securityRepository.findSessionByTokenHash(tokenHash))
                .thenReturn(java.util.Optional.of(agedSession));

        sessionSecurityService.validateSession(rawToken);
        org.mockito.Mockito.verify(securityRepository, org.mockito.Mockito.times(1))
                .updateSessionLastSeen(org.mockito.ArgumentMatchers.eq(sessionId), org.mockito.ArgumentMatchers.eq(clock.instant()), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testConfigurationDrivenTimeouts() {
        // Custom timeouts: 15m idle (900s), 8h absolute (28800s)
        properties.setSessionIdleTimeoutSeconds(900);
        properties.setSessionAbsoluteTimeoutSeconds(28800);

        java.util.UUID userId = java.util.UUID.randomUUID();
        var result = sessionSecurityService.createAndPersistSession(userId, "PASSWORD", "CustomTimeoutDevice", null);

        assertEquals(clock.instant().plusSeconds(900), result.sessionRecord().idleExpiresAt());
        assertEquals(clock.instant().plusSeconds(28800), result.sessionRecord().absoluteExpiresAt());
    }

    @Test
    void testSessionRevocationIsolation() {
        java.util.UUID sessionIdA = java.util.UUID.randomUUID();
        java.util.UUID sessionIdB = java.util.UUID.randomUUID();
        java.util.UUID userA = java.util.UUID.randomUUID();
        java.util.UUID userB = java.util.UUID.randomUUID();

        // Logout session A
        sessionSecurityService.revokeSession(sessionIdA);
        org.mockito.Mockito.verify(securityRepository).revokeSession(sessionIdA, clock.instant());
        org.mockito.Mockito.verify(securityRepository, org.mockito.Mockito.never()).revokeSession(sessionIdB, clock.instant());

        // Revoke all sessions for User A
        sessionSecurityService.revokeAllUserSessions(userA);
        org.mockito.Mockito.verify(securityRepository).revokeAllUserSessions(userA, clock.instant());
        org.mockito.Mockito.verify(securityRepository, org.mockito.Mockito.never()).revokeAllUserSessions(userB, clock.instant());
    }
}
