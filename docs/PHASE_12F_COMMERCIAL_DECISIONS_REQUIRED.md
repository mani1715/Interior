# Elégance — Phase 12F: Commercial Decisions Required & Business Policy Register

**Status:** PENDING EXPLICIT BUSINESS APPROVAL  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12F Architecture & Governance  

---

## 1. Mandatory Commercial Source-Truth Notice

> [!IMPORTANT]
> Technical architecture, database schemas, cryptographic signature validation, and API contracts are fully production-ready.
> However, per the **Critical Commercial Source-Truth Rule**, the engineering team **MUST NOT INVENT OR LOCK COMMERCIAL POLICIES**.
> Configuration defaults or placeholder values present in database migration scripts (e.g. ₹49.00 / ₹99.00 / ₹199.00) are **technical seed baselines only** and do not represent approved business policy.
> All items below remain strictly **DECISION REQUIRED** or **DEFERRED** pending user / commercial executive approval.

---

## 2. Register of Unresolved Commercial Decisions

| Item # | Commercial Decision Area | Current Technical Baseline | Status | Stakeholder Decision Required |
|:---:|:---|:---|:---:|:---|
| **1** | **Subscription Pricing Tiers** | `STANDARD`: ₹49/mo (4900 paise)<br>`PREMIUM`: ₹99/mo (9900 paise)<br>`PRO`: ₹199/mo (19900 paise) | **DECISION REQUIRED** | Finalize market pricing for Indian & international interior design studios. |
| **2** | **Billing Currency Strategy** | `INR` default in schema | **DECISION REQUIRED** | Choose between single-currency (`INR` only), geo-IP routed dual-currency (`INR` domestic / `USD` international), or full multi-currency. |
| **3** | **Billing Periods Available** | `MONTHLY` only in active catalog | **DECISION REQUIRED** | Determine if Annual plans (with discount, e.g. 2 months free) will be offered at launch. |
| **4** | **Team-Seat Limits per Studio** | Unbounded in V030 (all studios can invite members) | **DECISION REQUIRED** | Decide whether team size is unlimited or tiered (e.g. STANDARD: 2 seats, PREMIUM: 5 seats, PRO: 15 seats). |
| **5** | **Free Trial Policy** | No automated trial; fallback to `BASE` plan | **DECISION REQUIRED** | Determine if a 14-day or 30-day free trial with credit-card-upfront or no-card-required will be offered. |
| **6** | **Mid-Cycle Upgrade / Proration** | Instant upgrade; resets billing cycle | **DECISION REQUIRED** | Define formal proration formula: immediate proportional charge vs. credit applied to next invoice. |
| **7** | **Refund & Cancellation Policy** | Period-end cancellation; no automated refunds | **DECISION REQUIRED** | Approve written refund terms: strictly non-refundable, 7-day money-back guarantee, or prorated unused time. |
| **8** | **Payment Failure Grace Period** | 7 days (`PAST_DUE`), full access maintained | **DECISION REQUIRED** | Approve length of grace window (e.g. 7 vs 14 days) and functional degradation rules during past-due state. |
| **9** | **GST & Indirect Tax Handling** | Flat minor currency amount (tax inclusive assumed) | **DECISION REQUIRED** | Determine if prices are tax-inclusive or tax-exclusive (+18% GST), and configure GSTIN capture for B2B input credit. |
| **10** | **Invoice Numbering & Legal Compliance** | Auto-generated UUID / provider receipt URL | **DECISION REQUIRED** | Define sequential financial invoice numbering scheme (e.g. `ELG-FY26-XXXX`) compliant with local tax authorities. |
| **11** | **Discount & Coupon Policy** | Not implemented in database catalog | **DEFERRED** | Decide promotional coupon codes, introductory designer launch discounts, and referral credits. |
| **12** | **Commercial SLA, RPO & RTO Commitments**| RPO < 5 min, RTO < 15 min (infrastructure defaults) | **DECISION REQUIRED** | Formally approve customer-facing uptime SLA (e.g. 99.9%) and priority support commitments for PRO studios. |

---

## 3. Product Entitlements Locked vs. Unlocked Summary

### 3.1 Locked Technical Capacities (Established in V030 & Architecture Baseline)
- **STANDARD:** 10 projects, 15 photos/project, 5 GB storage, 0 Cinematic projects, Leads CRM enabled, Portfolio Publish enabled, Basic Analytics enabled.
- **PREMIUM:** 20 projects, 25 photos/project, 20 GB storage, 0 Cinematic projects, Leads CRM enabled, Portfolio Publish enabled, Advanced Analytics enabled.
- **PRO:** 20 projects, 30 photos/project, 50 GB storage, 5 Cinematic projects, Leads CRM enabled, Portfolio Publish enabled, Advanced Analytics enabled.
- **BASE:** Internal/legacy fallback (`purchasable = false`, `priceMinor = 0`).
- **Downgrade Invariant:** Zero data loss. Over-quota existing projects and photos are NEVER deleted upon plan downgrade or expiration.

### 3.2 Intentionally Unlocked Items (Awaiting Business Decision)
- Subscription monetary prices and currencies.
- Team member seat limits.
- Refund policy window.
- Tax/GST presentation policy.
