# Elégance — Phase 11 Deployment Blockers & Prerequisites
**Document Status:** Production Baseline  
**Phase:** Phase 10 — Final Production Release Audit  
**Classification:** Pre-Deployment Readiness Assessment

---

## 1. Overview

This document itemizes every external provider selection, commercial contract, cloud resource provisioning step, and secret credential requirement that must be fulfilled before the Elégance platform can be deployed to live environments in Phase 11.

---

## 2. Blockers Categorized by Launch Tier

### 2.1 Tier 1: Closed Beta (Studio Invitation Only)

The application code is 100% complete and fully operational. Deployment to a private staging / closed beta environment requires:

1. **Production PostgreSQL Cluster Provisioning:**
   - Host PostgreSQL 16+ on managed cloud (e.g., Supabase, Neon, AWS RDS, Railway).
   - Configure connection pooling and execute Flyway migrations V001 through V040.
   - Verify native RLS functionality under target database engine version.
2. **Cloud Container Runtime Provisioning:**
   - Provision production web runner (e.g., Railway, Render, AWS ECS) for Spring Boot API container (`apps/api/Dockerfile`).
   - Provision Edge / Node runtime (e.g., Vercel, Cloudflare, Node server) for Next.js web application (`apps/web`).
3. **Domain & TLS Setup:**
   - Attach official custom domain (e.g., `app.elegance.design` / `api.elegance.design`).
   - Provision automated TLS certificates (Let's Encrypt / Cloudflare Edge SSL).
4. **Core Identity Provider Credentials:**
   - Register OAuth 2.0 / OIDC credentials in Google Cloud Console and Apple Developer portal for the production callback URLs.

---

### 2.2 Tier 2: Public Beta (Broader Studio Onboarding)

In addition to Tier 1 prerequisites, opening the platform to public beta onboarding requires:

1. **Transactional Email Service Credentials:**
   - Select and contract transactional email provider (e.g., Resend, Postmark, AWS SES).
   - Configure DNS DKIM, SPF, and DMARC records on the official sending domain.
   - Inject `APP_EMAIL_API_KEY` and verify delivery of outbox messages.
2. **Production Object Storage Provisioning:**
   - Provision secure S3-compatible bucket (e.g., Cloudflare R2, AWS S3).
   - Set up private bucket policies with presigned GET/PUT URLs.
   - Configure CORS rules for direct client uploads.
   - Configure `APP_CDN_HOSTNAME` / `NEXT_PUBLIC_CDN_HOSTNAME` with the public storage / CDN domain to allow Next.js 16.3.8 image optimization to safely fetch assets without wildcard remote patterns.
3. **Production Malware Scanner Deployment:**
   - Deploy ClamAV daemon container or provision external anti-malware scanning endpoint.
   - Connect API worker to scan queue with fail-closed policy (`app.media.security.require-clean-scan=true`).
4. **AI Generation Provider API Key:**
   - Provision production Anthropic / OpenAI / Google Gemini API credentials.
   - Set workspace token budget alarms and enforce rate limits.

---

### 2.3 Tier 3: Commercial Launch (General Availability)

Full commercial public launch requires:

1. **Commercial Payment Gateway Contract & Credentials:**
   - Complete business underwriting and KYC with payment processor (e.g., Stripe).
   - Configure live subscription plans, prices, and customer portal in payment dashboard.
   - Register live webhook endpoint (`POST /api/v1/billing/webhook`) and configure `BILLING_WEBHOOK_SECRET`.
2. **Production Observability & Monitoring:**
   - Deploy Prometheus + Grafana or Datadog agent.
   - Configure PagerDuty / Opsgenie alert notifications for 5xx error rate spikes, database connection exhaustion, and queue backpressure.
3. **Disaster Recovery Sign-Off:**
   - Formal executive approval of RPO and RTO SLAs.
   - Automated daily database backup and retention policy activation in cloud provider.

---

## 3. Release Freeze Statement

Application feature development is **FROZEN**. All 10 product development phases (Phases 1 through 9G) have successfully completed. No live cloud resources were created or modified during Phase 10. Phase 11 will proceed strictly to address the deployment blockers enumerated above.
