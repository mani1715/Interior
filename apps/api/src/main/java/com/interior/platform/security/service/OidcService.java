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

    public List<ProviderInfo> getAvailableProviders() {
        Map<String, AuthSecurityProperties.OidcProviderProperties> configured = properties.getOidc().getProviders();
        if (configured.isEmpty()) {
            return List.of();
        }
        return configured.entrySet().stream()
                .map(entry -> new ProviderInfo(
                        entry.getKey(),
                        formatProviderName(entry.getKey()),
                        isProviderReady(entry.getValue())
                ))
                .toList();
    }

    public boolean isProviderConfigured(String providerId) {
        AuthSecurityProperties.OidcProviderProperties provider = properties.getOidc().getProviders().get(providerId);
        return isProviderReady(provider);
    }

    public AuthorizationResponse initiateLogin(String providerId, String returnUrl, String intentRole) {
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

        // Production provider implementation will exchange code with tokenUri using codeVerifier and validate ID token
        // In the absence of live provider credentials, throw configuration exception
        throw new IllegalStateException("Production OIDC token exchange requires live provider credentials and endpoints");
    }

    /**
     * Resolves an authenticated external identity into a platform user with safe account linking rules.
     */
    public UserRecord resolveExternalUser(String issuer, String subject, String email, String displayName) {
        // 1. Check existing link
        var existingUserOpt = securityRepository.findUserByExternalIdentity(issuer, subject);
        if (existingUserOpt.isPresent()) {
            UserRecord user = existingUserOpt.get();
            if ("SUSPENDED".equals(user.status()) || "DELETED".equals(user.status())) {
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
                        Map.of("issuer", issuer, "subject", subject, "email", email),
                        null,
                        null
                );
                throw new AccessDeniedException("An account with this email already exists. Linking requires explicit verification.");
            }
        }

        // 3. Register new user with baseline role CUSTOMER
        UUID userId = UUID.randomUUID();
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
        securityRepository.linkExternalIdentity(UUID.randomUUID(), userId, issuer, subject, now);

        // Assign baseline CUSTOMER role (least privilege)
        securityRepository.assignUserRole(UUID.randomUUID(), userId, "CUSTOMER", now);

        auditService.record(
                userId,
                null,
                "USER_REGISTERED_OIDC",
                "USER",
                userId.toString(),
                Map.of("issuer", issuer, "subject", subject),
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
        // Prevent open redirect (must start with / and not // or /\)
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.startsWith("/\\")) {
            return "/";
        }
        // Prevent CR/LF header injection
        if (trimmed.contains("\r") || trimmed.contains("\n")) {
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
