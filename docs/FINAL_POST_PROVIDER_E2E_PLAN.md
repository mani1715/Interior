# Final Post-Provider Live End-to-End Test Plan
## Elégance — Interior Designer Platform

This test plan defines the comprehensive, live end-to-end verification suites to be executed **only after all external provider keys have been provisioned and configured**.

---

## 1. Journey 1: Complete Designer Studio Lifecycle & Onboarding
1. **Authentication (Google OIDC)**:
   - Click "Sign In with Google" on `/sign-in`.
   - Complete real Google account consent.
   - Verify callback exchange on `/auth/callback` and session establishment.
2. **Studio Creation**:
   - Redirected to `/onboarding/professional`.
   - Submit studio details: "Studio Aethelgard", Mumbai, Luxury/Contemporary.
   - Verify atomic record creation in Neon PostgreSQL with native `uuidv7()`.
3. **Studio Verification & Email Notification**:
   - Designer receives welcome email via selected email provider (Resend or SES) to verified account inbox.
   - Check DKIM/SPF headers and responsive HTML rendering.

---

## 2. Journey 2: Project Management & Live Media Pipeline (Cloudflare R2 + ClamAV)
1. **Create Project**:
   - Create project "Juhu Penthouse Renovation" under Studio Aethelgard.
   - Add rooms: "Master Suite", "Double-Height Living Room".
2. **Direct Browser Presigned Upload**:
   - Select 45 MB high-resolution architectural photo.
   - Browser initiates HTTP PUT directly to Cloudflare R2 quarantine key (`pending/...`).
   - Confirm upload -> ClamAV streaming scan reports clean -> EXIF stripped -> WebP derivatives generated -> promoted to canonical R2 (`studio/...`).
3. **Public Exposure**:
   - Verify public derivative URL (`public/...`) resolves via CDN with immutable cache header.
   - Verify raw original cannot be accessed publicly without authenticated presigned token.

---

## 3. Journey 3: AI Visualizer & Concept Inpainting (Selected AI Image Provider)
1. **Inpainting Session**:
   - Open Master Suite room photo in `/workspace/ai`.
   - Paint precision mask over window treatment area.
   - Prompt: "Floor-to-ceiling Belgian linen sheer drapery in warm ivory".
2. **Generation Execution**:
   - Send generation request to selected AI adapter (Vertex AI, Stability AI, or Replicate).
   - Verify in-memory ClamAV scan on output image bytes.
   - Output concept stored in Cloudflare R2 and attached to project room as "AI Concept" revision.
   - Quota ledger decrements studio daily balance from 20 to 19.

---

## 4. Journey 4: Client Collaboration & Review Verification
1. **Client Invitation**:
   - Designer generates client review invitation token for project.
   - Client receives email notification via selected email provider with secure token link.
2. **Review Submission**:
   - Client opens `/review/[token]`.
   - Client rates project (5 stars) and submits testimonial: "Flawless craftsmanship and spatial balance".
3. **Moderation Flow**:
   - Review appears in `/admin/reviews` moderation queue.
   - Moderator approves review.
   - Review publishes to studio's public portfolio page (`/professionals/studio-aethelgard`).

---

## 5. Journey 5: Commercial Subscription Upgrade (Selected Payment Gateway)
1. **Plan Upgrade**:
   - Navigate to `/workspace/billing`.
   - Select PRO subscription plan.
   - Redirect to hosted checkout session of selected provider (Razorpay or Stripe).
2. **Test Payment**:
   - Complete test payment in vendor sandbox environment.
   - Return to `/workspace/billing?session_id=...`.
3. **Webhook Processing**:
   - Provider dispatches subscription activation webhook.
   - Platform verifies webhook cryptographic signature and commits entitlement upgrade in Neon PostgreSQL.
   - Studio capacity meters immediately reflect PRO entitlements (5 Cinematic projects, unlimited media).

---

## 6. Journey 6: Observability & Telemetry Verification
1. **Trace Propagation**:
   - Verify `X-Request-Id` correlation header spans across Next.js frontend, Spring Boot backend, and PostgreSQL database queries.
2. **Telemetry Alert Probe**:
   - Trigger diagnostic telemetry test event.
   - Verify event is received in configured monitoring platform (Prometheus metrics, Sentry issues dashboard, or OTLP collector) with complete stack trace, redacted PII, and non-root execution context.
