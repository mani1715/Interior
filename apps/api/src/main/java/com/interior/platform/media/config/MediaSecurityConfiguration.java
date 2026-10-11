package com.interior.platform.media.config;

import com.interior.platform.media.security.scanner.DisabledMalwareScanner;
import com.interior.platform.media.security.scanner.FakeMalwareScanner;
import com.interior.platform.media.security.scanner.MalwareScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaSecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MediaSecurityConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(MalwareScanner.class)
    public MalwareScanner malwareScanner(MediaSecurityProperties properties) {
        String provider = properties.getScanner() != null ? properties.getScanner().trim().toLowerCase() : "fake";
        log.info("Initializing MalwareScanner with provider: '{}', requireCleanScan={}", provider, properties.isRequireCleanScan());

        return switch (provider) {
            case "disabled" -> new DisabledMalwareScanner();
            case "fake" -> new FakeMalwareScanner();
            default -> {
                log.warn("Unknown malware scanner provider '{}', falling back to DisabledMalwareScanner", provider);
                yield new DisabledMalwareScanner();
            }
        };
    }
}
