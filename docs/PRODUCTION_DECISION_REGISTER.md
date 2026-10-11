# Elégance — Production Decision Register
**Document Status:** Production Baseline / Pre-Provisioning  
**Phase:** Phase 10 — Final Production Release Audit  
**Classification:** Internal Architectural & Operational Governance

---

## 1. Executive Summary & Purpose

This Decision Register provides the authoritative record of all architectural, infrastructural, commercial, security, privacy, and operational decisions for the Elégance Interior Designer Platform. 

Where decisions have been architected and implemented in code, their architectural contract is documented. Where operational parameters or vendor selections depend on business approval and live cloud provisioning (scheduled for Phase 11), they are explicitly designated as **`DECISION REQUIRED`** to prevent unapproved assumptions or simulated commitments.

---

## 2. Decision Register Matrix

| ID | Domain | Area | Status | Authoritative Decision / Contract |
|---|---|---|---|---|
| **PDR-001** | Infrastructure | Primary Cloud Hosting | `DECISION REQUIRED` | Target candidates: Railway / AWS / Render. Application is fully container-ready (Dockerized API & Next.js web application). Final selection pending Phase 11. |
| **PDR-002** | Infrastructure | Frontend CDN / Edge Hosting | `DECISION REQUIRED` | Target candidates: Vercel / Cloudflare Pages / AWS CloudFront. Standalone Next.js Node server mode tested and verified. |
| **PDR-003** | Database | Production PostgreSQL Host | `DECISION REQUIRED` | Requires PostgreSQL 16+ (tested against PostgreSQL 18.3). Native RLS (`app.current_user_id`, `app.current_tenant_id`) enforced. Target candidates: Neon, Supabase, AWS RDS, Railway PG. |
| **PDR-004** | Disaster Recovery | Recovery Point Objective (RPO) | `DECISION REQUIRED` | **RPO DECISION REQUIRED**. Non-binding engineering target: < 1 hour for commercial data. Subject to executive SLA and infrastructure tier selection. |
| **PDR-005** | Disaster Recovery | Recovery Time Objective (RTO) | `DECISION REQUIRED` | **RTO DECISION REQUIRED**. Non-binding engineering target: < 4 hours for full cluster restoration. Subject to executive SLA and infrastructure tier selection. |
| **PDR-006** | Disaster Recovery | Backup Retention Schedule | `DECISION REQUIRED` | **BACKUP RETENTION DECISION REQUIRED**. Non-binding operational recommendation: Daily snapshots retained 30 days, monthly retained 1 year. Subject to legal compliance review. |
| **PDR-007** | Payments | Commercial Billing Gateway | `DECISION REQUIRED` | Adapter pattern implemented (`BillingProvider` SPI). Provider-neutral baseline configured (`NOT_SELECTED`). Candidates: Stripe, Adyen, LemonSqueezy. Webhook signature verification and idempotency implemented. |
| **PDR-008** | Communications | Transactional Email Provider | `DECISION REQUIRED` | Adapter pattern implemented (`EmailTransport` SPI). Candidates: Resend, SendGrid, AWS SES, Postmark. Outbox queue pattern with exponential backoff and dead-letter handling active. |
| **PDR-009** | Artificial Intelligence | Generative AI LLM / Vision Provider | `DECISION REQUIRED` | Provider-agnostic client interface implemented. Candidates: Anthropic (Claude), OpenAI, Google Gemini. Strict rate limiting, prompt sanitization, token budgets, and quota enforcement active. |
| **PDR-010** | Storage | Object Storage (Media / Attachments) | `DECISION REQUIRED` | S3-compatible adapter implemented. Candidates: Cloudflare R2, AWS S3, Backblaze B2. Presigned URLs, content-type sniffing, magic byte validation, and upload quotas active. |
| **PDR-011** | Security | Malware Scanning Engine | `DECISION REQUIRED` | Multi-engine scanner interface implemented (`MalwareScanner` SPI). Candidates: ClamAV daemon, AWS GuardDuty / VirusTotal API. Fail-closed policy (`app.media.security.require-clean-scan=true`) enforced in production profiles. |
| **PDR-012** | Observability | Production Metrics & APM | `DECISION REQUIRED` | Spring Boot Actuator enabled. Candidates: Prometheus + Grafana, Datadog, New Relic. Proposed SLOs and alert rules documented as planning models requiring infrastructure selection. |
| **PDR-013** | Authentication | Identity & Session Architecture | **ADOPTED / IMPLEMENTED** | Stateful secure session tokens stored in PostgreSQL with SHA-256 hashing. HttpOnly, SameSite=Lax, Secure cookies. Multi-session concurrency limit (10 per user). Absolute and idle timeouts. |
| **PDR-014** | Authentication | Admin MFA Requirement | **ADOPTED / IMPLEMENTED** | RFC 6238 TOTP enforced for `ADMIN` and `SUPER_ADMIN` roles. Step-up challenge required for privileged operations within a 15-minute sliding window. Recovery codes hashed with Argon2id. |
| **PDR-015** | Data Isolation | Multi-Tenancy Architecture | **ADOPTED / IMPLEMENTED** | Shared database with Row Level Security (RLS) on all tenant-scoped tables (`projects`, `clients`, `proposals`, `invoices`, `media`). Studio-scoped tenant boundary enforced at SQL execution time. |
| **PDR-016** | Operations | Maintenance Mode Controls | **ADOPTED / IMPLEMENTED** | Three levels: `ACTIVE` (normal), `READ_ONLY` (blocks all state-changing mutations across regular users and super-admins, allowing only dedicated control-plane management endpoints), `OFFLINE` (503 Service Unavailable). |
| **PDR-017** | Privacy | GDPR / CCPA Compliance | **ADOPTED / IMPLEMENTED** | Automated data export generation (JSON bundle), account closure grace period (14 days), permanent hard deletion workflow with cascade cleanup and pseudonymization of financial audit records. |

---

## 3. Governance Process for Phase 11 Resolution

Before production deployment:
1. **Executive Stakeholder Sign-Off:** Executive leadership must select and approve the authoritative commercial billing gateway (PDR-007), cloud host (PDR-001/PDR-002), and database provider (PDR-003).
2. **Legal & Compliance Sign-Off:** General counsel must approve RPO/RTO SLAs (PDR-004/PDR-005) and Backup Retention (PDR-006).
3. **Operations Sign-Off:** Platform engineering must provision live API credentials and configure secrets into target environment secret managers (e.g., AWS Secrets Manager, Doppler, or Railway Variables).
