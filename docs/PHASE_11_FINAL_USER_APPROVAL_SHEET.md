# ELÉGANCE — PHASE 11 FINAL USER APPROVAL SHEET
**Document Status:** Ready for User Decision  
**Phase:** Phase 11B — Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Executive & Operational Decision Sign-Off  
**Application State:** Release Candidate Ready | Flyway `V040` | Final UI/UX Design Freeze Active  
**Deployment Directive:** **DEPLOYMENT NOT PERFORMED** (Awaiting explicit user approvals below)

---

## Instructions for User Approval

Before any live cloud infrastructure is created or configured, review each decision below. Every decision displays the engineering **Recommendation**, the primary **Alternative**, and the technical **Rationale**.

Reply with your choice for each item (e.g., `1. Approve`, `2. Approve`, `3. Change to Supabase`, etc.).

---

## Section 1: Closed-Beta Infrastructure & Hosting

### 1. Frontend Hosting Platform
* **Decision Required:** Where to host the Next.js 16.3.8 Turbopack web application (`apps/web`).
* **Recommendation:** **Vercel**
* **Alternative:** **Railway** (Docker standalone container)
* **Why:** First-party platform for Next.js, zero devops overhead, instant edge preview deploys, automatic edge caching and image optimization.
* **Your Decision:** `[ ] APPROVE (Vercel)  |  [ ] CHANGE (Railway)  |  [ ] OTHER`

---

### 2. Backend / API Hosting Platform
* **Decision Required:** Where to host the Spring Boot 3.4.3 / Java 21 Dockerized API (`apps/api/Dockerfile`).
* **Recommendation:** **Railway**
* **Alternative:** **AWS ECS Fargate**
* **Why:** Simplest operational profile for beta, direct Docker support, native private networking to PostgreSQL, automated health probes (`/actuator/health/liveness` and `/readiness`), instant rollbacks, and predictable pricing.
* **Your Decision:** `[ ] APPROVE (Railway)  |  [ ] CHANGE (AWS ECS)  |  [ ] OTHER`

---

### 3. Managed PostgreSQL Database Provider & Version
* **Decision Required:** Which managed cloud database provider and engine version to provision.
* **Recommendation:** **Railway Managed PostgreSQL 17** (or **Supabase PostgreSQL 17**)
* **Alternative:** **AWS RDS PostgreSQL 17** / **Neon**
* **Why:** PostgreSQL 17 provides maximum modern SQL performance and long-term support; Railway/Supabase offer instant provisioning, zero VPC peering friction, automated daily backups, and low operational cost during beta.
* **Your Decision:** `[ ] APPROVE (Railway PG 17)  |  [ ] CHANGE (Supabase / RDS)  |  [ ] OTHER`

---

### 4. Object Storage (Media, Original Photographs, Quarantine)
* **Decision Required:** S3-compatible cloud storage provider for portfolio photography and media assets.
* **Recommendation:** **Cloudflare R2**
* **Alternative:** **AWS S3**
* **Why:** 100% S3-compatible with AWS SDK v2 (`endpointOverride`), zero ($0.00) egress bandwidth fees (eliminating bandwidth bills for high-resolution interior photography portfolios), and global low-latency distribution.
* **Your Decision:** `[ ] APPROVE (Cloudflare R2)  |  [ ] CHANGE (AWS S3)  |  [ ] OTHER`

---

### 5. Media CDN Delivery & Hostname
* **Decision Required:** Hostname mapping and CDN distribution for public portfolio images.
* **Recommendation:** **Cloudflare CDN** via placeholder `media.<ROOT_DOMAIN>` mapped to `APP_CDN_HOSTNAME`.
* **Alternative:** AWS CloudFront
* **Why:** Couples natively with Cloudflare R2, provides global edge caching, and satisfies Next.js 16.3.8 strict RFC 1123 image optimization requirements.
* **Your Decision:** `[ ] APPROVE (Cloudflare CDN)  |  [ ] CHANGE (CloudFront)  |  [ ] OTHER`

---

### 6. Production Domain Selection
* **Decision Required:** The actual registered root domain for the platform (replaces placeholder `<ROOT_DOMAIN>`).
* **Current State:** **NOT SELECTED** (e.g., `elegance.design`, `elegancestudio.in`, or your chosen brand domain).
* **Desired Routing Structure:**
  * Web App: `https://<ROOT_DOMAIN>`
  * Backend API: `https://api.<ROOT_DOMAIN>`
  * Media CDN: `https://media.<ROOT_DOMAIN>`
* **Your Domain Entry:** `________________________________________`

---

### 7. Primary Authentication Provider (Closed Beta)
* **Decision Required:** Initial OIDC identity provider for studio owner sign-in.
* **Recommendation:** **Google OIDC first**
* **Alternative:** Add Apple OIDC / Email magic links
* **Why:** Lowest friction for design studio professionals; standard OAuth 2.0 flow natively supported in backend `OidcService`. Secondary providers (Apple, etc.) can be added for public launch.
* **Your Decision:** `[ ] APPROVE (Google OIDC First)  |  [ ] CHANGE  |  [ ] OTHER`

---

## Section 2: Subsystem Gates & Security Boundaries

### 8. Transactional Email Policy for Closed Beta
* **Decision Required:** How to handle invitations and transactional email during invitation-only closed beta.
* **Recommendation:** **DEFER TO PUBLIC BETA (Remain DISABLED for closed beta)**
* **Alternative:** Provision Resend / Postmark / AWS SES adapter immediately before beta.
* **Why:** In closed beta, studio invitations generate copyable secure invitation tokens directly in the admin UI and designer workspace. Keeping email disabled avoids DNS domain warmup, SPF/DKIM/DMARC provisioning delays, and spam reputation risks until public beta.
* **Your Decision:** `[ ] APPROVE (Keep Disabled for Beta)  |  [ ] CHANGE (Provision Resend/SES now)`

---

### 9. Generative AI Policy for Closed Beta
* **Decision Required:** Whether generative interior AI visualization is active in initial closed beta.
* **Recommendation:** **DEFER TO AI QUALITY EVALUATION (Keep AI DISABLED initially)**
* **Alternative:** Configure commercial AI provider (e.g. Stability / Replicate / Vertex) immediately.
* **Why:** Eliminates API spend exposure, protects closed beta from third-party rate limits, and allows thorough evaluation of interior diffusion model quality before enabling the feature for clients.
* **Your Decision:** `[ ] APPROVE (Keep AI Disabled for Beta)  |  [ ] CHANGE (Configure Provider now)`

---

### 10. Upload Malware Scanning & Security Boundary
* **Decision Required:** Upload inspection policy for initial beta participants.
* **Option A (Strict):** Deploy dedicated ClamAV container or external virus scanning service before accepting any uploads (`require-clean-scan=true`).
* **Option B (Controlled Beta):** Restrict upload access strictly to verified, invited studio participants with `require-clean-scan=false` initially, enforcing fail-closed clean scanning upon Public Beta.
* **Recommendation:** **Option B for Closed Beta (with trusted participants) $\rightarrow$ Option A required before Public Beta**.
* **Your Decision:** `[ ] APPROVE OPTION B (Controlled Beta)  |  [ ] SELECT OPTION A (Deploy Scanner Now)`

---

### 11. Commercial Billing & Payment Gateway
* **Decision Required:** Commercial billing status for beta and gateway selection for commercial launch.
* **Recommendation:** **Keep Billing DISABLED for Closed Beta $\rightarrow$ Launch India-First with Razorpay for Commercial Launch**.
* **Alternative:** Stripe
* **Why:** Closed beta studios receive complimentary access to test workflows. For Indian commercial launch, Razorpay natively supports UPI, domestic cards, NetBanking, and RBI-mandated e-mandate recurring debits with GST invoicing.
* **Your Decision:** `[ ] APPROVE (Disabled for Beta, Razorpay for Launch)  |  [ ] CHANGE (Stripe)  |  [ ] OTHER`

---

## Section 3: Commercial & Governance Decisions

### 12. Commercial Subscription Pricing Schedule
* **Decision Required:** Monthly and Annual pricing for commercial launch (in INR `₹`).
* **Locked Capacities:**
  * STANDARD: 10 projects, 15 photos/project, 5 GB storage, 0 Cinematic
  * PREMIUM: 20 projects, 25 photos/project, 20 GB storage, 0 Cinematic
  * PRO: 20 projects, 30 photos/project, 50 GB storage, max 5 Cinematic projects
* **Pricing Worksheet (Please Enter Values):**
  * STANDARD Monthly: `₹ _______` | Annual: `₹ _______`
  * PREMIUM Monthly: `₹ _______` | Annual: `₹ _______`
  * PRO Monthly: `₹ _______` | Annual: `₹ _______`

---

### 13. Billing Grace Period Policy
* **Decision Required:** Number of days studios retain access following an invoice payment failure.
* **Recommendation:** **7-Day Config Default** (Matches current application configuration).
* **Alternative:** 0-day instant suspension, or 14-day generous grace.
* **Your Decision:** `[ ] APPROVE (7 Days)  |  [ ] CHANGE (____ Days)`

---

### 14. Upgrade Proration & Downgrade Behavior
* **Technical Invariant (LOCKED):** Downgrades NEVER delete customer projects or photographs. Over-quota assets remain preserved in read-only state.
* **Policy Decision Required:**
  * Upgrade: Apply immediate pro-rata credit towards higher plan (`YES / NO`)?
  * Downgrade: Take effect at current billing period end (`YES / NO`)?
* **Recommendation:** **Immediate prorated upgrade; end-of-period downgrade**.
* **Your Decision:** `[ ] APPROVE (Prorated upgrade / Period-end downgrade)  |  [ ] OTHER`

---

### 15. Privacy & Data Retention Schedules
* **Privacy Export TTL:** Config default is 7 days (`app.privacy.export-ttl-days=7`). Approve? `[ ] 7 Days (Approved)  |  [ ] Other: ____`
* **Account Deletion Grace Period:** Config default is 14 days before permanent purge. Approve? `[ ] 14 Days (Approved)  |  [ ] Other: ____`
* **Audit Ledger Retention:** Immutable administrative audit logs. Recommendation: 3 years. Approve? `[ ] 3 Years  |  [ ] Other: ____`
* **Support Ticket Retention:** Closed customer support tickets. Recommendation: 2 years. Approve? `[ ] 2 Years  |  [ ] Other: ____`

---

### 16. Disaster Recovery Objectives (RPO & RTO)
* **Recovery Point Objective (RPO):** Maximum acceptable data loss window in a disaster.
  * *Option 1:* < 1 Hour (Requires continuous WAL archiving / PITR enabled on database).
  * *Option 2:* Up to 24 Hours (Daily snapshot backup only; simpler, lower cost).
  * **Recommendation:** **Option 1 (< 1 Hour with PITR)**.
  * **Your Decision:** `[ ] Option 1 (< 1 Hour)  |  [ ] Option 2 (Daily Snapshot)`
* **Recovery Time Objective (RTO):** Target time to restore service after catastrophic failure.
  * *Option 1:* < 2 Hours (Automated container redeploy + point-in-time database restore).
  * *Option 2:* < 8 Hours (Standard operational business-hours restore).
  * **Recommendation:** **Option 1 (< 2 Hours)**.
  * **Your Decision:** `[ ] Option 1 (< 2 Hours)  |  [ ] Option 2 (< 8 Hours)`

---

### 17. Monitoring & Logging Infrastructure
* **Closed Beta Logging:** Use native hosting logs (Vercel deployment logs + Railway container logs). Approve? `[ ] YES  |  [ ] NO`
* **Uptime Monitoring:** External lightweight uptime check pinging `/actuator/health` and `/health`. Recommendation: BetterStack / UptimeRobot. Approve? `[ ] YES  |  [ ] NO`

---

## Sign-Off Summary

Once you have reviewed and indicated your decisions on the items above, submit your choices. No cloud commands, resource provisioning, or code changes will be executed until your decisions are recorded.
