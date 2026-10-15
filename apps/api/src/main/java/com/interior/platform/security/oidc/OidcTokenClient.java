package com.interior.platform.security.oidc;

import com.interior.platform.security.config.AuthSecurityProperties;

/**
 * Service contract for exchanging OIDC authorization codes with identity provider token endpoints.
 */
public interface OidcTokenClient {

    /**
     * Exchanges an authorization code and PKCE code verifier for tokens (ID token, access token).
     *
     * @param provider     configured provider properties
     * @param code         authorization code received from provider
     * @param codeVerifier PKCE code verifier matching the authorization request challenge
     * @return token response containing raw ID token
     */
    OidcTokenResponse exchangeAuthorizationCode(
            AuthSecurityProperties.OidcProviderProperties provider,
            String code,
            String codeVerifier
    );
}
