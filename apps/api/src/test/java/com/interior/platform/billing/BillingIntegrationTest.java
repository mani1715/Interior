package com.interior.platform.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.billing.domain.*;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.billing.dto.CreateCheckoutRequest;
import com.interior.platform.billing.provider.FakeBillingProvider;
import com.interior.platform.billing.repository.BillingRepository;
import com.interior.platform.billing.service.BillingService;
import com.interior.platform.billing.service.EntitlementService;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 28 — Billing & Entitlements Integration Tests")
class BillingIntegrationTest {

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

    private UUID userAId;
    private UUID userBId;
    private UUID studioAId;
    private UUID studioBId;

    @BeforeEach
    void setUp() {
        cleanUp();

        userAId = UuidV7.randomUuid();
        userBId = UuidV7.randomUuid();
        studioAId = UuidV7.randomUuid();
        studioBId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Designer A', 'alpha@example.com', 'ACTIVE')", userAId);
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Designer B', 'beta@example.com', 'ACTIVE')", userBId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio Alpha', 'studio-alpha', ?, 'ACTIVE', 'PUBLISHED', 'Mumbai', 'Maharashtra', 'IN', now(), now())
        """, studioAId, userAId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio Beta', 'studio-beta', ?, 'ACTIVE', 'PUBLISHED', 'Delhi', 'Delhi', 'IN', now(), now())
        """, studioBId, userBId);

        jdbcTemplate.update("""
            INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
            VALUES (?, ?, ?, 'OWNER', now())
        """, UuidV7.randomUuid(), studioAId, userAId);

        jdbcTemplate.update("""
            INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
            VALUES (?, ?, ?, 'OWNER', now())
        """, UuidV7.randomUuid(), studioBId, userBId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM billing_events");
        jdbcTemplate.execute("DELETE FROM billing_transactions");
        jdbcTemplate.execute("DELETE FROM studio_subscriptions");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("Catalog & Base fallback: internal BASE plan is default with unlimited base entitlements")
    void testCatalogAndBaseFallback() {
        BillingPlanRecord basePlan = entitlementService.getBasePlan();
        assertNotNull(basePlan);
        assertEquals("BASE", basePlan.code());
        assertFalse(basePlan.purchasable(), "Base plan must not be purchasable");
        assertEquals(0, basePlan.priceMinor());
        assertEquals("NONE", basePlan.billingPeriod());

        // Studio A without subscription falls back to BASE plan
        BillingPlanRecord activePlan = entitlementService.getActivePlan(studioAId);
        assertEquals(basePlan.id(), activePlan.id());

        // Unlimited numeric limit (null)
        Long projectLimit = entitlementService.getNumericLimit(studioAId, EntitlementKey.PROJECT_LIMIT);
        assertNull(projectLimit, "Default operational plan must have unlimited (null) project capacity");

        // Verify boolean entitlements
        assertTrue(entitlementService.hasBooleanEntitlement(studioAId, EntitlementKey.LEADS_CRM));
        assertTrue(entitlementService.hasBooleanEntitlement(studioAId, EntitlementKey.PORTFOLIO_PUBLISH));
        assertTrue(entitlementService.hasBooleanEntitlement(studioAId, EntitlementKey.ANALYTICS_BASIC));
        assertTrue(entitlementService.hasBooleanEntitlement(studioAId, EntitlementKey.ANALYTICS_ADVANCED));
    }

    @Test
    @DisplayName("Disabled billing provider: online checkout fails gracefully when provider is NOT_CONFIGURED")
    void testDisabledProviderRejectsCheckout() throws Exception {
        CreateCheckoutRequest req = new CreateCheckoutRequest(
                "BASE",
                "https://example.com/success",
                "https://example.com/cancel"
        );

        mockMvc.perform(post("/studio/billing/checkout")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(com.interior.platform.security.interceptor.SecurityInterceptor.ACTOR_ATTRIBUTE,
                                new ActorContext(userAId, "User A", "alpha@example.com", Set.of("DESIGNER"), Set.of(), studioAId, "OWNER", "PASSKEY", true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tenant isolation: Studio B member cannot access Studio A billing summary")
    void testTenantIsolationOnBillingSummary() throws Exception {
        // Studio A accessing Studio A -> 200 OK
        mockMvc.perform(get("/studio/billing")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(com.interior.platform.security.interceptor.SecurityInterceptor.ACTOR_ATTRIBUTE,
                                new ActorContext(userAId, "User A", "alpha@example.com", Set.of("DESIGNER"), Set.of(), studioAId, "OWNER", "PASSKEY", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studioId").value(studioAId.toString()))
                .andExpect(jsonPath("$.currentPlan.code").value("BASE"))
                .andExpect(jsonPath("$.commercialCheckoutEnabled").value(false));

        // Studio B accessing Studio A -> 403 Forbidden
        mockMvc.perform(get("/studio/billing")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(com.interior.platform.security.interceptor.SecurityInterceptor.ACTOR_ATTRIBUTE,
                                new ActorContext(userBId, "User B", "beta@example.com", Set.of("DESIGNER"), Set.of(), studioBId, "OWNER", "PASSKEY", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Provider lifecycle & webhooks: FakeBillingProvider creates checkout, handles webhooks with deduplication, and safe period-end cancellation")
    void testFakeBillingProviderLifecycleAndCancellation() {
        FakeBillingProvider fakeProvider = new FakeBillingProvider();
        BillingService fakeBillingService = new BillingService(
                billingRepository,
                entitlementService,
                studioRepository,
                fakeProvider
        );

        UUID customPlanId = UuidV7.randomUuid();
        jdbcTemplate.update("""
            INSERT INTO billing_plans (id, code, name, description, billing_period, currency, price_minor, active, purchasable, display_order)
            VALUES (?, 'TEST_PRO', 'Test Pro', 'Simulated Pro Tier', 'MONTHLY', 'INR', 9900, true, true, 1)
        """, customPlanId);

        ActorContext actorA = new ActorContext(userAId, "User A", "alpha@example.com", Set.of("DESIGNER"), Set.of(), studioAId, "OWNER", "PASSKEY", true);

        // 1. Initiate checkout
        CheckoutSessionResponse checkoutRes = fakeBillingService.initiateCheckout(actorA, studioAId, new CreateCheckoutRequest(
                "TEST_PRO", "https://app.local/success", "https://app.local/cancel"
        ));
        assertNotNull(checkoutRes);
        assertTrue(checkoutRes.checkoutUrl().contains("checkout.fake-billing.local"));
        assertEquals("TEST_PRO", checkoutRes.planCode());

        // 2. Simulate external subscription creation
        String providerSubId = "sub_fake_12345";
        StudioSubscriptionRecord pendingSub = new StudioSubscriptionRecord(
                UuidV7.randomUuid(),
                studioAId,
                customPlanId,
                SubscriptionStatus.PENDING,
                "FAKE",
                "cus_fake_999",
                providerSubId,
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.DAYS),
                false,
                null,
                Instant.now(),
                Instant.now(),
                1L
        );
        billingRepository.saveSubscription(pendingSub);

        // 3. Webhook: payment.succeeded
        String paymentSucceededPayload = String.format("""
            {
                "id": "evt_pay_001",
                "event": "payment.succeeded",
                "subscription_id": "%s",
                "payment_id": "pay_98765",
                "amount": 9900
            }
        """, providerSubId);

        boolean handled = fakeBillingService.handleWebhook("FAKE", paymentSucceededPayload, Map.of("x-fake-signature", "valid-sig"));
        assertTrue(handled);

        StudioSubscriptionRecord activeSub = billingRepository.findActiveSubscription(studioAId).orElseThrow();
        assertEquals(SubscriptionStatus.ACTIVE, activeSub.status());

        List<BillingTransactionRecord> txs = billingRepository.listTransactions(studioAId, 10);
        assertEquals(1, txs.size());
        assertEquals(BillingTransactionStatus.SUCCEEDED, txs.get(0).status());
        assertEquals(9900, txs.get(0).amountMinor());

        // 4. Webhook replay protection (deduplication)
        boolean duplicateHandled = fakeBillingService.handleWebhook("FAKE", paymentSucceededPayload, Map.of("x-fake-signature", "valid-sig"));
        assertTrue(duplicateHandled); // skipped gracefully, no duplicate transaction inserted
        assertEquals(1, billingRepository.listTransactions(studioAId, 10).size());

        // 5. Studio self-service cancellation at period end
        fakeBillingService.cancelSubscription(actorA, studioAId);

        StudioSubscriptionRecord cancellingSub = billingRepository.findSubscriptionById(activeSub.id()).orElseThrow();
        assertEquals(SubscriptionStatus.CANCEL_AT_PERIOD_END, cancellingSub.status());
        assertTrue(cancellingSub.cancelAtPeriodEnd());
        assertNotNull(cancellingSub.cancelledAt());

        // Verify studio data is NOT deleted
        assertTrue(studioRepository.findStudioById(studioAId).isPresent());
    }
}
