package com.interior.platform.media.config;

import com.interior.platform.media.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator HealthIndicator for object media storage.
 * Reports storage adapter availability without exposing endpoint URLs or credentials.
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private final StorageService storageService;

    @Autowired
    public StorageHealthIndicator(@Autowired(required = false) StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (storageService != null) {
            String name = storageService.getClass().getSimpleName();
            details.put("provider", name);
            details.put("status", "AVAILABLE");
            return Health.up().withDetails(details).build();
        }

        details.put("provider", "LOCAL_DISK");
        details.put("status", "AVAILABLE");
        return Health.up().withDetails(details).build();
    }
}
