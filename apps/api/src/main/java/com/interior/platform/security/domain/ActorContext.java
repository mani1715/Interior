package com.interior.platform.security.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ActorContext(
    UUID userId,
    String displayName,
    String email,
    String status,
    Set<String> platformRoles,
    Set<String> permissions,
    UUID activeStudioId,
    String activeStudioRole,
    String assurance,
    boolean isAuthenticated,
    Instant mfaVerifiedAt
) {
    public ActorContext(
        UUID userId,
        String displayName,
        String email,
        String status,
        Set<String> platformRoles,
        Set<String> permissions,
        UUID activeStudioId,
        String activeStudioRole,
        String assurance,
        boolean isAuthenticated
    ) {
        this(userId, displayName, email, status, platformRoles, permissions, activeStudioId, activeStudioRole, assurance, isAuthenticated, null);
    }

    public ActorContext(
        UUID userId,
        String displayName,
        String email,
        Set<String> platformRoles,
        Set<String> permissions,
        UUID activeStudioId,
        String activeStudioRole,
        String assurance,
        boolean isAuthenticated
    ) {
        this(userId, displayName, email, "ACTIVE", platformRoles, permissions, activeStudioId, activeStudioRole, assurance, isAuthenticated, null);
    }

    public ActorContext(
        UUID userId,
        String displayName,
        String email,
        Set<String> platformRoles,
        UUID activeStudioId,
        String activeStudioRole,
        boolean isAuthenticated
    ) {
        this(userId, displayName, email, "ACTIVE", platformRoles, Set.of(), activeStudioId, activeStudioRole, "PASSKEY", isAuthenticated, null);
    }

    public static ActorContext anonymous() {
        return new ActorContext(null, "Anonymous", null, "NONE", Set.of("PUBLIC"), Set.of(), null, null, "NONE", false, null);
    }

    public boolean hasRole(String role) {
        return platformRoles != null && platformRoles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean isStudioOwnerOrAdmin(UUID studioId) {
        if (studioId == null || !studioId.equals(activeStudioId)) {
            return false;
        }
        return "DESIGNER_ADMIN".equalsIgnoreCase(activeStudioRole)
                || "OWNER".equalsIgnoreCase(activeStudioRole)
                || "ADMIN".equalsIgnoreCase(activeStudioRole);
    }

    public boolean isStudioAdmin(UUID studioId) {
        return isStudioOwnerOrAdmin(studioId);
    }

    public boolean isStudioMember(UUID studioId) {
        if (studioId == null || !studioId.equals(activeStudioId)) {
            return false;
        }
        return activeStudioRole != null;
    }

    public boolean hasMfaAssurance() {
        return mfaVerifiedAt != null
                || "MFA".equalsIgnoreCase(assurance)
                || "WEBAUTHN".equalsIgnoreCase(assurance)
                || "MFA_VERIFIED".equalsIgnoreCase(assurance);
    }

    public boolean isStepUpValid(Duration freshnessWindow, Instant now) {
        if (mfaVerifiedAt == null) {
            return false;
        }
        return now.isBefore(mfaVerifiedAt.plus(freshnessWindow));
    }

    public boolean isSuspended() {
        return "SUSPENDED".equalsIgnoreCase(status);
    }
}
