package com.interior.platform.notifications.service;

import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.notifications.domain.NotificationPreferencesRecord;
import com.interior.platform.notifications.domain.NotificationRecord;
import com.interior.platform.notifications.domain.NotificationType;
import com.interior.platform.notifications.dto.NotificationDto;
import com.interior.platform.notifications.dto.NotificationListResponse;
import com.interior.platform.notifications.dto.NotificationPreferencesDto;
import com.interior.platform.notifications.dto.UpdateNotificationPreferencesRequest;
import com.interior.platform.notifications.repository.NotificationRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final AuthorizationService authorizationService;
    private final com.interior.platform.realtime.service.RealtimeEventPublisher realtimeEventPublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            AuthorizationService authorizationService,
            com.interior.platform.realtime.service.RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.authorizationService = authorizationService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    /**
     * Internal method to dispatch a notification to a specific user.
     * Respects user notification preferences.
     */
    @Transactional
    public void dispatchNotification(
            UUID targetUserId,
            UUID studioId,
            NotificationType type,
            String title,
            String message,
            String actionUrl,
            String metadata
    ) {
        if (targetUserId == null) {
            log.warn("Cannot dispatch notification without a target user ID");
            return;
        }

        try {
            NotificationPreferencesRecord prefs = notificationRepository.getPreferences(targetUserId);

            // Check if in-app is disabled
            if (!prefs.inAppEnabled()) {
                log.debug("In-app notifications disabled for user {}", targetUserId);
                return;
            }

            // Check category preferences
            boolean allowed = switch (type) {
                case NEW_LEAD, LEAD_FOLLOW_UP -> prefs.leadNotifications();
                case NEW_REVIEW, REVIEW_RESPONSE -> prefs.reviewNotifications();
                case AI_GENERATION_COMPLETE, AI_GENERATION_FAILED, CLIENT_APPROVED_CONCEPT, CLIENT_REQUESTED_CHANGES -> prefs.aiNotifications();
                case VERIFICATION_UPDATE, PORTFOLIO_PUBLISHED, PORTFOLIO_ACTION_REQUIRED, PROJECT_ACTION_REQUIRED, SYSTEM_NOTICE -> prefs.systemNotifications();
            };

            if (!allowed) {
                log.debug("Notification type {} disabled in user preferences for user {}", type, targetUserId);
                return;
            }

            NotificationRecord record = new NotificationRecord(
                    UuidV7.randomUuid(),
                    targetUserId,
                    studioId,
                    type,
                    title,
                    message,
                    actionUrl,
                    null,
                    Instant.now(),
                    metadata
            );

            notificationRepository.createNotification(record);
            log.info("Dispatched notification {} to user {}", type, targetUserId);

            realtimeEventPublisher.publish(new com.interior.platform.realtime.domain.RealtimeEvent(
                    com.interior.platform.common.util.UuidV7.randomUuid().toString(),
                    com.interior.platform.realtime.domain.RealtimeEventType.NOTIFICATION_CREATED,
                    Instant.now(),
                    targetUserId,
                    studioId,
                    "NOTIFICATION",
                    record.id().toString(),
                    record.id().toString(),
                    java.util.Map.of(
                            "type", type.name(),
                            "title", title,
                            "actionUrl", actionUrl != null ? actionUrl : "",
                            "unreadCount", notificationRepository.countUnreadByUserId(targetUserId)
                    )
            ));
        } catch (Exception e) {
            log.error("Failed to persist notification for user {}: {}", targetUserId, e.getMessage());
        }
    }

    /**
     * Lists notifications for the authenticated actor.
     */
    public NotificationListResponse getNotifications(ActorContext actor, int limit, int offset) {
        authorizationService.requireAuthenticated(actor);

        int sanitizedLimit = Math.max(1, Math.min(100, limit));
        int sanitizedOffset = Math.max(0, offset);

        List<NotificationRecord> records = notificationRepository.findByUserId(actor.userId(), sanitizedLimit, sanitizedOffset);
        long unreadCount = notificationRepository.countUnreadByUserId(actor.userId());

        List<NotificationDto> dtos = records.stream().map(NotificationDto::from).toList();
        return new NotificationListResponse(dtos, unreadCount, sanitizedLimit, sanitizedOffset);
    }

    /**
     * Gets unread count for badge indicators.
     */
    public long getUnreadCount(ActorContext actor) {
        authorizationService.requireAuthenticated(actor);
        return notificationRepository.countUnreadByUserId(actor.userId());
    }

    /**
     * Marks a single notification as read.
     */
    @Transactional
    public void markAsRead(ActorContext actor, UUID notificationId) {
        authorizationService.requireAuthenticated(actor);

        NotificationRecord notif = notificationRepository.findByIdAndUserId(notificationId, actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (notif.readAt() == null) {
            notificationRepository.markAsRead(notificationId, actor.userId(), Instant.now());
            long unread = notificationRepository.countUnreadByUserId(actor.userId());
            realtimeEventPublisher.publish(new com.interior.platform.realtime.domain.RealtimeEvent(
                    com.interior.platform.common.util.UuidV7.randomUuid().toString(),
                    com.interior.platform.realtime.domain.RealtimeEventType.NOTIFICATION_READ,
                    Instant.now(),
                    actor.userId(),
                    notif.studioId(),
                    "NOTIFICATION",
                    notificationId.toString(),
                    notificationId.toString(),
                    java.util.Map.of("unreadCount", unread)
            ));
        }
    }

    /**
     * Marks all notifications as read for current user.
     */
    @Transactional
    public void markAllAsRead(ActorContext actor) {
        authorizationService.requireAuthenticated(actor);
        notificationRepository.markAllAsRead(actor.userId(), Instant.now());
        realtimeEventPublisher.publish(new com.interior.platform.realtime.domain.RealtimeEvent(
                com.interior.platform.common.util.UuidV7.randomUuid().toString(),
                com.interior.platform.realtime.domain.RealtimeEventType.NOTIFICATIONS_READ_ALL,
                Instant.now(),
                actor.userId(),
                null,
                "NOTIFICATION",
                null,
                null,
                java.util.Map.of("unreadCount", 0L)
        ));
    }

    /**
     * Retrieves current notification preferences.
     */
    public NotificationPreferencesDto getPreferences(ActorContext actor) {
        authorizationService.requireAuthenticated(actor);
        NotificationPreferencesRecord prefs = notificationRepository.getPreferences(actor.userId());
        return NotificationPreferencesDto.from(prefs);
    }

    /**
     * Updates notification preferences for current user.
     */
    @Transactional
    public NotificationPreferencesDto updatePreferences(ActorContext actor, UpdateNotificationPreferencesRequest req) {
        authorizationService.requireAuthenticated(actor);

        NotificationPreferencesRecord existing = notificationRepository.getPreferences(actor.userId());

        NotificationPreferencesRecord updated = new NotificationPreferencesRecord(
                actor.userId(),
                req.inAppEnabled() != null ? req.inAppEnabled() : existing.inAppEnabled(),
                req.emailEnabled() != null ? req.emailEnabled() : existing.emailEnabled(),
                req.whatsappEnabled() != null ? req.whatsappEnabled() : existing.whatsappEnabled(),
                req.leadNotifications() != null ? req.leadNotifications() : existing.leadNotifications(),
                req.reviewNotifications() != null ? req.reviewNotifications() : existing.reviewNotifications(),
                req.aiNotifications() != null ? req.aiNotifications() : existing.aiNotifications(),
                req.systemNotifications() != null ? req.systemNotifications() : existing.systemNotifications(),
                Instant.now()
        );

        notificationRepository.savePreferences(updated);
        return NotificationPreferencesDto.from(updated);
    }
}
