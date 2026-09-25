package com.interior.platform.reviews.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.leads.domain.LeadStatus;
import com.interior.platform.leads.domain.StudioLeadRecord;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.reviews.domain.ReviewInvitationRecord;
import com.interior.platform.reviews.domain.ReviewInvitationSessionRecord;
import com.interior.platform.reviews.domain.ReviewInvitationStatus;
import com.interior.platform.reviews.dto.CreateReviewInvitationRequest;
import com.interior.platform.reviews.dto.CreateReviewInvitationResponse;
import com.interior.platform.reviews.dto.ReviewInvitationDto;
import com.interior.platform.reviews.repository.ReviewRepository;
import com.interior.platform.security.domain.ActorContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewInvitationService {

    private static final Duration DEFAULT_INVITATION_VALIDITY = Duration.ofDays(30);
    private static final Duration SESSION_VALIDITY = Duration.ofHours(24);
    private final SecureRandom secureRandom = new SecureRandom();

    private final ReviewRepository reviewRepository;
    private final LeadRepository leadRepository;
    private final StudioRepository studioRepository;

    public ReviewInvitationService(
            ReviewRepository reviewRepository,
            LeadRepository leadRepository,
            StudioRepository studioRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.leadRepository = leadRepository;
        this.studioRepository = studioRepository;
    }

    @Transactional
    public CreateReviewInvitationResponse createInvitation(
            ActorContext actor,
            UUID studioId,
            CreateReviewInvitationRequest req
    ) {
        validateStudioAccess(actor, studioId);

        StudioLeadRecord lead = leadRepository.findById(studioId, req.leadId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + req.leadId()));

        if (lead.status() != LeadStatus.WON) {
            throw new IllegalStateException("Review invitations can only be generated for WON leads. Current status: " + lead.status());
        }

        // Check if an existing active/used invitation already exists
        Optional<ReviewInvitationRecord> existing = reviewRepository.findInvitationByLeadId(studioId, req.leadId());
        if (existing.isPresent()) {
            ReviewInvitationRecord prev = existing.get();
            if (prev.status() == ReviewInvitationStatus.PENDING && !prev.isExpired()) {
                throw new IllegalStateException("An active review invitation already exists for this client relationship.");
            }
            if (prev.status() == ReviewInvitationStatus.USED) {
                throw new IllegalStateException("A review has already been submitted for this client relationship.");
            }
            // If previous was revoked or expired, we revoke any active sessions and replace it
            reviewRepository.revokeSessionsForInvitation(prev.id());
        }

        // Generate 256-bit cryptographically secure raw token (32 bytes = 64 hex chars)
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = HexFormat.of().formatHex(tokenBytes);
        byte[] tokenHash = sha256(rawToken);

        UUID invitationId = UuidV7.randomUuid();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(DEFAULT_INVITATION_VALIDITY);

        ReviewInvitationRecord record = new ReviewInvitationRecord(
                invitationId,
                studioId,
                req.leadId(),
                req.projectId(),
                tokenHash,
                ReviewInvitationStatus.PENDING,
                expiresAt,
                actor.userId(),
                null,
                null,
                now,
                now,
                1L
        );

        reviewRepository.createInvitation(record);

        String invitationUrl = "/review/invite/" + rawToken;
        return new CreateReviewInvitationResponse(invitationId, rawToken, invitationUrl, expiresAt);
    }

    public List<ReviewInvitationDto> listInvitations(ActorContext actor, UUID studioId, int limit, int offset) {
        validateStudioAccess(actor, studioId);
        return reviewRepository.listInvitations(studioId, limit, offset).stream()
                .map(this::toDto)
                .toList();
    }

    public int countInvitations(ActorContext actor, UUID studioId) {
        validateStudioAccess(actor, studioId);
        return reviewRepository.countInvitations(studioId);
    }

    @Transactional
    public void revokeInvitation(ActorContext actor, UUID studioId, UUID invitationId) {
        validateStudioAccess(actor, studioId);
        ReviewInvitationRecord invitation = reviewRepository.findInvitationById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Review invitation not found: " + invitationId));

        if (!invitation.studioId().equals(studioId)) {
            throw new AccessDeniedException("Invitation does not belong to this studio");
        }

        reviewRepository.revokeInvitation(studioId, invitationId);
        reviewRepository.revokeSessionsForInvitation(invitationId);
    }

    @Transactional
    public ExchangeReviewSessionResult exchangeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invalid invitation token");
        }

        byte[] tokenHash = sha256(rawToken.trim());
        ReviewInvitationRecord invitation = reviewRepository.findInvitationByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Review invitation not found or invalid"));

        if (!invitation.isValidForSubmission()) {
            throw new IllegalStateException("Review invitation is " + invitation.status() + (invitation.isExpired() ? " (EXPIRED)" : ""));
        }

        // Fetch Studio details and Lead first name for friendly presentation
        StudioDetailRecord studio = studioRepository.findStudioById(invitation.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        StudioLeadRecord lead = leadRepository.findById(invitation.studioId(), invitation.leadId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead relationship not found"));

        String clientFirstName = extractFirstName(lead.name());

        // Generate 256-bit session token and CSRF token
        byte[] sessionBytes = new byte[32];
        secureRandom.nextBytes(sessionBytes);
        String sessionToken = HexFormat.of().formatHex(sessionBytes);
        byte[] sessionTokenHash = sha256(sessionToken);

        byte[] csrfBytes = new byte[32];
        secureRandom.nextBytes(csrfBytes);
        String csrfToken = HexFormat.of().formatHex(csrfBytes);
        byte[] csrfTokenHash = sha256(csrfToken);

        Instant expiresAt = Instant.now().plus(SESSION_VALIDITY);
        ReviewInvitationSessionRecord session = new ReviewInvitationSessionRecord(
                UuidV7.randomUuid(),
                invitation.id(),
                invitation.studioId(),
                sessionTokenHash,
                csrfTokenHash,
                expiresAt,
                null,
                Instant.now()
        );

        reviewRepository.createSession(session);

        return new ExchangeReviewSessionResult(
                invitation.id(),
                sessionToken,
                csrfToken,
                expiresAt,
                studio.name(),
                studio.slug(),
                clientFirstName
        );
    }

    public ReviewSessionValidation validateSessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new AccessDeniedException("Review session required");
        }

        byte[] sessionTokenHash = sha256(sessionToken.trim());
        ReviewInvitationSessionRecord session = reviewRepository.findSessionByTokenHash(sessionTokenHash)
                .orElseThrow(() -> new AccessDeniedException("Invalid review session"));

        if (!session.isActive()) {
            throw new AccessDeniedException("Review session has expired or been revoked");
        }

        ReviewInvitationRecord invitation = reviewRepository.findInvitationById(session.invitationId())
                .orElseThrow(() -> new AccessDeniedException("Associated invitation not found"));

        if (!invitation.isValidForSubmission()) {
            throw new AccessDeniedException("Associated invitation is no longer valid for submission");
        }

        return new ReviewSessionValidation(session, invitation);
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Client";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private void validateStudioAccess(ActorContext actor, UUID studioId) {
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN")) {
            return;
        }
        if (!actor.isStudioMember(studioId)) {
            throw new AccessDeniedException("User is not a member of studio: " + studioId);
        }
    }

    private ReviewInvitationDto toDto(ReviewInvitationRecord r) {
        return new ReviewInvitationDto(
                r.id(),
                r.studioId(),
                r.leadId(),
                r.projectId(),
                r.status().name(),
                r.expiresAt(),
                r.createdAt(),
                r.usedAt(),
                r.revokedAt()
        );
    }

    public static byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    public record ExchangeReviewSessionResult(
            UUID invitationId,
            String sessionToken,
            String csrfToken,
            Instant expiresAt,
            String studioName,
            String studioSlug,
            String clientFirstName
    ) {
    }

    public record ReviewSessionValidation(
            ReviewInvitationSessionRecord session,
            ReviewInvitationRecord invitation
    ) {
    }
}
