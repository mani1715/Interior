package com.interior.platform.privacy.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.privacy.domain.*;
import com.interior.platform.privacy.dto.LegalHoldDto;
import com.interior.platform.privacy.dto.PrivacyRequestResponse;
import com.interior.platform.privacy.repository.PrivacyRepository;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.SessionSecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PrivacyService {

    private final PrivacyRepository privacyRepository;
    private final SecurityRepository securityRepository;
    private final SessionSecurityService sessionSecurityService;
    private final AuditService auditService;

    public PrivacyService(
            PrivacyRepository privacyRepository,
            SecurityRepository securityRepository,
            SessionSecurityService sessionSecurityService,
            AuditService auditService
    ) {
        this.privacyRepository = privacyRepository;
        this.securityRepository = securityRepository;
        this.sessionSecurityService = sessionSecurityService;
        this.auditService = auditService;
    }

    public List<PrivacyRequestResponse> getRequestsForUser(UUID userId) {
        return privacyRepository.findPrivacyRequestsByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PrivacyRequestResponse> getRequestsForStudio(UUID studioId) {
        return privacyRepository.findPrivacyRequestsByStudioId(studioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PrivacyRequestResponse closeAccount(UUID userId) {
        // 1. Check Sole-Admin Studio Blocker
        if (privacyRepository.isSoleAdminOfActiveStudio(userId)) {
            throw new IllegalStateException(
                    "ACCOUNT_CLOSURE_BLOCKED_BY_STUDIO_OWNERSHIP: You are the sole administrator of an active studio. " +
                    "Transfer studio ownership or close the studio before closing your personal account."
            );
        }

        // 2. Check active legal hold
        if (privacyRepository.hasActiveLegalHold("USER", userId)) {
            throw new IllegalStateException(
                    "LEGAL_HOLD_ACTIVE: Your account is subject to an active compliance or legal hold. Account closure is temporarily suspended."
            );
        }

        UUID reqId = UuidV7.randomUuid();
        PrivacyRequestRecord req = privacyRepository.createPrivacyRequest(
                reqId,
                userId,
                null,
                PrivacyRequestType.ACCOUNT_CLOSURE,
                PrivacyRequestStatus.COMPLETED,
                Instant.now(),
                "{}"
        );

        // Perform immediate account closure: suspend status, set deactivated_at, revoke sessions
        securityRepository.deactivateUser(userId, Instant.now());
        sessionSecurityService.revokeAllUserSessions(userId);

        auditService.record(
                userId,
                null,
                "ACCOUNT_CLOSED",
                "USER",
                userId.toString(),
                Map.of("requestId", reqId.toString()),
                null,
                null
        );

        privacyRepository.updatePrivacyRequestStatus(reqId, PrivacyRequestStatus.COMPLETED, Instant.now(), null);
        return toResponse(req);
    }

    @Transactional
    public PrivacyRequestResponse deleteAccount(UUID userId) {
        // 1. Check Sole-Admin Studio Blocker
        if (privacyRepository.isSoleAdminOfActiveStudio(userId)) {
            throw new IllegalStateException(
                    "ACCOUNT_CLOSURE_BLOCKED_BY_STUDIO_OWNERSHIP: You are the sole administrator of an active studio. " +
                    "Transfer studio ownership or close the studio before requesting account deletion."
            );
        }

        // 2. Check active legal hold
        if (privacyRepository.hasActiveLegalHold("USER", userId)) {
            throw new IllegalStateException(
                    "LEGAL_HOLD_ACTIVE: Your account is subject to an active legal/compliance hold. Deletion is blocked."
            );
        }

        UUID reqId = UuidV7.randomUuid();
        PrivacyRequestRecord req = privacyRepository.createPrivacyRequest(
                reqId,
                userId,
                null,
                PrivacyRequestType.ACCOUNT_DELETION,
                PrivacyRequestStatus.PROCESSING,
                Instant.now(),
                "{}"
        );

        // Clean up personal non-financial data
        privacyRepository.cleanupUserPersonalData(userId);

        // Anonymize user record and credentials
        privacyRepository.anonymizeUserData(userId);

        auditService.recordRequired(
                userId,
                null,
                "ACCOUNT_ANONYMIZED_AND_DELETED",
                "USER",
                userId.toString(),
                Map.of("requestId", reqId.toString()),
                null,
                null
        );

        privacyRepository.updatePrivacyRequestStatus(reqId, PrivacyRequestStatus.COMPLETED, Instant.now(), null);
        return toResponse(req);
    }

    @Transactional
    public PrivacyRequestResponse closeStudio(UUID studioId, UUID actorUserId) {
        // Check studio-level legal hold
        if (privacyRepository.hasActiveLegalHold("STUDIO", studioId)) {
            throw new IllegalStateException(
                    "LEGAL_HOLD_ACTIVE: Studio is subject to an active legal/compliance hold. Studio closure is blocked."
            );
        }

        UUID reqId = UuidV7.randomUuid();
        PrivacyRequestRecord req = privacyRepository.createPrivacyRequest(
                reqId,
                actorUserId,
                studioId,
                PrivacyRequestType.STUDIO_CLOSURE,
                PrivacyRequestStatus.COMPLETED,
                Instant.now(),
                "{}"
        );

        privacyRepository.archiveStudioByOwner(studioId, Instant.now());

        auditService.recordRequired(
                actorUserId,
                studioId,
                "STUDIO_CLOSED_BY_OWNER",
                "STUDIO",
                studioId.toString(),
                Map.of("requestId", reqId.toString()),
                null,
                null
        );

        privacyRepository.updatePrivacyRequestStatus(reqId, PrivacyRequestStatus.COMPLETED, Instant.now(), null);
        return toResponse(req);
    }

    @org.springframework.transaction.annotation.Transactional
    public LegalHoldDto placeLegalHold(String subjectType, UUID subjectId, String reason, UUID placedBy) {
        UUID id = UuidV7.randomUuid();
        PrivacyLegalHoldRecord record = privacyRepository.createLegalHold(id, subjectType, subjectId, reason, placedBy, "{}");
        auditService.recordRequired(
                placedBy,
                null,
                "LEGAL_HOLD_PLACED",
                subjectType,
                subjectId.toString(),
                Map.of("reason", reason),
                null,
                null
        );
        return toLegalHoldDto(record);
    }

    @org.springframework.transaction.annotation.Transactional
    public void releaseLegalHold(UUID holdId, UUID adminUserId) {
        privacyRepository.releaseLegalHold(holdId, Instant.now());
        auditService.recordRequired(
                adminUserId,
                null,
                "LEGAL_HOLD_RELEASED",
                "LEGAL_HOLD",
                holdId.toString(),
                Map.of(),
                null,
                null
        );
    }

    public List<LegalHoldDto> getActiveLegalHolds(String subjectType, UUID subjectId) {
        return privacyRepository.findActiveLegalHolds(subjectType, subjectId).stream()
                .map(this::toLegalHoldDto)
                .toList();
    }

    private PrivacyRequestResponse toResponse(PrivacyRequestRecord r) {
        return new PrivacyRequestResponse(
                r.id(),
                r.userId(),
                r.studioId(),
                r.requestType().name(),
                r.status().name(),
                r.confirmedAt(),
                r.completedAt(),
                r.failureReason(),
                r.createdAt()
        );
    }

    private LegalHoldDto toLegalHoldDto(PrivacyLegalHoldRecord r) {
        return new LegalHoldDto(
                r.id(),
                r.subjectType(),
                r.subjectId(),
                r.reason(),
                r.placedBy(),
                r.placedAt(),
                r.releasedAt(),
                r.active()
        );
    }
}
