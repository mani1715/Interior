package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.security.domain.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationPolicyTest {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
    }

    @Test
    @DisplayName("Anonymous actor is rejected with UnauthorizedException")
    void testAnonymousRejected() {
        ActorContext anon = ActorContext.anonymous();
        assertThrows(UnauthorizedException.class, () -> authorizationService.requireAuthenticated(anon));
    }

    @Test
    @DisplayName("Role checks allow matching role or SUPER_ADMIN")
    void testRoleChecks() {
        ActorContext customer = new ActorContext(UUID.randomUUID(), "Customer", "c@test.com", Set.of("CUSTOMER"), null, null, true);
        ActorContext admin = new ActorContext(UUID.randomUUID(), "Admin", "a@test.com", Set.of("ADMIN"), null, null, true);
        ActorContext superAdmin = new ActorContext(UUID.randomUUID(), "SuperAdmin", "sa@test.com", Set.of("SUPER_ADMIN"), null, null, true);

        assertDoesNotThrow(() -> authorizationService.requirePlatformRole(customer, "CUSTOMER"));
        assertThrows(AccessDeniedException.class, () -> authorizationService.requirePlatformRole(customer, "ADMIN"));

        assertDoesNotThrow(() -> authorizationService.requirePlatformRole(admin, "ADMIN"));
        // Super Admin bypasses platform role check
        assertDoesNotThrow(() -> authorizationService.requirePlatformRole(superAdmin, "ADMIN"));
        assertDoesNotThrow(() -> authorizationService.requirePlatformRole(superAdmin, "MODERATOR"));
    }

    @Test
    @DisplayName("DESIGNER_TEAM is strictly scoped to assigned studio and denied elsewhere")
    void testDesignerTeamStudioScoping() {
        UUID studioA = UUID.randomUUID();
        UUID studioB = UUID.randomUUID();

        ActorContext teamMemberStudioA = new ActorContext(
                UUID.randomUUID(),
                "Team Member",
                "team@studioa.com",
                Set.of("DESIGNER_TEAM"),
                Set.of("project:read", "project:write"),
                studioA,
                "MEMBER",
                "MFA",
                true
        );

        // Allowed on own studio
        assertDoesNotThrow(() -> authorizationService.requireStudioAccess(teamMemberStudioA, studioA));

        // Denied on different studio
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireStudioAccess(teamMemberStudioA, studioB));

        // Denied studio admin access
        assertThrows(AccessDeniedException.class, () -> authorizationService.requireStudioAdmin(teamMemberStudioA, studioA));
    }

    @Test
    @DisplayName("Privileged MFA requirement enforces MFA or WebAuthn assurance")
    void testPrivilegedMfaRequirement() {
        UUID adminId = UUID.randomUUID();

        ActorContext lowAssuranceAdmin = new ActorContext(
                adminId,
                "Admin Low Assurance",
                "admin@platform.com",
                Set.of("ADMIN"),
                Set.of("*"),
                null,
                null,
                "PASSWORD",
                true
        );

        ActorContext mfaAdmin = new ActorContext(
                adminId,
                "Admin MFA",
                "admin@platform.com",
                Set.of("ADMIN"),
                Set.of("*"),
                null,
                null,
                "MFA",
                true
        );

        ActorContext webAuthnAdmin = new ActorContext(
                adminId,
                "Admin WebAuthn",
                "admin@platform.com",
                Set.of("ADMIN"),
                Set.of("*"),
                null,
                null,
                "WEBAUTHN",
                true
        );

        assertThrows(AccessDeniedException.class, () -> authorizationService.requireMfaAssurance(lowAssuranceAdmin));
        assertDoesNotThrow(() -> authorizationService.requireMfaAssurance(mfaAdmin));
        assertDoesNotThrow(() -> authorizationService.requireMfaAssurance(webAuthnAdmin));
    }
}
