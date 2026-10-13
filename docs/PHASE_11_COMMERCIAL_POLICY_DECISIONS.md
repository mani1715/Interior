# Elégance — Phase 11 Commercial Policy Decisions
**Document Status:** Complete / Ready for User Approval  
**Phase:** Phase 11A — Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Commercial & Financial Governance Specification  

---

## 1. Locked Commercial Entitlement Model

The product capacity model is **LOCKED** in application code and database schema (`V030`, `V034`). No changes to these capacities are permitted under feature freeze.

| Metric / Entitlement | STANDARD | PREMIUM | PRO | BASE (Grandfathered) |
|---|---|---|---|---|
| **Maximum Active Projects** | **10** projects | **20** projects | **20** projects | 5 projects |
| **Portfolio Photos per Project** | **15** photos | **25** photos | **30** photos | 10 photos |
| **Total Cloud Storage Allocation** | **5 GB** | **20 GB** | **50 GB** | 2 GB |
| **Cinematic Presentations** | **0** (Disabled) | **0** (Disabled) | **Max 5** Active Projects | 0 |
| **High-Resolution Zoom & Reticle** | Included | Included | Included | Included |
| **Client Review Workflows** | Included | Included | Included | Included |
| **Studio Team Collaboration** | 1 Designer | Up to 3 Designers | Up to 10 Designers | 1 Designer |
| **Custom Domain Portfolio** | Subdomain only | Subdomain only | Custom domain enabled | Subdomain only |

---

## 2. Pricing Schedule Decision Worksheet

> **NOTICE:** Pricing is NOT locked in application code. The table below represents a decision worksheet for business leadership. Cells marked `[ DECISION REQUIRED ]` must be filled before commercial launch.

| Plan | Billing Period | Proposed INR (₹) | Proposed USD ($) | Discount vs Monthly | Decision Status |
|---|---|---|---|---|---|
| **STANDARD** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | 0% | Pending User Decision |
| **STANDARD** | Quarterly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | ~5% to 10% | Pending User Decision |
| **STANDARD** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | ~15% to 20% | Pending User Decision |
| **PREMIUM** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | 0% | Pending User Decision |
| **PREMIUM** | Quarterly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | ~5% to 10% | Pending User Decision |
| **PREMIUM** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | ~15% to 20% | Pending User Decision |
| **PRO** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | 0% | Pending User Decision |
| **PRO** | Quarterly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | ~5% to 10% | Pending User Decision |
| **PRO** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | ~15% to 20% | Pending User Decision |

---

## 3. Currency & Payment Methods

- **Primary Target Geography:** India Interior Design Studios (primary) + International Creative Agencies (secondary).
- **Display Currency:**
  - Option A: Single currency (INR `₹` for India domestic launch).
  - Option B: Multi-currency geo-IP detection (INR for India, USD for rest of world).
- **Payment Method Requirements:**
  - India: UPI (Google Pay, PhonePe, Paytm), Domestic Credit/Debit Cards (Visa, Mastercard, RuPay), NetBanking, and RBI e-mandate recurring debit.
  - International: International Credit Cards (Visa, Mastercard, Amex), Apple Pay, Google Pay.
- **Decision Status:** `DECISION REQUIRED`.

---

## 4. Billing Grace Period & Renewal Failure Policy

- **Current Implementation Truth:**
  - Application configuration defines: `app.billing.grace-period-days: 7` (7 days config default).
  - Webhook listener receives `INVOICE_PAYMENT_FAILED` events.
  - While within grace period, the studio retains active status, and in-app banner notifications alert studio admins.
- **Policy Decision Options:**
  1. *Strict (0-day grace):* Instant suspension to read-only state upon payment failure.
  2. *Standard (7-day grace - Config Default):* 7 calendar days to update payment method before entering read-only status.
  3. *Generous (14-day grace):* 14 days with automated reminder emails on days 1, 3, 7, and 12.
- **Decision Status:** `DECISION REQUIRED` (Recommended: Adopt the 7-day default).

---

## 5. Upgrade, Downgrade & Cancellation Invariants

### 5.1 Upgrades (Immediate)
- **Timing:** Takes effect **immediately** upon successful payment authorization.
- **Proration:** Prorated charge for the remaining duration of the active billing cycle.
- **Entitlements:** New project quotas, photo ceilings, and cinematic allocations are instantly available.

### 5.2 Downgrades (Safe Period-End)
- **Timing:** Enforced strictly at the **end of the active paid billing cycle** (`current_period_end`).
- **Data Preservation Guarantee:**
  - **ZERO DATA DELETION:** Existing projects, photos, and cinematic configurations are **never deleted or modified** upon downgrade.
  - If a studio exceeds the new tier's quota (e.g. downgrading from Pro with 20 projects to Standard with a 10-project cap), all 20 existing projects remain publicly accessible and viewable.
  - New project creation and new uploads are blocked until the studio either upgrades or archives projects below the new cap.
- **Status:** `LOCKED` (Hardened in `BillingService.java`).

### 5.3 Cancellations
- **Timing:** Cancellation scheduled for period end; access remains uninterrupted until paid term expires.
- **After Expiry:** Studio automatically transitions to Standard tier with zero loss of existing published content.

---

## 6. Refund Policy

- **Current Implementation:** Refund webhook SPI implemented; zero automated refund policies locked in code.
- **Policy Options for User Approval:**
  - Option 1: *Strict No-Refunds:* All fees non-refundable except where required by law.
  - Option 2: *7-Day Money-Back Guarantee:* Full refund on initial subscription within 7 calendar days of first charge.
  - Option 3: *Prorated Credits:* Service credits applied to future billing for unused subscription time.
- **Decision Status:** `DECISION REQUIRED`.

---

## 7. GST & Tax Compliance Specification (India Focus)

- **Statutory Classification:**
  - Service Accounting Code (SAC): **998314** (Information Technology Software and SaaS Services).
  - Applicable GST Rate: **18%** (CGST 9% + SGST 9% for intra-state; IGST 18% for inter-state).
- **B2B Studios:**
  - Requirement to collect and validate GSTIN and legal business name during checkout.
  - Generation of compliant B2B tax invoices displaying Supplier GSTIN, Customer GSTIN, HSN/SAC code, and tax breakdown.
- **International Customers:**
  - Zero-rated export of services under Letter of Undertaking (LUT), subject to export invoice compliance.
- **Decision Status:** `DECISION REQUIRED` (Requires company accountant and legal review).

---

## 8. Generative AI Commercial Policy

- **Technical Quota Truth:** Application enforces a strict daily rate limit of **20 generations / studio / UTC calendar day** (`dailyStudioLimit = 20`, reset at 00:00 UTC).
- **Commercial Options:**
  - Option A: *Included Quota:* 20 daily generations included at no extra cost in Pro and Premium plans; disabled on Standard.
  - Option B: *Universal Free Allowance:* 5/day on Standard, 15/day on Premium, 25/day on Pro.
  - Option C: *Prepaid Credit Add-On:* Studios purchase credit packs (e.g. ₹500 for 100 generations) once the daily limit is exhausted.
- **Decision Status:** `DECISION REQUIRED` (Recommended for Beta: Option A).
