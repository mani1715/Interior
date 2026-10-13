# Elégance — Phase 11B Secret and Configuration Matrix
**Document Status:** Complete / Authoritative Source Truth  
**Phase:** Phase 11B — Final Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Production Credential & Configuration Specification  
**Release Candidate Baseline:** `7db3212fb4b43e507d60e81fd1e235b98bb87821` (Flyway `V040`)  

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

## 2. Complete Production Environment Variable Audit

| VARIABLE | SECRET? | REQUIRED FOR CLOSED BETA? | REQUIRED FOR PUBLIC BETA? | REQUIRED FOR COMMERCIAL? | SOURCE FILE | DESCRIPTION |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | NO | **YES** | **YES** | **YES** | `application-production.properties:3` | Activates production security validations, strict CORS, and connection pooling. Must be `production`. |
| `SERVER_PORT` | NO | **YES** | **YES** | **YES** | `application-production.properties:6` | Listening port for embedded Tomcat server (default: `8080`). |
| `SPRING_DATASOURCE_URL` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:19` | Production PostgreSQL JDBC connection URL with SSL mode (`jdbc:postgresql://<host>:<port>/<db>?sslmode=require`). |
| `SPRING_DATASOURCE_USERNAME` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:20` | PostgreSQL application runtime user. |
| `SPRING_DATASOURCE_PASSWORD` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:21` | PostgreSQL application runtime password. |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | NO | Optional | Optional | Optional | `application-production.properties:24` | HikariCP connection pool upper bound (default: `10`). |
| `APP_SECURITY_ALLOWED_ORIGINS` | NO | **YES** | **YES** | **YES** | `application-production.properties:50` | CORS allowed origin (`https://<ROOT_DOMAIN>`). Startup validator rejects wildcards or localhost. |
| `APP_SECURITY_DEV_AUTH_ENABLED` | NO | **YES** | **YES** | **YES** | `application-production.properties:47` | Must be `false`. Application fails fast at startup if true in production. |
| `APP_SECURITY_SESSION_COOKIE_SECURE` | NO | **YES** | **YES** | **YES** | `application-production.properties:49` | Enforces `Secure` flag on `__Host-session` cookie (default: `true`). |
| `APP_SECURITY_MFA_ENCRYPTION_KEY` | **YES** | **YES** | **YES** | **YES** | `AuthSecurityProperties.java` | 256-bit AES master key used to encrypt TOTP secrets at rest in PostgreSQL. |
| `OIDC_GOOGLE_CLIENT_ID` | NO | **YES** | **YES** | **YES** | `AuthSecurityProperties.java` | Google OAuth 2.0 Client ID for studio owner authentication. |
| `OIDC_GOOGLE_CLIENT_SECRET` | **YES** | **YES** | **YES** | **YES** | `AuthSecurityProperties.java` | Google OAuth 2.0 Client Secret for token exchange. |
| `MEDIA_STORAGE_PROVIDER` | NO | **YES** | **YES** | **YES** | `application-production.properties:61` | Set to `S3_COMPATIBLE` for Cloudflare R2 or AWS S3 cloud storage. |
| `S3_ENDPOINT` | NO | **YES** | **YES** | **YES** | `application-production.properties:62` | S3 API endpoint URL (e.g. `https://<account-id>.r2.cloudflarestorage.com` for R2). |
| `S3_REGION` | NO | **YES** | **YES** | **YES** | `application-production.properties:63` | S3 region identifier (`auto` for R2). |
| `S3_ACCESS_KEY_ID` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:64` | R2/S3 access key credential. |
| `S3_SECRET_ACCESS_KEY` | **YES** | **YES** | **YES** | **YES** | `application-production.properties:65` | R2/S3 secret access key credential. |
| `S3_BUCKET_PRIVATE` | NO | **YES** | **YES** | **YES** | `application-production.properties:66` | Private bucket for originals, quarantine uploads, and GDPR privacy export ZIPs. |
| `S3_BUCKET_PUBLIC` | NO | **YES** | **YES** | **YES** | `application-production.properties:67` | Public bucket for clean derivative photography. |
| `PUBLIC_MEDIA_BASE_URL` | NO | **YES** | **YES** | **YES** | `application-production.properties:68` | Base URL returned in API responses for public media assets (`https://<MEDIA_DOMAIN>`). |
| `PRIVACY_EXPORT_TTL_DAYS` | NO | Optional | Optional | Optional | `application-production.properties:71` | Expiration window in days for privacy export ZIP bundles (default: `7`). |
| `MALWARE_SCANNER_PROVIDER` | NO | Optional | **YES** | **YES** | `application-production.properties:74` | Malware scanner provider (`disabled`, `clamav`, `external`). |
| `REQUIRE_CLEAN_SCAN` | NO | Optional | **YES** | **YES** | `application-production.properties:75` | Whether uploads fail closed without clean scan (default: `true`). |
| `NEXT_PUBLIC_API_URL` | NO | **YES** | **YES** | **YES** | `apps/web/src/lib/env.ts` | Base API URL for Next.js browser client (`https://api.<ROOT_DOMAIN>/api/v1`). |
| `APP_CDN_HOSTNAME` | NO | **YES** | **YES** | **YES** | `apps/web/next.config.js` | Strict RFC 1123 hostname for Next.js image optimization (`<MEDIA_DOMAIN>`). |
| `APP_EMAIL_PROVIDER` | NO | NO | **YES** | **YES** | `TransactionalEmailService.java` | Transactional email provider selector (`resend`, `postmark`, `ses`). |
| `APP_EMAIL_API_KEY` | **YES** | NO | **YES** | **YES** | `TransactionalEmailService.java` | Transactional email provider secret API key. |
| `APP_EMAIL_FROM_ADDRESS` | NO | NO | **YES** | **YES** | `TransactionalEmailService.java` | Verified sender email address (e.g. `notifications@<ROOT_DOMAIN>`). |
| `APP_EMAIL_WEBHOOK_SECRET` | **YES** | NO | **YES** | **YES** | `EmailWebhookController.java` | HMAC secret for verifying email delivery and bounce webhooks. |
| `INTERIOR_AI_PROVIDER` | NO | NO | Optional | Optional | `AiVisualizerProperties.java` | AI provider selector (`stability`, `replicate`, `vertex`). |
| `INTERIOR_AI_API_KEY` | **YES** | NO | Optional | Optional | `AiVisualizerProperties.java` | AI model provider secret token. |
| `INTERIOR_AI_ENDPOINT` | NO | NO | Optional | Optional | `AiVisualizerProperties.java` | Custom AI endpoint URL if using custom inference service. |
| `INTERIOR_AI_MODEL` | NO | NO | Optional | Optional | `AiVisualizerProperties.java` | Model identifier string (e.g. `interior-diffusion-v1`). |
| `BILLING_PROVIDER` | NO | NO | Optional | **YES** | `BillingService.java` | Commercial payment gateway selector (`razorpay`, `stripe`). |
| `BILLING_API_KEY` | **YES** | NO | Optional | **YES** | `BillingService.java` | Commercial payment gateway live API key. |
| `BILLING_WEBHOOK_SECRET` | **YES** | NO | Optional | **YES** | `BillingWebhookController.java` | Cryptographic secret for verifying payment webhook signatures. |

---

## 3. Discrepancy Reconciliation & Unused Variables

During this source audit, historical documentation placeholders were reconciled against actual code truth:

1. **`JWT_SECRET` (DISAVOWED / UNUSED):**
   - *Audit Finding:* Historical documentation incorrectly listed `JWT_SECRET`.
   - *Source Truth:* Elégance does **NOT use JWT session tokens**. Authentication uses stateful 256-bit cryptographic tokens stored as SHA-256 hashes in PostgreSQL table `identity_sessions`. Zero JWT parsing or signing exists in the codebase.
   - *Instruction:* Do **NOT** configure `JWT_SECRET`.

2. **`APP_ADMIN_MFA_ENCRYPTION_KEY` vs `app.security.mfa.encryption-key`:**
   - *Audit Finding:* Early draft documents used `APP_ADMIN_MFA_ENCRYPTION_KEY`.
   - *Source Truth:* Spring Boot maps properties to `AuthSecurityProperties.MfaProperties.encryptionKey`, which corresponds to `app.security.mfa.encryption-key` (environment variable: `APP_SECURITY_MFA_ENCRYPTION_KEY`).

3. **`STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` (DEFERRED):**
   - *Audit Finding:* Stripe keys listed as mandatory defaults in early drafts.
   - *Source Truth:* Payment provider selection remains pending user decision. Live billing checkout is disabled (`DisabledBillingProvider`). Do not provision payment credentials until commercial launch.

---

## 4. Secret Storage & Operational Best Practices

1. **Never Commit `.env.production`:** Production credentials must never be committed to git or stored on developer workstations.
2. **Container Platform Secret Ingestion:** Inject secrets directly via managed cloud platforms (e.g., Railway Project Variables, Vercel Environment Variables, AWS Secrets Manager).
3. **Fail-Fast Startup Validation:** The application will immediately abort startup if required database credentials or encryption keys are missing or set to insecure defaults.
