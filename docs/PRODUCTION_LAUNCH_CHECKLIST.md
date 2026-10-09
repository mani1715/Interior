# PRODUCTION LAUNCH CHECKLIST
## Elégance — Interior Designer Platform

**Document Version:** 1.1.0  
**Effective Date:** October 9, 2026  
**Status:** Canonical Operations & Infrastructure Blueprint

---

## 1. PRE-LAUNCH PRIORITY MATRIX

### P0 — RELEASE BLOCKERS (FOR PHASE 8 CLOSED-BETA GO-LIVE)
*All code requirements are complete and verified (485 backend tests, 30 PostgreSQL RLS tests, 368 frontend tests, clean Next.js 16.3.3 build). The following infrastructure provisioning tasks must be completed before the closed beta environment can be declared live:*
- [ ] **Production Managed PostgreSQL:** Provision managed PostgreSQL (Tested: 18.3, Minimum supported: 15, Recommended: 16 or 17 due to Flyway official support boundary). Include connection pooling (HikariCP / PgBouncer max 25 connections).
- [ ] **Automated Database Backups:** Configure automated daily snapshots with point-in-time recovery (PITR) where available and execute one test restore drill. *Closed Beta cannot accept real customer data without verified backups.*
- [ ] **Object Storage Bucket (S3/Cloudflare R2):** Provision dedicated storage bucket with CORS policy restricted to platform domains, private object ACLs, and pre-signed PUT/GET capabilities.
- [ ] **Production OIDC Provider:** Register OAuth2 application (Google, Auth0, or Supabase Auth) with strict redirect URI allowlists (`https://<PRODUCTION_DOMAIN>/api/auth/callback`).
- [ ] **TLS Certificate & Domain Configuration:** Finalize domain selection (currently: `NOT YET SELECTED`) and configure HTTPS with HSTS and automated certificate renewal. Set `APP_BASE_URL` environment variable accordingly.
- [ ] **Environment Secrets Management:** Inject all required environment variables securely via secret manager; zero secrets committed to source control.

---

### P1 — BEFORE PUBLIC LAUNCH (OPEN ACCESS / PUBLIC BETA)
- [ ] **Admin Step-Up MFA:** Implement and enforce Time-based One-Time Password (TOTP) multi-factor authentication on all `SUPER_ADMIN` and `ADMIN` administrative logins. *Mandatory before open public internet access.*
- [ ] **Transactional Email Provider:** Configure production email credentials (Resend, AWS SES, or SendGrid) with custom sender domain (`notifications@<PRODUCTION_DOMAIN>`).
- [ ] **Email Domain Authentication:** Setup valid SPF (`v=spf1 ... ~all`), DKIM (2048-bit CNAME/TXT records), and DMARC (`v=DMARC1; p=quarantine; rua=...`) records to ensure high inbox deliverability.
- [ ] **Production AI Engine Key:** Configure live API key for image restyling/inpainting (OpenAI / Google Gemini) and verify daily studio quota ceilings (`dailyStudioLimit`, default 20/day).
- [ ] **Centralized Logging & APM:** Connect application and Next.js logs to central log aggregator (Datadog, Grafana Loki, or Papertrail) with structured JSON log formatting and PII redaction.
- [ ] **Production Monitoring & Alerting:** Configure external health check monitoring on `/actuator/health` and `/api/health` with PagerDuty / Slack alerts for HTTP 5xx errors.
- [ ] **Legal Counsel Execution:** Finalize and execute formal Terms of Service, Privacy Policy, and AI generation disclosures (current `/privacy` and `/terms` routes contain platform drafts).

---

### P2 — COMMERCIAL PRODUCTION & PAID BILLING
- [ ] **Payment Gateway Integration:** Configure live payment gateway (Stripe or Razorpay) credentials, register webhook endpoints (`/api/billing/webhook`) with secret signature validation.
- [ ] **Automated Antivirus / Malware Scanning:** Deploy ClamAV or AWS GuardDuty S3 bucket malware scanning to scan newly finalized media assets asynchronously. *(Accepted risk during Closed Beta due to magic-byte, ImageIO decoder probing, and 50MP limits).*
- [ ] **Subscription Plans Self-Serve Activation:** Transition from complimentary beta billing mode to automated self-serve checkout for locked tiers:
  - Standard: 10 projects, 15 photos/project, Standard presentation only (0 cinematic)
  - Premium: 20 projects, 25 photos/project, Standard presentation only (0 cinematic)
  - Pro: 20 projects total, 30 photos/project, Standard + Cinematic presentation (max 5 cinematic allocations)
- [ ] **Managed WhatsApp Business API:** Transition from direct sanitized WhatsApp wa.me links to verified Meta Business API integration for automated studio lead notifications.

---

### P3 — POST-LAUNCH ENHANCEMENTS (FUTURE)
- [ ] **True 3D / WebGL Room Walkthrough:** Interactive three-dimensional model visualization.
- [ ] **360-Degree Panoramic Viewers:** Equirectangular sphere viewer with camera gyro navigation on mobile devices.
- [ ] **Advanced Invoicing & Escrow:** Integrated client milestone billing, escrow management, and automated tax invoicing.
- [ ] **International Multi-Currency Support:** Automated localized currency conversion and local tax compliance (GST, VAT).

---

## 2. PRODUCTION SYSTEM CONFIGURATION BLUEPRINT

### 2.1 Production Authentication & Sessions
- **Session Cookie:** `__Host-session` cookie configured with attributes: `Secure; HttpOnly; SameSite=Lax; Path=/`.
- **Session Timeouts:** Idle timeout: 30 minutes (1,800s); Absolute timeout: 12 hours (43,200s); Last-seen throttled interval: 5 minutes (300s).
- **Session Eviction:** Database cleanup scheduled task purges expired sessions older than 12 hours every hour.
- **CSRF Defense:** State verification with PKCE on OIDC login; SameSite cookie restriction on internal APIs.

### 2.2 Database Operations & Migrations
- **Flyway Migrations:** Applied automatically during container startup via Spring Boot Flyway integration, validated up to baseline `V033`.
- **Connection Pool Tuning:**
  - `spring.datasource.hikari.maximum-pool-size=25`
  - `spring.datasource.hikari.minimum-idle=5`
  - `spring.datasource.hikari.connection-timeout=20000`
  - `spring.datasource.hikari.idle-timeout=300000`
- **Zero-Downtime Migration Policy:** Backward-compatible schema changes only (expand-contract pattern).

### 2.3 Object Storage Architecture
- **Bucket Layout:**
  - `media/studios/{studioId}/projects/{projectId}/original/{assetId}.{ext}`
  - `media/studios/{studioId}/projects/{projectId}/derivatives/{assetId}_{variant}.webp`
  - `media/quarantine/{uploadIntentId}/{assetId}.{ext}`
- **Retention Lifecycle:** Automatically delete uncommitted quarantined objects older than 24 hours via storage bucket lifecycle rule.
- **Media Deletion:** Soft-deletes `media_assets`, hard-deletes derivatives, and immediately releases committed storage byte quota.

### 2.4 Data Retention & Archive Policy
- **Project Archiving:** Projects are soft-archived (`project_status = 'ARCHIVED'`). Archived projects can be restored to `DRAFT`. There is NO 30-day automatic hard purge. **Archived projects continue to count toward the studio's `PROJECT_LIMIT` quota.**
- **Studio / User Lifecycle:** Managed via account status (`ACTIVE`, `SUSPENDED`) and session revocation. No automated cascading deletion endpoint exists.

### 2.5 Security Headers & CSP
Ensure the reverse proxy (Nginx, Caddy, or Cloudflare) emits the following security headers:
```http
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https://*.amazonaws.com https://*.r2.cloudflarestorage.com; connect-src 'self' https://*.r2.cloudflarestorage.com; font-src 'self'; object-src 'none'; frame-ancestors 'none';
```

---

## 3. POST-DEPLOYMENT SMOKE TEST SEQUENCE

*Execute the following deterministic smoke test sequence on the freshly deployed staging or closed beta environment:*

1. **Public Homepage Sanity:**
   - Request `GET /` -> HTTP 200.
   - Verify 40/60 split hero renders without visual layout shift (CLS < 0.1).
   - Test instant search bar: search for city "Mumbai" or style "Modern".
2. **OIDC Authentication Flow:**
   - Click "Sign In" -> Redirect to identity provider.
   - Complete authentication -> Redirect to `/api/auth/callback` -> Redirect to `/workspace`.
   - Verify `__Host-session` cookie is set with `Secure; HttpOnly; SameSite=Lax`.
3. **Professional Studio Onboarding:**
   - Complete 7-step studio creation wizard.
   - Verify new studio appears in studio selector dropdown.
4. **Project Creation & Room Setup:**
   - Create new project "Penthouse Suite".
   - Create rooms: "Living Room", "Master Bedroom".
5. **Media Upload & Derivation:**
   - Upload 2 sample high-resolution JPEG images.
   - Verify upload completes, status becomes `READY`, and WebP derivatives load.
6. **Project Publication:**
   - Select room cover photo and project cover photo.
   - Publish project -> Verify publication status is `PUBLISHED`.
7. **Public Discovery Verification:**
   - Navigate to `/projects` in an Incognito / unauthenticated window.
   - Verify published project appears in directory.
8. **Public Enquiry & CRM Intake:**
   - Open project page and submit public enquiry form.
   - Switch back to designer workspace -> Open `/workspace/leads`.
   - Verify new lead is present in pipeline under `NEW` stage.
   - Check notification bell -> Verify unread badge increments and notification displays.
9. **Cinematic Presentation:**
   - Open project in Cinematic mode (`/projects/{slug}?cinematic=true`).
   - Verify smooth scroll-driven presentation and room anchor navigation.
10. **Administrative Operations:**
    - Login as `SUPER_ADMIN`.
    - Open `/admin` -> Verify health status is `UP`.
    - Inspect audit logs -> Verify recent authentication and project events are logged.
11. **Logout & Session Destruction:**
    - Click "Sign Out".
    - Verify redirect to `/` and session cookie is cleared.
    - Attempt `GET /workspace` -> Verify redirect to `/auth/login`.

---

## 4. INCIDENT RESPONSE & ROLLBACK PLAN

- **Rollback Decision Criteria:** Error rate exceeds 2% over 5 minutes, database migration fails, or critical data corruption observed.
- **Container Rollback:** Revert container image tag to previous stable commit SHA (`docker service update --image ...` or Kubernetes deployment rollback).
- **Database Rollback:** Restore PostgreSQL database from automated pre-deployment snapshot if Flyway migration cannot be safely rolled back.
- **Incident Communication:** Internal status notification via operations channel within 15 minutes of severity-1 outage.
