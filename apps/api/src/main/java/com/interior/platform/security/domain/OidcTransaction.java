package com.interior.platform.security.domain;

import java.time.Instant;

public record OidcTransaction(
    String state,
    String nonce,
    String codeVerifier,
    String providerId,
    String returnUrl,
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
        this(state, nonce, codeVerifier, intentRole, returnUrl, Instant.now(), expiresAt);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
