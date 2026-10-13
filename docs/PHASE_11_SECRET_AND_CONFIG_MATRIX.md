# Elégance — Phase 11 Secret and Configuration Matrix
**Document Status:** Complete / Authoritative Source Truth  
**Phase:** Phase 11A — Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Production Credential & Configuration Specification  

---

## 1. Overview & Derivation Methodology

This document specifies the authoritative, source-verified inventory of all environment variables, secrets, and configuration parameters used across the Elégance platform.

Every variable listed below is mapped directly to actual source code definitions in:
- `apps/api/src/main/resources/application.properties`
- `apps/api/src/main/resources/application-production.properties`
- `apps/api/src/main/java/com/interior/platform/security/config/AuthSecurityProperties.java`
- `apps/api/src/main/java/com/interior/platform/media/storage/S3StorageProperties.java`
- `apps/api/src/main/java/com/interior/platform/ai/config/AiVisualizerProperties.java`
- `apps/api/src/main/java/com/interior/platform/media/config/MediaSecurityProperties.java`
- `apps/web/next.config.js` and `apps/web/src/lib/env.ts`

> **ZERO GUESSWORK RULE:** No variable names are assumed. If a variable is not consumed in application code, it is classified as *Unused / Not Implemented*.

---

## 2. Complete Environment Variable & Secret Inventory

### 2.1 Mandatory Secrets for Closed Beta (Tier 1)

| Environment Variable Name | Secret? | Required When? | Source Component & Class | Who / What Provides It? | Rotation Impact & Procedure |
|---|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | YES (Sensitive) | Closed Beta | Backend (`application-production.properties:19`) | Cloud Database Provider (RDS, Supabase, Neon) | Requires application restart. Format: `jdbc:postgresql://<host>:<port>/<db>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | YES | Closed Beta | Backend (`application-production.properties:20`) | Cloud Database Provider | Requires application restart. Least-privilege application role. |
| `SPRING_DATASOURCE_PASSWORD` | YES (Critical) | Closed Beta | Backend (`application-production.properties:21`) | Cloud Database Provider | Requires application restart. Strong 32+ character random password. |
| `APP_SECURITY_MFA_ENCRYPTION_KEY` | YES (Critical) | Closed Beta | Backend (`AuthSecurityProperties:154`, `TotpService:183`) | Platform Operations / Security Vault | **CRITICAL ROTATION IMPACT:** Master key used to encrypt/decrypt TOTP secrets at rest. Rotating key requires database re-encryption job. |
| `OIDC_GOOGLE_CLIENT_ID` | NO (Public ID) | Closed Beta | Backend (`AuthSecurityProperties:37`, `OidcService`) | Google Cloud Console (OAuth 2.0 Client) | Updates allowed OAuth client ID. |
| `OIDC_GOOGLE_CLIENT_SECRET` | YES (Critical) | Closed Beta | Backend (`AuthSecurityProperties:38`, `OidcService`) | Google Cloud Console (OAuth 2.0 Client) | Token exchanges fail immediately if rotated without updating backend. |
| `S3_ACCESS_KEY_ID` | YES | Closed Beta | Backend (`S3StorageProperties:40`, `S3StorageService`) | Cloudflare R2 / AWS S3 IAM | Uploads fail if mismatched. Update via zero-downtime deployment. |
| `S3_SECRET_ACCESS_KEY` | YES (Critical) | Closed Beta | Backend (`S3StorageProperties:48`, `S3StorageService`) | Cloudflare R2 / AWS S3 IAM | Uploads fail if mismatched. Update via zero-downtime deployment. |

---

### 2.2 Mandatory Non-Secret Production Configuration (Closed Beta)

| Environment Variable Name | Default / Production Setting | Required When? | Source Component & Class | Purpose & Behavioral Impact |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `production` | Closed Beta | Backend (`application-production.properties`) | Activates production security validations and Hikari connection pool. |
| `SERVER_PORT` | `8080` | Closed Beta | Backend (`application-production.properties:6`) | Embedded Tomcat listening port. |
| `APP_SECURITY_ALLOWED_ORIGINS` | `https://elegance.design` | Closed Beta | Backend (`application-production.properties:50`) | Enforces CORS allowlist. Startup validator throws error if wildcard or localhost. |
| `APP_SECURITY_DEV_AUTH_ENABLED`| `false` | Closed Beta | Backend (`application-production.properties:47`) | Must be `false`. Startup validator aborts startup if true in production. |
| `APP_SECURITY_SESSION_COOKIE_SECURE` | `true` | Closed Beta | Backend (`application-production.properties:49`) | Enforces `Secure` flag on `__Host-session` cookie. |
| `MEDIA_STORAGE_PROVIDER` | `S3_COMPATIBLE` | Closed Beta | Backend (`application-production.properties:61`) | Directs storage operations to S3/R2 cloud storage instead of local disk. |
| `S3_ENDPOINT` | e.g. `https://<id>.r2.cloudflarestorage.com` | Closed Beta | Backend (`application-production.properties:62`) | S3 API endpoint URL (required for Cloudflare R2, optional for AWS S3). |
| `S3_REGION` | `auto` (or `us-east-1`) | Closed Beta | Backend (`application-production.properties:63`) | S3 storage region identifier. |
| `S3_BUCKET_PRIVATE` | `elegance-media-private` | Closed Beta | Backend (`application-production.properties:66`) | Private bucket for originals, quarantine uploads, and user export bundles. |
| `S3_BUCKET_PUBLIC` | `elegance-media-public` | Closed Beta | Backend (`application-production.properties:67`) | Public bucket for web-optimized derivative photos. |
| `PUBLIC_MEDIA_BASE_URL` | `https://media.elegance.design` | Closed Beta | Backend (`application-production.properties:68`) | Base URL returned in API responses for public media assets. |
| `NEXT_PUBLIC_API_URL` | `https://api.elegance.design/api/v1` | Closed Beta | Frontend (`apps/web/src/lib/env.ts`) | Public API endpoint consumed by Next.js browser client and SSR server. |
| `APP_CDN_HOSTNAME` | `media.elegance.design` | Closed Beta | Frontend (`apps/web/next.config.js`) | Strict RFC 1123 hostname for Next.js image optimization (wildcard `*` prohibited). |

---

### 2.3 Deferred Credentials for Public Beta & Commercial Launch

| Variable Name | Subsystem | Required Launch Tier | Source Component & Class | Who Provides It? |
|---|---|---|---|---|
| `APP_EMAIL_PROVIDER` | Email | Public Beta | Backend (`TransactionalEmailService`) | Operator selector (`resend`, `postmark`, `ses`) |
| `APP_EMAIL_API_KEY` | Email | Public Beta | Backend (`TransactionalEmailService`) | Transactional Email Provider Dashboard |
| `APP_EMAIL_FROM_ADDRESS` | Email | Public Beta | Backend (`TransactionalEmailService`) | Verified Domain Sender (e.g. `notifications@elegance.design`) |
| `APP_EMAIL_WEBHOOK_SECRET` | Email | Public Beta | Backend (`EmailWebhookController:19`) | Webhook Signing Secret from Email Provider |
| `INTERIOR_AI_PROVIDER` | AI | Public Beta (if AI enabled) | Backend (`AiVisualizerProperties:13`) | Provider selector (`stability`, `replicate`, `vertex`) |
| `INTERIOR_AI_API_KEY` | AI | Public Beta (if AI enabled) | Backend (`AiVisualizerProperties:18`) | AI Model Provider Secret Token |
| `INTERIOR_AI_ENDPOINT` | AI | Public Beta (if AI enabled) | Backend (`AiVisualizerProperties:23`) | Custom AI Endpoint URL |
| `INTERIOR_AI_MODEL` | AI | Public Beta (if AI enabled) | Backend (`AiVisualizerProperties:28`) | Model identifier (e.g. `stable-diffusion-xl-1024-v1-0`) |
| `BILLING_PROVIDER` | Billing | Commercial Launch | Backend (`BillingService`) | Gateway selector (`razorpay`, `stripe`) |
| `BILLING_API_KEY` | Billing | Commercial Launch | Backend (`BillingService`) | Commercial Payment Gateway Live API Key |
| `BILLING_WEBHOOK_SECRET` | Billing | Commercial Launch | Backend (`BillingWebhookController`) | Commercial Payment Webhook HMAC Secret |

---

## 3. Discrepancy Reconciliation & Unused Variables

During this audit, historical documentation placeholders were reconciled against actual source truth:

1. **`JWT_SECRET` (DISAVOWED / UNUSED):**
   - *Audit Finding:* Historical documentation incorrectly listed `JWT_SECRET`.
   - *Source Truth:* Elégance does **NOT use JWT session tokens**. Authentication is implemented using stateful 256-bit cryptographic tokens stored as SHA-256 hashes in PostgreSQL table `identity_sessions`. Zero JWT parsing or signing exists in the codebase.
   - *Instruction:* Do **NOT** configure `JWT_SECRET`.

2. **`APP_ADMIN_MFA_ENCRYPTION_KEY` vs `app.security.mfa.encryption-key`:**
   - *Audit Finding:* Early draft documents used `APP_ADMIN_MFA_ENCRYPTION_KEY`.
   - *Source Truth:* Spring Boot maps properties to `AuthSecurityProperties.MfaProperties.encryptionKey`, which corresponds to `app.security.mfa.encryption-key` (environment variable: `APP_SECURITY_MFA_ENCRYPTION_KEY`).

3. **`STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` (DEFERRED):**
   - *Audit Finding:* Stripe keys listed in early drafts.
   - *Source Truth:* Payment provider selection remains pending user decision. Live billing checkout is disabled (`DisabledBillingProvider`). Do not provision payment credentials until Phase 11B.

---

## 4. Secret Storage & Operational Best Practices

1. **Never Commit `.env.production`:** Production credentials must never be committed to git or stored on developer workstations.
2. **Container Platform Secret Ingestion:** Inject secrets directly via managed cloud platforms (e.g., Railway Project Variables, Vercel Environment Variables, AWS Secrets Manager).
3. **Fail-Fast Validation:** The application will immediately abort startup if required database credentials or encryption keys are missing or set to insecure defaults.
