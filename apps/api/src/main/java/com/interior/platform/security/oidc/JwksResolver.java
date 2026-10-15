package com.interior.platform.security.oidc;

import java.security.PublicKey;

/**
 * Contract for fetching and caching JSON Web Key Sets (JWKS) to verify ID token signatures.
 */
public interface JwksResolver {

    /**
     * Resolves an RSA public key by its Key ID (`kid`) from the specified JWKS endpoint.
     * Caches keys in-memory and refreshes on unknown `kid` to handle key rotation.
     *
     * @param jwkSetUri URL to JWKS endpoint
     * @param kid       Key ID from JWT header
     * @return RSA public key
     */
    PublicKey resolvePublicKey(String jwkSetUri, String kid);

    /**
     * Clears cached keys for the given endpoint.
     */
    void invalidateCache(String jwkSetUri);
}
