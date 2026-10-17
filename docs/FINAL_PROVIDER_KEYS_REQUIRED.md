# Final Provider Keys Required Inventory
## Elégance — Interior Designer Platform

This document catalogs strictly the environment variable names, acquisition locations, and security classification for external cloud service providers.

> [!IMPORTANT]
> **NO VALUES ARE STORED IN THIS FILE.**
> All external credential inputs are deferred to the final provider activation phase.

---

### 1. Managed PostgreSQL
*Database storage engine for multi-tenant workspace data, Row-Level Security, and audit trails.*

| VARIABLE | WHERE TO GET IT | SERVER/CLIENT | SECRET? | WHEN WE WILL ADD IT |
| :--- | :--- | :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | Managed Cloud PostgreSQL Dashboard (Neon / RDS) | Server | Yes (Connection URL) | Already Configured / Final Staging |
| `SPRING_DATASOURCE_USERNAME` | Managed Cloud PostgreSQL Dashboard | Server | No | Already Configured / Final Staging |
| `SPRING_DATASOURCE_PASSWORD` | Managed Cloud PostgreSQL Dashboard | Server | Yes | Already Configured / Final Staging |

---

### 2. Cloudflare R2 Object Storage
*S3-compatible object storage for interior design photography, CAD renderings, and generated AI concept imagery.*

| VARIABLE | WHERE TO GET IT | SERVER/CLIENT | SECRET? | WHEN WE WILL ADD IT |
| :--- | :--- | :--- | :--- | :--- |
| `MEDIA_STORAGE_PROVIDER` | Fixed constant (`S3_COMPATIBLE`) | Server | No | Final Provider Activation |
| `S3_ENDPOINT` | Cloudflare Dashboard > R2 > Account ID | Server | No | Final Provider Activation |
| `S3_REGION` | Fixed constant (`auto`) | Server | No | Final Provider Activation |
| `S3_ACCESS_KEY_ID` | Cloudflare Dashboard > R2 > Manage R2 API Tokens | Server | Yes (Access Key) | Final Provider Activation |
| `S3_SECRET_ACCESS_KEY` | Cloudflare Dashboard > R2 > Manage R2 API Tokens | Server | Yes (Secret Key) | Final Provider Activation |
| `S3_BUCKET_PRIVATE` | Cloudflare Dashboard > R2 > Create Bucket (`interior-platform-media-private`) | Server | No | Final Provider Activation |
| `S3_BUCKET_PUBLIC` | Cloudflare Dashboard > R2 > Create Bucket (`interior-platform-media-public`) | Server | No | Final Provider Activation |
| `PUBLIC_MEDIA_BASE_URL` | Cloudflare Custom Domain (Phase 16) | Server | No | Final Production Deployment |

---

### 3. Google Identity Services (OIDC)
*OpenID Connect provider for secure single sign-on and designer authentication.*

| VARIABLE | WHERE TO GET IT | SERVER/CLIENT | SECRET? | WHEN WE WILL ADD IT |
| :--- | :--- | :--- | :--- | :--- |
| `GOOGLE_OIDC_ISSUER` | Standard discovery endpoint (`https://accounts.google.com`) | Server | No | Final Provider Activation |
| `GOOGLE_CLIENT_ID` | Google Cloud Console > APIs & Services > Credentials | Server & Client | No | Final Provider Activation |
| `GOOGLE_CLIENT_SECRET` | Google Cloud Console > APIs & Services > Credentials | Server | Yes | Final Provider Activation |
| `GOOGLE_REDIRECT_URI` | Configured redirect path in Google OAuth settings | Server | No | Final Provider Activation |

---

### 4. AI Visualizer & Room Concept Generator
*Frontier vision and diffusion model engine for interior room redesigns and style transfers.*

| VARIABLE | WHERE TO GET IT | SERVER/CLIENT | SECRET? | WHEN WE WILL ADD IT |
| :--- | :--- | :--- | :--- | :--- |
| `AI_VISUALIZER_ENABLED` | Platform flag (`true`) | Server | No | Final Provider Activation |
| `AI_PROVIDER` | Provider selection (`google-vertex` or `stability-ai`) | Server | No | Final Provider Activation |
| `AI_API_KEY` | Google Cloud Console (Vertex AI) or Stability AI Console | Server | Yes | Final Provider Activation |
| `AI_ENDPOINT` | Provider documentation (optional override) | Server | No | Final Provider Activation |
| `AI_MODEL` | Provider allowlisted model (`imagen-3.0-generate-002`) | Server | No | Final Provider Activation |
| `AI_DAILY_STUDIO_LIMIT` | Studio quota configuration (default: `20`) | Server | No | Final Provider Activation |

---

### 5. Transactional Email
*Strictly transactional communications for studio invites, client review links, and security alerts.*

| VARIABLE | WHERE TO GET IT | SERVER/CLIENT | SECRET? | WHEN WE WILL ADD IT |
| :--- | :--- | :--- | :--- | :--- |
| `EMAIL_PROVIDER` | Provider choice (`resend` or `ses`) | Server | No | Final Provider Activation |
| `RESEND_API_KEY` | Resend Dashboard > API Keys | Server | Yes | Final Provider Activation |
| `EMAIL_FROM_ADDRESS` | Verified domain in Resend/SES (`notifications@...`) | Server | No | Final Provider Activation |
| `EMAIL_WEBHOOK_SECRET` | Resend Dashboard > Webhooks (Svix signing secret) | Server | Yes | Final Provider Activation |
| `AWS_SES_REGION` | AWS Management Console > SES (if using SES) | Server | No | Final Provider Activation |

---

### 6. Commercial Payments & Subscriptions
*Commercial billing gateway for studio plans (STANDARD, PREMIUM, PRO) and storage upgrades.*

| VARIABLE | WHERE TO GET IT | SERVER/CLIENT | SECRET? | WHEN WE WILL ADD IT |
| :--- | :--- | :--- | :--- | :--- |
| `BILLING_PROVIDER` | Provider choice (`stripe` or `razorpay`) | Server | No | Final Provider Activation |
| `STRIPE_API_KEY` | Stripe Dashboard > Developers > API keys | Server | Yes (Secret Key) | Final Provider Activation |
| `STRIPE_WEBHOOK_SECRET` | Stripe Dashboard > Developers > Webhooks | Server | Yes | Final Provider Activation |
| `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` | Stripe Dashboard > Developers > API keys | Client | No (Publishable) | Final Provider Activation |
| `RAZORPAY_KEY_ID` | Razorpay Dashboard > Settings > API Keys (if using Razorpay) | Server & Client | No (Key ID) | Final Provider Activation |
| `RAZORPAY_KEY_SECRET` | Razorpay Dashboard > Settings > API Keys | Server | Yes | Final Provider Activation |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay Dashboard > Settings > Webhooks | Server | Yes | Final Provider Activation |

---

### 7. Observability & Sentry
*Centralized application performance monitoring, telemetry, and error reporting.*

| VARIABLE | WHERE TO GET IT | SERVER/CLIENT | SECRET? | WHEN WE WILL ADD IT |
| :--- | :--- | :--- | :--- | :--- |
| `SENTRY_DSN` | Sentry Dashboard > Project Settings > Client Keys (DSN) | Server | No | Optional / Final Provider Activation |
| `NEXT_PUBLIC_SENTRY_DSN` | Sentry Dashboard > Project Settings > Client Keys (DSN) | Client | No | Optional / Final Provider Activation |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry Collector / Grafana / Datadog endpoint | Server | No | Optional / Final Provider Activation |
| `OTEL_EXPORTER_OTLP_HEADERS` | OpenTelemetry Collector Authentication Header | Server | Yes | Optional / Final Provider Activation |
