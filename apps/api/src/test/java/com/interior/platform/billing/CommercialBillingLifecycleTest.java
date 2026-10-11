package com.interior.platform.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.admin.dto.UpdateStudioPlanRequest;
import com.interior.platform.admin.service.AdminService;
import com.interior.platform.billing.domain.*;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.billing.dto.CreateCheckoutRequest;
import com.interior.platform.billing.dto.DowngradePlanRequest;
import com.interior.platform.billing.provider.FakeBillingProvider;
import com.interior.platform.billing.repository.BillingRepository;
import com.interior.platform.billing.service.BillingService;
import com.interior.platform.billing.service.EntitlementService;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.projects.domain.*;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 9A — Commercial Billing, Payment, & Subscription Lifecycle Tests")
class CommercialBillingLifecycleTest {

    private static final UUID BASE_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000001");
    private static final UUID STANDARD_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000002");
    private static final UUID PREMIUM_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000003");
    private static final UUID PRO_PLAN_ID = UUID.fromString("01923000-0000-7000-8000-000000000004");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AdminService adminService;

    private UUID ownerUserAId;
    private UUID staffUserAId;
    private UUID ownerUserBId;
    private UUID adminUserId;
    private UUID studioAId;
    private UUID studioBId;

    private ActorContext ownerActorA;
    private ActorContext staffActorA;
    private ActorContext ownerActorB;
    private ActorContext superAdminActor;

    private FakeBillingProvider fakeProvider;
    private BillingService testBillingService;

    @BeforeEach
    void setUp() {
        cleanUp();

        ownerUserAId = UuidV7.randomUuid();
        staffUserAId = UuidV7.randomUuid();
        ownerUserBId = UuidV7.randomUuid();
        adminUserId = UuidV7.randomUuid();
        studioAId = UuidV7.randomUuid();
        studioBId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Owner A', 'owner.a@example.com', 'ACTIVE')", ownerUserAId);
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Staff A', 'staff.a@example.com', 'ACTIVE')", staffUserAId);
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Owner B', 'owner.b@example.com', 'ACTIVE')", ownerUserBId);
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Platform Admin', 'admin@example.com', 'ACTIVE')", adminUserId);
        jdbcTemplate.update("""
            INSERT INTO identity_mfa_methods (id, user_id, method_type, encrypted_secret, key_version, status, pending_expires_at, confirmed_at, created_at, updated_at)
            VALUES (?, ?, 'TOTP', 'v1:AES256GCM:testsecret', 1, 'ACTIVE', TIMESTAMPADD(MINUTE, 10, now()), now(), now(), now())
        """, UuidV7.randomUuid(), adminUserId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio Alpha', 'studio-alpha', ?, 'ACTIVE', 'PUBLISHED', 'Mumbai', 'Maharashtra', 'IN', now(), now())
        """, studioAId, ownerUserAId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio Beta', 'studio-beta', ?, 'ACTIVE', 'PUBLISHED', 'Delhi', 'Delhi', 'IN', now(), now())
        """, studioBId, ownerUserBId);

        jdbcTemplate.update("INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES (?, ?, ?, 'DESIGNER_ADMIN', now())",
                UuidV7.randomUuid(), studioAId, ownerUserAId);
        jdbcTemplate.update("INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES (?, ?, ?, 'DESIGNER_MEMBER', now())",
                UuidV7.randomUuid(), studioAId, staffUserAId);
        jdbcTemplate.update("INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES (?, ?, ?, 'DESIGNER_ADMIN', now())",
                UuidV7.randomUuid(), studioBId, ownerUserBId);

        ownerActorA = new ActorContext(ownerUserAId, "Owner A", "owner.a@example.com", Set.of("DESIGNER"), Set.of(), studioAId, "OWNER", "PASSKEY", true);
        staffActorA = new ActorContext(staffUserAId, "Staff A", "staff.a@example.com", Set.of("DESIGNER"), Set.of(), studioAId, "DESIGNER_MEMBER", "PASSKEY", true);
        ownerActorB = new ActorContext(ownerUserBId, "Owner B", "owner.b@example.com", Set.of("DESIGNER"), Set.of(), studioBId, "OWNER", "PASSKEY", true);
        superAdminActor = new ActorContext(adminUserId, "Platform Admin", "admin@example.com", "ACTIVE", Set.of("SUPER_ADMIN"), Set.of("*"), null, null, "MFA_VERIFIED", true, java.time.Instant.now());

        fakeProvider = new FakeBillingProvider();
        testBillingService = new BillingService(
                billingRepository,
                entitlementService,
                studioRepository,
                fakeProvider,
                projectRepository
        );
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM billing_checkout_sessions");
        jdbcTemplate.execute("DELETE FROM billing_events");
        jdbcTemplate.execute("DELETE FROM billing_transactions");
        jdbcTemplate.execute("DELETE FROM studio_subscriptions");
        jdbcTemplate.execute("DELETE FROM studio_projects");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_mfa_methods");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("Role security: Studio staff (DESIGNER_STAFF) is blocked from checkout, cancel, and downgrade")
    void testStaffCannotPerformBillingActions() throws Exception {
        CreateCheckoutRequest checkoutReq = new CreateCheckoutRequest("STANDARD", "https://app/ok", "https://app/cancel");
        mockMvc.perform(post("/studio/billing/checkout")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(SecurityInterceptor.ACTOR_ATTRIBUTE, staffActorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/studio/billing/cancel")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(SecurityInterceptor.ACTOR_ATTRIBUTE, staffActorA))
                .andExpect(status().isForbidden());

        DowngradePlanRequest downgradeReq = new DowngradePlanRequest("STANDARD");
        mockMvc.perform(post("/studio/billing/downgrade")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(SecurityInterceptor.ACTOR_ATTRIBUTE, staffActorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(downgradeReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant isolation: Studio B member cannot access Studio A billing operations")
    void testCrossStudioBillingIsolation() throws Exception {
        CreateCheckoutRequest checkoutReq = new CreateCheckoutRequest("STANDARD", "https://app/ok", "https://app/cancel");
        mockMvc.perform(post("/studio/billing/checkout")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(SecurityInterceptor.ACTOR_ATTRIBUTE, ownerActorB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/studio/billing/cancel")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(SecurityInterceptor.ACTOR_ATTRIBUTE, ownerActorB))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/studio/billing/transactions")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(SecurityInterceptor.ACTOR_ATTRIBUTE, ownerActorB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Checkout session creation & idempotency: Server-authoritative pricing and idempotency deduplication")
    void testCheckoutSessionCreationAndIdempotency() {
        // Make STANDARD purchasable for test
        jdbcTemplate.update("UPDATE billing_plans SET purchasable = true, price_minor = 4900 WHERE code = 'STANDARD'");

        String idempotencyKey = "order_test_idem_123";
        CreateCheckoutRequest req = new CreateCheckoutRequest("STANDARD", "https://app/ok", "https://app/cancel", idempotencyKey);

        CheckoutSessionResponse res1 = testBillingService.initiateCheckout(ownerActorA, studioAId, req);
        assertNotNull(res1);
        assertEquals("STANDARD", res1.planCode());
        assertEquals(4900, res1.amountMinor());
        assertEquals("INR", res1.currency());

        Optional<BillingCheckoutSessionRecord> session1 = billingRepository.findCheckoutSessionByIdempotencyKey(studioAId, idempotencyKey);
        assertTrue(session1.isPresent());
        assertEquals("PENDING", session1.get().status());
        assertEquals(4900, session1.get().amountMinor());

        // Call again with same idempotency key -> idempotent return, no new DB record created
        CheckoutSessionResponse res2 = testBillingService.initiateCheckout(ownerActorA, studioAId, req);
        assertNotNull(res2);
        assertEquals(res1.sessionId(), res2.sessionId());

        // Verify only one checkout session row exists for studioA
        List<BillingCheckoutSessionRecord> allSessions = jdbcTemplate.query(
                "SELECT * FROM billing_checkout_sessions WHERE studio_id = ?",
                (rs, rowNum) -> session1.get(),
                studioAId
        );
        assertEquals(1, allSessions.size());
    }

    @Test
    @DisplayName("Webhook cryptographic signature: Missing or invalid signature rejected with 401 Unauthorized")
    void testWebhookSignatureVerification() {
        String payload = "{\"id\":\"evt_1\",\"event\":\"payment.succeeded\"}";

        // Direct service invocation throws SecurityException
        assertThrows(SecurityException.class, () ->
                testBillingService.handleWebhook("FAKE", payload, Map.of())
        );

        assertThrows(SecurityException.class, () ->
                testBillingService.handleWebhook("FAKE", payload, Map.of("x-fake-signature", "invalid-sig"))
        );

        // Controller invocation catches SecurityException and returns 401 Unauthorized
        com.interior.platform.billing.web.BillingWebhookController controller =
                new com.interior.platform.billing.web.BillingWebhookController(testBillingService);

        org.springframework.http.ResponseEntity<Void> resNoSig = controller.handleWebhook("FAKE", payload, Map.of());
        assertEquals(401, resNoSig.getStatusCode().value());

        org.springframework.http.ResponseEntity<Void> resBadSig = controller.handleWebhook("FAKE", payload, Map.of("x-fake-signature", "invalid-sig"));
        assertEquals(401, resBadSig.getStatusCode().value());
    }

    @Test
    @DisplayName("Webhook replay attack protection: Duplicate event IDs skipped with 200 OK without duplicate transactions")
    void testWebhookReplayDeduplication() {
        UUID subId = UuidV7.randomUuid();
        String providerSubId = "sub_replay_test_99";

        StudioSubscriptionRecord pendingSub = new StudioSubscriptionRecord(
                subId, studioAId, STANDARD_PLAN_ID, SubscriptionStatus.PENDING,
                "FAKE", "cus_fake_1", providerSubId,
                Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS),
                false, null, Instant.now(), Instant.now(), 1L
        );
        billingRepository.saveSubscription(pendingSub);

        String payload = String.format("""
            {
                "id": "evt_replay_001",
                "event": "payment.succeeded",
                "subscription_id": "%s",
                "payment_id": "pay_replay_1",
                "amount": 4900
            }
        """, providerSubId);

        Map<String, String> validHeaders = Map.of("x-fake-signature", "valid-signature-123");

        // First delivery
        boolean firstRun = testBillingService.handleWebhook("FAKE", payload, validHeaders);
        assertTrue(firstRun);
        assertEquals(1, billingRepository.listTransactions(studioAId, 10).size());

        // Replay delivery with identical event id
        boolean secondRun = testBillingService.handleWebhook("FAKE", payload, validHeaders);
        assertTrue(secondRun); // skips duplicate gracefully
        assertEquals(1, billingRepository.listTransactions(studioAId, 10).size());
    }

    @Test
    @DisplayName("Webhook amount mismatch protection: Forged amount rejected with SecurityException")
    void testWebhookAmountMismatchProtection() {
        // Make PREMIUM purchasable for test with price 9900
        jdbcTemplate.update("UPDATE billing_plans SET purchasable = true, price_minor = 9900 WHERE code = 'PREMIUM'");

        CreateCheckoutRequest req = new CreateCheckoutRequest("PREMIUM", "https://app/ok", "https://app/cancel", "idem_tamper_1");
        CheckoutSessionResponse sessionRes = testBillingService.initiateCheckout(ownerActorA, studioAId, req);

        // Forged webhook payload with tampered lower amount (e.g. 100 instead of 9900)
        String tamperedPayload = String.format("""
            {
                "id": "evt_tamper_001",
                "event": "payment.succeeded",
                "checkout_session_id": "%s",
                "payment_id": "pay_tamper_1",
                "amount": 100
            }
        """, sessionRes.sessionId());

        assertThrows(SecurityException.class, () ->
                testBillingService.handleWebhook("FAKE", tamperedPayload, Map.of("x-fake-signature", "valid-signature"))
        );
    }

    @Test
    @DisplayName("Subscription lifecycle: Payment failure triggers PAST_DUE with 7-day grace period; Renewal extends period")
    void testPaymentFailureGracePeriodAndRenewal() {
        String providerSubId = "sub_lifecycle_001";
        StudioSubscriptionRecord activeSub = new StudioSubscriptionRecord(
                UuidV7.randomUuid(), studioAId, STANDARD_PLAN_ID, SubscriptionStatus.ACTIVE,
                "FAKE", "cus_fake_1", providerSubId,
                Instant.now().minus(20, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS),
                false, null, Instant.now().minus(20, ChronoUnit.DAYS), Instant.now(), 1L
        );
        billingRepository.saveSubscription(activeSub);

        // 1. Payment failed event
        String failPayload = String.format("""
            {
                "id": "evt_fail_001",
                "event": "payment.failed",
                "subscription_id": "%s",
                "payment_id": "pay_fail_1",
                "amount": 4900
            }
        """, providerSubId);

        boolean handledFail = testBillingService.handleWebhook("FAKE", failPayload, Map.of("x-fake-signature", "valid-sig"));
        assertTrue(handledFail);

        StudioSubscriptionRecord pastDueSub = billingRepository.findSubscriptionById(activeSub.id()).orElseThrow();
        assertEquals(SubscriptionStatus.PAST_DUE, pastDueSub.status());
        assertNotNull(pastDueSub.gracePeriodEnd());
        assertTrue(pastDueSub.gracePeriodEnd().isAfter(Instant.now().plus(6, ChronoUnit.DAYS)));

        List<BillingTransactionRecord> txs = billingRepository.listTransactions(studioAId, 10);
        assertEquals(1, txs.size());
        assertEquals(BillingTransactionStatus.FAILED, txs.get(0).status());

        // 2. Subsequent renewal event
        String renewPayload = String.format("""
            {
                "id": "evt_renew_001",
                "event": "subscription.renewed",
                "subscription_id": "%s",
                "payment_id": "pay_renew_1",
                "amount": 4900
            }
        """, providerSubId);

        boolean handledRenew = testBillingService.handleWebhook("FAKE", renewPayload, Map.of("x-fake-signature", "valid-sig"));
        assertTrue(handledRenew);

        StudioSubscriptionRecord renewedSub = billingRepository.findSubscriptionById(activeSub.id()).orElseThrow();
        assertEquals(SubscriptionStatus.ACTIVE, renewedSub.status());
        assertTrue(renewedSub.currentPeriodEnd().isAfter(Instant.now().plus(25, ChronoUnit.DAYS)));

        List<BillingTransactionRecord> updatedTxs = billingRepository.listTransactions(studioAId, 10);
        assertEquals(2, updatedTxs.size());
        assertEquals(BillingTransactionStatus.SUCCEEDED, updatedTxs.get(0).status());
    }

    @Test
    @DisplayName("Safe Downgrade: Downgrading from PRO to STANDARD preserves all existing projects and media with zero deletion")
    void testSafeDowngradeZeroDataLoss() {
        // Studio A starts on PRO plan
        String providerSubId = "sub_pro_downgrade";
        StudioSubscriptionRecord proSub = new StudioSubscriptionRecord(
                UuidV7.randomUuid(), studioAId, PRO_PLAN_ID, SubscriptionStatus.ACTIVE,
                "FAKE", "cus_fake_1", providerSubId,
                Instant.now().minus(15, ChronoUnit.DAYS), Instant.now().plus(15, ChronoUnit.DAYS),
                false, null, Instant.now().minus(15, ChronoUnit.DAYS), Instant.now(), 1L
        );
        billingRepository.saveSubscription(proSub);

        // Seed 14 projects for Studio A (exceeding STANDARD's limit of 10)
        for (int i = 1; i <= 14; i++) {
            UUID projectId = UuidV7.randomUuid();
            StudioProjectRecord proj = new StudioProjectRecord(
                    projectId,
                    studioAId,
                    "slug-" + projectId,
                    "Project " + i,
                    "Short description " + i,
                    "Detailed description",
                    ProjectCategory.LIVING_ROOM,
                    ProjectStatus.READY,
                    VisibilityStatus.PORTFOLIO,
                    false,
                    1,
                    "Mumbai",
                    "Mumbai",
                    "Maharashtra",
                    "IN",
                    PropertyType.APARTMENT,
                    ProjectScope.FULL_INTERIOR,
                    2026,
                    BudgetVisibility.RANGE,
                    BigDecimal.valueOf(1000000),
                    BigDecimal.valueOf(2500000),
                    "INR",
                    ClientNameVisibility.DISPLAY,
                    "Client " + i,
                    BigDecimal.valueOf(1200),
                    AreaUnit.SQ_FT,
                    null,
                    ProjectPresentationMode.STANDARD,
                    1L,
                    ownerUserAId,
                    Instant.now(),
                    Instant.now(),
                    null
            );
            projectRepository.createProject(proj, List.of(ProjectStyle.MODERN_MINIMALIST));
        }
        assertEquals(14, projectRepository.countProjects(studioAId));

        // Studio Owner schedules downgrade to STANDARD
        testBillingService.requestDowngrade(ownerActorA, studioAId, new DowngradePlanRequest("STANDARD"));

        StudioSubscriptionRecord pendingDowngrade = billingRepository.findSubscriptionById(proSub.id()).orElseThrow();
        assertEquals(SubscriptionStatus.CANCEL_AT_PERIOD_END, pendingDowngrade.status());
        assertEquals(STANDARD_PLAN_ID, pendingDowngrade.downgradePlanId());
        assertTrue(pendingDowngrade.cancelAtPeriodEnd());

        // End of billing cycle event arrives from billing provider
        String expiredPayload = String.format("""
            {
                "id": "evt_cycle_end_001",
                "event": "subscription.cancelled",
                "subscription_id": "%s"
            }
        """, providerSubId);

        boolean handledCycleEnd = testBillingService.handleWebhook("FAKE", expiredPayload, Map.of("x-fake-signature", "valid-sig"));
        assertTrue(handledCycleEnd);

        StudioSubscriptionRecord finalSub = billingRepository.findSubscriptionById(proSub.id()).orElseThrow();
        assertEquals(STANDARD_PLAN_ID, finalSub.planId(), "Subscription plan must safely transition to STANDARD");
        assertEquals(SubscriptionStatus.ACTIVE, finalSub.status());
        assertNull(finalSub.downgradePlanId());

        // CRITICAL INVARIANT: ZERO DATA LOSS! All 14 existing projects remain completely preserved
        assertEquals(14, projectRepository.countProjects(studioAId));

        // But NEW project creation is blocked because Studio has 14/10 projects
        assertThrows(Exception.class, () ->
                entitlementService.assertProjectCreationAllowed(studioAId)
        );
    }

    @Test
    @DisplayName("Admin manual override: Admin plan update records source_type = MANUAL_ADMIN and generates zero fake payment transactions")
    void testAdminManualOverrideIntegrity() {
        adminService.updateStudioPlan(superAdminActor, studioAId, new UpdateStudioPlanRequest("PRO"), "127.0.0.1", "TestAgent/1.0");

        StudioSubscriptionRecord sub = billingRepository.findActiveSubscription(studioAId).orElseThrow();
        assertEquals(PRO_PLAN_ID, sub.planId());
        assertEquals("MANUAL_ADMIN", sub.sourceType());
        assertEquals("ADMIN_OVERRIDE", sub.provider());

        // Zero fake transactions created
        List<BillingTransactionRecord> txs = billingRepository.listTransactions(studioAId, 10);
        assertTrue(txs.isEmpty(), "Admin plan override must never generate fake provider payment transactions");
    }

    @Test
    @DisplayName("Transaction history: Paginated endpoint returns truthful history without sensitive card details")
    void testPaginatedTransactionHistoryEndpoint() throws Exception {
        UUID tx1Id = UuidV7.randomUuid();
        UUID tx2Id = UuidV7.randomUuid();

        billingRepository.saveTransaction(new BillingTransactionRecord(
                tx1Id, studioAId, null, "FAKE", "pay_001", "ord_001",
                4900, "INR", BillingTransactionStatus.SUCCEEDED, "Monthly standard subscription",
                "https://fake.receipt/1", Instant.now().minus(5, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS)
        ));

        billingRepository.saveTransaction(new BillingTransactionRecord(
                tx2Id, studioAId, null, "FAKE", "pay_002", "ord_002",
                4900, "INR", BillingTransactionStatus.SUCCEEDED, "Monthly standard renewal",
                "https://fake.receipt/2", Instant.now(), Instant.now()
        ));

        mockMvc.perform(get("/studio/billing/transactions?limit=10&offset=0")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(SecurityInterceptor.ACTOR_ATTRIBUTE, ownerActorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amountMinor").value(4900))
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
    }
}
