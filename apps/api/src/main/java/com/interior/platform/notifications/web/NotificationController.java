package com.interior.platform.notifications.web;

import com.interior.platform.notifications.dto.NotificationListResponse;
import com.interior.platform.notifications.dto.NotificationPreferencesDto;
import com.interior.platform.notifications.dto.UpdateNotificationPreferencesRequest;
import com.interior.platform.notifications.service.NotificationService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/notifications", "/api/v1/notifications"})
@Tag(name = "Notification Center", description = "Persistent in-app notifications and channel preference controls")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private ActorContext getActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    @GetMapping
    @Operation(summary = "List user notifications", description = "Retrieves in-app notifications with pagination and unread counts")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(getActor(request), limit, offset));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count", description = "Returns the unread count for badge indicators")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest request) {
        long count = notificationService.getUnreadCount(getActor(request));
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Updates a notification's read status")
    public ResponseEntity<Map<String, Boolean>> markAsRead(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        notificationService.markAsRead(getActor(request), id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all user notifications as read")
    public ResponseEntity<Map<String, Boolean>> markAllAsRead(HttpServletRequest request) {
        notificationService.markAllAsRead(getActor(request));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences", description = "Returns user delivery and category preferences")
    public ResponseEntity<NotificationPreferencesDto> getPreferences(HttpServletRequest request) {
        return ResponseEntity.ok(notificationService.getPreferences(getActor(request)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences", description = "Updates in-app/email/whatsapp delivery preferences")
    public ResponseEntity<NotificationPreferencesDto> updatePreferences(
            @RequestBody UpdateNotificationPreferencesRequest req,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(notificationService.updatePreferences(getActor(request), req));
    }
}
