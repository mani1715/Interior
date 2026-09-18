package com.interior.platform.security.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuthMeResponse(
    boolean authenticated,
    UUID id,
    String displayName,
    String email,
    String status,
    Set<String> roles,
    Set<String> permissions,
    UUID activeStudioId,
    String activeStudioRole,
    List<StudioSummary> studios,
    String assurance
) {
    public static AuthMeResponse unauthenticated() {
        return new AuthMeResponse(false, null, "Anonymous", null, "NONE", Set.of(), Set.of(), null, null, List.of(), "NONE");
    }

    public static AuthMeResponse authenticated(
            UUID id,
            String displayName,
            String email,
            String status,
            Set<String> roles,
            Set<String> permissions,
            UUID activeStudioId,
            String activeStudioRole,
            List<StudioSummary> studios,
            String assurance
    ) {
        return new AuthMeResponse(true, id, displayName, email, status, roles, permissions, activeStudioId, activeStudioRole, studios, assurance);
    }

    public record StudioSummary(
        UUID studioId,
        String studioName,
        String studioSlug,
        String role
    ) {}
}
