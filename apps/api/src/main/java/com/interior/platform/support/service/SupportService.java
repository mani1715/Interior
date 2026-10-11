package com.interior.platform.support.service;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.notifications.domain.NotificationType;
import com.interior.platform.notifications.service.NotificationService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.support.domain.*;
import com.interior.platform.support.dto.*;
import com.interior.platform.support.repository.SupportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SupportService {

    private static final Logger log = LoggerFactory.getLogger(SupportService.class);

    private final SupportRepository supportRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public SupportService(
            SupportRepository supportRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            JdbcTemplate jdbcTemplate,
            @Autowired(required = false) NotificationService notificationService
    ) {
        this.supportRepository = supportRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    @Transactional
    public SupportRequestDetailResponse createRequest(ActorContext actor, CreateSupportRequest req, String ip, String ua) {
        authorizationService.requireAuthenticated(actor);

        // Rate limit: max 5 requests per hour per user
        int recentCount = supportRepository.countUserRequestsLastHour(actor.userId());
        if (recentCount >= 5) {
            throw new BadRequestException("Rate limit exceeded: You may open a maximum of 5 support requests per hour.");
        }

        UUID studioId = req.studioId();
        if (studioId != null && !actor.isStudioMember(studioId) && !actor.hasRole("ADMIN") && !actor.hasRole("SUPER_ADMIN")) {
            throw new UnauthorizedException("You are not authorized to create a support request for this studio.");
        }

        Instant now = Instant.now();
        UUID requestId = UuidV7.randomUuid();

        SupportRequestRecord requestRecord = new SupportRequestRecord(
                requestId,
                actor.userId(),
                studioId,
                req.category(),
                req.subject().trim(),
                SupportStatus.OPEN,
                SupportPriority.NORMAL,
                now,
                now,
                null
        );

        supportRepository.createRequest(requestRecord);

        // Create first message
        UUID messageId = UuidV7.randomUuid();
        SupportMessageRecord initialMessage = new SupportMessageRecord(
                messageId,
                requestId,
                actor.userId(),
                SupportAuthorType.USER,
                req.message().trim(),
                false,
                now
        );
        supportRepository.createMessage(initialMessage);

        auditService.record(
                actor.userId(),
                studioId,
                "SUPPORT_REQUEST_CREATED",
                "SUPPORT_REQUEST",
                requestId.toString(),
                Map.of("category", req.category().name(), "subject", req.subject()),
                ip,
                ua
        );

        return getRequestDetail(actor, requestId);
    }

    @Transactional
    public SupportMessageResponse addMessage(ActorContext actor, UUID requestId, CreateSupportMessageRequest req, String ip, String ua) {
        authorizationService.requireAuthenticated(actor);

        SupportRequestRecord request = supportRepository.findRequestById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Support request not found: " + requestId));

        boolean isAdmin = actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN") || actor.hasRole("MODERATOR");
        boolean isOwner = request.userId().equals(actor.userId());
        boolean isStudioMember = request.studioId() != null && actor.isStudioMember(request.studioId());

        if (!isAdmin && !isOwner && !isStudioMember) {
            throw new UnauthorizedException("You are not authorized to message on this support request.");
        }

        // Rate limit: max 30 messages per minute
        int recentMsgs = supportRepository.countRecentUserMessages(actor.userId(), Instant.now().minus(1, ChronoUnit.MINUTES));
        if (recentMsgs >= 30) {
            throw new BadRequestException("Rate limit exceeded: Please wait before sending more messages.");
        }

        Instant now = Instant.now();
        boolean isInternal = isAdmin && Boolean.TRUE.equals(req.isInternal());
        SupportAuthorType authorType = isAdmin ? SupportAuthorType.SUPPORT_ADMIN : SupportAuthorType.USER;

        UUID messageId = UuidV7.randomUuid();
        SupportMessageRecord message = new SupportMessageRecord(
                messageId,
                requestId,
                actor.userId(),
                authorType,
                req.message().trim(),
                isInternal,
                now
        );
        supportRepository.createMessage(message);

        // Status transitions
        if (!isAdmin) {
            if (request.status() == SupportStatus.WAITING_USER || request.status() == SupportStatus.RESOLVED) {
                supportRepository.updateRequestStatus(requestId, SupportStatus.IN_PROGRESS, null, null);
            }
        } else if (!isInternal) {
            if (request.status() == SupportStatus.OPEN) {
                supportRepository.updateRequestStatus(requestId, SupportStatus.WAITING_USER, null, null);
            }
            // Send in-app notification to the ticket owner
            if (notificationService != null && !request.userId().equals(actor.userId())) {
                try {
                    notificationService.dispatchNotification(
                            request.userId(),
                            request.studioId(),
                            NotificationType.SYSTEM_NOTICE,
                            "Support Reply: " + request.subject(),
                            "A support specialist has replied to your request.",
                            "/help?ticket=" + request.id(),
                            "{}"
                    );
                } catch (Exception e) {
                    log.warn("Failed to dispatch in-app support notification to user {}: {}", request.userId(), e.getMessage());
                }
            }
        }

        auditService.record(
                actor.userId(),
                request.studioId(),
                "SUPPORT_MESSAGE_ADDED",
                "SUPPORT_REQUEST",
                requestId.toString(),
                Map.of("messageId", messageId.toString(), "isInternal", isInternal),
                ip,
                ua
        );

        return toMessageResponse(message, actor.displayName());
    }

    public SupportRequestDetailResponse getRequestDetail(ActorContext actor, UUID requestId) {
        authorizationService.requireAuthenticated(actor);

        SupportRequestRecord request = supportRepository.findRequestById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Support request not found: " + requestId));

        boolean isAdmin = actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN") || actor.hasRole("MODERATOR");
        boolean isOwner = request.userId().equals(actor.userId());
        boolean isStudioMember = request.studioId() != null && actor.isStudioMember(request.studioId());

        if (!isAdmin && !isOwner && !isStudioMember) {
            throw new UnauthorizedException("You are not authorized to view this support request.");
        }

        List<SupportMessageRecord> messages = supportRepository.findMessagesByRequestId(requestId, isAdmin);

        Map<UUID, String> displayNames = resolveUserDisplayNames(messages.stream().map(SupportMessageRecord::authorUserId).toList());

        List<SupportMessageResponse> messageResponses = messages.stream()
                .map(m -> toMessageResponse(m, displayNames.getOrDefault(m.authorUserId(), "User")))
                .toList();

        String userDisplayName = resolveSingleDisplayName(request.userId());
        String userEmail = resolveSingleEmail(request.userId());
        String studioName = request.studioId() != null ? resolveStudioName(request.studioId()) : null;

        return new SupportRequestDetailResponse(
                request.id(),
                request.userId(),
                userDisplayName,
                userEmail,
                request.studioId(),
                studioName,
                request.category(),
                request.subject(),
                request.status(),
                request.priority(),
                request.createdAt(),
                request.updatedAt(),
                request.resolvedAt(),
                messageResponses
        );
    }

    public List<SupportRequestSummaryResponse> listUserRequests(ActorContext actor, int limit, int offset) {
        authorizationService.requireAuthenticated(actor);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        List<SupportRequestRecord> requests = supportRepository.findRequestsByUserId(actor.userId(), boundedLimit, boundedOffset);
        return toSummaryResponses(requests);
    }

    public List<SupportRequestSummaryResponse> listStudioRequests(ActorContext actor, UUID studioId, int limit, int offset) {
        authorizationService.requireAuthenticated(actor);
        if (!actor.isStudioMember(studioId) && !actor.hasRole("ADMIN") && !actor.hasRole("SUPER_ADMIN")) {
            throw new UnauthorizedException("You are not authorized to view support requests for this studio.");
        }
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        List<SupportRequestRecord> requests = supportRepository.findRequestsByStudioId(studioId, boundedLimit, boundedOffset);
        return toSummaryResponses(requests);
    }

    public List<SupportRequestSummaryResponse> listAllRequests(
            ActorContext actor,
            int limit,
            int offset,
            SupportStatus status,
            SupportCategory category,
            SupportPriority priority
    ) {
        authorizationService.requireAnyPlatformRole(actor, "MODERATOR", "ADMIN");
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);

        List<SupportRequestRecord> requests = supportRepository.listAllRequests(boundedLimit, boundedOffset, status, category, priority);
        return toSummaryResponses(requests);
    }

    @Transactional
    public void updateRequestStatus(ActorContext actor, UUID requestId, UpdateSupportStatusRequest req, String ip, String ua) {
        authorizationService.requireAuthenticated(actor);

        SupportRequestRecord request = supportRepository.findRequestById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Support request not found: " + requestId));

        boolean isAdmin = actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN") || actor.hasRole("MODERATOR");
        boolean isOwner = request.userId().equals(actor.userId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("You are not authorized to update this support request.");
        }

        SupportStatus newStatus = req.status();
        SupportPriority newPriority = isAdmin ? req.priority() : null;

        // If user is closing their own ticket
        if (!isAdmin && newStatus != null && newStatus != SupportStatus.CLOSED) {
            throw new BadRequestException("Non-admin users can only transition tickets to CLOSED.");
        }

        Instant resolvedAt = (newStatus == SupportStatus.RESOLVED || newStatus == SupportStatus.CLOSED) ? Instant.now() : null;

        supportRepository.updateRequestStatus(requestId, newStatus, newPriority, resolvedAt);

        auditService.record(
                actor.userId(),
                request.studioId(),
                "SUPPORT_STATUS_UPDATED",
                "SUPPORT_REQUEST",
                requestId.toString(),
                Map.of(
                        "oldStatus", request.status().name(),
                        "newStatus", newStatus != null ? newStatus.name() : request.status().name(),
                        "priority", newPriority != null ? newPriority.name() : request.priority().name()
                ),
                ip,
                ua
        );
    }

    private List<SupportRequestSummaryResponse> toSummaryResponses(List<SupportRequestRecord> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        return requests.stream().map(r -> {
            String userName = resolveSingleDisplayName(r.userId());
            String userEmail = resolveSingleEmail(r.userId());
            String studioName = r.studioId() != null ? resolveStudioName(r.studioId()) : null;
            return new SupportRequestSummaryResponse(
                    r.id(),
                    r.userId(),
                    userName,
                    userEmail,
                    r.studioId(),
                    studioName,
                    r.category(),
                    r.subject(),
                    r.status(),
                    r.priority(),
                    0,
                    r.createdAt(),
                    r.updatedAt(),
                    r.resolvedAt()
            );
        }).toList();
    }

    private SupportMessageResponse toMessageResponse(SupportMessageRecord m, String displayName) {
        return new SupportMessageResponse(
                m.id(),
                m.requestId(),
                m.authorUserId(),
                displayName,
                m.authorType(),
                m.body(),
                m.isInternal(),
                m.createdAt()
        );
    }

    private Map<UUID, String> resolveUserDisplayNames(List<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<UUID, String> map = new HashMap<>();
        for (UUID uid : userIds) {
            map.put(uid, resolveSingleDisplayName(uid));
        }
        return map;
    }

    private String resolveSingleDisplayName(UUID userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT display_name FROM users WHERE id = ?", String.class, userId);
        } catch (Exception e) {
            return "User";
        }
    }

    private String resolveSingleEmail(UUID userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, userId);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveStudioName(UUID studioId) {
        try {
            return jdbcTemplate.queryForObject("SELECT name FROM designer_studios WHERE id = ?", String.class, studioId);
        } catch (Exception e) {
            return null;
        }
    }
}
