package com.interior.platform.realtime.web;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.realtime.domain.RealtimeEvent;
import com.interior.platform.realtime.domain.RealtimeEventType;
import com.interior.platform.realtime.service.SseConnectionRegistry;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/events", "/api/v1/events"})
public class SseStreamController {

    private static final Logger log = LoggerFactory.getLogger(SseStreamController.class);

    private final SseConnectionRegistry connectionRegistry;
    private final SecurityRepository securityRepository;

    public SseStreamController(
            SseConnectionRegistry connectionRegistry,
            SecurityRepository securityRepository
    ) {
        this.connectionRegistry = connectionRegistry;
        this.securityRepository = securityRepository;
    }

    /**
     * Authenticated Server-Sent Events stream endpoint.
     * Enforces session authentication, account status verification, and server-derived identity.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);

        if (actor == null || !actor.isAuthenticated() || actor.userId() == null) {
            log.warn("Rejected anonymous SSE connection request");
            throw new UnauthorizedException("Authentication required to connect to real-time events stream");
        }

        // Verify account is strictly ACTIVE and not deactivated/suspended
        Optional<UserRecord> userOpt = securityRepository.findUserById(actor.userId());
        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("User account not found");
        }

        UserRecord user = userOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(user.status()) || user.deactivatedAt() != null) {
            log.warn("Rejected SSE connection for non-active user {} (status={}, deactivatedAt={})",
                    actor.userId(), user.status(), user.deactivatedAt());
            throw new AccessDeniedException("Account is not active or has been deactivated");
        }

        // Configure SSE headers to prevent proxy buffering
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = connectionRegistry.register(actor.userId());

        // Send initial handshake / resync event
        try {
            boolean isReconnect = (lastEventId != null && !lastEventId.isBlank());
            RealtimeEventType initialType = isReconnect ? RealtimeEventType.RESYNC : RealtimeEventType.SYSTEM_NOTICE;

            RealtimeEvent initialEvent = RealtimeEvent.of(
                    initialType,
                    actor.userId(),
                    "STREAM",
                    "connected",
                    Map.of(
                            "status", "CONNECTED",
                            "reconnected", isReconnect,
                            "timestamp", Instant.now().toString()
                    )
            );

            emitter.send(SseEmitter.event()
                    .id(initialEvent.eventId())
                    .name(initialEvent.type().name())
                    .data(initialEvent, MediaType.APPLICATION_JSON));

            log.debug("Sent initial {} event to user {}", initialType, actor.userId());
        } catch (IOException e) {
            log.debug("Client disconnected immediately upon SSE handshake for user {}: {}", actor.userId(), e.getMessage());
            emitter.complete();
        }

        return emitter;
    }
}
