package com.interior.platform.security.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SessionSecurityService {

    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${app.security.session-cookie-name:__Host-session}")
    private String cookieName;

    /**
     * Generates a 256-bit cryptographic random session token (32 bytes).
     */
    public String generateOpaqueSessionToken() {
        byte[] bytes = new byte[32]; // 256 bits of entropy
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes the SHA-256 hash of a session token for secure database storage.
     */
    public byte[] hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a CSRF token bound to a session.
     */
    public String generateCsrfToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Verifies CSRF header against session CSRF hash.
     */
    public boolean verifyCsrfToken(String headerToken, byte[] storedCsrfHash) {
        if (headerToken == null || headerToken.isBlank() || storedCsrfHash == null) {
            return false;
        }
        byte[] headerHash = hashToken(headerToken);
        return MessageDigest.isEqual(headerHash, storedCsrfHash);
    }

    /**
     * Sets the secure HttpOnly session cookie on the response.
     */
    public void attachSessionCookie(HttpServletResponse response, String rawToken, boolean isSecure) {
        Cookie cookie = new Cookie(cookieName, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        // SameSite=Lax via header formatting
        response.addHeader("Set-Cookie", String.format(
            "%s=%s; Path=/; HttpOnly; SameSite=Lax%s",
            cookieName, rawToken, isSecure ? "; Secure" : ""
        ));
    }

    /**
     * Clears the session cookie on logout.
     */
    public void clearSessionCookie(HttpServletResponse response, boolean isSecure) {
        response.addHeader("Set-Cookie", String.format(
            "%s=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax%s",
            cookieName, isSecure ? "; Secure" : ""
        ));
    }
}
