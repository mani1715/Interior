package com.interior.platform.realtime.service;

import com.interior.platform.realtime.domain.RealtimeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class RealtimeEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public RealtimeEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(RealtimeEvent event) {
        if (event != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}
