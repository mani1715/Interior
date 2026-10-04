package com.interior.platform.realtime.service;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.realtime.domain.RealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SseConnectionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionRegistry.class);

    public static final long EMITTER_TIMEOUT_MS = 180_000L; // 3 minutes timeout
    public static final int MAX_CONNECTIONS_PER_USER = 10;
    public static final int MAX_GLOBAL_CONNECTIONS = 5000;

    private final Map<UUID, List<RegisteredEmitter>> userConnections = new ConcurrentHashMap<>();
    private final AtomicInteger globalConnectionCount = new AtomicInteger(0);

    public record RegisteredEmitter(
            String connectionId,
            UUID userId,
            SseEmitter emitter,
            Instant createdAt
    ) {}

    /**
     * Registers a new SSE connection for the authenticated user.
     * Enforces per-user and global connection limits.
     */
    public SseEmitter register(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null for SSE registration");
        }

        if (globalConnectionCount.get() >= MAX_GLOBAL_CONNECTIONS) {
            log.warn("Global SSE connection limit reached ({})", MAX_GLOBAL_CONNECTIONS);
            throw new BadRequestException("Service connection limit reached. Please retry later.");
        }

        List<RegisteredEmitter> userList = userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // Clean up any stale or completed connections before enforcing limit
        cleanStaleUserConnections(userId, userList);

        if (userList.size() >= MAX_CONNECTIONS_PER_USER) {
            log.warn("User {} reached maximum concurrent SSE connections ({})", userId, MAX_CONNECTIONS_PER_USER);
            // Evict oldest connection gracefully to accommodate active tab
            if (!userList.isEmpty()) {
                RegisteredEmitter oldest = userList.remove(0);
                try {
                    oldest.emitter().complete();
                } catch (Exception ignored) {}
                globalConnectionCount.decrementAndGet();
            }
        }

        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        RegisteredEmitter registered = new RegisteredEmitter(connectionId, userId, emitter, Instant.now());

        emitter.onCompletion(() -> removeConnection(userId, connectionId, "completed"));
        emitter.onTimeout(() -> removeConnection(userId, connectionId, "timeout"));
        emitter.onError(e -> removeConnection(userId, connectionId, "error: " + e.getMessage()));

        userList.add(registered);
        globalConnectionCount.incrementAndGet();

        log.debug("SSE connection established for user {} (connectionId={}, activeUserTabs={})",
                userId, connectionId, userList.size());

        return emitter;
    }

    /**
     * Dispatches an event to all active connections belonging to the specified user.
     */
    public void sendToUser(UUID userId, RealtimeEvent event) {
        if (userId == null || event == null) {
            return;
        }

        List<RegisteredEmitter> connections = userConnections.get(userId);
        if (connections == null || connections.isEmpty()) {
            return;
        }

        List<RegisteredEmitter> deadEmitters = new ArrayList<>();

        for (RegisteredEmitter reg : connections) {
            try {
                SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
                        .id(event.eventId())
                        .name(event.type().name())
                        .data(event, MediaType.APPLICATION_JSON);

                reg.emitter().send(sseEvent);
            } catch (Exception ex) {
                log.debug("Failed to send SSE event {} to user {} on connection {}: {}",
                        event.type(), userId, reg.connectionId(), ex.getMessage());
                deadEmitters.add(reg);
            }
        }

        for (RegisteredEmitter dead : deadEmitters) {
            removeConnection(userId, dead.connectionId(), "send failure");
        }
    }

    /**
     * Sends heartbeat comments to all active connections every 20 seconds.
     * Keeps connections alive and prunes dead ones.
     */
    @Scheduled(fixedRate = 20_000)
    public void sendHeartbeats() {
        if (globalConnectionCount.get() == 0) {
            return;
        }

        int pruned = 0;
        for (Map.Entry<UUID, List<RegisteredEmitter>> entry : userConnections.entrySet()) {
            UUID userId = entry.getKey();
            List<RegisteredEmitter> emitters = entry.getValue();
            List<RegisteredEmitter> dead = new ArrayList<>();

            for (RegisteredEmitter reg : emitters) {
                try {
                    reg.emitter().send(SseEmitter.event().comment("keep-alive"));
                } catch (Exception ex) {
                    dead.add(reg);
                }
            }

            for (RegisteredEmitter d : dead) {
                removeConnection(userId, d.connectionId(), "heartbeat failure");
                pruned++;
            }
        }

        if (pruned > 0) {
            log.debug("Pruned {} inactive SSE emitters during heartbeat cycle", pruned);
        }
    }

    /**
     * Removes a single connection from registry.
     */
    public void removeConnection(UUID userId, String connectionId, String reason) {
        List<RegisteredEmitter> list = userConnections.get(userId);
        if (list != null) {
            boolean removed = list.removeIf(r -> r.connectionId().equals(connectionId));
            if (removed) {
                globalConnectionCount.decrementAndGet();
                log.debug("Closed SSE connection {} for user {} (reason: {})", connectionId, userId, reason);
            }
            if (list.isEmpty()) {
                userConnections.remove(userId);
            }
        }
    }

    private void cleanStaleUserConnections(UUID userId, List<RegisteredEmitter> list) {
        list.removeIf(r -> {
            try {
                // Ping test
                r.emitter().send(SseEmitter.event().comment("ping"));
                return false;
            } catch (Exception e) {
                globalConnectionCount.decrementAndGet();
                return true;
            }
        });
    }

    public int getActiveConnectionCount() {
        return globalConnectionCount.get();
    }

    public int getActiveUserCount() {
        return userConnections.size();
    }

    public int getUserConnectionCount(UUID userId) {
        List<RegisteredEmitter> list = userConnections.get(userId);
        return list != null ? list.size() : 0;
    }
}
