package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.OidcTransaction;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OidcSecurityTest {

    private OidcTransactionStore transactionStore;
    private SecurityRepository securityRepository;
    private AuditService auditService;
    private AuthSecurityProperties properties;
    private Clock clock;
    private OidcService oidcService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC);
        securityRepository = mock(SecurityRepository.class);
        transactionStore = new OidcTransactionStore(securityRepository, clock);
        auditService = new AuditService(securityRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        properties = new AuthSecurityProperties();
        oidcService = new OidcService(properties, transactionStore, securityRepository, auditService, clock);
    }

    @Test
    @DisplayName("Transaction store saves to database repository and delegates atomic take")
    void testTransactionStoreDatabaseDelegation() {
        OidcTransaction tx = new OidcTransaction(
                "state-123",
                "nonce-456",
                "verifier-789",
                "/account",
                "CUSTOMER",
                clock.instant().plusSeconds(300)
        );

        transactionStore.save(tx);
        verify(securityRepository).saveOidcTransaction(tx);

        when(securityRepository.consumeOidcTransaction("state-123", clock.instant()))
                .thenReturn(Optional.of(tx))
                .thenReturn(Optional.empty()); // Replay returns empty

        Optional<OidcTransaction> first = transactionStore.take("state-123");
        assertTrue(first.isPresent());
        assertEquals("nonce-456", first.get().nonce());

        // Replay attempt fails
        Optional<OidcTransaction> second = transactionStore.take("state-123");
        assertTrue(second.isEmpty());
    }

    @Test
    @DisplayName("Transaction store returns empty for unknown or null state")
    void testUnknownStateReturnsEmpty() {
        when(securityRepository.consumeOidcTransaction(any(), any())).thenReturn(Optional.empty());

        assertTrue(transactionStore.take("non-existent-state").isEmpty());
        assertTrue(transactionStore.take(null).isEmpty());
        assertTrue(transactionStore.take("   ").isEmpty());
    }

    @Test
    @DisplayName("ID token claims validation succeeds for valid matching token")
    void testValidateIdTokenSuccess() {
        AuthSecurityProperties.OidcProviderProperties provider = new AuthSecurityProperties.OidcProviderProperties();
        provider.setIssuer("https://accounts.google.com");
        provider.setClientId("client-app-id");

        OidcTransaction tx = new OidcTransaction("s1", "nonce-xyz", "v1", "/account", "CUSTOMER", clock.instant().plusSeconds(300));
        OidcService.ParsedIdToken token = new OidcService.ParsedIdToken(
                "https://accounts.google.com",
                "client-app-id",
                "sub-999",
                "nonce-xyz",
                clock.instant().plusSeconds(3600),
                clock.instant(),
                "user@example.com",
                "Valid User",
                null,
                null
        );

        OidcService.OidcClaims claims = oidcService.validateIdTokenClaims(token, tx, provider);
        assertNotNull(claims);
        assertEquals("user@example.com", claims.email());
        assertEquals("PASSWORD", claims.assurance());
    }

    @Test
    @DisplayName("ID token claims validation fails on issuer mismatch")
    void testValidateIdTokenIssuerMismatch() {
        AuthSecurityProperties.OidcProviderProperties provider = new AuthSecurityProperties.OidcProviderProperties();
        provider.setIssuer("https://accounts.google.com");
        provider.setClientId("client-app-id");

        OidcTransaction tx = new OidcTransaction("s1", "nonce-xyz", "v1", "/account", "CUSTOMER", clock.instant().plusSeconds(300));
        OidcService.ParsedIdToken token = new OidcService.ParsedIdToken(
                "https://rogue-idp.com", // Mismatched issuer!
                "client-app-id",
                "sub-999",
                "nonce-xyz",
                clock.instant().plusSeconds(3600),
                clock.instant(),
                "user@example.com",
                "Valid User",
                null,
                null
        );

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> oidcService.validateIdTokenClaims(token, tx, provider));
        assertTrue(ex.getMessage().contains("issuer mismatch"));
    }

    @Test
    @DisplayName("ID token claims validation fails on audience mismatch")
    void testValidateIdTokenAudienceMismatch() {
        AuthSecurityProperties.OidcProviderProperties provider = new AuthSecurityProperties.OidcProviderProperties();
        provider.setIssuer("https://accounts.google.com");
        provider.setClientId("legit-client-id");

        OidcTransaction tx = new OidcTransaction("s1", "nonce-xyz", "v1", "/account", "CUSTOMER", clock.instant().plusSeconds(300));
        OidcService.ParsedIdToken token = new OidcService.ParsedIdToken(
                "https://accounts.google.com",
                "foreign-client-id", // Mismatched audience!
                "sub-999",
                "nonce-xyz",
                clock.instant().plusSeconds(3600),
                clock.instant(),
                "user@example.com",
                "Valid User",
                null,
                null
        );

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> oidcService.validateIdTokenClaims(token, tx, provider));
        assertTrue(ex.getMessage().contains("audience mismatch"));
    }

    @Test
    @DisplayName("ID token claims validation fails on expired token")
    void testValidateIdTokenExpired() {
        AuthSecurityProperties.OidcProviderProperties provider = new AuthSecurityProperties.OidcProviderProperties();
        provider.setIssuer("https://accounts.google.com");
        provider.setClientId("client-app-id");

        OidcTransaction tx = new OidcTransaction("s1", "nonce-xyz", "v1", "/account", "CUSTOMER", clock.instant().plusSeconds(300));
        OidcService.ParsedIdToken token = new OidcService.ParsedIdToken(
                "https://accounts.google.com",
                "client-app-id",
                "sub-999",
                "nonce-xyz",
                clock.instant().minusSeconds(1), // Expired!
                clock.instant().minusSeconds(3600),
                "user@example.com",
                "Valid User",
                null,
                null
        );

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> oidcService.validateIdTokenClaims(token, tx, provider));
        assertTrue(ex.getMessage().contains("token has expired"));
    }

    @Test
    @DisplayName("ID token claims validation fails on nonce mismatch")
    void testValidateIdTokenNonceMismatch() {
        AuthSecurityProperties.OidcProviderProperties provider = new AuthSecurityProperties.OidcProviderProperties();
        provider.setIssuer("https://accounts.google.com");
        provider.setClientId("client-app-id");

        OidcTransaction tx = new OidcTransaction("s1", "nonce-xyz", "v1", "/account", "CUSTOMER", clock.instant().plusSeconds(300));
        OidcService.ParsedIdToken token = new OidcService.ParsedIdToken(
                "https://accounts.google.com",
                "client-app-id",
                "sub-999",
                "different-nonce", // Mismatched nonce!
                clock.instant().plusSeconds(3600),
                clock.instant(),
                "user@example.com",
                "Valid User",
                null,
                null
        );

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> oidcService.validateIdTokenClaims(token, tx, provider));
        assertTrue(ex.getMessage().contains("nonce mismatch"));
    }

    @Test
    @DisplayName("PKCE verification accurately tests SHA-256 code challenge match")
    void testVerifyPkce() throws Exception {
        String verifier = "E9Melhoa2OwvFrGMTJguCH5rtx64KlPUqSTbDtmtPDmM28";
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String validChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        assertTrue(oidcService.verifyPkce(verifier, validChallenge));
        assertFalse(oidcService.verifyPkce(verifier, "invalidChallengeString1234567890"));
        assertFalse(oidcService.verifyPkce(null, validChallenge));
        assertFalse(oidcService.verifyPkce(verifier, null));
    }

    @Test
    @DisplayName("deriveAssurance strictly elevates to MFA/WEBAUTHN only from trusted acr/amr claims")
    void testDeriveAssuranceLevels() {
        assertEquals("MFA", oidcService.deriveAssurance("gold", null));
        assertEquals("MFA", oidcService.deriveAssurance("phr", null));
        assertEquals("WEBAUTHN", oidcService.deriveAssurance(null, List.of("webauthn")));
        assertEquals("WEBAUTHN", oidcService.deriveAssurance(null, List.of("fido")));
        assertEquals("MFA", oidcService.deriveAssurance(null, List.of("otp")));
        assertEquals("MFA", oidcService.deriveAssurance(null, List.of("sms")));
        assertEquals("PASSWORD", oidcService.deriveAssurance(null, List.of("pwd")));
        assertEquals("PASSWORD", oidcService.deriveAssurance(null, null));
        assertEquals("PASSWORD", oidcService.deriveAssurance("unknown_acr", List.of("pin")));
    }

    @Test
    @DisplayName("getAvailableProviders returns empty list when no provider credentials are configured")
    void testUnconfiguredProvidersReturnEmptyList() {
        // Default properties has empty or unconfigured providers
        properties.getOidc().getProviders().clear();
        List<OidcService.ProviderInfo> list = oidcService.getAvailableProviders();
        assertTrue(list.isEmpty());

        // Partially configured provider without client-id or client-secret is also excluded
        AuthSecurityProperties.OidcProviderProperties incomplete = new AuthSecurityProperties.OidcProviderProperties();
        incomplete.setIssuer("https://accounts.google.com");
        // clientId and clientSecret left empty
        properties.getOidc().getProviders().put("google", incomplete);

        assertTrue(oidcService.getAvailableProviders().isEmpty());
    }

    @Test
    @DisplayName("New OIDC registration creates user with baseline CUSTOMER role only")
    void testNewOidcUserGetsCustomerRole() {
        when(securityRepository.findUserByExternalIdentity(any(), any())).thenReturn(Optional.empty());
        when(securityRepository.findUserByEmail(any())).thenReturn(Optional.empty());

        UserRecord createdUser = oidcService.resolveExternalUser(
                "https://accounts.google.com",
                "sub-123456",
                "newcustomer@example.com",
                "New Customer"
        );

        assertNotNull(createdUser);
        assertEquals("newcustomer@example.com", createdUser.email());
        assertEquals("ACTIVE", createdUser.status());

        // Verify CUSTOMER role assigned
        verify(securityRepository).assignUserRole(any(), eq(createdUser.id()), eq("CUSTOMER"), any());
        // Verify external identity linked
        verify(securityRepository).linkExternalIdentity(any(), eq(createdUser.id()), eq("https://accounts.google.com"), eq("sub-123456"), any());
    }

    @Test
    @DisplayName("Email collision with unlinked account prevents silent account takeover")
    void testEmailCollisionPreventsTakeover() {
        UUID existingUserId = UUID.randomUUID();
        UserRecord existingUser = new UserRecord(
                existingUserId,
                "Legitimate Owner",
                "owner@example.com",
                null,
                "ACTIVE",
                clock.instant().minusSeconds(86400),
                clock.instant().minusSeconds(86400),
                0L
        );

        when(securityRepository.findUserByExternalIdentity("https://accounts.google.com", "malicious-sub"))
                .thenReturn(Optional.empty());
        when(securityRepository.findUserByEmail("owner@example.com"))
                .thenReturn(Optional.of(existingUser));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> oidcService.resolveExternalUser(
                        "https://accounts.google.com",
                        "malicious-sub",
                        "owner@example.com",
                        "Attacker"
                )
        );

        assertTrue(exception.getMessage().contains("An account with this email already exists"));

        // Verify audit event recorded in repository
        verify(securityRepository).recordAuditEvent(
                any(),
                isNull(),
                eq(existingUserId),
                eq("AUTH_EMAIL_COLLISION_REJECTED"),
                eq("USER"),
                eq(existingUserId),
                any(),
                any(),
                any()
        );
    }
}
