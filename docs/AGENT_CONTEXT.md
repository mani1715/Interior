# AGENT CONTEXT

Last updated: 2026-09-17, Antigravity Phase 03 Security & Application Implementation PASS.
Canonical cross-agent state: maintain this file, never create numbered/replacement handoff files.
Both agents use the SAME LOCAL workspace: `C:\my projects\interior design`.
Read repository evidence before acting; no chat history is required or authoritative.

## PROJECT

Production startup platform combining Interior Designer Portfolio + SEO Discovery
+ AI Interior Visualizer + Project Discovery + Client Collaboration + Leads/CRM.
Serve interior designers/studios, architects, furniture/modular/woodwork and turnkey professionals.
India/mobile-first, with English initially and localization-ready content.
This is a combined product, not merely a directory or image generator.

## SOURCE OF TRUTH

Read these specifications; do not replace them with assumptions from a coding agent.
Paths below are relative to this document; filenames are the canonical repository documents.

- [MASTER_PRODUCT_SPEC.md](MASTER_PRODUCT_SPEC.md): product authority and all invariants.
- [00_REQUIREMENTS_MATRIX.md](00_REQUIREMENTS_MATRIX.md): 145 stable requirements and release scope.
- [00_ARCHITECTURE_DECISIONS.md](00_ARCHITECTURE_DECISIONS.md): approved decisions and explicit supersessions.
- [00_RISK_REGISTER.md](00_RISK_REGISTER.md): 36 OPEN risks; documentation does not close them.
- [00_IMPLEMENTATION_ROADMAP.md](00_IMPLEMENTATION_ROADMAP.md): all phases 00–29 and later milestones.
- [00_REPOSITORY_AUDIT.md](00_REPOSITORY_AUDIT.md): historical empty-workspace baseline.
- [01_PRODUCTION_ARCHITECTURE.md](01_PRODUCTION_ARCHITECTURE.md): stack, topology and contracts.
- [01_MODULE_BOUNDARIES.md](01_MODULE_BOUNDARIES.md): 16 modules, allowed calls and transaction ownership.
- [01_SECURITY_ARCHITECTURE.md](01_SECURITY_ARCHITECTURE.md): identity, sessions, policy and privileged access.
- [01_MEDIA_AI_ARCHITECTURE.md](01_MEDIA_AI_ARCHITECTURE.md): private media, watermark, workers and AI.
- [01_SEO_RENDERING_ARCHITECTURE.md](01_SEO_RENDERING_ARCHITECTURE.md): rendering, canonicals and live eligibility.
- [01_DEPLOYMENT_ARCHITECTURE.md](01_DEPLOYMENT_ARCHITECTURE.md): environments, tooling, CI, recovery and cost.
- [01_TESTING_STRATEGY.md](01_TESTING_STRATEGY.md): implementation gates and actual Phase01 checks.
- [01_ROUTE_NAMESPACE.md](01_ROUTE_NAMESPACE.md): route ownership, reserved roots and domain contracts.
- [02_DOMAIN_MODEL.md](02_DOMAIN_MODEL.md): aggregates, examples, transactions and eight relationship diagrams.
- [02_DATABASE_SCHEMA.md](02_DATABASE_SCHEMA.md): 94-table design dictionary, not executable SQL.
- [02_DATA_OWNERSHIP_SECURITY.md](02_DATA_OWNERSHIP_SECURITY.md): ownership matrix, RLS, PII and retention.
- [02_ENTITY_LIFECYCLES.md](02_ENTITY_LIFECYCLES.md): canonical states and transitions.
- [02_TAXONOMY_STRATEGY.md](02_TAXONOMY_STRATEGY.md): typed dimensions and reference seeds.
- [02_INDEX_QUERY_STRATEGY.md](02_INDEX_QUERY_STRATEGY.md): bounded queries, indexes and mobile summaries.
- [02_MIGRATION_STRATEGY.md](02_MIGRATION_STRATEGY.md): progressive Flyway plan and Phase02 validation evidence.
- [README.md](../README.md), [.gitignore](../.gitignore), [.editorconfig](../.editorconfig): root guidance/conventions.

## COMPLETED PHASES

- **PHASE 00 — PASS:** Master Product Specification; 145 requirements recorded.
- **PHASE 01 — PASS:** Production architecture selected; eight architecture documents.
- **PHASE 02 — PASS:** Database/domain architecture designed; seven design documents.
- **PHASE 03 — PASS:** Security Foundation + Actual Application Implementation Start. Real monorepo codebase created (`apps/web`, `apps/api`), JDK 25 installed, PostgreSQL 18 migrations applied, security foundation & RLS implemented, cross-tenant isolation tests PASSED, Next.js build PASSED.

## CURRENT IMPLEMENTATION STATE

- **MONOREPO CODEBASE IMPLEMENTED:**
  - `apps/web`: Next.js 16.3.3, React 19.3, TypeScript 6.0.3, App Router, design tokens with locked palette, security headers, mobile-first 360px-430px base, health diagnostic, API client foundation. Next.js build validated clean.
  - `apps/api`: Java 25 (Temurin 25.0.4.1), Spring Boot 3.4.3 / 4.1.1, Flyway versioned SQL migrations, Maven wrapper checked in. Opaque 256-bit hashed session tokens (`__Host-session`), CSRF protection, request correlation filter (`X-Request-Id`), error envelope, authorization service, RLS defense-in-depth. Maven test suite passed with 8 tests (0 failures, 0 errors).
  - `database`: PostgreSQL 18 running on port 5433 (database `interior_design_dev`). Applied `V001__security_identity_tenant_schema.sql` and `V002__pg_rls_policies.sql`.
- Git repository initialized. `.gitignore` protects credentials and build output.

## SELECTED ARCHITECTURE

| Area | Selected baseline |
|---|---|
| Frontend | Next.js 16.3.3, React 19.3.0, TypeScript 6.0.3; App Router/server-first |
| Web tooling | Node 24.19.0 LTS; native npm workspaces; CSS variables/CSS Modules |
| Backend | Java 25 (Temurin 25.0.4.1), Spring Boot 3.4.3 / 4.1.1; modular monolith |
| Java tooling | Maven 3.9.9 checked-in wrapper (`mvnw.cmd`); separate native build |
| Database / migration | PostgreSQL 18.3; Flyway versioned SQL, native `uuidv7()` |
| Cache | No initial Redis; PostgreSQL authoritative for sessions, quotas and sensitive counters |
| Storage | Private S3-compatible abstraction, initial AWS S3 |
| Public media / CDN | Approved watermarked derivatives only; CloudFront restricted origin/OAC |
| Queue | SQS Standard + transactional outbox + destination dispatch |
| Search | PostgreSQL initial search abstraction; rebuildable published projections |
| API / contracts | REST `/api/v1`; OpenAPI 3.1 contract-first |
| Authentication | Managed OIDC abstraction; backend-owned opaque browser sessions |

## CURRENT ENVIRONMENT

- Windows workstation; PowerShell; shared local workspace `c:\my projects\interior design`.
- Java 25 installed at `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot` (Temurin 25.0.4.1+1-LTS).
- Node 22.18.0 / npm 10.9.3 active for web execution; Node 24.19.0 LTS qualified.
- PostgreSQL 18.3 active on port 5433; database `interior_design_dev` created and migrated.
- Git repository initialized.

## RECENT VERIFICATION EVIDENCE

- `npm run typecheck` (apps/web): **PASS**
- `npm run build` (apps/web): **PASS** (Next.js 16.3.3 Turbopack build succeeded in 9.6s)
- `./mvnw clean test` (apps/api): **PASS** (8 tests run, 0 failures, 0 errors)
- `TenantIsolationTest`: **PASS** (Verified Studio A member denied access to Studio B resource)
- PostgreSQL 18 Migrations: `V001` and `V002` applied cleanly on port 5433 (PASS)

## NEXT PHASE

**PHASE 04 — GLOBAL DESIGN SYSTEM.**
Phase 03 completed successfully. Await Phase 04 prompt.
