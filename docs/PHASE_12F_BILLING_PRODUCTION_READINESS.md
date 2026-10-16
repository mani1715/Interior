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
- **Open Redirect Defense:** `BillingService.sanitizeRedirectUrl` sanitizes `successUrl` and `cancelUrl` against phishing and header injection vectors. External untrusted domains are stripped and safely redirected to `/workspace/billing?checkout=success` or `cancelled`. Safe relative internal paths (`/workspace/billing`, `/workspace/subscription`, `/account/billing`) are validated and preserved. Local development origins (`localhost`, `127.0.0.1`, `app.local`) are permitted ONLY in non-production environments.
- **Strict Final Domain Neutrality (Phase 12F.1):** The final production domain is NOT selected. Therefore, NO invented or speculative public hostname (such as `interior.com`) is trusted by default. In `production`, trusted redirect origins must be explicitly configured via `app.security.allowed-origins` or `app.billing.allowed-redirect-hosts`. If commercial checkout is enabled without a trusted production redirect origin, `ProductionConfigurationValidator` fails startup fail-fast.
- **Checkout Session Idempotency:** The database table `billing_checkout_sessions` enforces unique constraint `uq_checkout_studio_idempotency (studio_id, idempotency_key)`. Repeated clicks return the existing active checkout session idempotently without creating duplicate DB records.

### 2.3 Webhook Authenticity & Deduplication Engine
- **Cryptographic Signature Verification:** Incoming webhooks must pass provider HMAC-SHA256 verification (`X-Razorpay-Signature` or Stripe `v1` HMAC). Missing or invalid signatures immediately terminate with HTTP 401 Unauthorized.
- **Durable Event Deduplication Across Application Restart (Phase 12F.1):** The database table `billing_events` enforces unique constraint `uq_billing_event_provider (provider, provider_event_id)` at Flyway baseline `V040`. Deduplication is durable at the database tier; restarting the application, recycling containers, or running multiple backend nodes maintains strict idempotency. Duplicate webhook deliveries return HTTP 200 OK immediately without re-executing transactions or altering subscription status.
- **Concurrent Race-Condition Resilience:** Multi-threaded simultaneous webhook deliveries (tested with 10 concurrent threads) are serialized by the PostgreSQL unique constraint, guaranteeing exactly one successful transaction insertion and zero duplicate financial records.
- **Checkout Session Cross-Check:** For initial checkout events, the webhook amount and currency are verified against the recorded server checkout session. Any mismatch aborts processing with `SecurityException`.

### 2.4 Subscription Lifecycle & Downgrade Safety Invariant
- **Single Active Subscription:** The database enforces a partial unique index `idx_studio_single_active_sub (studio_id) WHERE status IN ('ACTIVE', 'PENDING', 'CANCEL_AT_PERIOD_END')`.
- **Payment Failure & Grace Period:** Failed renewal webhooks transition subscriptions to `PAST_DUE` with an active grace period (default 7 days). Studio owners are notified via in-app alerts and transactional email (`renderPaymentFailed`).
- **Downgrade Invariant (CRITICAL):** DOWNGRADES NEVER DELETE CUSTOMER DATA! When a studio downgrades (e.g., PRO to STANDARD), all existing projects, photographs, leads, and media remain safely preserved in read-only / published mode. Only future creation or upload attempts are constrained if current totals exceed the lower tier's quota.

---

## 3. Canonical Entitlement Matrix (V021 & V030 Canonical Database Source)

| Plan Code | Purchasable | Projects Limit | Photos / Project | Storage Limit | Cinematic Projects | Leads CRM | Public Portfolio | Basic Analytics | Advanced Analytics | Source Definition |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **BASE** | **No** (Internal/Legacy) | Unlimited (`null`) | Unlimited (`null`) | Unlimited (`null`) | Unlimited (`null`) | Yes | Yes | Yes | Yes | Canonical V021/V030 operational baseline |
| **STANDARD** | Yes | 10 | 15 | 5 GB | 0 (Disabled) | Yes | Yes | Yes | No | Locked commercial Tier 1 |
| **PREMIUM** | Yes | 20 | 25 | 20 GB | 0 (Disabled) | Yes | Yes | Yes | Yes | Locked commercial Tier 2 |
| **PRO** | Yes | 20 | 30 | 50 GB | 5 | Yes | Yes | Yes | Yes | Locked commercial Tier 3 |

> **SAFE FALLBACK INVARIANT (Phase 12F.2):** If a commercial subscription expires, is cancelled, or falls past due, `EntitlementService` deterministically resolves the studio's active plan to **`STANDARD`** (constrained to 10 projects, 15 photos, 5 GB, 0 cinematic), NEVER granting unmetered `BASE` access to expired commercial subscribers. Unresolvable or missing catalog plan IDs likewise safely default to `STANDARD`.
>
> *Note: Team-seat limits and commercial pricing remain `DECISION REQUIRED` and are intentionally not hardcoded into commercial policy.*

---

## 4. Production Configuration Hardening & Fake Provider Ban (Phase 12F.1)

`ProductionConfigurationValidator` and runtime components enforce startup safety in the `production` Spring profile:
- `app.billing.provider=disabled`: Permitted. Platform starts cleanly with checkout disabled (Phase 15 Pending). Does not require trusted redirect origins when disabled.
- `app.billing.provider=fake` or `mock`: **STRICTLY FORBIDDEN**.
  - `BillingProviderConfig` throws `IllegalStateException` on startup if `fake`/`mock`/`test` is requested in `production`.
  - `ProductionConfigurationValidator` rejects mock/fake providers with `CRITICAL CONFIGURATION FAULT`.
  - `FakeBillingProvider` features defensive direct instantiation guards that check active profiles and throw `IllegalStateException` even if called programmatically.
- `app.billing.provider=razorpay`: Requires non-empty, non-placeholder `keyId`, `keySecret`, and `webhookSecret`.
- `app.billing.provider=stripe`: Requires non-empty, non-placeholder `apiKey` and `webhookSecret`.
- `Trusted Production Redirect Origin`: When commercial checkout is enabled (`razorpay` or `stripe`) in `production`, at least one non-placeholder, non-localhost origin must be configured in `app.security.allowed-origins` or `app.billing.allowed-redirect-hosts`.
- Unsupported providers: Terminate startup fail-fast with `IllegalArgumentException`.

---

## 5. Verification & Test Execution Results

- **Targeted Billing & Entitlement Suites:**
  - `BillingSecurityAndAuthenticityTest`: **27 passed, 0 failed, 0 skipped**.
  - `CommercialBillingLifecycleTest`: **10 passed, 0 failed, 0 skipped**.
  - `EntitlementQuotaIntegrationTest`: **6 passed, 0 failed, 0 skipped**.
  - `BillingIntegrationTest`: **4 passed, 0 failed, 0 skipped**.
  - `PostgreSqlAnalyticsBillingRlsTest`: **4 passed, 0 failed, 0 skipped**.
  - Total billing/security tests: **51 passed in targeted suite, 0 failed**.
- **Complete Full Platform Backend Suite (`mvn test`):**
  - **Tests run: 714, Failures: 0, Errors: 0, Skipped: 1** (100% PASS, Build Success).
- **Frontend Regression Suite (`apps/web`):**
  - Vitest Unit & Integration: **52 test files passed (52/52), 373 tests passed (373/373)**.
  - TypeScript Type Check (`npx tsc --noEmit`): **Clean (Exit 0, zero errors)**.
  - ESLint (`npm run lint`): **Clean (Exit 0, zero errors/warnings)**.
  - Next.js Production Build (`npm run build`): **Compiled successfully in 13.7s, 29/29 static pages generated, zero errors (Exit 0)**.
- **Live Harness Script:**
  - `scripts/verify-payment-provider-live-integration.ps1`: Safe inspection passed (Exit 0).

