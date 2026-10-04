package com.interior.platform.realtime.service;

import com.interior.platform.realtime.domain.RealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeEventListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventListener.class);

    private final RealtimeEventBroadcaster broadcaster;

    public RealtimeEventListener(RealtimeEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Listens for published domain realtime events and broadcasts them
     * strictly AFTER the active database transaction commits.
     * Fallback execution ensures non-transactional triggers (e.g. background threads)
     * are still delivered without losing signals.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRealtimeEvent(RealtimeEvent event) {
        try {
            log.debug("Transactional event listener triggered after commit for event {}", event.type());
            broadcaster.broadcast(event);
        } catch (Exception e) {
            log.error("Failed to broadcast realtime event after transaction commit: {}", e.getMessage(), e);
        }
    }
}
