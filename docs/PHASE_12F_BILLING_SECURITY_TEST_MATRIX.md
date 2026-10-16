# Elégance — Phase 12F: Billing Security & Verification Test Matrix

**Document:** Comprehensive Billing, Security, Idempotency & Entitlement Test Suite Documentation  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12F Architecture & Quality Assurance  

---

## 1. Test Suite Coverage Summary

| Test Class | Focus Area | Test Count | Status |
|:---|:---|:---:|:---:|
| `BillingSecurityAndAuthenticityTest` | Provider HMAC-SHA256 authenticity, Stripe replay defense, redirect origin security, fake provider ban, durable idempotency across restart, race conditions, validator hardening, locked entitlement capacities | 25 | **PASSED** |
| `CommercialBillingLifecycleTest` | Zero-data-loss downgrade, server-authoritative checkout, replay deduplication, tampered amount rejection, past-due grace period, transaction history | 10 | **PASSED** |
| `EntitlementQuotaIntegrationTest` | Project and photo creation quotas, storage limits, cinematic presentation eligibility across plans | 6 | **PASSED** |
| `BillingIntegrationTest` | Base plan catalog fallback, disabled provider checkout rejection, tenant isolation on billing summary, fake provider checkout lifecycle | 4 | **PASSED** |
| `PostgreSqlAnalyticsBillingRlsTest` | Multi-tenant PostgreSQL Row-Level Security on billing tables (`studio_subscriptions`, `billing_transactions`) | 4 | **PASSED** |
| `ProductionConfigurationValidator*Test` | Production profile startup guard against mock providers, unconfigured gateways, and missing redirect origins | 20 | **PASSED** |
| **Total Automated Billing / Security Tests** | | **69** | **100% PASSED** |

---

## 2. Detailed Test Cases & Invariants Tested

### 2.1 Provider Cryptographic Authenticity & Webhook Hardening
- **`testRazorpayValidSignature`:** Verifies that an authentic Razorpay webhook payload matching `X-Razorpay-Signature` HMAC-SHA256 signature is processed cleanly with exact extraction of payment ID, subscription ID, amount, and event type.
- **`testRazorpayMissingSignature`:** Asserts that webhook requests without `X-Razorpay-Signature` throw `SecurityException` and return HTTP 401 Unauthorized.
- **`testRazorpayInvalidSignature`:** Asserts that tampered or invalid hex signatures are rejected with `SecurityException` (HTTP 401).
- **`testStripeValidSignature`:** Verifies Stripe timestamped signature validation (`t=..., v1=...`) using HMAC-SHA256.
- **`testStripeReplayAttackTolerance`:** Simulates an attacker capturing and replaying a valid webhook 10 minutes later (>300 second tolerance window). Asserts that `StripeBillingProvider` rejects the event with `SecurityException` ("outside tolerance window").
- **`testStripeTamperedPayload`:** Verifies that a valid timestamp combined with a modified payload body fails cryptographic hash comparison.
- **`testStripeReplayDeduplicationSurvivesTimestampWindow` (Phase 12F.1):** Proves that even within the timestamp window, DB-level uniqueness via `uq_billing_event_provider` suppresses replay attacks.
- **`testOutOfOrderWebhookStateTransitionProtection` (Phase 12F.1):** Verifies that a cancelled subscription cannot be reverted to active or pending by an out-of-order stale webhook.

### 2.2 Checkout Integrity, Redirect Origin Security & Domain Neutrality (Phase 12F.1)
- **`testOpenRedirectDefense`:** Evaluates malicious redirect inputs (`https://evil.com/phish`, `//attacker.org`, `/\attacker.org`, `javascript:alert(1)`, CRLF injection). Verifies that `BillingService.sanitizeRedirectUrl` converts all dangerous targets to safe platform defaults (`/workspace/billing?checkout=...`) while preserving legitimate relative routes.
- **`testProductionRedirectOriginEnforcement` (Phase 12F.1):** Proves that in `production` profile, unconfigured public domains (specifically `interior.com`) and local development origins (`localhost`, `127.0.0.1`, `app.local`) are strictly rejected unless explicitly configured on the server.
- **`testDisabledProviderFastFailure` (Phase 12F.1):** Confirms that `DisabledBillingProvider` throws fail-fast `IllegalStateException` on checkout and `UnsupportedOperationException` on webhooks.
- **`testCheckoutSessionCreationAndIdempotency`:** Confirms that multiple checkout initiation requests using the same `idempotencyKey` return the existing pending session without creating redundant database rows.
- **`testServerAuthoritativeCatalogTamperDefense` (Phase 12F.1):** Proves that user-controlled requests cannot alter prices or currency; catalog amounts are strictly retrieved from server `billing_plans`.
- **`testWebhookAmountMismatchProtection`:** Simulates a forged webhook reporting a lower payment amount (e.g., ₹1.00 instead of ₹99.00). Asserts that `BillingService` verifies the webhook amount against the recorded server checkout session and aborts with `SecurityException`.

### 2.3 Fake Provider Runtime Ban & Production Validation (Phase 12F.1)
- **`testProductionValidatorRejectsMockProvider`:** Verifies validator terminates startup if `fake` or `mock` is set in `production`.
- **`testProductionValidatorRejectsUnconfiguredRazorpay` & `testProductionValidatorRejectsUnconfiguredStripe`:** Verifies validator rejects enabled gateways with empty/placeholder keys.
- **`testProductionValidatorRequiresTrustedOriginWhenCheckoutEnabled`:** Enforces that enabling commercial checkout in production requires a configured, non-localhost trusted origin.
- **`testProductionValidatorAllowsDisabledProviderWithoutOrigin`:** Verifies `disabled` billing safely starts in production without requiring redirect host configuration.
- **`testBillingProviderConfigProductionBan`:** Asserts `BillingProviderConfig` throws `IllegalStateException` on `fake` in `production`.
- **`testBillingProviderConfigRejectsUnknownProvider`:** Asserts `BillingProviderConfig` throws `IllegalArgumentException` on unrecognized provider names.
- **`testFakeBillingProviderDirectInstantiationBlockInProduction`:** Confirms `FakeBillingProvider` constructors throw `IllegalStateException` if invoked in a production runtime environment.

### 2.4 Durable Financial Webhook Idempotency Across Restart & Race Conditions (Phase 12F.1)
- **`testDurableWebhookIdempotencySurvivesApplicationRestart`:** Simulates process termination and restart by re-instantiating fresh repository and service instances. Proves database constraint `uq_billing_event_provider` prevents duplicate event or transaction insertions.
- **`testConcurrentWebhookIdempotencyRaceCondition`:** Simulates 10 concurrent threads delivering the exact same webhook payload simultaneously. Asserts that exactly 1 event and 1 transaction row are persisted with zero race-condition duplicates.

### 2.5 Role Security & Tenant Isolation
- **`testStaffCannotPerformBillingActions`:** Verifies that a studio staff member (`DESIGNER_MEMBER`) is blocked with HTTP 403 Forbidden from initiating checkout, scheduling downgrades, or cancelling subscriptions. Only `OWNER` and `DESIGNER_ADMIN` are permitted.
- **`testCrossStudioBillingIsolation`:** Confirms that Studio B members cannot access Studio A's billing summary or transaction history.
- **`testTenantIsolationOnBillingSummary`:** Proves multi-tenant boundary enforcement via HTTP MockMvc.

### 2.6 Subscription Lifecycle & Zero-Data-Loss Invariant
- **`testSafeDowngradeZeroDataLoss`:** A studio with 14 active projects on the **PRO** plan downgrades to **STANDARD** (quota: 10 projects).
  - Asserts that when the downgrade webhook arrives, the plan transitions to `STANDARD`.
  - **CRITICAL INVARIANT:** All 14 existing projects and associated media are 100% retained with ZERO deletions.
  - New project creation is restricted until the studio upgrades or archives/deletes surplus projects.
- **`testPaymentFailureGracePeriodAndRenewal`:** Verifies that failed recurring renewal sets status to `PAST_DUE` with a 7-day grace period, followed by successful renewal extending the period end date without interruption.
- **`testAdminManualOverrideIntegrity`:** Asserts that administrative plan overrides record `source_type = 'MANUAL_ADMIN'` and generate zero fake financial transactions.

### 2.7 Entitlement Matrix Verification (V030 Bases)
- **`testStandardEntitlementCapacity`:** Verifies STANDARD tier capacity (10 projects, 15 photos/project, 5 GB storage, 0 Cinematic projects, Leads CRM = true).
- **`testPremiumEntitlementCapacity`:** Verifies PREMIUM tier capacity (20 projects, 25 photos/project, 20 GB storage, 0 Cinematic projects, Leads CRM = true, Advanced Analytics = true).
- **`testProEntitlementCapacity`:** Verifies PRO tier capacity (20 projects, 30 photos/project, 50 GB storage, 5 Cinematic projects, Leads CRM = true, Advanced Analytics = true).
- **`testBasePlanInternalFallback`:** Verifies BASE internal tier is non-purchasable with price 0.
