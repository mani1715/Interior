# Phase 14 — Production Secret & Credential Inventory

> **Status:** RELEASE CANDIDATE AUDITED — ZERO REAL SECRETS COMMITTED  
> **Platform:** Elégance Interior Designer Platform  
> **Phase:** 14 (Release Package & Pre-Launch Operations)  
> **Provider Activation:** DEFERRED to Phase 15  

---

## 1. Overview & Security Policy

All production secrets, encryption keys, and third-party API credentials must be injected at runtime via environment variables managed by an encrypted secret store (e.g. AWS Secrets Manager, HashiCorp Vault, Doppler, or cloud container platform secrets).

### Core Invariants
1. **Zero Secret Footprint in Repository:** No production secret, API key, JWT private key, or database password may ever be committed to git.
2. **Fail-Closed Validation:** The Spring Boot backend activates `ProductionConfigurationValidator` on boot when `SPRING_PROFILES_ACTIVE=production`. If any required secret is missing or contains a known placeholder value (e.g., `change-me`, `your-`, `*placeholder*`), the application will immediately terminate with an error code before binding any ports.
3. **No Secret Leaks in Builds:** Docker images (`interior-platform-api` and `interior-platform-web`) do not contain `.env` files, build-time secret injection, or bundled credentials. Next.js public environment variables (`NEXT_PUBLIC_*`) are strictly limited to non-sensitive client identifiers and API URLs.

---

## 2. Complete Secret Matrix

| Secret Variable | Target Service | Purpose / Scope | Generation / Rotation Method | Min Entropy / Format | Fail-Closed in Prod |
|---|---|---|---|---|---|
| `SPRING_DATASOURCE_PASSWORD` | Backend API | PostgreSQL primary user password | Generated via CSPRNG (`openssl rand -base64 32`) | >= 32 chars, base64 | Yes |
| `SECURITY_JWT_SECRET` | Backend API | HMAC-SHA512 signing key for stateless auth tokens | CSPRNG base64 string (`openssl rand -base64 64`) | >= 64 bytes (512 bits) | Yes |
| `ENCRYPTION_KEY` | Backend API | AES-GCM data encryption key for sensitive PII | CSPRNG base64 string (`openssl rand -base64 32`) | 256 bits (32 bytes) | Yes |
| `STORAGE_SECRET_KEY` | Backend API | Cloudflare R2 / S3 Secret Access Key | Cloudflare R2 Token Dashboard | AWS-style 40+ char secret | When storage enabled |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` | Backend API | Google OAuth2/OIDC client secret | Google Cloud Console OAuth 2.0 Credentials | Google client secret format | When OIDC enabled |
| `SPRING_AI_OPENAI_API_KEY` | Backend API | OpenAI / LLM API key for design assistance | OpenAI Developer Platform | `sk-proj-...` format | When AI enabled |
| `SPRING_AI_ANTHROPIC_API_KEY` | Backend API | Anthropic API key for multimodal reasoning | Anthropic Console | `sk-ant-...` format | When AI enabled |
| `SPRING_SENDGRID_API_KEY` | Backend API | SendGrid transactional email API key | SendGrid API Keys Console | `SG....` format | When SendGrid enabled |
| `RESEND_API_KEY` | Backend API | Resend transactional email API key | Resend Dashboard | `re_...` format | When Resend enabled |
| `POSTMARK_SERVER_TOKEN` | Backend API | Postmark server token | Postmark Server Settings | Postmark UUID / token | When Postmark enabled |
| `STRIPE_SECRET_KEY` | Backend API | Stripe private API secret key | Stripe Developer Dashboard | `sk_live_...` | When Stripe enabled |
| `STRIPE_WEBHOOK_SECRET` | Backend API | Stripe webhook signature verification key | Stripe Webhook Endpoint Dashboard | `whsec_...` | When Stripe enabled |
| `MANAGEMENT_METRICS_EXPORT_DATADOG_API_KEY` | Backend API | Datadog agent metrics submission | Datadog Organization API Keys | 32 hex chars | When Datadog enabled |

---

## 3. Frontend Variable Isolation

Next.js embeds environment variables prefixed with `NEXT_PUBLIC_` directly into the compiled JavaScript bundle delivered to public browsers.

### Public (Non-Secret) Frontend Variables
- `NEXT_PUBLIC_API_URL`: Backend API base URL (e.g. `https://api.elegance.interior.design/api/v1`).
- `NEXT_PUBLIC_APP_URL`: Frontend canonical origin (e.g. `https://elegance.interior.design`).
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`: Google OIDC client ID (public by design).
- `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY`: Stripe publishable key (`pk_live_...`, public by design).

> **Verification Rule:** CI and deployment verification runs a regex scan against `apps/web/.next/static` to guarantee that no private API key (`sk_live_`, `sk-proj-`, `SG.`, `whsec_`, `JWT_SECRET`, or `PASSWORD`) is present in client assets.

---

## 4. Secret Rotation Runbook

### 4.1 JWT Secret Key Rotation
1. Update `SECURITY_JWT_SECRET` in secret manager.
2. Trigger rolling restart of backend container instances.
3. Existing client refresh tokens / active sessions will fail signature validation and trigger a clean redirect to `/login`.
4. Users log in and receive new tokens signed with rotated key.

### 4.2 Database Password Rotation
1. In PostgreSQL:
   ```sql
   ALTER USER interior_user WITH PASSWORD 'new-secure-password';
   ```
2. Update `SPRING_DATASOURCE_PASSWORD` in secret manager.
3. Trigger rolling restart of backend containers.
4. Verify backend liveness: `GET /actuator/health/readiness`.

### 4.3 Stripe Webhook Secret Rotation
1. Add new endpoint or rotate secret in Stripe Dashboard.
2. Stripe provides a rolling secret grace period.
3. Update `STRIPE_WEBHOOK_SECRET` in secret manager.
4. Restart backend containers.

---

## 5. Phase 15 Provider Activation Handoff

In Phase 15, the platform owner will retrieve live credentials from respective providers and populate the production secret store. Until Phase 15 activation:
- All optional provider settings remain `disabled` or `none` in baseline configuration.
- The platform operates safely in local and staging validation environments without any external third-party credential dependencies.
