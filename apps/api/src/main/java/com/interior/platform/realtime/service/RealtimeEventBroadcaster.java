package com.interior.platform.realtime.service;

import com.interior.platform.realtime.domain.RealtimeEvent;

/**
 * Interface separating connection delivery from domain event publication.
 * Enables future horizontal multi-instance fan-out (e.g. via Redis pub/sub or PostgreSQL LISTEN/NOTIFY)
 * without modifying domain services.
 */
public interface RealtimeEventBroadcaster {

    void broadcast(RealtimeEvent event);
}
