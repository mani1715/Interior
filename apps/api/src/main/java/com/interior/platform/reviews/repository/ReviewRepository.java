package com.interior.platform.reviews.repository;

import com.interior.platform.reviews.domain.*;
import com.interior.platform.reviews.dto.PublicStudioReviewDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {

    ReviewInvitationRecord createInvitation(ReviewInvitationRecord invitation);

    Optional<ReviewInvitationRecord> findInvitationById(UUID id);

    Optional<ReviewInvitationRecord> findInvitationByLeadId(UUID studioId, UUID leadId);

    Optional<ReviewInvitationRecord> findInvitationByTokenHash(byte[] tokenHash);

    List<ReviewInvitationRecord> listInvitations(UUID studioId, int limit, int offset);

    int countInvitations(UUID studioId);

    void revokeInvitation(UUID studioId, UUID invitationId);

    void markInvitationUsed(UUID studioId, UUID invitationId, Instant usedAt);

    ReviewInvitationSessionRecord createSession(ReviewInvitationSessionRecord session);

    Optional<ReviewInvitationSessionRecord> findSessionByTokenHash(byte[] sessionTokenHash);

    void revokeSessionsForInvitation(UUID invitationId);

    StudioReviewRecord createReview(StudioReviewRecord review);

    Optional<StudioReviewRecord> findReviewById(UUID id);

    Optional<StudioReviewRecord> findReviewByInvitationId(UUID invitationId);

    Optional<StudioReviewRecord> findReviewByLeadId(UUID studioId, UUID leadId);

    List<StudioReviewRecord> listStudioReviews(UUID studioId, int limit, int offset);

    int countStudioReviews(UUID studioId);

    List<PublicStudioReviewDto> listPublishedReviews(UUID studioId, int limit, int offset);

    int countPublishedReviews(UUID studioId);

    ReviewAggregate getReviewAggregate(UUID studioId);

    void updateStudioResponse(UUID studioId, UUID reviewId, String responseText, Instant responseAt);

    ReviewReportRecord createReport(ReviewReportRecord report);

    void updateReviewStatus(UUID reviewId, ReviewStatus status, Instant flaggedAt, Instant removedAt);
}
