package com.interior.platform.email.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.email.provider.DisabledEmailProvider;
import com.interior.platform.email.provider.EmailProvider;
import com.interior.platform.email.provider.FakeEmailProvider;
import com.interior.platform.email.provider.ResendEmailProvider;
import com.interior.platform.email.provider.SesEmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Deterministic Spring configuration for transactional email provider selection.
 * Defaults to DisabledEmailProvider to ensure zero external calls occur until Phase 15.
 */
@Configuration
public class EmailProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailProviderConfig.class);

    @Bean
    @Primary
    public EmailProvider emailProvider(EmailProperties emailProperties, ObjectMapper objectMapper) {
        String provider = emailProperties.getProvider();
        log.info("Initializing Transactional Email Provider: mode='{}'", provider);

        return switch (provider) {
            case "disabled", "none", "" -> new DisabledEmailProvider();
            case "fake", "mock", "test" -> new FakeEmailProvider();
            case "resend" -> new ResendEmailProvider(emailProperties, objectMapper);
            case "ses" -> new SesEmailProvider(emailProperties);
            default -> throw new IllegalArgumentException(
                    "Unknown transactional email provider: '" + provider + "'. Valid options are: disabled, fake, resend, ses."
            );
        };
    }
}
