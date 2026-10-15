package com.interior.platform.security.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.OidcTransaction;
import com.interior.platform.security.service.OidcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Standard implementation of OIDC ID token validation adhering to OpenID Connect Core 1.0 and RFC 7519.
 * Validates cryptographic RS256 signature, issuer, audience, expiration, nonce, and email verification.
 */
@Component
public class DefaultIdTokenValidator implements IdTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(DefaultIdTokenValidator.class);
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 60L;

    private final JwksResolver jwksResolver;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public DefaultIdTokenValidator() {
        this(new DefaultJwksResolver(), Clock.systemUTC(), new ObjectMapper());
    }

    public DefaultIdTokenValidator(JwksResolver jwksResolver) {
        this(jwksResolver, Clock.systemUTC(), new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultIdTokenValidator(JwksResolver jwksResolver, Clock clock, ObjectMapper objectMapper) {
        this.jwksResolver = jwksResolver != null ? jwksResolver : new DefaultJwksResolver();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public OidcService.ParsedIdToken validateAndParse(
            String rawIdToken,
            OidcTransaction tx,
            AuthSecurityProperties.OidcProviderProperties provider
    ) {
        if (rawIdToken == null || rawIdToken.isBlank()) {
            throw new AccessDeniedException("ID token is missing");
        }

        String[] parts = rawIdToken.trim().split("\\.");
        if (parts.length != 3) {
            throw new AccessDeniedException("Malformed OIDC ID token structure (expected 3 parts)");
        }

        // 1. Decode and validate header
        JsonNode headerNode;
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            headerNode = objectMapper.readTree(headerBytes);
        } catch (Exception e) {
            throw new AccessDeniedException("Failed to parse OIDC ID token header: " + e.getMessage());
        }

        String alg = headerNode.has("alg") ? headerNode.get("alg").asText() : "";
        if (!"RS256".equalsIgnoreCase(alg)) {
            throw new AccessDeniedException("Unsupported or insecure OIDC algorithm: " + alg + ". Only RS256 is permitted.");
        }

        String kid = headerNode.has("kid") ? headerNode.get("kid").asText() : null;
        if (kid == null || kid.isBlank()) {
            throw new AccessDeniedException("Key ID (kid) is missing from OIDC ID token header");
        }

        // 2. Decode payload
        JsonNode payloadNode;
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            payloadNode = objectMapper.readTree(payloadBytes);
        } catch (Exception e) {
            throw new AccessDeniedException("Failed to parse OIDC ID token payload: " + e.getMessage());
        }

        // 3. Cryptographic Signature Verification
        if (provider.getJwkSetUri() != null && !provider.getJwkSetUri().isBlank()) {
            PublicKey publicKey = jwksResolver.resolvePublicKey(provider.getJwkSetUri(), kid);
            try {
                byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
                byte[] signedData = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);

                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(publicKey);
                sig.update(signedData);

                if (!sig.verify(signatureBytes)) {
                    log.warn("ID token cryptographic signature verification failed for kid '{}'", kid);
                    throw new AccessDeniedException("Invalid OIDC ID token cryptographic signature");
                }
            } catch (AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                log.error("Cryptographic signature check encountered internal error: {}", e.getMessage());
                throw new AccessDeniedException("OIDC token cryptographic verification error: " + e.getMessage());
            }
        }

        // 4. Claims Validation
        String iss = payloadNode.has("iss") ? payloadNode.get("iss").asText() : null;
        if (iss == null || !isIssuerMatching(iss, provider.getIssuer())) {
            throw new AccessDeniedException("OIDC token validation failed: issuer mismatch (expected " + provider.getIssuer() + ", got " + iss + ")");
        }

        String aud = payloadNode.has("aud") ? payloadNode.get("aud").asText() : null;
        if (aud == null || !aud.equals(provider.getClientId())) {
            throw new AccessDeniedException("OIDC token validation failed: audience mismatch (expected " + provider.getClientId() + ", got " + aud + ")");
        }

        Instant now = clock.instant();

        long expSeconds = payloadNode.has("exp") ? payloadNode.get("exp").asLong() : 0L;
        Instant expiresAt = Instant.ofEpochSecond(expSeconds);
        if (now.isAfter(expiresAt.plusSeconds(ALLOWED_CLOCK_SKEW_SECONDS))) {
            throw new AccessDeniedException("OIDC token validation failed: token has expired");
        }

        long iatSeconds = payloadNode.has("iat") ? payloadNode.get("iat").asLong() : 0L;
        Instant issuedAt = Instant.ofEpochSecond(iatSeconds);
        if (now.isBefore(issuedAt.minusSeconds(ALLOWED_CLOCK_SKEW_SECONDS))) {
            throw new AccessDeniedException("OIDC token validation failed: token issued in the future");
        }

        String nonce = payloadNode.has("nonce") ? payloadNode.get("nonce").asText() : null;
        if (tx.nonce() != null) {
            if (nonce == null || !nonce.equals(tx.nonce())) {
                throw new AccessDeniedException("OIDC token validation failed: nonce mismatch");
            }
        }

        String sub = payloadNode.has("sub") ? payloadNode.get("sub").asText() : null;
        if (sub == null || sub.isBlank()) {
            throw new AccessDeniedException("OIDC token validation failed: missing subject (sub) claim");
        }

        String email = payloadNode.has("email") ? payloadNode.get("email").asText() : null;
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("OIDC token validation failed: missing email claim");
        }

        if (payloadNode.has("email_verified")) {
            boolean emailVerified = payloadNode.get("email_verified").asBoolean();
            if (!emailVerified) {
                throw new AccessDeniedException("OIDC email address is not verified by provider");
            }
        }

        String name = payloadNode.has("name") ? payloadNode.get("name").asText() : null;
        String acr = payloadNode.has("acr") ? payloadNode.get("acr").asText() : null;

        List<String> amr = new ArrayList<>();
        if (payloadNode.has("amr") && payloadNode.get("amr").isArray()) {
            for (JsonNode m : payloadNode.get("amr")) {
                amr.add(m.asText());
            }
        }

        return new OidcService.ParsedIdToken(
                iss,
                aud,
                sub,
                nonce,
                expiresAt,
                issuedAt,
                email,
                name,
                acr,
                amr
        );
    }

    private boolean isIssuerMatching(String tokenIssuer, String configuredIssuer) {
        if (tokenIssuer == null || configuredIssuer == null) {
            return false;
        }
        if (tokenIssuer.equals(configuredIssuer)) {
            return true;
        }
        // Google standard compatibility: "accounts.google.com" vs "https://accounts.google.com"
        String normToken = tokenIssuer.replaceFirst("^https?://", "").replaceAll("/$", "");
        String normConfig = configuredIssuer.replaceFirst("^https?://", "").replaceAll("/$", "");
        return normToken.equals(normConfig);
    }
}
