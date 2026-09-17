package com.interior.platform.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionSecurityServiceTest {

    private SessionSecurityService sessionSecurityService;

    @BeforeEach
    void setUp() {
        sessionSecurityService = new SessionSecurityService();
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
}
