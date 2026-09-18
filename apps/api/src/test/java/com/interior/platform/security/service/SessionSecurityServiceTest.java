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
}
