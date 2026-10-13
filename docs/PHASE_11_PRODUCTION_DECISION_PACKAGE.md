# Elégance — Phase 11A Production Decision Package
**Document Status:** Complete / Ready for User Approval  
**Phase:** Phase 11A — Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Executive & Architectural Production Decision Package  
**Release Candidate Baseline:** `ccbbcec534f466c8f40fbfe86d63915e66ecbb9e` (Flyway `V040`)  

---

## 1. Executive Summary & Purpose

The Elégance Interior Designer Platform has reached **100% Release Candidate (RC) verification** with zero open defects across all severity tiers (P0=0, P1=0, P2=0, P3=0). Application feature freeze is active.

**CRITICAL DEPLOYMENT DIRECTIVE:**
> **DEPLOYMENT NOT PERFORMED.** No cloud infrastructure, databases, storage buckets, domains, DNS records, or external vendor integrations have been provisioned or altered. This phase exists solely to provide the definitive, source-verified decision package required before any Phase 11 deployment begins.

This package provides the authoritative blueprint detailing:
1. Every infrastructure, vendor, and commercial decision requiring explicit user selection.
2. What the existing codebase actually supports versus what requires vendor binding.
3. The exact minimum decisions needed for **Closed Beta (Invitation-Only)** vs **Public Beta** vs **General Commercial Launch**.
4. Full secret and configuration variable matrices derived directly from the application source code.
5. Migration, storage, DNS, and rollback procedures.

---

## 2. Decision Classification Standard

Every decision item in this package is assigned exactly one authoritative status:
- **`LOCKED`**: Architected, implemented, and verified in code; immutable under feature freeze.
- **`RECOMMENDED — NOT SELECTED`**: Technical recommendation based on architecture, security, and low operational complexity; awaits user decision.
- **`DECISION REQUIRED`**: Requires user / stakeholder / legal selection before deployment can proceed.
- **`NOT REQUIRED FOR THIS LAUNCH LEVEL`**: Can remain safely disabled/unconfigured for the designated launch tier.
- **`DEFERRED`**: Intentionally postponed to a post-launch phase.

---

## 3. Comprehensive Domain Decisions (A through AB)

### A. Production Domain
- **Current Status:** `DECISION REQUIRED` (Current state: `NOT SELECTED`)
- **Required Architecture:**
  - **Root / Web Frontend:** `elegance.design` or `www.elegance.design`
  - **API Ingress:** `api.elegance.design` or reverse-proxied path `/api/v1` on the main domain
  - **Media / CDN Host:** `media.elegance.design` or dedicated storage CDN hostname (bound to `APP_CDN_HOSTNAME`)
  - **OIDC Redirect URI:** `https://<web-domain>/api/v1/auth/oidc/callback/<provider>`
  - **Email Sending Subdomain:** `mail.elegance.design` or `send.elegance.design`
- **Technical Recommendation:** Select a custom domain (e.g. `elegance.design`). Route frontend on apex/www and backend API on `api.` subdomain with TLS termination at Cloudflare / Edge Proxy.

### B. Frontend Hosting
- **Current Status:** `RECOMMENDED — NOT SELECTED`
- **Runtime Mode:** Standalone Node.js server (`next.config.js` specifies `output: 'standalone'`). Runs Next.js 16.3.8 with Turbopack.
- **Requirements:** Node.js 20+ runtime, support for environment variables (`NEXT_PUBLIC_API_URL`, `APP_CDN_HOSTNAME`), SSR support for SEO-indexed routes.
- **Candidate Evaluation:**
  - **Vercel:** *Fit:* Native Next.js edge platform. *Pros:* Zero devops, instant preview, automatic edge caching. *Cons:* Higher commercial tier costs.
  - **Railway / Render:** *Fit:* Dockerized standalone container (`node server.js`). *Pros:* Unified billing with backend container, predictable cost. *Cons:* Manual CDN edge configuration.
  - **AWS ECS / Amplify:** *Fit:* Enterprise cloud container. *Pros:* Deep enterprise controls. *Cons:* High operational complexity.
- **Technical Recommendation:** **Vercel** or **Railway** (using standalone Docker container).
- **Closed-Beta Blocker:** **YES**.

### C. Backend / API Hosting
- **Current Status:** `RECOMMENDED — NOT SELECTED`
- **Runtime Mode:** Spring Boot 3.4.3 on Java 21, multi-stage Docker container (`apps/api/Dockerfile`).
- **Requirements:** 1 vCPU / 2GB RAM minimum, outbound internet for provider APIs, persistent TLS database connectivity, graceful shutdown support (`server.shutdown=graceful`), health probe endpoints (`/actuator/health/liveness`, `/actuator/health/readiness`).
- **Candidate Evaluation:**
  - **Railway:** *Fit:* Container runner with native private networking to PostgreSQL. *Pros:* Simplest setup, zero-downtime deploys, built-in health checks. *Cons:* Fixed regions.
  - **Render:** *Fit:* Managed web service with Docker runtime. *Pros:* Transparent pricing, automated TLS. *Cons:* Cold starts on lower tiers.
  - **AWS ECS Fargate:** *Fit:* Production enterprise standard. *Pros:* Extreme scalability, VPC isolation, IAM integration. *Cons:* Significant operational overhead.
- **Technical Recommendation:** **Railway** (for rapid beta onboarding with automated health probes) or **AWS ECS Fargate** (for long-term enterprise scalability).
- **Closed-Beta Blocker:** **YES**.

### D. PostgreSQL Database Provider & Version
- **Current Status:** `DECISION REQUIRED`
- **Source Truth:**
  - Locally tested: PostgreSQL 18.3 (port 5433).
  - Minimum supported: PostgreSQL 15.
  - Recommended production family: PostgreSQL 16 or 17.
  - Migrations: Exactly 40 canonical Flyway migrations (`V001` through `V040`).
  - Row Level Security: 59 entity tables enforced via session variables `app.current_user_id`, `app.current_studio_id`, `app.is_admin`.
- **Candidate Evaluation:**
  - **Neon:** Serverless PostgreSQL with instant branching and connection pooling. Supports PG 16.
  - **Supabase:** Managed PG 16 with integrated connection pooling (PgBouncer/Supavisor).
  - **AWS RDS PostgreSQL:** Fully managed PostgreSQL 16 with Multi-AZ HA and automated PITR.
  - **Railway Managed PostgreSQL:** Simple single-node PG 16 provisioned alongside backend.
- **Technical Recommendation:** **AWS RDS PostgreSQL 16** (Multi-AZ) for enterprise production, or **Supabase / Neon** for beta.
- **Closed-Beta Blocker:** **YES**.

### E. Object Storage (Media & Attachments)
- **Current Status:** `RECOMMENDED — NOT SELECTED`
- **Source Architecture:** S3-compatible storage adapter (`S3StorageService`).
  - Dual bucket model: Private bucket (`S3_BUCKET_PRIVATE`) for original uploads, quarantine objects, and GDPR exports; Public bucket (`S3_BUCKET_PUBLIC`) for verified clean derivatives.
  - Capabilities required: Presigned PUT URLs, content-type verification, metadata attachment, byte streaming, deletion.
- **Candidate Evaluation:**
  - **Cloudflare R2:** *Pros:* Zero egress fees, native S3 compatibility, global edge distribution. *Cons:* Requires custom domain binding for public assets.
  - **AWS S3:** *Pros:* Global standard, fine-grained IAM policies, AWS KMS encryption. *Cons:* Egress bandwidth fees.
  - **Backblaze B2:** *Pros:* Low cost. *Cons:* Slower propagation.
- **Technical Recommendation:** **Cloudflare R2** (zero egress fee makes portfolio photo hosting highly cost-effective).
- **Closed-Beta Blocker:** **YES** (or local filesystem for single-node closed beta container).

### F. CDN / Media Delivery
- **Current Status:** `DECISION REQUIRED`
- **Source Architecture:** Next.js `next.config.js` enforces strict RFC 1123 hostname validation on `APP_CDN_HOSTNAME` / `NEXT_PUBLIC_CDN_HOSTNAME`. Arbitrary wildcards (`*`) are prohibited.
- **Hostname Binding:** Must match the public custom CDN domain serving derivative media assets (e.g., `media.elegance.design` or `cdn.elegance.design`).
- **Technical Recommendation:** Bind Cloudflare CDN or AWS CloudFront to the public storage bucket, and supply that exact FQDN to `APP_CDN_HOSTNAME`.
- **Closed-Beta Blocker:** **YES** (if cloud storage is active).

### G. Identity & OIDC Provider(s)
- **Current Status:** `DECISION REQUIRED` (Current: Generic OIDC architecture implemented, `OidcService` handles OAuth2 authorization code flow with PKCE, state, and nonce verification).
- **Source Architecture:** Configurable OIDC map under `app.security.oidc.providers.<provider-name>` (properties: `issuer`, `clientId`, `clientSecret`, `authorizationUri`, `tokenUri`, `redirectUri`).
- **Candidates:** Google Identity (`google`), Apple Sign-In (`apple`), GitHub (`github`), or unified auth broker (e.g., Clerk / Auth0).
- **Technical Recommendation:** **Google Identity** for primary closed-beta onboarding.
- **Closed-Beta Blocker:** **YES** (at least one production OIDC provider must be configured, as Dev Personas are locked out in production).

### H. Transactional Email Provider
- **Current Status:** `DECISION REQUIRED` (Current: `EmailProvider` SPI; implementations: `DisabledEmailProvider` in production, `FakeEmailProvider` in test).
- **Required Capabilities:** Outbox queue dispatch, exponential backoff, bounce and complaint webhook ingestion (`/api/v1/public/webhooks/email/{provider}`).
- **Candidates:** Resend, Postmark, AWS SES, SendGrid.
- **DNS Requirements:** SPF record (`v=spf1 ...`), DKIM key (CNAME/TXT), DMARC policy (`v=DMARC1; p=quarantine...`), verified From address (`notifications@elegance.design`).
- **Closed-Beta Blocker:** **NO** (Closed beta can operate with invitations or disabled email if team onboarding is manual).
- **Public-Beta Blocker:** **YES**.

### I. AI Image Generation Provider & Model
- **Current Status:** `DECISION REQUIRED` (Current: `ConfigurableAiImageProvider` SPI supporting `stability`, `replicate`, `vertex`, `mock_configured`).
- **Workflow Requirements:** Multimodal image conditioning, interior room style transfer, inpainting masks (`supportsMaskEditing`), reference image weighting (`supportsReferenceImages`, up to 4 references), prompt enhancement.
- **Candidates:**
  - **Stability AI:** SDXL Inpainting / Stable Image Core. Proven interior architecture fine-tunes.
  - **Replicate:** Hosted FLUX.1 or SDXL pipelines with custom LoRA support.
  - **Google Vertex AI:** Imagen 3 with enterprise data protection (guaranteed zero customer data retention for model training).
- **Compliance / DPA:** Data Processing Agreement (DPA) required ensuring customer room photos are never used for model training.
- **Closed-Beta Blocker:** **NO** (AI feature can remain disabled via `interior.ai.enabled=false` or `interior.ai.provider=none`).
- **Public-Beta Blocker:** **YES** (if AI visualizer feature is marketed).

### J. AI Commercial Policy
- **Current Status:** `DECISION REQUIRED`
- **Source Truth:** Application code enforces a hard rate limit of **20 generations / studio / UTC calendar day** (`AiVisualizerProperties.dailyStudioLimit = 20`, reset at 00:00 UTC).
- **Policy Options:**
  1. *Included Allowance:* 20 free generations daily included in active paid plans.
  2. *Credit Pack Add-on:* Base daily allowance with purchased credit packs for overage.
  3. *Tiered Allowance:* Standard = 5/day, Premium = 15/day, Pro = 30/day.
- **Technical Recommendation:** Maintain the implemented 20 generations/day included with Pro/Premium subscriptions during beta.

### K. Malware Scanner
- **Current Status:** `DECISION REQUIRED` (Current: `MalwareScanner` SPI; implementations: `DisabledMalwareScanner`, `FakeMalwareScanner`).
- **Source Truth:** `app.media.security.require-clean-scan=true` enforces that uninspected uploads fail closed and cannot be committed to canonical storage.
- **Candidates:**
  - **ClamAV Daemon:** Containerized ClamAV daemon running alongside the API worker.
  - **AWS GuardDuty for S3:** Native AWS malware protection scanning bucket uploads asynchronously.
  - **VirusTotal / ICAP API:** External commercial malware scanning API.
- **Closed-Beta Blocker:** **NO** (Can set `app.media.security.require-clean-scan=false` for trusted closed-beta testers, or deploy lightweight ClamAV container).
- **Public-Beta Blocker:** **YES** (Fail-closed clean scan mandatory for public uploads).

### L. Payment Gateway & Commercial Billing
- **Current Status:** `DECISION REQUIRED` (Current: `BillingProvider` SPI; implementations: `DisabledBillingProvider`, `FakeBillingProvider`).
- **Target Market Considerations:**
  - **India:** Must support UPI, domestic credit/debit cards, NetBanking, and RBI e-mandate recurring guidelines. Primary candidate: **Razorpay** or **Stripe India**.
  - **International:** Must support global credit cards, multi-currency settlement, and automated tax handling (Stripe Billing, LemonSqueezy, Paddle).
- **Closed-Beta Blocker:** **NO** (Commercial checkout disabled; beta users assigned complimentary Pro plans).
- **Public-Beta Blocker:** **NO** (Can run in Sandbox/Test mode).
- **Commercial-Launch Blocker:** **YES**.

### M. Commercial Plans Capacity Model
- **Current Status:** `LOCKED` (Hardened in database schema `V030`, `V034` and backend code).
- **Capacities:**
  - **STANDARD:** 10 projects, 15 portfolio photos/project, 5 GB storage, 0 Cinematic projects.
  - **PREMIUM:** 20 projects, 25 portfolio photos/project, 20 GB storage, 0 Cinematic projects.
  - **PRO:** 20 projects, 30 portfolio photos/project, 50 GB storage, max 5 Cinematic projects.
  - **BASE:** Legacy/internal baseline only.

### N. Pricing Schedule
- **Current Status:** `DECISION REQUIRED` (No prices locked in code; pricing worksheet presented below).

### O. Billing Currency
- **Current Status:** `DECISION REQUIRED` (Options: INR `₹` for India domestic market, USD `$` for global SaaS, or dual currency display).

### P. Billing Periods
- **Current Status:** `DECISION REQUIRED` (Options: Monthly, Quarterly, Annual with discount).

### Q. Billing Grace Period
- **Current Status:** `DECISION REQUIRED`
- **Source Default:** 7 days (`app.billing.grace-period-days:7`).
- **Business Policy:** Must decide whether studios enter read-only state immediately upon renewal failure or after a formal grace period (e.g., 3 days, 7 days, 14 days).

### R. Upgrade & Downgrade Behavior
- **Current Status:** `DECISION REQUIRED` (for upgrade timing/proration) / `LOCKED` (for downgrade architecture).
- **Source Architecture:** Downgrades take effect strictly at **end-of-period**. Existing project records and photos are **never deleted** on downgrade; over-quota records enter safe read-only display status.

### S. Refund Policy
- **Current Status:** `DECISION REQUIRED` (Options: No refunds, 14-day money-back guarantee, or pro-rated service credits).

### T. GST & Tax Treatment
- **Current Status:** `DECISION REQUIRED` (Requires accounting/legal counsel; B2B GSTIN collection and SAC code invoicing specifications).

### U. Production Monitoring & APM
- **Current Status:** `RECOMMENDED — NOT SELECTED`
- **Application Instrumentation:** Spring Boot Actuator enabled (`/actuator/health`, `/actuator/metrics`, `/actuator/info`). Micrometer tags application `interior-platform-api`.
- **Candidates:** Prometheus + Grafana, Datadog, New Relic, Better Stack.
- **Closed-Beta Blocker:** Basic uptime check required.

### V. Centralized Logging
- **Current Status:** `RECOMMENDED — NOT SELECTED`
- **Application Instrumentation:** Structured SLF4J/Logback logs with `X-Request-Id` MDC correlation. Recipient email hashes and PII masking active.
- **Candidates:** Better Stack Logtail, AWS CloudWatch Logs, Papertrail.
- **Closed-Beta Blocker:** Container stdout logs sufficient; centralized log aggregation recommended.

### W. Production Backup Infrastructure
- **Current Status:** `DECISION REQUIRED`
- **Verification Drill:** Clean `pg_dump` and `pg_restore` verified in Phase 10 audit.
- **Requirement:** Automated daily PostgreSQL snapshots, WAL archiving for Point-In-Time Recovery (PITR), cross-region replication.
- **Closed-Beta Blocker:** **YES** (automated daily backups mandatory before storing any user data).

### X. Recovery Point Objective (RPO)
- **Current Status:** `DECISION REQUIRED` (Non-binding planning target: `< 1 hour` for commercial data).

### Y. Recovery Time Objective (RTO)
- **Current Status:** `DECISION REQUIRED` (Non-binding planning target: `< 4 hours` for full platform restoration).

### Z. Data Retention Policies (12 Categories)
- **Current Status:** `DECISION REQUIRED` across all 12 operational data categories (detailed in Section 5).

### AA. Privacy & Terms Legal Review
- **Current Status:** `DECISION REQUIRED` (Technical export/deletion mechanisms verified; draft Privacy Policy and Terms of Service require formal legal sign-off before public launch).

### AB. Launch Tier Phasing
- **Current Status:** `LOCKED` (Tier 1 Closed Beta → Tier 2 Public Beta → Tier 3 General Availability).

---

## 4. Launch Tier Readiness Minimums

```
                       ┌───────────────────────────────┐
                       │   TIER 1: CLOSED BETA         │
                       │   - Hosted Web & API          │
                       │   - PostgreSQL 16+ (RLS)      │
                       │   - Google OIDC               │
                       │   - Cloud Storage (R2/S3)     │
                       │   - Daily DB Backups          │
                       │   * Billing: DISABLED         │
                       │   * Email: LOG/MANUAL         │
                       │   * AI: OPTIONAL/DISABLED     │
                       └───────────────┬───────────────┘
                                       │
                                       ▼
                       ┌───────────────────────────────┐
                       │   TIER 2: PUBLIC BETA         │
                       │   - Live Transactional Email  │
                       │   - Fail-Closed Malware AV    │
                       │   - Live AI Provider + DPA    │
                       │   - Sandbox Billing Gateway   │
                       │   - Legal Policy Sign-Off     │
                       │   - Centralized APM & Logs    │
                       └───────────────┬───────────────┘
                                       │
                                       ▼
                       ┌───────────────────────────────┐
                       │   TIER 3: COMMERCIAL LAUNCH   │
                       │   - Live Payment Underwriting │
                       │   - GST Invoicing & Tax       │
                       │   - Final Locked Pricing      │
                       │   - Formally Approved SLAs    │
                       │   - 24/7 On-Call Alerts       │
                       └───────────────────────────────┘
```

---

## 5. Unresolved Retention Policies Matrix

Every retention policy requires formal business, legal, or security determination:

| # | Data Category | Source Baseline / Current Default | Governance Classification | Decision Required |
|---|---|---|---|---|
| 1 | **Database Backups** | Daily snapshots, 30 days recommended | `BUSINESS & COMPLIANCE` | Snapshot frequency, PITR window, offsite retention |
| 2 | **Support Tickets** | Indefinite (table `support_requests`) | `BUSINESS DECISION` | Closed ticket archival period (e.g. 1 year vs 3 years) |
| 3 | **Billing & Invoices** | 7 years minimum (`billing_transactions`) | `LEGAL & TAX MANDATE` | Statutory tax compliance retention period |
| 4 | **CRM Leads & Enquiries** | Indefinite with user anonymization | `BUSINESS DECISION` | Inactive lead purge schedule |
| 5 | **System Audit Logs** | 5 years (`audit_events`) | `SECURITY & COMPLIANCE` | Forensic audit log archival duration |
| 6 | **Malware Scan Metadata** | Indefinite (`media_security_scans`) | `SECURITY DECISION` | Clean scan log retention (e.g. 90 days vs 1 year) |
| 7 | **Infected Quarantine Files** | Immediate isolation in quarantine namespace | `SECURITY DECISION` | Auto-purge delay for forensic investigation (e.g. 14 days) |
| 8 | **Identity Verification Records** | Session lifetime (`identity_sessions`) | `SECURITY DECISION` | Terminated session tombstone cleanup (e.g. 30 days) |
| 9 | **AI Prompts & Cost Ledgers** | 5 years (`ai_cost_ledger`) | `FINANCIAL AUDIT` | AI generation history retention for users vs financial accounting |
| 10 | **GDPR Privacy Exports** | 7 days (`app.privacy.export-ttl-days:7`) | `LEGAL & SECURITY` | ZIP export download link expiration window |
| 11 | **Client Reviews & Ratings** | Lifetime of published portfolio | `BUSINESS DECISION` | Unapproved / draft review purge policy |
| 12 | **Communication Deliveries** | 90 days (`communication_deliveries`) | `OPERATIONAL DECISION` | Outbox message tracking & bounce log cleanup window |

---

## 6. Commercial Pricing Decision Worksheet

The capacity model is **LOCKED**. The pricing values below require executive commercial sign-off:

| Plan | Included Projects | Included Photos / Proj | Included Storage | Cinematic Projects | Monthly Price (INR / USD) | Quarterly Price | Annual Price (Discount %) |
|---|---|---|---|---|---|---|---|
| **STANDARD** | 10 | 15 | 5 GB | 0 | *[ USER DECISION ]* | *[ USER DECISION ]* | *[ USER DECISION ]* |
| **PREMIUM** | 20 | 25 | 20 GB | 0 | *[ USER DECISION ]* | *[ USER DECISION ]* | *[ USER DECISION ]* |
| **PRO** | 20 | 30 | 50 GB | 5 | *[ USER DECISION ]* | *[ USER DECISION ]* | *[ USER DECISION ]* |
| **BASE** | *Legacy/Internal* | *Legacy/Internal* | *Legacy/Internal* | 0 | *N/A (Grandfathered)* | *N/A* | *N/A* |

---

## 7. Master Environment Secret & Config Inventory

Derived directly from application source code (`application-production.properties`, `AuthSecurityProperties`, `S3StorageProperties`, `AiVisualizerProperties`, `next.config.js`):

### 7.1 Mandatory Core Platform Secrets (Closed Beta)

| Variable Name | Component | Sensitivity | Purpose & Source | Rotation Impact |
|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Backend | CRITICAL | Production PostgreSQL JDBC connection URL with SSL mode (`jdbc:postgresql://...`) | API restart required |
| `SPRING_DATASOURCE_USERNAME` | Backend | HIGH | PostgreSQL database application username | API restart required |
| `SPRING_DATASOURCE_PASSWORD` | Backend | CRITICAL | PostgreSQL database application user password | API restart required |
| `APP_SECURITY_MFA_ENCRYPTION_KEY` | Backend | CRITICAL | 256-bit AES master key used by `TotpService` to encrypt TOTP secrets at rest | Requires secret re-encryption |
| `OIDC_GOOGLE_CLIENT_ID` | Backend | HIGH | Google OAuth2 Client ID for authentication | Auth disruption if mismatched |
| `OIDC_GOOGLE_CLIENT_SECRET` | Backend | CRITICAL | Google OAuth2 Client Secret for token exchange | Auth disruption if mismatched |
| `S3_ACCESS_KEY_ID` | Backend | HIGH | Object storage (R2/S3) access key ID | Upload failures if rotated wrong |
| `S3_SECRET_ACCESS_KEY` | Backend | CRITICAL | Object storage (R2/S3) secret access key | Upload failures if rotated wrong |

### 7.2 Core Non-Secret Production Configuration (Closed Beta)

| Variable Name | Component | Default / Production Value | Purpose & Source |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | `production` | Activates production configuration and security validators |
| `SERVER_PORT` | Backend | `8080` | Port for embedded Tomcat web server |
| `APP_SECURITY_ALLOWED_ORIGINS` | Backend | `https://elegance.design` | CORS allowed origin; must not contain wildcard or localhost |
| `APP_SECURITY_DEV_AUTH_ENABLED`| Backend | `false` | Must be `false` in production; application fails fast if true |
| `APP_SECURITY_SESSION_COOKIE_SECURE` | Backend | `true` | Enforces `Secure` flag on session cookies |
| `MEDIA_STORAGE_PROVIDER` | Backend | `S3_COMPATIBLE` | Switches from ephemeral local storage to cloud storage |
| `S3_ENDPOINT` | Backend | e.g. `https://<account-id>.r2.cloudflarestorage.com` | Custom endpoint for Cloudflare R2 or MinIO |
| `S3_REGION` | Backend | `auto` (or `us-east-1`) | Cloud storage region |
| `S3_BUCKET_PRIVATE` | Backend | `elegance-media-private` | Private bucket for originals, quarantine, and GDPR bundles |
| `S3_BUCKET_PUBLIC` | Backend | `elegance-media-public` | Public bucket for clean, web-optimized derivatives |
| `PUBLIC_MEDIA_BASE_URL` | Backend | `https://media.elegance.design` | Base URL used in API responses for public media assets |
| `NEXT_PUBLIC_API_URL` | Frontend | `https://api.elegance.design/api/v1` | Public API endpoint consumed by Next.js client |
| `APP_CDN_HOSTNAME` | Frontend | `media.elegance.design` | Strict RFC 1123 hostname for Next.js image optimization |

### 7.3 Conditional / Launch Tier Deferred Credentials (Public Beta & Commercial)

| Variable Name | Subsystem | Required When? | Purpose |
|---|---|---|---|
| `APP_EMAIL_API_KEY` | Email | Public Beta | API key for transactional email provider (Resend / SES) |
| `APP_EMAIL_FROM_ADDRESS` | Email | Public Beta | Verified sender email (e.g. `notifications@elegance.design`) |
| `APP_EMAIL_WEBHOOK_SECRET` | Email | Public Beta | HMAC secret for verifying email delivery/bounce webhooks |
| `INTERIOR_AI_API_KEY` | AI | Public Beta (if AI enabled) | API key for AI generation provider (Stability / Replicate) |
| `INTERIOR_AI_PROVIDER` | AI | Public Beta (if AI enabled) | Provider selector (`stability`, `replicate`, `vertex`) |
| `BILLING_API_KEY` | Billing | Commercial Launch | Live secret API key for payment gateway (Razorpay / Stripe) |
| `BILLING_WEBHOOK_SECRET` | Billing | Commercial Launch | Cryptographic secret for verifying payment webhook signatures |

---

## 8. Network Topology & Traffic Routing Flow

```
 [ End User Browser / Client ]
            │
            │  HTTPS (Port 443)
            ▼
 ┌──────────────────────────────────────────────────────────────┐
 │                EDGE PROXY / CDN (Cloudflare)                 │
 └──────┬───────────────────────────────┬───────────────────────┘
        │                               │
        │ Host: elegance.design         │ Host: api.elegance.design
        ▼                               ▼
 ┌──────────────────────┐      ┌────────────────────────────────┐
 │   FRONTEND RUNNER    │      │         BACKEND API            │
 │   (Next.js 16.3.8)   │─────▶│     (Spring Boot Java 21)      │
 │   Standalone Node    │      │     Port 8080 / Docker         │
 └──────────────────────┘      └────────┬──────────────┬────────┘
                                        │              │
                   ┌────────────────────┘              └──────────────────┐
                   ▼                                                      ▼
     ┌────────────────────────────┐                         ┌───────────────────────────┐
     │   POSTGRESQL 16+ DATABASE  │                         │   S3-COMPATIBLE STORAGE   │
     │   - 40 Flyway Migrations   │                         │   - Private Bucket        │
     │   - 59 RLS Entity Tables   │                         │   - Public Derivative CDN │
     └────────────────────────────┘                         └───────────────────────────┘
```

---

## 9. Cloud Migration & Deployment Execution Sequence (Future Phase 11B)

When Phase 11A decisions are approved, execution will follow this strict sequence:

1. **Infrastructure Provisioning:** Provision PostgreSQL database, S3 storage buckets, and container services.
2. **Secret Injection:** Inject validated environment variables into deployment secret managers.
3. **Database Migration Drill:**
   - Execute Flyway migration runner against target production database (`V001` through `V040`).
   - Run verification script `verify-cloud-db-rls.sql` to confirm all 59 tables have `FORCE ROW LEVEL SECURITY`.
4. **Backend Container Launch:**
   - Deploy backend API container.
   - Verify `/actuator/health` returns HTTP 200 `UP`.
   - Verify `ProductionConfigurationValidator` logs successful startup.
5. **Frontend Web Deployment:**
   - Build standalone Next.js container.
   - Verify SSR rendering and image optimization against `APP_CDN_HOSTNAME`.
6. **Live Smoke Test:**
   - Execute synthetic OIDC login, project creation, media upload intent, and health checks.

---

## 10. Final Authoritative Decision

**Status:** **READY FOR USER PRODUCTION DECISIONS — DEPLOYMENT NOT YET AUTHORIZED.**

> [!IMPORTANT]
> **NO DEPLOYMENT HAS BEEN PERFORMED.** All application features remain frozen. Antigravity will NOT generate or execute cloud deployment commands until the user reviews and explicitly approves the decision items presented in this package.
