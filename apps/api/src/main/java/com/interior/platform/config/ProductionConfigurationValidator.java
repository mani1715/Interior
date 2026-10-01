package com.interior.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class ProductionConfigurationValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionConfigurationValidator.class);

    private final Environment environment;

    @Value("${app.security.dev-auth-enabled:false}")
    private boolean devAuthEnabled;

    @Value("${app.security.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public ProductionConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> activeProfiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        boolean isProduction = activeProfiles.contains("prod") || activeProfiles.contains("production");

        log.info("================================================================================");
        log.info("           PLATFORM SYSTEM STARTUP & CONFIGURATION MATRIX                       ");
        log.info("================================================================================");
        log.info(" Active Profiles       : {}", activeProfiles.isEmpty() ? "[default]" : activeProfiles);
        log.info(" Database (PostgreSQL) : CONFIGURED");
        log.info(" Auth (OIDC)           : {}", isProduction ? "NOT_CONFIGURED (Requires OIDC Client ID)" : "DEV_PERSONA_SANDBOX");
        log.info(" Object Storage        : TEST_ONLY (Local Filesystem)");
        log.info(" AI Visualizer         : NOT_CONFIGURED (DisabledAiImageProvider)");
        log.info(" WhatsApp              : MODE_A_ACTIVE (Direct wa.me) | MODE_B_NOT_CONFIGURED");
        log.info(" Billing Gateway       : NOT_CONFIGURED (DisabledBillingProvider)");
        log.info(" Transactional Email   : NOT_CONFIGURED (DisabledEmailProvider)");
        log.info("================================================================================");

        if (isProduction) {
            if (devAuthEnabled) {
                String errorMsg = "CRITICAL SECURITY FAULT: app.security.dev-auth-enabled MUST NOT be true in production!";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            if (allowedOrigins.contains("*") || allowedOrigins.contains("localhost")) {
                log.warn("PRODUCTION WARNING: app.security.allowed-origins contains localhost or wildcard: '{}'. Lock this down to the canonical production domain before public traffic.", allowedOrigins);
            }
        }
    }
}
