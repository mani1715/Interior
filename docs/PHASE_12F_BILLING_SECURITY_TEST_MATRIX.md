# Elégance — Phase 12F: Billing Security & Verification Test Matrix

**Document:** Comprehensive Billing, Security, Idempotency & Entitlement Test Suite Documentation  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12F Architecture & Quality Assurance  

---

## 1. Test Suite Coverage Summary

| Test Class | Focus Area | Test Count | Status |
|:---|:---|:---:|:---:|
| `BillingSecurityAndAuthenticityTest` | Provider HMAC-SHA256 authenticity, Stripe replay defense, open redirect defense, validator hardening, locked entitlement capacities | 16 | **PASSED** |
| `CommercialBillingLifecycleTest` | Zero-data-loss downgrade, server-authoritative checkout, replay deduplication, tampered amount rejection, past-due grace period, transaction history | 10 | **PASSED** |
| `EntitlementQuotaIntegrationTest` | Project and photo creation quotas, storage limits, cinematic presentation eligibility across plans | 6 | **PASSED** |
| `BillingIntegrationTest` | Base plan catalog fallback, disabled provider checkout rejection, tenant isolation on billing summary, fake provider checkout lifecycle | 4 | **PASSED** |
| `PostgreSqlAnalyticsBillingRlsTest` | Multi-tenant PostgreSQL Row-Level Security on billing tables (`studio_subscriptions`, `billing_transactions`) | 4 | **PASSED** |
| `ProductionConfigurationValidator*Test` | Production profile startup guard against mock providers and missing keys | 20 | **PASSED** |
| **Total Automated Billing / Security Tests** | | **60** | **100% PASSED** |

---

## 2. Detailed Test Cases & Invariants Tested

### 2.1 Provider Cryptographic Authenticity & Webhook Hardening
- **`testRazorpayValidSignature`:** Verifies that an authentic Razorpay webhook payload matching `X-Razorpay-Signature` HMAC-SHA256 signature is processed cleanly with exact extraction of payment ID, subscription ID, amount, and event type.
- **`testRazorpayMissingSignature`:** Asserts that webhook requests without `X-Razorpay-Signature` throw `SecurityException` and return HTTP 401 Unauthorized.
- **`testRazorpayInvalidSignature`:** Asserts that tampered or invalid hex signatures are rejected with `SecurityException` (HTTP 401).
- **`testStripeValidSignature`:** Verifies Stripe timestamped signature validation (`t=..., v1=...`) using HMAC-SHA256.
- **`testStripeReplayAttackTolerance`:** Simulates an attacker capturing and replaying a valid webhook 10 minutes later (>300 second tolerance window). Asserts that `StripeBillingProvider` rejects the event with `SecurityException` ("outside tolerance window").
- **`testStripeTamperedPayload`:** Verifies that a valid timestamp combined with a modified payload body fails cryptographic hash comparison.

### 2.2 Checkout Integrity & Open Redirect Defense
- **`testOpenRedirectDefense`:** Evaluates malicious redirect inputs (`https://evil.com/phish`, `//attacker.org`, `/\attacker.org`, `javascript:alert(1)`, CRLF injection). Verifies that `BillingService.sanitizeRedirectUrl` converts all dangerous targets to safe platform defaults (`/workspace/billing?checkout=...`) while preserving legitimate relative routes and local test hosts.
- **`testCheckoutSessionCreationAndIdempotency`:** Confirms that multiple checkout initiation requests using the same `idempotencyKey` return the existing pending session without creating redundant database rows.
- **`testWebhookAmountMismatchProtection`:** Simulates a forged webhook reporting a lower payment amount (e.g., ₹1.00 instead of ₹99.00). Asserts that `BillingService` verifies the webhook amount against the recorded server checkout session and aborts with `SecurityException`.

### 2.3 Role Security & Tenant Isolation
- **`testStaffCannotPerformBillingActions`:** Verifies that a studio staff member (`DESIGNER_MEMBER`) is blocked with HTTP 403 Forbidden from initiating checkout, scheduling downgrades, or cancelling subscriptions. Only `OWNER` and `DESIGNER_ADMIN` are permitted.
- **`testCrossStudioBillingIsolation`:** Confirms that Studio B members cannot access Studio A's billing summary or transaction history.
- **`testTenantIsolationOnBillingSummary`:** Proves multi-tenant boundary enforcement via HTTP MockMvc.

### 2.4 Subscription Lifecycle & Zero-Data-Loss Invariant
- **`testSafeDowngradeZeroDataLoss`:** A studio with 14 active projects on the **PRO** plan downgrades to **STANDARD** (quota: 10 projects).
  - Asserts that when the downgrade webhook arrives, the plan transitions to `STANDARD`.
  - **CRITICAL INVARIANT:** All 14 existing projects and associated media are 100% retained with ZERO deletions.
  - New project creation is restricted until the studio upgrades or archives/deletes surplus projects.
- **`testPaymentFailureGracePeriodAndRenewal`:** Verifies that failed recurring renewal sets status to `PAST_DUE` with a 7-day grace period, followed by successful renewal extending the period end date without interruption.
- **`testAdminManualOverrideIntegrity`:** Asserts that administrative plan overrides record `source_type = 'MANUAL_ADMIN'` and generate zero fake financial transactions.

### 2.5 Entitlement Matrix Verification (V030 Bases)
- **`testStandardEntitlementCapacity`:** Verifies STANDARD tier capacity (10 projects, 15 photos/project, 5 GB storage, 0 Cinematic projects, Leads CRM = true).
- **`testPremiumEntitlementCapacity`:** Verifies PREMIUM tier capacity (20 projects, 25 photos/project, 20 GB storage, 0 Cinematic projects, Leads CRM = true, Advanced Analytics = true).
- **`testProEntitlementCapacity`:** Verifies PRO tier capacity (20 projects, 30 photos/project, 50 GB storage, 5 Cinematic projects, Leads CRM = true, Advanced Analytics = true).
- **`testBasePlanInternalFallback`:** Verifies BASE internal tier is non-purchasable with price 0.
