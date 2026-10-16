package com.interior.platform.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.billing.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Deterministic Spring configuration for commercial billing provider selection.
 * Defaults to DisabledBillingProvider to guarantee zero external commercial requests until Phase 15.
 */
@Configuration
public class BillingProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingProviderConfig.class);

    @Bean
    @Primary
    public BillingProvider billingProvider(BillingProperties billingProperties, ObjectMapper objectMapper) {
        String provider = billingProperties.getProvider();
        log.info("Initializing Billing Provider: mode='{}'", provider);

        return switch (provider) {
            case "disabled", "none", "" -> new DisabledBillingProvider();
            case "fake", "mock", "test" -> new FakeBillingProvider(objectMapper);
            case "razorpay" -> new RazorpayBillingProvider(billingProperties, objectMapper);
            case "stripe" -> new StripeBillingProvider(billingProperties, objectMapper);
            default -> throw new IllegalArgumentException(
                    "Unknown commercial billing provider: '" + provider + "'. Valid options are: disabled, fake, razorpay, stripe."
            );
        };
    }
}
