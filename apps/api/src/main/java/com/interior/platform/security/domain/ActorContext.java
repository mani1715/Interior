package com.interior.platform.security.domain;

import java.util.Set;
import java.util.UUID;

public record ActorContext(
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
    public ActorContext(
        UUID userId,
        String displayName,
        String email,
        Set<String> platformRoles,
        UUID activeStudioId,
        String activeStudioRole,
        boolean isAuthenticated
    ) {
        this(userId, displayName, email, platformRoles, Set.of(), activeStudioId, activeStudioRole, "PASSKEY", isAuthenticated);
    }

    public static ActorContext anonymous() {
        return new ActorContext(null, "Anonymous", null, Set.of("PUBLIC"), Set.of(), null, null, "NONE", false);
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
        return "OWNER".equalsIgnoreCase(activeStudioRole) || "ADMIN".equalsIgnoreCase(activeStudioRole);
    }

    public boolean isStudioMember(UUID studioId) {
        return studioId != null && studioId.equals(activeStudioId) && activeStudioRole != null;
    }

    public boolean hasMfaAssurance() {
        return "MFA".equalsIgnoreCase(assurance) || "WEBAUTHN".equalsIgnoreCase(assurance);
    }
}
