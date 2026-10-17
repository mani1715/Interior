# Phase 14 — Provider Activation Handoff Package

> **Status:** AUDITED & PREPARED FOR PHASE 15 ACTIVATION  
> **Platform:** Elégance Interior Designer Platform  
> **Current State:** RELEASE CANDIDATE (Zero Live Credentials Active)  
> **Next Milestone:** Phase 15 — Real Provider Activation  

---

## 1. Executive Summary

Phase 14 delivers a completely packaged, tested, and deployment-ready interior designer platform with zero third-party external provider dependencies active.

The platform architecture is designed to boot safely in production with mock or disabled providers, or seamlessly switch to real cloud providers by supplying valid runtime environment variables. All provider connectors have been engineered and tested with **fail-closed validation**: if an invalid or placeholder key is provided when a provider is activated, the system aborts startup immediately.

---

## 2. Provider Inventory & Activation Requirements for Phase 15

The following table summarizes the external services required for full production operation, along with specific activation instructions for Phase 15:

| Provider Subsystem | Target Cloud Service | Current Phase 14 State | Phase 15 Required Actions |
|---|---|---|---|
| **Object Storage** | Cloudflare R2 | S3-compatible test / local | 1. Create Cloudflare R2 bucket.<br>2. Generate S3 Access Key ID & Secret Key.<br>3. Set `STORAGE_PROVIDER=s3`<br>4. Configure endpoint, bucket name, and public URL. |
| **Relational Database** | Managed PostgreSQL (Neon PostgreSQL 18.6) | **ACTIVATED & CERTIFIED (Phase 15A)** | Fully live-validated against Neon Serverless PostgreSQL 18.6 in AWS Singapore. All 40 Flyway migrations cleanly applied; 59 tenant tables forced RLS verified; native `uuidv7()` certified; TLS 1.3 enforced. |
| **Authentication / OIDC** | Google Cloud Console OAuth 2.0 | Mock / Disabled | 1. Configure OAuth consent screen in Google Cloud Console.<br>2. Create OAuth 2.0 Client ID (Web Application).<br>3. Add Authorized Redirect URI: `https://api.domain.com/login/oauth2/code/google`.<br>4. Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`. |
| **AI Design Assistant** | OpenAI / Anthropic | Mock / Disabled | 1. Create OpenAI / Anthropic organization and API keys.<br>2. Set `SPRING_AI_OPENAI_API_KEY` or `SPRING_AI_ANTHROPIC_API_KEY`.<br>3. Configure active provider in Spring Boot. |
| **Transactional Email** | SendGrid / Resend / Postmark | Mock / Disabled | 1. Verify sending domain in email provider DNS.<br>2. Generate API Key.<br>3. Set `SPRING_SENDGRID_API_KEY` (or `RESEND_API_KEY` / `POSTMARK_SERVER_TOKEN`).<br>4. Set `EMAIL_FROM=notifications@domain.com`. |
| **Billing & Payments** | Stripe | Mock / Disabled | 1. Create Stripe account & activate live mode.<br>2. Configure product plans & price IDs.<br>3. Set `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY`.<br>4. Configure webhook listener for `invoice.paid`, `customer.subscription.updated`. |
| **Application Metrics & Monitoring** | Datadog / Prometheus / Sentry | Actuator local metrics | 1. Configure APM / metrics agent or collector.<br>2. Set `MANAGEMENT_METRICS_EXPORT_DATADOG_API_KEY` or Prometheus scrape endpoint.<br>3. Configure frontend and backend Sentry DSNs if desired. |

---

## 3. Configuration Template for Phase 15 Activation

When transitioning to Phase 15, copy this block into the deployment environment manager:

```env
# -----------------------------------------------------------------------------
# PHASE 15 PROVIDER ACTIVATION TEMPLATE
# Replace all placeholders with genuine credentials obtained during Phase 15.
# -----------------------------------------------------------------------------

# Storage (Cloudflare R2)
STORAGE_PROVIDER=s3
STORAGE_ENDPOINT=https://<CLOUDFLARE_ACCOUNT_ID>.r2.cloudflarestorage.com
STORAGE_BUCKET_NAME=elegance-production-assets
STORAGE_REGION=auto
STORAGE_ACCESS_KEY=<CF_R2_ACCESS_KEY_ID>
STORAGE_SECRET_KEY=<CF_R2_SECRET_ACCESS_KEY>
STORAGE_PUBLIC_URL=https://assets.elegance.interior.design

# Managed PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://<MANAGED_DB_HOST>:5432/interior_platform_prod?sslmode=require
SPRING_DATASOURCE_USERNAME=interior_app_user
SPRING_DATASOURCE_PASSWORD=<GENERATED_STRONG_PASSWORD>

# Google OIDC
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=<GOOGLE_CLIENT_ID>.apps.googleusercontent.com
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=<GOOGLE_CLIENT_SECRET>

# AI Assistance
SPRING_AI_OPENAI_API_KEY=sk-proj-<ACTUAL_OPENAI_KEY>

# Transactional Email (SendGrid / Resend)
SPRING_SENDGRID_API_KEY=SG.<ACTUAL_SENDGRID_KEY>
EMAIL_FROM=concierge@elegance.interior.design

# Billing & Stripe
STRIPE_SECRET_KEY=sk_live_<ACTUAL_STRIPE_KEY>
STRIPE_WEBHOOK_SECRET=whsec_<ACTUAL_WEBHOOK_SECRET>
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_live_<ACTUAL_STRIPE_PUBLIC_KEY>
```

---

## 4. Phase 15 Verification Protocol

Once credentials are provided:
1. Run `scripts/validate-production-environment.ps1` to ensure no syntax errors or placeholder values exist.
2. Boot the backend in staging with `SPRING_PROFILES_ACTIVE=production`.
3. Check actuator health: `/actuator/health` must report all subsystem components as `UP`.
4. Run integration tests against real sandbox endpoints before cutting over to live production.
