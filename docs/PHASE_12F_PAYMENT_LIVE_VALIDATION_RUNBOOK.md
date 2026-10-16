# Elégance — Phase 12F: Payment Provider Live Validation Runbook

**Runbook Target:** Phase 15 Pre-Launch Provider Activation  
**Objective:** Step-by-step procedures to activate and validate real payment gateways safely without incurring unauthorized customer charges.  
**Audience:** Platform Operations, DevOps, Lead Engineer  

---

## 1. Prerequisites for Live Activation

Before configuring credentials:
1. Approved merchant account with **Razorpay** and/or **Stripe**.
2. Completed KYC and business registration documents.
3. Registered production website domain and published terms of service, refund policy, and contact details.
4. Access to provider developer dashboards.

---

## 2. Validation Procedure 1: Razorpay Activation (Phase 15)

### Step 1: Obtain Test Mode Credentials
1. Log in to the Razorpay Dashboard.
2. Toggle to **Test Mode**.
3. Navigate to **Settings -> API Keys** and generate:
   - `Key ID` (e.g., `rzp_test_...`)
   - `Key Secret`
4. Navigate to **Settings -> Webhooks**:
   - Add new webhook URL: `https://<canonical-domain>/api/v1/public/webhooks/billing/razorpay`
   - Set Secret: generate a high-entropy secret string (min 32 characters)
   - Subscribe to events:
     - `payment.captured`
     - `payment.failed`
     - `order.paid`
     - `subscription.authenticated`
     - `subscription.activated`
     - `subscription.charged`
     - `subscription.pending`
     - `subscription.halted`
     - `subscription.cancelled`

### Step 2: Set Environment Variables in Staging / Sandbox
```bash
BILLING_PROVIDER=razorpay
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=whsec_...
```

### Step 3: Run Live Verification Harness
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-payment-provider-live-integration.ps1 -RequireLiveCredentials
```
Verify that the harness outputs:
```
Razorpay API Reachable: Yes (<150 ms)
Authentication Status : Authenticated (HTTP 200)
HMAC-SHA256 Engine    : Functional
RAZORPAY LIVE INTEGRATION VERIFIED SUCCESSFULLY.
```

### Step 4: Perform Test Subscription Checkout & Webhook Delivery
1. Log in as a studio owner to the Elégance staging environment.
2. Open `/workspace/billing` and initiate an upgrade to **STANDARD** or **PREMIUM**.
3. Use Razorpay test card / UPI credentials (`success@razorpay`) to complete payment.
4. Verify from the Razorpay dashboard that the webhook delivered with HTTP 200 OK.
5. In Elégance database, confirm:
   - `billing_events` has recorded the webhook event.
   - `studio_subscriptions` has transitioned to `ACTIVE`.
   - `billing_transactions` has logged the transaction with `status = 'SUCCEEDED'`.

---

## 3. Validation Procedure 2: Stripe Activation (Phase 15)

### Step 1: Obtain Test Mode Credentials
1. Log in to the Stripe Dashboard.
2. Toggle to **Test Mode**.
3. Navigate to **Developers -> API keys** and copy:
   - `Secret key` (`sk_test_...`)
4. Navigate to **Developers -> Webhooks**:
   - Add endpoint: `https://<canonical-domain>/api/v1/public/webhooks/billing/stripe`
   - Select events:
     - `checkout.session.completed`
     - `invoice.payment_succeeded`
     - `invoice.payment_failed`
     - `customer.subscription.created`
     - `customer.subscription.updated`
     - `customer.subscription.deleted`
   - Copy **Signing secret** (`whsec_...`).

### Step 2: Set Environment Variables in Staging / Sandbox
```bash
BILLING_PROVIDER=stripe
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_TIMESTAMP_TOLERANCE_SECONDS=300
```

### Step 3: Run Live Verification Harness
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-payment-provider-live-integration.ps1 -RequireLiveCredentials
```
Verify that the harness outputs:
```
Stripe API Reachable  : Yes (<150 ms)
Authentication Status : Authenticated (HTTP 200)
Stripe Signature (v1) : Functional
STRIPE LIVE INTEGRATION VERIFIED SUCCESSFULLY.
```

### Step 4: Perform Test Stripe Checkout & Webhook Delivery
1. Initiate checkout from `/workspace/billing`.
2. Use standard Stripe test card numbers (`4242 4242 4242 4242`).
3. Complete checkout and redirect back to platform.
4. Verify webhook delivery with HTTP 200 in Stripe dashboard and active subscription in database.

---

## 4. Promotion to Live Production Mode

When promoting from test mode to live production mode in Phase 15:
1. Replace `_test_` keys with live production keys (`rzp_live_...` or `sk_live_...`).
2. Replace test webhook endpoints with production HTTPS endpoints.
3. Validate that `ProductionConfigurationValidator` starts up without error.
4. Perform 1 live end-to-end charge of minimal value (e.g., ₹100 or $1), verify immediate receipt, and execute refund to confirm refund workflow.
