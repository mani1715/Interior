# Phase 12E / Phase 15: Transactional Email Live Validation Runbook

> **Target Phase:** Phase 15 Pre-Launch Provider Activation  
> **Audience:** DevOps Engineers, Platform Administrators  
> **Prerequisites:** Production domain purchased, DNS management access (Cloudflare / Route 53)

---

## 1. Overview & Strategy

This runbook guides platform operators through activating and verifying live transactional email in **Phase 15**. In Phase 12E, all code, adapters, template escaping, out-of-order defenses, and contract tests are fully implemented and validated with `EMAIL_PROVIDER=disabled`.

Follow this step-by-step checklist when the production domain is live and real email credentials are ready to be activated.

---

## 2. Option A: Activating Resend (Recommended)

### Step 1: Create Resend Account & Add Domain
1. Log in to [Resend Console](https://resend.com).
2. Navigate to **Domains** -> **Add Domain**.
3. Enter the canonical sending domain (e.g. `notifications.elegance.interior` or `elegance.interior`).
4. Select the target region (e.g. `us-east-1` or `eu-west-1`).

### Step 2: Configure DNS Records
Add the DNS records provided by the Resend dashboard to your DNS provider (e.g. Cloudflare):
- **DKIM (Public Key):** CNAME record pointing `resend._domainkey.elegance.interior` to `dkim.resend.com`.
- **SPF (Return-Path):** MX record for `bounces.elegance.interior` pointing to `feedback-smtp.us-east-1.amazonses.com` (or Resend's designated bounce host) and TXT record `v=spf1 include:amazonses.com ~all`.
- **DMARC:** TXT record at `_dmarc.elegance.interior`:
  ```
  v=DMARC1; p=reject; rua=mailto:dmarc-reports@elegance.interior; pct=100;
  ```
5. Click **Verify Domain** in Resend. Ensure status shows **Verified**.

### Step 3: Create API Key
1. In Resend dashboard, go to **API Keys** -> **Create API Key**.
2. Name: `elegance-production-api-key`.
3. Permissions: `Sending access` (restrict to sending domain).
4. Copy key (`re_live_...`). Store in production secret manager (AWS Secrets Manager, Doppler, or HashiCorp Vault).

### Step 4: Configure Webhooks
1. In Resend dashboard, go to **Webhooks** -> **Add Webhook**.
2. Endpoint URL: `https://api.elegance.interior/api/v1/webhooks/email/resend`.
3. Select Events: `Sent`, `Delivered`, `Bounced`, `Complained`.
4. Copy the Webhook Signing Secret (`whsec_...`).

### Step 5: Configure Production Environment Variables
Set the following environment variables on the production application container:
```bash
EMAIL_PROVIDER=resend
EMAIL_FROM_ADDRESS=notifications@elegance.interior
EMAIL_FROM_NAME="Elégance Platform"
EMAIL_REPLY_TO=support@elegance.interior
RESEND_API_KEY=re_live_xxxxxxxxxxxxxxxxxxxxxxxx
EMAIL_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxx
```

---

## 3. Option B: Activating AWS SES

### Step 1: Verify Domain Identity in AWS SES
1. Open AWS Management Console -> **Amazon SES** -> **Configuration: Verified identities**.
2. Click **Create identity** -> select **Domain**.
3. Enter domain: `elegance.interior`.
4. Enable **Easy DKIM** with 2048-bit RSA key.
5. Publish the 3 generated CNAME records to DNS.

### Step 2: Configure Custom MAIL FROM Domain (SPF)
1. In the SES domain identity, set **Custom MAIL FROM domain**: `mail.elegance.interior`.
2. Add MX record: `mail.elegance.interior` $\to$ `feedback-smtp.us-east-1.amazonses.com` (priority 10).
3. Add TXT record: `mail.elegance.interior` $\to$ `v=spf1 include:amazonses.com ~all`.

### Step 3: Publish DMARC Record
Add TXT record at `_dmarc.elegance.interior`:
```
v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@elegance.interior; pct=100;
```

### Step 4: Request Production Access (Exit Sandbox)
1. In SES console -> **Account dashboard** -> **Request production access**.
2. Use-case description: "Transactional operational emails for Elégance Interior Designer Platform (designer invitations, client review invites, payment receipts). No marketing mail."
3. Wait for AWS approval (typically 4–24 hours).

### Step 5: Configure SNS Notifications for Bounces & Complaints
1. Create an SNS Topic: `elegance-ses-events`.
2. In SES identity, add event destination: Send Bounce, Complaint, and Delivery events to the SNS topic.
3. Subscribe platform webhook URL `https://api.elegance.interior/api/v1/webhooks/email/ses` to the SNS topic. Confirm the subscription.

### Step 6: Configure Production Environment Variables
```bash
EMAIL_PROVIDER=ses
EMAIL_FROM_ADDRESS=notifications@elegance.interior
EMAIL_FROM_NAME="Elégance Platform"
EMAIL_REPLY_TO=support@elegance.interior
AWS_SES_REGION=us-east-1
AWS_SES_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:elegance-ses-events
AWS_ACCESS_KEY_ID=AKIAxxxxxxxxxxxxxxxx
AWS_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

## 4. Verification Drill & Smoke Testing

### Step 1: Execute Live Integration Harness
Run the provided validation script from the repository root:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-email-provider-live-integration.ps1 -RequireLiveCredentials
```
**Expected Output:**
- `HTTP Status Code: 200`
- `Roundtrip Latency: < 400 ms`
- `LIVE [RESEND|AWS SES] EMAIL PROVIDER CONNECTIVITY VERIFIED SUCCESSFULLY.`

### Step 2: Production Container Startup Verification
Start the backend container in `production` profile and verify the banner output in the logs:
```
================================================================================
           PLATFORM SYSTEM STARTUP & CONFIGURATION MATRIX                       
================================================================================
 Active Profiles       : [production]
 Database (PostgreSQL) : CONFIGURED
 Auth (OIDC)           : CONFIGURED
 Object Storage        : CONFIGURED (S3/R2 Cloud Storage)
 Malware Scanner       : CONFIGURED (ClamAV Active)
 AI Visualizer         : CONFIGURED (Phase 15)
 WhatsApp              : MODE_A_ACTIVE (Direct wa.me)
 Billing Gateway       : CONFIGURED (Razorpay / Stripe)
 Transactional Email   : CONFIGURED (Resend)
================================================================================
```

### Step 3: End-to-End Send & Delivery Test
1. Invite a test team member using an internal inbox: `test-designer@elegance.interior`.
2. Inspect `communication_deliveries` in PostgreSQL:
   ```sql
   SELECT id, recipient, template_name, status, provider_message_id, created_at, sent_at, delivered_at
   FROM communication_deliveries
   ORDER BY created_at DESC LIMIT 5;
   ```
3. Verify status transitions: `PENDING` $\to$ `SENT` $\to$ `DELIVERED`.
4. Inspect email in recipient client: verify HTML styling, UTF-8 rendering, sender display name, and CTA link resolution.

---

## 5. Rollback / Emergency Fallback Procedure

If the active email provider experiences upstream outages, DNS misconfigurations, or API rate-limiting issues:
1. Immediately switch the environment variable to:
   ```bash
   EMAIL_PROVIDER=disabled
   ```
2. Restart the API container.
3. The platform will continue running with zero downtime:
   - Team invitations and review invitations will log a structured warning and succeed locally.
   - Invitation tokens remain queryable via admin APIs or UI.
   - No user-facing 500 error will be thrown to designers or customers.
