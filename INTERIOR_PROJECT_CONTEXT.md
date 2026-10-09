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

Current Flyway:
V033 (`V033__admin_operations_and_moderation_controls.sql`)

Backend Core Status:
CLOSED & RELEASE READY

Next planned milestone:
Final Whole-Product Visual, UX, Performance & Production-Readiness QA.

Cinematic Portfolio:
COMPLETE (Standard is permanent content/fallback system, Cinematic is progressive enhancement)

Homepage Refinement:
COMPLETE (Discovery first, approved 11-section order, responsive, accessible, zero regression)

Subscriptions & Entitlements:
COMPLETE (Server-side quota enforcement, Pro 5-cinematic allocation, truthful usage breakdown, disabled-checkout boundary modal)

Security Hardening & Adversarial Audit:
COMPLETE (100 controls audited, 426 backend tests PASS, 352 frontend tests PASS, 0 Open Critical/High, release status: READY FOR RELEASE QA)

True 3D / 360 / guided camera capture:
DEFERRED

Deployment:
DEFERRED BY USER

---

# MANDATORY STARTUP PROCEDURE

Before performing any work in this repository, all agents (Astra, Antigravity, and future agents) MUST run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-interior-context.ps1
```

If this script fails:
STOP. Do not switch automatically to another repository. Do not search for another project with a similar name.
