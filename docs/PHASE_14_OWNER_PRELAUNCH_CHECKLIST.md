# Phase 14 — Platform Owner Pre-Launch Checklist

> **Target Audience:** Platform Owner / Business Stakeholders / Operations Lead  
> **Platform:** Elégance Interior Designer Platform  
> **Status:** READY FOR OWNER ACTION ITEMS  

---

## 1. Executive Summary

This checklist outlines the business, legal, brand, and operational decisions that must be resolved by the platform owner prior to public production launch (targeted for Phase 16, following Phase 15 Provider Activation).

---

## 2. Business & Legal Action Items

- [ ] **Final Brand & Trademark Confirmation:**
  - Working product name is currently `Elégance`.
  - Confirm whether the final registered commercial product name remains `Elégance` or if an alternative brand name will be selected.
- [ ] **Domain Name Registration & DNS:**
  - Register canonical production apex domain (e.g. `elegance.design` or chosen alternative).
  - Configure authoritative DNS nameservers on Cloudflare or enterprise DNS provider.
- [ ] **Legal Terms & Privacy Policy Review:**
  - Ensure terms of service covers multi-tenant interior designer portfolio copyright, client design ownership, floor plan intellectual property, and cancellation/refund policies.
  - Review privacy policy for GDPR / CCPA compliance regarding client address information, interior photography uploads, and contact submissions.
  - Status remains: `LEGAL REVIEW REQUIRED BEFORE PUBLIC PRODUCTION`.
- [ ] **Commercial Pricing & Stripe Merchant Account:**
  - Set up legal business entity details with Stripe.
  - Link verified business bank account for payouts.
  - Confirm monthly/annual subscription tier pricing for interior design studios:
    - *Solo Designer Tier*
    - *Boutique Studio Tier*
    - *Enterprise Design Firm Tier*

---

## 3. Account & Service Registration Tasks (for Phase 15)

The owner or designated administrator must register accounts with the following infrastructure providers:

1. [ ] **Cloudflare:**
   - Account setup for edge CDN, DNS, and Cloudflare R2 object storage.
2. [ ] **Google Cloud Console:**
   - Create Google Cloud Project.
   - Configure OAuth consent screen with business logo, support email, and privacy policy URL.
   - Generate OAuth 2.0 Client credentials for user login.
3. [ ] **Transactional Email Provider (SendGrid / Resend / Postmark):**
   - Create account and add domain verification DNS records (DKIM, SPF, DMARC).
   - Generate API keys for transaction emails (invites, password resets, client consultation requests).
4. [ ] **AI Provider (OpenAI / Anthropic):**
   - Create team organization account.
   - Configure billing limits and generate production API keys for design style extraction and moodboard generation.
5. [ ] **Hosting Infrastructure:**
   - Select cloud hosting target (e.g. Railway Team Plan, AWS ECS / EKS, or managed container VPS).
   - Provision managed PostgreSQL database with high-availability standby.

---

## 4. Operational Sign-Off

| Milestone | Target Phase | Status | Owner Sign-Off |
|---|---|---|---|
| Release Candidate Engineering Package | Phase 14 | **COMPLETE** | Ready |
| Live Provider Account Activation | Phase 15 | **PENDING** | Awaiting Provider Creation |
| Brand / Domain / Staging / Production Launch | Phase 16 | **SCHEDULED** | Awaiting Phase 15 Completion |
