# Elégance — Final Provider Deferred Validation Register

## Executive Overview
Under the **Pre-Provider Production Certification** directive, all external third-party cloud provider credentials and live activations (Cloudflare R2, Google OAuth, AI Provider, Transactional Email, Commercial Billing, Production Monitoring/APM, and Domain Provisioning) are intentionally deferred to a unified, sequential activation phase.

This register formally documents each provider pillar, recording its:
1. Architectural role and integration points in the Elégance Interior Designer Platform
2. Codebase implementation status and readiness
3. Local/mock/contract verification results
4. Exact deferral justification and required credentials
5. Pre-flight verification checklist for final live activation

---

## Pillar 1: Managed PostgreSQL 18 (Neon)
- **Role:** Primary system of record, relational persistence, multi-tenant row-level security (RLS), Flyway schema migrations, and native `uuidv7()` generation.
- **Provider:** Neon Serverless PostgreSQL (Engine: PostgreSQL 18.6).
- **Direct Application Host:** `ep-crimson-fog-b3t603ml.c-4.ap-southeast-1.aws.neon.tech` (AWS ap-southeast-1).
- **Pooled Host (Edge/Serverless):** `ep-crimson-fog-b3t603ml-pooler.c-4.ap-southeast-1.aws.neon.tech`.
- **Codebase Implementation:** 100% complete. HikariCP pool, tenant context propagation via PostgreSQL session variables (`app.current_tenant_id`), advisory locking, full Flyway V001–V040 migrations.
- **Verification Status:** **ACTIVE & LIVE VERIFIED (Phase 15A)**. Evidence documented in `docs/PHASE_15A_MANAGED_POSTGRESQL_EVIDENCE.md`.
- **Deferral Status:** None. Fully operational in AWS Singapore.

---

## Pillar 2: Object Storage (Cloudflare R2)
- **Role:** S3-compatible media asset storage for interior design portfolios, floor plans, room 3D renders, mood boards, client proposal attachments, and designer avatars.
- **Provider:** Cloudflare R2 (S3-compatible API).
- **Codebase Implementation:** 100% complete (`StorageService`, `S3StorageService`, presigned PUT generation, ClamAV antivirus quarantine gate, EXIF stripping, canonical derivative generation).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Local mock/filesystem test passing.
  - Opt-in live test (`R2IntegrationLiveTest`) cleanly skipped when `CLOUDFLARE_R2_ACCESS_KEY_ID` is absent.
  - Quota checks, file validation (PNG, JPEG, WebP, PDF), and MIME verification verified across unit and integration tests.
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.
- **Required Credentials at Activation:**
  - `CLOUDFLARE_R2_ACCOUNT_ID`
  - `CLOUDFLARE_R2_ACCESS_KEY_ID`
  - `CLOUDFLARE_R2_SECRET_ACCESS_KEY`
  - `CLOUDFLARE_R2_BUCKET_NAME`
  - `CLOUDFLARE_R2_PUBLIC_URL`

---

## Pillar 3: Single Sign-On / OIDC (Google OAuth 2.0)
- **Role:** Federated authentication and single sign-on for interior designers, home remodeling clients, and studio staff.
- **Provider:** Google Identity Services (OAuth 2.0 / OpenID Connect).
- **Codebase Implementation:** 100% complete (`OidcService`, PKCE token exchange, user identity linking, email domain validation). Note: Authentication is exclusively OIDC-based with server-side sessions; no local password authentication or password reset exists by design.
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - UI displays graceful fallback or login buttons disabled with clear tooltips when unconfigured.
  - Dev persona sandbox provides deterministic authentication testing across roles in development and test environments.
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.
- **Required Credentials at Activation:**
  - `OIDC_GOOGLE_CLIENT_ID`
  - `OIDC_GOOGLE_CLIENT_SECRET`
  - `OIDC_GOOGLE_REDIRECT_URI`

---

## Pillar 4: AI Visualizer & Room Inpainting (Provider Decision Required)
- **Role:** Interior design room transformation, color matching, finish inpainting, and conceptual styling.
- **Architecture:** Provider-neutral image generation SPI (`ConfigurableAiImageProvider`).
- **Supported Adapters:** Google Vertex AI / Imagen 3, Stability AI, Replicate, or custom diffusion HTTP endpoint.
- **Codebase Implementation:** 100% complete (Structured prompt validation, reference image conditioning up to 4 images, 20 generations/day quota, SSRF output protection, and in-memory malware check).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Mock AI response provider simulates concept generations in test profiles.
  - Frontend AI assistant components render graceful "Configuration Pending" badge if unconfigured.
- **Deferral Status:** **LIVE VALIDATION DEFERRED — PROVIDER NOT YET SELECTED**.
- **Required Credentials at Activation (Conditional by Selected Vendor):**
  - `INTERIOR_AI_PROVIDER` (`stability`, `vertex`, `replicate`, or custom)
  - `INTERIOR_AI_API_KEY`
  - `INTERIOR_AI_ENDPOINT` (if applicable)
  - `INTERIOR_AI_MODEL`

---

## Pillar 5: Transactional Email & Notifications (Provider Decision Required)
- **Role:** Interior design project milestone updates, client consultation invitations, studio member invitations, review verification links, and billing alerts.
- **Architecture:** Provider-neutral communication SPI (`EmailProvider`).
- **Supported Adapters:** Resend REST API, AWS SES v2 SDK, Disabled (safe default), Fake (test mode).
- **Codebase Implementation:** 100% complete (HTML templates with Elégance luxury styling, OWASP HTML escaping, CRLF injection defense, Svix/SNS webhook signature validators).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Local memory/log email sink captures outgoing emails during testing without outbound network calls.
  - 100% of email rendering tests and template variable replacements pass in Vitest and Spring Boot tests.
- **Deferral Status:** **LIVE VALIDATION DEFERRED — PROVIDER NOT YET SELECTED**.
- **Required Credentials at Activation (Conditional by Selected Vendor):**
  - `APP_EMAIL_PROVIDER` (`resend` or `ses`)
  - If Resend: `APP_EMAIL_RESEND_API_KEY`, `APP_EMAIL_RESEND_WEBHOOK_SECRET`
  - If SES: `AWS_SES_REGION`, `AWS_SES_ACCESS_KEY_ID`, `AWS_SES_SECRET_ACCESS_KEY`, `APP_EMAIL_SES_SNS_TOPIC_ARN`

---

## Pillar 6: Commercial Billing & Payments (Provider Decision Required)
- **Role:** Designer studio subscription tiers (Standard, Premium, Pro), consultation retainers, and project milestone payments.
- **Architecture:** Provider-neutral billing SPI (`BillingProvider`).
- **Supported Adapters:** Razorpay (domestic India launch candidate), Stripe (global launch candidate), Disabled (safe default), Fake (test mode).
- **Codebase Implementation:** 100% complete (Server-authoritative checkout sessions, zero client price trust, open-redirect defense, timestamped webhook replay defense, single active subscription guarantee, and zero data loss on downgrade).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Webhook payload HMAC-SHA256 signature verification contract tested.
  - Disabled billing provider renders truthful boundary modal without unhandled errors.
- **Deferral Status:** **LIVE VALIDATION DEFERRED — PROVIDER NOT YET SELECTED**.
- **Required Credentials at Activation (Conditional by Selected Vendor):**
  - `APP_BILLING_PROVIDER` (`razorpay` or `stripe`)
  - If Razorpay: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`
  - If Stripe: `STRIPE_PUBLISHABLE_KEY`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`

---

## Pillar 7: Monitoring, APM & Observability (Provider Decision Required)
- **Role:** High-performance structured logging, request correlation (`X-Request-Id`), Actuator metrics, and optional application performance monitoring.
- **Architecture:** Built-in Spring Boot Actuator (`/actuator/prometheus`) and zero-dependency `JsonLoggingLayout` with PII and secret masking.
- **Supported Options:** Native Prometheus scraping (built-in default, no external keys needed), Sentry, or OpenTelemetry OTLP collector.
- **Codebase Implementation:** 100% complete.
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Backend Actuator health, readiness, and liveness endpoints verified UP.
  - Passive frontend error boundary displays sanitized correlation ID.
- **Deferral Status:** **LIVE VALIDATION DEFERRED — APM VENDOR DECISION OPTIONAL**.
- **Required Credentials at Activation (Conditional):**
  - If Sentry: `NEXT_PUBLIC_SENTRY_DSN`, `SENTRY_DSN`, `SENTRY_AUTH_TOKEN`
  - If OTLP: `MANAGEMENT_OTLP_METRICS_EXPORT_URL`, `MANAGEMENT_OTLP_TRACING_ENDPOINT`

---

## Pillar 8: Custom Domain & Production Ingress
- **Role:** Production ingress, SSL/TLS certificate termination, HTTP/2 edge routing, strict CORS and CSP enforcement.
- **Provider:** Cloudflare / Vercel / AWS.
- **Verification Status:** **CHECKLIST PREPARED**. `docs/FINAL_DOMAIN_ACTIVATION_CHECKLIST.md` contains the full pre-launch DNS, SSL, and security policy protocol.
- **Deferral Status:** **LIVE VALIDATION DEFERRED (Phase 16)**.

---

## Summary Matrix

| Pillar | Candidate / Provider | Architecture Role | Code Ready | Tests Passing | Live Status | Action at Activation |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Database** | Neon PostgreSQL 18.6 | Multi-tenant relational store | YES | YES (751 backend) | **ACTIVE (Phase 15A)** | Connection pool configured |
| **2. Storage** | Cloudflare R2 | Portfolio & design uploads | YES | YES (Mock/Contract) | DEFERRED | Provide R2 credentials |
| **3. SSO / Auth** | Google OAuth 2.0 | Federated identity | YES | YES (Mock/Contract) | DEFERRED | Provide Google Client ID |
| **4. AI Assistant**| *Decision Required* (Vertex/Stability/Replicate) | Interior styling & inpainting | YES | YES (Mock/Contract) | DEFERRED | Select vendor & provide API Key |
| **5. Email** | *Decision Required* (Resend / AWS SES) | Client alerts & receipts | YES | YES (Log Sink) | DEFERRED | Select vendor & provide Key |
| **6. Payments** | *Decision Required* (Razorpay / Stripe) | Design fees & subscriptions | YES | YES (Mock/Contract) | DEFERRED | Select vendor & provide Keys |
| **7. Monitoring**| *Decision Required* (Prometheus / Sentry / OTLP) | Error tracking & APM | YES | YES (Passive Mode) | DEFERRED | Select vendor (optional) |
| **8. Ingress** | Custom Domain / CDN | Edge routing & SSL | YES | YES (Checklist ready) | DEFERRED | Configure DNS in Phase 16 |
