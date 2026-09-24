package com.interior.platform.leads.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.leads.domain.*;
import com.interior.platform.leads.dto.*;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final SecurityRepository securityRepository;
    private final AuthorizationService authorizationService;
    private final PhoneNormalizationService phoneNormalizationService;

    public LeadService(
            LeadRepository leadRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            PhoneNormalizationService phoneNormalizationService
    ) {
        this.leadRepository = leadRepository;
        this.securityRepository = securityRepository;
        this.authorizationService = authorizationService;
        this.phoneNormalizationService = phoneNormalizationService;
    }

    @Transactional(readOnly = true)
    public List<LeadSummaryDto> listLeads(
            ActorContext actor,
            UUID requestedStudioId,
            LeadStatus status,
            UUID assignedUserId,
            String search,
            String sort,
            int limit,
            int offset
    ) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        List<StudioLeadRecord> records = leadRepository.listLeads(
                studioId, status, assignedUserId, search, sort, limit, offset
        );

        return records.stream().map(this::toSummaryDto).toList();
    }

    @Transactional(readOnly = true)
    public long countLeads(
            ActorContext actor,
            UUID requestedStudioId,
            LeadStatus status,
            UUID assignedUserId,
            String search
    ) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        return leadRepository.countLeads(studioId, status, assignedUserId, search);
    }

    @Transactional(readOnly = true)
    public LeadCountsDto getCounts(ActorContext actor, UUID requestedStudioId) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        return leadRepository.getCounts(studioId);
    }

    @Transactional(readOnly = true)
    public LeadDetailDto getLeadDetail(ActorContext actor, UUID requestedStudioId, UUID leadId) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        StudioLeadRecord lead = leadRepository.findById(studioId, leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        List<LeadNoteDto> notes = leadRepository.listNotes(studioId, leadId).stream()
                .map(n -> new LeadNoteDto(
                        n.id(),
                        n.authorId(),
                        leadRepository.getUserDisplayName(n.authorId()),
                        n.content(),
                        n.createdAt()
                ))
                .toList();

        List<LeadActivityDto> activities = leadRepository.listActivities(studioId, leadId).stream()
                .map(a -> new LeadActivityDto(
                        a.id(),
                        a.actorId(),
                        leadRepository.getUserDisplayName(a.actorId()),
                        a.activityType().name(),
                        a.activityType().getDisplayName(),
                        a.details(),
                        a.createdAt()
                ))
                .toList();

        List<WhatsAppMessageDto> messages = leadRepository.listWhatsAppMessages(studioId, leadId).stream()
                .map(m -> new WhatsAppMessageDto(
                        m.id(),
                        m.direction().name(),
                        m.provider(),
                        m.status().name(),
                        m.body(),
                        m.failureCode(),
                        m.createdAt(),
                        m.statusUpdatedAt()
                ))
                .toList();

        String projectTitle = lead.projectId() != null ? leadRepository.getProjectTitle(lead.projectId()) : null;
        String assignedUserName = lead.assignedUserId() != null ? leadRepository.getUserDisplayName(lead.assignedUserId()) : null;

        return new LeadDetailDto(
                lead.id(),
                lead.studioId(),
                lead.projectId(),
                projectTitle,
                null,
                lead.source().name(),
                lead.source().getDisplayName(),
                lead.status().name(),
                lead.status().getDisplayName(),
                lead.name(),
                phoneNormalizationService.mask(lead.phoneNormalized()),
                lead.phoneNormalized(),
                lead.emailNormalized(),
                lead.city(),
                lead.projectCategory(),
                lead.budgetRange(),
                lead.message(),
                lead.preferredContactChannel() != null ? lead.preferredContactChannel().name() : null,
                lead.contactConsentAt(),
                lead.whatsappConsentAt(),
                lead.whatsappConsentAt() != null,
                lead.assignedUserId(),
                assignedUserName,
                lead.nextFollowUpAt(),
                lead.lostReason(),
                lead.possibleDuplicate(),
                lead.createdAt(),
                lead.updatedAt(),
                lead.version(),
                lead.archivedAt(),
                notes,
                activities,
                messages
        );
    }

    @Transactional
    public LeadDetailDto updateLead(ActorContext actor, UUID requestedStudioId, UUID leadId, LeadUpdateRequest req) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        StudioLeadRecord current = leadRepository.findById(studioId, leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        long expectedVersion = req.expectedVersion() != null ? req.expectedVersion() : current.version();

        LeadStatus newStatus = current.status();
        if (req.status() != null && !req.status().isBlank()) {
            try {
                newStatus = LeadStatus.valueOf(req.status().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid lead status: " + req.status());
            }

            if (!current.status().canTransitionTo(newStatus)) {
                throw new BadRequestException("Cannot transition lead from " + current.status() + " to " + newStatus);
            }
        }

        String lostReason = (newStatus == LeadStatus.LOST) ? req.lostReason() : current.lostReason();
        Instant nextFollowUpAt = (req.nextFollowUpAt() != null) ? req.nextFollowUpAt() : current.nextFollowUpAt();

        StudioLeadRecord updatedRecord = new StudioLeadRecord(
                current.id(),
                current.studioId(),
                current.projectId(),
                current.source(),
                newStatus,
                current.name(),
                current.phoneNormalized(),
                current.emailNormalized(),
                current.city(),
                current.projectCategory(),
                current.budgetRange(),
                current.message(),
                current.preferredContactChannel(),
                current.contactConsentAt(),
                current.whatsappConsentAt(),
                current.assignedUserId(),
                nextFollowUpAt,
                lostReason,
                current.possibleDuplicate(),
                current.idempotencyKey(),
                current.createdAt(),
                Instant.now(),
                current.version() + 1,
                (newStatus == LeadStatus.ARCHIVED) ? Instant.now() : current.archivedAt()
        );

        boolean updated = leadRepository.update(updatedRecord, expectedVersion);
        if (!updated) {
            throw new ConflictException("Lead was modified concurrently by another user. Please refresh and try again.");
        }

        // Audit state changes
        if (newStatus != current.status()) {
            LeadActivityType actType = switch (newStatus) {
                case WON -> LeadActivityType.WON;
                case LOST -> LeadActivityType.LOST;
                case ARCHIVED -> LeadActivityType.ARCHIVED;
                default -> LeadActivityType.STATUS_CHANGED;
            };

            String details = "{\"oldStatus\":\"" + current.status().name() + "\",\"newStatus\":\"" + newStatus.name() + "\"}";
            leadRepository.saveActivity(new LeadActivityRecord(
                    UuidV7.randomUuid(),
                    leadId,
                    studioId,
                    actor.userId(),
                    actType,
                    details,
                    Instant.now()
            ));
        }

        if (nextFollowUpAt != null && !nextFollowUpAt.equals(current.nextFollowUpAt())) {
            leadRepository.saveActivity(new LeadActivityRecord(
                    UuidV7.randomUuid(),
                    leadId,
                    studioId,
                    actor.userId(),
                    LeadActivityType.FOLLOW_UP_CHANGED,
                    "{\"nextFollowUpAt\":\"" + nextFollowUpAt + "\"}",
                    Instant.now()
            ));
        }

        return getLeadDetail(actor, requestedStudioId, leadId);
    }

    @Transactional
    public LeadDetailDto assignLead(ActorContext actor, UUID requestedStudioId, UUID leadId, LeadAssignmentRequest req) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        StudioLeadRecord current = leadRepository.findById(studioId, leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        long expectedVersion = req.expectedVersion() != null ? req.expectedVersion() : current.version();

        if (req.assignedUserId() != null) {
            boolean isMember = leadRepository.isStudioMember(studioId, req.assignedUserId());
            if (!isMember) {
                throw new BadRequestException("Assignee is not an active member of this studio");
            }
        }

        StudioLeadRecord updatedRecord = new StudioLeadRecord(
                current.id(),
                current.studioId(),
                current.projectId(),
                current.source(),
                current.status(),
                current.name(),
                current.phoneNormalized(),
                current.emailNormalized(),
                current.city(),
                current.projectCategory(),
                current.budgetRange(),
                current.message(),
                current.preferredContactChannel(),
                current.contactConsentAt(),
                current.whatsappConsentAt(),
                req.assignedUserId(),
                current.nextFollowUpAt(),
                current.lostReason(),
                current.possibleDuplicate(),
                current.idempotencyKey(),
                current.createdAt(),
                Instant.now(),
                current.version() + 1,
                current.archivedAt()
        );

        boolean updated = leadRepository.update(updatedRecord, expectedVersion);
        if (!updated) {
            throw new ConflictException("Lead was modified concurrently by another user. Please refresh and try again.");
        }

        String assigneeName = req.assignedUserId() != null ? leadRepository.getUserDisplayName(req.assignedUserId()) : "Unassigned";
        leadRepository.saveActivity(new LeadActivityRecord(
                UuidV7.randomUuid(),
                leadId,
                studioId,
                actor.userId(),
                LeadActivityType.ASSIGNED,
                "{\"assignedUserName\":\"" + assigneeName + "\"}",
                Instant.now()
        ));

        return getLeadDetail(actor, requestedStudioId, leadId);
    }

    @Transactional
    public LeadNoteDto addNote(ActorContext actor, UUID requestedStudioId, UUID leadId, LeadNoteCreateRequest req) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        leadRepository.findById(studioId, leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        if (req.content() == null || req.content().isBlank()) {
            throw new BadRequestException("Note content cannot be empty");
        }

        UUID noteId = UuidV7.randomUuid();
        Instant now = Instant.now();
        String sanitizedContent = req.content().trim();
        if (sanitizedContent.length() > 4000) {
            sanitizedContent = sanitizedContent.substring(0, 4000);
        }

        LeadNoteRecord note = new LeadNoteRecord(
                noteId,
                leadId,
                studioId,
                actor.userId(),
                sanitizedContent,
                now
        );

        leadRepository.saveNote(note);

        leadRepository.saveActivity(new LeadActivityRecord(
                UuidV7.randomUuid(),
                leadId,
                studioId,
                actor.userId(),
                LeadActivityType.NOTE_ADDED,
                "{\"noteId\":\"" + noteId + "\"}",
                now
        ));

        return new LeadNoteDto(
                noteId,
                actor.userId(),
                actor.displayName(),
                sanitizedContent,
                now
        );
    }

    @Transactional
    public boolean archiveLead(ActorContext actor, UUID requestedStudioId, UUID leadId, Long expectedVersion) {
        UUID studioId = resolveStudioId(actor, requestedStudioId);
        StudioLeadRecord current = leadRepository.findById(studioId, leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        long version = expectedVersion != null ? expectedVersion : current.version();

        boolean archived = leadRepository.archive(studioId, leadId, version);
        if (!archived) {
            throw new ConflictException("Lead was modified concurrently. Please refresh.");
        }

        leadRepository.saveActivity(new LeadActivityRecord(
                UuidV7.randomUuid(),
                leadId,
                studioId,
                actor.userId(),
                LeadActivityType.ARCHIVED,
                "{\"status\":\"ARCHIVED\"}",
                Instant.now()
        ));

        return true;
    }

    private UUID resolveStudioId(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);

        List<StudioMemberRecord> memberships = securityRepository.getStudioMemberships(actor.userId());
        if (memberships == null || memberships.isEmpty()) {
            throw new AccessDeniedException("No studio membership found for account");
        }

        UUID studioId = requestedStudioId != null ? requestedStudioId : actor.activeStudioId();
        if (studioId == null) {
            studioId = memberships.get(0).studioId();
        }

        UUID finalStudioId = studioId;
        boolean hasAccess = memberships.stream().anyMatch(m -> m.studioId().equals(finalStudioId));
        if (!hasAccess && !actor.hasRole("SUPER_ADMIN") && !actor.hasRole("ADMIN")) {
            throw new AccessDeniedException("Access denied: you are not a member of the requested studio");
        }

        return studioId;
    }

    private LeadSummaryDto toSummaryDto(StudioLeadRecord l) {
        String projectTitle = l.projectId() != null ? leadRepository.getProjectTitle(l.projectId()) : null;
        String assignedUserName = l.assignedUserId() != null ? leadRepository.getUserDisplayName(l.assignedUserId()) : null;

        return new LeadSummaryDto(
                l.id(),
                l.studioId(),
                l.projectId(),
                projectTitle,
                null,
                l.source().name(),
                l.source().getDisplayName(),
                l.status().name(),
                l.status().getDisplayName(),
                l.name(),
                phoneNormalizationService.mask(l.phoneNormalized()),
                null,
                null,
                l.city(),
                l.projectCategory(),
                l.budgetRange(),
                l.preferredContactChannel() != null ? l.preferredContactChannel().name() : null,
                l.whatsappConsentAt() != null,
                l.assignedUserId(),
                assignedUserName,
                l.nextFollowUpAt(),
                l.possibleDuplicate(),
                l.createdAt(),
                l.updatedAt(),
                l.version(),
                l.archivedAt()
        );
    }
}
