# Elégance — Production Decision Register
**Document Status:** Complete / Awaiting User Sign-Off  
**Phase:** Phase 11B — Final Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Internal Architectural & Operational Governance Register  
**Release Candidate Baseline:** `7db3212fb4b43e507d60e81fd1e235b98bb87821` (Flyway `V040`)  

---

## 1. Executive Summary & Purpose

This Decision Register provides the authoritative record of all architectural, infrastructural, commercial, security, privacy, and operational decisions for the Elégance Interior Designer Platform.

Where decisions have been architected and implemented in code, their architectural contract is documented as **`LOCKED`**. Where operational parameters or vendor selections await user sign-off, they are explicitly designated as **`RECOMMENDED — WAITING USER APPROVAL`** or **`DECISION REQUIRED`**. Where features are postponed to minimize initial blast radius, they are designated as **`DEFERRED`**.

---

## 2. Decision Register Matrix

| ID | Domain | Area | Status | Authoritative Contract & Technical Recommendation |
| :--- | :--- | :--- | :--- | :--- |
| **PDR-001** | Infrastructure | Backend API Hosting | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: Railway** (Alternative: AWS ECS Fargate). Multi-stage Docker build ready (`apps/api/Dockerfile`). Native health check integration with `/api/v1/actuator/health/liveness` and `/readiness`. |
| **PDR-002** | Infrastructure | Frontend Web Hosting | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: Vercel** (Alternative: Railway Docker standalone). Next.js 16.3.8 Turbopack with standalone output. Remote image optimization strictly requires valid RFC 1123 hostname in `APP_CDN_HOSTNAME`. |
| **PDR-003** | Database | Production PostgreSQL Host & Version | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: PostgreSQL 17 on Railway Managed PostgreSQL** (Alternative: Supabase PG 17 / AWS RDS PG 17). Native RLS enforced across 59 tables. Exactly 40 Flyway migrations (`V001`-`V040`). |
| **PDR-004** | Storage | Cloud Object Storage | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: Cloudflare R2** (Alternative: AWS S3). 100% S3 API compatibility with `S3StorageService`, **zero ($0.00) egress fees**, dual bucket topology (`S3_BUCKET_PRIVATE`, `S3_BUCKET_PUBLIC`). |
| **PDR-005** | Media Delivery | CDN Hostname Mapping | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: Cloudflare CDN** via placeholder `<MEDIA_DOMAIN>`. Fulfills Next.js RFC 1123 `APP_CDN_HOSTNAME` constraint. |
| **PDR-006** | Domain | Production Domain Selection | `DECISION REQUIRED` | **Status: NOT SELECTED**. Desired routing: `https://<ROOT_DOMAIN>`, `https://api.<ROOT_DOMAIN>`, `https://media.<ROOT_DOMAIN>`. User must select and register root domain. |
| **PDR-007** | Authentication | Identity & Session Architecture | **LOCKED** | Stateful secure session tokens stored in PostgreSQL with SHA-256 hashing. Cookie name: `__Host-session`, Secure=true, HttpOnly=true, SameSite=Lax. Idle timeout: 1800s (30m), Absolute timeout: 43200s (12h). |
| **PDR-008** | Authentication | Primary OIDC Provider | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: Google OIDC First**. Generic OAuth 2.0 flow supported by `OidcService`. Secondary providers (Apple, GitHub) deferred. |
| **PDR-009** | Authentication | Admin MFA Requirement | **LOCKED** | RFC 6238 TOTP enforced for `SUPER_ADMIN` and `ADMIN`. Step-up challenge required for privileged operations within sliding 15-minute window. Master encryption key fails closed if missing. |
| **PDR-010** | Communications | Transactional Email Policy | `DEFERRED TO PUBLIC BETA` | `DisabledEmailProvider` active. Invitation flow creates secure copyable tokens directly in UI/admin. Email deferred to public beta to avoid domain warmup risks. |
| **PDR-011** | Artificial Intelligence | Generative AI Provider & Policy | `DEFERRED TO AI EVALUATION` | `DisabledAiImageProvider` active. Hard quota limit: 20 generations/studio/day locked. AI remains disabled during initial closed beta to protect spend. |
| **PDR-012** | Security | Upload Malware Scanning Policy | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: Option B for Closed Beta (Controlled/Trusted Beta)** $\rightarrow$ Option A (ClamAV container) required with `require-clean-scan=true` before Public Beta. |
| **PDR-013** | Commercial | Subscription Capacity Model | **LOCKED** | STANDARD: 10 projects, 15 photos/project, 5 GB storage, 0 Cinematic. PREMIUM: 20 projects, 25 photos/project, 20 GB storage, 0 Cinematic. PRO: 20 projects, 30 photos/project, 50 GB storage, max 5 Cinematic projects. |
| **PDR-014** | Commercial | Commercial Billing Gateway | `RECOMMENDED — WAITING USER APPROVAL` | `DisabledBillingProvider` active for closed beta. **Recommendation for Commercial Launch: Razorpay (India-First)** (Alternative: Stripe). |
| **PDR-015** | Commercial | Pricing Schedule | `DECISION REQUIRED` | Business leadership must provide monthly and annual rates for Standard, Premium, and Pro tiers in INR (`₹`). |
| **PDR-016** | Commercial | Primary Billing Currency | `RECOMMENDED — WAITING USER APPROVAL` | **Recommendation: INR (`₹`) PRIMARY** for India domestic launch. |
| **PDR-017** | Commercial | Billing Grace Period | `RECOMMENDED — WAITING USER APPROVAL` | Code default: 7 days (`gracePeriodDays = 7`). **Recommendation: Adopt 7-day default**. |
| **PDR-018** | Commercial | Upgrade Proration Policy | `DECISION REQUIRED` | Decision required on whether upgrade charges are immediately prorated or reset on upgrade date. |
| **PDR-019** | Commercial | Downgrade Non-Deletion Invariant | **LOCKED** | **Technical Invariant:** Downgrades **never delete** customer projects, photos, or media. Over-quota records enter safe read-only display status. Downgrades take effect strictly at period-end. |
| **PDR-020** | Commercial | Refund Policy | `DECISION REQUIRED` | Decision required on refund terms (No refunds vs 7-day money-back guarantee vs service credits). |
| **PDR-021** | Commercial | GST & Tax Treatment | `DECISION REQUIRED` | **GST / TAX CONFIGURATION REQUIRES QUALIFIED ACCOUNTING / LEGAL REVIEW**. SAC code and GST rates pending accountant sign-off. |
| **PDR-022** | Privacy | Privacy Export Retention (TTL) | `CONFIG DEFAULT — USER APPROVAL REQUIRED` | Application default: 7 days (`app.privacy.export-ttl-days=7`). User confirmation required. |
| **PDR-023** | Privacy | Account Deletion Grace Period | `DECISION REQUIRED` | Non-binding recommendation: 14 days before permanent purge. |
| **PDR-024** | Operations | Maintenance Mode Controls | **LOCKED** | Canonical states: `NORMAL` and `READ_ONLY` (blocks all mutating requests with HTTP 503 `MAINTENANCE_MODE_ACTIVE` for regular users; administrators restricted to control plane). |
| **PDR-025** | Governance | Privileged Mutation Auditing | **LOCKED** | Fail-closed administrative audit logging on table `audit_events`. Audit write failure aborts transaction and rolls back database state. |
| **PDR-026** | Disaster Recovery | Recovery Point Objective (RPO) | `DECISION REQUIRED` | **Recommendation: Option 1 (< 1 Hour with continuous WAL archiving / PITR)**. Alternative: Option 2 (24 Hours snapshot-only). |
| **PDR-027** | Disaster Recovery | Recovery Time Objective (RTO) | `DECISION REQUIRED` | **Recommendation: Option 1 (< 2 Hours automated restore)**. Alternative: Option 2 (< 8 Hours business-day restore). |
| **PDR-028** | Disaster Recovery | Database Backup Strategy | `RECOMMENDED — WAITING USER APPROVAL` | Automated daily managed database snapshots + PITR WAL archiving. Annual restoration verification drill. |
| **PDR-029** | Observability | Monitoring & Logging | `RECOMMENDED — WAITING USER APPROVAL` | Closed Beta: Hosting-native logs (Vercel & Railway) + Actuator `/actuator/health` probes. External APM deferred to commercial launch. |
| **PDR-030** | Launch Phasing | Multi-Tier Deployment Phasing | **LOCKED** | Tier 1 Closed Beta (Invitation-Only) $\rightarrow$ Tier 2 Public Beta $\rightarrow$ Tier 3 Commercial Launch. |

---

## 3. Governance Process for Phase 11 Resolution

Before any live cloud infrastructure commands are executed:
1. **User Sign-Off:** User reviews and records choices in [docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md](file:///c:/my%20projects/interior%20design/docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md).
2. **Account Provisioning:** Accounts registered for the approved providers.
3. **Execution Sequence:** Deployment commences strictly following the 15-step sequence in [docs/PHASE_11_PRODUCTION_DECISION_PACKAGE.md](file:///c:/my%20projects/interior%20design/docs/PHASE_11_PRODUCTION_DECISION_PACKAGE.md).
