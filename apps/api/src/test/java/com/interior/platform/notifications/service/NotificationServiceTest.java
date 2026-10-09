package com.interior.platform.notifications.service;

import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.notifications.domain.NotificationPreferencesRecord;
import com.interior.platform.notifications.domain.NotificationRecord;
import com.interior.platform.notifications.domain.NotificationType;
import com.interior.platform.notifications.dto.NotificationListResponse;
import com.interior.platform.notifications.repository.NotificationRepository;
import com.interior.platform.realtime.service.RealtimeEventPublisher;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    private AuthorizationService authorizationService;
    private NotificationService notificationService;

    private UUID userId;
    private UUID studioId;
    private ActorContext actor;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        notificationService = new NotificationService(notificationRepository, authorizationService, realtimeEventPublisher);

        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        actor = new ActorContext(userId, "Studio Admin", "admin@studio.com", Set.of("DESIGNER"), studioId, "DESIGNER_ADMIN", true);
    }

    @Test
    @DisplayName("Notification Dispatch: Creates persistent record and emits SSE event when preferences allow")
    void testDispatchNotificationSuccess() {
        NotificationPreferencesRecord defaultPrefs = NotificationPreferencesRecord.defaultForUser(userId);
        when(notificationRepository.getPreferences(userId)).thenReturn(defaultPrefs);
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(1L);

        notificationService.dispatchNotification(
                userId,
                studioId,
                NotificationType.NEW_LEAD,
                "New Lead",
                "New inquiry received",
                "/workspace/leads/123",
                "{\"leadId\":\"123\"}"
        );

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRepository).createNotification(captor.capture());

        NotificationRecord saved = captor.getValue();
        assertEquals(userId, saved.userId());
        assertEquals(studioId, saved.studioId());
        assertEquals(NotificationType.NEW_LEAD, saved.type());
        assertEquals("New Lead", saved.title());
        assertNull(saved.readAt());

        verify(realtimeEventPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("Notification Dispatch: Honors user preference suppressing disabled category")
    void testDispatchSuppressedByPreference() {
        NotificationPreferencesRecord customPrefs = new NotificationPreferencesRecord(
                userId, true, false, false, false, true, true, true, Instant.now()
        );
        when(notificationRepository.getPreferences(userId)).thenReturn(customPrefs);

        // lead notifications disabled
        notificationService.dispatchNotification(
                userId,
                studioId,
                NotificationType.NEW_LEAD,
                "New Lead",
                "Suppressed inquiry",
                "/workspace/leads/123",
                null
        );

        verify(notificationRepository, never()).createNotification(any());
        verify(realtimeEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Notification Dispatch: Supports new Phase 7C event types (LEAD_ASSIGNED, CLIENT_FEEDBACK_RECEIVED)")
    void testDispatchNewPhase7cTypes() {
        NotificationPreferencesRecord defaultPrefs = NotificationPreferencesRecord.defaultForUser(userId);
        when(notificationRepository.getPreferences(userId)).thenReturn(defaultPrefs);
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(1L);

        notificationService.dispatchNotification(
                userId,
                studioId,
                NotificationType.LEAD_ASSIGNED,
                "New Lead Assigned",
                "Lead assigned to you",
                "/workspace/leads/456",
                null
        );

        notificationService.dispatchNotification(
                userId,
                studioId,
                NotificationType.CLIENT_FEEDBACK_RECEIVED,
                "Client Feedback",
                "New client comment",
                "/workspace/ai",
                null
        );

        verify(notificationRepository, times(2)).createNotification(any());
    }

    @Test
    @DisplayName("Read Operations: Mark as read updates repository and emits SSE update")
    void testMarkAsRead() {
        UUID notifId = UuidV7.randomUuid();
        NotificationRecord unreadNotif = new NotificationRecord(
                notifId, userId, studioId, NotificationType.NEW_LEAD,
                "Title", "Msg", "/url", null, Instant.now(), null
        );

        when(notificationRepository.findByIdAndUserId(notifId, userId))
                .thenReturn(Optional.of(unreadNotif));
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(0L);

        notificationService.markAsRead(actor, notifId);

        verify(notificationRepository).markAsRead(eq(notifId), eq(userId), any());
        verify(realtimeEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Tenant/User Isolation: User cannot mark another user's notification as read")
    void testMarkAsReadForeignUserDenied() {
        UUID foreignNotifId = UuidV7.randomUuid();
        when(notificationRepository.findByIdAndUserId(foreignNotifId, userId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                notificationService.markAsRead(actor, foreignNotifId)
        );

        verify(notificationRepository, never()).markAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("Listing: Retrieves bounded paginated notifications and accurate unread count")
    void testGetNotificationsPagination() {
        when(notificationRepository.findByUserId(userId, 10, 0))
                .thenReturn(List.of(
                        new NotificationRecord(UuidV7.randomUuid(), userId, studioId, NotificationType.NEW_LEAD, "T1", "M1", null, null, Instant.now(), null)
                ));
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(1L);

        NotificationListResponse res = notificationService.getNotifications(actor, 10, 0);

        assertNotNull(res);
        assertEquals(1, res.notifications().size());
        assertEquals(1L, res.unreadCount());
        assertEquals(10, res.limit());
        assertEquals(0, res.offset());
    }
}
