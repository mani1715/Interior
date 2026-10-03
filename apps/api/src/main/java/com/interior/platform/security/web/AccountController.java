package com.interior.platform.security.web;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.dto.*;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.SessionSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account & Settings", description = "User account profile, security sessions, and privacy controls")
public class AccountController {

    private final SecurityRepository securityRepository;
    private final SessionSecurityService sessionSecurityService;
    private final AuthorizationService authorizationService;
    private final LeadRepository leadRepository;
    private final AuditService auditService;

    public AccountController(
            SecurityRepository securityRepository,
            SessionSecurityService sessionSecurityService,
            AuthorizationService authorizationService,
            LeadRepository leadRepository,
            AuditService auditService
    ) {
        this.securityRepository = securityRepository;
        this.sessionSecurityService = sessionSecurityService;
        this.authorizationService = authorizationService;
        this.leadRepository = leadRepository;
        this.auditService = auditService;
    }

    private ActorContext getActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return actor;
    }

    private SessionRecord getSession(HttpServletRequest request) {
        return (SessionRecord) request.getAttribute(SecurityInterceptor.SESSION_ATTRIBUTE);
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user account profile", description = "Returns verified personal info, tenancy, roles, and status")
    public ResponseEntity<AccountProfileDto> getProfile(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        UserRecord user = securityRepository.findUserById(actor.userId())
                .orElseThrow(() -> new AccessDeniedException("User account not found"));

        var memberships = securityRepository.getStudioMemberships(user.id());
        var studioDtos = memberships.stream()
                .map(m -> new AccountProfileDto.StudioSummaryDto(m.studioId(), m.studioName(), m.studioSlug(), m.role()))
                .toList();

        var roles = securityRepository.getUserRoles(user.id());

        AccountProfileDto dto = new AccountProfileDto(
                user.id(),
                user.displayName(),
                user.email(),
                user.phone(),
                user.avatarUrl(),
                user.status(),
                user.createdAt(),
                user.updatedAt(),
                roles,
                studioDtos,
                user.deactivatedAt(),
                user.deletionRequestedAt()
        );

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update account profile", description = "Updates user display name, phone, and avatar")
    public ResponseEntity<AccountProfileDto> updateProfile(
            @Valid @RequestBody UpdateAccountProfileRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);

        String cleanName = req.displayName().trim();
        String cleanPhone = req.phone() != null ? req.phone().trim() : null;
        String cleanAvatar = req.avatarUrl() != null && !req.avatarUrl().isBlank() ? req.avatarUrl().trim() : null;

        securityRepository.updateUserProfile(actor.userId(), cleanName, cleanPhone, cleanAvatar);

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "USER_PROFILE_UPDATED",
                "USER",
                actor.userId().toString(),
                Map.of("displayName", cleanName),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        return getProfile(request);
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active sessions", description = "Returns active sessions with device labels and expiry info")
    public ResponseEntity<List<ActiveSessionDto>> listSessions(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        SessionRecord currentSession = getSession(request);
        UUID currentSessionId = currentSession != null ? currentSession.id() : null;

        List<SessionRecord> sessions = securityRepository.findActiveSessionsByUserId(actor.userId(), Instant.now());
        List<ActiveSessionDto> dtos = sessions.stream().map(s -> new ActiveSessionDto(
                s.id(),
                s.deviceLabel() != null ? s.deviceLabel() : "Browser Session",
                s.authTime(),
                s.lastSeenAt(),
                s.idleExpiresAt(),
                s.id().equals(currentSessionId)
        )).toList();

        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Revoke specific session", description = "Terminates another session by ID")
    public ResponseEntity<Map<String, Boolean>> revokeSession(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        SessionRecord target = securityRepository.findSessionById(id)
                .orElseThrow(() -> new AccessDeniedException("Session not found"));

        if (!target.userId().equals(actor.userId())) {
            throw new AccessDeniedException("Cannot revoke another user's session");
        }

        sessionSecurityService.revokeSession(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate account", description = "Deactivates account and terminates all active sessions")
    public ResponseEntity<Map<String, Boolean>> deactivateAccount(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ActorContext actor = getActor(request);
        securityRepository.deactivateUser(actor.userId(), Instant.now());
        sessionSecurityService.revokeAllUserSessions(actor.userId());
        sessionSecurityService.clearSessionCookie(response);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/request-deletion")
    @Operation(summary = "Request account deletion", description = "Records a formal account deletion request")
    public ResponseEntity<Map<String, Boolean>> requestDeletion(
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        securityRepository.requestUserDeletion(actor.userId(), Instant.now());

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "USER_DELETION_REQUESTED",
                "USER",
                actor.userId().toString(),
                Map.of(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/inquiries")
    @Operation(summary = "List customer submitted inquiries", description = "Retrieves inquiries submitted by the authenticated customer")
    public ResponseEntity<List<LeadRepository.CustomerInquiryRecord>> getInquiries(
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        List<LeadRepository.CustomerInquiryRecord> inquiries = leadRepository.findCustomerInquiries(actor.userId(), limit, offset);
        return ResponseEntity.ok(inquiries);
    }

    @PostMapping("/feedback")
    @Operation(summary = "Submit platform feedback", description = "Records user platform feedback or bug report")
    public ResponseEntity<Map<String, Boolean>> submitFeedback(
            @Valid @RequestBody PlatformFeedbackRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        UUID userId = (actor != null && actor.isAuthenticated()) ? actor.userId() : null;

        securityRepository.savePlatformFeedback(
                UuidV7.randomUuid(),
                userId,
                req.category(),
                req.message().trim(),
                req.contactEmail() != null ? req.contactEmail().trim() : null,
                Instant.now()
        );

        return ResponseEntity.ok(Map.of("success", true));
    }
}
