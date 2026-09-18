package com.interior.platform.security.service;

import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(prefix = "app.security", name = "dev-auth-enabled", havingValue = "true")
public class DevAuthService {

    private static final Logger log = LoggerFactory.getLogger(DevAuthService.class);

    public record DevAuthResult(UserRecord user, String assurance) {}

    public record DevPersona(String key, String displayName, String email, String role, String assurance) {}

    public static final Map<String, DevPersona> PERSONAS = Map.of(
            "customer", new DevPersona("customer", "Dev Customer", "dev.customer@platform.local", "CUSTOMER", "PASSWORD"),
            "designer_owner", new DevPersona("designer_owner", "Dev Designer Owner", "dev.designer@platform.local", "DESIGNER", "MFA"),
            "designer_team", new DevPersona("designer_team", "Dev Team Member", "dev.team@platform.local", "DESIGNER_TEAM", "MFA"),
            "moderator", new DevPersona("moderator", "Dev Content Moderator", "dev.moderator@platform.local", "MODERATOR", "MFA"),
            "admin", new DevPersona("admin", "Dev Administrator", "dev.admin@platform.local", "ADMIN", "MFA"),
            "super_admin", new DevPersona("super_admin", "Dev Super Admin", "dev.superadmin@platform.local", "SUPER_ADMIN", "WEBAUTHN")
    );

    private final SecurityRepository securityRepository;

    public DevAuthService(SecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
        log.warn("DevAuthService activated! Dev/test authentication enabled. DO NOT USE IN PRODUCTION.");
    }

    public List<DevPersona> getAvailablePersonas() {
        return List.copyOf(PERSONAS.values());
    }

    public DevAuthResult loginPersona(String personaKey) {
        DevPersona persona = PERSONAS.get(personaKey != null ? personaKey.toLowerCase() : "");
        if (persona == null) {
            throw new ResourceNotFoundException("Unknown dev persona: " + personaKey);
        }

        UserRecord user = securityRepository.findUserByEmail(persona.email())
                .orElseGet(() -> createPersonaUser(persona));

        return new DevAuthResult(user, persona.assurance());
    }

    private synchronized UserRecord createPersonaUser(DevPersona persona) {
        // Double check
        var existing = securityRepository.findUserByEmail(persona.email());
        if (existing.isPresent()) {
            return existing.get();
        }

        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        UserRecord user = new UserRecord(userId, persona.displayName(), persona.email(), null, "ACTIVE", now, now, 0L);
        securityRepository.createUser(user);

        // Assign platform role
        securityRepository.assignUserRole(UUID.randomUUID(), userId, persona.role(), now);

        // If designer or designer_team, ensure test studio exists
        if ("DESIGNER".equals(persona.role()) || "DESIGNER_TEAM".equals(persona.role())) {
            UUID studioId = securityRepository.findStudioIdBySlug("dev-studio-atelier");
            if (studioId == null) {
                studioId = UUID.randomUUID();
                securityRepository.createStudio(studioId, "Studio Atelier", "dev-studio-atelier", userId, "ACTIVE");
            }
            String studioRole = "DESIGNER".equals(persona.role()) ? "OWNER" : "MEMBER";
            securityRepository.addStudioMember(UUID.randomUUID(), studioId, userId, studioRole);
        }

        return user;
    }
}
