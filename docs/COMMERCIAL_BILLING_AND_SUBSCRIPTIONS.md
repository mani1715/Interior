# Commercial Billing & Subscription Lifecycle Architecture

## 1. Executive Summary & Status

The Elégance Interior Designer Platform features a production-ready, provider-neutral commercial billing and subscription lifecycle architecture. The platform decouples core business logic, capacity quotas, and entitlement enforcement from external payment providers, ensuring strict multi-tenant isolation, tamper protection, idempotency, and zero data loss on downgrades or cancellations.

> [!IMPORTANT]
> **Production Commercial Status:**
> - **Commercial Checkout Gateway:** Disabled (`NOT_CONFIGURED`) by default via `DisabledBillingProvider`.
> - **Pricing Decision Required:** Commercial price amounts and currency structures must be approved by product stakeholders prior to public activation.
> - **Provider Selection Required Before Commercial Activation:** Stripe or Razorpay credentials have not been configured and live infrastructure provisioning is postponed.
> - **Truthful UI State:** The `/workspace/billing` interface surfaces current tier, capacity usage, and plan comparison while clearly notifying users that live checkout is not active.

---

## 2. Locked Commercial Model & Capacity Envelopes

The platform enforces a four-tier plan structure defined across `billing_plans` and `plan_entitlements`:

| Feature / Envelope | BASE (Legacy) | STANDARD (Tier 1) | PREMIUM (Tier 2) | PRO (Tier 3) |
| :--- | :--- | :--- | :--- | :--- |
| **Purchasable** | **No** (Grandfathered) | Yes (when activated) | Yes (when activated) | Yes (when activated) |
| **Max Projects** | Unlimited (NULL) | **10** | **20** | **20** |
| **Photos per Project** | Unlimited (NULL) | **15** | **25** | **30** |
| **Cinematic Projects** | Unlimited (NULL) | **0** (Disabled) | **0** (Disabled) | **5** (within 20 total) |
| **Presentation Mode** | Standard + Cinematic | Standard only | Standard only | Standard + Cinematic |
| **Asset Storage** | Unlimited (NULL) | **5 GB** | **20 GB** | **50 GB** |
| **Lead CRM & WhatsApp**| Included | Included | Included | Included |
| **Basic Analytics** | Included | Included | Included | Included |
| **Advanced Analytics**| Included | Included | Included | Included |

### AI Visualizer Daily Quota Independence
- AI generation operates under a strictly separate daily quota (`AiDailyQuota`), resetting at 00:00 UTC daily.
- AI limits are completely independent of portfolio project and photograph capacity envelopes. Subscription changes, downgrades, or cancellations do not bundle or delete AI quotas.

---

## 3. Provider-Neutral Abstraction Architecture

Payment integration adheres to a pluggable provider boundary:
- **`BillingProvider` Interface:**
  - `getProviderName()`: Returns identifier (e.g., `NONE`, `RAZORPAY`, `STRIPE`, `FAKE`).
  - `isConfigured()`: Boolean indicating active credentials and ready checkout.
  - `createCheckoutSession(StudioDetailRecord, BillingPlanRecord, successUrl, cancelUrl, idempotencyKey)`: Generates provider-hosted checkout UI URL.
  - `cancelSubscription(StudioSubscriptionRecord, boolean atPeriodEnd)`: Communicates cancellation to external billing system.
  - `parseWebhook(rawPayload, headers)`: Cryptographically verifies signatures and extracts standard `WebhookEvent`.
- **`DisabledBillingProvider` (`@ConditionalOnMissingBean`):**
  - Default production component. Returns `NONE`, `isConfigured() == false`.
  - Blocks speculative checkout attempts with descriptive 400 Bad Request.
- **`FakeBillingProvider` (Test Harness):**
  - Fully simulated provider enabling deterministic testing of checkouts, webhooks, signature validation, amounts, replays, and failures.

---

## 4. Server-Authoritative Checkout & Idempotency

Client requests cannot specify prices, currencies, or arbitrary capacity parameters:
1. **Request:** Client calls `POST /studio/billing/checkout` with `planCode` and optional `idempotencyKey`.
2. **Authorization:** Only studio `OWNER` or `DESIGNER_ADMIN` (or platform `SUPER_ADMIN`) may initiate checkout. `DESIGNER_MEMBER` receives `403 Forbidden`. Cross-studio requests receive `403 Forbidden`.
3. **Plan Validation:** Plan must exist in `billing_plans`, have `active = true`, and `purchasable = true`.
4. **Idempotency Guarantee:**
   - Table `billing_checkout_sessions` enforces a unique constraint on `(studio_id, idempotency_key)`.
   - Repeated calls with the same key return the existing pending checkout session without duplicate records.
5. **Session Expiry:** Sessions have a 30-minute validity window.

---

## 5. Webhook Security & Tamper Protection

Incoming provider events at `/public/webhooks/billing/{provider}` pass through strict defenses:
1. **Cryptographic Signature Verification:**
   - Provider signature headers are inspected. Invalid or missing signatures fail immediately with `401 Unauthorized` (`SecurityException`).
2. **Replay Defense (Deduplication):**
   - Every event is recorded in `billing_events` with unique constraint `(provider, provider_event_id)`.
   - Duplicate deliveries are acknowledged (`200 OK`) and skipped without reprocessing or generating duplicate financial ledger transactions.
3. **Amount & Currency Integrity:**
   - Received payment amount is checked against the server-recorded `billing_checkout_sessions.amount_minor`.
   - Tampered webhook payloads with altered amounts or currencies are rejected with `SecurityException`.

---

## 6. Subscription State Machine & Lifecycle Workflows

Subscriptions transition across well-defined lifecycle states:

```
[ PENDING ] ──(Payment Succeeded)──> [ ACTIVE ]
    │                                   │   │
    │ (Payment Failed)                  │   └──(Renewal)──> [ ACTIVE (Extended) ]
    ▼                                   │
[ PAST_DUE ] <──(Payment Failed)────────┤
 (7-Day Grace)                          │
    │                                   │ (Cancel / Downgrade Requested)
    │ (Grace Expired)                   ▼
    │                           [ CANCEL_AT_PERIOD_END ]
    │                                   │
    │                                   │ (Period Ended)
    ▼                                   ▼
[ EXPIRED ] <───────────────────────[ CANCELLED ]
```

### Key State Behaviors:
- **`ACTIVE`:** Studio operates with full plan entitlements.
- **`PAST_DUE`:** Triggered by `payment.failed`. A 7-day grace period (`grace_period_end`) begins. Data remains accessible.
- **`CANCEL_AT_PERIOD_END`:** Studio retains full plan entitlements until `current_period_end`.
- **`EXPIRED` / `CANCELLED`:** EntitlementService safely falls back to `STANDARD` for commercial studios with expired subscriptions.

---

## 7. Safe Downgrade & Zero Data Loss Guarantee

The platform guarantees **ZERO DATA LOSS** under all billing transitions:
- Downgrades are scheduled for the end of the billing period via `downgrade_plan_id`.
- When transitioning to a lower tier (e.g., PRO to STANDARD):
  - **Existing Projects:** Fully preserved, retained, and publicly visible.
  - **Existing Photographs & Rooms:** Fully preserved and intact.
  - **Cinematic Presentations:** Existing projects retain cinematic configuration.
  - **Quota Enforcement:** If current project count exceeds target plan limit (e.g., 14/10), `assertProjectCreationAllowed` blocks *new* project creations until the studio is within limits.
  - **Zero Deletions:** No records are deleted, archived, or hidden automatically upon downgrade.

---

## 8. Administrative Overrides & Audit Boundaries

Platform Super Admins can manually update studio plans via `AdminService.updateStudioPlan`:
- **Source Separation:** Subscriptions updated or created by administrators are tagged with `source_type = 'MANUAL_ADMIN'` and `provider = 'ADMIN_OVERRIDE'`.
- **Financial Ledger Integrity:** Admin overrides never generate fake payment transactions in `billing_transactions`.
- **Audit Logging:** Every administrative override records an immutable `SUBSCRIPTION_OVERRIDE` event in `audit_events`.

---

## 9. Multi-Tenant Row Level Security (PostgreSQL 18.3)

All billing and subscription tables enforce strict PostgreSQL Row Level Security:
- `billing_checkout_sessions` (RLS ENABLED & FORCED)
- `studio_subscriptions` (RLS ENABLED & FORCED)
- `billing_transactions` (RLS ENABLED & FORCED)
- `billing_events` (RLS ENABLED & FORCED)
- `studio_daily_metrics` (RLS ENABLED & FORCED)
- `analytics_events` (RLS ENABLED & FORCED)

Tenants are strictly isolated: Studio A cannot query, insert, or update Studio B's checkout sessions, subscriptions, or transactions. Platform admins access cross-tenant records through verified `is_admin = true` session variables.

---

## 10. Verification & Test Evidence

| Test Suite | Tests Run | Result | Key Coverage |
| :--- | :--- | :--- | :--- |
| `CommercialBillingLifecycleTest` | 10 | **PASS** | Role access, idempotency, signature validation, replay defense, tamper checks, grace periods, safe downgrades, admin overrides, transaction history |
| `PostgreSqlAnalyticsBillingRlsTest` | 4 | **PASS** | PostgreSQL 18.3 RLS isolation for checkout sessions, subscriptions, transactions, and metrics |
| `BillingIntegrationTest` | 4 | **PASS** | Base plan fallback, disabled provider guard, tenant isolation |
| `EntitlementQuotaIntegrationTest` | 6 | **PASS** | Project limits, photo limits, cinematic limits, storage quotas, upload reservation quotas |
| Full Backend Test Suite (`mvnw test`) | 496 | **PASS** | Zero regressions across entire backend codebase |
| Frontend Vitest (`npm test`) | 368 | **PASS** | 52 test files, client-side billing components and stores |
| Frontend Production Build (`npm run build`) | 28 Routes | **PASS** | Zero TypeScript errors, all static and dynamic routes compiled |
