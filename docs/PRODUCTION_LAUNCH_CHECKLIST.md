# PRODUCTION LAUNCH CHECKLIST
## Elégance — Interior Designer Platform

**Document Version:** 1.0.0  
**Effective Date:** October 9, 2026  
**Status:** Canonical Operations Guide

---

## 1. PRE-LAUNCH PRIORITY MATRIX

### P0 — RELEASE BLOCKERS (FOR CLOSED BETA)
*All code requirements are complete and passing (485 backend tests, 30 PostgreSQL RLS tests, 368 frontend tests). The following infrastructure configurations are required to open the closed beta environment:*
- [ ] **Production Managed PostgreSQL:** Provision PostgreSQL 15+ with pgvector/trigram extensions, connection pooling (PgBouncer/HikariCP max 30 connections), and automatic daily snapshots.
- [ ] **Object Storage Bucket (S3/Cloudflare R2):** Provision dedicated storage bucket with CORS policy restricted to platform domains, private object ACLs, and pre-signed PUT/GET capabilities.
- [ ] **Production OIDC Provider:** Register OAuth2 application (Google, Auth0, or Supabase Auth) with strict redirect URI allowlists (`https://<domain>/api/auth/callback`).
- [ ] **TLS Certificate & Domain Configuration:** Configure Apex and wildcard domains with HTTP Strict Transport Security (HSTS) and automatic Let's Encrypt / Cloudflare TLS renewal.
- [ ] **Environment Secrets Management:** Inject all required environment variables securely via secret manager (AWS Secrets Manager, GCP Secret Manager, or Doppler); zero secrets committed to source control.

---

### P1 — BEFORE PUBLIC LAUNCH (OPEN ACCESS / PUBLIC BETA)
- [ ] **Transactional Email Provider:** Configure production email credentials (Resend, AWS SES, or SendGrid) with custom sender domain (`notifications@elegance.design`).
- [ ] **Email Domain Authentication:** Setup valid SPF (`v=spf1 ... ~all`), DKIM (2048-bit CNAME/TXT records), and DMARC (`v=DMARC1; p=quarantine; rua=...`) records to ensure high inbox deliverability.
- [ ] **Production AI Engine Key:** Configure live API key for image restyling/inpainting (OpenAI / Google Gemini / Replicate) and verify daily quota limits.
- [ ] **Centralized Logging & APM:** Connect application and Next.js logs to central log aggregator (Datadog, Grafana Loki, or Papertrail) with structured JSON log formatting and PII redaction.
- [ ] **Application Monitoring & Alerting:** Configure health check monitors (`/actuator/health`, `/api/health`) with PagerDuty / Slack notifications for HTTP 5xx spikes or unhandled exceptions.
- [ ] **Database Backup & Recovery Drill:** Verify automated daily backups and execute one end-to-end point-in-time recovery (PITR) drill to a secondary staging instance.
- [ ] **Terms of Service & Privacy Policy:** Publish legally vetted Terms of Service, Privacy Policy, Cookie Policy, and explicit AI Visualizer generation disclosure notices.

---

### P2 — COMMERCIAL PRODUCTION & PAID BILLING
- [ ] **Payment Gateway Integration:** Configure live payment gateway (Stripe or Razorpay) credentials, register webhook endpoints (`/api/billing/webhook`) with secret signature validation.
- [ ] **Subscription Plans Activation:** Transition from complimentary beta billing mode to self-serve automated checkout for Standard, Premium, and Pro tiers.
- [ ] **Admin Step-Up MFA:** Enforce mandatory Time-based One-Time Password (TOTP) multi-factor authentication on all `SUPER_ADMIN` and `ADMIN` administrative logins.
- [ ] **Automated Antivirus / Malware Scanning:** Deploy ClamAV or AWS GuardDuty S3 bucket malware scanning to scan newly finalized media assets asynchronously.
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
- **Session Eviction:** Database cleanup scheduled task purges expired sessions older than 12 hours every hour.
- **CSRF Defense:** State verification with PKCE on OIDC login; SameSite cookie restriction on internal APIs.

### 2.2 Database Operations & Migrations
- **Flyway Migrations:** Applied automatically during container startup via Spring Boot Flyway integration, validated up to baseline `V033`.
- **Connection Pool Tuning:**
  - `spring.datasource.hikari.maximum-pool-size=25`
  - `spring.datasource.hikari.minimum-idle=5`
  - `spring.datasource.hikari.connection-timeout=20000`
  - `spring.datasource.hikari.idle-timeout=300000`
- **Zero-Downtime Migration Policy:** Backward-compatible schema changes only (expand-contract pattern: add nullable column first, backfill, make non-nullable in subsequent migration).

### 2.3 Object Storage Architecture
- **Bucket Layout:**
  - `media/studios/{studioId}/projects/{projectId}/original/{assetId}.{ext}`
  - `media/studios/{studioId}/projects/{projectId}/derivatives/{assetId}_{variant}.webp`
  - `media/quarantine/{uploadIntentId}/{assetId}.{ext}`
- **Retention Lifecycle:** Automatically delete uncommitted quarantined objects older than 24 hours via storage bucket lifecycle rule.

### 2.4 Security Headers & CSP
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
   - Verify hero images render without visual layout shift (CLS < 0.1).
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
    - Open `/admin/operations` -> Verify health status is `UP`.
    - Inspect audit logs -> Verify recent authentication and project events are logged.
11. **Logout & Session Destruction:**
    - Click "Sign Out".
    - Verify redirect to `/` and session cookie is cleared.
    - Attempt `GET /workspace` -> Verify redirect to `/auth/login`.

---

## 4. INCIDENT RESPONSE & ROLLBACK PLAN

- **Rollback Decision Criteria:** Error rate exceeds 2% over 5 minutes, database migration fails, or critical data corruption observed.
- **Container Rollback:** Revert container image tag to previous stable commit SHA (`docker service update --image ...` or Kubernetes deployment rollback).
- **Database Rollback:** If Flyway migration cannot be rolled back safely, restore PostgreSQL database from automated pre-deployment snapshot.
- **Incident Communication:** Internal status notification via operations channel within 15 minutes of severity-1 outage.
