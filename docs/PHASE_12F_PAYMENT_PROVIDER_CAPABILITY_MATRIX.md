# Elégance — Phase 12F: Payment Provider Capability Matrix

**Document:** Candidate Payment Gateway Comparison & Architectural Tradeoffs  
**Candidate Providers:** Razorpay vs. Stripe  
**Platform:** Elégance — Interior Designer Platform  
**Target Activation:** Phase 15 Pre-Launch Activation  

---

## 1. Gateway Overview

| Feature / Dimension | Razorpay | Stripe |
|:---|:---|:---|
| **Primary Market Strength** | India domestic (INR, UPI, RuPay, NetBanking, e-Mandates) | Global / International (135+ currencies, Cards, Wallets, SEPA) |
| **Domestic Currency** | INR native | INR supported (subject to RBI local entity / export rules) |
| **Recurring Subscriptions** | Razorpay Subscriptions (UPI AutoPay, Card e-mandate) | Stripe Billing (Customer Portal, Smart Retries, Proration) |
| **Hosted Checkout** | Razorpay Standard Checkout & Custom Hosted Checkout | Stripe Hosted Checkout (`checkout.stripe.com`) |
| **Webhook Signature Verification** | HMAC-SHA256 (`X-Razorpay-Signature`) | HMAC-SHA256 timestamped (`t=..., v1=...` with replay defense) |
| **Tax / Invoicing** | GST invoice generation with HSN/SAC codes | Stripe Tax (Global tax calculation, VAT, GST, Sales Tax) |
| **Customer Self-Service** | Requires custom frontend workspace management | Stripe Customer Portal (out-of-the-box self-service management) |
| **Developer API & SDK** | REST API + Java SDK (`razorpay-java`) | REST API + Java SDK (`stripe-java`) |

---

## 2. In-Depth Architectural Evaluation

### 2.1 Razorpay (Candidate 1)
- **Strengths:**
  1. **UPI & Indian Banking Superiority:** Flawless conversion for Indian interior design studios with native UPI intent, QR codes, netbanking with 50+ Indian banks, and RuPay cards.
  2. **RBI Compliance:** Built natively around Reserve Bank of India (RBI) tokenization, e-mandate, and recurring payment guidelines.
  3. **GST Invoicing:** Native GST compliance supporting State Code, GSTIN capture, and automated tax breakdown for B2B studios.
- **Limitations:**
  1. **International Friction:** Processing foreign credit cards requires additional documentation, international card activation, and forex handling.
  2. **Webhook Replay Protection:** Standard Razorpay webhooks do not include an explicit signed timestamp in the signature header, requiring server-side idempotency storage (which Elégance fully implements via `billing_events`).
- **Ideal Use Case:** Primary payment gateway if the commercial launch focuses primarily on Indian design studios.

### 2.2 Stripe (Candidate 2)
- **Strengths:**
  1. **Global Reach:** Unmatched multi-currency support (USD, EUR, GBP, AUD, SGD, AED) for global interior design studios and international clientele.
  2. **Stripe Billing & Customer Portal:** Fully managed self-service portal where studio owners can update payment cards, view invoice history, and manage recurring plans without custom UI code.
  3. **Timestamped Webhooks:** Built-in replay attack protection using timestamp header (`t=...`) verified against an expiration window (300 seconds).
  4. **Smart Retries & Dunning:** Machine-learning-driven dunning retry logic to maximize successful recurring renewals.
- **Limitations:**
  1. **Indian Domestic Restrictions:** Stripe India operates under RBI local processing regulations, requiring domestic entity establishment and specific recurring mandate setups.
  2. **UPI Recurring Availability:** UPI AutoPay integration on Stripe India is more constrained compared to Razorpay.
- **Ideal Use Case:** Primary payment gateway if the commercial launch targets international markets, or secondary gateway in a multi-gateway routing architecture.

---

## 3. Recommended Hybrid Provider Strategy for Phase 15

To maximize market capture and maintain architectural resilience, the recommended strategy for Phase 15 is a **Dual-Gateway Hybrid Architecture**:

```
+------------------------------------------------------------------------------------+
|                         CHECKOUT GATEWAY ROUTING                                   |
+------------------------------------------------------------------------------------+
|                                                                                    |
|                               +------------------+                                 |
|                               | Checkout Request |                                 |
|                               +------------------+                                 |
|                                        |                                           |
|                   +--------------------+--------------------+                      |
|                   |                                         |                      |
|        [Studio Country == "IN"                  [Studio Country != "IN"            |
|         & Currency == "INR"]                     or Multi-currency]                |
|                   |                                         |                      |
|                   v                                         v                      |
|         +-------------------+                     +-------------------+            |
|         |     Razorpay      |                     |      Stripe       |            |
|         | Billing Provider  |                     | Billing Provider  |            |
|         +-------------------+                     +-------------------+            |
|                                                                                    |
+------------------------------------------------------------------------------------+
```

1. **Domestic Studios (India / INR):** Route to **Razorpay** for frictionless UPI AutoPay, netbanking, and native GST compliance.
2. **International Studios (Global / Multi-Currency):** Route to **Stripe** for seamless international card processing, multi-currency settlement, and Stripe Customer Portal access.
3. **Common Domain Abstraction:** The existing `BillingProvider` SPI in `apps/api` seamlessly supports both providers with zero alterations to database schema or business logic.
