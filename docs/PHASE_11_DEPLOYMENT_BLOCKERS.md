# Elégance — Phase 11B Deployment Blockers & Prerequisites
**Document Status:** Complete / Awaiting User Sign-Off  
**Phase:** Phase 11B — Final Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Pre-Deployment Readiness Assessment  
**Release Candidate Baseline:** `7db3212fb4b43e507d60e81fd1e235b98bb87821` (Flyway `V040`)  

---

## 1. Overview

This document itemizes every external provider selection, commercial policy decision, cloud resource provisioning step, and secret credential requirement that must be fulfilled before the Elégance platform can be deployed to live cloud environments.

---

## 2. Blockers Categorized by Launch Phasing

### 2.1 Tier 1: Closed Beta (Studio Invitation Only)

The application code is 100% complete and fully verified. Deployment to an invitation-only closed beta requires:

1. **User Decision Approvals:**
   - Formal sign-off on the items in [docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md](file:///c:/my%20projects/interior%20design/docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md).
2. **Production Domain Selection:**
   - User must supply the registered root domain (replaces placeholder `<ROOT_DOMAIN>`).
3. **Production PostgreSQL 17 Database Provisioning:**
   - Provision managed PostgreSQL 17 on chosen provider (e.g., Railway Managed PostgreSQL or Supabase).
   - Configure SSL connection mode (`sslmode=require`) and execute Flyway migrations `V001` through `V040`.
   - Run verification script `verify-cloud-db-rls.sql` to confirm Row Level Security on all 59 tables.
4. **Cloud Container Runtime Provisioning:**
   - Provision backend container runner (e.g., Railway) for Spring Boot API container (`apps/api/Dockerfile`).
   - Provision frontend edge platform (e.g., Vercel) for Next.js web application (`apps/web`).
5. **Domain & Edge TLS Routing:**
   - Map official custom domain routing (`https://<ROOT_DOMAIN>`, `https://api.<ROOT_DOMAIN>`, `https://<MEDIA_DOMAIN>`).
   - Provision automated TLS certificates (Let's Encrypt / Cloudflare Edge SSL).
6. **Core Identity Provider Credentials:**
   - Register OAuth 2.0 credentials in Google Cloud Console for the production callback URL.
7. **Cloud Object Storage Provisioning:**
   - Provision S3-compatible dual buckets (e.g., Cloudflare R2: private and public).
   - Configure CORS rules and map `APP_CDN_HOSTNAME` / `NEXT_PUBLIC_CDN_HOSTNAME` to `<MEDIA_DOMAIN>`.
8. **Automated Database Backups:**
   - Verify automated daily snapshots and Point-In-Time Recovery (PITR) WAL archiving are active.

---

### 2.2 Tier 2: Public Beta (Broader Studio Onboarding)

Opening the platform to public beta onboarding requires resolving the following deferred items:

1. **Transactional Email Service Provisioning:**
   - Select and contract transactional email provider (e.g., Resend, Postmark, AWS SES).
   - Configure DNS DKIM, SPF, and DMARC records on the official sending domain.
   - Inject `APP_EMAIL_API_KEY` and verify delivery of outbox notifications.
2. **Production Malware Scanner Deployment:**
   - Deploy ClamAV daemon container or provision external anti-malware scanning endpoint.
   - Connect API worker to scan queue with fail-closed policy (`app.media.security.require-clean-scan=true`).
3. **Generative AI Provider Decision (If Enabled):**
   - Conduct model quality evaluation (Stability / Replicate / Vertex).
   - Sign enterprise Data Processing Agreement (DPA) guaranteeing zero customer room photo training.
   - Provision production API credentials and enforce daily studio rate limits.
4. **Legal Policy Sign-Off:**
   - Formal legal counsel sign-off on public Terms of Service and Privacy Policy.

---

### 2.3 Tier 3: Commercial Launch (General Availability)

Full commercial public launch requires:

1. **Commercial Payment Gateway Contract & Credentials:**
   - Complete business underwriting and KYC with payment processor (e.g., Razorpay for India-First, or Stripe).
   - Configure live subscription plans, prices, and customer portal in payment dashboard.
   - Register live webhook endpoint (`POST /api/v1/billing/webhook`) and configure `BILLING_WEBHOOK_SECRET`.
2. **Pricing & Currency Finalization:**
   - Business leadership approval of monthly and annual subscription rates in INR (`₹`).
3. **Statutory Tax & GST Review:**
   - Qualified accounting and legal sign-off on GST treatment, SAC code 998314, and B2B invoicing rules.
4. **Disaster Recovery & SLA Sign-Off:**
   - Formal executive approval of RPO (< 1 hour) and RTO (< 2 hours) targets.
   - Centralized 24/7 alert notifications for error spikes and connection pool exhaustion.
