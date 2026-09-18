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

    public void requirePermission(ActorContext actor, String permission) {
        requireAuthenticated(actor);
        if (actor.hasRole("SUPER_ADMIN")) {
            return;
        }
        if (!actor.hasPermission(permission)) {
            throw new AccessDeniedException("Required permission missing: " + permission);
        }
    }

    /**
     * Enforces elevated authentication assurance (MFA / WebAuthn) for privileged roles.
     */
    public void requireMfaAssurance(ActorContext actor) {
        requireAuthenticated(actor);
        if (!actor.hasMfaAssurance()) {
            throw new AccessDeniedException("Multi-factor authentication (MFA) assurance required for this action");
        }
    }

    public void requireStudioAccess(ActorContext actor, UUID targetStudioId) {
        requireAuthenticated(actor);
        
        // Super Admin or Admin bypass for platform oversight
        if (actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return;
        }

        // DESIGNER_TEAM and DESIGNER are strictly scoped to their assigned studio
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
