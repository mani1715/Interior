package com.interior.platform.security.oidc;

public record OidcTokenResponse(
        String accessToken,
        String idToken,
        String tokenType,
        Long expiresIn
) {}
