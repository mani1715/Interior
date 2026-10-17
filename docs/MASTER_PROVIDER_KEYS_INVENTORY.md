# Master Provider Keys & Configuration Inventory
## Elégance — Interior Designer Platform

This document serves as the single, authoritative reference for **all external provider keys, credentials, endpoints, and configuration parameters** required by the Elégance platform.

As requested, all platform code, fail-closed validation routines, and verification harnesses are structured to allow you to configure all provider keys **together in a single unified step at the end**.

---

## 1. Provider Status Overview

| Provider Domain | Target Service | Current Readiness Status | Required at Final Setup |
|---|---|---|---|
| **Relational Database** | **Neon PostgreSQL 18.6** | **COMPLETED & VERIFIED (Phase 15A)** | Already configured & running |
| **Object Storage** | **Cloudflare R2** | Architecture & 6-Op Contract Verified | **YES** |
| **Identity & Auth** | **Google Identity Services (OIDC)** | PKCE S256 & RFC 7517 Engine Ready | **YES** |
| **AI Visualizer** | **Google Vertex AI / Stability AI** | SSRF Sandbox & Quota Ledger Ready | **YES** |
| **Transactional Email** | **Resend (or AWS SES)** | Templating & Svix HMAC Webhook Ready | **YES** |
| **Commercial Billing** | **Stripe (or Razorpay)** | Idempotent Webhook & Quota Ready | **YES** |
| **Observability** | **OpenTelemetry / Sentry** | Health, Metrics & Tracing Ready | **OPTIONAL / RECOMMENDED** |
| **Production Domain** | **Cloudflare DNS & CDN** | Production Build & Routes Ready | **PHASE 16 (FINAL)** |

---

## 2. Complete Inventory by Provider

### Provider 1: Relational Database — Neon PostgreSQL (Phase 15A — COMPLETE)
*Already provisioned and verified live against AWS Singapore (`ap-southeast-1`).*
- `SPRING_DATASOURCE_URL`: `jdbc:postgresql://<HOST>:5432/neondb?sslmode=require`
- `SPRING_DATASOURCE_USERNAME`: `neondb_owner`
- `SPRING_DATASOURCE_PASSWORD`: `<MANAGED_IN_LOCAL_ENV>`

---

### Provider 2: Object Storage — Cloudflare R2 (Phase 15B)
Used for interior design photography, high-res floor plans, 3D renderings, and generated AI concept images.

| Variable Name | Required | Example / Expected Value | Where to Obtain |
|---|---|---|---|
| `MEDIA_STORAGE_PROVIDER` | **Yes** | `S3_COMPATIBLE` | Fixed platform constant |
| `S3_ENDPOINT` | **Yes** | `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` | Cloudflare Dashboard > R2 > Account ID |
| `S3_REGION` | **Yes** | `auto` | Always `auto` for Cloudflare R2 |
| `S3_ACCESS_KEY_ID` | **Yes** | `a1b2c3d4e5f6...` (32 characters) | Cloudflare Dashboard > R2 > Manage R2 API Tokens |
| `S3_SECRET_ACCESS_KEY` | **Yes** | `f8e7d6c5...` (64 characters) | Cloudflare Dashboard > R2 > Manage R2 API Tokens |
| `S3_BUCKET_PRIVATE` | **Yes** | `interior-platform-media-private` | Created in Cloudflare R2 |
| `S3_BUCKET_PUBLIC` | **Yes** | `interior-platform-media-public` | Created in Cloudflare R2 |
| `PUBLIC_MEDIA_BASE_URL` | No | `https://media.elegance.interior` | Configured in Phase 16 with custom domain |

#### Setup Instructions:
1. Go to [Cloudflare Dashboard](https://dash.cloudflare.com) > **R2 Object Storage**.
2. Click **Create bucket**:
   - Bucket 1: `interior-platform-media-private` (Quarantine and high-res originals; leave public access disabled).
   - Bucket 2: `interior-platform-media-public` (Public web derivatives).
3. Under **Manage R2 API Tokens**, click **Create API Token**:
   - Permissions: `Object Read & Write`.
   - Specify bucket(s): Select the two buckets created above.
   - Copy the **Access Key ID** and **Secret Access Key**.
4. In `interior-platform-media-private` > **Settings** > **CORS Policy**, add the JSON from `docs/PHASE_15B_R2_CORS_AND_PRESIGNED_UPLOAD_RUNBOOK.md`.

---

### Provider 3: Identity & Authentication — Google OIDC (Phase 15C)
Used for secure designer login and Google single sign-on.

| Variable Name | Required | Example / Expected Value | Where to Obtain |
|---|---|---|---|
| `GOOGLE_OIDC_ISSUER` | **Yes** | `https://accounts.google.com` | Standard Google OpenID discovery URL |
| `GOOGLE_CLIENT_ID` | **Yes** | `<PROJECT_ID>.apps.googleusercontent.com` | Google Cloud Console > APIs & Services > Credentials |
| `GOOGLE_CLIENT_SECRET` | **Yes** | `GOCSPX-...` | Google Cloud Console > APIs & Services > Credentials |
| `GOOGLE_REDIRECT_URI` | **Yes** | `http://localhost:8080/api/v1/login/oauth2/code/google` (local) or `https://api.elegance.interior/api/v1/login/oauth2/code/google` (production) | Configured in OAuth Client settings |

#### Setup Instructions:
1. Go to [Google Cloud Console](https://console.cloud.google.com) > **APIs & Services** > **Credentials**.
2. Configure OAuth Consent Screen (Internal or External, App name: `Elégance Interior Platform`).
3. Click **Create Credentials** > **OAuth client ID** > **Web application**.
4. Add **Authorized redirect URIs**:
   - `http://localhost:8080/api/v1/login/oauth2/code/google` (for local verification)
   - `https://api.elegance.interior/api/v1/login/oauth2/code/google` (for production)
5. Copy the **Client ID** and **Client Secret**.

---

### Provider 4: AI Visualizer & Room Concept Generator (Phase 15D)
Used for AI-assisted room redesigns, interior style transfer, and material visualization.

| Variable Name | Required | Example / Expected Value | Where to Obtain |
|---|---|---|---|
| `AI_VISUALIZER_ENABLED` | **Yes** | `true` | Platform toggle |
| `AI_PROVIDER` | **Yes** | `google-vertex` or `stability-ai` | Provider choice |
| `AI_API_KEY` | **Yes** | `AIzaSy...` (Google) or `sk-...` (Stability) | Google Cloud Vertex AI or Stability AI Console |
| `AI_ENDPOINT` | No | Provider canonical endpoint URL | Optional override |
| `AI_MODEL` | **Yes** | `imagen-3.0-generate-002` or `stable-diffusion-xl-1024-v1-0` | Allowlisted model identifier |
| `AI_DAILY_STUDIO_LIMIT` | No | `20` (default) | Daily quota cap per studio |

#### Setup Instructions:
- **For Google Vertex AI / Imagen 3**: Obtain API key from Google Cloud Console with Vertex AI API enabled.
- **For Stability AI**: Obtain API key from [platform.stability.ai](https://platform.stability.ai).

---

### Provider 5: Transactional Email — Resend or AWS SES (Phase 15E)
Used strictly for transactional communications: studio invites, client design presentation links, subscription receipts, security alerts.

| Variable Name | Required | Example / Expected Value | Where to Obtain |
|---|---|---|---|
| `EMAIL_PROVIDER` | **Yes** | `resend` (recommended) or `ses` | Choice of provider |
| `RESEND_API_KEY` | If Resend | `re_123456789...` | [resend.com/api-keys](https://resend.com/api-keys) |
| `EMAIL_FROM_ADDRESS` | **Yes** | `notifications@elegance.interior` | Verified sending domain |
| `EMAIL_WEBHOOK_SECRET` | If webhooks | `whsec_...` | Resend Webhook signing secret (Svix) |
| `AWS_SES_REGION` | If SES | `us-east-1` or `ap-southeast-1` | AWS SES region |

#### Setup Instructions:
1. Create a free account at [resend.com](https://resend.com).
2. Go to **Domains** > Add your domain (e.g. `elegance.interior`) and add the DNS records (DKIM, SPF).
3. Go to **API Keys** > **Create API Key** (Permission: `Sending access`).
4. Copy the API key.

---

### Provider 6: Commercial Billing — Stripe or Razorpay (Phase 15F)
Used for designer studio subscription plans (STANDARD, PREMIUM, PRO) and add-on storage quota.

| Variable Name | Required | Example / Expected Value | Where to Obtain |
|---|---|---|---|
| `BILLING_PROVIDER` | **Yes** | `stripe` (recommended) or `razorpay` | Provider choice |
| `STRIPE_API_KEY` | If Stripe | `sk_test_...` or `sk_live_...` | Stripe Dashboard > Developers > API keys |
| `STRIPE_WEBHOOK_SECRET` | If Stripe | `whsec_...` | Stripe Dashboard > Developers > Webhooks |
| `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` | If Stripe | `pk_test_...` or `pk_live_...` | Stripe Dashboard > Developers > API keys |
| `RAZORPAY_KEY_ID` | If Razorpay | `rzp_test_...` or `rzp_live_...` | Razorpay Dashboard > Settings > API Keys |
| `RAZORPAY_KEY_SECRET` | If Razorpay | Secret string | Razorpay Dashboard > Settings > API Keys |
| `RAZORPAY_WEBHOOK_SECRET` | If Razorpay | Webhook secret | Razorpay Dashboard > Settings > Webhooks |

#### Setup Instructions:
1. In [Stripe Dashboard](https://dashboard.stripe.com) (Test Mode):
2. Copy **Publishable key** (`pk_test_...`) and **Secret key** (`sk_test_...`).
3. Under **Webhooks**, add endpoint: `https://<DOMAIN>/api/v1/billing/webhook/stripe`.
4. Copy the **Signing secret** (`whsec_...`).

---

### Provider 7: Observability, Metrics & Sentry (Phase 15G — OPTIONAL)
Used for real-time error tracking and telemetry.

| Variable Name | Required | Example / Expected Value |
|---|---|---|
| `SENTRY_DSN` | No | `https://<KEY>@o0.ingest.sentry.io/<PROJECT_ID>` |
| `NEXT_PUBLIC_SENTRY_DSN` | No | `https://<KEY>@o0.ingest.sentry.io/<PROJECT_ID>` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | No | `http://localhost:4318` or Datadog/Grafana OTLP collector |

---

## 3. Unified Local Configuration Template (`.env.local`)

When you are ready to configure the platform at the end, create or update `.env.local` in the project root with the block below.

> [!IMPORTANT]
> `.env.local` is strictly ignored by Git (`.gitignore`) and will NEVER be committed or leaked.

```bash
# ==============================================================================
# ELÉGANCE INTERIOR DESIGN PLATFORM -- UNIFIED PROVIDER KEYS (.env.local)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. MANAGED POSTGRESQL (NEON) -- PHASE 15A (ALREADY ACTIVE)
# ------------------------------------------------------------------------------
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-crimson-fog-b3t603ml-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
SPRING_DATASOURCE_USERNAME=neondb_owner
SPRING_DATASOURCE_PASSWORD=npg_WsN70GdLwvte

# ------------------------------------------------------------------------------
# 2. CLOUDFLARE R2 OBJECT STORAGE -- PHASE 15B
# ------------------------------------------------------------------------------
MEDIA_STORAGE_PROVIDER=S3_COMPATIBLE
S3_ENDPOINT=https://<YOUR_CLOUDFLARE_ACCOUNT_ID>.r2.cloudflarestorage.com
S3_REGION=auto
S3_ACCESS_KEY_ID=<YOUR_R2_ACCESS_KEY_ID>
S3_SECRET_ACCESS_KEY=<YOUR_R2_SECRET_ACCESS_KEY>
S3_BUCKET_PRIVATE=interior-platform-media-private
S3_BUCKET_PUBLIC=interior-platform-media-public
PUBLIC_MEDIA_BASE_URL=https://media.elegance.interior

# ------------------------------------------------------------------------------
# 3. GOOGLE OIDC IDENTITY & AUTH -- PHASE 15C
# ------------------------------------------------------------------------------
GOOGLE_OIDC_ISSUER=https://accounts.google.com
GOOGLE_CLIENT_ID=<YOUR_GOOGLE_CLIENT_ID>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<YOUR_GOOGLE_CLIENT_SECRET>
GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/login/oauth2/code/google

# ------------------------------------------------------------------------------
# 4. AI VISUALIZER -- PHASE 15D
# ------------------------------------------------------------------------------
AI_VISUALIZER_ENABLED=true
AI_PROVIDER=google-vertex
AI_API_KEY=<YOUR_AI_API_KEY>
AI_MODEL=imagen-3.0-generate-002
AI_DAILY_STUDIO_LIMIT=20

# ------------------------------------------------------------------------------
# 5. TRANSACTIONAL EMAIL (RESEND) -- PHASE 15E
# ------------------------------------------------------------------------------
EMAIL_PROVIDER=resend
RESEND_API_KEY=re_<YOUR_RESEND_API_KEY>
EMAIL_FROM_ADDRESS=notifications@elegance.interior
EMAIL_WEBHOOK_SECRET=whsec_<YOUR_RESEND_WEBHOOK_SECRET>

# ------------------------------------------------------------------------------
# 6. COMMERCIAL BILLING (STRIPE) -- PHASE 15F
# ------------------------------------------------------------------------------
BILLING_PROVIDER=stripe
STRIPE_API_KEY=sk_test_<YOUR_STRIPE_SECRET_KEY>
STRIPE_WEBHOOK_SECRET=whsec_<YOUR_STRIPE_WEBHOOK_SECRET>
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_<YOUR_STRIPE_PUBLISHABLE_KEY>

# ------------------------------------------------------------------------------
# 7. OBSERVABILITY & SENTRY (OPTIONAL) -- PHASE 15G
# ------------------------------------------------------------------------------
# SENTRY_DSN=https://<KEY>@o0.ingest.sentry.io/<PROJECT_ID>
# NEXT_PUBLIC_SENTRY_DSN=https://<KEY>@o0.ingest.sentry.io/<PROJECT_ID>
```

---

## 4. One-Click Unified Verification Script

Once you paste your credentials into `.env.local`, you can run a single command to verify the entire platform stack in one shot:

```powershell
# In PowerShell:
powershell -ExecutionPolicy Bypass -File scripts/verify-all-providers.ps1
```

*(This runner will execute the verification checks across Neon DB, R2 Storage, Google OIDC, AI Visualizer, Email, and Stripe sequentially and report a comprehensive green scorecard.)*
