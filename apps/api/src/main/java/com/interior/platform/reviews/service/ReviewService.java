package com.interior.platform.reviews.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.leads.domain.StudioLeadRecord;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.reviews.domain.*;
import com.interior.platform.reviews.dto.*;
import com.interior.platform.reviews.repository.ReviewRepository;
import com.interior.platform.security.domain.ActorContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import com.interior.platform.analytics.domain.AnalyticsEventType;
import com.interior.platform.analytics.service.AnalyticsService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReviewService {

    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile("<(script|style)[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private final ReviewRepository reviewRepository;
    private final ReviewInvitationService invitationService;
    private final LeadRepository leadRepository;
    private final StudioRepository studioRepository;
    private final AnalyticsService analyticsService;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewInvitationService invitationService,
            LeadRepository leadRepository,
            StudioRepository studioRepository,
            AnalyticsService analyticsService
    ) {
        this.reviewRepository = reviewRepository;
        this.invitationService = invitationService;
        this.leadRepository = leadRepository;
        this.studioRepository = studioRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public PublicStudioReviewDto submitReview(String sessionToken, SubmitReviewRequest req) {
        ReviewInvitationService.ReviewSessionValidation validation = invitationService.validateSessionToken(sessionToken);
        ReviewInvitationRecord invitation = validation.invitation();

        // Idempotency: check if review already submitted for this invitation
        Optional<StudioReviewRecord> existing = reviewRepository.findReviewByInvitationId(invitation.id());
        if (existing.isPresent()) {
            return toDto(existing.get(), null);
        }

        StudioLeadRecord lead = leadRepository.findById(invitation.studioId(), invitation.leadId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        // Rating validation
        if (req.rating() < 1 || req.rating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }

        // Clean plain text (strip any HTML tags to prevent XSS)
        String cleanReviewText = stripHtml(req.reviewText());
        if (cleanReviewText.length() < 10 || cleanReviewText.length() > 2000) {
            throw new IllegalArgumentException("Review text must be between 10 and 2000 characters");
        }

        String cleanTitle = req.title() != null ? stripHtml(req.title()).trim() : null;
        if (cleanTitle != null && cleanTitle.length() > 150) {
            cleanTitle = cleanTitle.substring(0, 150);
        }

        // Determine reviewer display name according to chosen privacy mode
        String baseName = (req.customDisplayName() != null && !req.customDisplayName().isBlank())
                ? req.customDisplayName().trim()
                : lead.name();

        String reviewerDisplayName = formatDisplayName(baseName, req.displayNameMode());

        UUID reviewId = UuidV7.randomUuid();
        Instant now = Instant.now();

        StudioReviewRecord review = new StudioReviewRecord(
                reviewId,
                invitation.studioId(),
                invitation.leadId(),
                invitation.projectId(),
                invitation.id(),
                req.rating(),
                cleanTitle,
                cleanReviewText,
                reviewerDisplayName,
                req.displayNameMode(),
                ReviewStatus.PUBLISHED,
                null,
                null,
                null,
                null,
                now,
                now,
                now,
                1L
        );

        reviewRepository.createReview(review);
        reviewRepository.markInvitationUsed(invitation.studioId(), invitation.id(), now);
        reviewRepository.revokeSessionsForInvitation(invitation.id());

        analyticsService.recordServerEvent(
                invitation.studioId(),
                AnalyticsEventType.REVIEW_SUBMITTED,
                "REVIEW",
                review.id(),
                "client_review",
                Map.of("rating", req.rating()),
                "review_sub:" + review.id()
        );

        return toDto(review, null);
    }

    public PublicStudioReviewsResponse getPublicReviews(String studioSlug, int limit, int offset) {
        StudioDetailRecord studio = studioRepository.findStudioBySlug(studioSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found with slug: " + studioSlug));

        ReviewAggregate agg = reviewRepository.getReviewAggregate(studio.id());
        List<PublicStudioReviewDto> reviews = reviewRepository.listPublishedReviews(studio.id(), limit, offset);
        int total = reviewRepository.countPublishedReviews(studio.id());

        PublicReviewAggregateDto aggDto = new PublicReviewAggregateDto(
                agg.averageRating(),
                agg.totalReviews(),
                agg.distribution()
        );

        return new PublicStudioReviewsResponse(aggDto, reviews, total, limit, offset);
    }

    public List<StudioReviewRecord> listStudioReviews(ActorContext actor, UUID studioId, int limit, int offset) {
        validateStudioAccess(actor, studioId);
        return reviewRepository.listStudioReviews(studioId, limit, offset);
    }

    public int countStudioReviews(ActorContext actor, UUID studioId) {
        validateStudioAccess(actor, studioId);
        return reviewRepository.countStudioReviews(studioId);
    }

    @Transactional
    public void respondToReview(ActorContext actor, UUID studioId, UUID reviewId, StudioReviewResponseRequest req) {
        validateStudioAccess(actor, studioId);

        StudioReviewRecord review = reviewRepository.findReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.studioId().equals(studioId)) {
            throw new AccessDeniedException("Review does not belong to studio: " + studioId);
        }

        String cleanResponse = stripHtml(req.responseText());
        if (cleanResponse.length() > 1500) {
            cleanResponse = cleanResponse.substring(0, 1500);
        }

        reviewRepository.updateStudioResponse(studioId, reviewId, cleanResponse, Instant.now());
    }

    @Transactional
    public void reportReview(UUID reviewId, ReviewReportRequest req, String clientIp, UUID reporterUserId) {
        StudioReviewRecord review = reviewRepository.findReviewById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        String cleanDetails = req.details() != null ? stripHtml(req.details()).trim() : null;
        if (cleanDetails != null && cleanDetails.length() > 500) {
            cleanDetails = cleanDetails.substring(0, 500);
        }

        ReviewReportRecord report = new ReviewReportRecord(
                UuidV7.randomUuid(),
                review.id(),
                review.studioId(),
                reporterUserId,
                clientIp,
                req.reason(),
                cleanDetails,
                ReportStatus.PENDING,
                Instant.now()
        );

        reviewRepository.createReport(report);
    }

    public ReviewAggregate getReviewAggregate(UUID studioId) {
        return reviewRepository.getReviewAggregate(studioId);
    }

    private String formatDisplayName(String fullName, DisplayNameMode mode) {
        if (fullName == null || fullName.isBlank()) {
            return "Anonymous Client";
        }
        String cleaned = stripHtml(fullName).trim();
        String[] tokens = cleaned.split("\\s+");

        return switch (mode) {
            case FIRST_NAME -> tokens[0];
            case INITIALS -> {
                StringBuilder sb = new StringBuilder();
                for (String t : tokens) {
                    if (!t.isEmpty()) {
                        sb.append(Character.toUpperCase(t.charAt(0))).append(". ");
                    }
                }
                yield sb.toString().trim();
            }
            case ANONYMOUS -> "Anonymous Client";
        };
    }

    private String stripHtml(String input) {
        if (input == null) return "";
        String withoutScripts = SCRIPT_STYLE_PATTERN.matcher(input).replaceAll("");
        return HTML_TAG_PATTERN.matcher(withoutScripts).replaceAll("").trim();
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

    private PublicStudioReviewDto toDto(StudioReviewRecord r, String projectTitle) {
        return new PublicStudioReviewDto(
                r.id(),
                r.rating(),
                r.title(),
                r.reviewText(),
                r.reviewerDisplayName(),
                r.displayNameMode(),
                r.publishedAt(),
                r.studioResponseText(),
                r.studioResponseAt(),
                r.projectId(),
                projectTitle
        );
    }
}
