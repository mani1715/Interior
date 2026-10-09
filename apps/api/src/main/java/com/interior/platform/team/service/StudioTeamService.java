package com.interior.platform.team.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.service.EmailTemplateService;
import com.interior.platform.notifications.domain.NotificationType;
import com.interior.platform.notifications.service.NotificationService;
import com.interior.platform.realtime.domain.RealtimeEvent;
import com.interior.platform.realtime.domain.RealtimeEventType;
import com.interior.platform.realtime.service.RealtimeEventPublisher;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.security.service.RateLimiterService;
import com.interior.platform.team.domain.StudioMemberDetails;
import com.interior.platform.team.domain.StudioMemberInvitationRecord;
import com.interior.platform.team.dto.*;
import com.interior.platform.team.repository.StudioTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StudioTeamService {

    private static final Logger log = LoggerFactory.getLogger(StudioTeamService.class);
    private static final Duration INVITATION_EXPIRY = Duration.ofDays(7);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public record InvitationSession(
            String sessionToken,
            UUID invitationId,
            byte[] tokenHash,
            UUID studioId,
            String invitedEmail,
            String role,
            Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record ExchangeResult(
            String sessionToken,
            ExchangeInvitationResponse response
    ) {}

    private final ConcurrentHashMap<String, InvitationSession> invitationSessions = new ConcurrentHashMap<>();

    private final StudioTeamRepository teamRepository;
    private final AuthorizationService authorizationService;
    private final RateLimiterService rateLimiterService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final CommunicationDeliveryService communicationDeliveryService;
    private final EmailTemplateService emailTemplateService;

    public StudioTeamService(
            StudioTeamRepository teamRepository,
            AuthorizationService authorizationService,
            RateLimiterService rateLimiterService,
            AuditService auditService,
            NotificationService notificationService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this(teamRepository, authorizationService, rateLimiterService, auditService, notificationService, realtimeEventPublisher, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public StudioTeamService(
            StudioTeamRepository teamRepository,
            AuthorizationService authorizationService,
            RateLimiterService rateLimiterService,
            AuditService auditService,
            NotificationService notificationService,
            RealtimeEventPublisher realtimeEventPublisher,
            CommunicationDeliveryService communicationDeliveryService,
            EmailTemplateService emailTemplateService
    ) {
        this.teamRepository = teamRepository;
        this.authorizationService = authorizationService;
        this.rateLimiterService = rateLimiterService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.communicationDeliveryService = communicationDeliveryService;
        this.emailTemplateService = emailTemplateService;
    }

    /**
     * Retrieves team overview for the studio.
     */
    @Transactional(readOnly = true)
    public TeamOverviewResponse getTeamOverview(ActorContext actor, UUID requestedStudioId) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        authorizationService.requireStudioAccess(actor, studioId);

        boolean isCurrentUserAdmin = actor.isStudioOwnerOrAdmin(studioId);
        String studioName = teamRepository.findStudioName(studioId);

        List<StudioMemberDetails> memberRecords = teamRepository.getStudioMembersWithUserDetails(studioId);
        int adminCount = teamRepository.countAdmins(studioId);

        List<TeamMemberDto> memberDtos = memberRecords.stream().map(m -> {
            boolean isSelf = m.userId().equals(actor.userId());
            boolean targetIsAdmin = isAdminRole(m.role());

            // Can remove: current user must be admin, and cannot remove last admin
            boolean canRemove = isCurrentUserAdmin && (!targetIsAdmin || adminCount > 1);
            // Can change role: current user must be admin, and cannot demote last admin
            boolean canChangeRole = isCurrentUserAdmin && (!targetIsAdmin || adminCount > 1);

            return new TeamMemberDto(
                    m.membershipId(),
                    m.userId(),
                    m.displayName(),
                    m.email(),
                    normalizeRoleDisplay(m.role()),
                    m.joinedAt(),
                    isSelf,
                    canRemove,
                    canChangeRole
            );
        }).toList();

        List<PendingInvitationDto> pendingDtos = List.of();
        if (isCurrentUserAdmin) {
            List<StudioMemberInvitationRecord> pendingInvites = teamRepository.findPendingInvitations(studioId, Instant.now());
            pendingDtos = pendingInvites.stream().map(inv -> new PendingInvitationDto(
                    inv.id(),
                    inv.invitedEmail(),
                    normalizeRoleDisplay(inv.role()),
                    inv.status(),
                    inv.createdAt(),
                    inv.expiresAt()
            )).toList();
        }

        return new TeamOverviewResponse(
                studioId,
                studioName,
                memberDtos.size(),
                normalizeRoleDisplay(actor.activeStudioRole()),
                isCurrentUserAdmin,
                memberDtos,
                pendingDtos
        );
    }

    /**
     * Creates a secure member invitation.
     */
    @Transactional
    public CreateInvitationResponse createInvitation(
            ActorContext actor,
            UUID requestedStudioId,
            CreateInvitationRequest request
    ) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        authorizationService.requireStudioAdmin(actor, studioId);

        rateLimiterService.acquire("studio-invite:" + studioId, 20, Duration.ofHours(1));

        String email = request.email().trim().toLowerCase();
        String normalizedRole = canonicalizeRole(request.role());

        // Check if user is already a member
        List<StudioMemberDetails> existingMembers = teamRepository.getStudioMembersWithUserDetails(studioId);
        boolean alreadyMember = existingMembers.stream()
                .anyMatch(m -> email.equalsIgnoreCase(m.email()));
        if (alreadyMember) {
            throw new ConflictException("User with email " + email + " is already a member of this studio");
        }

        // Check for existing pending invitation - revoke it to issue fresh invite
        teamRepository.findPendingInvitationByEmail(studioId, email, Instant.now())
                .ifPresent(existing -> teamRepository.revokeInvitation(existing.id(), Instant.now()));

        // Generate cryptographically secure token and SHA-256 hash
        String rawToken = generateSecureToken();
        byte[] tokenHash = computeSha256(rawToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(INVITATION_EXPIRY);
        UUID invitationId = UuidV7.randomUuid();

        StudioMemberInvitationRecord record = new StudioMemberInvitationRecord(
                invitationId,
                studioId,
                email,
                normalizedRole,
                tokenHash,
                actor.userId(),
                "PENDING",
                expiresAt,
                null,
                null,
                null,
                now,
                now
        );

        teamRepository.saveInvitation(record);

        // Audit Event
        auditService.record(
                actor.userId(),
                studioId,
                "STUDIO_INVITATION_CREATED",
                "INVITATION",
                invitationId.toString(),
                Map.of("invitedEmail", email, "role", normalizedRole),
                null,
                null
        );

        // Realtime Event
        realtimeEventPublisher.publish(RealtimeEvent.ofStudio(
                RealtimeEventType.STUDIO_INVITATION_CREATED,
                null,
                studioId,
                "INVITATION",
                invitationId.toString(),
                Map.of("invitedEmail", email, "role", normalizedRole)
        ));

        String inviteUrl = "/invite/" + rawToken;

        // Attempt transactional email delivery (truthfully records NOT_CONFIGURED when disabled)
        if (communicationDeliveryService != null && emailTemplateService != null) {
            String studioName = teamRepository.findStudioName(studioId);
            EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderTeamInvitation(
                    studioName, normalizeRoleDisplay(normalizedRole), inviteUrl
            );
            communicationDeliveryService.attemptEmailDelivery(
                    studioId,
                    null,
                    "TEAM_INVITATION",
                    email,
                    rendered.subject(),
                    rendered.textBody(),
                    rendered.htmlBody(),
                    "team_invite:" + invitationId
            );
        }

        return new CreateInvitationResponse(
                invitationId,
                email,
                normalizeRoleDisplay(normalizedRole),
                rawToken,
                inviteUrl,
                expiresAt
        );
    }

    /**
     * Revokes a pending invitation.
     */
    @Transactional
    public void revokeInvitation(ActorContext actor, UUID requestedStudioId, UUID invitationId) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        authorizationService.requireStudioAdmin(actor, studioId);

        StudioMemberInvitationRecord invitation = teamRepository.findInvitationById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.studioId().equals(studioId)) {
            throw new AccessDeniedException("Access denied: tenant isolation violation");
        }

        if (!"PENDING".equalsIgnoreCase(invitation.status())) {
            throw new BadRequestException("Only pending invitations can be revoked");
        }

        teamRepository.revokeInvitation(invitationId, Instant.now());

        // Audit Event
        auditService.record(
                actor.userId(),
                studioId,
                "STUDIO_INVITATION_REVOKED",
                "INVITATION",
                invitationId.toString(),
                Map.of("invitedEmail", invitation.invitedEmail()),
                null,
                null
        );
    }

    /**
     * Updates a member's role within the studio.
     */
    @Transactional
    public void updateMemberRole(
            ActorContext actor,
            UUID requestedStudioId,
            UUID membershipId,
            UpdateMemberRoleRequest request
    ) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        authorizationService.requireStudioAdmin(actor, studioId);

        StudioMemberDetails member = teamRepository.findMembershipById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        if (!member.studioId().equals(studioId)) {
            throw new AccessDeniedException("Access denied: tenant isolation violation");
        }

        String newRole = canonicalizeRole(request.role());
        boolean currentIsAdmin = isAdminRole(member.role());
        boolean newIsAdmin = isAdminRole(newRole);

        // LAST-ADMIN LOCKOUT INVARIANT: Cannot demote final admin
        if (currentIsAdmin && !newIsAdmin) {
            int adminCount = teamRepository.countAdmins(studioId);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot demote the last remaining studio administrator. Assign another administrator first.");
            }
        }

        teamRepository.updateMemberRole(membershipId, newRole);

        // Audit Event
        auditService.record(
                actor.userId(),
                studioId,
                "STUDIO_MEMBER_ROLE_CHANGED",
                "STUDIO_MEMBER",
                membershipId.toString(),
                Map.of("targetUserId", member.userId().toString(), "oldRole", member.role(), "newRole", newRole),
                null,
                null
        );

        // Persistent notification to target member
        String studioName = teamRepository.findStudioName(studioId);
        notificationService.dispatchNotification(
                member.userId(),
                studioId,
                NotificationType.STUDIO_ROLE_CHANGED,
                "Role Updated",
                "Your role in " + studioName + " was changed to " + normalizeRoleDisplay(newRole) + ".",
                "/workspace/team",
                null
        );

        // Realtime SSE Event
        realtimeEventPublisher.publish(RealtimeEvent.ofStudio(
                RealtimeEventType.STUDIO_MEMBER_ROLE_CHANGED,
                member.userId(),
                studioId,
                "STUDIO_MEMBER",
                membershipId.toString(),
                Map.of("newRole", newRole)
        ));
    }

    /**
     * Removes a member from the studio.
     */
    @Transactional
    public void removeMember(ActorContext actor, UUID requestedStudioId, UUID membershipId) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        authorizationService.requireStudioAdmin(actor, studioId);

        StudioMemberDetails member = teamRepository.findMembershipById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        if (!member.studioId().equals(studioId)) {
            throw new AccessDeniedException("Access denied: tenant isolation violation");
        }

        // LAST-ADMIN LOCKOUT INVARIANT: Cannot remove final admin
        if (isAdminRole(member.role())) {
            int adminCount = teamRepository.countAdmins(studioId);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot remove the last remaining studio administrator. Assign another administrator first.");
            }
        }

        teamRepository.removeMember(membershipId);

        // Audit Event
        auditService.record(
                actor.userId(),
                studioId,
                "STUDIO_MEMBER_REMOVED",
                "STUDIO_MEMBER",
                membershipId.toString(),
                Map.of("removedUserId", member.userId().toString(), "removedEmail", member.email()),
                null,
                null
        );

        // Persistent notification to removed user
        String studioName = teamRepository.findStudioName(studioId);
        notificationService.dispatchNotification(
                member.userId(),
                studioId,
                NotificationType.STUDIO_MEMBER_REMOVED,
                "Studio Access Removed",
                "Your membership in " + studioName + " has been revoked.",
                "/workspace",
                null
        );

        // Realtime SSE Event
        realtimeEventPublisher.publish(RealtimeEvent.ofStudio(
                RealtimeEventType.STUDIO_MEMBER_REMOVED,
                member.userId(),
                studioId,
                "STUDIO_MEMBER",
                membershipId.toString(),
                Map.of()
        ));
    }

    /**
     * Allows a member to voluntarily leave the studio.
     */
    @Transactional
    public void leaveStudio(ActorContext actor, UUID requestedStudioId) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        authorizationService.requireStudioAccess(actor, studioId);

        StudioMemberDetails member = teamRepository.findMembershipByStudioAndUser(studioId, actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this studio"));

        // LAST-ADMIN LOCKOUT INVARIANT: Final admin cannot leave
        if (isAdminRole(member.role())) {
            int adminCount = teamRepository.countAdmins(studioId);
            if (adminCount <= 1) {
                throw new BadRequestException("As the only administrator, you cannot leave the studio. Promote another member to administrator before leaving.");
            }
        }

        teamRepository.removeMember(member.membershipId());

        // Audit Event
        auditService.record(
                actor.userId(),
                studioId,
                "STUDIO_MEMBER_LEFT",
                "STUDIO_MEMBER",
                member.membershipId().toString(),
                Map.of("userId", actor.userId().toString()),
                null,
                null
        );

        // Realtime SSE Event
        realtimeEventPublisher.publish(RealtimeEvent.ofStudio(
                RealtimeEventType.STUDIO_MEMBER_REMOVED,
                actor.userId(),
                studioId,
                "STUDIO_MEMBER",
                member.membershipId().toString(),
                Map.of()
        ));
    }

    /**
     * Validates an invitation token for public/authenticated preview.
     */
    @Transactional(readOnly = true)
    public ValidateInvitationResponse validateInvitation(String rawToken, String clientIp) {
        rateLimiterService.acquire("inv-val:" + (clientIp != null ? clientIp : "local"), 60, Duration.ofMinutes(1));

        if (rawToken == null || rawToken.isBlank()) {
            return new ValidateInvitationResponse(false, null, null, null, "INVALID", "Invitation token is missing.");
        }

        byte[] tokenHash = computeSha256(rawToken.trim());
        Optional<StudioMemberInvitationRecord> opt = teamRepository.findInvitationByTokenHash(tokenHash);

        if (opt.isEmpty()) {
            return new ValidateInvitationResponse(false, null, null, null, "INVALID", "Invitation not found or invalid.");
        }

        StudioMemberInvitationRecord inv = opt.get();
        String studioName = teamRepository.findStudioName(inv.studioId());
        String displayRole = normalizeRoleDisplay(inv.role());
        String masked = maskEmail(inv.invitedEmail());

        if ("REVOKED".equalsIgnoreCase(inv.status())) {
            return new ValidateInvitationResponse(false, studioName, masked, displayRole, "REVOKED", "This invitation has been revoked by the studio administrator.");
        }

        if ("ACCEPTED".equalsIgnoreCase(inv.status())) {
            return new ValidateInvitationResponse(false, studioName, masked, displayRole, "ALREADY_ACCEPTED", "This invitation has already been accepted.");
        }

        if (inv.expiresAt().isBefore(Instant.now())) {
            return new ValidateInvitationResponse(false, studioName, masked, displayRole, "EXPIRED", "This invitation has expired.");
        }

        return new ValidateInvitationResponse(true, studioName, masked, displayRole, "PENDING", null);
    }

    /**
     * Exchanges raw invitation token for an ephemeral session token, scrubbing the token from browser URL.
     */
    @Transactional(readOnly = true)
    public ExchangeResult exchangeInvitation(String rawToken, String clientIp) {
        rateLimiterService.acquire("inv-exch:" + (clientIp != null ? clientIp : "local"), 60, Duration.ofMinutes(1));

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Invitation token is required");
        }

        byte[] tokenHash = computeSha256(rawToken.trim());
        StudioMemberInvitationRecord inv = teamRepository.findInvitationByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or invalid"));

        Instant now = Instant.now();
        if ("REVOKED".equalsIgnoreCase(inv.status())) {
            throw new BadRequestException("This invitation has been revoked");
        }
        if ("ACCEPTED".equalsIgnoreCase(inv.status())) {
            throw new BadRequestException("This invitation has already been accepted");
        }
        if (inv.expiresAt().isBefore(now)) {
            throw new BadRequestException("This invitation has expired");
        }

        String sessionToken = generateSecureToken();
        Instant sessionExpiresAt = inv.expiresAt().isBefore(now.plus(Duration.ofHours(1)))
                ? inv.expiresAt()
                : now.plus(Duration.ofHours(1));

        invitationSessions.put(sessionToken, new InvitationSession(
                sessionToken,
                inv.id(),
                tokenHash,
                inv.studioId(),
                inv.invitedEmail(),
                inv.role(),
                sessionExpiresAt
        ));

        String studioName = teamRepository.findStudioName(inv.studioId());
        String displayRole = normalizeRoleDisplay(inv.role());

        ExchangeInvitationResponse response = new ExchangeInvitationResponse(
                true,
                studioName,
                maskEmail(inv.invitedEmail()),
                displayRole,
                null
        );

        return new ExchangeResult(sessionToken, response);
    }

    /**
     * Retrieves invitation session info by ephemeral session token.
     */
    @Transactional(readOnly = true)
    public Optional<ExchangeInvitationResponse> getInvitationSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }

        InvitationSession session = invitationSessions.get(sessionToken.trim());
        if (session == null || session.isExpired()) {
            if (session != null) {
                invitationSessions.remove(sessionToken.trim());
            }
            return Optional.empty();
        }

        StudioMemberInvitationRecord inv = teamRepository.findInvitationById(session.invitationId())
                .orElse(null);

        if (inv == null || !"PENDING".equalsIgnoreCase(inv.status()) || inv.expiresAt().isBefore(Instant.now())) {
            invitationSessions.remove(sessionToken.trim());
            return Optional.empty();
        }

        String studioName = teamRepository.findStudioName(inv.studioId());
        return Optional.of(new ExchangeInvitationResponse(
                true,
                studioName,
                maskEmail(inv.invitedEmail()),
                normalizeRoleDisplay(inv.role()),
                null
        ));
    }

    /**
     * Accepts an invitation and grants studio membership.
     */
    @Transactional
    public void acceptInvitation(ActorContext actor, AcceptInvitationRequest request) {
        acceptInvitation(actor, request, null);
    }

    @Transactional
    public void acceptInvitation(ActorContext actor, AcceptInvitationRequest request, String cookieSessionToken) {
        authorizationService.requireAuthenticated(actor);

        rateLimiterService.acquire("inv-acc:" + actor.userId(), 10, Duration.ofMinutes(5));

        byte[] tokenHash = null;

        String providedToken = request != null && request.token() != null ? request.token().trim() : null;

        if (providedToken != null && !providedToken.isBlank()) {
            InvitationSession session = invitationSessions.get(providedToken);
            if (session != null && !session.isExpired()) {
                tokenHash = session.tokenHash();
                invitationSessions.remove(providedToken);
            }
        }

        if (tokenHash == null && cookieSessionToken != null && !cookieSessionToken.isBlank()) {
            InvitationSession session = invitationSessions.get(cookieSessionToken.trim());
            if (session != null && !session.isExpired()) {
                tokenHash = session.tokenHash();
                invitationSessions.remove(cookieSessionToken.trim());
            }
        }

        if (tokenHash == null) {
            if (providedToken == null || providedToken.isBlank()) {
                throw new BadRequestException("Invitation session or token is required");
            }
            tokenHash = computeSha256(providedToken);
        }

        StudioMemberInvitationRecord inv = teamRepository.findInvitationByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or invalid"));

        Instant now = Instant.now();
        if (!inv.isPending(now)) {
            if ("REVOKED".equalsIgnoreCase(inv.status())) {
                throw new BadRequestException("This invitation has been revoked");
            }
            if ("ACCEPTED".equalsIgnoreCase(inv.status())) {
                throw new BadRequestException("This invitation has already been accepted");
            }
            if (inv.expiresAt().isBefore(now)) {
                throw new BadRequestException("This invitation has expired");
            }
            throw new BadRequestException("Invitation is no longer valid");
        }

        // EMAIL MISMATCH GUARD
        String actorEmail = actor.email() != null ? actor.email().trim().toLowerCase() : "";
        String invitedEmail = inv.invitedEmail() != null ? inv.invitedEmail().trim().toLowerCase() : "";
        if (!actorEmail.equalsIgnoreCase(invitedEmail)) {
            throw new AccessDeniedException("This invitation was sent to " + maskEmail(inv.invitedEmail()) + ". You are currently signed in as " + actor.email() + ". Please sign in with the invited email address to accept.");
        }

        // DUPLICATE MEMBERSHIP GUARD
        Optional<StudioMemberDetails> existingMember = teamRepository.findMembershipByStudioAndUser(inv.studioId(), actor.userId());
        if (existingMember.isPresent()) {
            // Already member - mark invitation accepted and complete safely
            teamRepository.updateInvitationStatus(inv.id(), "ACCEPTED", now, actor.userId());
            return;
        }

        // Add studio membership with canonical role
        String canonicalRole = canonicalizeRole(inv.role());
        UUID membershipId = UuidV7.randomUuid();
        teamRepository.addStudioMember(membershipId, inv.studioId(), actor.userId(), canonicalRole);

        // Update invitation record
        teamRepository.updateInvitationStatus(inv.id(), "ACCEPTED", now, actor.userId());

        // Audit Event
        auditService.record(
                actor.userId(),
                inv.studioId(),
                "STUDIO_INVITATION_ACCEPTED",
                "INVITATION",
                inv.id().toString(),
                Map.of("role", canonicalRole, "acceptedByUserId", actor.userId().toString()),
                null,
                null
        );

        String studioName = teamRepository.findStudioName(inv.studioId());

        // Notification to studio inviter / owner
        notificationService.dispatchNotification(
                inv.invitedByUserId(),
                inv.studioId(),
                NotificationType.STUDIO_INVITATION_ACCEPTED,
                "Team Invitation Accepted",
                actor.displayName() + " (" + actor.email() + ") accepted the invitation to join " + studioName + ".",
                "/workspace/team",
                null
        );

        // Notification to accepting user
        notificationService.dispatchNotification(
                actor.userId(),
                inv.studioId(),
                NotificationType.STUDIO_MEMBER_ADDED,
                "Joined Studio Team",
                "You have joined " + studioName + " as " + normalizeRoleDisplay(canonicalRole) + ".",
                "/workspace",
                null
        );

        // Realtime SSE Event
        realtimeEventPublisher.publish(RealtimeEvent.ofStudio(
                RealtimeEventType.STUDIO_MEMBER_ADDED,
                inv.invitedByUserId(),
                inv.studioId(),
                "STUDIO_MEMBER",
                membershipId.toString(),
                Map.of("newMemberName", actor.displayName(), "newMemberEmail", actor.email())
        ));
    }

    private UUID resolveStudioId(ActorContext actor, UUID requestedStudioId) {
        if (requestedStudioId != null) {
            return requestedStudioId;
        }
        if (actor.activeStudioId() != null) {
            return actor.activeStudioId();
        }
        throw new AccessDeniedException("No active studio context found");
    }

    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String r = role.toUpperCase();
        return "OWNER".equals(r) || "ADMIN".equals(r) || "DESIGNER_ADMIN".equals(r);
    }

    private String canonicalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "DESIGNER_MEMBER";
        }
        String r = role.trim().toUpperCase();
        if (r.contains("ADMIN") || "OWNER".equals(r)) {
            return "DESIGNER_ADMIN";
        }
        return "DESIGNER_MEMBER";
    }

    private String normalizeRoleDisplay(String role) {
        if (role == null) return "Team Member";
        String r = role.toUpperCase();
        if (r.contains("ADMIN") || "OWNER".equals(r)) return "Studio Admin";
        return "Team Member";
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        if (local.length() <= 1) {
            return local + "***@" + domain;
        } else if (local.length() == 2) {
            return local.charAt(0) + "***" + local.charAt(1) + "@" + domain;
        } else {
            return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
