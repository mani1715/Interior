package com.interior.platform.config;

import com.interior.platform.media.config.MediaSecurityProperties;
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
    private final MediaSecurityProperties mediaSecurityProperties;
    private final com.interior.platform.security.config.AuthSecurityProperties authSecurityProperties;

    @Value("${app.security.dev-auth-enabled:false}")
    private boolean devAuthEnabled;

    @Value("${app.security.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${app.security.session-cookie-secure:true}")
    private boolean sessionCookieSecure;

    @Value("${app.storage.provider:LOCAL}")
    private String storageProvider;

    @Value("${app.storage.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${app.storage.s3.access-key-id:}")
    private String s3AccessKeyId;

    @Value("${app.storage.s3.secret-access-key:}")
    private String s3SecretAccessKey;

    public ProductionConfigurationValidator(Environment environment) {
        this(environment, new MediaSecurityProperties(), new com.interior.platform.security.config.AuthSecurityProperties());
    }

    public ProductionConfigurationValidator(Environment environment, MediaSecurityProperties mediaSecurityProperties) {
        this(environment, mediaSecurityProperties, new com.interior.platform.security.config.AuthSecurityProperties());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProductionConfigurationValidator(
            Environment environment,
            MediaSecurityProperties mediaSecurityProperties,
            com.interior.platform.security.config.AuthSecurityProperties authSecurityProperties
    ) {
        this.environment = environment;
        this.mediaSecurityProperties = mediaSecurityProperties != null ? mediaSecurityProperties : new MediaSecurityProperties();
        this.authSecurityProperties = authSecurityProperties != null ? authSecurityProperties : new com.interior.platform.security.config.AuthSecurityProperties();
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> activeProfiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        boolean isProduction = activeProfiles.contains("prod") || activeProfiles.contains("production");

        String storageStatus;
        if ("S3_COMPATIBLE".equalsIgnoreCase(storageProvider)) {
            if (s3Endpoint != null && !s3Endpoint.isBlank()) {
                storageStatus = "CONFIGURED (S3/R2 Cloud Storage)";
            } else {
                storageStatus = "NOT_CONFIGURED (S3/R2 Endpoint Missing)";
            }
        } else {
            storageStatus = isProduction ? "TEST_ONLY (Local Filesystem — Ephemeral)" : "TEST_ONLY (Local Filesystem)";
        }

        String scanner = mediaSecurityProperties.getScanner() != null ? mediaSecurityProperties.getScanner().trim().toLowerCase() : "disabled";
        String scannerStatus;
        if ("clamav".equals(scanner)) {
            scannerStatus = "CONFIGURED (ClamAV TCP Streaming Daemon)";
        } else if ("fake".equals(scanner)) {
            scannerStatus = isProduction ? "FORBIDDEN (FakeMalwareScanner in Production)" : "TEST_ONLY (FakeMalwareScanner)";
        } else {
            scannerStatus = mediaSecurityProperties.isRequireCleanScan() ? "DISABLED (Fail-Closed Default)" : "DISABLED (Bypassed)";
        }

        String oidcStatus;
        int readyProviders = 0;
        if (authSecurityProperties.getOidc().getProviders() != null) {
            for (var entry : authSecurityProperties.getOidc().getProviders().entrySet()) {
                var p = entry.getValue();
                boolean hasId = p.getClientId() != null && !p.getClientId().isBlank();
                boolean hasSecret = p.getClientSecret() != null && !p.getClientSecret().isBlank();
                if (hasId && hasSecret && !isPlaceholder(p.getClientId()) && !isPlaceholder(p.getClientSecret())) {
                    readyProviders++;
                }
            }
        }
        if (readyProviders > 0) {
            oidcStatus = "CONFIGURED (" + readyProviders + " active provider(s))";
        } else if (isProduction) {
            oidcStatus = "NOT_CONFIGURED (Phase 15 Provider Activation Pending)";
        } else {
            oidcStatus = devAuthEnabled ? "DEV_PERSONA_SANDBOX" : "DISABLED";
        }

        log.info("================================================================================");
        log.info("           PLATFORM SYSTEM STARTUP & CONFIGURATION MATRIX                       ");
        log.info("================================================================================");
        log.info(" Active Profiles       : {}", activeProfiles.isEmpty() ? "[default]" : activeProfiles);
        log.info(" Database (PostgreSQL) : CONFIGURED");
        log.info(" Auth (OIDC)           : {}", oidcStatus);
        log.info(" Object Storage        : {}", storageStatus);
        log.info(" Malware Scanner       : {}", scannerStatus);
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

            if (authSecurityProperties.getOidc().getProviders() != null) {
                for (var entry : authSecurityProperties.getOidc().getProviders().entrySet()) {
                    var p = entry.getValue();
                    boolean hasClientId = p.getClientId() != null && !p.getClientId().isBlank();
                    boolean hasSecret = p.getClientSecret() != null && !p.getClientSecret().isBlank();

                    if (hasClientId || hasSecret) {
                        if (!hasClientId || isPlaceholder(p.getClientId())) {
                            String errorMsg = "CRITICAL CONFIGURATION FAULT: OIDC provider '" + entry.getKey() + "' is enabled but clientId is missing or placeholder.";
                            log.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                        if (!hasSecret || isPlaceholder(p.getClientSecret())) {
                            String errorMsg = "CRITICAL CONFIGURATION FAULT: OIDC provider '" + entry.getKey() + "' is enabled but clientSecret is missing or placeholder.";
                            log.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                        if (p.getIssuer() == null || p.getIssuer().isBlank()) {
                            String errorMsg = "CRITICAL CONFIGURATION FAULT: OIDC provider '" + entry.getKey() + "' is enabled but issuer is not configured.";
                            log.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                        if (p.getTokenUri() == null || p.getTokenUri().isBlank()) {
                            String errorMsg = "CRITICAL CONFIGURATION FAULT: OIDC provider '" + entry.getKey() + "' is enabled but tokenUri is not configured.";
                            log.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                        if (p.getRedirectUri() == null || p.getRedirectUri().isBlank()) {
                            String errorMsg = "CRITICAL CONFIGURATION FAULT: OIDC provider '" + entry.getKey() + "' is enabled but redirectUri is not configured.";
                            log.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                    }
                }
            }

            if (!sessionCookieSecure) {
                String errorMsg = "CRITICAL SECURITY FAULT: app.security.session-cookie-secure MUST be true in production!";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            if (allowedOrigins.contains("*") || allowedOrigins.contains("localhost")) {
                log.warn("PRODUCTION WARNING: app.security.allowed-origins contains localhost or wildcard: '{}'. Lock this down to the canonical production domain before public traffic.", allowedOrigins);
            }

            String datasourceUrl = environment.getProperty("spring.datasource.url", "");
            if (datasourceUrl.startsWith("jdbc:h2:")) {
                String errorMsg = "CRITICAL CONFIGURATION FAULT: Production cannot run against in-memory H2 database!";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            if (!datasourceUrl.isBlank() && !datasourceUrl.contains("sslmode=require") && !datasourceUrl.contains("ssl=true") && !datasourceUrl.contains("localhost")) {
                log.warn("PRODUCTION DATABASE SECURITY WARNING: Database connection string does not explicitly specify sslmode=require. Ensure your cloud provider enforces SSL encryption at the network boundary.");
            }

            if ("LOCAL".equalsIgnoreCase(storageProvider)) {
                log.warn("PRODUCTION STORAGE WARNING: Running with LOCAL filesystem storage in production. Container storage is ephemeral. Set app.storage.provider=S3_COMPATIBLE for persistent media.");
            } else if ("S3_COMPATIBLE".equalsIgnoreCase(storageProvider)) {
                if (s3Endpoint == null || s3Endpoint.isBlank()) {
                    String errorMsg = "CRITICAL CONFIGURATION FAULT: S3-compatible media storage is selected (app.storage.provider=S3_COMPATIBLE) but S3_ENDPOINT is not configured.";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                if (s3AccessKeyId == null || s3AccessKeyId.isBlank() || s3SecretAccessKey == null || s3SecretAccessKey.isBlank()) {
                    String errorMsg = "CRITICAL CONFIGURATION FAULT: S3-compatible media storage is selected but S3_ACCESS_KEY_ID or S3_SECRET_ACCESS_KEY is not configured.";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            }

            if ("fake".equals(scanner)) {
                String errorMsg = "CRITICAL SECURITY FAULT: FakeMalwareScanner MUST NOT be used in production! Configure ClamAV or disabled.";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            } else if ("clamav".equals(scanner)) {
                if (mediaSecurityProperties.getClamav().getHost() == null || mediaSecurityProperties.getClamav().getHost().isBlank()) {
                    String errorMsg = "CRITICAL CONFIGURATION FAULT: Malware scanner 'clamav' is selected but app.media.security.clamav.host is not configured.";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            } else if ("disabled".equals(scanner) && mediaSecurityProperties.isRequireCleanScan()) {
                log.warn("PRODUCTION UPLOAD NOTICE: Malware scanner is 'disabled' with require-clean-scan=true. Upload commits will fail-closed until ClamAV is configured.");
            }
        }
    }

    private boolean isPlaceholder(String val) {
        if (val == null) return true;
        String lower = val.toLowerCase().trim();
        return lower.startsWith("your-") || lower.contains("placeholder") || lower.equals("change-me");
    }
}

