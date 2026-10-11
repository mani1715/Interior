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
| **PDR-001** | Infrastructure | Primary Cloud Hosting | `DECISION REQUIRED` | Target candidates: Railway / AWS / Render. Application is container-ready (Dockerized Spring Boot API & Next.js web application). Final selection pending Phase 11. |
| **PDR-002** | Infrastructure | Frontend CDN / Edge Hosting | `DECISION REQUIRED` | Target candidates: Vercel / Cloudflare Pages / AWS CloudFront. Standalone Next.js Node server mode tested and verified. |
| **PDR-003** | Database | Production PostgreSQL Host | `DECISION REQUIRED` | Requires PostgreSQL 16+ (tested against PostgreSQL 18.3, supported min 15, recommended 16/17 due to Flyway 10.20 notice). Native RLS (`app.current_user_id`, `app.current_studio_id`, `app.is_admin`) enforced across 59 tables. Target candidates: Neon, Supabase, AWS RDS, Railway PG. |
| **PDR-004** | Disaster Recovery | Recovery Point Objective (RPO) | `DECISION REQUIRED` | **RPO DECISION REQUIRED**. Non-binding engineering target: < 1 hour for commercial data. Subject to executive SLA and infrastructure tier selection. |
| **PDR-005** | Disaster Recovery | Recovery Time Objective (RTO) | `DECISION REQUIRED` | **RTO DECISION REQUIRED**. Non-binding engineering target: < 4 hours for full cluster restoration. Subject to executive SLA and infrastructure tier selection. |
| **PDR-006** | Disaster Recovery | Backup Retention Schedule | `DECISION REQUIRED` | **BACKUP RETENTION DECISION REQUIRED**. Non-binding operational recommendation: Daily snapshots retained 30 days, monthly retained 1 year. Subject to legal compliance review. |
| **PDR-007** | Payments | Commercial Billing Gateway | `DECISION REQUIRED` | Adapter pattern implemented (`BillingProvider` SPI). Provider-neutral baseline configured (`NOT_SELECTED`). Candidates: Stripe, Adyen, LemonSqueezy, Razorpay. Webhook signature verification and idempotency implemented. |
| **PDR-008** | Communications | Transactional Email Provider | `DECISION REQUIRED` | Adapter pattern implemented (`EmailTransport` SPI). Candidates: Resend, SendGrid, AWS SES, Postmark. Outbox queue pattern with exponential backoff and dead-letter handling active. Provider unconfigured (`NOT_CONFIGURED`). |
| **PDR-009** | Artificial Intelligence | Generative AI LLM / Vision Provider | `DECISION REQUIRED` | Provider-agnostic client interface implemented. Candidates: Anthropic (Claude), OpenAI, Google Gemini. Daily quota limit: 20 generations/studio/UTC calendar day (configurable default). Strict token budgets, prompt sanitization, and quota enforcement active. |
| **PDR-010** | Storage | Object Storage (Media / Attachments) | `DECISION REQUIRED` | S3-compatible adapter implemented. Candidates: Cloudflare R2, AWS S3, Backblaze B2. Presigned URLs, content-type sniffing, magic byte validation, and upload quotas active. |
| **PDR-011** | Security | Malware Scanning Engine | `DECISION REQUIRED` | Multi-engine scanner interface implemented (`MalwareScanner` SPI). Candidates: ClamAV daemon, AWS GuardDuty / VirusTotal API. Fail-closed policy (`app.media.security.require-clean-scan=true`) enforced in production profiles. |
| **PDR-012** | Observability | Production Metrics & APM | `DECISION REQUIRED` | Spring Boot Actuator enabled (only `health`, `info`, `metrics` exposed). Candidates: Prometheus + Grafana, Datadog, New Relic. Proposed SLOs and alert rules documented as planning models requiring infrastructure selection. |
| **PDR-013** | Authentication | Identity & Session Architecture | **ADOPTED / IMPLEMENTED** | Stateful secure session tokens stored in PostgreSQL with SHA-256 hashing. Cookie name: `__Host-session`, Secure=true, HttpOnly, SameSite=Lax. Idle timeout: 1800s (30m), Absolute timeout: 43200s (12h), Throttled last-seen: 300s (5m). Multi-session concurrency is unconstrained in application code; active sessions are tracked in `identity_sessions`. |
| **PDR-014** | Authentication | Admin MFA Requirement | **ADOPTED / IMPLEMENTED** | RFC 6238 TOTP enforced for `SUPER_ADMIN` and `ADMIN` roles. Configurable default `true` for `MODERATOR`. Optional for `DESIGNER` and `CUSTOMER`. Step-up challenge required for privileged operations within a 15-minute sliding window (config default 900s / 15m; subject to security policy review). Recovery codes hashed with SHA-256. Master encryption key fails closed if missing. |
| **PDR-015** | Data Isolation | Multi-Tenancy Architecture | **ADOPTED / IMPLEMENTED** | Shared database with Row Level Security (RLS) on all 59 entity tables. Studio-scoped tenant boundary enforced at PostgreSQL SQL execution time via session variables `app.current_user_id`, `app.current_studio_id`, `app.is_admin`. |
| **PDR-016** | Operations | Maintenance Mode Controls | **ADOPTED / IMPLEMENTED** | Canonical states: `NORMAL` (full platform read/write operational state) and `READ_ONLY` (blocks all mutating requests with HTTP 503 `MAINTENANCE_MODE_ACTIVE` for regular users; platform administrators restricted strictly to control-plane endpoints `/admin/operations/**`, `/admin/mfa/**`, `/admin/audit/**`, user status suspension, and studio moderation). |
| **PDR-017** | Privacy | Account Closure & Deletion Recovery Period | `DECISION REQUIRED` | **ACCOUNT CLOSURE / DELETION RECOVERY PERIOD DECISION REQUIRED**. Immediate account suspension and session termination implemented; 14-day deletion grace period is **CONFIG DEFAULT ONLY**, pending formal legal and regulatory review. |
| **PDR-018** | Support | Support Ticket Data Retention | `DECISION REQUIRED` | **SUPPORT RETENTION POLICY DECISION REQUIRED**. Non-binding operational recommendation: Closed tickets retained 2 years. Subject to compliance review. |
| **PDR-019** | Security | Malware Scan Ledger & Quarantine Retention | `DECISION REQUIRED` | **MALWARE RETENTION POLICY DECISION REQUIRED**. Non-binding operational recommendation: Quarantined metadata retained 90 days. Subject to security compliance approval. |

---

## 3. Governance Process for Phase 11 Resolution

Before production deployment:
1. **Executive Stakeholder Sign-Off:** Executive leadership must select and approve the authoritative commercial billing gateway (PDR-007), cloud host (PDR-001/PDR-002), database provider (PDR-003), and transactional email provider (PDR-008).
2. **Legal & Compliance Sign-Off:** General counsel must approve RPO/RTO SLAs (PDR-004/PDR-005), Backup Retention (PDR-006), Account Closure & Deletion Grace Period (PDR-017), Support Retention (PDR-018), and Malware Retention (PDR-019).
3. **Operations Sign-Off:** Platform engineering must provision live API credentials and configure secrets into target environment secret managers (e.g., AWS Secrets Manager, Doppler, or Railway Variables).
