package com.interior.platform.ai.repository;

import com.interior.platform.ai.domain.AiClientReviewCommentRecord;
import com.interior.platform.ai.domain.AiClientReviewDecisionRecord;
import com.interior.platform.ai.domain.AiClientReviewItemRecord;
import com.interior.platform.ai.domain.AiClientReviewRecord;
import com.interior.platform.ai.domain.AiClientReviewSessionRecord;
import com.interior.platform.ai.domain.ClientReviewStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiClientReviewRepository {

    void createReview(AiClientReviewRecord review);

    Optional<AiClientReviewRecord> findReviewById(UUID studioId, UUID reviewId);

    Optional<AiClientReviewRecord> findReviewByIdGlobal(UUID reviewId);

    Optional<AiClientReviewRecord> findReviewByTokenHash(byte[] tokenHash);

    List<AiClientReviewRecord> listReviewsByStudio(UUID studioId, UUID projectId, int limit, int offset);

    int countReviewsByStudio(UUID studioId, UUID projectId);

    void updateReviewStatus(UUID studioId, UUID reviewId, ClientReviewStatus status);

    void updateTokenHash(UUID studioId, UUID reviewId, byte[] newTokenHash);

    void updateReviewCurrentApprovedJob(UUID reviewId, UUID jobId);

    void createReviewSession(AiClientReviewSessionRecord session);

    Optional<AiClientReviewSessionRecord> findReviewSessionByTokenHash(byte[] sessionTokenHash);

    void revokeAllSessionsForReview(UUID reviewId);

    void createReviewItems(List<AiClientReviewItemRecord> items);

    List<AiClientReviewItemRecord> findItemsByReviewId(UUID reviewId);

    Optional<AiClientReviewItemRecord> findItemByReviewAndJob(UUID reviewId, UUID jobId);

    boolean isMediaInReview(UUID reviewId, UUID mediaId);

    void createDecision(AiClientReviewDecisionRecord decision);

    void clearCurrentDecisionsForReview(UUID reviewId);

    List<AiClientReviewDecisionRecord> findDecisionsByReviewId(UUID reviewId);

    Optional<AiClientReviewDecisionRecord> findCurrentDecisionForJob(UUID reviewId, UUID jobId);

    boolean hasAnyClientInteraction(UUID reviewId);

    void createComment(AiClientReviewCommentRecord comment);

    List<AiClientReviewCommentRecord> findCommentsByReviewId(UUID reviewId);
}
