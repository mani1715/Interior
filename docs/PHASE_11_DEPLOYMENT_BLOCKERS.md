# Elégance — Phase 11B.1 Deployment Blockers & Prerequisites
**Document Status:** Complete / Source-Truth Corrected  
**Phase:** Phase 11B.1 — Production Decision Source-Truth Correction  
**Date:** 2026-10-13  
**Classification:** Pre-Deployment Readiness Assessment  
**Release Candidate Baseline:** `5c3f41772304b9a5ad33dd43607d1d7e2bbd820f` (Flyway `V040`)  

---

## 1. Overview

This document itemizes every external provider selection, commercial policy decision, cloud resource provisioning step, security boundary condition, and secret credential requirement that must be fulfilled before the Elégance platform can be deployed to live cloud environments.

---

## 2. Blockers Categorized by Launch Phasing

### 2.1 Tier 1: Closed Beta (Studio Invitation Only)

The application code is 100% complete and fully verified. Deployment to an invitation-only closed beta requires:

1. **User Decision Approvals:**
   - Formal sign-off on the items in [docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md](file:///c:/my%20projects/interior%20design/docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md).
2. **Production Domain Selection:**
   - User must supply the registered root domain (replaces placeholder `<ROOT_DOMAIN>`).
3. **Production PostgreSQL 17 Service Provisioning:**
   - Provision Railway PostgreSQL service (PostgreSQL 17) or Supabase PG 17.
   - Configure SSL connection mode (`sslmode=require`) and execute Flyway migrations `V001` through `V040`.
   - Run verification script `verify-cloud-db-rls.sql` to confirm Row Level Security on all 59 tables.
4. **Scheduled Backups & PITR Activation (MANDATORY):**
   - Scheduled database/volume backups must be **explicitly enabled and verified** in provider dashboard before storing real customer data.
   - Continuous WAL archiving / Point-In-Time Recovery (PITR) must be **explicitly enabled and verified** if selected.
5. **Media Upload Malware Scanner (CRITICAL SECURITY BLOCKER):**
   - Production invariant `require-clean-scan=true` fails closed (`MALWARE_SCANNER_NOT_CONFIGURED`) without a configured scanner.
   - User uploads cannot be treated as trusted merely because the user is invited.
   - **CLOSED-BETA MEDIA UPLOAD BLOCKER:** A real production scanner adapter (e.g. ClamAV container) must be implemented, deployed, and configured before normal user media uploads are enabled in production (unless beta testing is restricted strictly to pre-scanned administrator fixtures).
6. **Cloud Container Runtime Provisioning:**
   - Provision backend container runner (Railway) for Spring Boot API container (`apps/api/Dockerfile`).
   - Provision frontend edge platform (Vercel) for Next.js web application (`apps/web`).
7. **Cloud Object Storage Provisioning:**
   - Provision Cloudflare R2 dual buckets (private and public).
   - Configure CORS rules and map `APP_CDN_HOSTNAME` / `NEXT_PUBLIC_CDN_HOSTNAME` to `<MEDIA_DOMAIN>`.
8. **Domain & Edge TLS Routing:**
   - Map official custom domain routing (`https://<ROOT_DOMAIN>`, `https://api.<ROOT_DOMAIN>`, `https://<MEDIA_DOMAIN>`).
   - Provision automated TLS certificates (Let's Encrypt / Cloudflare Edge SSL).
9. **Core Identity Provider Credentials:**
   - Register OAuth 2.0 credentials in Google Cloud Console for the production callback URL.

---

### 2.2 Tier 2: Public Beta (Broader Studio Onboarding)

Opening the platform to public beta onboarding requires resolving the following deferred items:

1. **Transactional Email Service Provisioning:**
   - Select and contract transactional email provider (e.g., Resend, Postmark, AWS SES).
   - Configure DNS DKIM, SPF, and DMARC records on the official sending domain.
   - Inject `APP_EMAIL_API_KEY` and verify delivery of outbox notifications.
2. **Production Malware Scanner Deployment (If not resolved in Tier 1):**
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
3. **Team Seat Monetization Policy:**
   - Business leadership approval of team member limits per studio tier (if monetization restricts seats).
4. **Statutory Tax & GST Review:**
   - Qualified accounting and legal sign-off on GST treatment, SAC code 998314, and B2B invoicing rules.
5. **Disaster Recovery & SLA Sign-Off:**
   - Formal executive approval of commercial RPO and RTO contract SLAs.
   - Centralized 24/7 alert notifications for error spikes and connection pool exhaustion.
