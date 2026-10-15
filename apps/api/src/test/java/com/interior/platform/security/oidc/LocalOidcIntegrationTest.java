package com.interior.platform.security.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.OidcTransaction;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.OidcService;
import com.interior.platform.security.service.OidcTransactionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LocalOidcIntegrationTest {

    private MockOidcServer mockServer;
    private AuthSecurityProperties properties;
    private SecurityRepository securityRepository;
    private AuditService auditService;
    private Clock clock;
    private OidcTransactionStore transactionStore;
    private OidcService oidcService;

    // In-memory simulation of securityRepository transaction & identity tables
    private final Map<String, OidcTransaction> storedTransactions = new ConcurrentHashMap<>();
    private final Map<String, UserRecord> usersByExternalIdentity = new ConcurrentHashMap<>();
    private final Map<String, UserRecord> usersByEmail = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockOidcServer();
        clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);

        properties = new AuthSecurityProperties();
        var provider = new AuthSecurityProperties.OidcProviderProperties();
        provider.setIssuer(mockServer.getIssuer());
        provider.setClientId("test-elegance-client");
        provider.setClientSecret("test-elegance-secret-xyz");
        provider.setAuthorizationUri(mockServer.getAuthorizationUri());
        provider.setTokenUri(mockServer.getTokenUri());
        provider.setJwkSetUri(mockServer.getJwkSetUri());
        provider.setRedirectUri("http://127.0.0.1:8080/api/v1/auth/callback");
        provider.setScopes(List.of("openid", "email", "profile"));
        properties.getOidc().getProviders().put("mock", provider);

        securityRepository = mock(SecurityRepository.class);

        // Transaction store mock logic
        doAnswer(inv -> {
            OidcTransaction tx = inv.getArgument(0);
            storedTransactions.put(tx.state(), tx);
            return null;
        }).when(securityRepository).saveOidcTransaction(any());

        doAnswer(inv -> {
            String state = inv.getArgument(0);
            OidcTransaction tx = storedTransactions.remove(state);
            return Optional.ofNullable(tx);
        }).when(securityRepository).consumeOidcTransaction(any(), any());

        // Identity query mock logic
        doAnswer(inv -> {
            String issuer = inv.getArgument(0);
            String subject = inv.getArgument(1);
            return Optional.ofNullable(usersByExternalIdentity.get(issuer + "|" + subject));
        }).when(securityRepository).findUserByExternalIdentity(any(), any());

        doAnswer(inv -> {
            String email = inv.getArgument(0);
            return Optional.ofNullable(usersByEmail.get(email));
        }).when(securityRepository).findUserByEmail(any());

        doAnswer(inv -> {
            UserRecord u = inv.getArgument(0);
            usersByEmail.put(u.email(), u);
            return null;
        }).when(securityRepository).createUser(any());

        doAnswer(inv -> {
            UUID userId = inv.getArgument(1);
            String issuer = inv.getArgument(2);
            String subject = inv.getArgument(3);
            UserRecord u = usersByEmail.values().stream().filter(usr -> usr.id().equals(userId)).findFirst().orElse(null);
            if (u != null) {
                usersByExternalIdentity.put(issuer + "|" + subject, u);
            }
            return null;
        }).when(securityRepository).linkExternalIdentity(any(), any(), any(), any(), any());

        auditService = new AuditService(securityRepository, new ObjectMapper());
        transactionStore = new OidcTransactionStore(securityRepository, clock);

        var tokenClient = new DefaultOidcTokenClient(new ObjectMapper());
        var jwksResolver = new DefaultJwksResolver(new ObjectMapper(), clock);
        var idTokenValidator = new DefaultIdTokenValidator(jwksResolver, clock, new ObjectMapper());

        oidcService = new OidcService(
                properties,
                transactionStore,
                securityRepository,
                auditService,
                clock,
                tokenClient,
                idTokenValidator
        );
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.close();
        }
    }

    @Test
    @DisplayName("Happy path: Full OIDC login creates customer session with verified cryptographic signature")
    void testFullOidcFlow_Success_CustomerLogin() {
        var authResponse = oidcService.initiateLogin("mock", "/portfolio", "CUSTOMER");
        assertNotNull(authResponse);
        assertNotNull(authResponse.state());
        assertTrue(authResponse.authorizationUrl().contains("client_id=test-elegance-client"));
        assertTrue(authResponse.authorizationUrl().contains("code_challenge_method=S256"));

        OidcTransaction savedTx = storedTransactions.get(authResponse.state());
        assertNotNull(savedTx);

        // Mock provider produces valid ID token
        String validIdToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-customer-101",
                savedTx.nonce(),
                "customer101@example.com",
                "Elégance Customer",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-access-token\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"id_token\":\"" + validIdToken + "\"}");

        var result = oidcService.handleCallback("auth-code-valid-1", authResponse.state());
        assertNotNull(result);
        assertEquals("/portfolio", result.returnUrl());
        assertEquals("PASSWORD", result.assurance());
        assertEquals("customer101@example.com", result.user().email());
        assertEquals("ACTIVE", result.user().status());

        // Verify baseline role CUSTOMER assigned (least privilege)
        verify(securityRepository).assignUserRole(any(), eq(result.user().id()), eq("CUSTOMER"), any());
        verify(securityRepository).linkExternalIdentity(any(), eq(result.user().id()), eq(mockServer.getIssuer()), eq("sub-customer-101"), any());
    }

    @Test
    @DisplayName("Key rotation: Validates ID token signed by newly rotated key without application restart")
    void testFullOidcFlow_KeyRotation_Success() {
        var authResponse = oidcService.initiateLogin("mock", "/workspace", "DESIGNER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        // Enable key rotation on mock server so JWKS serves kid2
        mockServer.enableKeyRotation();

        // Sign token with kid2
        String rotatedIdToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-designer-202",
                savedTx.nonce(),
                "designer202@example.com",
                "Studio Designer",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid2(),
                mockServer.getKeyPair2()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + rotatedIdToken + "\"}");

        var result = oidcService.handleCallback("code-rot", authResponse.state());
        assertNotNull(result);
        assertEquals("designer202@example.com", result.user().email());
    }

    @Test
    @DisplayName("Security: Rejects ID token with tampered cryptographic signature")
    void testFullOidcFlow_TamperedSignature_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String validIdToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-attacker-999",
                savedTx.nonce(),
                "attacker@example.com",
                "Attacker",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        // Tamper with the signature bytes at the end
        String tamperedToken = validIdToken.substring(0, validIdToken.length() - 8) + "Tampered";
        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + tamperedToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-tampered", authResponse.state()));
        assertTrue(ex.getMessage().contains("signature"));
    }

    @Test
    @DisplayName("Security: Rejects ID token with unknown key ID")
    void testFullOidcFlow_UnknownKid_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String unknownKidToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-unknown-kid",
                savedTx.nonce(),
                "unknown@example.com",
                "Unknown Kid",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                "unregistered-rogue-kid",
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + unknownKidToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-kid", authResponse.state()));
        assertTrue(ex.getMessage().contains("unknown key ID"));
    }

    @Test
    @DisplayName("Security: Rejects ID token when issuer does not match configured provider")
    void testFullOidcFlow_IssuerMismatch_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String rogueIssuerToken = mockServer.createSignedIdToken(
                "https://rogue-idp.example.com",
                "test-elegance-client",
                "sub-rogue",
                savedTx.nonce(),
                "rogue@example.com",
                "Rogue User",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + rogueIssuerToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-rogue", authResponse.state()));
        assertTrue(ex.getMessage().contains("issuer mismatch"));
    }

    @Test
    @DisplayName("Security: Rejects ID token when audience does not match configured clientId")
    void testFullOidcFlow_AudienceMismatch_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String foreignAudToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "foreign-mobile-app",
                "sub-foreign",
                savedTx.nonce(),
                "foreign@example.com",
                "Foreign App User",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + foreignAudToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-foreign", authResponse.state()));
        assertTrue(ex.getMessage().contains("audience mismatch"));
    }

    @Test
    @DisplayName("Security: Rejects expired ID token")
    void testFullOidcFlow_ExpiredToken_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        // Expired 5 minutes ago
        String expiredToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-expired",
                savedTx.nonce(),
                "expired@example.com",
                "Expired User",
                true,
                clock.instant().minusSeconds(300),
                clock.instant().minusSeconds(3600),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + expiredToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-exp", authResponse.state()));
        assertTrue(ex.getMessage().contains("token has expired"));
    }

    @Test
    @DisplayName("Security: Rejects ID token when nonce does not match session transaction")
    void testFullOidcFlow_NonceMismatch_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");

        String wrongNonceToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-wrong-nonce",
                "wrong-nonce-value-999",
                "wrongnonce@example.com",
                "Wrong Nonce",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + wrongNonceToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-nonce", authResponse.state()));
        assertTrue(ex.getMessage().contains("nonce mismatch"));
    }

    @Test
    @DisplayName("Security: Rejects unverified email from identity provider")
    void testFullOidcFlow_UnverifiedEmail_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String unverifiedEmailToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-unverified",
                savedTx.nonce(),
                "unverified@example.com",
                "Unverified Email User",
                false, // email_verified: false
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"id_token\":\"" + unverifiedEmailToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-unverified", authResponse.state()));
        assertTrue(ex.getMessage().contains("email address is not verified"));
    }

    @Test
    @DisplayName("Resilience: Gracefully handles provider token endpoint 400 Bad Request error")
    void testFullOidcFlow_ProviderEndpointError_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");

        mockServer.setTokenResponseOverride(400, "{\"error\":\"invalid_grant\",\"error_description\":\"Code has expired\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-err", authResponse.state()));
        assertTrue(ex.getMessage().contains("token exchange failed"));
    }

    @Test
    @DisplayName("Resilience: Gracefully handles token endpoint timeout")
    void testFullOidcFlow_ProviderTimeout_Rejected() {
        properties.getOidc().getProviders().get("mock").setReadTimeoutMs(150);
        mockServer.setTokenDelayMs(800); // 800ms delay triggers 150ms timeout

        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                oidcService.handleCallback("code-timeout", authResponse.state()));
        assertTrue(ex.getMessage().contains("communication failure"));
    }

    @Test
    @DisplayName("Security: Rejects replayed transaction state (single-use defense)")
    void testFullOidcFlow_ReplayedState_Rejected() {
        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String validIdToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-single-use",
                savedTx.nonce(),
                "singleuse@example.com",
                "Single Use User",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"token\",\"token_type\":\"Bearer\",\"id_token\":\"" + validIdToken + "\"}");

        // First callback succeeds
        oidcService.handleCallback("code-1", authResponse.state());

        // Replay fails
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-1", authResponse.state()));
        assertTrue(ex.getMessage().contains("previously used"));
    }

    @Test
    @DisplayName("Security: Prevents silent account takeover when OIDC email collides with existing local account")
    void testFullOidcFlow_EmailCollision_PreventTakeover() {
        UUID existingUserId = UUID.randomUUID();
        UserRecord existingUser = new UserRecord(
                existingUserId,
                "Original Owner",
                "owner@example.com",
                null,
                "ACTIVE",
                clock.instant().minusSeconds(86400),
                clock.instant().minusSeconds(86400),
                0L
        );
        usersByEmail.put("owner@example.com", existingUser);

        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        // Malicious or new external identity claiming owner@example.com
        String collisedToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-colliding-attacker",
                savedTx.nonce(),
                "owner@example.com",
                "Impostor",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"token\",\"token_type\":\"Bearer\",\"id_token\":\"" + collisedToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-col", authResponse.state()));
        assertTrue(ex.getMessage().contains("An account with this email already exists"));

        // Audit collision rejected
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

    @Test
    @DisplayName("Account lifecycle: Suspended account is denied entry upon OIDC authentication")
    void testFullOidcFlow_SuspendedAccount_Denied() {
        UUID suspendedUserId = UUID.randomUUID();
        UserRecord suspendedUser = new UserRecord(
                suspendedUserId,
                "Suspended Designer",
                "suspended@example.com",
                null,
                "SUSPENDED", // Suspended!
                clock.instant().minusSeconds(86400),
                clock.instant().minusSeconds(86400),
                0L
        );
        usersByExternalIdentity.put(mockServer.getIssuer() + "|sub-suspended-user", suspendedUser);

        var authResponse = oidcService.initiateLogin("mock", "/workspace", "DESIGNER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String validToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-suspended-user",
                savedTx.nonce(),
                "suspended@example.com",
                "Suspended Designer",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"token\",\"token_type\":\"Bearer\",\"id_token\":\"" + validToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-susp", authResponse.state()));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    @DisplayName("Account lifecycle: Permanently deleted account is denied entry upon OIDC authentication")
    void testFullOidcFlow_DeletedAccount_Denied() {
        UUID deletedUserId = UUID.randomUUID();
        UserRecord deletedUser = new UserRecord(
                deletedUserId,
                "Deleted User",
                "deleted@example.com",
                null,
                "DELETED", // Permanently closed
                clock.instant().minusSeconds(86400),
                clock.instant().minusSeconds(86400),
                0L
        );
        usersByExternalIdentity.put(mockServer.getIssuer() + "|sub-deleted-user", deletedUser);

        var authResponse = oidcService.initiateLogin("mock", "/account", "CUSTOMER");
        OidcTransaction savedTx = storedTransactions.get(authResponse.state());

        String validToken = mockServer.createSignedIdToken(
                mockServer.getIssuer(),
                "test-elegance-client",
                "sub-deleted-user",
                savedTx.nonce(),
                "deleted@example.com",
                "Deleted User",
                true,
                clock.instant().plusSeconds(3600),
                clock.instant(),
                mockServer.getKid1(),
                mockServer.getKeyPair1()
        );

        mockServer.setTokenResponseOverride(200, "{\"access_token\":\"token\",\"token_type\":\"Bearer\",\"id_token\":\"" + validToken + "\"}");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                oidcService.handleCallback("code-del", authResponse.state()));
        assertTrue(ex.getMessage().contains("ACCOUNT_PERMANENTLY_DELETED"));
    }

    @Test
    @DisplayName("Open redirect defense: Unsafe returnUrl values are safely sanitized to root destination")
    void testReturnUrlSanitization() {
        assertEquals("/", OidcService.sanitizeReturnUrl("https://evil.example.com/steal"));
        assertEquals("/", OidcService.sanitizeReturnUrl("//evil.example.com"));
        assertEquals("/", OidcService.sanitizeReturnUrl("/\\evil.example.com"));
        assertEquals("/", OidcService.sanitizeReturnUrl("javascript:alert(1)"));
        assertEquals("/", OidcService.sanitizeReturnUrl("data:text/html;base64,PHNjcmlwdD4="));
        assertEquals("/", OidcService.sanitizeReturnUrl("/workspace\r\nSet-Cookie: evil=1"));
        assertEquals("/", OidcService.sanitizeReturnUrl("/path%2fevil"));
        assertEquals("/", OidcService.sanitizeReturnUrl("/path%5cevil"));

        // Valid safe relative application paths
        assertEquals("/account", OidcService.sanitizeReturnUrl("/account"));
        assertEquals("/workspace/projects", OidcService.sanitizeReturnUrl("/workspace/projects"));
        assertEquals("/gallery?style=modern", OidcService.sanitizeReturnUrl("/gallery?style=modern"));
    }
}
