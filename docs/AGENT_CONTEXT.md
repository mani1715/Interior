# AGENT CONTEXT

Last updated: 2026-09-21, Phase 21 — AI Visualizer Foundation COMPLETE & PASS. Verified, tested, and production-ready. Next: Phase 22 (Reference Image System) pending instruction.
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
- **PHASE 11 — PASS:** BASIC 1.0.0 is AVAILABLE and selectable. Pure props-based editorial renderer, scoped CSS, responsive container queries, mobile navigation, semantic section rendering and privacy-filtered contact actions.
- **PHASE 12 — PASS:** MODERN 1.0.0 is AVAILABLE and selectable. Distinct contemporary renderer, offset grid, high-contrast typography, and accessible navigation.
- **PHASE 13 — PASS:** LUXURY 1.0.0 is AVAILABLE and selectable. Tailored restrained editorial renderer, generous serif hierarchy, fine rules, and quiet luxury aesthetic.
- **PHASE 14 — PASS:** ARCHITECTURAL 1.0.0 is AVAILABLE and selectable. Structured monograph renderer, drafting-grid alignment rails, project-oriented indexes, and geometric discipline.
- **PHASE 15 — PASS:** WARM_NATURAL 1.0.0 is AVAILABLE and selectable. Warm, tactile, residential renderer, soft ivory surfaces, restrained sage/terracotta accents, and biophilic focus.
- **PHASE 16 — PASS:** DARK_CINEMATIC 1.0.0 is AVAILABLE and selectable. Deep layered charcoal surfaces, warm ivory type, restrained bronze accents, and dramatic cinematic aesthetic.
- **PHASE 17 — PASS:** Portfolio Integration, Hardening & QA. Full six-template system verified: canonical registry (all 6 AVAILABLE, selectable, 1.0.0), builder selector, cross-template switching with 100% content preservation, optimistic locking, pure-props contract, non-null navigation derivation via `normalizePortfolioProps()`, shared `PortfolioMotion.tsx` island (IntersectionObserver, cleanup, reduced-motion, SSR visible), contrast-safe palettes, empty state handling, terminology audit clean ("project photography" replaced with "project visuals"), no migration required, 155/155 frontend tests (19 files) passed, 125/125 backend tests passed, Next.js production build clean.
- **PHASE 18 — PASS:** Professional Project CMS (Production Implementation). Production system for managing interior design project stories. PostgreSQL Flyway migration `V007__project_cms.sql` with `studio_projects` (UUIDv7, tenant isolation, RLS) and `project_styles`. Server-derived readiness (`READY` vs `DRAFT`), optimistic locking (`version`, HTTP 409), client privacy safeguards (confidential client name, budget privacy), canonical categories (15) and styles (8). Integration with Portfolio Engine (`ProjectPresentationDto` for `FEATURED_PROJECTS` and `PROJECT_GRID` sections). Workspace UI at `/workspace/projects` (KPIs, filtering, search, quick reorder, archiving) and `/workspace/projects/[projectId]` (full story editor, classification, location, privacy controls, readiness checklist). Module readiness updated to `READY`. Zero media upload (strictly truthful note for Phase 19). 136/136 backend tests PASS, 165/165 frontend tests (20 files) PASS, TypeScript clean, ESLint clean, production build clean.
- **PHASE 19 — PASS:** Media Engine + Automatic Watermarks (Production Implementation). Complete media lifecycle with direct upload pipeline and pre-authenticated upload intents (`pending/{studioId}/{intentId}/{mediaAssetId}`), quarantine validation (25MB max, MIME checks), and promotion to canonical private originals (`studio/{studioId}/projects/{projectId}/original/{mediaAssetId}`). Core Architectural Invariant: ORIGINAL MEDIA != PUBLIC MEDIA. Original masters are clean, private, never exposed publicly. Public responsive derivatives (`THUMBNAIL` 400px, `MEDIUM` 1200px, `LARGE` 1920px) generated in WebP with all EXIF metadata stripped. Automatic studio watermarking with configurable position (5 options), opacity (0.10–1.00), and fallback text. Mandatory permanent AI Concept disclosure badge (`✦ AI Concept Visualization`) automatically embedded on all public AI_CONCEPT derivatives for truth in advertising. Strict privacy invariant: CLIENT_PRIVATE and REFERENCE media remain strictly private (never watermarked, never generated into public derivatives, never served via public endpoints). Full ProjectMediaManager component integrated into Project Story Editor (`/workspace/projects/[projectId]`) with upload, reorder, cover selection, and metadata editing. Upgraded Studio Media Library at `/workspace/media` from placeholder to READY with asset metrics, project/type/visibility filters, and watermark settings modal. WorkspaceService media module updated to READY. Integrated real project cover image URLs into ProjectPresentationDto and Portfolio Engine. 147/147 backend tests PASS, 173/173 frontend tests (21 files) PASS, TypeScript clean, ESLint clean, Next.js build clean.
- **NEXT ACTION:** STOP AFTER PHASE 19. Phase 20 (Before / After Engine) is the next milestone. Do not implement without explicit authorization.

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
  - BASIC, MODERN, LUXURY, ARCHITECTURAL, WARM_NATURAL and DARK_CINEMATIC are all **AVAILABLE**, version **1.0.0**, `isSelectable: true`, using their own template directories.
  - See the 2026-09-21 visual excellence entry below for the latest verification and remaining limitations.
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

## CURRENT TEST BASELINE (PHASE 17 PASS)

- **Backend (Maven)**: **125 / 125 PASSED** across 15 test classes (0 failures, 0 errors).
- **Frontend (Vitest)**: **155 / 155 PASSED** across 19 test files (0 failures).
- **Typecheck**: **PASS** (`npm run typecheck` / `tsc --noEmit` exit code 0).
- **Lint**: **PASS** (`npm run lint` / `eslint .` exit code 0).
- **Production Build**: **PASS** (`npm run build` / Next.js Turbopack build clean).
- **Terminology Audit**: **PASS** (0 occurrences of wedding, photographer, photography, bride, groom in template code; "project photography" replaced with "project visuals").
- **Git State**: Local commit `phase-11-17: complete portfolio templates and integration qa` pushed to `https://github.com/mani1715/Interior.git`, with `local HEAD == origin/main`.
- **Database**: NO MIGRATION REQUIRED.


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

Astra proceeded with Phase 11 after this closure. The following section is the current handoff.

## PHASE 11 — BASIC COMPLETION (2026-09-19)

- **Implementation:** `apps/web/src/components/portfolio/templates/basic/` owns `BasicTemplate.tsx`, `BasicHeader.tsx`, `BasicSections.tsx`, `presentation.ts`, and `BasicTemplate.module.css`. BASIC remains version **1.0.0**, **AVAILABLE**, and selectable. Other five themes remain unchanged. The only builder integration is semantic theme buttons honoring the existing `isSelectable` flag; no save, concurrency, security, API, schema, or backend changes.
- **Content behavior:** Canonical sections render in engine order when usable; unknown/empty content disappears without blank sections or broken navigation. Project IDs cannot yet resolve to project/media DTOs, so project sections are omitted until the engine supplies that contract. No fabricated content or stock images. The current AI comparison contract supplies concept/result images, not a before image; these are visibly labeled `AI Concept Visualization` and `Real Result`. Video is an explicit link, not an invented embed pipeline. Contacts use only the supplied public contacts.
- **Responsive validation:** Browser inspection of the actual BASIC renderer in an isolated, clearly labeled QA fixture at **360, 390, 430, 768, 1024 and 1440px**; no horizontal overflow. Long name/headline/story, many services, empty content and a 375px embedded frame also checked. Production global styles were included. This was component visual QA, not a live authenticated backend session; private preview integration remains covered by the existing mocked preview test. Temporary QA server/files removed before commit.
- **Accessibility/performance:** One meaningful h1, semantic landmarks and logical headings, skip link, visible focus, native FAQ disclosure, mobile menu Enter/Escape/focus return, and >=44px visible controls checked. Contrast-safe color fallbacks have automated coverage. Reduced-motion CSS included. Server rendering tested; only navigation is a client island. No dependencies or remote fonts added. This is an accessibility review, not independent WCAG certification.
- **Validation:** **110 frontend tests across 13 files**, including **16 BASIC tests**; **125 backend tests**, all passing. Typecheck, lint, production build and diff whitespace check pass. Existing registry/switch tests updated for BASIC availability without removing coverage. Backend regression uses the existing H2 test environment.
- **Audit:** BASIC rendering has no API/auth/storage imports, mutations, raw HTML injection, tenant logic, or unrelated-domain terminology. Synthetic data is limited to the explicitly labeled test fixture.
- **Git handoff:** Implementation commit is the commit containing this section, titled `phase-11: implement basic portfolio design` on `main`. The exact resulting SHA and normal push verification are reported in the task completion response; no self-referential commit hash is embedded here.
- **Next:** Phase 12 MODERN via Astra. Do not begin automatically. Use the normalized props and existing canonical enums; BASIC styles and presentation helpers are BASIC-owned, not a mandatory visual system for MODERN.

## PHASE 12 — MODERN COMPLETION (2026-09-19)

- **Implementation:** `apps/web/src/components/portfolio/templates/modern/` owns `ModernTemplate.tsx`, `ModernHeader.tsx`, `ModernSections.tsx`, `modern.ts`, and `ModernTemplate.module.css`. MODERN is **AVAILABLE**, selectable, and remains version **1.0.0**. Its visual system uses offset editorial hierarchy, an index-style header, numbered service rows, a vertical process rhythm, high-contrast CTA bands, and a distinct footer. BASIC visual files were not changed.
- **Validation:** Frontend **121 tests across 14 files** passed, including 11 MODERN tests. Typecheck, lint, and production build passed. Backend source was unchanged; the existing Phase 10.1 regression baseline is 125 passed. A browser visual fixture was prepared but localhost approval was blocked by the Codex usage limit, so six-width visual QA is not claimed.
- **Boundary:** No backend, database, auth, portfolio contract, API, tenant, or BASIC visual changes. No new dependency. MODERN is props-driven, SSR-renderable, uses native disclosure/menu interactions, safe URL filtering, explicit AI labels, and no fake media/data.
- **Git policy:** Phase 11 changes remain uncommitted in the working tree. No Phase 12 commit or GitHub push was made, as instructed. Final commit organization and push remain deferred until Phase 16.
- **Next:** Phase 13 LUXURY via Astra. Do not begin automatically.

## PHASE 13 — LUXURY COMPLETION (2026-09-19)

- **Implementation:** `apps/web/src/components/portfolio/templates/luxury/` owns `LuxuryTemplate.tsx`, `LuxuryHeader.tsx`, `LuxurySections.tsx`, `luxury.ts`, and `LuxuryTemplate.module.css`. LUXURY is **AVAILABLE**, selectable, and remains version **1.0.0**. Its visual system uses refined serif hierarchy, generous editorial proportions, fine rules, quiet metadata, numbered service/process lists, and an understated invitation CTA. BASIC and MODERN visual files were not changed.
- **Validation:** Frontend **130 tests across 15 files** passed, including 9 LUXURY tests. Typecheck, lint, and production build passed. Backend source was unchanged; the existing Phase 10.1 regression baseline is 125 passed. Manual browser visual QA at 360–1440px is pending because localhost approval was blocked by the Codex usage limit.
- **Boundary:** No backend, database, auth, portfolio contract, API, tenant, or BASIC/MODERN visual changes. No new dependency. LUXURY is props-driven, SSR-renderable, uses native FAQ/menu interactions, safe URL filtering, explicit AI labels, and no fake media/data.
- **Git policy:** Phase 11–13 changes remain uncommitted in the working tree. No GitHub push was made, as instructed. Final commit organization and push remain deferred until Phase 16.
- **Next:** Phase 14 ARCHITECTURAL via Astra. Do not begin automatically.

## PHASE 14 — ARCHITECTURAL COMPLETION (2026-09-19)

- Canonical six verified across backend enum, frontend registry, and contract: BASIC, MODERN, LUXURY, ARCHITECTURAL, WARM_NATURAL, DARK_CINEMATIC. No ORGANIC/MINIMAL drift was present.
- ARCHITECTURAL 1.0.0 graduated to AVAILABLE/selectable with an independent structured monograph renderer under `apps/web/src/components/portfolio/templates/architectural/`.
- Visual system: drafting-grid alignment rails, numbered section index, restrained technical palette, project-documentation rhythm, structured metadata, and editorial geometry.
- Pure props renderer; no backend, database, auth, contract, API, tenant, or earlier-template visual changes.
- Validation: frontend 137/137, typecheck PASS, lint PASS, production build PASS. Backend source unchanged; existing 125-test baseline retained because rerun was blocked by approval usage limits. Manual browser QA remains pending for the same reason.
- Git policy: preserve Phase 11–14 changes locally; no commit or push. Final push remains deferred until all six templates are complete.
- Next: Phase 15 WARM_NATURAL only; do not start automatically.

## PHASE 15 — WARM NATURAL COMPLETION (2026-09-19)

- WARM_NATURAL 1.0.0 graduated to AVAILABLE/selectable with an independent warm, tactile, residential renderer under `apps/web/src/components/portfolio/templates/warm-natural/`.
- Visual system: warm ivory surfaces, restrained sage/terracotta accents, controlled radius, comfortable widths, human-friendly process, testimonials, contact actions, and calm footer treatment.
- Pure props renderer; no backend, database, auth, contract, API, tenant, or earlier-template visual changes.
- Validation: frontend 144/144, typecheck PASS, lint PASS, production build PASS, diff check PASS. Security/terminology scan clean. Backend source unchanged; existing 125-test baseline retained because rerun was blocked by approval usage limits. Manual browser QA remains pending for the same reason.
- Git policy: preserve Phase 11–15 changes locally; no commit or push. Final push remains deferred until all six templates are complete.
- Next: Phase 16 DARK_CINEMATIC only; do not start automatically.

## PHASE 16 — DARK CINEMATIC COMPLETION (2026-09-19)

- DARK_CINEMATIC 1.0.0 graduated to AVAILABLE/selectable, completing the initial six-template registry set.
- Independent visual system: deep layered charcoal surfaces, warm ivory type, restrained bronze accent, cinematic hero treatment without media dependency, high-contrast editorial sections, and lightweight CSS-only motion readiness.
- Pure props renderer; no backend, database, auth, contract, API, tenant, or earlier-template visual changes.
- Validation: frontend 150/150, typecheck PASS, lint PASS, production build PASS, diff check PASS. Security/terminology scan clean. Backend source unchanged; fresh rerun pending because approval limits blocked it; last verified baseline remains 125/125. Manual browser QA remains pending for the same reason.
- All six canonical keys are AVAILABLE, selectable, and version 1.0.0. No ORGANIC, MINIMAL, or alternate keys.
- Git policy: preserve Phase 11–16 changes locally; no commit or push. Final six-template QA and Git finalization are next.
- Phase 17 has not started.

## FINAL SIX-TEMPLATE QA (2026-09-19)

- Verified exact canonical keys: BASIC, MODERN, LUXURY, ARCHITECTURAL, WARM_NATURAL, DARK_CINEMATIC. All six are AVAILABLE, selectable, and version 1.0.0.
- Cross-template QA confirms distinct visual systems: clean editorial, expressive modern, refined luxury, structured architectural, warm tactile natural, and immersive dark cinematic.
- Pure props/security boundary preserved across all template folders. No API, auth, CSRF, tenant, persistence, or unsafe HTML access. Style isolation remains template-specific.
- Fresh frontend validation: 150/150 tests across 18 files, typecheck PASS, lint PASS, production build PASS. Backend source unchanged; fresh rerun blocked by `AccessDeniedException: C:\.m2`, last verified baseline 125/125. Manual browser QA pending due approval limits.
- Accessibility coverage includes semantic landmarks, single h1, keyboard menus with Escape focus restoration, skip links, native FAQ disclosure, 44px controls, contrast-aware dark theme, and reduced-motion handling.
- Performance: no new dependencies, SSR-compatible renderers, CSS-only presentation, no WebGL/carousels/autoplay video.
- Created `docs/ANTIGRAVITY_PHASE_17_HANDOFF.md`. Phase 17 belongs to Antigravity integration/QA and has not started.
- Git finalization remains pending because commit/push approval is unavailable; preserve all Phase 11–16 changes locally.

## VISUAL EXCELLENCE PASS (2026-09-21)

- All six canonical templates remain AVAILABLE, selectable, version 1.0.0. Backend, database, auth, API, tenant logic, PortfolioTemplateProps and section contracts were not changed.
- Presentation-only `PortfolioMotion` island: one IntersectionObserver per mounted portfolio, one-shot section/photo entrances, cleanup and live reduced-motion preference handling. Content is visible in SSR HTML and remains visible without JS, matchMedia or IntersectionObserver. No scroll listeners, parallax loop, WebGL, animation dependency or new package.
- Responsive motion uses container widths (including embedded previews): mobile <768 uses short 12px movement; tablet 768–1199 uses restrained theme-specific reveals; desktop >=1200 adds selective photographic perspective/scale. Reduced-motion CSS disables animations and transitions, scoped inside each template. Native scrolling is preserved. Section text remains fully opaque during entrance.
- Distinct photo treatments: BASIC lift; MODERN offset crop; LUXURY slow vertical mask/zoom; ARCHITECTURAL aligned horizontal clip; WARM_NATURAL gentle scale/elevation; DARK_CINEMATIC restrained 3-degree perspective. Hover effects require a fine pointer and wide container. Effects attach only to actual supplied media.
- Real defects corrected: missing brand-link anchors in five templates; label-only comparisons in Architectural/Warm Natural/Dark Cinematic now render supplied images; global heading color/font interference on dark surfaces; global reduced-motion leakage; insufficiently safe custom text colors; missing hero CTA fallbacks in the later three templates. Removed template version labels from visitor footers and invented fallback practice descriptions. Warm Natural no longer wraps every section in a rounded card.
- Shared visual-palette helper retains valid brand surfaces/accent and derives heading/link/button text with >=4.5:1 contrast. Canonical font pairings are respected, no remote fonts added. Scope remains presentation-only; this is not independent WCAG certification.
- Live browser QA used actual renderer components, production global CSS and explicitly synthetic test data (no portfolio stock images). All six tested at 360, 390, 430, 768, 1024 and 1440px. Final 36-case long-content matrix had zero horizontal overflow and zero missing anchors. Screenshot review covered all six mobile pages, desktop heroes and tablet contact/footer; no claim of physical low-power device profiling. All six mobile menus opened/closed, restored focus on Escape, and measured >=44px.
- Automated verification: 155/155 frontend tests across 19 files with two workers, typecheck PASS, lint PASS, production build PASS. Initial heavily concurrent frontend run had a timeout; rerun passed without altering timeout/assertions. Fresh backend regression PASS: 125/125 (approved Maven cache access), superseding prior blocked-baseline reports.
- Five new tests cover SSR visibility, missing browser API fallback, reduced-motion opt-out/change/cleanup, custom palette contrast and supplied comparison media. No fake projects added: Project CMS/media DTO integration remains outside the current props contract.
- Git: all legitimate Phase 11–16 work preserved locally; no commit or push during this pass. Temporary preview/build-helper files removed after QA. Next = PHASE 17 — ANTIGRAVITY INTEGRATION & QA; do not start Phase 17 from Astra.

## PHASE 18 — PROFESSIONAL PROJECT CMS (2026-09-21)

- Production system for managing interior design project stories feeding discovery, portfolio, and media engines.
- Database & Migrations: Flyway `V007__project_cms.sql` with `studio_projects` (UUIDv7, tenant isolation, RLS) and `project_styles`.
- Server-derived readiness: projects evaluate mandatory attributes for `READY` state.
- Optimistic locking: version tracking prevents concurrent write conflicts (HTTP 409).
- Privacy protection: confidential client names and budget ranges strictly guarded.
- Verification: 136/136 backend tests PASS, 165/165 frontend tests PASS.

## PHASE 19 — MEDIA ENGINE & AUTOMATIC WATERMARKS (2026-09-21)

- Built complete production media engine in `apps/api` (`com.interior.platform.media`) and `apps/web` (`src/components/media/`, `src/lib/media/`, `/workspace/media`).
- Database & Migrations: Flyway `V008__media_engine.sql` with PostgreSQL Row Level Security (RLS) for `studio_watermark_settings`, `upload_intents`, `media_assets`, and `media_derivatives`. Corresponding H2 test migration in `test-migration/V008__media_engine.sql`.
- Direct Upload Flow: Client initiates upload via `POST /media/upload-intent` with MIME, size, project, and type validation; streams bytes via `PUT /media/upload/{id}` into quarantined storage; calls `POST /media/commit` to trigger server-side verification, image dimension extraction, and derivative generation.
- Derivative & Watermark Pipeline: `ImageProcessingService` strips EXIF metadata, resizes to responsive variants (`THUMBNAIL` 400px, `MEDIUM` 1200px, `LARGE` 1920px), compresses to modern WebP format, applies subtle studio watermark, and embeds mandatory permanent `✦ AI Concept Visualization` badge on all public variants of `AI_CONCEPT` media.
- Strict Privacy Invariants: `CLIENT_PRIVATE` and `REFERENCE` media are strictly private to the studio team. No public derivatives are generated for private assets, and their original masters cannot be accessed via the public endpoint `/api/v1/media/public/**`.
- Project Story Integration: Replaced Phase 18 placeholder banner in `/workspace/projects/[projectId]` with interactive `ProjectMediaManager` (real-time progress upload, drag/order reordering, cover photo selection, metadata editing, and deletion with derivative purge).
- Studio Media Library: Upgraded `/workspace/media` from placeholder to `READY` status in `WorkspaceService`. Provides multi-metric asset analytics, project/type/visibility filtering, detail modal inspection of public derivatives, and interactive Watermark Settings configuration.
- Real Cover Media in Portfolios: `ProjectService` dynamically resolves the project's cover image or lowest-sort-order image, injecting it into `ProjectPresentationDto.coverImageUrl` for portfolio showcases.
- Locked UI Palette Compliance: All platform UI strictly respects the platform theme (`#FAF8F5` background, `#1F1F1F` text, `#B88A5A` accents, `#FFFFFF` cards with `#E7E1D8` borders).
- Verification: 147/147 backend tests PASS, 173/173 frontend tests PASS, TypeScript clean, ESLint clean, Next.js production build clean.

## PHASE 20 — PRODUCTION SEO ENGINE (2026-09-21)

- Built complete production SEO engine spanning `apps/api` (`com.interior.platform.seo`) and `apps/web` (`src/lib/seo/`, `src/app/robots.ts`, `src/app/sitemap.ts`, `/workspace/seo`, `/professionals/[professionalSlug]`, `/projects/[projectSlug]`, `/categories/[categorySlug]`, `/locations/[locationSlug]`).
- Publication Gate & Truthful Indexing:
  - Private by default: newly onboarded studios start `UNPUBLISHED` (draft).
  - Explicit publication gate: `/seo/publish` validates strict prerequisites (profile completeness, ready portfolio, consented public contact, published project, cover photography).
  - Instant unpublish: `/seo/unpublish` immediately revokes public access and search indexing (returns HTTP 404, never 200 with "draft" notices).
  - Private routes (`/workspace/**`, `/account/**`, `/auth/**`, preview routes) strictly disallowed in `robots.ts` and set to `noindex, nofollow`.
- Truthful Metadata & Structured Data:
  - Meta titles & descriptions built truthfully without false ranking claims, fake review stars, or doorway spam.
  - Safe JSON-LD serialization: `SafeJsonLd` escapes `<`, `>`, and `&` to eliminate script breakout XSS vulnerabilities.
  - Generates Schema.org `ProfessionalService` / `LocalBusiness`, `CreativeWork`, and `BreadcrumbList` schemas.
- High-Performance XML Sitemap & Image SEO:
  - Automated dynamic Next.js `sitemap.ts` (`/sitemap.xml`) combining core platform routes, canonical categories, canonical locations, and live published studios and projects.
  - Image SEO strictly delivers responsive WebP derivatives with explicit dimensions (preventing CLS) and permanent `✦ AI Concept Visualization` badge preservation for AI imagery.
- SEO Center Workspace (`/workspace/seo`):
  - Upgraded from `COMING_SOON` to `READY` status in `WorkspaceService`.
  - Real-time publication status badge, live Publish / Unpublish actions.
  - Automated 6-point SEO readiness checklist diagnostics with actionable remediation links.
  - Live Google SERP appearance preview (desktop & mobile simulation).
  - Live Social Share preview card (OpenGraph / Twitter card).
  - Custom meta title and description overrides with character counters and HTML stripping.
- Locked UI Palette Compliance:
  - Fully compliant with platform rules (`#FAF8F5` cream canvas, `#1F1F1F` charcoal typography, `#B88A5A` warm bronze accents, `#FFFFFF` cards, `#E7E1D8` borders).
- Verification:
  - Backend: **158 / 158 PASS** (11 new SEO unit and integration tests added, 0 failures, 0 errors).
  - Frontend: **184 / 184 PASS** (11 new SEO engine tests added, 0 failures, 0 errors).
  - Typecheck: **PASS** (`tsc --noEmit` clean).
  - Lint: **PASS** (`eslint .` clean).
  - Production build: **PASS** (Next.js 16 Turbopack optimized production build clean).
  - Working tree: clean. local HEAD == origin/main.

## PHASE 21 — AI VISUALIZER FOUNDATION (2026-09-21)

- Built complete production AI visualizer foundation spanning `apps/api` (`com.interior.platform.ai`) and `apps/web` (`src/lib/ai/`, `src/components/ai/`, `/workspace/ai`).
- Durable Asynchronous AI Job Pipeline:
  - Database schema Flyway `V010__ai_visualizer_foundation.sql` for PostgreSQL 18 with RLS forced for tenant isolation, UUIDv7 primary keys, and H2 test parity in `test-migration/V010__ai_visualizer_foundation.sql`.
  - Tables `ai_visualization_jobs` and `ai_usage_events` with strict foreign key cascading to `designer_studios`, `studio_projects`, and `media_assets`.
  - Durable job lifecycle state machine: `QUEUED` -> `PROCESSING` -> `SUCCEEDED` / `FAILED` / `CANCELLED`.
  - Idempotency deduplication: `uq_ai_jobs_studio_idempotency` unique constraint ensures duplicate submissions with the same key return the existing job.
  - Quota and rate limiting: Daily studio limit tracking (`countTodayUsage`) and burst rate protection (`RateLimiterService`).
- Provider Abstraction & Truthful Execution:
  - `AiImageProvider` interface with `isConfigured()`, `submitGeneration()`, `checkStatus()`, and `cancel()`.
  - `DisabledAiImageProvider` and `ConfigurableAiImageProvider`: fail-fast with HTTP 503 `AiProviderNotConfiguredException` when unconfigured.
  - Truthful AI behavior: zero fake AI, zero fake progress percentages, no fake placeholder images.
- Media Engine Ingestion & Derivative Watermarking:
  - Generated images validated (`validateAndGetDimensions`), stored into canonical storage keys via `StorageService`.
  - Registered as `MediaType.AI_CONCEPT` with `MediaVisibility.PUBLIC`.
  - Public derivatives generated with permanent, non-removable `✦ AI Concept Visualization` badge burned into the image pixels alongside studio watermark.
  - Never allowed to process `CLIENT_PRIVATE` confidential media assets.
- Interactive Workspace UI (`/workspace/ai`):
  - Upgraded from `COMING_SOON` to `READY` (or `NOT_CONFIGURED` if provider disabled) in `WorkspaceService`.
  - Locked UI palette compliance: `#FAF8F5` background, `#1F1F1F` text, `#FFFFFF` cards, `#E7E1D8` borders, `#B88A5A` warm bronze accents.
  - Unconfigured banner explains required backend environment variables.
  - Project and unfinished room photo selector with visual previews.
  - Natural language prompt input with 500-char counter and 5 architectural design presets.
  - Real-time generation state with cancel action.
  - Side-by-side / split comparison viewer (Original vs AI Concept) with permanent legal disclaimer:
    "✦ AI Concept Visualization — final colors, materials, proportions, and execution may differ."
  - Bounded recent generation history with quick-load into comparison workspace.
  - Deep-link support: `?projectId=...&mediaId=...` preselects project and media directly from project editor ("Visualize with AI").
- Project Editor Integration:
  - Added "Visualize with AI" link button on eligible media items in `ProjectMediaManager.tsx`.
- Verification Baseline:
  - Backend: **171 / 171 PASS** (13 new AI visualizer unit and integration tests added, 0 failures, 0 errors).
  - Frontend: **189 / 189 PASS** (5 new AI visualizer frontend tests added, 0 failures, 0 errors).
  - Typecheck: **PASS** (`tsc --noEmit` clean).
  - Lint: **PASS** (`eslint .` clean).
  - Production build: **PASS** (Next.js 16 Turbopack optimized production build clean).

## PHASE 21.1 — AI CONCEPT PRIVACY & PUBLICATION HARDENING (2026-09-21)

- Canonical Rule Enforced: "AI CONCEPTS MUST NOT BECOME PUBLIC AUTOMATICALLY". Generation success means the concept exists in the workspace; it does NOT mean it is approved for public portfolio/SEO publication.
- Separation of 3 Distinct Lifecycle Stages:
  - Generation Complete: newly generated AI concepts strictly default to `MediaVisibility.PRIVATE`.
  - Portfolio Eligible: professional deliberately promotes/assigns to portfolio via `PATCH /media/{mediaId}` with `visibility = PORTFOLIO`.
  - Publicly Exposed: public delivery occurs only when studio is `PUBLISHED`, project is `PORTFOLIO`/`PUBLIC`, and media is `PORTFOLIO`/`PUBLIC`.
- Privacy Hardening & Delivery Gating Architecture:
  - Master Clean Original: stored permanently under private key (`studio/{studioId}/projects/{projectId}/original/...`); never exposed over public CDN or API.
  - Authenticated Private Workspace Preview (`GET /api/v1/media/{mediaId}/preview`): workspace members view preview bytes with `✦ AI Concept Visualization` badge burned in. Clean original master is never exposed. Cross-tenant calls return 403 Forbidden; unauthenticated calls return 401 Unauthorized.
  - Public Derivative CDN Delivery Gate (`GET /media/public/**`): inspects storage key; enforces that studio must be `PUBLISHED`, project must not be `PRIVATE`, and media must not be `PRIVATE`. Returns HTTP 404 if any check fails.
  - Invalidation: Updating media visibility from `PORTFOLIO`/`PUBLIC` to `PRIVATE` or deleting media immediately purges all public derivatives from storage and the `media_derivatives` table.
  - SEO Isolation: Sitemap generation and public SEO project pages strictly exclude private AI concepts.
- Frontend Alignment (`apps/web`):
  - `AiVisualizerClient.tsx`: Visualizer workspace indicates "Private Concept" status badge; preview URLs route through authenticated preview endpoint.
  - `ProjectMediaManager.tsx`: Private media assets render authenticated preview thumbnails; designers can deliberately promote AI concepts to `PORTFOLIO` or `PUBLIC` via the edit modal.
- Verification Baseline:
  - Backend: **180 / 180 PASS** (9 comprehensive integration scenarios in `AiPrivacyHardeningTest` + updated `MediaIntegrationTest` and `AiVisualizerServiceTest`).
  - Frontend: **189 / 189 PASS** (All Vitest unit tests clean).
  - Typecheck: **PASS** (`tsc --noEmit` clean).
  - Lint: **PASS** (`eslint .` clean).
  - Production build: **PASS** (Next.js 16 Turbopack optimized production build clean).
  - Working tree: clean. local HEAD == origin/main.




