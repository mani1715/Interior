# Elégance — Phase 12F: Commercial Billing, Payments & Entitlements Production Readiness

**Subsystem:** Commercial Billing, Subscriptions, Payment Provider Gateways, Webhook Authenticity & Entitlements  
**Phase:** 12F  
**Target Completion State:** Production-Ready Architecture without Real Payment Provider Credentials  
**Live Provider Activation:** Intentionally Deferred to Phase 15 (Pre-Launch Provider Activation)  
**Commercial Decision Status:** Policies & Pricing Deferred to Explicit Commercial Approval  

---

## 1. Executive Summary

Phase 12F establishes an enterprise-grade commercial billing, subscription lifecycle, entitlement enforcement, and payment provider integration subsystem for **Elégance — Interior Designer Platform**.

All components are implemented, locally validated, contract-tested, and configuration-ready with zero live API keys, zero paid merchant accounts, and zero customer card charges.

```
+------------------------------------------------------------------------------------+
|                         ELÉGANCE BILLING ARCHITECTURE                              |
+------------------------------------------------------------------------------------+
|                                                                                    |
|   +-------------------+       +-----------------------+     +------------------+   |
|   | Studio Designer   |       | Hosted Checkout       |     | Payment Gateway  |   |
|   | Admin Workspace   | ----> | Session Initiation    | --> | (Razorpay/Stripe)|   |
|   +-------------------+       | (Server-Authoritative)|     +------------------+   |
|                               +-----------------------+              |             |
|                                                                      | Webhook     |
|   +-------------------+       +-----------------------+              v             |
|   | Entitlements      |       | Webhook Processor     |     +------------------+   |
|   | & Quota Enforcer  | <---- | - HMAC Cryptographic  | <-- | Authenticated    |   |
|   | (Zero Data Loss)  |       |   Signature Check     |     | Webhook Endpoint |   |
|   +-------------------+       | - Durable Idempotency |     +------------------+   |
|                               +-----------------------+                            |
|                                                                                    |
+------------------------------------------------------------------------------------+
```

---

## 2. Core Architectural Pillars

### 2.1 Provider SPI & Factory Pattern
- **`BillingProvider` (SPI):** Standardized interface decoupling the core billing domain from payment gateway specifics. Defines `getProviderName()`, `isConfigured()`, `createCheckoutSession()`, `cancelSubscription()`, and `parseWebhook()`.
- **`DisabledBillingProvider`:** Production-safe default active when `app.billing.provider=disabled`. Rejects online checkout initiation fail-fast (`IllegalStateException`) and rejects webhooks (`UnsupportedOperationException`), guaranteeing zero state mutation.
- **`RazorpayBillingProvider`:** Candidate provider for domestic Indian payments (INR, UPI, Cards, NetBanking, Mandates). Validates `X-Razorpay-Signature` using server-computed HMAC-SHA256 with constant-time equality (`MessageDigest.isEqual`).
- **`StripeBillingProvider`:** Candidate provider for international transactions (multi-currency, Stripe Checkout, Customer Portal). Validates `Stripe-Signature` (`t=..., v1=...`) with timestamp replay attack defense (300-second tolerance window) and HMAC-SHA256 signature verification.
- **`FakeBillingProvider`:** Local development and test fixture validating end-to-end integration flows without external network requests. Strictly forbidden in production by `ProductionConfigurationValidator`.

### 2.2 Server-Authoritative Checkout & Open Redirect Protection
- **Amount & Currency Authority:** `CreateCheckoutRequest` accepts only `planCode`, `successUrl`, `cancelUrl`, and `idempotencyKey`. Client parameters cannot specify price, currency, or entitlement quotas; these are strictly resolved server-side from `billing_plans`.
- **Open Redirect Defense:** `BillingService.sanitizeRedirectUrl` sanitizes `successUrl` and `cancelUrl` against phishing and header injection vectors. External untrusted domains are stripped and safely redirected to `/workspace/billing?checkout=success` or `cancelled`. Relative URLs and configured origins (e.g., `app.local`, `interior.com`) are validated and preserved.
- **Checkout Session Idempotency:** The database table `billing_checkout_sessions` enforces unique constraint `uq_checkout_studio_idempotency (studio_id, idempotency_key)`. Repeated clicks return the existing active checkout session idempotently without creating duplicate DB records.

### 2.3 Webhook Authenticity & Deduplication Engine
- **Cryptographic Signature Verification:** Incoming webhooks must pass provider HMAC-SHA256 verification. Missing or invalid signatures immediately terminate with HTTP 401 Unauthorized.
- **Durable Event Deduplication:** The database table `billing_events` enforces unique constraint `uq_billing_event_provider (provider, provider_event_id)`. Duplicate webhook deliveries return HTTP 200 OK immediately without re-executing transactions or altering subscription status.
- **Checkout Session Cross-Check:** For initial checkout events, the webhook amount and currency are verified against the recorded server checkout session. Any mismatch aborts processing with `SecurityException`.

### 2.4 Subscription Lifecycle & Downgrade Safety Invariant
- **Single Active Subscription:** The database enforces a partial unique index `idx_studio_single_active_sub (studio_id) WHERE status IN ('ACTIVE', 'PENDING', 'CANCEL_AT_PERIOD_END')`.
- **Payment Failure & Grace Period:** Failed renewal webhooks transition subscriptions to `PAST_DUE` with an active grace period (default 7 days). Studio owners are notified via in-app alerts and transactional email (`renderPaymentFailed`).
- **Downgrade Invariant (CRITICAL):** DOWNGRADES NEVER DELETE CUSTOMER DATA! When a studio downgrades (e.g., PRO to STANDARD), all existing projects, photographs, leads, and media remain safely preserved in read-only / published mode. Only future creation or upload attempts are constrained if current totals exceed the lower tier's quota.

---

## 3. Entitlement Matrix (V030 Canonical Bases)

| Plan Code | Purchasable | Projects Limit | Photos / Project | Storage Limit | Cinematic Projects | AI Monthly Credits | Leads CRM | Basic Analytics | Advanced Analytics |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **BASE** | **No** (Internal fallback) | Unlimited (null) | Unlimited (null) | Unlimited (null) | 0 | 0 | Yes | Yes | Yes |
| **STANDARD** | Yes | 10 | 15 | 5 GB | 0 | 50 | Yes | Yes | No |
| **PREMIUM** | Yes | 20 | 25 | 20 GB | 0 | 150 | Yes | Yes | Yes |
| **PRO** | Yes | 20 | 30 | 50 GB | 5 | 500 | Yes | Yes | Yes |

*Note: Team-seat limits and commercial pricing remain `DECISION REQUIRED` and are intentionally not hardcoded into commercial policy.*

---

## 4. Production Configuration Hardening

`ProductionConfigurationValidator` enforces startup safety in the `production` Spring profile:
- `app.billing.provider=disabled`: Permitted. Platform starts cleanly with checkout disabled (Phase 15 Pending).
- `app.billing.provider=fake` or `mock`: **STRICTLY FORBIDDEN**. Fails startup with `IllegalStateException`.
- `app.billing.provider=razorpay`: Requires non-empty, non-placeholder `keyId`, `keySecret`, and `webhookSecret`.
- `app.billing.provider=stripe`: Requires non-empty, non-placeholder `apiKey` and `webhookSecret`.
- Unsupported providers: Terminate startup fail-fast.

---

## 5. Verification & Test Execution Results

- **Backend Suites (Maven surefire):**
  - `BillingSecurityAndAuthenticityTest`: 16 passed, 0 failed.
  - `CommercialBillingLifecycleTest`: 10 passed, 0 failed.
  - `EntitlementQuotaIntegrationTest`: 6 passed, 0 failed.
  - `BillingIntegrationTest`: 4 passed, 0 failed.
  - Total billing/security tests: **60 passed, 0 failed**.
- **Frontend Suites (Vitest):**
  - 52 test files passed, 373 tests passed, 0 failed.
- **Live Harness Script:**
  - `scripts/verify-payment-provider-live-integration.ps1`: Safe inspection passed (Exit 0).
