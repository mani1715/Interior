# Elégance — Phase 11B Commercial Policy Decisions
**Document Status:** Complete / Awaiting User Sign-Off  
**Phase:** Phase 11B — Final Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Commercial & Financial Governance Specification  
**Release Candidate Baseline:** `7db3212fb4b43e507d60e81fd1e235b98bb87821` (Flyway `V040`)  

---

## 1. Locked Commercial Entitlement Model

The product capacity model is **LOCKED** in application code and database schema (`V030`, `V034`). No changes to these capacities are permitted under feature freeze.

| Metric / Entitlement | STANDARD | PREMIUM | PRO | BASE (Grandfathered) | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Maximum Active Projects** | **10** projects | **20** projects | **20** projects | 5 projects | **LOCKED** |
| **Portfolio Photos per Project** | **15** photos | **25** photos | **30** photos | 10 photos | **LOCKED** |
| **Total Cloud Storage Allocation** | **5 GB** | **20 GB** | **50 GB** | 2 GB | **LOCKED** |
| **Cinematic Presentations** | **0** (Disabled) | **0** (Disabled) | **Max 5** Active Projects | 0 | **LOCKED** |
| **High-Resolution Zoom & Reticle** | Included | Included | Included | Included | **LOCKED** |
| **Client Review Workflows** | Included | Included | Included | Included | **LOCKED** |
| **Studio Team Collaboration** | 1 Designer | Up to 3 Designers | Up to 10 Designers | 1 Designer | **LOCKED** |
| **Custom Domain Portfolio** | Subdomain only | Subdomain only | Custom domain enabled | Subdomain only | **LOCKED** |

---

## 2. Pricing Schedule Decision Worksheet

> **NOTICE:** Pricing is NOT hardcoded in application code. The table below represents a decision worksheet for business leadership. Cells marked `[ DECISION REQUIRED ]` must be approved before commercial launch.

| Plan | Billing Period | Target INR (₹) | Target USD ($) | Decision Status |
| :--- | :--- | :--- | :--- | :--- |
| **STANDARD** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **STANDARD** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PREMIUM** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PREMIUM** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PRO** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PRO** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |

---

## 3. Currency & Payment Methods

- **Primary Target Geography:** India Interior Design Studios (primary).
- **Primary Currency Recommendation:** **INR (`₹`) PRIMARY** for India-first commercial launch.
- **Status:** **RECOMMENDED — USER APPROVAL REQUIRED**.
- **Multi-Currency:** Architecture supports multi-currency settlement via gateway; international currencies (USD, EUR) remain deferred until international rollout.

---

## 4. Billing Periods

- **Supported Options in Architecture:** Monthly, Annual (Quarterly optional).
- **Recommendation:** **Monthly and Annual** (with ~15-20% discount on Annual plans).
- **Status:** **RECOMMENDED — USER APPROVAL REQUIRED**.

---

## 5. Billing Grace Period

- **Current Implementation Truth:**
  - Application configuration defines: `gracePeriodDays = 7` (field default in `BillingService.java`).
  - Webhook listener sets `gracePeriodEnd = now.plus(gracePeriodDays, ChronoUnit.DAYS)` on payment failure.
- **Commercial Policy Status:** **7-DAY CONFIG DEFAULT — USER BUSINESS APPROVAL REQUIRED**.

---

## 6. Upgrade Proration & Downgrade Behavior

### 6.1 Upgrade Proration Policy
- **Architectural Capability:** Gateway adapters can support immediate prorated charges or full new billing period resets.
- **Business Policy Status:** **UPGRADE PRORATION POLICY — DECISION REQUIRED**.
  - Option A: Immediate prorated charge for the remaining duration of the active cycle.
  - Option B: Full charge on upgrade date resetting the monthly billing anniversary.

### 6.2 Downgrade Invariant & Period-End Behavior (LOCKED)
- **Zero Data Deletion Guarantee (LOCKED INVARIANT):**
  - Downgrading **never deletes** customer projects, photographs, or studio media.
  - If a studio exceeds the new tier's quota (e.g., downgrading from Pro with 20 projects to Standard with a 10-project cap), all 20 existing projects remain publicly accessible and viewable.
  - Creation of new projects and new uploads is blocked until the studio either upgrades or archives projects below the new cap.
- **Period-End Timing (LOCKED INVARIANT):**
  - Downgrades take effect strictly at the **end of the active paid billing cycle** (`current_period_end`).
  - Handled via `requestDowngrade` in `BillingService.java`, which sets `downgradePlanId`, marks `cancelAtPeriodEnd = true`, and preserves full current plan entitlements until the period expires.

---

## 7. Refund Policy

- **Current Implementation:** Refund webhook SPI exists; zero refund policies are hardcoded in application logic.
- **Business Policy Status:** **DECISION REQUIRED**.
  - Option 1: Strict No-Refunds (except where required by law).
  - Option 2: 7-Day Money-Back Guarantee on initial subscription.
  - Option 3: Prorated Service Credits towards future billing.

---

## 8. GST & Tax Treatment (India Domestic)

- **Policy Status:** **GST / TAX CONFIGURATION REQUIRES QUALIFIED ACCOUNTING / LEGAL REVIEW**.
  - SAC Code (e.g. 998314) and GST rate (e.g. 18%) must be verified by the business accountant.
  - Invoicing details (Supplier GSTIN, Customer GSTIN collection, B2B tax invoice generation) require legal and accounting review.
  - Statutory invoice retention must follow qualified accounting counsel rather than assumptions.

---

## 9. Generative AI Commercial Policy

- **Technical Quota Truth:** Application code enforces a strict rate limit of **20 generations / studio / UTC calendar day** (`dailyStudioLimit = 20` in `AiVisualizerProperties.java`).
- **Commercial Policy Status:** **DEFERRED TO AI QUALITY EVALUATION**.
  - Generative AI is recommended to remain disabled for initial closed beta.
  - Quota options (included in Pro/Premium vs. credit pack add-on) will be finalized before public launch.
