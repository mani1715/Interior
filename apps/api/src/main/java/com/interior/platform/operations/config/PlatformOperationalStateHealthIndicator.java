package com.interior.platform.operations.config;

import com.interior.platform.operations.domain.PlatformOperationalStateRecord;
import com.interior.platform.operations.service.PlatformOperationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator HealthIndicator for platform operations incident controls and maintenance state.
 * Reports NORMAL vs READ_ONLY and granular kill-switch positions without leaking admin secrets.
 */
@Component
public class PlatformOperationalStateHealthIndicator implements HealthIndicator {

    private final PlatformOperationsService operationsService;

    @Autowired
    public PlatformOperationalStateHealthIndicator(@Autowired(required = false) PlatformOperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (operationsService != null) {
            PlatformOperationalStateRecord state = operationsService.getOperationalState();
            details.put("maintenanceMode", state.maintenanceMode());
            details.put("uploadsEnabled", state.uploadsEnabled());
            details.put("aiEnabled", state.aiEnabled());
            details.put("registrationsEnabled", state.registrationsEnabled());
            return Health.up().withDetails(details).build();
        }

        details.put("maintenanceMode", "NORMAL");
        details.put("uploadsEnabled", true);
        details.put("aiEnabled", true);
        details.put("registrationsEnabled", true);
        return Health.up().withDetails(details).build();
    }
}
