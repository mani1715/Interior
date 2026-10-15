package com.interior.platform.security.oidc;

import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.OidcTransaction;
import com.interior.platform.security.service.OidcService;

/**
 * Service contract for parsing and cryptographically verifying OIDC ID tokens.
 */
public interface IdTokenValidator {

    /**
     * Validates cryptographic signature, header algorithm, claims (issuer, audience,
     * expiry, nonce, email verification) and returns parsed representation.
     */
    OidcService.ParsedIdToken validateAndParse(
            String rawIdToken,
            OidcTransaction tx,
            AuthSecurityProperties.OidcProviderProperties provider
    );
}
