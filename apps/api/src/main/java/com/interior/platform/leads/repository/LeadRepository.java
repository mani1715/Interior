package com.interior.platform.leads.repository;

import com.interior.platform.leads.domain.*;
import com.interior.platform.leads.dto.LeadCountsDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository {

    StudioLeadRecord save(StudioLeadRecord lead);

    Optional<StudioLeadRecord> findById(UUID studioId, UUID leadId);

    boolean update(StudioLeadRecord lead, long expectedVersion);

    boolean archive(UUID studioId, UUID leadId, long expectedVersion);

    List<StudioLeadRecord> listLeads(
            UUID studioId,
            LeadStatus status,
            UUID assignedUserId,
            String search,
            String sort,
            int limit,
            int offset
    );

    long countLeads(
            UUID studioId,
            LeadStatus status,
            UUID assignedUserId,
            String search
    );

    LeadCountsDto getCounts(UUID studioId);

    Optional<StudioLeadRecord> findRecentDuplicate(
            UUID studioId,
            String phoneNormalized,
            String emailNormalized,
            Instant since
    );

    Optional<StudioLeadRecord> findByIdempotencyKey(UUID studioId, String idempotencyKey);

    void saveActivity(LeadActivityRecord activity);

    List<LeadActivityRecord> listActivities(UUID studioId, UUID leadId);

    void saveNote(LeadNoteRecord note);

    List<LeadNoteRecord> listNotes(UUID studioId, UUID leadId);

    void saveWhatsAppMessage(LeadWhatsAppMessageRecord message);

    boolean updateWhatsAppMessageStatus(
            UUID studioId,
            UUID messageId,
            WhatsAppMessageStatus status,
            String failureCode
    );

    Optional<LeadWhatsAppMessageRecord> findWhatsAppMessageByProviderId(String provider, String providerMessageId);

    List<LeadWhatsAppMessageRecord> listWhatsAppMessages(UUID studioId, UUID leadId);

    // Target validation helpers
    record PublicStudioTarget(UUID id, String name, String slug) {}
    record PublicProjectTarget(UUID id, UUID studioId, String title, String slug) {}
    record PublicWhatsAppContact(UUID id, UUID studioId, String contactValue) {}

    Optional<PublicStudioTarget> findPublicStudioBySlug(String slug);

    Optional<PublicStudioTarget> findPublicStudioById(UUID studioId);

    Optional<PublicProjectTarget> findPublicProjectBySlug(UUID studioId, String projectSlug);

    Optional<PublicProjectTarget> findPublicProjectById(UUID studioId, UUID projectId);

    Optional<PublicWhatsAppContact> findPublicWhatsAppContact(UUID studioId);

    boolean isStudioMember(UUID studioId, UUID userId);

    String getUserDisplayName(UUID userId);

    String getProjectTitle(UUID projectId);
}
