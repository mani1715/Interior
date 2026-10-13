# Elégance — Phase 11B Final Production Decision Package
**Document Status:** Complete / Awaiting User Sign-Off  
**Phase:** Phase 11B — Final Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Authoritative Production Decision & Governance Specification  
**Release Candidate Baseline:** `7db3212fb4b43e507d60e81fd1e235b98bb87821` (Flyway `V040`)  
**UI/UX Design Freeze:** **ACTIVE & LOCKED**  
**Application Feature Freeze:** **ACTIVE & LOCKED**  

---

## 1. Executive Summary & Purpose

The Elégance Interior Designer Platform has completed all 5 UI/UX quality passes and 100% of functional verification suites (52 route handlers, 52 Vitest test files with 373 passing tests, TypeScript and ESLint with 0 errors/warnings). Application feature freeze and UI/UX design freeze are strictly active.

**CRITICAL DEPLOYMENT DIRECTIVE:**
> **DEPLOYMENT NOT PERFORMED.** No cloud infrastructure, databases, storage buckets, domains, DNS records, or external vendor integrations have been provisioned or altered. This phase exists solely to provide the definitive, source-verified production configuration that the user must review and approve before Phase 11 deployment begins.

This package clearly separates:
1. **`LOCKED`**: Hardened in code, tests, and database schema; immutable under freeze.
2. **`RECOMMENDED — WAITING USER APPROVAL`**: Engineering recommendations based on operational simplicity, low cost, and architectural compatibility; awaits user approval.
3. **`DEFERRED`**: Postponed to Public Beta or General Commercial Launch to minimize initial blast radius.

---

## 2. Decision Status Standard

Every item in this specification belongs to one of three strict categories:
- **`LOCKED`**: Architected, implemented, and verified in code; immutable under feature freeze.
- **`RECOMMENDED — WAITING USER APPROVAL`**: Technical recommendation based on architecture, security, and low operational complexity; requires user confirmation.
- **`DEFERRED`**: Intentionally postponed to Public Beta, Commercial Launch, or specialized evaluation.

---

## 3. Authoritative Closed-Beta Deployment Profile (Minimal Recommendation)

For an invitation-only closed beta, the recommended minimal profile is:

| Area | Recommended Choice | Status | Rationale & Code Truth |
| :--- | :--- | :--- | :--- |
| **Frontend Hosting** | **Vercel** | RECOMMENDED — WAITING USER APPROVAL | Native Next.js 16.3.8 Turbopack support, automatic edge caching, zero devops. Alternative: Railway. |
| **Backend API Hosting** | **Railway** | RECOMMENDED — WAITING USER APPROVAL | Direct Spring Boot Java 21 Docker runner, native private networking to PostgreSQL, built-in health probes (`/actuator/health`). Alternative: AWS ECS Fargate. |
| **Database Provider** | **Railway Managed PostgreSQL** (or **Supabase**) | RECOMMENDED — WAITING USER APPROVAL | Instant provisioning, zero VPC peering friction, automated daily backups, low operational cost. Alternative: AWS RDS PG 17 / Neon. |
| **PostgreSQL Version** | **PostgreSQL 17** | RECOMMENDED — WAITING USER APPROVAL | Supported: 15+; Locally tested: 18.3; Flyway 10.20 certified: 16/17. PG 17 recommended for maximum performance. |
| **Object Storage** | **Cloudflare R2** | RECOMMENDED — WAITING USER APPROVAL | 100% S3 API compatibility with `S3StorageService`, **zero ($0.00) egress fees**, native Cloudflare global edge caching. Alternative: AWS S3. |
| **Media CDN Hostname** | `<MEDIA_DOMAIN>` (e.g. `media.<ROOT_DOMAIN>`) | RECOMMENDED — WAITING USER APPROVAL | Cloudflare CDN edge mapped to public R2 bucket; strictly satisfies Next.js RFC 1123 `APP_CDN_HOSTNAME` requirement. |
| **Production Domain** | **`<ROOT_DOMAIN>` (Not Selected)** | USER SELECTION REQUIRED | Root domain remains unselected. Desired routing: `https://<ROOT_DOMAIN>`, `https://api.<ROOT_DOMAIN>`, `https://media.<ROOT_DOMAIN>`. |
| **Authentication (OIDC)** | **Google OIDC First** | RECOMMENDED — WAITING USER APPROVAL | Supported via generic `OidcService`. Lowest onboarding friction for interior design studios. Secondary providers (Apple, etc.) deferred. |
| **Transactional Email** | **DISABLED** (`DisabledEmailProvider`) | DEFERRED TO PUBLIC BETA | Closed beta invitations operate via direct secure tokens generated in UI/admin. Avoids email reputation and domain warmup risks during beta. |
| **Generative AI** | **DISABLED** (`DisabledAiImageProvider`) | DEFERRED TO AI QUALITY EVALUATION | Avoids early third-party API spend and model rate limits during initial workflow testing. |
| **Upload Malware Scanning** | **Controlled Closed Beta (Option B)** | RECOMMENDED — WAITING USER APPROVAL | Restrict upload permissions to controlled, trusted beta participants initially; full fail-closed scanning (`require-clean-scan=true`) mandatory before Public Beta. |
| **Commercial Billing** | **DISABLED** (`DisabledBillingProvider`) | DEFERRED TO COMMERCIAL LAUNCH | Beta studios receive complimentary access to test workflows. For commercial launch, India-first **Razorpay** recommended (Alternative: Stripe). |
| **Monitoring & Uptime** | **Hosting-Native Checks** | RECOMMENDED — WAITING USER APPROVAL | Platform `/actuator/health` and `/health` endpoints + hosting-native uptime alarms. Dedicated APM deferred. |
| **Centralized Logging** | **Hosting-Native Logs** | RECOMMENDED — WAITING USER APPROVAL | Vercel and Railway native log streams are sufficient for closed beta. External centralized retention deferred. |
| **Database Backups** | **Managed Automated Backups + PITR** | RECOMMENDED — WAITING USER APPROVAL | Automated daily snapshots + WAL continuous archiving. RPO < 1 hour recommended. |

---

## 4. Comprehensive Architectural Evaluation

### 4.1 Frontend Decision: Vercel vs. Railway
* **Application Baseline:** Next.js 16.3.8 Turbopack, standalone output (`next.config.js` sets `output: 'standalone'`).
* **Evaluation:**
  * **Vercel (RECOMMENDED):**
    * *Pros:* Native Next.js first-party platform, zero server management, automatic edge asset caching, instant preview URLs, built-in edge performance insights.
    * *Cons:* Bandwidth pricing at high commercial scale.
  * **Railway (ALTERNATIVE):**
    * *Pros:* Can build and run `apps/web/Dockerfile` standalone container, unified dashboard and billing alongside backend and database.
    * *Cons:* Requires manual CDN configuration to achieve edge caching equivalent to Vercel.
* **Output:** RECOMMENDED: **Vercel** | ALTERNATIVE: **Railway** | **USER APPROVAL REQUIRED**.

### 4.2 Backend Decision: Railway vs. AWS ECS Fargate
* **Application Baseline:** Spring Boot 3.4.3 on Java 21, multi-stage Docker container (`apps/api/Dockerfile`), embedded Tomcat on port 8080, context path `/api/v1`.
* **Evaluation:**
  * **Railway (RECOMMENDED for Beta):**
    * *Pros:* Native Docker support, automatic health check integration with `/api/v1/actuator/health/liveness` and `/readiness`, zero VPC setup, simple secret injection, predictable monthly costs, instant rollbacks.
    * *Cons:* Fixed cloud regions.
  * **AWS ECS Fargate (ALTERNATIVE - Enterprise Standard):**
    * *Pros:* Multi-AZ high availability, deep AWS IAM governance, enterprise VPC isolation.
    * *Cons:* High operational setup overhead (ALB, target groups, VPC endpoints, Route 53, Secrets Manager).
* **Output:** RECOMMENDED: **Railway** | ALTERNATIVE: **AWS ECS Fargate** | **USER APPROVAL REQUIRED**.

### 4.3 Database Provider & PostgreSQL Version
* **Source Truth:**
  * Minimum supported: PostgreSQL 15.
  * Locally tested & verified: PostgreSQL 18.3.
  * Flyway 10.20 certified & recommended: **PostgreSQL 16 or 17**.
  * Exactly 40 Flyway migrations (`V001` through `V040`).
  * Row Level Security (RLS) active on all 59 entity tables.
* **Provider Evaluation:**
  * **Railway Managed PostgreSQL (RECOMMENDED for Beta):** Provisioned in same project network as backend, zero egress latency, automated daily backups.
  * **Supabase PostgreSQL (ALTERNATIVE A):** Managed PostgreSQL 17 with Supavisor connection pooling and WAL archiving.
  * **Neon (ALTERNATIVE B):** Serverless PostgreSQL 17 with instant branching.
  * **AWS RDS PostgreSQL (ALTERNATIVE C - Enterprise):** Multi-AZ PostgreSQL 17 with 35-day automated PITR.
* **Output:** RECOMMENDED: **PostgreSQL 17** on **Railway Managed PostgreSQL** (or **Supabase**) | **USER APPROVAL REQUIRED**.

### 4.4 Object Storage: Cloudflare R2 vs. AWS S3
* **Application Baseline:** S3-compatible adapter (`S3StorageService`) using AWS Java SDK v2 (`S3Client`, `S3Presigner`) with `endpointOverride`.
* **Requirements:**
  * Private bucket (`S3_BUCKET_PRIVATE`) for original uploads, quarantine objects, and GDPR privacy export bundles.
  * Public bucket (`S3_BUCKET_PUBLIC`) for clean derivative web assets.
  * Presigned PUT requests, content-type verification, metadata attachment, byte streaming, and deletion.
* **Evaluation:**
  * **Cloudflare R2 (RECOMMENDED):** Zero ($0.00) egress fees. Portfolio platforms host heavy high-resolution photography; zero egress saves significant recurring costs. 100% S3 API compatibility.
  * **AWS S3 (ALTERNATIVE):** Industry standard, but incurs per-GB egress fees.
* **Output:** RECOMMENDED: **Cloudflare R2** | ALTERNATIVE: **AWS S3** | **USER APPROVAL REQUIRED**.

### 4.5 CDN & Media Delivery
* **Application Baseline:** Next.js `next.config.js` enforces strict RFC 1123 hostname matching via `APP_CDN_HOSTNAME` / `NEXT_PUBLIC_CDN_HOSTNAME`. Arbitrary wildcards (`*`) are prohibited.
* **Recommendation:** Cloudflare CDN edge mapped to the public R2 bucket.
* **Placeholder FQDN:** `<MEDIA_DOMAIN>` (e.g. `media.<ROOT_DOMAIN>`).
* **Output:** RECOMMENDED: **Cloudflare CDN** | **USER APPROVAL REQUIRED**.

### 4.6 Production Domain Structure
* **Status:** **NOT SELECTED** (User must supply actual domain name).
* **Desired Routing Hierarchy:**
  * Web Application (Apex / WWW): `https://<ROOT_DOMAIN>`
  * Backend API Ingress: `https://api.<ROOT_DOMAIN>`
  * Media Delivery CDN: `https://media.<ROOT_DOMAIN>`
  * OIDC Callback Redirect: `https://<ROOT_DOMAIN>/api/v1/auth/oidc/callback/<provider>`
* **Rule:** Placeholders only; no hardcoded domain until user registration and approval.

### 4.7 Identity & OIDC Provider
* **Application Baseline:** Generic OIDC authorization code flow with PKCE (`OidcService`).
* **Recommendation:** **Google OIDC first** for closed beta. Lowest friction for design studios.
* **Secondary Providers:** Apple, GitHub, Clerk remain deferred.
* **Output:** RECOMMENDED: **Google OIDC First** | **USER APPROVAL REQUIRED**.

### 4.8 Transactional Email Policy
* **Source Truth:** Backend implements `DisabledEmailProvider` and `FakeEmailProvider` (test-only). No third-party provider adapter exists in code.
* **Closed-Beta Strategy:** Invitation flow creates unique tokens. Studio owners can copy invitation links directly from the UI/admin console. Email can safely remain **DISABLED**.
* **Recommendation:** **DEFER TO PUBLIC BETA**. Avoids DNS warmup, SPF/DKIM/DMARC provisioning, and spam reputation management during beta.
* **Public Beta Strategy:** Select and configure Resend, Postmark, or AWS SES before public onboarding.

### 4.9 Generative AI Policy
* **Source Truth:** `ConfigurableAiImageProvider` throws `AiProviderNotConfiguredException` when unconfigured. `DisabledAiImageProvider` safely handles unconfigured state. Daily studio limit hard-capped at 20 generations (`AiVisualizerProperties.dailyStudioLimit = 20`).
* **Closed-Beta Strategy:** Keep AI **DISABLED** (`interior.ai.enabled=false` or `provider=none`).
* **Recommendation:** **DEFER TO AI QUALITY EVALUATION**. Conduct model evaluation on sample architectural spaces before incurring vendor API expenses.

### 4.10 Upload Malware Scanning & Security Boundary
* **Source Truth:** `app.media.security.require-clean-scan=${REQUIRE_CLEAN_SCAN:true}` and `app.media.security.scanner=${MALWARE_SCANNER_PROVIDER:disabled}`.
* **Security Invariant:** In production with `require-clean-scan=true`, if no scanner is configured, upload security **fails closed** (`IllegalStateException: MALWARE_SCANNER_NOT_CONFIGURED`).
* **Policy Options:**
  * **Option A:** Deploy ClamAV daemon container before accepting any uploads (`require-clean-scan=true`).
  * **Option B:** Restrict uploads to controlled, trusted beta studio participants initially with `require-clean-scan=false`, and enforce fail-closed scanning upon Public Beta.
* **Recommendation:** **Option B for Closed Beta $\rightarrow$ Option A required before Public Beta**.

### 4.11 Commercial Payment Gateway
* **Source Truth:** Backend implements `DisabledBillingProvider`. Commercial checkout throws `IllegalStateException` when unconfigured.
* **Closed Beta:** Billing remains **DISABLED** (complimentary beta access).
* **Commercial Launch Recommendation:** For India-first market, **Razorpay** is recommended (native UPI, RuPay, domestic debit/credit, NetBanking, and RBI e-mandate recurring debits with GST invoicing). Alternative: **Stripe**.
* **Output:** RECOMMENDED: **Razorpay (India-First)** | ALTERNATIVE: **Stripe** | **USER APPROVAL REQUIRED**.

---

## 5. Commercial Policy & Governance Invariants

### 5.1 Subscription Capacity Model (LOCKED)
The subscription capacity model is **LOCKED** in application code and Flyway migrations `V030`, `V034`:

| Metric / Entitlement | STANDARD | PREMIUM | PRO | BASE (Grandfathered) | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Max Active Projects** | **10** | **20** | **20** | 5 | **LOCKED** |
| **Photos per Project** | **15** | **25** | **30** | 10 | **LOCKED** |
| **Total Cloud Storage** | **5 GB** | **20 GB** | **50 GB** | 2 GB | **LOCKED** |
| **Cinematic Presentations** | **0** | **0** | **Max 5** Active Projects | 0 | **LOCKED** |
| **Client Review Workflows** | Included | Included | Included | Included | **LOCKED** |
| **Studio Team Collaboration** | 1 Designer | Up to 3 | Up to 10 | 1 Designer | **LOCKED** |
| **Custom Domain Portfolio** | Subdomain | Subdomain | Custom domain | Subdomain | **LOCKED** |

### 5.2 Pricing Schedule Worksheet (DECISION REQUIRED)
Pricing is not hardcoded. Business leadership must specify target amounts:

| Plan | Billing Period | Target INR (₹) | Target USD ($) | Decision Status |
| :--- | :--- | :--- | :--- | :--- |
| **STANDARD** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **STANDARD** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PREMIUM** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PREMIUM** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PRO** | Monthly | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |
| **PRO** | Annual | `[ DECISION REQUIRED ]` | `[ DECISION REQUIRED ]` | Pending User Approval |

### 5.3 Currency & Billing Periods
* **Primary Currency:** **INR (`₹`)** recommended for India-first launch. Status: **USER APPROVAL REQUIRED**.
* **Billing Periods:** **Monthly** and **Annual** recommended. Status: **USER APPROVAL REQUIRED**.

### 5.4 Billing Grace Period
* **Source Default:** 7 days (`private int gracePeriodDays = 7;` in `BillingService.java`).
* **Status:** **7-DAY CONFIG DEFAULT — USER BUSINESS APPROVAL REQUIRED**.

### 5.5 Upgrade Proration & Downgrade Behavior
* **Upgrade Proration:** Policy for applying pro-rata credit towards upgraded tier is **DECISION REQUIRED**.
* **Downgrade Behavior (LOCKED INVARIANT):** Downgrading **never deletes** customer projects, photographs, or studio media. Over-quota assets remain preserved in read-only state. Downgrade takes effect strictly at **end-of-period** via `cancelAtPeriodEnd=true` and `downgradePlanId`.

### 5.6 Refund Policy
* **Status:** **DECISION REQUIRED** (Options: No refunds, 14-day refund window, or prorated credits).

### 5.7 GST & Statutory Tax Treatment
* **Status:** **GST / TAX CONFIGURATION REQUIRES QUALIFIED ACCOUNTING / LEGAL REVIEW**.

---

## 6. Privacy & Operational Data Retention Schedules

| Data Category | Current Baseline / Code Default | Governance Classification | Decision Status |
| :--- | :--- | :--- | :--- |
| **Privacy Export TTL** | 7 days (`app.privacy.export-ttl-days=7`) | Privacy / Legal | **CONFIG DEFAULT (7 Days) — USER APPROVAL REQUIRED** |
| **Account Deletion Grace** | 14 days before permanent purge | Privacy / Legal | **DECISION REQUIRED (14 Days Recommended)** |
| **Database Backups** | Daily snapshots, 30 days recommended | Operations / Disaster Recovery | **DECISION REQUIRED** |
| **System Audit Ledger** | Immutable write-ahead audit table (`audit_events`) | Security / Compliance | **DECISION REQUIRED (3 Years Recommended)** |
| **Support Tickets** | Indefinite (`support_requests`) | Business / Customer Ops | **DECISION REQUIRED (2 Years Recommended)** |
| **CRM Leads & Enquiries** | Indefinite with anonymization (`leads`) | Business Operations | **DECISION REQUIRED** |
| **Malware Scan Records** | Scan metadata in `media_security_scans` | Security Operations | **DECISION REQUIRED (90 Days Recommended)** |
| **Infected Quarantine Files** | Isolated in quarantine storage namespace | Security Operations | **DECISION REQUIRED (14 Days Auto-Purge Recommended)** |
| **Identity Sessions** | Terminated session records in `identity_sessions` | Security Operations | **DECISION REQUIRED (30 Days Tombstone Recommended)** |
| **AI Cost Ledgers** | Historical cost entries in `ai_cost_ledger` | Financial Audit | **DECISION REQUIRED (5 Years Recommended)** |
| **Client Review Drafts** | Unsubmitted reviews | Business Operations | **DECISION REQUIRED** |
| **Communications Tracking** | Outbox delivery logs (`communication_deliveries`) | Operations | **DECISION REQUIRED (90 Days Recommended)** |

---

## 7. Disaster Recovery & Operational Objectives

### 7.1 Recovery Point Objective (RPO) — DECISION REQUIRED
* **Option 1 (< 1 Hour):** Requires managed PostgreSQL continuous Write-Ahead Log (WAL) archiving and Point-In-Time Recovery (PITR). (Recommended).
* **Option 2 (24 Hours):** Daily snapshot backups only; lower cost, but up to 24 hours of data loss in catastrophic disaster.

### 7.2 Recovery Time Objective (RTO) — DECISION REQUIRED
* **Option 1 (< 2 Hours):** Automated container re-deployment and point-in-time database restoration runbook. (Recommended).
* **Option 2 (< 8 Hours):** Standard business-hours manual restoration.

### 7.3 Backup Strategy
* **Minimum Closed Beta Requirement:** Managed PostgreSQL automated daily backups + verified restore capability.

### 7.4 Monitoring & Centralized Logging
* **Closed Beta:** Hosting-native container and deployment logs (Vercel & Railway) + endpoint probes on `/actuator/health` and `/health`.
* **Public/Commercial:** Centralized searchable logging (e.g. BetterStack Logtail) and alert notifications (PagerDuty / Opsgenie).

---

## 8. Master Production Environment Variable Audit

All variables derived directly from application source code (`application-production.properties`, `AuthSecurityProperties.java`, `S3StorageProperties.java`, `AiVisualizerProperties.java`, `apps/web/next.config.js`, `apps/web/src/lib/env.ts`):

| Variable Name | Secret? | Closed Beta? | Public Beta? | Commercial? | Source File | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | NO | **YES** | **YES** | **YES** | `application-production.properties:3` | Must be set to `production` to activate production validators and security filters. |
| `SERVER_PORT` | NO | **YES** | **YES** | **YES** | `application-production.properties:6` | Embedded Tomcat listening port (default: `8080`). |
| `SPRING_DATASOURCE_URL` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:19` | PostgreSQL JDBC connection URL with SSL mode (`jdbc:postgresql://<host>:<port>/<db>?sslmode=require`). |
| `SPRING_DATASOURCE_USERNAME` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:20` | PostgreSQL application database username. |
| `SPRING_DATASOURCE_PASSWORD` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:21` | PostgreSQL application database password. |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | NO | Optional | Optional | Optional | `application-production.properties:24` | HikariCP maximum pool size (default: `10`). |
| `APP_SECURITY_ALLOWED_ORIGINS` | NO | **YES** | **YES** | **YES** | `application-production.properties:50` | CORS allowed origins (e.g. `https://<ROOT_DOMAIN>`). Fails fast if wildcard or localhost in production. |
| `APP_SECURITY_DEV_AUTH_ENABLED` | NO | **YES** | **YES** | **YES** | `application-production.properties:47` | Must be `false`. Application refuses startup if true in production. |
| `APP_SECURITY_SESSION_COOKIE_SECURE` | NO | **YES** | **YES** | **YES** | `application-production.properties:49` | Enforces `Secure` flag on `__Host-session` cookie. Must be `true`. |
| `APP_SECURITY_MFA_ENCRYPTION_KEY` | **YES** | **YES** | **YES** | **YES** | `AuthSecurityProperties.java` | 256-bit AES master key used to encrypt TOTP secrets at rest in PostgreSQL. |
| `OIDC_GOOGLE_CLIENT_ID` | NO | **YES** | **YES** | **YES** | `AuthSecurityProperties.java` | Google OAuth 2.0 Client ID for studio owner authentication. |
| `OIDC_GOOGLE_CLIENT_SECRET` | **YES** | **YES** | **YES** | **YES** | `AuthSecurityProperties.java` | Google OAuth 2.0 Client Secret for token exchange. |
| `MEDIA_STORAGE_PROVIDER` | NO | **YES** | **YES** | **YES** | `application-production.properties:61` | Set to `S3_COMPATIBLE` for Cloudflare R2 or AWS S3 cloud storage. |
| `S3_ENDPOINT` | NO | **YES** | **YES** | **YES** | `application-production.properties:62` | S3 API endpoint URL (e.g. `https://<account-id>.r2.cloudflarestorage.com` for R2). |
| `S3_REGION` | NO | **YES** | **YES** | **YES** | `application-production.properties:63` | S3 region identifier (`auto` for R2). |
| `S3_ACCESS_KEY_ID` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:64` | R2/S3 access key credential. |
| `S3_SECRET_ACCESS_KEY` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:65` | R2/S3 secret access key credential. |
| `S3_BUCKET_PRIVATE` | NO | **YES** | **YES** | **YES** | `application-production.properties:66` | Private bucket for originals, quarantine, and GDPR exports. |
| `S3_BUCKET_PUBLIC` | NO | **YES** | **YES** | **YES** | `application-production.properties:67` | Public bucket for clean derivative photography. |
| `PUBLIC_MEDIA_BASE_URL` | NO | **YES** | **YES** | **YES** | `application-production.properties:68` | Base URL returned in API responses for public media assets (`https://<MEDIA_DOMAIN>`). |
| `PRIVACY_EXPORT_TTL_DAYS` | NO | Optional | Optional | Optional | `application-production.properties:71` | Expiration window in days for privacy export ZIP bundles (default: `7`). |
| `MALWARE_SCANNER_PROVIDER` | NO | Optional | **YES** | **YES** | `application-production.properties:74` | Malware scanner provider (`disabled`, `clamav`, `external`). |
| `REQUIRE_CLEAN_SCAN` | NO | Optional | **YES** | **YES** | `application-production.properties:75` | Whether uploads fail closed without clean scan (default: `true`). |
| `NEXT_PUBLIC_API_URL` | NO | **YES** | **YES** | **YES** | `apps/web/src/lib/env.ts` | Base API URL for Next.js browser client (`https://api.<ROOT_DOMAIN>/api/v1`). |
| `APP_CDN_HOSTNAME` | NO | **YES** | **YES** | **YES** | `apps/web/next.config.js` | Strict RFC 1123 hostname for Next.js image optimization (`<MEDIA_DOMAIN>`). |
| `APP_EMAIL_PROVIDER` | NO | NO | **YES** | **YES** | `TransactionalEmailService.java` | Email provider selector (`resend`, `postmark`, `ses`). |
| `APP_EMAIL_API_KEY` | **YES** | NO | **YES** | **YES** | `TransactionalEmailService.java` | Transactional email provider API key. |
| `APP_EMAIL_FROM_ADDRESS` | NO | NO | **YES** | **YES** | `TransactionalEmailService.java` | Verified sender email address (e.g. `notifications@<ROOT_DOMAIN>`). |
| `APP_EMAIL_WEBHOOK_SECRET` | **YES** | NO | **YES** | **YES** | `EmailWebhookController.java` | Secret for verifying email bounce and complaint webhooks. |
| `INTERIOR_AI_PROVIDER` | NO | NO | Optional | Optional | `AiVisualizerProperties.java` | AI provider selector (`stability`, `replicate`, `vertex`). |
| `INTERIOR_AI_API_KEY` | **YES** | NO | Optional | Optional | `AiVisualizerProperties.java` | AI model provider API key. |
| `BILLING_PROVIDER` | NO | NO | Optional | **YES** | `BillingService.java` | Commercial gateway selector (`razorpay`, `stripe`). |
| `BILLING_API_KEY` | **YES** | NO | Optional | **YES** | `BillingService.java` | Live payment gateway API key. |
| `BILLING_WEBHOOK_SECRET` | **YES** | NO | Optional | **YES** | `BillingWebhookController.java` | HMAC signing secret for payment webhooks. |

---

## 9. Launch Phasing Decision Tables

### 9.1 Closed-Beta Decision Table
| Area | Recommended Option | Status | Required Before Deploy? | User Approval Required? |
| :--- | :--- | :--- | :--- | :--- |
| Frontend Hosting | Vercel | RECOMMENDED | **YES** | **YES** |
| Backend Hosting | Railway | RECOMMENDED | **YES** | **YES** |
| Database Provider | Railway Managed PostgreSQL | RECOMMENDED | **YES** | **YES** |
| PostgreSQL Version | PostgreSQL 17 | RECOMMENDED | **YES** | **YES** |
| Object Storage | Cloudflare R2 | RECOMMENDED | **YES** | **YES** |
| Media CDN | Cloudflare CDN (`<MEDIA_DOMAIN>`) | RECOMMENDED | **YES** | **YES** |
| Production Domain | `<ROOT_DOMAIN>` | DECISION REQUIRED | **YES** | **YES** |
| OIDC Authentication | Google OIDC First | RECOMMENDED | **YES** | **YES** |
| Transactional Email | DISABLED | DEFERRED TO PUBLIC BETA | NO | **YES** |
| Generative AI | DISABLED | DEFERRED TO AI EVAL | NO | **YES** |
| Upload Malware Policy | Controlled Trusted Beta (Option B) | RECOMMENDED | **YES** | **YES** |
| Commercial Billing | DISABLED | DEFERRED TO COMMERCIAL | NO | **YES** |
| Database Backups | Automated Daily + PITR | RECOMMENDED | **YES** | **YES** |
| Monitoring & Logs | Hosting-Native | RECOMMENDED | **YES** | **YES** |

### 9.2 Public-Beta Decision Table
| Area | Recommended Option | Status | Required Before Public Beta? | User Approval Required? |
| :--- | :--- | :--- | :--- | :--- |
| Transactional Email | Resend / Postmark / SES | DECISION REQUIRED | **YES** | **YES** |
| Malware Scanning | ClamAV / Commercial Scanner (`require-clean-scan=true`) | DECISION REQUIRED | **YES** | **YES** |
| Generative AI | Evaluate Diffusion Models (Stability / Replicate) | DEFERRED | Optional | **YES** |
| Legal Policies | Terms of Service & Privacy Policy Sign-Off | DECISION REQUIRED | **YES** | **YES** |
| Centralized Logs | External Log Indexing (e.g. BetterStack) | RECOMMENDED | **YES** | **YES** |
| Customer Support | Support Desk Workflow | DECISION REQUIRED | **YES** | **YES** |

### 9.3 Commercial-Launch Decision Table
| Area | Recommended Option | Status | Required Before Commercial? | User Approval Required? |
| :--- | :--- | :--- | :--- | :--- |
| Payment Gateway | Razorpay (India-First) | RECOMMENDED | **YES** | **YES** |
| Pricing Schedule | Finalized Monthly & Annual Rates | DECISION REQUIRED | **YES** | **YES** |
| Currency | INR (`₹`) Primary | RECOMMENDED | **YES** | **YES** |
| Billing Periods | Monthly + Annual | RECOMMENDED | **YES** | **YES** |
| Grace Period | 7 Days (Config Default) | RECOMMENDED | **YES** | **YES** |
| Upgrade Proration | Prorated Immediate Credit | RECOMMENDED | **YES** | **YES** |
| Downgrade Invariant | No Data Deletion (Locked) | **LOCKED** | **YES** | Pre-approved |
| Refund Policy | Defined Refund Terms | DECISION REQUIRED | **YES** | **YES** |
| GST / Tax Treatment | Qualified Legal / Accounting Review | DECISION REQUIRED | **YES** | **YES** |
| Data Retention | Formal Legal Retention Policies | DECISION REQUIRED | **YES** | **YES** |
| RPO / RTO | RPO < 1h / RTO < 2h | RECOMMENDED | **YES** | **YES** |

---

## 10. Future Deployment Execution Sequence (Not Executed)

When user decisions are formally approved, execution will strictly follow this ordered sequence:

1. **User Decision Lock:** User approves items in [PHASE_11_FINAL_USER_APPROVAL_SHEET.md](file:///c:/my%20projects/interior%20design/docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md).
2. **Domain Registration & DNS:** Register `<ROOT_DOMAIN>` and configure authoritative nameservers.
3. **Cloud Accounts & Providers:** Provision accounts on selected providers (Vercel, Railway, Cloudflare R2, Google Cloud).
4. **Production Secret Generation:** Generate 256-bit AES MFA master key, database credentials, and R2 tokens.
5. **Database Provisioning:** Deploy managed PostgreSQL 17 cluster with SSL mode required.
6. **Automated Backups:** Verify automated daily snapshots and WAL archiving are active.
7. **Object Storage Provisioning:** Create private and public R2 buckets with CORS rules.
8. **Backend API Deployment:** Deploy Spring Boot Docker container to Railway with production environment variables.
9. **Flyway Migration Execution:** Run migrations `V001` through `V040` on production database.
10. **RLS Security Verification:** Execute `verify-cloud-db-rls.sql` to confirm all 59 tables have `FORCE ROW LEVEL SECURITY`.
11. **Frontend Deployment:** Deploy Next.js web application to Vercel with production API and CDN environment variables.
12. **OIDC Callback Configuration:** Register production redirect URI in Google Cloud Console.
13. **DNS & Edge TLS:** Map apex/www to frontend, `api.` to backend, and `media.` to public storage.
14. **Production Smoke Testing:** Execute synthetic end-to-end check (OIDC login, project creation, media upload intent).
15. **Closed-Beta Invitation Onboarding:** Issue initial designer studio invitation tokens.

---

## 11. Deployment Rollback Plan (Future Operational Runbook)

### 11.1 Application Rollback
* **Frontend:** Instant rollback to previous Vercel deployment via Vercel dashboard or CLI (`vercel rollback`).
* **Backend:** Redeploy previous verified Docker image tag on Railway (`railway rollback` or previous image digest).

### 11.2 Database Rollback Strategy
* **Strict Rule:** **NO BLIND DOWN-MIGRATIONS.** Flyway migrations `V001` through `V040` are additive and backwards-compatible.
* **Corrupted Schema / Data Event:** Restore database from the pre-deployment automated snapshot or point-in-time recovery (PITR) WAL log.

---

## 12. Final Status & Directive

* **Application Code Status:** Release Candidate Ready (100% verified).
* **UI/UX Design Freeze:** **ACTIVE & LOCKED**.
* **Deployment Status:** **DEPLOYMENT NOT PERFORMED.**
* **Next Action:** Wait for explicit user approval of the decision items in [docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md](file:///c:/my%20projects/interior%20design/docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md).
