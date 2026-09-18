# AGENT CONTEXT

Last updated: 2026-09-18, Antigravity Phase 06 Public Discovery UI PASS.
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
- **PHASE 04 — PASS:** Global Design System (Actual UI Implementation — Mobile-First). Complete reusable UI library created in `apps/web/src/components/`, locked palette tokens with WCAG 2.2 AA (5.35:1) compliance, responsive layout primitives, feedback/alerts/dialogs/sheets, media components (BeforeAfterSlider, BeforeAiReality with mandatory AI disclaimer badge, WatermarkPreview), interactive showcase route `/design-system` (marked `noindex`), Vitest test suite (20 tests passed), TypeScript and Next.js Turbopack build PASSED.
- **PHASE 04.1 — PASS:** Design System Canonicalization & Runtime Decisions Locked. Restored exact 12 canonical brand tokens (`#1F1F1F`, `#FAF8F5`, `#E7E1D8`, `#B88A5A`, `#2E5D4B`, `#3E6D8C`, `#C76F4A`, `#E6C9C3`, `#FFFFFF`, `#F4F4F4`, `#D9D9D9`, `#6B6B6B`). Clearly separated brand tokens from accessible semantic derived UI tokens (`--status-warning-text: #9E4522`, etc.). Formally marked Spring Boot 3.4.3 running on JDK 25 with Java 21 compiler bytecode target as canonical backend baseline.
- **PHASE 05 — PASS:** Homepage + 3D Experience (Production Implementation). Production homepage implemented in `apps/web/src/app/page.tsx` replacing the Phase 03 shell. Features 16 integrated sections communicating the unified platform: Hero (single `<h1>`, dual-audience copy, prioritized LCP visual), Core Value Strip, Signature 7-Stage Architectural Transformation Experience (interactive scrubber, accessible screen-reader fallback, layered vector compositing saving >600KB JS over WebGL and eliminating canvas crash risks), Portfolio Platform Section (watermark protection, client hub), Public Discovery Preview (search preview, trending chips, regional context), AI Concept Visualizer (4-step workflow, prompt demonstration, mandatory legal disclaimer), Before AI Reality narrative, Built for Google Discoverability Section (structured data, snippet preview), Project Inspiration Gallery (category filtering chips), Professional Practitioner Types, 6-Step Workflow, Lead Generation Engine (WhatsApp inquiry preview), Mobile-First On-Site Workflow, Final Call-To-Action, and Production Footer. Strict mobile-first design (360-430px base). WebSite and Organization JSON-LD schemas included. 27 unit tests passing across 6 test files. Next.js Turbopack build and Spring Boot backend test suite all 100% PASS.
- **PHASE 05.1 — PASS:** Homepage Truthfulness & SEO Correction Pass. Audited all visible homepage claims, mock data, badges, and links. Removed all unsupported "verified" assertions (verification system not yet implemented). Replaced with truthful regional positioning and professional trade descriptors. Cleaned SEO claims to clarify architecture foundation vs independent search engine ranking. Updated mock Google snippet to neutral SERP preview labeled Example Search Appearance. Refined AI disclaimer to canonical concise architectural wording. Audited all links to point to safe section anchors without broken routes or fake feature pages. Cleaned JSON-LD structured data and social metadata. 27 frontend tests and 8 backend tests PASS.
- **PHASE 06 — PASS:** Public Discovery UI (Project-First Interior Discovery Experience). Built the complete public discovery UI architecture, client islands, centralized demo query boundary, and route hierarchy: `/projects` (Project Catalog with search, filters, sorting, responsive grid), `/projects/[projectSlug]` (Project Detail Story with gallery, technical specifications, before/after slider, before->AI->reality narrative with mandatory AI disclaimer badge, creator attribution, and related projects), `/professionals` (Professional Directory with trade tabs and city filtering), `/professionals/[professionalSlug]` (Public Studio Profile with services, specialties, and published projects), `/categories/[categorySlug]` (Category Landing with SEO content, popular styles, and filtered projects), and `/locations/[locationSlug]` (Location Landing with regional design context, popular categories, and local studios). Implemented "I Want Something Similar" enquiry modal with project context and honest development preview notice (zero fake backend lead transmission). Strict mobile-first design, single `<h1>` per route, JSON-LD BreadcrumbList and Article schemas, 404 handling via `notFound()`. 57 frontend tests across 7 test files, full TypeScript check, ESLint, Next.js build, and Spring Boot backend tests all 100% PASS.
- **PHASE 07 — PARTIAL:** Authentication & Roles (Production Identity, Session & Authorization Implementation). Full security architecture implemented: backend-owned opaque 256-bit sessions (`identity_sessions`), SHA-256 hashed tokens, secure cookie transport (`__Host-session`), CSRF synchronizer token rotation/verification, in-memory sliding-window bucket rate limiter (`RateLimiterService`), replay-resistant single-use OIDC transaction store (`OidcTransactionStore`), provider-neutral OIDC boundary (`OidcService`), safe account linking preventing silent account takeover on email collision, least-privilege customer baseline role on registration, strictly studio-scoped `DESIGNER_TEAM` authorization, privileged MFA assurance enforcement (`requireMfaAssurance`), dev/test auth persona sandbox (`dev-login` strictly guarded by `@Profile({"dev", "test"})` AND `app.security.dev-auth-enabled=true`), open redirect defense (`sanitizeRedirectUrl`), auth-aware navigation (`PublicHeader`), and mobile-first frontend auth pages (`/sign-in`, `/sign-up`, `/auth/callback`, `/auth/error`, `/account`). Status is truthfully PARTIAL because complete architecture and runtime are verified locally, while external live production OIDC provider credentials await production environment provisioning.
- **PHASE 07.1 — PARTIAL (SECURITY CLOSURE COMPLETE):** Authentication Security Closure. Closed all 18 security verification gaps:
  1. Durable database-backed OIDC transaction store (`auth_oidc_transactions` Flyway migration `V003__auth_oidc_transactions.sql`) with atomic SQL consumption (`UPDATE ... SET consumed_at = ? WHERE state = ? AND consumed_at IS NULL AND expires_at > ?`), eliminating process-local memory vulnerability in multi-instance load-balanced deployments.
  2. Protocol-level ID token claims validation (`validateIdTokenClaims`) covering issuer mismatch, audience mismatch, token expiration, and nonce mismatch.
  3. PKCE S256 verification (`verifyPkce`) testing SHA-256 code challenge match and replay rejection.
  4. Trusted MFA assurance source (`deriveAssurance`) strictly derived from IdP `acr`/`amr` claims (`gold`, `phr`, `webauthn`, `fido`, `otp`, `sms`); ordinary sessions cannot self-upgrade.
  5. Dev auth profile matrix test (`DevAuthSecurityTest`) verifying `DevAuthService` is absent under `production`, `staging`, and default profiles even if `dev-auth-enabled=true`, and verifying absolute absence and complete rejection of `X-User-Id`, `X-Role`, and `X-Studio-Id` headers.
  6. Session lifecycle & revocation: current session revocation, user revocation isolation, and canonical account states enforcement (only `ACTIVE` accounts hold valid sessions; `PENDING`, `SUSPENDED`, and `DELETED` accounts are rejected).
  7. Configuration-driven session timeouts (`AuthSecurityProperties`) and write throttling (zero DB writes inside 300s window).
  8. CSRF closure: session-bound CSRF tokens, `Cache-Control: no-store, private` header on `/csrf`, cross-session CSRF token rejection, and CSRF enforcement on `POST /auth/logout` and `POST /auth/revoke-all`.
  9. Rate limiting: sliding-window in-memory rate limiter (`RateLimiterService`) tested for capacity thresholds, IP isolation, and recovery after duration window.
  10. Truthful `/auth/providers` contract returning only configured providers (empty if unconfigured) with truthful frontend rendering (zero fake buttons).
  11. Open redirect defense across frontend and backend sanitized against `//`, `/\`, `\\`, `javascript:`, `data:`, `%2f`, `%5c`, and CRLF injection.
  12. Least privilege role enforcement: professional intent (`DESIGNER`) receives `CUSTOMER` role only on registration; injected roles rejected.
  13. `DESIGNER_TEAM` tenant isolation verified with explicit test `testDesignerTeamCrossStudioAccessDenied`.
  14. `/auth/me` audit confirmed zero exposure of session tokens, hashes, or CSRF secrets.
  15. Audit logging sanitization confirmed zero logging of tokens, codes, secrets, or PKCE verifiers.
  16. Production cookie policy: `__Host-session`, Secure, HttpOnly, Path=/, SameSite=Lax.
  17. SpringDoc OpenAPI 3.1 annotations on `AuthController`.
- **PHASE 08.1 — PASS:** Designer Onboarding Closure & Invariant Audit.
  - Normalized Specialty Storage: Migrated schema via `V005__studio_specialties_and_onboarding_closure.sql` creating `studio_specialties` table with unique constraint `(studio_id, specialty_code)`. Implemented `CanonicalSpecialty` enum with 8 canonical design specialties (`MODERN_MINIMALIST`, `WARM_CONTEMPORARY`, `INDIAN_TRADITIONAL`, `NEO_CLASSICAL`, `SCANDINAVIAN`, `INDUSTRIAL`, `LUXURY_ECLECTIC`, `BIOPHILIC`). Non-canonical specialties rejected with HTTP 400 `BadRequestException`, inputs deduplicated before insertion, and retrieved as structured lists in studio profiles.
  - Canonical Initial Onboarding Idempotency: Created `designer_onboarding_completions` table (`user_id` PRIMARY KEY, `studio_id`, `completed_at`). Repeated completion calls return the existing studio summary idempotently without creating duplicate studios, duplicate slug claims, or duplicate roles.
  - Concurrency & Race Condition Guarantee: Multi-threaded test verified that concurrent near-simultaneous completions produce exactly 1 studio, 1 slug claim, 1 OWNER membership, 1 DESIGNER role, and 1 completion record, with zero orphan rows left behind.
  - Transaction Rollback Guarantee: Integration test verified that any mid-transaction failure completely rolls back all operations across all 9 related tables (0 studios, 0 slug claims, 0 contacts, 0 services, 0 specialties, 0 areas, 0 members, 0 roles, 0 completions).
  - Tenant Bootstrap & RLS Invariant: Verified creator gains OWNER access to the new studio while other authenticated users are strictly DENIED (HTTP 403 / `AccessDeniedException` tenant isolation violation). Tenant context is strictly derived from session/database membership, never from client-supplied IDs.
  - Mass Assignment Defense: Verified client payload injection of `userId`, `ownerUserId`, `studioId`, `tenantId`, `role`, `roles`, `permissions`, `publicationStatus`, `status`, `verified`, and `isAdmin` is completely ignored during completion and stripped from JSON drafts.
  - Privilege Elevation & Session Rotation: Tested full session rotation lifecycle on completion: old session invalidated (revoked in DB), new session created and attached to response cookie, CSRF token rotated, new session validates with `DESIGNER` platform role and `OWNER` studio membership, and subsequent access with old session returns 401 Unauthorized.
  - Slug Claim Invariants: Global uniqueness enforced; partial unique index on `(studio_id)` for state `CURRENT`; check constraint `chk_reserved_null_studio` prevents reserved slugs from being claimed as current; duplicate claims rejected with HTTP 409 Conflict.
  - Truthful Copy & Privacy Compliance: Replaced "Licensed architect" with "Architectural professional"; replaced "Verified Designer Role" with "Professional access enabled" and "DESIGNER (Role Activated)"; confirmed GSTIN format-only validation (no fake "Government Verified" claim); confirmed contact `public_consent` stored and isolated from public discovery; confirmed zero fake logo/media endpoints.
  - PostgreSQL 18 Migration: Applied `V005__studio_specialties_and_onboarding_closure.sql` to live PostgreSQL 18.3 on port 5433 (database `interior_design_dev`), bringing schema version to `v005`.
  - Verification Suite: 87 backend tests (0 failures, 0 errors) in Maven; 73 frontend tests (0 failures) in Vitest; `tsc --noEmit` clean exit 0; `eslint .` clean exit 0; Next.js Turbopack build succeeded.

## CURRENT IMPLEMENTATION STATE

- **MONOREPO CODEBASE IMPLEMENTED:**
  - `apps/web`: Next.js 16.3.3, React 19.3.0, TypeScript 6.0.3, App Router, Vitest test suite, ESLint 9, design tokens with exact 12-color locked brand palette, mobile-first 360px-430px base, health diagnostic, full component library, showcase route `/design-system` (`noindex`), full public homepage (`/`), 6 public discovery routes, auth pages (`/sign-in`, `/sign-up`, `/auth/callback`, `/auth/error`, `/account`), professional onboarding wizard (`/onboarding/professional`) with hardened truthful copy, auth-aware `PublicHeader`, and zero-token client session context (`AuthProvider`). Next.js Turbopack build clean, 73 unit/integration tests passed across 9 test files.
  - `apps/api`: Java 25 (Temurin 25.0.4.1 runtime, compiler target/release 21), Spring Boot 3.4.3, Flyway versioned SQL migrations (V001, V002, V003, V004, V005), Maven wrapper checked in. Opaque 256-bit hashed session tokens (`__Host-session`), database-backed durable OIDC transactions, CSRF protection, request correlation filter (`X-Request-Id`), error envelope, rate limiting, provider-neutral OIDC service, dev/test auth persona adapter, tenant-aware authorization service with studio scoping, professional onboarding service with role promotion and slug claim registry. Maven test suite passed with 87 tests (0 failures, 0 errors).
  - `database`: PostgreSQL 18 running on port 5433 (database `interior_design_dev`). Applied `V001__security_identity_tenant_schema.sql`, `V002__pg_rls_policies.sql`, `V003__auth_oidc_transactions.sql`, `V004__designer_onboarding.sql`, and `V005__studio_specialties_and_onboarding_closure.sql`.
- Git repository initialized. `.gitignore` protects credentials and build output.
- **REMOTE PUSH POLICY:** Local commits only. Remote push prohibited unless explicitly requested by the user.

## SELECTED ARCHITECTURE

| Area | Selected baseline |
|---|---|
| Frontend | Next.js 16.3.3, React 19.3.0, TypeScript 6.0.3; App Router/server-first |
| Web tooling | Node 22.18.0 / npm 10.9.3; CSS variables + Tailwind; Vitest 5.0.1; ESLint 9 |
| Backend | Java 25 (Temurin 25.0.4.1 runtime, compiler target Java 21), Spring Boot 3.4.3; modular monolith |
| Java tooling | Maven 3.9.9 checked-in wrapper (`mvnw.cmd`); separate native build |
| Database / migration | PostgreSQL 18.3; Flyway versioned SQL, native `uuidv7()`, durable OIDC state in `auth_oidc_transactions`, studio registry in `designer_studios` & `studio_slug_claims`, normalized specialties in `studio_specialties`, initial onboarding completions in `designer_onboarding_completions` |
| Cache | No initial Redis; PostgreSQL authoritative for sessions, OIDC transactions, quotas and sensitive counters |
| Storage | Private S3-compatible abstraction, initial AWS S3 |
| Public media / CDN | Approved watermarked derivatives only; CloudFront restricted origin/OAC |
| Queue | SQS Standard + transactional outbox + destination dispatch |
| Search | PostgreSQL initial search abstraction; rebuildable published projections |
| API / contracts | REST `/api/v1`; OpenAPI 3.1 contract-first (SpringDoc annotated) |
| Authentication | Managed OIDC abstraction; backend-owned opaque browser sessions; role promotion to DESIGNER on onboarding |

## CURRENT ENVIRONMENT

- Windows workstation; PowerShell; shared local workspace `c:\my projects\interior design`.
- Java 25 installed at `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot` (Temurin 25.0.4.1+1-LTS).
- Node 22.18.0 / npm 10.9.3 active for web execution; Node 24.19.0 LTS qualified.
- PostgreSQL 18.3 active on port 5433; database `interior_design_dev` created and migrated to `v005`.
- Git repository initialized.
- Standing Rule: NEVER PUSH TO REMOTE without explicit user command.

## RECENT VERIFICATION EVIDENCE

- `npm run test` (apps/web): **PASS** (9 test files, 73 tests run, 0 failures)
- `npm run typecheck` (apps/web): **PASS** (`tsc --noEmit` clean exit code 0)
- `npm run lint` (apps/web): **PASS** (`eslint .` clean exit code 0, 0 warnings, 0 errors)
- `npm run build` (apps/web): **PASS** (Next.js 16.3.3 Turbopack build succeeded, `/onboarding/professional` rendered cleanly)
- `mvnw test` (apps/api): **PASS** (87 tests run, 0 failures, 0 errors, Spring Boot 3.4.3 on JDK 25 with H2 and Flyway v005)
- PostgreSQL 18 dev database on port 5433 migrated to `v005` cleanly via `mvnw flyway:migrate`.

## NEXT PHASE

**PHASE 09 — DESIGNER DASHBOARD & WORKSPACE.**
Professional onboarding domain and security invariants closed. Ready to implement the designer dashboard, studio workspace overview, operational metrics shell, and navigation modules upon user approval.
