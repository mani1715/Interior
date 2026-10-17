# Elégance — Final Provider Keys Required & Configuration Schema

## Executive Notice
This document outlines the exact environment variable schema required for final external cloud provider activation.

> [!IMPORTANT]
> **Zero Plaintext Secrets**: This document contains strictly variable names and setup instructions. Never commit real credentials, passwords, or tokens to source control. Configure these variables locally in `.env.local` or through your hosting environment's secret manager.

---

## 1. Managed Database (PostgreSQL 18)
- **Status:** **ACTIVE & LIVE VALIDATED (Phase 15A)**
- **Provider:** Neon Serverless PostgreSQL (PostgreSQL 18.6)
- **Direct Application Connection:**
  ```env
  SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-direct-host>:5432/<database>?sslmode=require
  SPRING_DATASOURCE_USERNAME=<database-user>
  SPRING_DATASOURCE_PASSWORD=<database-password>
  ```
- *Note:* If running on existing verified Neon database (`ep-crimson-fog-b3t603ml`), these variables are already configured in local `.env.local`.

---

## 2. Object Storage (Cloudflare R2)
- **Status:** **IMPLEMENTATION COMPLETE / LIVE VALIDATION DEFERRED**
- **Role:** High-resolution interior architecture photography, 3D renderings, floor plans, and client moodboard storage.
- **Provider:** Cloudflare R2 (S3-compatible API).
- **Required Variables:**
  ```env
  CLOUDFLARE_R2_ACCOUNT_ID=
  CLOUDFLARE_R2_ACCESS_KEY_ID=
  CLOUDFLARE_R2_SECRET_ACCESS_KEY=
  CLOUDFLARE_R2_BUCKET_NAME=
  CLOUDFLARE_R2_PUBLIC_URL=
  ```
- **How to Obtain:**
  1. In Cloudflare Dashboard, navigate to **R2** > **Overview**.
  2. Create an API Token with **Object Read & Write** permissions for the dedicated bucket.
  3. Copy the Account ID, Access Key ID, and Secret Access Key.

---

## 3. Social & Single Sign-On (Google OIDC)
- **Status:** **IMPLEMENTATION COMPLETE / LIVE VALIDATION DEFERRED**
- **Role:** Federated OpenID Connect single sign-on for interior design studio owners, staff, and clients.
- **Provider:** Google Identity Services (OAuth 2.0 / OpenID Connect).
- **Required Variables:**
  ```env
  OIDC_GOOGLE_CLIENT_ID=
  OIDC_GOOGLE_CLIENT_SECRET=
  OIDC_GOOGLE_REDIRECT_URI=http://localhost:3000/auth/callback
  ```
- **How to Obtain:**
  1. Open [Google Cloud Console](https://console.cloud.google.com/) > **APIs & Services** > **Credentials**.
  2. Create an **OAuth 2.0 Client ID** (Web application).
  3. Add Authorized Redirect URIs matching your local and production callback endpoints.

---

## 4. AI Visualizer & Inpainting (Provider Decision Required)
- **Status:** **IMPLEMENTATION COMPLETE / PROVIDER NOT YET SELECTED**
- **Role:** Interior design room transformation, color matching, finish inpainting, and conceptual styling.
- **Architecture:** Provider-neutral image diffusion SPI (`ConfigurableAiImageProvider`).
- **Configuration Schema:**
  ```env
  # Set provider choice: "none", "stability", "vertex", "replicate", or custom
  INTERIOR_AI_PROVIDER=none
  INTERIOR_AI_API_KEY=
  INTERIOR_AI_ENDPOINT=
  INTERIOR_AI_MODEL=interior-diffusion-v1
  ```

### Conditional Variables by Selected Vendor:
- **Option A — Stability AI:**
  ```env
  INTERIOR_AI_PROVIDER=stability
  INTERIOR_AI_API_KEY=<stability-api-key>
  INTERIOR_AI_MODEL=stable-diffusion-xl-1024-v1-0
  ```
- **Option B — Google Vertex AI / Imagen 3:**
  ```env
  INTERIOR_AI_PROVIDER=vertex
  INTERIOR_AI_API_KEY=<gcp-service-account-oauth-token>
  INTERIOR_AI_ENDPOINT=https://<region>-aiplatform.googleapis.com
  INTERIOR_AI_MODEL=imagegeneration@006
  ```
- **Option C — Replicate / Custom Diffusion Endpoint:**
  ```env
  INTERIOR_AI_PROVIDER=replicate
  INTERIOR_AI_API_KEY=<replicate-api-token>
  INTERIOR_AI_ENDPOINT=https://api.replicate.com/v1
  INTERIOR_AI_MODEL=<owner-selected-model-version>
  ```

---

## 5. Transactional Email & Notifications (Provider Decision Required)
- **Status:** **IMPLEMENTATION COMPLETE / PROVIDER NOT YET SELECTED**
- **Role:** Studio team invitations, client review verification links, inquiry alerts, and billing receipts.
- **Architecture:** Provider-neutral communication SPI (`EmailProvider`).
- **Configuration Schema:**
  ```env
  # Set email provider: "disabled", "resend", or "ses"
  APP_EMAIL_PROVIDER=disabled
  APP_EMAIL_FROM_ADDRESS=notifications@elegance.design
  APP_EMAIL_FROM_NAME=Elégance Interior Platform
  ```

### Conditional Variables by Selected Vendor:
- **Option A — Resend:**
  ```env
  APP_EMAIL_PROVIDER=resend
  APP_EMAIL_RESEND_API_KEY=<resend-api-key>
  APP_EMAIL_RESEND_WEBHOOK_SECRET=<svix-webhook-secret>
  ```
- **Option B — Amazon SES:**
  ```env
  APP_EMAIL_PROVIDER=ses
  AWS_SES_REGION=ap-south-1
  AWS_SES_ACCESS_KEY_ID=<aws-access-key-id>
  AWS_SES_SECRET_ACCESS_KEY=<aws-secret-access-key>
  APP_EMAIL_SES_SNS_TOPIC_ARN=<ses-sns-topic-arn>
  ```

---

## 6. Commercial Billing & Invoicing (Provider Decision Required)
- **Status:** **IMPLEMENTATION COMPLETE / PROVIDER NOT YET SELECTED**
- **Role:** Designer studio subscription tiers (Standard, Premium, Pro) and project entitlement billing.
- **Architecture:** Provider-neutral billing SPI (`BillingProvider`).
- **Configuration Schema:**
  ```env
  # Set billing provider: "disabled", "razorpay", or "stripe"
  APP_BILLING_PROVIDER=disabled
  ```

### Conditional Variables by Selected Vendor:
- **Option A — Razorpay (Primary for Domestic / India Commercial Launch):**
  ```env
  APP_BILLING_PROVIDER=razorpay
  RAZORPAY_KEY_ID=<razorpay-key-id>
  RAZORPAY_KEY_SECRET=<razorpay-key-secret>
  RAZORPAY_WEBHOOK_SECRET=<razorpay-webhook-secret>
  ```
- **Option B — Stripe (Primary for International / Global Commercial Launch):**
  ```env
  APP_BILLING_PROVIDER=stripe
  STRIPE_PUBLISHABLE_KEY=<stripe-publishable-key>
  STRIPE_SECRET_KEY=<stripe-secret-key>
  STRIPE_WEBHOOK_SECRET=<stripe-webhook-signing-secret>
  ```

---

## 7. Observability & APM (Provider Decision Required)
- **Status:** **IMPLEMENTATION COMPLETE / DECISION REQUIRED**
- **Role:** Centralized logging, distributed tracing, error tracking, and performance alerting.
- **Architecture:** Built-in Spring Boot Actuator with `/actuator/prometheus` metrics endpoint and high-performance structured JSON logging (`JsonLoggingLayout`). External APM connection is optional.

### Conditional Variables by Selected APM Provider:
- **Option A — Standard Built-In Metrics (Default / No External Account Required):**
  - Uses native Prometheus scraping and Docker container log drivers. Zero external keys required.
- **Option B — Sentry Error Tracking (If Selected):**
  ```env
  NEXT_PUBLIC_SENTRY_DSN=<frontend-sentry-dsn>
  SENTRY_DSN=<backend-sentry-dsn>
  SENTRY_AUTH_TOKEN=<sentry-auth-token>
  ```
- **Option C — OpenTelemetry / OTLP Collector (Datadog / Grafana Tempo / New Relic):**
  ```env
  MANAGEMENT_OTLP_METRICS_EXPORT_URL=http://<otel-collector>:4318/v1/metrics
  MANAGEMENT_OTLP_TRACING_ENDPOINT=http://<otel-collector>:4318/v1/traces
  ```

---

## Summary of Decisions Needed Prior to Live Cloud Activation

| Pillar | Selected Provider | Alternatives Available | Decision Timing |
| :--- | :--- | :--- | :--- |
| **1. Database** | **Neon PostgreSQL 18.6** | None (PostgreSQL 18 is canonical) | **COMPLETED (Phase 15A)** |
| **2. Storage** | **Cloudflare R2** | None (S3-compatible R2 selected) | Keys needed at final activation |
| **3. SSO / OIDC** | **Google Identity** | None (Google OIDC selected) | Keys needed at final activation |
| **4. AI Visualizer** | *Decision Required* | Vertex / Imagen 3 vs Stability AI vs Replicate | Owner selects vendor prior to Phase 15D |
| **5. Email** | *Decision Required* | Resend vs AWS SES | Owner selects vendor prior to Phase 15E |
| **6. Billing** | *Decision Required* | Razorpay vs Stripe | Owner selects vendor prior to Phase 15F |
| **7. Observability**| *Decision Required* | Native Prometheus vs Sentry vs OTLP | Owner selects vendor prior to Phase 15G |
