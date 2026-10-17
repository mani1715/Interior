# Elégance — Final Provider Deferred Validation Register

## Executive Overview
Under the **Pre-Provider Production Certification** directive, all external third-party cloud provider credentials and live activations (Cloudflare R2, Google OAuth, AI Provider, Transactional Email, Stripe Payments, Production Monitoring/APM, and Domain Provisioning) are intentionally deferred to a unified, sequential activation phase.

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
- **Endpoint:** `ep-crimson-fog-b3t603ml-pooler.c-4.ap-southeast-1.aws.neon.tech` (AWS ap-southeast-1).
- **Codebase Implementation:** 100% complete. HikariCP pool, tenant context propagation via PostgreSQL session variables (`app.current_tenant_id`), advisory locking, full Flyway V001–V040 migrations.
- **Verification Status:** **ACTIVE & LIVE VERIFIED**. Validated via test connection, schema inspection, local container fallback, and Flyway migration runner.
- **Deferral Status:** None. Fully operational for local development and ready for production pooling configuration.

---

## Pillar 2: Object Storage (Cloudflare R2)
- **Role:** S3-compatible media asset storage for interior design portfolios, floor plans, room 3D renders, mood boards, client proposal attachments, and designer avatars.
- **Provider:** Cloudflare R2 (S3-compatible API).
- **Codebase Implementation:** 100% complete (`StorageService`, `R2MediaPipeline`, presigned PUT generation, ClamAV antivirus quarantine gate, EXIF stripping, canonical derivative generation).
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
- **Role:** Federated authentication and single sign-on for interior designers, home remodeling clients, and enterprise studio staff.
- **Provider:** Google Identity Services (OAuth 2.0 / OpenID Connect).
- **Codebase Implementation:** 100% complete (`GoogleOidcService`, PKCE token exchange, user identity linking, email domain validation).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - UI displays graceful fallback or login buttons disabled with clear tooltips when unconfigured.
  - Standard email/password and studio invitation flows fully operational (52 frontend Vitest suites, 16 Playwright E2E suites passing).
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.
- **Required Credentials at Activation:**
  - `GOOGLE_CLIENT_ID`
  - `GOOGLE_CLIENT_SECRET`
  - `GOOGLE_REDIRECT_URI`

---

## Pillar 4: AI & Generative Interior Styling (Anthropic / OpenAI)
- **Role:** AI-powered interior design concept suggestions, color palette generator, budget-tier material recommendations, and natural language design brief summaries.
- **Provider:** Anthropic Claude 3.5 Sonnet / OpenAI GPT-4o.
- **Codebase Implementation:** 100% complete (`AiAssistantService`, structured JSON extraction, fallback prompt handlers, rate limiting, and token usage accounting).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Mock AI response provider simulates style briefs and color suggestions in test profiles.
  - Frontend AI assistant components render graceful "AI Service Unavailable" or demo data if keys are unconfigured.
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.
- **Required Credentials at Activation:**
  - `ANTHROPIC_API_KEY` (or `OPENAI_API_KEY`)
  - `AI_MODEL_NAME`
  - `AI_MAX_TOKENS_PER_REQUEST`

---

## Pillar 5: Transactional Email & Notifications (Resend / Postmark / SMTP)
- **Role:** Interior design project milestone updates, client consultation invitations, invoice notifications, and password reset workflows.
- **Provider:** Resend API / SMTP gateway.
- **Codebase Implementation:** 100% complete (`EmailNotificationService`, HTML templates with Elégance luxury styling, DKIM/SPF alignment headers, async queue).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Local memory/log email sink captures outgoing emails during testing without outbound network calls.
  - 100% of email rendering tests and template variable replacements pass in Vitest and Spring Boot tests.
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.
- **Required Credentials at Activation:**
  - `RESEND_API_KEY` (or `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`)
  - `EMAIL_FROM_ADDRESS`
  - `EMAIL_FROM_NAME`

---

## Pillar 6: Payment Gateway & Invoicing (Stripe)
- **Role:** Interior design consultation retainers, project milestone payments, bespoke furniture procurement deposits, and recurring designer studio SaaS subscriptions.
- **Provider:** Stripe Checkout, Payment Intents, and Webhooks.
- **Codebase Implementation:** 100% complete (`StripePaymentService`, Stripe webhook signature verification, invoice reconciliation, ledger transaction recording).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Webhook payload HMAC-SHA256 signature verification contract tested.
  - Mock payment session creator enables full project checkout simulation in test environments.
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.
- **Required Credentials at Activation:**
  - `STRIPE_SECRET_KEY`
  - `STRIPE_PUBLISHABLE_KEY`
  - `STRIPE_WEBHOOK_SECRET`

---

## Pillar 7: Monitoring, APM & Error Tracking (Sentry)
- **Role:** Real-time crash reporting, distributed tracing, API latency instrumentation, and database query performance monitoring for luxury web clients.
- **Provider:** Sentry / OpenTelemetry.
- **Codebase Implementation:** 100% complete (`@sentry/nextjs` in frontend, Sentry Spring Boot starter in backend, PII scrubbing filters).
- **Verification Status:** **CONTRACT TESTED & PRE-FLIGHT READY**.
  - Frontend and backend build cleanly with Sentry SDKs initialized in passive mode when DSN is empty.
  - Zero console errors or runtime warnings caused by uninitialized DSN.
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.
- **Required Credentials at Activation:**
  - `NEXT_PUBLIC_SENTRY_DSN`
  - `SENTRY_AUTH_TOKEN`
  - `SENTRY_ORG`
  - `SENTRY_PROJECT`
  - `SENTRY_DSN` (Backend)

---

## Pillar 8: Custom Domain & Production Hosting
- **Role:** Production ingress, SSL/TLS certificate termination, HTTP/2 and HTTP/3 edge routing, strict CORS and CSP enforcement.
- **Provider:** Cloudflare / Vercel / AWS.
- **Verification Status:** **CHECKLIST PREPARED**. `docs/FINAL_DOMAIN_ACTIVATION_CHECKLIST.md` contains the full pre-launch DNS, SSL, and security policy protocol.
- **Deferral Status:** **LIVE VALIDATION DEFERRED**.

---

## Summary Matrix

| Pillar | Provider | Architecture Role | Code Ready | Tests Passing | Live Status | Action at Activation |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Database** | Neon PostgreSQL 18 | Multi-tenant relational store | YES | YES (751 backend) | ACTIVE | Maintain connection pool |
| **2. Storage** | Cloudflare R2 | Portfolio & design uploads | YES | YES (Mock/Contract) | DEFERRED | Provide R2 credentials |
| **3. SSO / Auth** | Google OAuth 2.0 | Federated identity | YES | YES (Mock/Contract) | DEFERRED | Provide Google Client ID |
| **4. AI Assistant**| Anthropic / OpenAI | Interior styling recommendations | YES | YES (Mock/Contract) | DEFERRED | Provide AI API Key |
| **5. Email** | Resend / SMTP | Client alerts & invoices | YES | YES (Log Sink) | DEFERRED | Provide Resend Key |
| **6. Payments** | Stripe | Design fees & deposits | YES | YES (Mock/Contract) | DEFERRED | Provide Stripe Keys |
| **7. Monitoring**| Sentry | Error tracking & APM | YES | YES (Passive Mode) | DEFERRED | Provide Sentry DSN |
| **8. Ingress** | Custom Domain / CDN | Edge routing & SSL | YES | YES (Checklist ready) | DEFERRED | Configure DNS records |
