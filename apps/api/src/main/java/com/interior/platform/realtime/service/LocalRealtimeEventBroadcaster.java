package com.interior.platform.realtime.service;

import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.realtime.domain.RealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LocalRealtimeEventBroadcaster implements RealtimeEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LocalRealtimeEventBroadcaster.class);

    private final SseConnectionRegistry connectionRegistry;
    private final LeadRepository leadRepository;

    public LocalRealtimeEventBroadcaster(
            SseConnectionRegistry connectionRegistry,
            LeadRepository leadRepository
    ) {
        this.connectionRegistry = connectionRegistry;
        this.leadRepository = leadRepository;
    }

    @Override
    public void broadcast(RealtimeEvent event) {
        if (event == null) {
            return;
        }

        // 1. If explicitly directed to a user, deliver to that user
        if (event.recipientUserId() != null) {
            connectionRegistry.sendToUser(event.recipientUserId(), event);
            log.debug("Delivered realtime event {} to user {}", event.type(), event.recipientUserId());
            return;
        }

        // 2. If studio-scoped and recipient not specified, resolve studio owner and deliver
        if (event.studioId() != null) {
            UUID studioId = event.studioId();
            leadRepository.findPublicStudioById(studioId).ifPresent(target -> {
                if (target.ownerId() != null) {
                    connectionRegistry.sendToUser(target.ownerId(), event);
                    log.debug("Delivered studio realtime event {} to owner {} of studio {}",
                            event.type(), target.ownerId(), studioId);
                }
            });
        }
    }
}
