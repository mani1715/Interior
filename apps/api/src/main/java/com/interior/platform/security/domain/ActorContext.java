package com.interior.platform.security.domain;

import java.util.Set;
import java.util.UUID;

public record ActorContext(
    UUID userId,
    String displayName,
    String email,
    Set<String> platformRoles,
    UUID activeStudioId,
    String activeStudioRole,
    boolean isAuthenticated
) {
    public static ActorContext anonymous() {
        return new ActorContext(null, "Anonymous", null, Set.of("PUBLIC"), null, null, false);
    }

    public boolean hasRole(String role) {
        return platformRoles.contains(role);
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
}
