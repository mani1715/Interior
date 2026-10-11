# Elégance — Release Configuration Matrix
**Document Status:** Production Baseline / Release Candidate  
**Phase:** Phase 10 — Final Production Release Audit  
**Classification:** Operational Configuration Specification

---

## 1. Overview

This matrix specifies the configuration status, provider binding, required credentials, and launch tier availability for every major capability in the Elégance platform.

Launch Tiers:
- **Tier 1: Closed Beta (Studio Invitation Only)** — Core design operations, internal testing, simulated payments/email/AI if necessary.
- **Tier 2: Public Beta (Limited Onboarding)** — Real transactional email, real cloud object storage, live malware scanning, sandbox billing.
- **Tier 3: Commercial Launch (General Availability)** — Full commercial billing, production AI quotas, complete observability, multi-region CDN.

---

## 2. Capability Configuration Matrix

| Subsystem / Feature | Provider Status | Required Environment Variables | Tier 1 (Closed Beta) | Tier 2 (Public Beta) | Tier 3 (General Availability) |
|---|---|---|---|---|---|
| **Identity & Authentication (OIDC / OAuth)** | Provider Ready (`Google`, `Apple`, `Email OTP`) | `AUTH_OIDC_GOOGLE_CLIENT_ID`, `AUTH_OIDC_GOOGLE_CLIENT_SECRET`, `APP_JWT_SECRET` | Active | Active | Active |
| **Session & Token Management** | Implemented (Database-backed SHA-256 session store) | `APP_SESSION_COOKIE_DOMAIN`, `APP_SESSION_SECURE_COOKIE` | Active | Active | Active |
| **Admin MFA (RFC 6238 TOTP)** | Implemented (In-app TOTP + Argon2id backup codes) | N/A (Internal crypto algorithms) | Active | Active | Active |
| **Tenant Isolation & RLS** | Implemented (Native PostgreSQL RLS across 59 tables) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Active | Active | Active |
| **Project & Proposal Engine** | Implemented | N/A | Active | Active | Active |
| **Object Storage (Media Uploads)** | Provider Pending (Implemented `S3` adapter; candidate: `Cloudflare R2` / `AWS S3`) | `AWS_S3_BUCKET`, `AWS_S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_ENDPOINT` (if R2) | Local / Mock Storage | Live Cloud Storage | Live Cloud Storage + Multi-Region CDN |
| **Malware Scanning** | Provider Pending (Implemented `MalwareScanner` SPI; candidate: `ClamAV`) | `APP_MEDIA_SECURITY_REQUIRE_CLEAN_SCAN`, `CLAMAV_HOST`, `CLAMAV_PORT` | Permissive dev / mock scan | Fail-Closed ClamAV | Fail-Closed ClamAV + Edge Pre-Filter |
| **Transactional Email** | Provider Pending (Implemented `EmailTransport` SPI; candidate: `Resend` / `SES`; status: `NOT_CONFIGURED`) | `APP_EMAIL_PROVIDER`, `APP_EMAIL_API_KEY`, `APP_EMAIL_FROM_ADDRESS` | Log Outbox / Test Inbox | Live Outbox (Resend/SES) | Production Verified Sender Domain + DMARC |
| **Generative AI & Computer Vision** | Provider Pending (Implemented client adapter; candidate: `Anthropic` / `OpenAI`; quota: 20/studio/day config default) | `AI_PROVIDER`, `AI_API_KEY`, `AI_DEFAULT_MODEL`, `AI_MONTHLY_TOKEN_BUDGET` | Mock / Seeded Responses | Live LLM with strict user rate-limits | Full Generative Model Tiering |
| **Commercial Billing & Subscriptions** | Provider Pending (Implemented `BillingProvider` SPI; candidate: `Stripe` / `Razorpay`; status: `NOT_SELECTED`) | `BILLING_PROVIDER`, `BILLING_API_KEY`, `BILLING_WEBHOOK_SECRET`, `BILLING_PRICE_IDS` | Seeded Mock Billing | Sandbox Billing (Stripe/Razorpay Test) | Production Gateway + Live Webhooks |
| **Support & Ticket Operations** | Implemented (Database-backed queue & assignment; retention decision required) | N/A | Active | Active | Active |
| **Platform Ops & Maintenance Mode** | Implemented (`NORMAL` and `READ_ONLY` with strict admin boundary) | `APP_OPERATIONS_MAINTENANCE_MODE` | Active | Active | Active |
| **Audit Logging & Security Events** | Implemented (`audit_logs`, `security_events`) | N/A | Active | Active | Active |
| **Data Export & Account Closure** | Implemented (JSON bundle export + immediate suspension/deletion; 14-day recovery is config default only) | N/A | Active | Active | Active |

---

## 3. Environment Variable Validation Rules

In production (`prod` Spring profile / `NODE_ENV=production`):
1. **No Blank Secret Fallbacks:** If `JWT_SECRET`, `SESSION_SECRET`, or `SPRING_DATASOURCE_PASSWORD` are missing or set to default example strings (e.g., `dev-secret`, `changeme`), the application fails fast at startup.
2. **Fail-Closed Malware Scanning:** When `APP_MEDIA_SECURITY_REQUIRE_CLEAN_SCAN=true`, files without a verified clean scan result are permanently quarantined and inaccessible.
3. **Strict Origin Validation:** `APP_ALLOWED_ORIGINS` must match the production custom domain exactly; wildcard origins (`*`) are disallowed when cookies are enabled.
