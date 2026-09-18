package com.interior.platform.security.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedirectSanitizerTest {

    @Test
    @DisplayName("Valid internal relative paths are preserved")
    void testValidRelativePaths() {
        assertEquals("/account", OidcService.sanitizeReturnUrl("/account"));
        assertEquals("/projects/modern-villa", OidcService.sanitizeReturnUrl("/projects/modern-villa"));
        assertEquals("/account/settings?tab=profile", OidcService.sanitizeReturnUrl("/account/settings?tab=profile"));
    }

    @Test
    @DisplayName("Open redirect attempts with external domains are sanitized to root")
    void testOpenRedirectRejection() {
        assertEquals("/", OidcService.sanitizeReturnUrl("https://evil.com"));
        assertEquals("/", OidcService.sanitizeReturnUrl("http://attacker.com/steal"));
        assertEquals("/", OidcService.sanitizeReturnUrl("//evil.com"));
        assertEquals("/", OidcService.sanitizeReturnUrl("//evil.com/path"));
        assertEquals("/", OidcService.sanitizeReturnUrl("/\\evil.com"));
        assertEquals("/", OidcService.sanitizeReturnUrl("javascript:alert(1)"));
        assertEquals("/", OidcService.sanitizeReturnUrl(null));
        assertEquals("/", OidcService.sanitizeReturnUrl("   "));
    }

    @Test
    @DisplayName("CRLF header injection attempts are sanitized to root")
    void testCrlfInjectionRejection() {
        assertEquals("/", OidcService.sanitizeReturnUrl("/account\r\nSet-Cookie: evil=1"));
        assertEquals("/", OidcService.sanitizeReturnUrl("/account\nLocation: https://evil.com"));
    }
}
