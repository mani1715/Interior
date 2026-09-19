# AGENT CONTEXT

Last updated: 2026-09-19, Antigravity Phase 10.1 Portfolio Contract Alignment Closure PASS. Next: Phase 11 Basic Portfolio Design (Astra).
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
- [PORTFOLIO_TEMPLATE_CONTRACT.md](PORTFOLIO_TEMPLATE_CONTRACT.md): formal theme engine contract, props, and invariants for Phases 11–16.
- [ASTRA_PHASE_11_HANDOFF.md](ASTRA_PHASE_11_HANDOFF.md): focused Phase 11 execution boundary for Astra.
- [README.md](../README.md), [.gitignore](../.gitignore), [.editorconfig](../.editorconfig): root guidance/conventions.

## PHASE STATUS

- **PHASE 00 — PASS:** Master Product Specification; 145 requirements recorded.
- **PHASE 01 — PASS:** Production architecture selected; eight architecture documents.
- **PHASE 02 — PASS:** Database/domain architecture designed; seven design documents.
- **PHASE 03 — PASS:** Security Foundation + Actual Application Implementation Start. Real monorepo codebase created (`apps/web`, `apps/api`), JDK 25 installed, PostgreSQL 18 migrations applied, security foundation & RLS implemented, cross-tenant isolation tests PASSED, Next.js build PASSED.
- **PHASE 04 — PASS:** Global Design System (Actual UI Implementation — Mobile-First). Complete reusable UI library created in `apps/web/src/components/`, locked palette tokens with WCAG 2.2 AA (5.35:1) compliance, responsive layout primitives, feedback/alerts/dialogs/sheets, media components (BeforeAfterSlider, BeforeAiReality with mandatory AI disclaimer badge, WatermarkPreview), interactive showcase route `/design-system` (marked `noindex`), Vitest test suite (20 tests passed), TypeScript and Next.js Turbopack build PASSED.
- **PHASE 04.1 — PASS:** Design System Canonicalization & Runtime Decisions Locked. Restored exact 12 canonical brand tokens (`#1F1F1F`, `#FAF8F5`, `#E7E1D8`, `#B88A5A`, `#2E5D4B`, `#3E6D8C`, `#C76F4A`, `#E6C9C3`, `#FFFFFF`, `#F4F4F4`, `#D9D9D9`, `#6B6B6B`). Clearly separated brand tokens from accessible semantic derived UI tokens (`--status-warning-text: #9E4522`, etc.). Formally marked Spring Boot 3.4.3 running on JDK 25 with Java 21 compiler bytecode target as canonical backend baseline.
- **PHASE 05 — PASS:** Homepage + 3D Experience (Production Implementation). Production homepage implemented in `apps/web/src/app/page.tsx` replacing the Phase 03 shell. 16 integrated sections, 7-Stage Architectural Transformation Experience (interactive scrubber, accessible screen-reader fallback, layered vector compositing saving >600KB JS over WebGL and eliminating canvas crash risks), Portfolio Platform Section, Public Discovery Preview, AI Concept Visualizer (4-step workflow, prompt demonstration, mandatory legal disclaimer), Before AI Reality narrative, Built for Google Discoverability Section, Project Inspiration Gallery, Professional Practitioner Types, 6-Step Workflow, Lead Generation Engine, Mobile-First On-Site Workflow, Final Call-To-Action, and Production Footer. Strict mobile-first design (360-430px base). WebSite and Organization JSON-LD schemas included. 27 unit tests passing across 6 test files. Next.js Turbopack build and Spring Boot backend test suite all 100% PASS.
- **PHASE 05.1 — PASS:** Homepage Truthfulness & SEO Correction Pass. Audited all visible homepage claims, mock data, badges, and links. Removed all unsupported "verified" assertions. Replaced with truthful regional positioning and professional trade descriptors. Cleaned SEO claims to clarify architecture foundation vs independent search engine ranking. Updated mock Google snippet to neutral SERP preview labeled Example Search Appearance. Refined AI disclaimer to canonical concise architectural wording. Audited all links to point to safe section anchors without broken routes or fake feature pages. Cleaned JSON-LD structured data and social metadata. 27 frontend tests and 8 backend tests PASS.
- **PHASE 06 — PASS:** Public Discovery UI (Project-First Interior Discovery Experience). Built the complete public discovery UI architecture, client islands, centralized demo query boundary, and route hierarchy: `/projects`, `/projects/[projectSlug]`, `/professionals`, `/professionals/[professionalSlug]`, `/categories/[categorySlug]`, and `/locations/[locationSlug]`. Implemented "I Want Something Similar" enquiry modal with project context and honest development preview notice (zero fake backend lead transmission). Strict mobile-first design, single `<h1>` per route, JSON-LD BreadcrumbList and Article schemas, 404 handling via `notFound()`. 57 frontend tests across 7 test files, full TypeScript check, ESLint, Next.js build, and Spring Boot backend tests all 100% PASS.
- **PHASE 07 — PARTIAL:** Authentication & Roles (Production Identity, Session & Authorization Implementation). Full security architecture implemented: backend-owned opaque 256-bit sessions (`identity_sessions`), SHA-256 hashed tokens, secure cookie transport (`__Host-session`), CSRF synchronizer token rotation/verification, in-memory sliding-window bucket rate limiter (`RateLimiterService`), replay-resistant single-use OIDC transaction store (`OidcTransactionStore`), provider-neutral OIDC boundary (`OidcService`), safe account linking preventing silent account takeover on email collision, least-privilege customer baseline role on registration, strictly studio-scoped `DESIGNER_TEAM` authorization, privileged MFA assurance enforcement (`requireMfaAssurance`), dev/test auth persona sandbox (`dev-login` strictly guarded by `@Profile({"dev", "test"})` AND `app.security.dev-auth-enabled=true`), open redirect defense (`sanitizeRedirectUrl`), auth-aware navigation (`PublicHeader`), and mobile-first frontend auth pages (`/sign-in`, `/sign-up`, `/auth/callback`, `/auth/error`, `/account`). Status is truthfully PARTIAL solely because production external OIDC provider configuration remains deployment-time external work.
- **PHASE 07.1 — PASS (SECURITY CLOSURE COMPLETE):** Authentication Security Closure. Closed all 18 security verification gaps: durable database-backed OIDC transaction store (`auth_oidc_transactions` Flyway migration `V003__auth_oidc_transactions.sql`), protocol-level claims validation, PKCE S256 verification, and trusted MFA assurance.
- **PHASE 08 — PASS:** Professional Onboarding Wizard (7-Step Production Engine). Multi-step onboarding wizard (`/onboarding/professional`) with draft persistence, role promotion, studio slug reservation, contact collection, and service taxonomy.
- **PHASE 08.1 — PASS:** Designer Onboarding Closure & Invariant Audit. Normalized relational storage for specialties (`studio_specialties` via `V005__studio_specialties_and_onboarding_closure.sql`).
- **PHASE 08.2 — PASS:** UUID & Tenant-Isolation Canonicalization. Enforced canonical UUIDv7 across all persisted platform entities.
- **PHASE 09 — PASS:** Designer Dashboard & Professional Workspace (Production Implementation). Authenticated workspace shell at `/workspace` and 9 module sub-routes, dual-score completeness engine, owner-scoped GSTIN confidentiality, and responsive mobile bottom navigation.
- **PHASE 09.1 — PASS:** Workspace Truthfulness & Readiness Closure. Module readiness corrected: `PORTFOLIO` = `NOT_CONFIGURED` (prior to Phase 10), `BUSINESS_PROFILE` = `READY`, and remaining future product engines set to `COMING_SOON`. Unpublished profile safeguards active.
- **PHASE 10 — PASS:** Portfolio Builder Engine (Production Implementation). Shared, reusable portfolio builder engine decoupling content from presentation (`CONTENT != TEMPLATE`). 18 canonical section types, optimistic locking, two-pass reordering, immutable version snapshotting & restore, privacy-filtered preview, neutral `ReferenceTemplate` tolerating 0 projects/testimonials, full interactive builder UI (`/workspace/portfolio`), and private preview (`/workspace/portfolio/preview` with `noindex, nofollow`). 118 backend tests, 89 frontend tests PASS.
- **PHASE 11 — BLOCKED (2026-09-19):** Astra conducted intake verification and halted Phase 11 due to engine-owned contract mismatches between frontend and backend (endpoint path, versioning on mutations, field names, section and font enums).
- **PHASE 10.1 — PASS:** Portfolio Contract Alignment Closure. All 7 engine blockers resolved. Aligned initialization endpoint to `POST /portfolio`. Enforced `version` on all mutation request bodies (`UpdateSectionRequest`, `ReorderSectionsRequest`, `CreateVersionSnapshotRequest`, `RestoreVersionRequest`). Aligned reorder payload to `sectionIds`. Canonicalized 18 `SectionType` and 6 `FontPairing` enums. Introduced centralized `normalizePortfolioProps()` in `normalize-props.ts` supplying strongly-typed, non-null `NavigationSettings`. Aligned section content properties with `PortfolioSectionValidator.java`. Versioned template registry entries (`1.0.0`, `SCAFFOLD`, `isSelectable: false`). Added contract test suites `PortfolioContractTest.java` and `PortfolioContract.test.ts`. 125 backend tests, 94 frontend tests PASS.
- **NEXT ACTION:** Resume **PHASE 11 — BASIC PORTFOLIO DESIGN** (Assigned to: **ASTRA**). Contract alignment complete; Astra can implement `BasicTemplate.tsx` cleanly. Phase 12 has not started.

## CURRENT TECHNICAL FOUNDATION

- **Frontend Stack**:
  - Next.js 16.3.3 (App Router, Turbopack, React Server Components where applicable)
  - React 19.3.0
  - TypeScript 6.0.3 (`tsc --noEmit` clean)
  - Styling: Tailwind CSS with 12 locked brand palette variables
  - Icons: `lucide-react`
  - Tests: Vitest 5.0.1, React Testing Library, JSDOM
- **Backend Stack**:
  - Java 25 runtime (Eclipse Adoptium Temurin 25.0.4.1+1-LTS)
  - Java 21 compiler release bytecode target
  - Spring Boot 3.4.3 (Web, Data JPA/JDBC, Validation, Security, Actuator)
  - Build Tool: Maven 3.9.9 (`mvnw.cmd` checked in)
- **Database & Persistence**:
  - PostgreSQL 18.3 active on port 5433 (database: `interior_design_dev`)
  - Flyway versioned SQL migrations: `V001` through `V006` applied cleanly
  - In-memory H2 with Flyway test migrations for fast, isolated backend test execution
- **Architecture Highlights**:
  - Modular monolith with explicit module boundaries
  - RESTful API under `/api/v1`
  - Backend-owned opaque browser sessions (`__Host-session` cookie, SHA-256 tokens)
  - Studio membership tenant isolation (`designer_studios`, `studio_members`, composite keys)
  - Server-side role & capability authorization (`AuthorizationService`)
  - Canonical RFC 9562 UUIDv7 identifiers (`UuidV7`)
  - Workspace routes protected with `Cache-Control: private, no-store, max-age=0, must-revalidate`
  - Strict mobile-first design (360px-430px base).

## PHASE 10 PORTFOLIO ARCHITECTURE

- **Core Architectural Invariant**: `CONTENT != TEMPLATE`
  - The portfolio engine strictly separates domain content and section structure from presentation themes.
  - Professionals can switch between themes without destroying, mutating, or losing section data, text, or ordering.
- **Database Entities (`V006__portfolio_engine.sql`)**:
  - `portfolios`: Exactly one primary portfolio record per studio (`studio_id REFERENCES designer_studios(id) ON DELETE CASCADE`). Tracks `template_key`, aggregate `version` (for optimistic concurrency), editorial text (`headline`, `subheadline`, `bio`, `design_philosophy`, `years_of_experience`), design tokens (`primary_color`, `secondary_color`, `accent_color`, `font_pairing`), and lifecycle status (`DRAFT`, `READY`, `UNPUBLISHED`).
  - `portfolio_sections`: Normalized modular sections with composite tenant foreign key `(portfolio_id, studio_id)` ensuring strict cross-tenant isolation. Enforces non-negative `display_order >= 0`.
  - `portfolio_versions`: Immutable JSONB snapshots for historical milestone preservation (retains up to 10 snapshots per studio, automatically pruned on creation).
- **Lifecycle & Publishing Rules**:
  - Phase 10 operates exclusively on `DRAFT`, `READY`, and `UNPUBLISHED`.
  - **Live public publishing is NOT implemented**: No `POST /publish`, no client-controlled `PUBLISHED` state, no public search engine indexing.
  - Studio publication status and portfolio readiness are distinct: Portfolio readiness contributes 25% to platform launch readiness score when required sections are configured and visible.
- **18 Canonical Section Types (PostgreSQL V006 & SectionType.java)**:
  `HERO`, `ABOUT`, `SERVICES`, `FEATURED_PROJECTS`, `PROJECT_GRID`, `BEFORE_AFTER`, `BEFORE_AI_REALITY`, `DESIGN_PROCESS`, `TESTIMONIALS`, `TEAM`, `AWARDS`, `PRESS`, `SERVICE_AREAS`, `FAQ`, `CONTACT`, `CTA`, `VIDEO`, `CUSTOM_NOTE`.
- **Validation & Security**:
  - `PortfolioSectionValidator` bounds section JSON payloads to 32 KB and snapshots to 64 KB.
  - Rejects HTML tags (`<script`, `<iframe`, `<object`, `<embed`, etc.) and JavaScript event handlers (`\bon[a-z]{3,20}\s*=`).
  - Strict optimistic locking: Aggregate version mismatches return HTTP 409 Conflict.
- **Privacy & Previews**:
  - `GET /api/v1/portfolio/preview` strictly exposes only studio contacts where `public_consent == true`. Private phone numbers and emails are completely omitted from preview output.
  - Private preview route `/workspace/portfolio/preview` includes `<meta name="robots" content="noindex, nofollow" />`.
- **Known Upstream Phase Boundaries**:
  - No Project CMS yet (Phase 18): Sections tolerate 0 projects with clean editorial empty states.
  - No Media Engine yet (Phase 19): Logo/hero image references remain null or future references; no fake file upload endpoints.
  - No AI Visualizer generation yet (Phase 24).
  - No public portfolio routing yet.

## TEMPLATE ENGINE & THEME DEFINITIONS

- **Six Canonical Template Keys**:
  1. `BASIC`: Clean Editorial (Phase 11 — Astra)
  2. `MODERN`: Modern Minimalist (Phase 12 — Astra)
  3. `LUXURY`: Luxury Atelier (Phase 13 — Astra)
  4. `ARCHITECTURAL`: Architectural Monograph (Phase 14 — Astra)
  5. `WARM_NATURAL`: Warm & Natural Biophilic (Phase 15 — Astra)
  6. `DARK_CINEMATIC`: Dark Cinematic Moodboard (Phase 16 — Astra)
- **Current Implementation Status**:
  - All six template keys are registered in `apps/web/src/lib/portfolio/template-registry.tsx` with status **`SCAFFOLD`**, version **`1.0.0`**, and `isSelectable: false`.
  - No finished production template exists yet.
  - `ReferenceTemplate` (`apps/web/src/components/portfolio/templates/ReferenceTemplate.tsx`) is an **ENGINE TEST / STRUCTURAL REFERENCE ONLY**. It is **NOT** the final Basic design and must not be used as a production portfolio.
  - Astra will design and implement the real visual templates in Phases 11–16.

## ASTRA OWNERSHIP BOUNDARIES (PHASES 11–16)

### Allowed / Astra-Owned Files:
- `apps/web/src/components/portfolio/templates/basic/**` (Phase 11)
- `apps/web/src/components/portfolio/templates/modern/**` (Phase 12)
- `apps/web/src/components/portfolio/templates/luxury/**` (Phase 13)
- `apps/web/src/components/portfolio/templates/architectural/**` (Phase 14)
- `apps/web/src/components/portfolio/templates/warm-natural/**` (Phase 15)
- `apps/web/src/components/portfolio/templates/dark-cinematic/**` (Phase 16)
- Template-specific styling, visual components, typography tokens, decorative SVG assets, and template unit tests (e.g. `apps/web/src/components/__tests__/BasicTemplate.test.tsx`).
- Graduating a template in `apps/web/src/lib/portfolio/template-registry.tsx` from `'SCAFFOLD'` to `'AVAILABLE'` upon completing its phase.

### Strictly Forbidden / Engine-Owned:
- Backend Java code (`apps/api/**`)
- Database migrations (`V001` through `V006`)
- Authentication, session, and cookie logic (`apps/api/.../security/**`, `apps/web/src/lib/auth/**`)
- CSRF protection and rate limiting
- Authorization policies and tenant scoping (`AuthorizationService`)
- Portfolio API endpoints and contracts (`PortfolioController`, `portfolio/api.ts`)
- Section schema definitions and validation logic (`PortfolioSectionValidator`)
- Optimistic concurrency and version snapshot mechanisms
- The `PortfolioTemplateProps` contract interface (`apps/web/src/lib/portfolio/template-contract.ts`)
- Global workspace shell and navigation (`apps/web/src/components/workspace/**`, `apps/web/src/app/workspace/page.tsx`).

## CURRENT TEST BASELINE (PHASE 10.1 PASS)

- **Backend (Maven)**: **125 / 125 PASSED** across 15 test classes (0 failures, 0 errors), including `PortfolioContractTest`.
- **Frontend (Vitest)**: **94 / 94 PASSED** across 12 test files (0 failures), including `PortfolioContract.test.ts`.
- **Typecheck**: **PASS** (`npm run typecheck` / `tsc --noEmit` exit code 0)
- **Lint**: **PASS** (`npm run lint` / `eslint .` exit code 0)
- **Production Build**: **PASS** (`npm run build` / Next.js Turbopack build clean)
- **Terminology Audit**: **PASS** (0 occurrences of wedding, photoshoot, photographer, bride, groom)
- **Git State**: Local commit `phase-10.1: align portfolio frontend backend contracts` pushed to `https://github.com/mani1715/Interior.git`, with `local HEAD == origin/main`.

## REPOSITORY INTAKE QUALIFICATION & PHASE 10.1 RESOLUTION

During repository intake on 2026-09-19, Astra correctly halted Phase 11 and documented contract discrepancies between frontend and backend in Section 18 of `ASTRA_PHASE_11_HANDOFF.md` (initialization routes, mutation versions, reorder field names, font and section enums, section content fields, missing navigation settings).

Antigravity executed Phase 10.1 to resolve all engine-owned contract blockers without breaking architecture or database migrations:
1. Endpoint aligned: `POST /portfolio` across frontend and backend.
2. Optimistic concurrency aligned: `version: number` on all mutation payloads; `sectionIds` on reordering.
3. 18 canonical `SectionType` and 6 `FontPairing` enums synchronized between backend, DB V006, and frontend.
4. Centralized presentation normalizer `normalizePortfolioProps()` implemented, providing strongly-typed, non-null `NavigationSettings`.
5. Section content names aligned with `PortfolioSectionValidator.java`.
6. Template registry entries updated to `version: '1.0.0'`, `status: 'SCAFFOLD'`, `isSelectable: false`.
7. Full regression passing: 125 backend tests and 94 frontend tests.

Astra can now proceed directly with **PHASE 11 — BASIC PORTFOLIO DESIGN**.

