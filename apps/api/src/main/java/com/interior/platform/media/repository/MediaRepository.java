package com.interior.platform.media.repository;

import com.interior.platform.media.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository {

    // 1. Watermark Settings
    Optional<StudioWatermarkSettingsRecord> findWatermarkSettings(UUID studioId);
    StudioWatermarkSettingsRecord saveWatermarkSettings(StudioWatermarkSettingsRecord settings);

    // 2. Upload Intents
    UploadIntentRecord createUploadIntent(UploadIntentRecord intent);
    Optional<UploadIntentRecord> findUploadIntent(UUID intentId, UUID studioId);
    Optional<UploadIntentRecord> findUploadIntentGlobal(UUID intentId);
    void updateUploadIntentStatus(UUID intentId, UploadIntentStatus status);

    // 3. Media Assets
    MediaAssetRecord createMediaAsset(MediaAssetRecord asset);
    Optional<MediaAssetRecord> findMediaAsset(UUID mediaId, UUID studioId);
    List<MediaAssetRecord> findMediaAssetsByProject(UUID projectId, UUID studioId, boolean includeDeleted);
    List<MediaAssetRecord> findMediaAssetsByStudio(
            UUID studioId,
            UUID projectId,
            MediaType mediaType,
            MediaVisibility visibility,
            MediaProcessingStatus status,
            boolean includeDeleted
    );
    Optional<MediaAssetRecord> findCoverMedia(UUID projectId, UUID studioId);
    void updateMediaAsset(MediaAssetRecord asset);
    void unsetOtherCovers(UUID projectId, UUID studioId, UUID keepCoverMediaId);
    void updateSortOrder(UUID mediaId, UUID studioId, int sortOrder);
    void softDeleteMediaAsset(UUID mediaId, UUID studioId);
    int getNextSortOrder(UUID projectId, UUID studioId);
    long countActiveMediaByStudio(UUID studioId);
    int countCommittedPortfolioPhotos(UUID studioId, UUID projectId);
    int countPendingPortfolioUploadIntents(UUID studioId, UUID projectId);

    // 3b. Room Association & Room Covers
    void unsetOtherRoomCovers(UUID roomId, UUID studioId, UUID keepCoverMediaId);
    void clearRoomForMediaByRoomId(UUID roomId, UUID studioId);
    int countMediaByRoom(UUID roomId, UUID studioId);
    Optional<MediaAssetRecord> findRoomCoverMedia(UUID roomId, UUID studioId);
    List<MediaAssetRecord> findMediaAssetsByRoom(UUID roomId, UUID studioId);

    // 4. Media Derivatives
    void saveDerivatives(List<MediaDerivativeRecord> derivatives);
    List<MediaDerivativeRecord> findDerivativesByMediaId(UUID mediaId, UUID studioId);
    void deleteDerivativesByMediaId(UUID mediaId, UUID studioId);
}
