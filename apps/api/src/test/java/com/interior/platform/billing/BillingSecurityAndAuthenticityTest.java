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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    @DisplayName("Open Redirect: External phishing URLs sanitized to safe platform workspace paths")
    void testOpenRedirectDefense() {
        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("https://evil.com/steal-creds", "/workspace/billing?checkout=success"));

        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("//attacker.org/phish", "/workspace/billing?checkout=success"));

        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/\\attacker.org", "/workspace/billing?checkout=success"));

        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("javascript:alert(1)", "/workspace/billing?checkout=success"));

        assertEquals("/workspace/billing?checkout=success",
                billingService.sanitizeRedirectUrl("/billing\r\nLocation: https://evil.com", "/workspace/billing?checkout=success"));

        // Trusted relative and local development paths preserved
        assertEquals("/workspace/billing?status=complete",
                billingService.sanitizeRedirectUrl("/workspace/billing?status=complete", "/workspace/billing?checkout=success"));

        assertEquals("https://app.local/checkout/success",
                billingService.sanitizeRedirectUrl("https://app.local/checkout/success", "/workspace/billing?checkout=success"));
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
    // PART 5: PRODUCTION CONFIGURATION VALIDATOR — BILLING HARDENING
    // =========================================================================

    private ProductionConfigurationValidator createValidator(MockEnvironment env, BillingProperties props) {
        env.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/interior_prod");
        com.interior.platform.media.config.MediaSecurityProperties mediaProps = new com.interior.platform.media.config.MediaSecurityProperties();
        mediaProps.setScanner("disabled");
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                env, mediaProps, null, null, null, props
        );
        ReflectionTestUtils.setField(validator, "devAuthEnabled", false);
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://interior.com");
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
    @DisplayName("Entitlement Matrix: BASE tier is internal/legacy fallback (purchasable = false, priceMinor = 0)")
    void testBasePlanInternalFallback() {
        BillingPlanRecord basePlan = billingRepository.findPlanByCode("BASE").orElseThrow();
        assertFalse(basePlan.purchasable(), "BASE plan must not be purchasable");
        assertEquals(0L, basePlan.priceMinor(), "BASE plan price must be 0");
    }

    private long findNumericEntitlement(List<PlanEntitlementRecord> ents, EntitlementKey key) {
        return ents.stream()
                .filter(e -> key.name().equalsIgnoreCase(e.entitlementKey()))
                .map(PlanEntitlementRecord::numericValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(-1L);
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
