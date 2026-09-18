package com.interior.platform.security.domain;

import java.time.Instant;

public record OidcTransaction(
    String state,
    String nonce,
    String codeVerifier,
    String providerId,
    String returnUrl,
    String intentRole,
    Instant createdAt,
    Instant expiresAt
) {
    public OidcTransaction(
        String state,
        String nonce,
        String codeVerifier,
        String returnUrl,
        String intentRole,
        Instant expiresAt
    ) {
        this(state, nonce, codeVerifier, "oidc", returnUrl, intentRole, Instant.now(), expiresAt);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
