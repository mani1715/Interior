package com.interior.platform.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.billing.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Locale;

/**
 * Deterministic Spring configuration for commercial billing provider selection.
 * Defaults to DisabledBillingProvider to guarantee zero external commercial requests until Phase 15.
 */
@Configuration
public class BillingProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingProviderConfig.class);

    private final Environment environment;

    public BillingProviderConfig(@Autowired(required = false) Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public BillingProvider billingProvider(BillingProperties billingProperties, ObjectMapper objectMapper) {
        String rawProvider = billingProperties.getProvider();
        String provider = rawProvider != null ? rawProvider.trim().toLowerCase(Locale.ROOT) : "disabled";
        log.info("Initializing Billing Provider: mode='{}'", provider);

        boolean isProduction = environment != null && Arrays.asList(environment.getActiveProfiles()).contains("production");

        if (isProduction && ("fake".equals(provider) || "mock".equals(provider) || "test".equals(provider))) {
            String errorMsg = "CRITICAL SECURITY FAULT: Mock billing provider ('" + rawProvider + "') is strictly forbidden in production profile!";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        return switch (provider) {
            case "disabled", "none", "" -> new DisabledBillingProvider();
            case "fake", "mock", "test" -> new FakeBillingProvider(objectMapper);
            case "razorpay" -> new RazorpayBillingProvider(billingProperties, objectMapper);
            case "stripe" -> new StripeBillingProvider(billingProperties, objectMapper);
            default -> throw new IllegalArgumentException(
                    "Unknown commercial billing provider: '" + rawProvider + "'. Valid options are: disabled, fake, razorpay, stripe."
            );
        };
    }
}
