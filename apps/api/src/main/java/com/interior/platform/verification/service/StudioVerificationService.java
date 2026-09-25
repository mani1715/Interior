package com.interior.platform.verification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.verification.domain.*;
import com.interior.platform.verification.dto.*;
import com.interior.platform.verification.repository.VerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class StudioVerificationService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L; // 10MB
    private static final Duration DEFAULT_VERIFICATION_EXPIRY = Duration.ofDays(365);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private final VerificationRepository verificationRepository;
    private final StudioRepository studioRepository;
    private final ObjectMapper objectMapper;

    public StudioVerificationService(
            VerificationRepository verificationRepository,
            StudioRepository studioRepository,
            ObjectMapper objectMapper
    ) {
        this.verificationRepository = verificationRepository;
        this.studioRepository = studioRepository;
        this.objectMapper = objectMapper;
    }

    public StudioVerificationDto getStudioVerification(ActorContext actor, UUID studioId) {
        validateStudioAccess(actor, studioId);
        StudioVerificationRecord record = getOrCreateRecord(studioId);

        List<VerificationDocumentDto> docs = verificationRepository.listDocuments(studioId).stream()
                .map(d -> new VerificationDocumentDto(
                        d.id(),
                        d.documentType().name(),
                        d.originalFilename(),
                        d.mimeType(),
                        d.fileSizeBytes(),
                        d.createdAt()
                )).toList();

        List<VerificationEventDto> events = verificationRepository.listEvents(studioId).stream()
                .map(e -> new VerificationEventDto(
                        e.id(),
                        e.eventType().name(),
                        e.actorUserId(),
                        e.reason(),
                        e.createdAt()
                )).toList();

        return toDto(record, docs, events);
    }

    @Transactional
    public StudioVerificationDto submitVerification(
            ActorContext actor,
            UUID studioId,
            SubmitVerificationRequest req
    ) {
        validateStudioAdminAccess(actor, studioId);
        StudioVerificationRecord existing = getOrCreateRecord(studioId);

        if (existing.status() == VerificationStatus.PENDING) {
            throw new IllegalStateException("A verification request is already PENDING review.");
        }

        String cleanBusinessName = stripHtml(req.businessName());
        String cleanProfType = stripHtml(req.professionalType());
        String cleanRegNumber = req.registrationNumber() != null ? stripHtml(req.registrationNumber()) : null;
        String cleanGst = req.gstNumber() != null ? stripHtml(req.gstNumber()) : null;
        String cleanDomain = req.websiteDomain() != null ? stripHtml(req.websiteDomain()) : null;
        String cleanNotes = req.notes() != null ? stripHtml(req.notes()) : null;

        Instant now = Instant.now();
        StudioVerificationRecord updated = new StudioVerificationRecord(
                existing.id(),
                studioId,
                VerificationStatus.PENDING,
                cleanBusinessName,
                cleanProfType,
                cleanRegNumber,
                cleanGst,
                cleanDomain,
                cleanNotes,
                null,
                existing.verifiedAt(),
                existing.expiresAt(),
                existing.verifiedSnapshot(),
                existing.createdAt(),
                now,
                existing.version() + 1
        );

        verificationRepository.save(updated);

        // Record submission event
        VerificationEventRecord event = new VerificationEventRecord(
                UuidV7.randomUuid(),
                existing.id(),
                studioId,
                VerificationEventType.SUBMITTED,
                actor.userId(),
                "Studio submitted business verification request",
                null,
                now
        );
        verificationRepository.saveEvent(event);

        return getStudioVerification(actor, studioId);
    }

    @Transactional
    public VerificationDocumentDto uploadDocument(
            ActorContext actor,
            UUID studioId,
            VerificationDocumentType docType,
            String filename,
            String mimeType,
            long sizeBytes
    ) {
        validateStudioAccess(actor, studioId);
        StudioVerificationRecord verification = getOrCreateRecord(studioId);

        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type. Allowed formats: PDF, JPEG, PNG.");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be between 1 byte and 10MB.");
        }

        UUID docId = UuidV7.randomUuid();
        String safeFilename = sanitizeFilename(filename);
        String storageKey = "verification-evidence/" + studioId + "/" + docId + "-" + safeFilename;
        Instant now = Instant.now();

        VerificationDocumentRecord doc = new VerificationDocumentRecord(
                docId,
                verification.id(),
                studioId,
                docType,
                storageKey,
                safeFilename,
                mimeType.toLowerCase(),
                sizeBytes,
                now
        );

        verificationRepository.saveDocument(doc);

        // Record document added event
        VerificationEventRecord event = new VerificationEventRecord(
                UuidV7.randomUuid(),
                verification.id(),
                studioId,
                VerificationEventType.DOCUMENT_ADDED,
                actor.userId(),
                "Attached evidence document: " + docType.name(),
                null,
                now
        );
        verificationRepository.saveEvent(event);

        return new VerificationDocumentDto(
                doc.id(),
                doc.documentType().name(),
                doc.originalFilename(),
                doc.mimeType(),
                doc.fileSizeBytes(),
                doc.createdAt()
        );
    }

    @Transactional
    public StudioVerificationDto adminDecision(
            ActorContext actor,
            UUID studioId,
            AdminVerificationDecisionRequest req
    ) {
        // Enforce admin boundary: studio owner/member cannot approve themselves!
        if (actor == null || (!actor.hasRole("ADMIN") && !actor.hasRole("SUPER_ADMIN"))) {
            throw new AccessDeniedException("Platform administrator privileges required to review verification");
        }

        StudioVerificationRecord verification = verificationRepository.findByStudioId(studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification record not found for studio: " + studioId));

        Instant now = Instant.now();
        Instant verifiedAt = null;
        Instant expiresAt = null;
        String verifiedSnapshot = null;
        VerificationEventType eventType = VerificationEventType.valueOf(req.status().name());

        if (req.status() == VerificationStatus.VERIFIED) {
            verifiedAt = now;
            expiresAt = now.plus(DEFAULT_VERIFICATION_EXPIRY);
            verifiedSnapshot = buildSnapshotJson(verification, now);
        } else if (req.status() == VerificationStatus.REVERIFY_REQUIRED || req.status() == VerificationStatus.REJECTED || req.status() == VerificationStatus.EXPIRED) {
            verifiedAt = null;
            expiresAt = null;
        }

        verificationRepository.updateStatusAndDecision(
                studioId,
                req.status(),
                req.reason(),
                verifiedAt,
                expiresAt,
                verifiedSnapshot
        );

        VerificationEventRecord event = new VerificationEventRecord(
                UuidV7.randomUuid(),
                verification.id(),
                studioId,
                eventType,
                actor.userId(),
                req.reason(),
                verifiedSnapshot,
                now
        );
        verificationRepository.saveEvent(event);

        return getStudioVerification(actor, studioId);
    }

    @Transactional
    public void handleCriticalProfileChange(UUID studioId, String newBusinessName, String newProfessionalType) {
        Optional<StudioVerificationRecord> opt = verificationRepository.findByStudioId(studioId);
        if (opt.isEmpty()) return;

        StudioVerificationRecord v = opt.get();
        if (v.status() != VerificationStatus.VERIFIED) {
            return;
        }

        // Check if verified snapshot exists and whether critical fields changed
        boolean criticalMismatch = false;
        if (v.verifiedSnapshot() != null && !v.verifiedSnapshot().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(v.verifiedSnapshot(), Map.class);
                String oldName = (String) map.get("businessName");
                String oldProf = (String) map.get("professionalType");

                if ((newBusinessName != null && !newBusinessName.trim().equalsIgnoreCase(oldName)) ||
                    (newProfessionalType != null && !newProfessionalType.trim().equalsIgnoreCase(oldProf))) {
                    criticalMismatch = true;
                }
            } catch (JsonProcessingException e) {
                criticalMismatch = true;
            }
        } else {
            if (!v.businessName().equalsIgnoreCase(newBusinessName) ||
                !v.professionalType().equalsIgnoreCase(newProfessionalType)) {
                criticalMismatch = true;
            }
        }

        if (criticalMismatch) {
            Instant now = Instant.now();
            verificationRepository.updateStatusAndDecision(
                    studioId,
                    VerificationStatus.REVERIFY_REQUIRED,
                    "Critical business identity fields modified. Reverification required.",
                    null,
                    null,
                    null
            );

            VerificationEventRecord event = new VerificationEventRecord(
                    UuidV7.randomUuid(),
                    v.id(),
                    studioId,
                    VerificationEventType.REVERIFY_REQUIRED,
                    null,
                    "Automatic invalidation due to profile business name or professional type modification",
                    null,
                    now
            );
            verificationRepository.saveEvent(event);
        }
    }

    public PublicVerificationBadgeDto getPublicBadge(UUID studioId) {
        Optional<StudioVerificationRecord> opt = verificationRepository.findByStudioId(studioId);
        if (opt.isEmpty()) {
            return PublicVerificationBadgeDto.unverified();
        }

        StudioVerificationRecord record = opt.get();
        if (record.isVerified()) {
            return PublicVerificationBadgeDto.verified(record.verifiedAt());
        }

        return PublicVerificationBadgeDto.unverified();
    }

    public PublicVerificationBadgeDto getPublicBadgeBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return PublicVerificationBadgeDto.unverified();
        }
        return studioRepository.findStudioBySlug(slug)
                .map(s -> getPublicBadge(s.id()))
                .orElseGet(PublicVerificationBadgeDto::unverified);
    }

    private StudioVerificationRecord getOrCreateRecord(UUID studioId) {
        return verificationRepository.findByStudioId(studioId).orElseGet(() -> {
            StudioDetailRecord studio = studioRepository.findStudioById(studioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Studio not found: " + studioId));

            Instant now = Instant.now();
            StudioVerificationRecord newRecord = new StudioVerificationRecord(
                    UuidV7.randomUuid(),
                    studioId,
                    VerificationStatus.NOT_SUBMITTED,
                    studio.name(),
                    studio.professionalType() != null ? studio.professionalType() : "INTERIOR_DESIGNER",
                    null,
                    studio.gstNumber(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    now,
                    now,
                    1L
            );
            return verificationRepository.save(newRecord);
        });
    }

    private String buildSnapshotJson(StudioVerificationRecord v, Instant verifiedAt) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("businessName", v.businessName());
        snapshot.put("professionalType", v.professionalType());
        snapshot.put("registrationNumber", v.registrationNumber());
        snapshot.put("gstNumber", v.gstNumber());
        snapshot.put("websiteDomain", v.websiteDomain());
        snapshot.put("verifiedAt", verifiedAt.toString());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{\"businessName\":\"" + v.businessName() + "\"}";
        }
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

    private void validateStudioAdminAccess(ActorContext actor, UUID studioId) {
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN")) {
            return;
        }
        if (!actor.isStudioOwnerOrAdmin(studioId)) {
            throw new AccessDeniedException("Studio Owner or Admin role required for verification actions");
        }
    }

    private String stripHtml(String input) {
        if (input == null) return "";
        return HTML_TAG_PATTERN.matcher(input).replaceAll("").trim();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.pdf";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private StudioVerificationDto toDto(
            StudioVerificationRecord r,
            List<VerificationDocumentDto> docs,
            List<VerificationEventDto> events
    ) {
        return new StudioVerificationDto(
                r.id(),
                r.studioId(),
                r.status().name(),
                r.businessName(),
                r.professionalType(),
                r.registrationNumber(),
                r.gstNumber(),
                r.websiteDomain(),
                r.notes(),
                r.decisionReason(),
                r.verifiedAt(),
                r.expiresAt(),
                docs,
                events
        );
    }
}
