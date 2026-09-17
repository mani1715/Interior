package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.security.domain.ActorContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationService {

    public void requireAuthenticated(ActorContext actor) {
        if (!actor.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required to access this resource");
        }
    }

    public void requirePlatformRole(ActorContext actor, String role) {
        requireAuthenticated(actor);
        if (!actor.hasRole(role) && !actor.hasRole("SUPER_ADMIN")) {
            throw new AccessDeniedException("Required platform role missing: " + role);
        }
    }

    public void requireStudioAccess(ActorContext actor, UUID targetStudioId) {
        requireAuthenticated(actor);
        
        // Super Admin or Moderator bypass for system operations
        if (actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return;
        }

        if (targetStudioId == null || !actor.isStudioMember(targetStudioId)) {
            throw new AccessDeniedException("Access denied: tenant isolation violation");
        }
    }

    public void requireStudioAdmin(ActorContext actor, UUID targetStudioId) {
        requireStudioAccess(actor, targetStudioId);
        
        if (actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return;
        }

        if (!actor.isStudioOwnerOrAdmin(targetStudioId)) {
            throw new AccessDeniedException("Access denied: elevated studio permission required");
        }
    }
}
