package com.interior.platform.email.config;

import com.interior.platform.email.service.TransactionalEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator HealthIndicator for transactional email delivery.
 * Accurately reports configured status without failing platform liveness or readiness
 * when email is disabled by design.
 */
@Component
public class TransactionalEmailHealthIndicator implements HealthIndicator {

    private final TransactionalEmailService emailService;

    @Autowired
    public TransactionalEmailHealthIndicator(@Autowired(required = false) TransactionalEmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (emailService != null) {
            boolean configured = emailService.isConfigured();
            details.put("provider", emailService.getProviderName());
            details.put("configured", configured);
            details.put("status", configured ? "CONFIGURED" : "NOT_CONFIGURED");
            return Health.up().withDetails(details).build();
        }

        details.put("provider", "DISABLED");
        details.put("configured", false);
        details.put("status", "NOT_CONFIGURED");
        return Health.up().withDetails(details).build();
    }
}
