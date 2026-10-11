package com.interior.platform.support;

import com.interior.platform.common.exception.BadRequestException;
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
import com.interior.platform.support.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9G — Support Service Unit Tests")
class SupportServiceTest {

    @Mock private SupportRepository supportRepository;
    @Mock private AuditService auditService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private NotificationService notificationService;

    private AuthorizationService authorizationService;
    private SupportService supportService;

    private UUID userId;
    private ActorContext userActor;
    private ActorContext adminActor;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        supportService = new SupportService(
                supportRepository,
                authorizationService,
                auditService,
                jdbcTemplate,
                notificationService
        );

        userId = UuidV7.randomUuid();
        userActor = new ActorContext(userId, "John Doe", "john@example.com", "ACTIVE", Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true, null);
        adminActor = new ActorContext(UuidV7.randomUuid(), "Admin Jane", "admin@elegance.com", "ACTIVE", Set.of("ADMIN"), Set.of(), null, null, "MFA", true, Instant.now());
    }

    @Test
    @DisplayName("Create support request creates ticket and initial message")
    void createRequest_success() {
        when(supportRepository.countUserRequestsLastHour(userId)).thenReturn(0);
        when(supportRepository.findRequestById(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.of(new SupportRequestRecord(id, userId, null, SupportCategory.BILLING, "Billing issue", SupportStatus.OPEN, SupportPriority.NORMAL, Instant.now(), Instant.now(), null));
        });
        when(supportRepository.findMessagesByRequestId(any(), anyBoolean())).thenReturn(List.of());

        CreateSupportRequest req = new CreateSupportRequest(SupportCategory.BILLING, "Billing issue", "Need help with invoice", null);
        SupportRequestDetailResponse response = supportService.createRequest(userActor, req, "127.0.0.1", "TestAgent");

        assertNotNull(response);
        assertEquals(SupportCategory.BILLING, response.category());
        verify(supportRepository).createRequest(any(SupportRequestRecord.class));
        verify(supportRepository).createMessage(any(SupportMessageRecord.class));
        verify(auditService).record(eq(userId), isNull(), eq("SUPPORT_REQUEST_CREATED"), eq("SUPPORT_REQUEST"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Create support request rate limit enforcement (max 5/hr)")
    void createRequest_rateLimitExceeded() {
        when(supportRepository.countUserRequestsLastHour(userId)).thenReturn(5);

        CreateSupportRequest req = new CreateSupportRequest(SupportCategory.TECHNICAL, "Bug", "Page crashed", null);
        assertThrows(BadRequestException.class, () ->
                supportService.createRequest(userActor, req, "127.0.0.1", "TestAgent")
        );
        verify(supportRepository, never()).createRequest(any());
    }

    @Test
    @DisplayName("User reply sets status to IN_PROGRESS when waiting on user")
    void addMessage_userReply_transitionsStatus() {
        UUID reqId = UuidV7.randomUuid();
        SupportRequestRecord ticket = new SupportRequestRecord(reqId, userId, null, SupportCategory.ACCOUNT, "Account Help", SupportStatus.WAITING_USER, SupportPriority.NORMAL, Instant.now(), Instant.now(), null);
        when(supportRepository.findRequestById(reqId)).thenReturn(Optional.of(ticket));
        when(supportRepository.countRecentUserMessages(eq(userId), any())).thenReturn(0);

        CreateSupportMessageRequest req = new CreateSupportMessageRequest("Here is the additional detail", false);
        SupportMessageResponse msg = supportService.addMessage(userActor, reqId, req, "127.0.0.1", "TestAgent");

        assertNotNull(msg);
        assertFalse(msg.isInternal());
        verify(supportRepository).updateRequestStatus(eq(reqId), eq(SupportStatus.IN_PROGRESS), isNull(), isNull());
    }

    @Test
    @DisplayName("Admin public reply dispatches in-app notification to ticket owner")
    void addMessage_adminPublicReply_notifiesOwner() {
        UUID reqId = UuidV7.randomUuid();
        SupportRequestRecord ticket = new SupportRequestRecord(reqId, userId, null, SupportCategory.TECHNICAL, "System bug", SupportStatus.OPEN, SupportPriority.NORMAL, Instant.now(), Instant.now(), null);
        when(supportRepository.findRequestById(reqId)).thenReturn(Optional.of(ticket));
        when(supportRepository.countRecentUserMessages(eq(adminActor.userId()), any())).thenReturn(0);

        CreateSupportMessageRequest req = new CreateSupportMessageRequest("We have identified the bug and fixed it.", false);
        supportService.addMessage(adminActor, reqId, req, "127.0.0.1", "TestAgent");

        verify(notificationService).dispatchNotification(
                eq(userId),
                isNull(),
                eq(NotificationType.SYSTEM_NOTICE),
                eq("Support Reply: System bug"),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Admin internal note does NOT dispatch notification and marks is_internal = true")
    void addMessage_adminInternalNote_noNotification() {
        UUID reqId = UuidV7.randomUuid();
        SupportRequestRecord ticket = new SupportRequestRecord(reqId, userId, null, SupportCategory.TECHNICAL, "System bug", SupportStatus.OPEN, SupportPriority.NORMAL, Instant.now(), Instant.now(), null);
        when(supportRepository.findRequestById(reqId)).thenReturn(Optional.of(ticket));
        when(supportRepository.countRecentUserMessages(eq(adminActor.userId()), any())).thenReturn(0);

        CreateSupportMessageRequest req = new CreateSupportMessageRequest("Internal triage note: check log correlation", true);
        SupportMessageResponse resp = supportService.addMessage(adminActor, reqId, req, "127.0.0.1", "TestAgent");

        assertTrue(resp.isInternal());
        verify(notificationService, never()).dispatchNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Unauthorized user cannot view another user's support request")
    void getRequestDetail_unauthorizedUser_throws() {
        UUID reqId = UuidV7.randomUuid();
        UUID otherUserId = UuidV7.randomUuid();
        SupportRequestRecord ticket = new SupportRequestRecord(reqId, otherUserId, null, SupportCategory.BILLING, "Secret Billing", SupportStatus.OPEN, SupportPriority.NORMAL, Instant.now(), Instant.now(), null);
        when(supportRepository.findRequestById(reqId)).thenReturn(Optional.of(ticket));

        assertThrows(UnauthorizedException.class, () ->
                supportService.getRequestDetail(userActor, reqId)
        );
    }
}
