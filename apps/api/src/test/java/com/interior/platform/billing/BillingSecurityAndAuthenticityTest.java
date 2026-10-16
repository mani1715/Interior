package com.interior.platform.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.billing.config.BillingProperties;
import com.interior.platform.billing.domain.EntitlementKey;
import com.interior.platform.billing.domain.PlanEntitlementRecord;
import com.interior.platform.billing.domain.BillingPlanRecord;
import com.interior.platform.billing.dto.CreateCheckoutRequest;
import com.interior.platform.billing.provider.DisabledBillingProvider;
import com.interior.platform.billing.provider.RazorpayBillingProvider;
import com.interior.platform.billing.provider.StripeBillingProvider;
import com.interior.platform.billing.provider.WebhookEvent;
import com.interior.platform.billing.repository.BillingRepository;
import com.interior.platform.billing.service.BillingService;
import com.interior.platform.config.ProductionConfigurationValidator;
import com.interior.platform.billing.config.BillingProviderConfig;
import com.interior.platform.billing.domain.BillingTransactionRecord;
import com.interior.platform.billing.domain.BillingTransactionStatus;
import com.interior.platform.billing.domain.StudioSubscriptionRecord;
import com.interior.platform.billing.domain.SubscriptionStatus;
import com.interior.platform.billing.provider.FakeBillingProvider;
import com.interior.platform.billing.service.EntitlementService;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 12F — Billing Security, Provider Cryptographic Authenticity, & Entitlement Guard Tests")
class BillingSecurityAndAuthenticityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private BillingService billingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired(required = false)
    private ProjectRepository projectRepository;

    private UUID createTestStudio(String slugPrefix) {
        UUID userId = UuidV7.randomUuid();
        UUID studioId = UuidV7.randomUuid();
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Test User', ?, 'ACTIVE')",
                userId, "user_" + userId + "@example.com");
        jdbcTemplate.update("INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at) VALUES (?, 'Test Studio', ?, ?, 'ACTIVE', 'PUBLISHED', 'Mumbai', 'MH', 'IN', now(), now())",
                studioId, slugPrefix + "-" + studioId.toString().substring(0, 8), userId);
        jdbcTemplate.update("INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES (?, ?, ?, 'DESIGNER_ADMIN', now())",
                UuidV7.randomUuid(), studioId, userId);
        return studioId;
    }

    private void cleanupStudio(UUID studioId) {
        if (studioId == null) return;
        jdbcTemplate.update("DELETE FROM billing_events WHERE studio_id = ?", studioId);
        jdbcTemplate.update("DELETE FROM billing_transactions WHERE studio_id = ?", studioId);
        jdbcTemplate.update("DELETE FROM billing_checkout_sessions WHERE studio_id = ?", studioId);
        jdbcTemplate.update("DELETE FROM studio_subscriptions WHERE studio_id = ?", studioId);
        jdbcTemplate.update("DELETE FROM studio_members WHERE studio_id = ?", studioId);
        jdbcTemplate.update("DELETE FROM designer_studios WHERE id = ?", studioId);
    }

    // =========================================================================
    // PART 1: RAZORPAY HMAC-SHA256 SIGNATURE VALIDATION
    // =========================================================================

    @Test
    @DisplayName("Razorpay: Valid HMAC-SHA256 signature accepted and correctly parsed")
    void testRazorpayValidSignature() throws Exception {
        String webhookSecret = "rzp_secret_key_123456789";
        BillingProperties props = new BillingProperties();
        props.setProvider("razorpay");
        props.getRazorpay().setKeyId("rzp_live_test_key_id");
        props.getRazorpay().setKeySecret("rzp_live_test_key_secret");
        props.getRazorpay().setWebhookSecret(webhookSecret);

        RazorpayBillingProvider provider = new RazorpayBillingProvider(props, objectMapper);

        String payload = """
            {
                "entity": "event",
                "account_id": "acc_12345",
                "event": "payment.captured",
                "payload": {
                    "payment": {
                        "entity": {
                            "id": "pay_987654321",
                            "amount": 4900,
                            "currency": "INR",
                            "status": "captured",
                            "order_id": "order_112233"
                        }
                    },
                    "subscription": {
                        "entity": {
                            "id": "sub_556677",
                            "status": "active"
                        }
                    }
                },
                "created_at": 1700000000
            }
        """;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String validSignature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        WebhookEvent event = provider.parseWebhook(payload, Map.of("X-Razorpay-Signature", validSignature));

        assertNotNull(event);
        assertEquals("payment.captured", event.eventType());
        assertEquals("pay_987654321", event.providerPaymentId());
        assertEquals("sub_556677", event.providerSubscriptionId());
        assertEquals(4900L, event.amountMinor());
        assertEquals("captured", event.status());
    }

    @Test
    @DisplayName("Razorpay: Missing signature header rejected with SecurityException")
    void testRazorpayMissingSignature() {
        BillingProperties props = new BillingProperties();
        props.setProvider("razorpay");
        props.getRazorpay().setWebhookSecret("test_secret");

        RazorpayBillingProvider provider = new RazorpayBillingProvider(props, objectMapper);
        String payload = "{\"event\":\"payment.captured\"}";

        assertThrows(SecurityException.class, () ->
                provider.parseWebhook(payload, Map.of())
        );
    }

    @Test
    @DisplayName("Razorpay: Forged/invalid signature rejected with SecurityException")
    void testRazorpayInvalidSignature() {
        BillingProperties props = new BillingProperties();
        props.setProvider("razorpay");
        props.getRazorpay().setWebhookSecret("test_secret");

        RazorpayBillingProvider provider = new RazorpayBillingProvider(props, objectMapper);
        String payload = "{\"event\":\"payment.captured\"}";

        assertThrows(SecurityException.class, () ->
                provider.parseWebhook(payload, Map.of("X-Razorpay-Signature", "forged_hex_signature_12345"))
        );
    }

    // =========================================================================
    // PART 2: STRIPE TIMESTAMPED SIGNATURE & REPLAY DEFENSE
    // =========================================================================

    @Test
    @DisplayName("Stripe: Valid timestamped signature accepted and correctly parsed")
    void testStripeValidSignature() throws Exception {
        String webhookSecret = "whsec_test_stripe_secret_key_123456";
        BillingProperties props = new BillingProperties();
        props.setProvider("stripe");
        props.getStripe().setApiKey("sk_test_12345");
        props.getStripe().setWebhookSecret(webhookSecret);
        props.getStripe().setTimestampToleranceSeconds(300);

        StripeBillingProvider provider = new StripeBillingProvider(props, objectMapper);

        String payload = """
            {
                "id": "evt_stripe_test_01",
                "type": "checkout.session.completed",
                "data": {
                    "object": {
                        "id": "cs_test_session_11",
                        "payment_intent": "pi_test_payment_22",
                        "subscription": "sub_test_sub_33",
                        "amount_total": 9900,
                        "currency": "inr",
                        "status": "complete"
                    }
                },
                "created": 1700000000
            }
        """;

        long nowSec = Instant.now().getEpochSecond();
        String signedPayload = nowSec + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sigHex = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        String header = "t=" + nowSec + ",v1=" + sigHex;

        WebhookEvent event = provider.parseWebhook(payload, Map.of("Stripe-Signature", header));

        assertNotNull(event);
        assertEquals("evt_stripe_test_01", event.eventId());
        assertEquals("checkout.session.completed", event.eventType());
        assertEquals("pi_test_payment_22", event.providerPaymentId());
        assertEquals("sub_test_sub_33", event.providerSubscriptionId());
        assertEquals(9900L, event.amountMinor());
        assertEquals("complete", event.status());
    }

    @Test
    @DisplayName("Stripe: Replay attack (timestamp older than 300s tolerance) rejected with SecurityException")
    void testStripeReplayAttackTolerance() throws Exception {
        String webhookSecret = "whsec_test_stripe_secret_key_123456";
        BillingProperties props = new BillingProperties();
        props.setProvider("stripe");
        props.getStripe().setApiKey("sk_test_12345");
        props.getStripe().setWebhookSecret(webhookSecret);
        props.getStripe().setTimestampToleranceSeconds(300);

        StripeBillingProvider provider = new StripeBillingProvider(props, objectMapper);
        String payload = "{\"id\":\"evt_old\",\"type\":\"payment_intent.succeeded\"}";

        // 600 seconds in the past
        long expiredTimestamp = Instant.now().getEpochSecond() - 600;
        String signedPayload = expiredTimestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sigHex = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        String header = "t=" + expiredTimestamp + ",v1=" + sigHex;

        SecurityException ex = assertThrows(SecurityException.class, () ->
                provider.parseWebhook(payload, Map.of("Stripe-Signature", header))
        );
        assertTrue(ex.getMessage().contains("outside tolerance window"));
    }

    @Test
    @DisplayName("Stripe: Tampered payload with valid timestamp rejected with SecurityException")
    void testStripeTamperedPayload() throws Exception {
        String webhookSecret = "whsec_test_stripe_secret_key_123456";
        BillingProperties props = new BillingProperties();
        props.setProvider("stripe");
        props.getStripe().setApiKey("sk_test_12345");
        props.getStripe().setWebhookSecret(webhookSecret);

        StripeBillingProvider provider = new StripeBillingProvider(props, objectMapper);
        String originalPayload = "{\"id\":\"evt_1\",\"type\":\"payment.succeeded\"}";
        String tamperedPayload = "{\"id\":\"evt_1\",\"type\":\"payment.succeeded\",\"forged\":true}";

        long nowSec = Instant.now().getEpochSecond();
        String signedPayload = nowSec + "." + originalPayload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sigHex = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        String header = "t=" + nowSec + ",v1=" + sigHex;

        // Valid signature for original, but delivered with tampered payload
        assertThrows(SecurityException.class, () ->
                provider.parseWebhook(tamperedPayload, Map.of("Stripe-Signature", header))
        );
    }

    // =========================================================================
    // PART 3: OPEN REDIRECT DEFENSE IN CHECKOUT SESSIONS
    // =========================================================================

    @Test
    @DisplayName("Open Redirect: Full redirect security matrix")
    void testRedirectSecurityMatrix() {
        // 1. Relative internal paths accepted
        assertEquals("/workspace/billing",
                billingService.sanitizeRedirectUrl("/workspace/billing", "/workspace/billing?checkout=success"));
        assertEquals("/workspace/subscription",
                billingService.sanitizeRedirectUrl("/workspace/subscription", "/workspace/billing?checkout=success"));
        assertEquals("/account/billing",
                billingService.sanitizeRedirectUrl("/account/billing", "/workspace/billing?checkout=success"));
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/workspace/billing?checkout=success", "/workspace/billing?checkout=success"));

        // 2. Configured trusted production origin accepted
        BillingProperties props = new BillingProperties();
        props.setAllowedRedirectHosts(List.of("firm.example", "app.myfirm.com"));
        billingService.setBillingProperties(props);
        billingService.setAllowedOrigins("https://configured-origin.com");

        assertEquals("https://firm.example/billing",
                billingService.sanitizeRedirectUrl("https://firm.example/billing", "/workspace/billing?checkout=success"));
        assertEquals("https://app.myfirm.com/dashboard",
                billingService.sanitizeRedirectUrl("https://app.myfirm.com/dashboard", "/workspace/billing?checkout=success"));
        assertEquals("https://configured-origin.com/callback",
                billingService.sanitizeRedirectUrl("https://configured-origin.com/callback", "/workspace/billing?checkout=success"));

        // 3. Localhost accepted in dev/test, but rejected when in production environment
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("test");
        billingService.setEnvironment(devEnv);
        assertEquals("http://localhost:3000/callback",
                billingService.sanitizeRedirectUrl("http://localhost:3000/callback", "/workspace/billing?checkout=success"));

        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");
        billingService.setEnvironment(prodEnv);
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("http://localhost:3000/callback", "/workspace/billing?checkout=success"),
                "localhost must be rejected in production environment when not explicitly configured");

        // 4. interior.com rejected when not explicitly configured
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("https://interior.com/checkout/success", "/workspace/billing?checkout=success"),
                "interior.com must be rejected as an unconfigured speculative domain");

        // 5. Arbitrary HTTPS external rejected
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("https://evil.example/steal-creds", "/workspace/billing?checkout=success"));

        // 6. Protocol-relative rejected
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("//evil.example/phish", "/workspace/billing?checkout=success"));

        // 7. Backslash form rejected
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("\\evil.example\\phish", "/workspace/billing?checkout=success"));
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/\\evil.example", "/workspace/billing?checkout=success"));

        // 8. CRLF rejected
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/billing\r\nLocation: https://evil.example", "/workspace/billing?checkout=success"));
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("https://evil.example/path\r\nInjected: val", "/workspace/billing?checkout=success"));

        // 9. Encoded external URL rejected
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/%2f%2fevil.example", "/workspace/billing?checkout=success"));
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/%5cevil.example", "/workspace/billing?checkout=success"));
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/%252f%252fevil.example", "/workspace/billing?checkout=success"));

        // Reset environment for subsequent tests
        billingService.setEnvironment(devEnv);
        billingService.setBillingProperties(new BillingProperties());
        billingService.setAllowedOrigins("http://localhost:3000");
    }

    // =========================================================================
    // PART 4: DISABLED PROVIDER BEHAVIOR & FAIL-FAST CONTROLS
    // =========================================================================

    @Test
    @DisplayName("DisabledBillingProvider: Fails fast on checkout creation and rejects webhooks")
    void testDisabledProviderBehavior() {
        DisabledBillingProvider disabled = new DisabledBillingProvider();
        assertEquals("NONE", disabled.getProviderName());
        assertFalse(disabled.isConfigured());

        assertThrows(IllegalStateException.class, () ->
                disabled.createCheckoutSession(null, null, "/ok", "/cancel", "idem_key")
        );

        assertThrows(UnsupportedOperationException.class, () ->
                disabled.parseWebhook("{}", Map.of())
        );
    }

    // =========================================================================
    // PART 5: PRODUCTION CONFIGURATION VALIDATOR & PROVIDER CONFIG HARDENING
    // =========================================================================

    private ProductionConfigurationValidator createValidator(MockEnvironment env, BillingProperties props) {
        env.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/interior_prod");
        com.interior.platform.media.config.MediaSecurityProperties mediaProps = new com.interior.platform.media.config.MediaSecurityProperties();
        mediaProps.setScanner("disabled");
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                env, mediaProps, null, null, null, props
        );
        ReflectionTestUtils.setField(validator, "devAuthEnabled", false);
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://app.firm.example");
        ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        ReflectionTestUtils.setField(validator, "storageProvider", "S3_COMPATIBLE");
        ReflectionTestUtils.setField(validator, "s3Endpoint", "https://r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(validator, "s3AccessKeyId", "valid-r2-key");
        ReflectionTestUtils.setField(validator, "s3SecretAccessKey", "valid-r2-secret");
        return validator;
    }

    @Test
    @DisplayName("ProductionConfigurationValidator: Forbids mock/fake billing provider in production profile")
    void testProductionValidatorRejectsMockProvider() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        BillingProperties props = new BillingProperties();
        props.setProvider("fake");

        ProductionConfigurationValidator validator = createValidator(env, props);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                validator.run(new DefaultApplicationArguments())
        );
        assertTrue(ex.getMessage().contains("Mock billing provider ('fake') MUST NOT be used in production"));
    }

    @Test
    @DisplayName("ProductionConfigurationValidator: Rejects Razorpay in production when keys are missing or placeholder")
    void testProductionValidatorRejectsUnconfiguredRazorpay() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        BillingProperties props = new BillingProperties();
        props.setProvider("razorpay");
        props.getRazorpay().setKeyId("your-key-id"); // placeholder

        ProductionConfigurationValidator validator = createValidator(env, props);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                validator.run(new DefaultApplicationArguments())
        );
        assertTrue(ex.getMessage().contains("Razorpay billing provider is selected in production but keyId, keySecret, or webhookSecret is missing or placeholder"));
    }

    @Test
    @DisplayName("ProductionConfigurationValidator: Rejects Stripe in production when keys are missing or placeholder")
    void testProductionValidatorRejectsUnconfiguredStripe() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        BillingProperties props = new BillingProperties();
        props.setProvider("stripe");
        props.getStripe().setApiKey("change-me"); // placeholder

        ProductionConfigurationValidator validator = createValidator(env, props);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                validator.run(new DefaultApplicationArguments())
        );
        assertTrue(ex.getMessage().contains("Stripe billing provider is selected in production but apiKey or webhookSecret is missing or placeholder"));
    }

    @Test
    @DisplayName("ProductionConfigurationValidator: Rejects commercial billing in production when no trusted production redirect host is configured")
    void testProductionValidatorRejectsEnabledBillingWithoutTrustedProductionHost() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        BillingProperties props = new BillingProperties();
        props.setProvider("razorpay");
        props.getRazorpay().setKeyId("rzp_live_valid_key");
        props.getRazorpay().setKeySecret("rzp_live_valid_secret");
        props.getRazorpay().setWebhookSecret("rzp_live_valid_whsec");

        ProductionConfigurationValidator validator = createValidator(env, props);
        ReflectionTestUtils.setField(validator, "allowedOrigins", ""); // no origin configured!

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                validator.run(new DefaultApplicationArguments())
        );
        assertTrue(ex.getMessage().contains("no trusted production redirect host or origin is configured"));
    }

    @Test
    @DisplayName("ProductionConfigurationValidator: Allows disabled billing provider in production (Phase 15 Pending)")
    void testProductionValidatorAllowsDisabledProvider() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        BillingProperties props = new BillingProperties();
        props.setProvider("disabled");

        ProductionConfigurationValidator validator = createValidator(env, props);

        // Does not throw
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("ProductionConfigurationValidator: Allows disabled billing in production even without configured redirect origins")
    void testProductionValidatorAllowsDisabledProviderWithoutRedirectOrigins() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        BillingProperties props = new BillingProperties();
        props.setProvider("disabled");

        ProductionConfigurationValidator validator = createValidator(env, props);
        ReflectionTestUtils.setField(validator, "allowedOrigins", "");

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("BillingProviderConfig: Strictly forbids mock/fake billing providers in production profile")
    void testBillingProviderConfigProductionEnforcement() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");
        BillingProviderConfig config = new BillingProviderConfig(prodEnv);

        BillingProperties fakeProps = new BillingProperties();
        fakeProps.setProvider("fake");
        assertThrows(IllegalStateException.class, () -> config.billingProvider(fakeProps, objectMapper));

        BillingProperties fakeUpperProps = new BillingProperties();
        fakeUpperProps.setProvider("  FAKE  ");
        assertThrows(IllegalStateException.class, () -> config.billingProvider(fakeUpperProps, objectMapper));

        BillingProperties mockProps = new BillingProperties();
        mockProps.setProvider("mock");
        assertThrows(IllegalStateException.class, () -> config.billingProvider(mockProps, objectMapper));

        BillingProperties testProps = new BillingProperties();
        testProps.setProvider("test");
        assertThrows(IllegalStateException.class, () -> config.billingProvider(testProps, objectMapper));

        // Unknown provider throws IllegalArgumentException regardless of profile
        BillingProperties garbageProps = new BillingProperties();
        garbageProps.setProvider("garbage");
        assertThrows(IllegalArgumentException.class, () -> config.billingProvider(garbageProps, objectMapper));
    }

    @Test
    @DisplayName("FakeBillingProvider: Direct instantiation blocked in production JVM")
    void testFakeBillingProviderDirectInstantiationBlockedInProduction() {
        System.setProperty("spring.profiles.active", "production");
        try {
            assertThrows(IllegalStateException.class, FakeBillingProvider::new);
        } finally {
            System.clearProperty("spring.profiles.active");
        }
    }

    // =========================================================================
    // PART 6: PRODUCT ENTITLEMENT MATRIX INTEGRITY (V030 CANONICAL BASES)
    // =========================================================================

    @Test
    @DisplayName("Entitlement Matrix: STANDARD tier matches locked capacity (10 projects, 15 photos/project, 5 GB storage, 0 Cinematic)")
    void testStandardEntitlementCapacity() {
        BillingPlanRecord standardPlan = billingRepository.findPlanByCode("STANDARD").orElseThrow();
        List<PlanEntitlementRecord> ents = billingRepository.findEntitlementsByPlanId(standardPlan.id());

        long projectLimit = findNumericEntitlement(ents, EntitlementKey.PROJECT_LIMIT);
        assertEquals(10L, projectLimit, "STANDARD must have 10 projects");

        long photoLimit = findNumericEntitlement(ents, EntitlementKey.PROJECT_PHOTO_LIMIT);
        assertEquals(15L, photoLimit, "STANDARD must have 15 photos per project");

        long storageBytes = findNumericEntitlement(ents, EntitlementKey.STORAGE_LIMIT_BYTES);
        assertEquals(5L * 1024L * 1024L * 1024L, storageBytes, "STANDARD must have 5 GB storage");

        long cinematicLimit = findNumericEntitlement(ents, EntitlementKey.CINEMATIC_PROJECT_LIMIT);
        assertEquals(0L, cinematicLimit, "STANDARD must have 0 cinematic projects");

        boolean cinematicAllowed = findBooleanEntitlement(ents, EntitlementKey.CINEMATIC_PORTFOLIO);
        assertFalse(cinematicAllowed, "STANDARD must not have cinematic portfolio access");
    }

    @Test
    @DisplayName("Entitlement Matrix: PREMIUM tier matches locked capacity (20 projects, 25 photos/project, 20 GB storage, 0 Cinematic)")
    void testPremiumEntitlementCapacity() {
        BillingPlanRecord premiumPlan = billingRepository.findPlanByCode("PREMIUM").orElseThrow();
        List<PlanEntitlementRecord> ents = billingRepository.findEntitlementsByPlanId(premiumPlan.id());

        long projectLimit = findNumericEntitlement(ents, EntitlementKey.PROJECT_LIMIT);
        assertEquals(20L, projectLimit, "PREMIUM must have 20 projects");

        long photoLimit = findNumericEntitlement(ents, EntitlementKey.PROJECT_PHOTO_LIMIT);
        assertEquals(25L, photoLimit, "PREMIUM must have 25 photos per project");

        long storageBytes = findNumericEntitlement(ents, EntitlementKey.STORAGE_LIMIT_BYTES);
        assertEquals(20L * 1024L * 1024L * 1024L, storageBytes, "PREMIUM must have 20 GB storage");

        long cinematicLimit = findNumericEntitlement(ents, EntitlementKey.CINEMATIC_PROJECT_LIMIT);
        assertEquals(0L, cinematicLimit, "PREMIUM must have 0 cinematic projects");

        boolean cinematicAllowed = findBooleanEntitlement(ents, EntitlementKey.CINEMATIC_PORTFOLIO);
        assertFalse(cinematicAllowed, "PREMIUM must not have cinematic portfolio access");
    }

    @Test
    @DisplayName("Entitlement Matrix: PRO tier matches locked capacity (20 projects, 30 photos/project, 50 GB storage, 5 Cinematic)")
    void testProEntitlementCapacity() {
        BillingPlanRecord proPlan = billingRepository.findPlanByCode("PRO").orElseThrow();
        List<PlanEntitlementRecord> ents = billingRepository.findEntitlementsByPlanId(proPlan.id());

        long projectLimit = findNumericEntitlement(ents, EntitlementKey.PROJECT_LIMIT);
        assertEquals(20L, projectLimit, "PRO must have 20 projects");

        long photoLimit = findNumericEntitlement(ents, EntitlementKey.PROJECT_PHOTO_LIMIT);
        assertEquals(30L, photoLimit, "PRO must have 30 photos per project");

        long storageBytes = findNumericEntitlement(ents, EntitlementKey.STORAGE_LIMIT_BYTES);
        assertEquals(50L * 1024L * 1024L * 1024L, storageBytes, "PRO must have 50 GB storage");

        long cinematicLimit = findNumericEntitlement(ents, EntitlementKey.CINEMATIC_PROJECT_LIMIT);
        assertEquals(5L, cinematicLimit, "PRO must have 5 cinematic projects");

        boolean cinematicAllowed = findBooleanEntitlement(ents, EntitlementKey.CINEMATIC_PORTFOLIO);
        assertTrue(cinematicAllowed, "PRO must have cinematic portfolio access");
    }

    @Test
    @DisplayName("Entitlement Matrix: BASE tier matches canonical V021/V030 source (non-purchasable, price 0, operational baseline)")
    void testBasePlanInternalFallback() {
        BillingPlanRecord basePlan = billingRepository.findPlanByCode("BASE").orElseThrow();
        assertFalse(basePlan.purchasable(), "BASE plan must not be purchasable");
        assertEquals(0L, basePlan.priceMinor(), "BASE plan price must be 0");
        assertEquals("NONE", basePlan.billingPeriod(), "BASE billing period must be NONE");

        List<PlanEntitlementRecord> ents = billingRepository.findEntitlementsByPlanId(basePlan.id());

        // Canonical V021 and V030 limits: numeric NULL represents unlimited in operational internal tier
        assertNull(findNullableNumericEntitlement(ents, EntitlementKey.PROJECT_LIMIT), "BASE project limit must be null (unlimited)");
        assertNull(findNullableNumericEntitlement(ents, EntitlementKey.PROJECT_PHOTO_LIMIT), "BASE photo limit must be null (unlimited)");
        assertNull(findNullableNumericEntitlement(ents, EntitlementKey.STORAGE_LIMIT_BYTES), "BASE storage limit must be null (unlimited)");
        assertNull(findNullableNumericEntitlement(ents, EntitlementKey.CINEMATIC_PROJECT_LIMIT), "BASE cinematic project limit must be null (unlimited)");

        // Canonical boolean entitlements
        assertTrue(findBooleanEntitlement(ents, EntitlementKey.CINEMATIC_PORTFOLIO), "BASE cinematic portfolio must be true");
        assertTrue(findBooleanEntitlement(ents, EntitlementKey.LEADS_CRM), "BASE CRM must be true");
        assertTrue(findBooleanEntitlement(ents, EntitlementKey.PORTFOLIO_PUBLISH), "BASE portfolio publish must be true");
        assertTrue(findBooleanEntitlement(ents, EntitlementKey.ANALYTICS_BASIC), "BASE basic analytics must be true");
        assertTrue(findBooleanEntitlement(ents, EntitlementKey.ANALYTICS_ADVANCED), "BASE advanced analytics must be true");
    }

    @Test
    @DisplayName("Entitlement Fallback: Missing or expired commercial subscription falls back safely to STANDARD, NOT unlimited BASE")
    void testMissingSubscriptionFallbackDoesNotGrantUnlimitedBase() {
        UUID studioId = createTestStudio("fallback-safety");
        try {
            BillingPlanRecord standardPlan = billingRepository.findPlanByCode("STANDARD").orElseThrow();

            // When a studio has an expired or cancelled commercial subscription
            StudioSubscriptionRecord expiredSub = new StudioSubscriptionRecord(
                    UuidV7.randomUuid(),
                    studioId,
                    standardPlan.id(),
                    SubscriptionStatus.EXPIRED,
                    "NONE",
                    null,
                    null,
                    Instant.now().minus(60, ChronoUnit.DAYS),
                    Instant.now().minus(30, ChronoUnit.DAYS),
                    false,
                    null,
                    Instant.now().minus(60, ChronoUnit.DAYS),
                    Instant.now().minus(30, ChronoUnit.DAYS),
                    1L,
                    "PAID_PROVIDER",
                    null,
                    null
            );
            billingRepository.saveSubscription(expiredSub);

            // Active plan must resolve to STANDARD, not BASE
            BillingPlanRecord effectivePlan = entitlementService.getActivePlan(studioId);
            assertEquals("STANDARD", effectivePlan.code(), "Expired commercial subscription must fall back to STANDARD tier");
            assertEquals(10L, entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_LIMIT), "Standard project limit must apply");
            assertEquals(15L, entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_PHOTO_LIMIT), "Standard photo limit must apply");
            assertEquals(5L * 1024L * 1024L * 1024L, entitlementService.getNumericLimit(studioId, EntitlementKey.STORAGE_LIMIT_BYTES), "Standard storage limit must apply");
            assertFalse(entitlementService.hasBooleanEntitlement(studioId, EntitlementKey.CINEMATIC_PORTFOLIO), "Standard must not have cinematic portfolio");
        } finally {
            cleanupStudio(studioId);
        }
    }

    @Test
    @DisplayName("Entitlement Fallback: Subscription with unresolvable plan ID safely defaults to STANDARD")
    void testUnknownPlanIdFailsSafely() {
        BillingRepository mockRepo = org.mockito.Mockito.mock(BillingRepository.class);
        ProjectRepository mockProjRepo = org.mockito.Mockito.mock(ProjectRepository.class);
        EntitlementService serviceWithMock = new EntitlementService(mockRepo, mockProjRepo);

        UUID studioId = UUID.randomUUID();
        UUID unmappedPlanId = UUID.randomUUID();
        BillingPlanRecord standardPlan = billingRepository.findPlanByCode("STANDARD").orElseThrow();

        StudioSubscriptionRecord mockSub = new StudioSubscriptionRecord(
                UuidV7.randomUuid(),
                studioId,
                unmappedPlanId,
                SubscriptionStatus.ACTIVE,
                "NONE",
                null,
                null,
                Instant.now().minus(5, ChronoUnit.DAYS),
                Instant.now().plus(25, ChronoUnit.DAYS),
                false,
                null,
                Instant.now().minus(5, ChronoUnit.DAYS),
                Instant.now().minus(5, ChronoUnit.DAYS),
                1L,
                "PAID_PROVIDER",
                null,
                null
        );

        org.mockito.Mockito.when(mockRepo.findActiveSubscription(studioId)).thenReturn(Optional.of(mockSub));
        org.mockito.Mockito.when(mockRepo.findPlanById(unmappedPlanId)).thenReturn(Optional.empty());
        org.mockito.Mockito.when(mockRepo.findPlanByCode("STANDARD")).thenReturn(Optional.of(standardPlan));

        BillingPlanRecord effective = serviceWithMock.getActivePlan(studioId);
        assertEquals("STANDARD", effective.code(), "Unresolvable plan ID must fall back safely to STANDARD");
    }

    // =========================================================================
    // PART 7: DURABLE FINANCIAL WEBHOOK IDEMPOTENCY ACROSS APPLICATION RESTART
    // =========================================================================

    @Test
    @DisplayName("Durable Idempotency: Webhook replay across service restart/re-instantiation is deduplicated by database constraint")
    void testDurableFinancialWebhookIdempotencySurvivesRestart() throws Exception {
        UUID studioId = createTestStudio("sub-restart");
        try {
            BillingPlanRecord plan = billingRepository.findPlanByCode("STANDARD").orElseThrow();
            String extSubId = "sub_ext_restart_001";
            StudioSubscriptionRecord sub = new StudioSubscriptionRecord(
                    UuidV7.randomUuid(), studioId, plan.id(), SubscriptionStatus.ACTIVE,
                    "RAZORPAY", "cus_restart_001", extSubId,
                    Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().plus(25, ChronoUnit.DAYS),
                    false, null, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now(), 1L
            );
            billingRepository.saveSubscription(sub);

            String webhookSecret = "whsec_restart_test_12345";
            BillingProperties props = new BillingProperties();
            props.setProvider("razorpay");
            props.getRazorpay().setKeyId("rzp_key_test");
            props.getRazorpay().setKeySecret("rzp_sec_test");
            props.getRazorpay().setWebhookSecret(webhookSecret);

            RazorpayBillingProvider provider1 = new RazorpayBillingProvider(props, objectMapper);
            BillingService serviceInstance1 = new BillingService(
                    billingRepository, entitlementService, studioRepository, provider1, projectRepository
            );

            String eventId = "evt_restart_unique_001";
            String payload = String.format("""
                {
                    "id": "%s",
                    "entity": "event",
                    "event": "payment.captured",
                    "payload": {
                        "payment": {
                            "entity": {
                                "id": "pay_restart_001",
                                "amount": 4900,
                                "currency": "INR",
                                "status": "captured",
                                "order_id": "order_restart_001"
                            }
                        },
                        "subscription": {
                            "entity": {
                                "id": "%s",
                                "status": "active"
                            }
                        }
                    },
                    "created_at": 1700000000
                }
            """, eventId, extSubId);

            // Compute valid HMAC-SHA256 signature
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String validSig = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            Map<String, String> headers = Map.of("X-Razorpay-Signature", validSig);

            // Step 1: Process initial event
            boolean initialHandled = serviceInstance1.handleWebhook("RAZORPAY", payload, headers);
            assertTrue(initialHandled, "Initial valid event must be processed");

            List<BillingTransactionRecord> txs = billingRepository.listTransactions(studioId, 10);
            assertEquals(1, txs.size(), "Initial event must create exactly 1 transaction record");
            assertEquals("pay_restart_001", txs.get(0).providerPaymentId());

            // Step 2: Tear down / simulate application restart (new service & provider instances)
            serviceInstance1 = null;
            RazorpayBillingProvider provider2 = new RazorpayBillingProvider(props, objectMapper);
            BillingService serviceInstance2 = new BillingService(
                    billingRepository, entitlementService, studioRepository, provider2, projectRepository
            );

            // Step 3: Replay exact same event payload & signature against restarted service
            boolean replayedHandled = serviceInstance2.handleWebhook("RAZORPAY", payload, headers);
            assertTrue(replayedHandled, "Replayed event must return true (acknowledged to provider)");

            // Step 4: Verify zero duplicate transaction or ledger mutation
            List<BillingTransactionRecord> txsAfterReplay = billingRepository.listTransactions(studioId, 10);
            assertEquals(1, txsAfterReplay.size(), "Durable idempotency must prevent duplicate financial ledger transaction");

            // Verify billing_events table has exactly 1 entry for this event
            Integer eventCount = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM billing_events WHERE provider = 'RAZORPAY' AND provider_event_id = ?",
                    Integer.class,
                    eventId
            );
            assertNotNull(eventCount);
            assertEquals(1, eventCount, "Durable database constraint uq_billing_event_provider must prevent duplicate event rows");
        } finally {
            cleanupStudio(studioId);
        }
    }

    @Test
    @DisplayName("Durable Idempotency: Concurrent duplicate webhooks result in exactly one financial ledger mutation")
    void testConcurrentDuplicateWebhookRace() throws Exception {
        UUID studioId = createTestStudio("conc-race");
        try {
            BillingPlanRecord plan = billingRepository.findPlanByCode("STANDARD").orElseThrow();
            String extSubId = "sub_ext_conc_race_001";
            StudioSubscriptionRecord sub = new StudioSubscriptionRecord(
                    UuidV7.randomUuid(), studioId, plan.id(), SubscriptionStatus.ACTIVE,
                    "RAZORPAY", "cus_conc_001", extSubId,
                    Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().plus(25, ChronoUnit.DAYS),
                    false, null, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now(), 1L
            );
            billingRepository.saveSubscription(sub);

            String webhookSecret = "whsec_conc_race_test";
            BillingProperties props = new BillingProperties();
            props.setProvider("razorpay");
            props.getRazorpay().setKeyId("rzp_key_test");
            props.getRazorpay().setKeySecret("rzp_sec_test");
            props.getRazorpay().setWebhookSecret(webhookSecret);

            RazorpayBillingProvider provider = new RazorpayBillingProvider(props, objectMapper);
            BillingService service = new BillingService(
                    billingRepository, entitlementService, studioRepository, provider, projectRepository
            );

            String eventId = "evt_conc_race_001";
            String payload = String.format("""
                {
                    "id": "%s",
                    "entity": "event",
                    "event": "payment.captured",
                    "payload": {
                        "payment": {
                            "entity": {
                                "id": "pay_conc_race_001",
                                "amount": 4900,
                                "currency": "INR",
                                "status": "captured",
                                "order_id": "order_conc_001"
                            }
                        },
                        "subscription": {
                            "entity": {
                                "id": "%s",
                                "status": "active"
                            }
                        }
                    },
                    "created_at": 1700000000
                }
            """, eventId, extSubId);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String validSig = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            Map<String, String> headers = Map.of("X-Razorpay-Signature", validSig);

            int threads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    return service.handleWebhook("RAZORPAY", payload, headers);
                }));
            }

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            for (Future<Boolean> f : futures) {
                assertTrue(f.get(10, TimeUnit.SECONDS), "Every webhook invocation must return true (either processed or deduplicated)");
            }
            executor.shutdown();

            // Assert exactly 1 transaction in DB
            List<BillingTransactionRecord> txs = billingRepository.listTransactions(studioId, 10);
            assertEquals(1, txs.size(), "Concurrent duplicate webhooks must result in exactly 1 transaction row in database");

            Integer eventCount = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM billing_events WHERE provider = 'RAZORPAY' AND provider_event_id = ?",
                    Integer.class,
                    eventId
            );
            assertNotNull(eventCount);
            assertEquals(1, eventCount, "Concurrent duplicate webhooks must result in exactly 1 billing_events record");
        } finally {
            cleanupStudio(studioId);
        }
    }

    @Test
    @DisplayName("Durable Idempotency: Stripe replay with fresh timestamp is deduplicated by database event ID")
    void testStripeReplayDeduplicatedSeparatelyFromTimestamp() throws Exception {
        UUID studioId = createTestStudio("stripe-replay");
        try {
            BillingPlanRecord plan = billingRepository.findPlanByCode("STANDARD").orElseThrow();
            String extSubId = "sub_stripe_replay_001";
            StudioSubscriptionRecord sub = new StudioSubscriptionRecord(
                    UuidV7.randomUuid(), studioId, plan.id(), SubscriptionStatus.ACTIVE,
                    "STRIPE", "cus_stripe_rep", extSubId,
                    Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().plus(25, ChronoUnit.DAYS),
                    false, null, Instant.now().minus(5, ChronoUnit.DAYS), Instant.now(), 1L
            );
            billingRepository.saveSubscription(sub);

            String webhookSecret = "whsec_stripe_replay_secret";
            BillingProperties props = new BillingProperties();
            props.setProvider("stripe");
            props.getStripe().setApiKey("sk_test_12345");
            props.getStripe().setWebhookSecret(webhookSecret);

            StripeBillingProvider provider = new StripeBillingProvider(props, objectMapper);
            BillingService service = new BillingService(
                    billingRepository, entitlementService, studioRepository, provider, projectRepository
            );

            String eventId = "evt_stripe_replay_test_001";
            String payload = String.format("""
                {
                    "id": "%s",
                    "type": "invoice.payment_succeeded",
                    "data": {
                        "object": {
                            "subscription": "%s",
                            "payment_intent": "pi_stripe_replay_001",
                            "amount_paid": 4900,
                            "currency": "inr",
                            "status": "paid"
                        }
                    }
                }
            """, eventId, extSubId);

            // T1 signature
            long t1 = Instant.now().getEpochSecond();
            String sigPayload1 = t1 + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sig1 = HexFormat.of().formatHex(mac.doFinal(sigPayload1.getBytes(StandardCharsets.UTF_8)));

            boolean handled1 = service.handleWebhook("STRIPE", payload, Map.of("Stripe-Signature", "t=" + t1 + ",v1=" + sig1));
            assertTrue(handled1);
            assertEquals(1, billingRepository.listTransactions(studioId, 10).size());

            // T2 signature: brand new timestamp within tolerance window, freshly signed, but SAME eventId
            long t2 = t1 + 5;
            String sigPayload2 = t2 + "." + payload;
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sig2 = HexFormat.of().formatHex(mac.doFinal(sigPayload2.getBytes(StandardCharsets.UTF_8)));

            boolean handled2 = service.handleWebhook("STRIPE", payload, Map.of("Stripe-Signature", "t=" + t2 + ",v1=" + sig2));
            assertTrue(handled2, "Replayed event with fresh signature must acknowledge true");

            // Transaction count must STILL be 1
            assertEquals(1, billingRepository.listTransactions(studioId, 10).size(),
                    "Database idempotency on uq_billing_event_provider must prevent duplicate processing even with fresh timestamp");
        } finally {
            cleanupStudio(studioId);
        }
    }

    @Test
    @DisplayName("Out-of-Order: Subscription cancelled state is immune to regression from stale pending events")
    void testOutOfOrderStateTransitions() {
        UUID studioId = createTestStudio("out-of-order");
        try {
            BillingPlanRecord plan = billingRepository.findPlanByCode("STANDARD").orElseThrow();
            String extSubId = "sub_ooo_001";
            StudioSubscriptionRecord cancelledSub = new StudioSubscriptionRecord(
                    UuidV7.randomUuid(), studioId, plan.id(), SubscriptionStatus.CANCELLED,
                    "FAKE", "cus_ooo", extSubId,
                    Instant.now().minus(30, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS),
                    true, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(30, ChronoUnit.DAYS), Instant.now(), 1L
            );
            billingRepository.saveSubscription(cancelledSub);

            FakeBillingProvider fake = new FakeBillingProvider();
            BillingService service = new BillingService(
                    billingRepository, entitlementService, studioRepository, fake, projectRepository
            );

            // Stale payment pending event arrives
            String stalePendingPayload = String.format("""
                {
                    "id": "evt_stale_pending_001",
                    "event": "payment.pending",
                    "subscription_id": "%s"
                }
            """, extSubId);

            boolean handled = service.handleWebhook("FAKE", stalePendingPayload, Map.of("x-fake-signature", "valid-sig"));
            assertTrue(handled);

            StudioSubscriptionRecord subAfter = billingRepository.findSubscriptionById(cancelledSub.id()).orElseThrow();
            assertEquals(SubscriptionStatus.CANCELLED, subAfter.status(), "Cancelled subscription must not regress to pending");
        } finally {
            cleanupStudio(studioId);
        }
    }

    @Test
    @DisplayName("Server-Authoritative Catalog: Client amount, currency, or unpurchasable plan overrides are rejected")
    void testServerAuthoritativeCatalogTamperDefense() {
        UUID studioId = createTestStudio("authoritative-cat");
        try {
            ActorContext adminActor = new ActorContext(
                    UuidV7.randomUuid(), "Studio Admin", "admin@example.com",
                    java.util.Set.of("DESIGNER"), java.util.Set.of(), studioId, "OWNER", "PASSKEY", true
            );

            BillingService activeBillingService = new BillingService(
                    billingRepository, entitlementService, studioRepository, new FakeBillingProvider(), projectRepository
            );

            // Attempting to checkout BASE plan (purchasable = false) must be rejected
            CreateCheckoutRequest baseReq = new CreateCheckoutRequest("BASE", "/ok", "/cancel");
            assertThrows(com.interior.platform.common.exception.BadRequestException.class, () ->
                    activeBillingService.initiateCheckout(adminActor, studioId, baseReq)
            );

            // Non-existent plan code rejected
            CreateCheckoutRequest invalidReq = new CreateCheckoutRequest("NON_EXISTENT_PLAN_XYZ", "/ok", "/cancel");
            assertThrows(com.interior.platform.common.exception.ResourceNotFoundException.class, () ->
                    activeBillingService.initiateCheckout(adminActor, studioId, invalidReq)
            );

            // Make STANDARD purchasable for test
            jdbcTemplate.update("UPDATE billing_plans SET purchasable = true, price_minor = 4900 WHERE code = 'STANDARD'");

            // Server-authoritative price check: Standard plan checkout is initialized with server price (4900)
            CreateCheckoutRequest standardReq = new CreateCheckoutRequest("STANDARD", "/ok", "/cancel");
            var response = activeBillingService.initiateCheckout(adminActor, studioId, standardReq);
            assertEquals("STANDARD", response.planCode());
            assertEquals(4900L, response.amountMinor());
            assertEquals("INR", response.currency());
        } finally {
            jdbcTemplate.update("UPDATE billing_plans SET purchasable = false, price_minor = 0 WHERE code = 'STANDARD'");
            cleanupStudio(studioId);
        }
    }

    private long findNumericEntitlement(List<PlanEntitlementRecord> ents, EntitlementKey key) {
        return ents.stream()
                .filter(e -> key.name().equalsIgnoreCase(e.entitlementKey()))
                .map(PlanEntitlementRecord::numericValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(-1L);
    }

    private Long findNullableNumericEntitlement(List<PlanEntitlementRecord> ents, EntitlementKey key) {
        PlanEntitlementRecord record = ents.stream()
                .filter(e -> key.name().equalsIgnoreCase(e.entitlementKey()))
                .findFirst()
                .orElse(null);
        return record != null ? record.numericValue() : null;
    }

    private boolean findBooleanEntitlement(List<PlanEntitlementRecord> ents, EntitlementKey key) {
        return ents.stream()
                .filter(e -> key.name().equalsIgnoreCase(e.entitlementKey()))
                .map(PlanEntitlementRecord::booleanValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(false);
    }
}
