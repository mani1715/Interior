# ELÉGANCE — PHASE 11 FINAL USER APPROVAL SHEET
**Document Status:** Ready for User Decision  
**Phase:** Phase 11B.1 — Production Decision Source-Truth Correction  
**Date:** 2026-10-13  
**Classification:** Executive & Operational Decision Sign-Off  
**Application State:** Release Candidate Ready | Flyway `V040` | Final UI/UX Design Freeze Active  
**Deployment Directive:** **DEPLOYMENT NOT PERFORMED** (Awaiting explicit user approvals below)

---

## Instructions for User Approval

Before any live cloud infrastructure is created or configured, review each decision below. Every decision displays the engineering **Recommendation**, the primary **Alternative**, and the technical **Rationale**.

Reply with your choice for each item (e.g., `1. Approve`, `2. Approve`, `3. Change to Supabase`, etc.).

---

## Section 1: Closed-Beta Core Infrastructure & Hosting

### 1. Frontend Hosting Platform
* **Recommendation:** **Vercel**
* **Alternative:** Railway (Docker standalone container)
* **Why:** First-party platform for Next.js, zero devops overhead, instant edge preview deploys, automatic edge caching.
* **Approve?** `[ ] YES (Vercel)  |  [ ] NO  |  [ ] CHANGE (Railway)`

---

### 2. Backend / API Hosting Platform
* **Recommendation:** **Railway**
* **Alternative:** AWS ECS Fargate
* **Why:** Simplest operational profile for beta, direct Docker runner (`apps/api/Dockerfile`), native private networking, automated health check integration (`/api/v1/actuator/health/liveness` and `/readiness`).
* **Approve?** `[ ] YES (Railway)  |  [ ] NO  |  [ ] CHANGE (AWS ECS)`

---

### 3. Database Provider
* **Recommendation:** **Railway PostgreSQL service**
* **Alternative:** Supabase PostgreSQL / AWS RDS / Neon
* **Why:** Low operational friction for beta, co-located within the same Railway private network as backend, simple provisioning.
* **Approve?** `[ ] YES (Railway PostgreSQL service)  |  [ ] NO  |  [ ] CHANGE (Supabase / AWS RDS)`

---

### 4. PostgreSQL Engine Version
* **Recommendation:** **PostgreSQL 17**
* **Alternative:** PostgreSQL 16
* **Why:** Modern engine performance, supported by all current drivers, fully compatible with all 40 Flyway migrations (`V001`-`V040`) and Row Level Security on 59 tables.
* **Approve?** `[ ] YES (PostgreSQL 17)  |  [ ] NO  |  [ ] CHANGE (PostgreSQL 16)`

---

### 5. Cloud Object Storage
* **Recommendation:** **Cloudflare R2**
* **Alternative:** AWS S3
* **Why:** S3-compatible API for the exact call set used by Elégance (`PutObject`, `GetObject`, `HeadObject`, `CopyObject`, `DeleteObject`, presigned `PUT`), with zero ($0.00) egress fees under Cloudflare's pricing model.
* **Approve?** `[ ] YES (Cloudflare R2)  |  [ ] NO  |  [ ] CHANGE (AWS S3)`

---

### 6. Primary Authentication Provider (Closed Beta)
* **Recommendation:** **Google OIDC**
* **Alternative:** Apple Sign-In / Email magic links
* **Why:** Lowest friction for design studio professionals; standard OAuth 2.0 flow natively supported in backend `OidcService`. Secondary providers deferred to public launch.
* **Approve?** `[ ] YES (Google OIDC)  |  [ ] NO  |  [ ] CHANGE`

---

### 7. Transactional Email for Closed Beta
* **Recommendation:** **Disabled for closed beta**
* **Alternative:** Provision Resend / Postmark / AWS SES immediately
* **Why:** In closed beta, studio invitations generate copyable secure tokens directly in the UI/admin console (`/invite/[rawToken]`). Keeping email disabled avoids DNS domain warmup and spam reputation risks until Public Beta.
* **Approve?** `[ ] YES (Disabled for Closed Beta)  |  [ ] NO  |  [ ] CHANGE`

---

### 8. Generative AI for Closed Beta
* **Recommendation:** **Disabled for closed beta**
* **Alternative:** Configure commercial AI provider immediately
* **Why:** Eliminates API spend exposure and third-party rate limits while evaluating interior diffusion model quality before client rollout.
* **Approve?** `[ ] YES (Disabled for Closed Beta)  |  [ ] NO  |  [ ] CHANGE`

---

### 9. Commercial Billing for Closed Beta
* **Recommendation:** **Disabled for closed beta**
* **Alternative:** Enable commercial checkout immediately
* **Why:** Closed beta studios receive complimentary access to test workflows. Commercial billing (Razorpay India-first / Stripe) deferred to commercial launch.
* **Approve?** `[ ] YES (Disabled for Closed Beta)  |  [ ] NO  |  [ ] CHANGE`

---

### 10. Upload Malware Scanner & Security Boundary
* **Source-Truth Audit Result:**
  * Interface: `MalwareScanner`
  * Implementations in Code: `DisabledMalwareScanner` (Implemented, reports unavailable), `FakeMalwareScanner` (Test/dev only).
  * Production scanner adapters (ClamAV, GuardDuty, VirusTotal): **0 IMPLEMENTED**.
  * Production invariant: `require-clean-scan=true` and **NO CLEAN SCAN $\rightarrow$ NO CANONICAL COMMIT**.
* **Policy Invariant:** Insecure bypass is **PROHIBITED**. Inviting a user does not make their uploaded files trusted.
* **Status:** **CLOSED-BETA MEDIA UPLOAD BLOCKER — PRODUCTION MALWARE SCANNER IMPLEMENTATION/CONFIGURATION REQUIRED** before normal user media uploads are enabled in production (unless closed-beta uploads remain restricted to pre-scanned administrator fixtures).
* **Decision:**
  * Option 1: Implement & deploy containerized ClamAV scanner adapter before enabling closed-beta media uploads.
  * Option 2: Restrict closed-beta testing to non-upload workflows (portfolio builder with fixtures, navigation, reviews, CRM) until ClamAV adapter is wired.
* **Your Decision:** `[ ] OPTION 1 (Deploy ClamAV Adapter)  |  [ ] OPTION 2 (Restrict Uploads to Fixtures)`

---

### 11. Database Automated Backups
* **Requirement:** Scheduled database/volume backups must be **explicitly enabled and verified** before storing real customer data.
* **Recommendation:** **Enable scheduled backups before beta launch**.
* **Approve?** `[ ] YES  |  [ ] NO  |  [ ] CHANGE`

---

### 12. Point-In-Time Recovery (PITR)
* **Requirement:** PostgreSQL continuous WAL archiving and Point-In-Time Recovery must be **explicitly enabled and verified** if selected.
* **Recommendation:** **Enable PITR before storing real beta data**.
* **Approve?** `[ ] YES  |  [ ] NO  |  [ ] CHANGE`

---

### 13. Observability & Monitoring for Closed Beta
* **Recommendation:** **Hosting-native initial health monitoring** (`/api/v1/actuator/health` + hosting uptime alerts).
* **Alternative:** External APM (Prometheus + Grafana / Datadog)
* **Why:** Sufficient for initial closed beta; minimizes operational complexity.
* **Approve?** `[ ] YES (Hosting-Native)  |  [ ] NO  |  [ ] CHANGE`

---

### 14. Centralized Logging for Closed Beta
* **Recommendation:** **Hosting-native logs for beta** (Vercel deployment logs + Railway container logs).
* **Alternative:** External centralized logging (BetterStack / Papertrail)
* **Why:** Avoids third-party log infrastructure setup during beta; structured MDC request logging already active.
* **Approve?** `[ ] YES (Hosting-Native)  |  [ ] NO  |  [ ] CHANGE`

---

### 15. Production Domain Selection
* **Status:** **USER ENTRY REQUIRED**
* **Desired Routing Structure:**
  * Web App: `https://<ROOT_DOMAIN>`
  * Backend API: `https://api.<ROOT_DOMAIN>`
  * Media CDN: `https://media.<ROOT_DOMAIN>`
* **Your Domain Entry:** `________________________________________`

---

## Section 2: Commercial & Governance Decisions (Deferred / Unapproved)

### 16. Team Member / Seat Limit Policy
* **Status:** **DECISION REQUIRED / DEFERRED**
* **Source Truth:** Current database schema and `EntitlementKey` enum contain **NO team seat limits**. The plan models currently enforce project, photo, and storage capacity only.
* **Decision:** Whether commercial monetization will cap team members per studio (e.g. Standard: 1, Premium: 3, Pro: 10, or unconstrained).
* **Your Decision:** `[ ] DEFER TO COMMERCIAL LAUNCH  |  [ ] SPECIFY LIMITS: ________________`

---

### 17. Commercial Subscription Pricing & Policies
* **Status:** **DEFERRED TO COMMERCIAL LAUNCH**
* **Items Requiring Eventual User Sign-Off:**
  * Standard, Premium, and Pro monthly/annual rates in INR (`₹`).
  * Billing periods (Monthly + Annual).
  * Primary currency (INR `₹` India-first).
  * Grace period policy (7-day config default).
  * Upgrade proration policy.
  * Refund policy.
  * GST / tax compliance review.
  * Contractual RPO & RTO SLAs (Closed beta operates with **NO CONTRACTUAL SLA**).
  * Data retention schedules.
