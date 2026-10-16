package com.interior.platform.billing.config;

import com.interior.platform.billing.provider.BillingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator HealthIndicator for commercial billing and payment providers.
 * Accurately surfaces payment gateway readiness without secret exposure and never
 * interrupts platform core operations when disabled.
 */
@Component
public class BillingHealthIndicator implements HealthIndicator {

    private final BillingProvider billingProvider;

    @Autowired
    public BillingHealthIndicator(@Autowired(required = false) BillingProvider billingProvider) {
        this.billingProvider = billingProvider;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (billingProvider != null) {
            String providerId = billingProvider.getProviderName();
            boolean configured = billingProvider.isConfigured();
            details.put("provider", providerId);
            details.put("configured", configured);
            details.put("status", configured ? "CONFIGURED" : "NOT_CONFIGURED");
            return Health.up().withDetails(details).build();
        }

        details.put("provider", "disabled");
        details.put("configured", false);
        details.put("status", "NOT_CONFIGURED");
        return Health.up().withDetails(details).build();
    }
}
