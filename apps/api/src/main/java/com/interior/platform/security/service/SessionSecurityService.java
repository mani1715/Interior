package com.interior.platform.security.service;

import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class SessionSecurityService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthSecurityProperties properties;
    private final SecurityRepository securityRepository;
    private final Clock clock;

    public record ValidatedSession(
        SessionRecord session,
        UserRecord user,
        Set<String> roles,
        List<StudioMemberRecord> studioMemberships,
        String csrfToken
    ) {}

    public record SessionCreationResult(
        String rawSessionToken,
        String rawCsrfToken,
        SessionRecord sessionRecord
    ) {}

    public SessionSecurityService(
            AuthSecurityProperties properties,
            SecurityRepository securityRepository,
            Clock clock) {
        this.properties = properties;
        this.securityRepository = securityRepository;
        this.clock = clock;
    }

    public String generateOpaqueSessionToken() {
        byte[] bytes = new byte[32]; // 256 bits of entropy
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public byte[] hashToken(String rawToken) {
        if (rawToken == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public String generateCsrfToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public boolean verifyCsrfToken(String headerToken, byte[] storedCsrfHash) {
        if (headerToken == null || headerToken.isBlank() || storedCsrfHash == null) {
            return false;
        }
        byte[] headerHash = hashToken(headerToken);
        return MessageDigest.isEqual(headerHash, storedCsrfHash);
    }

    public boolean verifyCsrfToken(String headerToken, SessionRecord session) {
        if (session == null) {
            return false;
        }
        return verifyCsrfToken(headerToken, session.csrfHash());
    }

    public SessionCreationResult createAndPersistSession(
            UUID userId,
            String assurance,
            String deviceLabel,
            HttpServletResponse response) {

        Instant now = clock.instant();
        String rawToken = generateOpaqueSessionToken();
        byte[] tokenHash = hashToken(rawToken);

        String rawCsrfToken = generateCsrfToken();
        byte[] csrfHash = hashToken(rawCsrfToken);

        Instant idleExpiresAt = now.plusSeconds(properties.getSessionIdleTimeoutSeconds());
        Instant absoluteExpiresAt = now.plusSeconds(properties.getSessionAbsoluteTimeoutSeconds());

        SessionRecord session = new SessionRecord(
                com.interior.platform.common.util.UuidV7.randomUuid(),
                userId,
                tokenHash,
                csrfHash,
                now,
                assurance != null ? assurance : "PASSWORD",
                now,
                idleExpiresAt,
                absoluteExpiresAt,
                null,
                deviceLabel
        );

        securityRepository.createSession(session);

        if (response != null) {
            attachSessionCookie(response, rawToken);
        }

        return new SessionCreationResult(rawToken, rawCsrfToken, session);
    }

    public Optional<ValidatedSession> validateSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        byte[] tokenHash = hashToken(rawToken);
        Optional<SessionRecord> sessionOpt = securityRepository.findSessionByTokenHash(tokenHash);

        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        SessionRecord session = sessionOpt.get();
        Instant now = clock.instant();

        if (session.isRevoked() || session.isExpired(now)) {
            return Optional.empty();
        }

        Optional<UserRecord> userOpt = securityRepository.findUserById(session.userId());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        UserRecord user = userOpt.get();
        // Only canonical ACTIVE accounts are permitted to hold valid sessions.
        // PENDING, SUSPENDED, and DELETED accounts are rejected.
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            return Optional.empty();
        }

        // Throttled last_seen update to prevent write amplification
        long secondsSinceLastSeen = now.getEpochSecond() - session.lastSeenAt().getEpochSecond();
        if (secondsSinceLastSeen >= properties.getSessionLastSeenUpdateIntervalSeconds()) {
            Instant newIdleExpires = now.plusSeconds(properties.getSessionIdleTimeoutSeconds());
            securityRepository.updateSessionLastSeen(session.id(), now, newIdleExpires);
        }

        Set<String> roles = securityRepository.getUserRoles(user.id());
        List<StudioMemberRecord> studioMemberships = securityRepository.getStudioMemberships(user.id());

        return Optional.of(new ValidatedSession(session, user, roles, studioMemberships, null));
    }

    public SessionCreationResult rotateSession(SessionRecord oldSession, HttpServletResponse response) {
        Instant now = clock.instant();
        securityRepository.revokeSession(oldSession.id(), now);

        return createAndPersistSession(
                oldSession.userId(),
                oldSession.assurance(),
                oldSession.deviceLabel(),
                response
        );
    }

    public void revokeSession(UUID sessionId) {
        securityRepository.revokeSession(sessionId, clock.instant());
    }

    public void revokeAllUserSessions(UUID userId) {
        securityRepository.revokeAllUserSessions(userId, clock.instant());
    }

    public void logout(String rawToken, HttpServletResponse response) {
        if (rawToken != null && !rawToken.isBlank()) {
            byte[] tokenHash = hashToken(rawToken);
            securityRepository.findSessionByTokenHash(tokenHash).ifPresent(s -> {
                revokeSession(s.id());
            });
        }
        if (response != null) {
            clearSessionCookie(response);
        }
    }

    public String refreshSessionCsrfToken(UUID sessionId) {
        String rawCsrf = generateCsrfToken();
        byte[] hash = hashToken(rawCsrf);
        securityRepository.updateSessionCsrfHash(sessionId, hash);
        return rawCsrf;
    }

    public void attachSessionCookie(HttpServletResponse response, String rawToken) {
        String cookieName = properties.getSessionCookieName();
        boolean isSecure = properties.isSessionCookieSecure();

        response.addHeader("Set-Cookie", String.format(
            "%s=%s; Path=/; HttpOnly; SameSite=Lax%s",
            cookieName, rawToken, isSecure ? "; Secure" : ""
        ));
    }

    public void clearSessionCookie(HttpServletResponse response) {
        String cookieName = properties.getSessionCookieName();
        boolean isSecure = properties.isSessionCookieSecure();

        response.addHeader("Set-Cookie", String.format(
            "%s=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax%s",
            cookieName, isSecure ? "; Secure" : ""
        ));
        response.addHeader("Set-Cookie", String.format(
            "XSRF-TOKEN=; Path=/; Max-Age=0; SameSite=Lax%s",
            isSecure ? "; Secure" : ""
        ));
    }
}
