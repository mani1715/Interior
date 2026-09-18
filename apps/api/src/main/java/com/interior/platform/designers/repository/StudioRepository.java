package com.interior.platform.designers.repository;

import com.interior.platform.designers.domain.OnboardingDraftRecord;
import com.interior.platform.designers.domain.StudioDetailRecord;

import java.util.Optional;
import java.util.UUID;

public interface StudioRepository {
    void saveDraft(UUID userId, int step, String draftPayload, String status);
    Optional<OnboardingDraftRecord> findDraftByUserId(UUID userId);
    void markDraftCompleted(UUID userId);

    boolean isSlugClaimed(String slug);
    void claimSlug(UUID studioId, String slug, String state);

    void createStudio(StudioDetailRecord studio);
    void addStudioContact(UUID studioId, String kind, String value, boolean publicConsent, int sortOrder);
    void addStudioService(UUID studioId, String serviceCode, String serviceName);
    void addStudioSpecialty(UUID studioId, String specialtyCode, String specialtyName);
    void addStudioServiceArea(UUID studioId, String cityName, String locality);

    java.util.List<com.interior.platform.designers.domain.StudioSpecialtyRecord> getStudioSpecialties(UUID studioId);

    Optional<StudioDetailRecord> findStudioById(UUID studioId);
    Optional<StudioDetailRecord> findStudioByOwnerId(UUID ownerId);
    Optional<StudioDetailRecord> findStudioBySlug(String slug);

    Optional<UUID> findInitialOnboardingStudioId(UUID userId);
    void recordInitialOnboardingCompletion(UUID userId, UUID studioId);
    boolean hasCompletedOnboarding(UUID userId);
}
