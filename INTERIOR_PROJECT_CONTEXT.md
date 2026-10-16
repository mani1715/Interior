# PROJECT IDENTITY

Product:
Elégance — Interior Designer Platform

Repository:
C:\my projects\interior design

Remote:
https://github.com/mani1715/Interior.git

Branch:
main

Repository purpose:
A professional interior-design discovery, portfolio, AI visualization, lead-generation, collaboration, and studio-management platform.

---

# NOT THIS PROJECT

Explicitly state:

This repository is NOT Maneesh's personal developer portfolio.

Do not import or modify content relating to:
- MAJA Invites + Pixora
- Aishu Home Foods
- MSPN Dev
- SEC-OPS / Security Agency Platform
- personal résumé
- personal contact page
- developer showcase project cards

unless the user explicitly requests work on this Interior application that genuinely references them.

---

# TERMINOLOGY

Clarify:

"Portfolio" in this repository means interior-professional/studio portfolios.

It does NOT mean the personal developer portfolio (`C:\my projects\Portifolio\portfolio`).

---

# CURRENT DEVELOPMENT TRACK

Current completed milestone:
- Portfolio Evolution Phase 1: Studio → Project → Room → Photos (Canonical content model, multi-upload, spatial index, dual independent covers, photo inspector, interactive focal-point reticle modal, safe room deletion).
- Portfolio Evolution Phase 2: Public Room Gallery + Full-Screen Photo Viewer (`ProjectSpaceShowcase`, sticky room pills, room-scoped photo viewer, zoom/pan controls, contact sheet, and EnquirySheet handoff).
- Portfolio Evolution Phase 3: Cinematic Portfolio (`ProjectPresentationMode`, Flyway V029, pure deterministic eligibility engine, Focus Push & Gentle Drift scroll-driven transforms, matte framing, transform ownership `data-cinematic-controlled="true"`, portrait containment, real `#room-<roomId>` anchor deep linking, and owner presentation settings).
- Implementation Phase 4: Homepage Refinement (Discovery-first visitor architecture: PublicHeader with 64px mobile bar & shortcut, BrandHero 180svh desktop motion with unpinned mobile/tablet fallback, Immediate Project Search, Projects to Explore, Professional Discovery with live API + fallback, Cinematic Portfolio Introduction, Inspiration Categories, Secondary AI Visualizer with disclosure, Trust Explanations, Visitor & Professional Dual CTAs).
- Implementation Phase 5: Subscription & Entitlement Implementation (Flyway V030, Standard/Premium/Pro commercial plan model, server-side project & photo-per-project quota enforcement, upload intent reservations, Cinematic presentation entitlement & 5-project allocation cap inside Pro tier, truthful Plan & Usage workspace with legacy BASE preservation, safe over-limit policy with zero deletions, safe subscription expiration fallback to Standard).
- Phase 6: Security Hardening & Adversarial Audit (Full 100-control audit, magic-byte upload validation, unbounded pagination guards, adversarial regression test suite, zero open Critical/High vulnerabilities, RLS & multi-tenant isolation verified).
- Phase 7A: Authentication & Workspace Access Flow (Server-side opaque sessions in PostgreSQL, SHA-256 hashed tokens, `__Host-session` cookie, `ActorContext` status resolution & suspended account rejection, deterministic role-aware post-login routing, open redirect defense, multi-studio switching with `X-Studio-Id`, transactional professional onboarding, and studio invitation flow).
- Phase 7B: Project Lifecycle, Publishing Gates, Enquiries & Client Workflow (Draft/Ready state machine, publishability evaluation gate, explicit idempotent unpublishing, transactional soft-archive, anti-abuse honeypot and rate-limited public enquiries, same-studio project attribution and active member assignment in CRM leads, WON lead verification gate for client review invitations, and multi-tenant RLS isolation).
- Phase 7C: CRM, Notifications & Communication Delivery Completion (Authoritative In-App notification system, SSE real-time stream with heartbeats, unread counting and batch mark read, external delivery tracking in `communication_deliveries` with RLS, fail-safe transaction decoupling, XSS/host-header hardened `EmailTemplateService`, truthful provider reporting with `NOT_CONFIGURED`, self-action spam suppression).
- Phase 7D: AI, Media Storage, Upload Lifecycle & Failure Recovery Completion (Two-phase commit upload intents, byte/photo quota enforcement, slot-preserving media replacement, SSRF defense service with IP resolution & redirect checks, decompression bomb 50MP limits, asynchronous AI timeout recovery, automated storage reconciliation report, multi-tenant RLS isolation across media & AI tables).
- Phase 7E: Admin, Moderation, Audit & Operational Controls Completion (Segregated trust domains, Super Admin privilege boundaries, self-suspension/lockout protection, session invalidation on suspension, content moderation with PostgreSQL RLS `public_read_studio_projects` moderation gating, truthful delivery audit with recipient PII masking, operational health diagnostics, storage reconciliation, and stuck AI job recovery).
- Phase 7F: Final Security, API, Concurrency & End-to-End Release Audit (Whole-system release audit, complete API inventory, 34-table RLS verification, quota concurrency verification, secrets hygiene, zero critical/high findings, backend core verified release-ready).
- Final Whole-Product QA & Production-Readiness Review: Visual, UX, Accessibility, Performance & Infrastructure Readiness complete. 485/485 Backend PASS, 30/30 PostgreSQL 18.3 RLS PASS, 368/368 Frontend Vitest PASS, Next.js 16.3.3 Turbopack build clean, Zero release-blocking code defects. Canonical release decision: READY FOR PHASE 8 CLOSED-BETA INFRASTRUCTURE SETUP.
- Phase 9A: Commercial Billing, Payment & Subscription Lifecycle Architecture (Flyway V034, `billing_checkout_sessions` table with RLS tenant isolation, provider-neutral abstraction, server-authoritative checkout sessions, idempotency protection, cryptographic webhook signature verification, replay deduplication, tamper-resistant amount/currency checks, period-end cancellation, safe scheduled downgrade with zero data loss, admin manual override source separation, bounded transaction history endpoint, in-app billing notifications, 496/496 Backend PASS, 34/34 PostgreSQL 18.3 RLS PASS, 368/368 Frontend Vitest PASS, clean Next.js 16.3.3 build).
- Phase 9B: Transactional Email & Communication Delivery Productionization (Flyway V035, `communication_deliveries` status expansion to `BOUNCED` and `COMPLAINT`, `EmailProvider` neutral abstraction with `DisabledEmailProvider` and deterministic `FakeEmailProvider`, bounded exponential backoff retry scheduler with PostgreSQL `FOR UPDATE SKIP LOCKED` concurrency protection, public webhook reconciliation endpoint `/public/webhooks/email/{provider}` with HMAC-SHA256 signature verification, CRLF header injection rejection, server-configurable billing grace period via `app.billing.grace-period-days`, transactional email wiring for team invitations, review invitations, billing activations, renewals, cancellations, payment failures, and account security notices, 503/503 Backend PASS, 22/22 PostgreSQL 18.3 RLS PASS, 368/368 Frontend Vitest PASS, clean Next.js 16.3.3 Turbopack build, ESLint 0 warnings/errors).

- Phase 9C: Production AI Provider, Usage, Cost Controls & Privacy Hardening (Flyway V036, `ai_cost_ledger` table with RLS tenant isolation, multi-currency provider accounting, `TIMED_OUT` job state, atomic quota reservation with `GENERATION_RESERVED` and `RESERVATION_REFUNDED`, locked 20 generations/studio/day limit reset at 00:00 UTC, four-tier budget guards with global/studio kill switches and monthly spend ceilings, autonomous stuck-job recovery, 505/505 Backend PASS, 33/33 PostgreSQL 18.3 RLS PASS across 9 classes, 368/368 Frontend Vitest PASS, clean Next.js 16.3.3 Turbopack build, ESLint 0 warnings/errors).

- Phase 9D: Admin MFA, Account Security, Session Assurance & Recovery Architecture (Flyway V037, `identity_mfa_methods` and `identity_recovery_codes` tables with strict uniqueness & RLS user isolation, RFC 6238 TOTP with AES-256-GCM encrypted secrets at rest and key versioning, 10 one-time SHA-256 hashed recovery codes with atomic single-use redemption, session `mfa_verified_at` elevation to `MFA_VERIFIED`, 15-minute step-up freshness requirement on privileged actions, Super Admin emergency reset with audit & session termination, self-bypass defense, user self-service TOTP enrollment and backup code regeneration, revoke all other active sessions endpoint, 508/508 Backend PASS, 36/36 PostgreSQL 18.3 RLS PASS across 10 classes, 368/368 Frontend Vitest PASS, clean Next.js 16.3.3 Turbopack build, ESLint 0 warnings/errors).

- Phase 9E: Privacy, Data Export, Account Closure, Anonymization & Retention Architecture (Flyway V038, `privacy_requests`, `data_export_jobs`, `privacy_legal_holds` with PostgreSQL 18 RLS isolation, sanitized ZIP archive generator for personal and studio data with CSV formula injection defense, sole-admin studio blocker preventing orphaned active studios, privileged legal hold defense blocking destructive erasure during litigation/audit, self-service account closure and permanent anonymization, studio closure unpublishing and archiving, accounting and audit ledgers preserved in pseudonymized state, 39/39 PostgreSQL 18.3 RLS PASS across 11 classes, 368/368 Frontend Vitest PASS, clean Next.js 16.3.3 Turbopack build, ESLint 0 warnings/errors).

- Phase 9F: Media Malware Scanning, Quarantining & Production Upload Security (Flyway V039, `media_security_scans` audit ledger table with RLS tenant isolation, `upload_intents` scan status state machine, provider-agnostic `MalwareScanner` abstraction with `DisabledMalwareScanner` and `FakeMalwareScanner`, fail-closed production commit gate with clean-only invariant, slot-preserving media replacement protection, quota release on infection, 516/516 Backend PASS, 41/41 PostgreSQL 18.3 RLS PASS across 12 classes, 368/368 Frontend Vitest PASS, clean Next.js 16.3.3 Turbopack build, ESLint 0 warnings/errors).

- Phase 9G: Support + Operations + Incident Controls + Commercial Admin Completion (Flyway V040, `support_requests`, `support_messages` with internal admin notes & in-app user reply dispatch, `platform_operational_state` with `NORMAL` vs `READ_ONLY` maintenance mode and HTTP 503 `MAINTENANCE_MODE_ACTIVE` fail-closed interceptor, granular kill switches `uploads_enabled`, `ai_enabled`, `registrations_enabled`, `operational_job_runs` with PostgreSQL advisory locking across 6 core tasks, RFC-compliant `X-Request-Id` correlation with MDC propagation, truthful provider matrix reporting, 537/537 Backend PASS, 45/45 PostgreSQL 18.3 RLS PASS across 13 classes, 368/368 Frontend Vitest PASS, clean Next.js 16.3.3 Turbopack build, ESLint 0 warnings/errors).

- Phase 10: Final 100% Production Release Audit & Defect Closure (Exhaustive 166-point audit across 30 domains; Pre-flight remediations: RPO/RTO, Backup Retention, Support Retention, Malware Retention marked DECISION REQUIRED in runbooks; payment provider corrected to neutral NOT_SELECTED; MaintenanceModeInterceptor hardened to restrict Super Admin mutations to control-plane endpoints during READ_ONLY; canonical Flyway verified at exactly 40 migrations V001-V040; account closure/deletion recovery period designated as DECISION REQUIRED with 14 days as CONFIG DEFAULT ONLY; maintenance states strictly NORMAL and READ_ONLY; local PostgreSQL 18.3 drills executed: fresh DB migration of V001-V040, upgrade migration from V033 to V040, backup and restore drill with pg_dump/pg_restore; Next.js security patch to 16.3.8 and sharp 0.35.5; strict remote image host validation with wildcard elimination; fail-closed audit semantics on all admin/privacy mutations; quality gates: 545/545 Backend tests PASS, 45/45 PostgreSQL 18.3 RLS tests PASS across 13 classes, 368/368 Frontend Vitest tests PASS across 52 suites, 0 TypeScript errors, 0 ESLint warnings/errors, clean 28-route Next.js 16.3.8 production build, 0 committed secrets; Release Candidate Approved).

- Phase 12A: Cloud Media Storage & Malware Scanning Foundation (Cloudflare R2 selected as production object storage provider; S3StorageService full contract verified; ClamAvMalwareScanner verified; live cloud credentials deferred to Phase 15 pre-launch activation).
- Phase 12B: PostgreSQL Database Production Readiness (Clean bootstrap V001..V040 from zero verified in 1.49s; 59 tenant/user tables forced Row Level Security verified across 14 test classes; pg_dump and pg_restore tooling verified; real destructive restore drill verified with 100% data fidelity and schema re-validation; HikariCP leak detection and in-memory H2 rejection in production profile; managed cloud DB activation deferred to Phase 15).
- Phase 12C: Production Authentication & OIDC Readiness (Server-side confidential authorization code flow, PKCE S256, RFC 7517 caching JWKS resolver with key rotation auto-recovery, RS256 native cryptographic ID token verification, claim enforcement, anti-takeover collision defense, destination routing, privileged MFA step-up, ProductionConfigurationValidator OIDC hardening; 16/16 Mock OIDC integration tests PASS, 603/603 backend tests PASS, 373/373 frontend tests PASS; live Google cloud activation deferred to Phase 15).
- Phase 12D: Production AI Visualizer & Image Editing Readiness (Domain-focused interior visualization: wardrobe/cupboard colors, cabinet/door materials, wall/cabinet finishes, reference swatches, room geometry preservation; provider-neutral ConfigurableAiImageProvider; SSRF defense service on output URLs; pre-persistence ClamAV malware scanning; 20 generations/studio/day quota; multi-currency cost ledger; fail-fast production startup validator; Web Speech API client-side dictation; precision mask canvas editor; 13/13 Local AI integration tests PASS, 5/5 SSRF & output tests PASS, 6/6 AI validator tests PASS; live AI provider activation deferred to Phase 15).

- Phase 12E: Transactional Email & Communication Delivery Production Readiness (Strictly transactional operational communications: studio invitations, client review invites, lead alerts, feedback alerts, subscription lifecycle, security alerts, privacy export notices; Resend REST API provider via native Java 21 HttpClient; AWS SES provider via AWS SES v2 SDK; OWASP HTML entity escaping; CRLF header injection neutralization; out-of-order webhook delivery defense with monotonic status guard; secret-safe masked logging; canonical base URL normalization; fail-fast production startup validator; opt-in live integration verification harness; 40/40 email tests PASS; live email provider activation deferred to Phase 15).

Current Flyway:
V040 (`V040__support_and_operations_foundation.sql` - exactly 40 canonical PostgreSQL migrations)

Whole-Product Release Status:
PHASE 12E TRANSACTIONAL EMAIL PRODUCTION READINESS COMPLETE. CLOUD PROVIDER ACTIVATION DEFERRED TO PHASE 15 (PRE-LAUNCH ACTIVATION). FEATURE FREEZE ACTIVE. UI/UX FREEZE ACTIVE. NO LIVE DEPLOYMENT.

Next planned milestone:
PHASE 12F: COMMERCIAL BILLING / PAYMENT GATEWAY PRODUCTION READINESS (OR USER DIRECTED NEXT PHASE).

Cinematic Portfolio:
COMPLETE (Standard is permanent content/fallback system, Cinematic is progressive enhancement)

Homepage Refinement:
COMPLETE (Approved 40/60 editorial split, responsive, accessible, zero regression)

Subscriptions & Entitlements:
COMPLETE (Locked commercial tiers: Standard 10 proj/15 photos, Premium 20 proj/25 photos, Pro 20 proj total/30 photos/5 cinematic, legacy BASE preserved, server-side quota enforcement, disabled-checkout boundary modal)

Security Hardening & Adversarial Audit:
COMPLETE (100 controls audited, 485 backend tests PASS, 30 PostgreSQL tests PASS, 368 frontend tests PASS, 0 Open Critical/High)

True 3D / 360 / guided camera capture:
DEFERRED

Deployment:
DEFERRED (Awaiting closed beta infrastructure configuration)

---

# MANDATORY STARTUP PROCEDURE

Before performing any work in this repository, all agents (Astra, Antigravity, and future agents) MUST run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-interior-context.ps1
```

If this script fails:
STOP. Do not switch automatically to another repository. Do not search for another project with a similar name.
