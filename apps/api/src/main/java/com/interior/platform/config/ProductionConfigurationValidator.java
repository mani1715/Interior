package com.interior.platform.config;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.billing.config.BillingProperties;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.media.config.MediaSecurityProperties;
import com.interior.platform.security.config.AuthSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AuthSecurityProperties authSecurityProperties;
    private final AiVisualizerProperties aiVisualizerProperties;
    private final EmailProperties emailProperties;
    private final BillingProperties billingProperties;

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
        this(environment, new MediaSecurityProperties(), new AuthSecurityProperties(), new AiVisualizerProperties(), new EmailProperties(), new BillingProperties());
    }

    public ProductionConfigurationValidator(Environment environment, MediaSecurityProperties mediaSecurityProperties) {
        this(environment, mediaSecurityProperties, new AuthSecurityProperties(), new AiVisualizerProperties(), new EmailProperties(), new BillingProperties());
    }

    public ProductionConfigurationValidator(
            Environment environment,
            MediaSecurityProperties mediaSecurityProperties,
            AuthSecurityProperties authSecurityProperties
    ) {
        this(environment, mediaSecurityProperties, authSecurityProperties, new AiVisualizerProperties(), new EmailProperties(), new BillingProperties());
    }

    public ProductionConfigurationValidator(
            Environment environment,
            MediaSecurityProperties mediaSecurityProperties,
            AuthSecurityProperties authSecurityProperties,
            AiVisualizerProperties aiVisualizerProperties
    ) {
        this(environment, mediaSecurityProperties, authSecurityProperties, aiVisualizerProperties, new EmailProperties(), new BillingProperties());
    }

    public ProductionConfigurationValidator(
            Environment environment,
            MediaSecurityProperties mediaSecurityProperties,
            AuthSecurityProperties authSecurityProperties,
            AiVisualizerProperties aiVisualizerProperties,
            EmailProperties emailProperties
    ) {
        this(environment, mediaSecurityProperties, authSecurityProperties, aiVisualizerProperties, emailProperties, new BillingProperties());
    }

    @Autowired
    public ProductionConfigurationValidator(
            Environment environment,
            @Autowired(required = false) MediaSecurityProperties mediaSecurityProperties,
            @Autowired(required = false) AuthSecurityProperties authSecurityProperties,
            @Autowired(required = false) AiVisualizerProperties aiVisualizerProperties,
            @Autowired(required = false) EmailProperties emailProperties,
            @Autowired(required = false) BillingProperties billingProperties
    ) {
        this.environment = environment;
        this.mediaSecurityProperties = mediaSecurityProperties != null ? mediaSecurityProperties : new MediaSecurityProperties();
        this.authSecurityProperties = authSecurityProperties != null ? authSecurityProperties : new AuthSecurityProperties();
        this.aiVisualizerProperties = aiVisualizerProperties != null ? aiVisualizerProperties : new AiVisualizerProperties();
        this.emailProperties = emailProperties != null ? emailProperties : new EmailProperties();
        this.billingProperties = billingProperties != null ? billingProperties : new BillingProperties();
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> activeProfiles = Set.of(environment.getActiveProfiles());
        boolean isProduction = activeProfiles.contains("production");

        String storageStatus;
        if ("S3_COMPATIBLE".equalsIgnoreCase(storageProvider)) {
            storageStatus = "CONFIGURED (S3/R2 Cloud Storage)";
        } else {
            storageStatus = isProduction ? "WARNING (Local Ephemeral Filesystem in Production)" : "TEST_ONLY (Local Filesystem — Ephemeral)";
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

        String aiStatus;
        boolean aiEnabled = aiVisualizerProperties != null && aiVisualizerProperties.isEnabled();
        String aiProvider = aiVisualizerProperties != null && aiVisualizerProperties.getProvider() != null
                ? aiVisualizerProperties.getProvider().trim().toLowerCase()
                : "none";
        if (!aiEnabled || "none".equals(aiProvider)) {
            aiStatus = isProduction ? "NOT_CONFIGURED (Phase 15 Provider Activation Pending)" : "DISABLED (DisabledAiImageProvider)";
        } else if ("mock_configured".equals(aiProvider) || "fake".equals(aiProvider) || "mock".equals(aiProvider)) {
            aiStatus = isProduction ? "FORBIDDEN (Mock provider in Production)" : "TEST_ONLY (Mock AI Provider)";
        } else {
            boolean hasKey = aiVisualizerProperties.getApiKey() != null && !aiVisualizerProperties.getApiKey().isBlank() && !isPlaceholder(aiVisualizerProperties.getApiKey());
            aiStatus = hasKey ? "CONFIGURED (" + aiProvider + ")" : "NOT_CONFIGURED (Missing/Placeholder Key)";
        }

        String emailStatus;
        String emailProv = emailProperties != null && emailProperties.getProvider() != null
                ? emailProperties.getProvider().trim().toLowerCase()
                : "disabled";
        if ("disabled".equals(emailProv) || "none".equals(emailProv) || emailProv.isEmpty()) {
            emailStatus = isProduction ? "NOT_CONFIGURED (Phase 15 Provider Activation Pending)" : "DISABLED (DisabledEmailProvider)";
        } else if ("fake".equals(emailProv) || "mock".equals(emailProv) || "test".equals(emailProv)) {
            emailStatus = isProduction ? "FORBIDDEN (Mock provider in Production)" : "TEST_ONLY (Fake Email Provider)";
        } else if ("resend".equals(emailProv)) {
            boolean hasKey = emailProperties.getResend().getApiKey() != null && !emailProperties.getResend().getApiKey().isBlank() && !isPlaceholder(emailProperties.getResend().getApiKey());
            boolean hasFrom = emailProperties.getFromAddress() != null && !emailProperties.getFromAddress().isBlank() && !isPlaceholder(emailProperties.getFromAddress());
            emailStatus = (hasKey && hasFrom) ? "CONFIGURED (Resend)" : "NOT_CONFIGURED (Missing/Placeholder Key or From Address)";
        } else if ("ses".equals(emailProv)) {
            boolean hasFrom = emailProperties.getFromAddress() != null && !emailProperties.getFromAddress().isBlank() && !isPlaceholder(emailProperties.getFromAddress());
            emailStatus = hasFrom ? "CONFIGURED (AWS SES)" : "NOT_CONFIGURED (Missing/Placeholder From Address)";
        } else {
            emailStatus = "UNKNOWN (" + emailProv + ")";
        }

        String billingStatus;
        String billProv = billingProperties != null && billingProperties.getProvider() != null
                ? billingProperties.getProvider().trim().toLowerCase()
                : "disabled";
        if ("disabled".equals(billProv) || "none".equals(billProv) || billProv.isEmpty()) {
            billingStatus = isProduction ? "NOT_CONFIGURED (Phase 15 Provider Activation Pending)" : "DISABLED (DisabledBillingProvider)";
        } else if ("fake".equals(billProv) || "mock".equals(billProv) || "test".equals(billProv)) {
            billingStatus = isProduction ? "FORBIDDEN (Mock provider in Production)" : "TEST_ONLY (Fake Billing Provider)";
        } else if ("razorpay".equals(billProv)) {
            boolean hasKeys = billingProperties.getRazorpay().isConfigured();
            billingStatus = hasKeys ? "CONFIGURED (Razorpay)" : "NOT_CONFIGURED (Missing/Placeholder Key or Webhook Secret)";
        } else if ("stripe".equals(billProv)) {
            boolean hasKeys = billingProperties.getStripe().isConfigured();
            billingStatus = hasKeys ? "CONFIGURED (Stripe)" : "NOT_CONFIGURED (Missing/Placeholder Key or Webhook Secret)";
        } else {
            billingStatus = "UNKNOWN (" + billProv + ")";
        }

        log.info("================================================================================");
        log.info("           PLATFORM SYSTEM STARTUP & CONFIGURATION MATRIX                       ");
        log.info("================================================================================");
        log.info(" Active Profiles       : {}", activeProfiles.isEmpty() ? "[default]" : activeProfiles);
        log.info(" Database (PostgreSQL) : CONFIGURED");
        log.info(" Auth (OIDC)           : {}", oidcStatus);
        log.info(" Object Storage        : {}", storageStatus);
        log.info(" Malware Scanner       : {}", scannerStatus);
        log.info(" AI Visualizer         : {}", aiStatus);
        log.info(" WhatsApp              : MODE_A_ACTIVE (Direct wa.me) | MODE_B_NOT_CONFIGURED");
        log.info(" Billing Gateway       : {}", billingStatus);
        log.info(" Transactional Email   : {}", emailStatus);
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

            // AI Visualizer Production Validation
            if (aiEnabled && !"none".equals(aiProvider)) {
                if ("mock_configured".equals(aiProvider) || "fake".equals(aiProvider) || "mock".equals(aiProvider)) {
                    String errorMsg = "CRITICAL SECURITY FAULT: Mock AI provider ('" + aiProvider + "') MUST NOT be used in production!";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                String key = aiVisualizerProperties.getApiKey();
                if (key == null || key.isBlank() || isPlaceholder(key)) {
                    String errorMsg = "CRITICAL CONFIGURATION FAULT: AI provider '" + aiProvider + "' is enabled in production but apiKey is missing or placeholder.";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                String endpoint = aiVisualizerProperties.getEndpoint();
                if (endpoint != null && isPlaceholder(endpoint)) {
                    String errorMsg = "CRITICAL CONFIGURATION FAULT: AI provider '" + aiProvider + "' endpoint is a placeholder.";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                if (aiVisualizerProperties.getAllowedModels() != null && !aiVisualizerProperties.getAllowedModels().isEmpty()) {
                    String configuredModel = aiVisualizerProperties.getModel();
                    if (!aiVisualizerProperties.getAllowedModels().contains(configuredModel)) {
                        String errorMsg = "CRITICAL CONFIGURATION FAULT: Configured AI model '" + configuredModel + "' is not on permitted models allowlist.";
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                }
            } else {
                log.info("Production AI Visualizer is disabled/unconfigured. Platform starting safely with AI disabled (Phase 15 Provider Activation Pending).");
            }

            // Transactional Email Production Validation
            if (!"disabled".equals(emailProv) && !"none".equals(emailProv) && !emailProv.isEmpty()) {
                if ("fake".equals(emailProv) || "mock".equals(emailProv) || "test".equals(emailProv)) {
                    String errorMsg = "CRITICAL SECURITY FAULT: Mock email provider ('" + emailProv + "') MUST NOT be used in production!";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                if ("resend".equals(emailProv)) {
                    String key = emailProperties.getResend().getApiKey();
                    if (key == null || key.isBlank() || isPlaceholder(key)) {
                        String errorMsg = "CRITICAL CONFIGURATION FAULT: Resend email provider is selected in production but apiKey is missing or placeholder.";
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                    String from = emailProperties.getFromAddress();
                    if (from == null || from.isBlank() || isPlaceholder(from)) {
                        String errorMsg = "CRITICAL CONFIGURATION FAULT: Resend email provider is selected in production but fromAddress is missing or placeholder.";
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                } else if ("ses".equals(emailProv)) {
                    String region = emailProperties.getSes().getRegion();
                    if (region == null || region.isBlank() || isPlaceholder(region)) {
                        String errorMsg = "CRITICAL CONFIGURATION FAULT: AWS SES email provider is selected in production but region is missing or placeholder.";
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                    String from = emailProperties.getFromAddress();
                    if (from == null || from.isBlank() || isPlaceholder(from)) {
                        String errorMsg = "CRITICAL CONFIGURATION FAULT: AWS SES email provider is selected in production but fromAddress is missing or placeholder.";
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                } else {
                    String errorMsg = "CRITICAL CONFIGURATION FAULT: Unsupported production email provider: '" + emailProv + "'";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            } else {
                log.info("Production Transactional Email is disabled/unconfigured. Platform starting safely with email disabled (Phase 15 Provider Activation Pending).");
            }

            // Commercial Billing Production Validation
            if (!"disabled".equals(billProv) && !"none".equals(billProv) && !billProv.isEmpty()) {
                if ("fake".equals(billProv) || "mock".equals(billProv) || "test".equals(billProv)) {
                    String errorMsg = "CRITICAL SECURITY FAULT: Mock billing provider ('" + billProv + "') MUST NOT be used in production!";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                if ("razorpay".equals(billProv)) {
                    if (!billingProperties.getRazorpay().isConfigured()) {
                        String errorMsg = "CRITICAL CONFIGURATION FAULT: Razorpay billing provider is selected in production but keyId, keySecret, or webhookSecret is missing or placeholder.";
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                } else if ("stripe".equals(billProv)) {
                    if (!billingProperties.getStripe().isConfigured()) {
                        String errorMsg = "CRITICAL CONFIGURATION FAULT: Stripe billing provider is selected in production but apiKey or webhookSecret is missing or placeholder.";
                        log.error(errorMsg);
                        throw new IllegalStateException(errorMsg);
                    }
                } else {
                    String errorMsg = "CRITICAL CONFIGURATION FAULT: Unsupported production billing provider: '" + billProv + "'";
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            } else {
                log.info("Production Commercial Billing is disabled/unconfigured. Platform starting safely with commercial checkout disabled (Phase 15 Provider Activation Pending).");
            }
        }
    }

    private boolean isPlaceholder(String val) {
        if (val == null) return true;
        String lower = val.toLowerCase().trim();
        return lower.startsWith("your-") || lower.contains("placeholder") || lower.equals("change-me");
    }
}
