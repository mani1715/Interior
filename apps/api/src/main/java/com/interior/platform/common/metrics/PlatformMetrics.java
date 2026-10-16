package com.interior.platform.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized, bounded platform metrics service.
 * Enforces strict cardinality limits: tags are strictly low-cardinality enums or normalized categories.
 * NEVER records user IDs, studio IDs, project IDs, emails, raw URLs, or freeform error messages.
 */
@Component
public class PlatformMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger maintenanceModeGauge = new AtomicInteger(0);

    @Autowired
    public PlatformMetrics(MeterRegistry registry) {
        this.registry = registry;
        registry.gauge("interior.operations.maintenance_mode", maintenanceModeGauge);
    }

    public void recordMediaUpload(String operation, String status) {
        Counter.builder("interior.media.uploads")
                .description("Media upload lifecycle intents and commits")
                .tags("operation", normalize(operation, "upload"), "status", normalize(status, "unknown"))
                .register(registry)
                .increment();
    }

    public void recordMediaScan(String scanner, String outcome) {
        Counter.builder("interior.media.scans")
                .description("Upload malware scanning outcomes")
                .tags("scanner", normalize(scanner, "none"), "outcome", normalize(outcome, "unknown"))
                .register(registry)
                .increment();
    }

    public void recordAuthEvent(String eventType) {
        Counter.builder("interior.auth.events")
                .description("Authentication and identity security events")
                .tags("type", normalize(eventType, "unknown"))
                .register(registry)
                .increment();
    }

    public void recordAiGeneration(String provider, String operation, String status) {
        Counter.builder("interior.ai.generations")
                .description("AI visualizer and interior generation events")
                .tags("provider", normalize(provider, "disabled"),
                      "operation", normalize(operation, "generate"),
                      "status", normalize(status, "unknown"))
                .register(registry)
                .increment();
    }

    public void recordEmailDelivery(String provider, String status) {
        Counter.builder("interior.email.deliveries")
                .description("Transactional operational email delivery outcomes")
                .tags("provider", normalize(provider, "disabled"), "status", normalize(status, "unknown"))
                .register(registry)
                .increment();
    }

    public void recordBillingEvent(String provider, String eventType, String outcome) {
        Counter.builder("interior.billing.events")
                .description("Commercial billing and payment provider events")
                .tags("provider", normalize(provider, "disabled"),
                      "event_type", normalize(eventType, "unknown"),
                      "outcome", normalize(outcome, "unknown"))
                .register(registry)
                .increment();
    }

    public void recordJobExecution(String jobKey, String status) {
        Counter.builder("interior.jobs.executions")
                .description("Operational background and scheduled job executions")
                .tags("job_key", normalize(jobKey, "unknown"), "status", normalize(status, "unknown"))
                .register(registry)
                .increment();
    }

    public void recordError(String errorCode) {
        Counter.builder("interior.errors.normalized")
                .description("Normalized server error categories")
                .tags("code", normalize(errorCode, "INTERNAL_ERROR"))
                .register(registry)
                .increment();
    }

    public void setMaintenanceMode(boolean isReadOnly) {
        maintenanceModeGauge.set(isReadOnly ? 1 : 0);
    }

    public int getMaintenanceModeState() {
        return maintenanceModeGauge.get();
    }

    private static String normalize(String val, String defaultVal) {
        if (val == null || val.isBlank()) {
            return defaultVal;
        }
        return val.toLowerCase().trim().replaceAll("[^a-z0-9_-]", "_");
    }
}
