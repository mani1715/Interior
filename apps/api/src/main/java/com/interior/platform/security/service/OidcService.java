package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.OidcTransaction;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OidcService {

    private static final Logger log = LoggerFactory.getLogger(OidcService.class);
    private static final Duration TRANSACTION_TTL = Duration.ofMinutes(5);

    public record ProviderInfo(String id, String displayName, boolean isConfigured) {}
    public record AuthorizationResponse(String authorizationUrl, String state) {}
    public record OidcAuthResult(UserRecord user, String returnUrl, String assurance) {}

    public record ParsedIdToken(
        String issuer,
        String audience,
        String subject,
        String nonce,
        Instant expiresAt,
        Instant issuedAt,
        String email,
        String name,
        String acr,
        List<String> amr
    ) {}

    public record OidcClaims(
        String issuer,
        String subject,
        String email,
        String name,
        String assurance
    ) {}

    private final AuthSecurityProperties properties;
    private final OidcTransactionStore transactionStore;
    private final SecurityRepository securityRepository;
    private final AuditService auditService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public OidcService(
            AuthSecurityProperties properties,
            OidcTransactionStore transactionStore,
            SecurityRepository securityRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.properties = properties;
        this.transactionStore = transactionStore;
        this.securityRepository = securityRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Returns only actually configured OIDC providers. Unconfigured providers are omitted.
     */
    public List<ProviderInfo> getAvailableProviders() {
        Map<String, AuthSecurityProperties.OidcProviderProperties> configured = properties.getOidc().getProviders();
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        return configured.entrySet().stream()
                .filter(entry -> isProviderReady(entry.getValue()))
                .map(entry -> new ProviderInfo(
                        entry.getKey(),
                        formatProviderName(entry.getKey()),
                        true
                ))
                .toList();
    }

    public boolean isProviderConfigured(String providerId) {
        if (properties.getOidc().getProviders() == null) {
            return false;
        }
        AuthSecurityProperties.OidcProviderProperties provider = properties.getOidc().getProviders().get(providerId);
        return isProviderReady(provider);
    }

    public AuthorizationResponse initiateLogin(String providerId, String returnUrl, String intentRole) {
        if (properties.getOidc().getProviders() == null) {
            throw new IllegalStateException("OIDC Provider '" + providerId + "' is not configured on this platform");
        }
        AuthSecurityProperties.OidcProviderProperties provider = properties.getOidc().getProviders().get(providerId);
        if (provider == null || !isProviderReady(provider)) {
            throw new IllegalStateException("OIDC Provider '" + providerId + "' is not configured on this platform");
        }

        String safeReturnUrl = sanitizeReturnUrl(returnUrl);
        String state = generateSecureString(32);
        String nonce = generateSecureString(32);
        String codeVerifier = generateSecureString(32);
        String codeChallenge = generateCodeChallenge(codeVerifier);

        Instant expiresAt = clock.instant().plus(TRANSACTION_TTL);
        OidcTransaction tx = new OidcTransaction(state, nonce, codeVerifier, safeReturnUrl, intentRole, expiresAt);
        transactionStore.save(tx);

        String authUrl = UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", provider.getClientId())
                .queryParam("redirect_uri", provider.getRedirectUri())
                .queryParam("scope", String.join(" ", provider.getScopes()))
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();

        return new AuthorizationResponse(authUrl, state);
    }

    public OidcAuthResult handleCallback(String code, String state) {
        OidcTransaction tx = transactionStore.take(state)
                .orElseThrow(() -> new AccessDeniedException("Invalid, expired, or previously used authentication transaction"));

        if (code == null || code.isBlank()) {
            throw new AccessDeniedException("Authorization code missing from callback");
        }

        // Production provider implementation exchanges code with tokenUri using codeVerifier and validates ID token.
        // In the absence of live provider credentials in Phase 07, throw configuration exception.
        throw new IllegalStateException("Production OIDC token exchange requires live provider credentials and endpoints");
    }

    /**
     * Protocol-level validation of parsed ID token claims against transaction and provider contracts.
     */
    public OidcClaims validateIdTokenClaims(
            ParsedIdToken token,
            OidcTransaction tx,
            AuthSecurityProperties.OidcProviderProperties provider
    ) {
        if (token == null) {
            throw new AccessDeniedException("OIDC token validation failed: token is missing");
        }
        if (provider == null || provider.getIssuer() == null || !provider.getIssuer().equals(token.issuer())) {
            throw new AccessDeniedException("OIDC token validation failed: issuer mismatch");
        }
        if (provider.getClientId() == null || !provider.getClientId().equals(token.audience())) {
            throw new AccessDeniedException("OIDC token validation failed: audience mismatch");
        }
        if (token.expiresAt() == null || token.expiresAt().isBefore(clock.instant())) {
            throw new AccessDeniedException("OIDC token validation failed: token has expired");
        }
        if (tx.nonce() == null || !tx.nonce().equals(token.nonce())) {
            throw new AccessDeniedException("OIDC token validation failed: nonce mismatch");
        }

        String assurance = deriveAssurance(token.acr(), token.amr());
        return new OidcClaims(token.issuer(), token.subject(), token.email(), token.name(), assurance);
    }

    /**
     * Verifies PKCE code challenge match using SHA-256.
     */
    public boolean verifyPkce(String codeVerifier, String codeChallenge) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }
        String computedChallenge = generateCodeChallenge(codeVerifier);
        return computedChallenge.equals(codeChallenge);
    }

    /**
     * Derives authentication assurance level strictly from trusted OIDC acr/amr claims.
     */
    public String deriveAssurance(String acr, List<String> amr) {
        if ("gold".equalsIgnoreCase(acr) || "phr".equalsIgnoreCase(acr)) {
            return "MFA";
        }
        if (amr != null) {
            for (String method : amr) {
                if ("webauthn".equalsIgnoreCase(method) || "fido".equalsIgnoreCase(method)) {
                    return "WEBAUTHN";
                }
                if ("mfa".equalsIgnoreCase(method) || "otp".equalsIgnoreCase(method) || "sms".equalsIgnoreCase(method)) {
                    return "MFA";
                }
            }
        }
        return "PASSWORD";
    }

    /**
     * Resolves an authenticated external identity into a platform user with safe account linking rules.
     * Principle of least privilege: all new accounts receive CUSTOMER baseline role only.
     */
    public UserRecord resolveExternalUser(String issuer, String subject, String email, String displayName) {
        // 1. Check existing link
        var existingUserOpt = securityRepository.findUserByExternalIdentity(issuer, subject);
        if (existingUserOpt.isPresent()) {
            UserRecord user = existingUserOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(user.status())) {
                throw new AccessDeniedException("User account is inactive");
            }
            return user;
        }

        // 2. Check email collision - prevent silent account takeover
        if (email != null && !email.isBlank()) {
            var existingByEmail = securityRepository.findUserByEmail(email);
            if (existingByEmail.isPresent()) {
                auditService.record(
                        existingByEmail.get().id(),
                        null,
                        "AUTH_EMAIL_COLLISION_REJECTED",
                        "USER",
                        existingByEmail.get().id().toString(),
                        Map.of("issuer", issuer, "email", email),
                        null,
                        null
                );
                throw new AccessDeniedException("An account with this email already exists. Linking requires explicit verification.");
            }
        }

        // 3. Register new user with baseline role CUSTOMER (least privilege)
        UUID userId = com.interior.platform.common.util.UuidV7.randomUuid();
        Instant now = clock.instant();
        UserRecord newUser = new UserRecord(
                userId,
                displayName != null && !displayName.isBlank() ? displayName : "User " + userId.toString().substring(0, 8),
                email,
                null,
                "ACTIVE",
                now,
                now,
                0L
        );
        securityRepository.createUser(newUser);

        // Bind external identity
        securityRepository.linkExternalIdentity(com.interior.platform.common.util.UuidV7.randomUuid(), userId, issuer, subject, now);

        // Always assign baseline CUSTOMER role (least privilege)
        securityRepository.assignUserRole(com.interior.platform.common.util.UuidV7.randomUuid(), userId, "CUSTOMER", now);

        auditService.record(
                userId,
                null,
                "USER_REGISTERED_OIDC",
                "USER",
                userId.toString(),
                Map.of("issuer", issuer),
                null,
                null
        );

        return newUser;
    }

    public static String sanitizeReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return "/";
        }
        String trimmed = returnUrl.trim();

        // Reject CRLF header injection
        if (trimmed.contains("\r") || trimmed.contains("\n")) {
            return "/";
        }

        // Must start with single forward slash, and not double slash, backslash, or protocol-relative
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.startsWith("/\\") || trimmed.startsWith("\\")) {
            return "/";
        }

        // Disallow javascript:, data:, backslash, or encoded slashes/backslashes
        String lower = trimmed.toLowerCase();
        if (lower.contains("javascript:") || lower.contains("data:") || lower.contains("\\")
                || lower.contains("%2f") || lower.contains("%5c")) {
            return "/";
        }

        return trimmed;
    }

    private boolean isProviderReady(AuthSecurityProperties.OidcProviderProperties provider) {
        return provider != null
                && provider.getClientId() != null && !provider.getClientId().isBlank()
                && provider.getClientSecret() != null && !provider.getClientSecret().isBlank()
                && provider.getIssuer() != null && !provider.getIssuer().isBlank();
    }

    private String formatProviderName(String id) {
        if (id == null || id.isEmpty()) return "";
        return id.substring(0, 1).toUpperCase() + id.substring(1).toLowerCase();
    }

    private String generateSecureString(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }
}
