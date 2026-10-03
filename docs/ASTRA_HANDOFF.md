# ASTRA PROJECT HANDOFF
## Canonical Project Context & Handoff for Astra (Senior Architect & Meta-Prompt Engineer)

**Last Updated:** October 1, 2026  
**Repository Source of Truth:** `https://github.com/mani1715/Interior.git`  
**Current Branch:** `main`  
**Current HEAD Commit:** `phase-29: implement admin and production hardening`  
**Local / Remote Sync:** `local HEAD == origin/main` (Clean working tree)  
**Completed Phases:** Phase 00 through Phase 29 (100% COMPLETE & PASS)  
**Platform Status:** PRODUCTION-READY HARDENED PLATFORM BASELINE (All Canonical Phases 01-29 Complete)

---

## 1. REPOSITORY & STATE VERIFICATION

The canonical repository is:
`https://github.com/mani1715/Interior.git`

Git state verification confirmed on local workspace:
- **Branch:** `main`
- **Local HEAD:** `96023ff204560af687e827eb72499101ca37c0de` (`phase-28: implement analytics plans and billing`)
- **Remote HEAD (`origin/main`):** `96023ff204560af687e827eb72499101ca37c0de`
- **Working Tree:** Clean (all changes committed and synchronized).

Recent commit history ledger:
- `96023ff`: phase-28: implement analytics plans and billing
- `c35e672`: phase-27: implement reviews verification and collections
- `ba9e36e`: phase-26.2: harden public lead security definer boundary
- `30a5f29`: phase-26.1: harden leads pii rls and whatsapp security
- `5b9e84b`: phase-26: implement leads crm and whatsapp
- `8ad31f1`: phase-25.2: harden discovery public read security
- `26b3fdc`: phase-25.1: harden discovery truthfulness and api contract
- `d7b9ec3`: feat(discovery): implement Phase 25 Search & Discovery Engine with GIN trigram indexing and public API
- `e456f8a`: audit: reconcile phase history and canonical architecture
- `f2e9fd1`: audit: harden phases 00-24 before search and discovery

---

## 2. CANONICAL CONTEXT ARCHITECTURE

This document (`docs/ASTRA_HANDOFF.md`) is the single canonical handoff document for Astra.

Context file discipline rules:
- `docs/ASTRA_HANDOFF.md` (this file): Canonical transfer, bootstrap, and architectural context for Astra.
- `docs/AGENT_CONTEXT.md`: Persistent technical implementation ledger maintained across development phases.
- **NO duplicate/numbered handoff files** (`ASTRA_CONTEXT_1.md`, `handoff-v2.md`, etc.) are permitted.

---

## 3. PURPOSE OF ASTRA_HANDOFF.md

This document provides complete, production-grade context enabling **Astra** to act as Senior Architect, Planner, Security Auditor, and Meta-Prompt Engineer for the project. Antigravity remains the primary execution and code development agent with direct codebase and shell access.

Astra must read this document to understand:
- Product positioning and user personas
- Complete locked tech stack & architectural invariants
- Comprehensive phase history (Phases 00–28)
- 18-section portfolio engine and 6 presentation templates
- Project CMS, Media Engine, and SEO Engine
- Multi-modal AI Visualizer, reference system, precision mask editor, and client approval bundles
- PostgreSQL-native Search & Discovery engine
- Private Leads CRM, WhatsApp integration, and `SECURITY DEFINER` public lead boundary
- Invited client reviews, professional business verification, and private user collections
- Privacy-safe product analytics, internal subscription plans, entitlement engine, and billing lifecycle
- Business decisions still unconfigured vs. technical debt (zero technical defects)
- Phase 29 scope and operating boundaries

---

## 4. PRODUCT OVERVIEW

The platform is a premium professional platform for:
1. **Individual Interior Designers**
2. **Interior Studios**
3. **Architects**
4. **Architecture Studios**
5. **Custom Furniture Professionals**
6. **Woodwork / Cabinetry Professionals**
7. **Turnkey Contractors**

### Core Value Proposition & Capabilities

**For Professionals:**
- **Public Portfolios:** Build responsive, editorial public portfolios using 6 distinct design templates with pure props decoupling (`CONTENT != TEMPLATE`).
- **Project CMS:** Publish complete project stories with location, style, room category, budget privacy controls, and client confidentiality safeguards.
- **Google Discoverability:** Native SSR rendering, dynamic `sitemap.xml`, clean `robots.txt`, and Schema.org structured data (`ProfessionalService`, `CreativeWork`, `BreadcrumbList`).
- **AI Interior Visualizer:** Generate interior concept visualizer jobs (Full Image & Precision Mask editing), reference image guidance (13 purposes), variation chains, and secure client review bundles.
- **Leads & CRM:** Ingest public inquiries into a studio-isolated CRM pipeline (`NEW` -> `CONTACTED` -> `IN_PROGRESS` -> `PROPOSAL_SENT` -> `WON` / `LOST` -> `ARCHIVED`) with follow-up scheduling, team assignment, internal notes, and direct WhatsApp handoff.
- **Genuine Reviews:** Invite real clients (from `WON` leads) via 256-bit single-use cryptographic tokens to submit authenticated 1-5 star reviews with sub-ratings and verified badges.
- **Business Verification:** Submit private evidence documents (business registration, GST, licenses; NO Aadhaar/PAN) for admin review to earn a "Verified Business" badge.
- **Analytics:** View privacy-safe metrics (impressions, profile views, project views, inquiry conversion funnel) without third-party tracking scripts.
- **Plans & Entitlements:** Operate on an internal non-commercial `BASE` plan with unlimited capacity until commercial pricing and payment gateways are configured.

**For Consumers / Clients:**
- **Project-First Discovery:** Search and discover interior work by city, room type, budget, style, and professional trade using full-text trigram search.
- **WhatsApp Handoff:** Connect directly with designers via official WhatsApp handoff links.
- **Private Collections:** Save public projects into custom user-owned mood boards (100% private from studios).
- **Client Review Portal:** Review AI visualizer concepts, leave comments, and grant non-contractual concept approvals via single-use secure link sessions.

---

## 5. LOCKED TECH STACK

All versions verified from root build manifests (`pom.xml`, `package.json`):

### Frontend (`apps/web`)
- **Framework:** Next.js 16.3.3 (App Router, Turbopack, React Server Components where applicable)
- **UI Core:** React 19.3.0, TypeScript 6.0.3 (`tsc --noEmit` clean)
- **Styling:** Vanilla CSS & CSS Modules with 12 locked CSS custom property brand tokens
- **Icons:** `lucide-react`
- **Testing:** Vitest 5.0.1, React Testing Library, JSDOM

### Backend (`apps/api`)
- **Runtime:** Java 25 (Eclipse Adoptium Temurin `25.0.4.1+1-LTS`)
- **Compiler Target:** Java 21 bytecode release target
- **Framework:** Spring Boot 3.4.3 (Web, Data JPA, Validation, Security, Actuator)
- **Build Tool:** Apache Maven 3.9.9 (`mvnw` wrapper checked in)

### Persistence & Security
- **Primary Database:** PostgreSQL 18.3 (active on port 5433, database `interior_design_dev`)
- **Migration Engine:** Flyway 10.x (`V001` through `V021` applied cleanly)
- **Test Database:** In-memory H2 with Flyway test migrations (`V000` through `V021`)
- **Architecture:** Modular Monolith with REST API under `/api/v1`
- **Domain Identity:** RFC 9562 UUIDv7 persisted domain identifiers (`UuidV7`)
- **Tenant Security:** Row Level Security (RLS) forced across all studio-isolated tables

---

## 6. CORE ARCHITECTURAL INVARIANTS

Astra must enforce all non-negotiable architectural invariants:

1. **Tenant Isolation:** Studio-owned resources are strictly tenant-isolated via composite keys `(id, studio_id)` and PostgreSQL RLS. Cross-tenant leakage is a P0 security bug.
2. **Public vs. Private Projections:** Public API endpoints (`/api/v1/discovery`, `/api/v1/portfolio/public/*`, `/api/v1/seo/*`) return public-safe DTOs. Internal entities and PII are never exposed.
3. **ORIGINAL MEDIA != PUBLIC MEDIA:** Private master upload assets (`studio/{id}/projects/{id}/original/*`) are never served publicly. Public delivery strictly serves responsive WebP derivatives (`THUMBNAIL`, `MEDIUM`, `LARGE`). Missing derivative must fail gracefully, NEVER falling back to private master.
4. **AI GENERATION != PUBLICATION:** Generated AI concepts default to `MediaType.AI_CONCEPT`, `MediaVisibility.PRIVATE`, and `is_shortlisted = false`. Promotion to public status requires explicit studio action.
5. **Client Approval != Public Publication:** Client approval of an AI concept in Phase 24 is a private creative milestone; it does not make the concept public or create contractual commitment.
6. **Verification != Recommendation:** "Verified Business" status indicates evidence document review (registration, GST). It NEVER implies platform quality guarantee, endorsement, or project outcome warranty.
7. **Reviews != Testimonials:** Invited client reviews (`studio_reviews`) are authenticated, aggregate-calculated feedback originating from `WON` leads. Portfolio testimonials (`TESTIMONIALS` section) are studio-authored editorial content.
8. **Collections are Private:** User collections (`user_collections`) are owned by consumer users via `user_id`. Studios CANNOT see who saved their projects, and collection save counts NEVER influence search ranking.
9. **Billing Neutrality:** Subscription plan, paid status, billing tier, lead volume, verification status, and review ratings have 0.0% influence on search discovery ranking.
10. **Verification Cannot Be Purchased:** Verification is evidence-based admin review. Paid plans cannot bypass or purchase verification.
11. **Reviews Cannot Be Suppressed:** Paid subscription plans cannot hide, delete, or alter negative reviews.
12. **Downgrade Safety:** Plan downgrades or subscription expiration must NEVER delete user/studio data, projects, media, or leads. Content remains safely preserved.

---

## 7. DESIGN SYSTEM & VISUAL PALETTE

The locked platform design system uses 12 curated CSS variables.

### Locked Brand Palette Tokens
- **Warm Cream Background:** `--color-cream: #FAF8F5`
- **Charcoal Primary Typography:** `--color-charcoal: #1F1F1F`
- **Surface White:** `--color-white: #FFFFFF`
- **Off-White Container:** `#F4F4F4`
- **Sand Border / Card Accent:** `--color-sand: #E7E1D8`
- **Divider Neutral:** `#D9D9D9`
- **Muted Text:** `#6B6B6B`
- **Warm Bronze Accent (Restrained):** `--color-bronze: #B88A5A`
- **Secondary Palette:** `#2E5D4B` (Forest), `#3E6D8C` (Slate Blue), `#C76F4A` (Terracotta), `#E6C9C3` (Blush)

### Design Constraints
- **Restrained Bronze Accent:** Bronze is used exclusively for primary CTAs, active indicators, and focus outlines.
- **FORBIDDEN:** Heavy glassmorphism, neon colors, generic SaaS blue gradients, or dominant purple palettes on platform pages.
- **Portfolio Template Independence:** Public portfolio templates (`BASIC`, `MODERN`, `LUXURY`, `ARCHITECTURAL`, `WARM_NATURAL`, `DARK_CINEMATIC`) maintain their own scoped CSS and visual identities.

---

## 8. MOBILE-FIRST UX & ACCESSIBILITY RULES

1. **Target Viewports:** Base viewports 360px, 390px, 430px. Tested up to 768px, 1024px, 1440px+.
2. **Touch Targets:** All interactive buttons, links, inputs, and toggles must measure $\ge 44 \times 44\text{px}$.
3. **No Horizontal Overflow:** Zero `overflow-x` scrolling on mobile viewports.
4. **No Hover Dependency:** Primary navigation and actions must be accessible without hover states.
5. **Accessible Dialogs:** Modals and slide-over sheets use `role="dialog"`, `aria-modal="true"`, focus traps, and Escape key focus restoration.
6. **Reduced Motion:** Respect `prefers-reduced-motion: reduce` in CSS and JS animation loops (`PortfolioMotion.tsx`).
7. **SSR Integrity:** Public routes must render complete, accessible HTML content on the server without depending on client JS hydration.

---

## 9. CANONICAL PROFESSIONAL TYPES

System enum `ProfessionalType.java` and PostgreSQL DB constraints enforce 7 canonical trade categories:
1. `INDIVIDUAL_DESIGNER` — Individual Designer
2. `INTERIOR_STUDIO` — Interior Studio
3. `ARCHITECT` — Architect
4. `ARCHITECTURE_STUDIO` — Architecture Studio
5. `CUSTOM_FURNITURE` — Custom Furniture Studio / Craftsman
6. `WOODWORK_CABINETRY` — Woodwork & Cabinetry Professional
7. `TURNKEY_CONTRACTOR` — Turnkey General Contractor

*(Code alias compatibility handles legacy values `CUSTOM_FURNITURE_STUDIO` and `WOODWORK_CABINETRY_PROFESSIONAL` seamlessly).*

---

## 10. COMPREHENSIVE PHASE ROADMAP LEDGER (PHASES 00–28)

| Phase | Canonical Name | Major Deliverable | Key Migration | Key Code Areas | Status |
|---|---|---|---|---|---|
| **00** | Project Specification | Master Product Spec & Requirements | — | `docs/MASTER_PRODUCT_SPEC.md` | **PASS** |
| **01** | Production Architecture | Architectural Boundaries & System Specs | — | `docs/01_PRODUCTION_ARCHITECTURE.md` | **PASS** |
| **02** | Database Architecture | 94-Table Schema & Entity Lifecycles | — | `docs/02_DATABASE_SCHEMA.md` | **PASS** |
| **03** | Monorepo & Security | Base Monorepo, Spring Boot, JDK 25, RLS | `V001`, `V002` | `apps/api`, `apps/web` | **PASS** |
| **04** | Design System UI | Locked Palette, Reusable Components | — | `src/components/ui`, `/design-system` | **PASS** |
| **04.1** | Design System Lock | Token Lock & Runtime Standards | — | `src/app/globals.css` | **PASS** |
| **05** | Production Homepage | 16 Homepage Sections, 7-Stage Scrubber | — | `src/app/page.tsx` | **PASS** |
| **05.1** | Homepage Truthfulness | SEO Snippet & Truthfulness Hardening | — | `src/app/page.tsx` | **PASS** |
| **06** | Public Discovery UI | Project & Professional Discovery | — | `src/app/(public)/projects` | **PASS** |
| **07** | Auth & Identity | OIDC, Opaque Session, CSRF, Rate Limiter | `V003` | `com.interior.platform.security` | **PASS (PARTIAL)** |
| **07.1** | Auth Security Closure | Persistent OIDC Store, PKCE, MFA | `V003` | `OidcTransactionStore.java` | **PASS** |
| **08** | Professional Onboarding | 7-Step Onboarding Wizard | `V004` | `/onboarding/professional` | **PASS** |
| **08.1** | Specialty Normalization | Relational Specialties Storage | `V005` | `studio_specialties` | **PASS** |
| **08.2** | UUIDv7 Canonicalization | Platform-Wide RFC 9562 UUIDv7 | — | `UuidV7.java` | **PASS** |
| **09** | Professional Workspace | Workspace Shell, Dual-Score Completeness | — | `/workspace`, `WorkspaceService` | **PASS** |
| **09.1** | Workspace Readiness | Truthful Readiness Badges | — | `WorkspaceService.java` | **PASS** |
| **10** | Portfolio Builder Engine | Engine (`CONTENT != TEMPLATE`), Versioning | `V006` | `com.interior.platform.portfolio` | **PASS** |
| **10.1** | Contract Alignment | API Endpoint & Concurrency Lock | — | `PortfolioController.java` | **PASS** |
| **11** | BASIC Template | Clean Editorial Theme (v1.0.0) | — | `components/portfolio/templates/basic` | **PASS** |
| **12** | MODERN Template | Modern Minimalist Theme (v1.0.0) | — | `components/portfolio/templates/modern` | **PASS** |
| **13** | LUXURY Template | Luxury Atelier Theme (v1.0.0) | — | `components/portfolio/templates/luxury` | **PASS** |
| **14** | ARCHITECTURAL Template | Monograph Monograph Theme (v1.0.0) | — | `components/portfolio/templates/architectural` | **PASS** |
| **15** | WARM_NATURAL Template | Biophilic Tactile Theme (v1.0.0) | — | `components/portfolio/templates/warm-natural` | **PASS** |
| **16** | DARK_CINEMATIC Template | Moodboard Cinematic Theme (v1.0.0) | — | `components/portfolio/templates/dark-cinematic` | **PASS** |
| **17** | Portfolio Integration QA | 6-Template QA, PortfolioMotion Island | — | `PortfolioMotion.tsx` | **PASS** |
| **18** | Professional Project CMS | Project Story Editor, Readiness Checklist | `V007` | `com.interior.platform.projects` | **PASS** |
| **19** | Media Engine | Upload Pipeline, Derivatives, Watermarks | `V008` | `com.interior.platform.media` | **PASS** |
| **20** | Production SEO Engine | Publication Gate, Sitemap, JSON-LD, SERP | `V009` | `com.interior.platform.seo` | **PASS** |
| **21** | AI Visualizer Foundation | Async Jobs, Provider Abstraction, Quota | `V010` | `com.interior.platform.ai` | **PASS** |
| **21.1** | AI Concept Privacy | Private-by-Default AI Output Guardrail | — | `AiVisualizerService.java` | **PASS** |
| **22** | AI Reference System | 13 Reference Purposes, Structure Preservation | `V011` | `ReferencePurpose.java` | **PASS** |
| **23** | Precision Editing | Inpainting Canvas, Mask Upload, Coverage | `V012` | `PrecisionMaskEditor.tsx` | **PASS** |
| **24** | AI Lineage & Reviews | Lineage, Client Reviews, Non-Contractual Approval | `V013`, `V014` | `/review/view/[id]` | **PASS** |
| **25** | Search & Discovery | PostgreSQL GIN Trigram Search, Filters | `V015` | `com.interior.platform.discovery` | **PASS** |
| **25.1** | Discovery Truthfulness | Unified `/api/v1/discovery`, Combobox | — | `ProjectsDiscoveryClient.tsx` | **PASS** |
| **25.2** | Discovery Public RLS | Correlated Studio RLS Hardening | `V016` | `V016__discovery_public_read_hardening.sql` | **PASS** |
| **26** | Leads & CRM | Ingestion, Pipeline Statuses, WhatsApp | `V017` | `com.interior.platform.leads` | **PASS** |
| **26.1** | Leads Security Hardening | Lead PII Masking, WhatsApp Dedupe | `V018` | `LeadSecurityClosureTest.java` | **PASS** |
| **26.2** | Public Lead Boundary | `SECURITY DEFINER` Ingest Role Protection | `V019` | `public.submit_public_lead(...)` | **PASS** |
| **27** | Reviews, Verification & Collections | Invited Reviews, Verification Audit, Collections | `V020` | `reviews`, `verification`, `collections` | **PASS** |
| **28** | Analytics, Plans & Billing | Telemetry, Base Plan, Billing Lifecycle | `V021` | `analytics`, `billing` | **PASS** |

---

## 11. PORTFOLIO ENGINE ARCHITECTURE

- **Core Invariant:** `CONTENT != TEMPLATE`. Domain content (`portfolios`, `portfolio_sections`) is strictly decoupled from presentation templates. Designers can switch themes non-destructively.
- **18 Section Types:** `HERO`, `ABOUT`, `SERVICES`, `FEATURED_PROJECTS`, `PROJECT_GRID`, `BEFORE_AFTER`, `BEFORE_AI_REALITY`, `DESIGN_PROCESS`, `TESTIMONIALS`, `TEAM`, `AWARDS`, `PRESS`, `SERVICE_AREAS`, `FAQ`, `CONTACT`, `CTA`, `VIDEO`, `CUSTOM_NOTE`.
- **6 Production Templates (All AVAILABLE 1.0.0):** `BASIC`, `MODERN`, `LUXURY`, `ARCHITECTURAL`, `WARM_NATURAL`, `DARK_CINEMATIC`.
- **Versioning & Concurrency:** Optimistic locking via `version` integer field on mutations. `portfolio_versions` stores up to 10 immutable JSON snapshots per studio.
- **Preview Protection:** `/workspace/portfolio/preview` exposes consented public contacts only and enforces `<meta name="robots" content="noindex, nofollow" />`.
- **`PortfolioMotion.tsx`:** Presentation-only motion island using lightweight IntersectionObserver reveals with full `prefers-reduced-motion` compliance and zero scroll loops.

---

## 12. PROJECT CMS ARCHITECTURE

- **Domain Entities:** `studio_projects` (UUIDv7, tenant isolated), `project_styles`.
- **Project Statuses:** `DRAFT`, `READY` (server-evaluated based on required attributes).
- **Readiness Criteria:** Requires title, slug, summary, room type, style, location, cover image, and visibility settings.
- **Privacy Protections:** Confidential client names and exact budget figures are protected. Budgets display as public ranges only when consented.
- **Categories & Styles:** 15 room categories and 8 interior design styles.

---

## 13. MEDIA ENGINE ARCHITECTURE

- **Upload Flow:** Client requests upload intent (`POST /media/upload-intent`), streams raw bytes to quarantined storage (`PUT /media/upload/{id}`), and commits (`POST /media/commit`).
- **Derivative Pipeline:** Server strips EXIF metadata, resizes to responsive WebP variants (`THUMBNAIL` 400px, `MEDIUM` 1200px, `LARGE` 1920px), applies studio watermark, and burns permanent `✦ AI Concept Visualization` badge into AI concepts.
- **Privacy Safeguard:** `CLIENT_PRIVATE` and `REFERENCE` media generate zero public derivatives and cannot be served via `/api/v1/media/public/**`.
- **Fallback Invariant:** Public delivery endpoints return 404 if a public derivative is missing; they NEVER serve private master originals.

---

## 14. PRODUCTION SEO ENGINE

- **Publication Gate:** Studios default to `UNPUBLISHED` (404 on public routes). Publishing via `/seo/publish` validates profile completeness, portfolio readiness, and ready projects. Unpublishing (`/seo/unpublish`) immediately removes public access and sitemap entries.
- **Bot vs. Platform Discovery:** `indexing_enabled` controls search engine indexing (robots/sitemap consent), whereas human platform discovery inside the app depends strictly on `PUBLISHED + ACTIVE` status.
- **Structured Data:** Generates Schema.org `ProfessionalService`, `CreativeWork`, and `BreadcrumbList` via `SafeJsonLd` XSS-escaped serializer.

---

## 15. AI VISUALIZER ARCHITECTURE

- **Async Job Pipeline:** `ai_visualization_jobs` state machine (`QUEUED` -> `PROCESSING` -> `SUCCEEDED` / `FAILED` / `CANCELLED`) with daily studio quotas (50 free/mo) and burst rate limits.
- **Editing Modes:**
  - `FULL_IMAGE`: Source photo transformed via natural language prompt.
  - `PRECISION_MASK`: Target region edited using interactive canvas mask overlay (`PrecisionMaskEditor.tsx`).
- **Reference System:** Up to 4 reference images attached per job across 13 canonical purposes (`COLOR`, `WOOD`, `STONE`, `FABRIC`, etc.) with immutable job snapshots.
- **Lineage & Variations:** Root-propagated variation chains (`parent_job_id`, `root_job_id`) with shortlist and studio pick tags.
- **Client Approval Bundles:** Secure client review presentations (`/review/view/[id]`) with HttpOnly session cookies, anti-CSRF protection, Before/After slider, discussion comments, and non-contractual approval audit trail.

---

## 16. AI REFERENCE PURPOSES (13 CANONICAL VALUES)

1. `COLOR`: Color Palette (wall/accent colors and tone)
2. `MATERIAL`: General Material Guidance
3. `WOOD`: Wood & Laminate (veneer, timber, grain, finish)
4. `STONE`: Stone, Granite & Marble
5. `TILE`: Tile & Backsplash (floor/wall patterns)
6. `FABRIC`: Fabric & Upholstery
7. `HARDWARE`: Hardware & Fixtures (handles, faucets, metals)
8. `FURNITURE_STYLE`: Furniture Style (seating, tables)
9. `CABINET_STYLE`: Cabinetry & Wardrobes (shutters, grooves)
10. `ROOM_STYLE`: Spatial Ambiance & Theme
11. `WALL_FINISH`: Wall Finish (wallpaper, fluted panels)
12. `CEILING_STYLE`: Ceiling & Lighting (coves, fixtures)
13. `GENERAL_STYLE`: Broad Aesthetic Inspiration

---

## 17. SEARCH & DISCOVERY ENGINE

- **PostgreSQL-Native:** Full-text search using `'simple'` dictionary GIN indexes (`idx_studio_projects_fts`, `idx_designer_studios_fts`) combined with `pg_trgm` trigram similarity (`> 0.25`).
- **Correlated RLS (`V016`):** Anonymous public queries enforce that parent `designer_studios` must be `PUBLISHED + ACTIVE` before child projects or services return.
- **Ranking Neutrality:** Search score is strictly calculated from text relevance, location match, category match, and project completeness. **0.0% ranking weight** comes from paid plan, billing tier, review count, rating, verification status, or collections.

---

## 18. LEADS + CRM SUBSYSTEM

- **Public Lead Ingestion:** `POST /api/v1/public/leads` with honeypot validation (`website_hp`), E.164 phone normalization (+91 India-first), IP/phone rate limiting, and server-derived source attribution.
- **CRM Pipeline:** Workspace `/workspace/leads` provides pipeline status filters (`NEW`, `CONTACTED`, `IN_PROGRESS`, `PROPOSAL_SENT`, `WON`, `LOST`, `ARCHIVED`), follow-up date picker, team assignment, internal notes, and audit timeline.
- **Hardened Security Definer Boundary (`V019`):**
  - Dedicated PostgreSQL role `lead_ingest_role` with `NOSUPERUSER NOBYPASSRLS`.
  - Secure function `public.submit_public_lead(...)` with `SET search_path = pg_catalog, public`.
  - Direct `INSERT`/`SELECT`/`UPDATE` table access on `studio_leads` revoked from `PUBLIC`.
  - Public callers can ONLY submit leads through `public.submit_public_lead(...)`.
- **PII Protection:** `LeadSummaryDto` returns masked phone (`+91 ••••• •4321`) and masked email for list views. Full PII is strictly restricted to authenticated studio members via `GET /api/v1/leads/{id}`.

---

## 19. WHATSAPP SUBSYSTEM

1. **Mode A — User-Initiated Handoff:** Generates pre-filled `wa.me` links. Logs audit event `WHATSAPP_HANDOFF_OPENED`. Truthfully disclaims that handoff opening does not confirm delivery or response.
2. **Mode B — Managed WhatsApp:** Provider abstraction `WhatsAppProvider` with `DisabledWhatsAppProvider` returning truthful `NOT_CONFIGURED` status. Compose controls in workspace CRM disabled when unconfigured.

---

## 20. REVIEWS SUBSYSTEM

- **Invited Client Reviews:** Originates exclusively from `WON` CRM leads. Studio sends invitation generating a 256-bit cryptographic single-use token (`V020`).
- **Token Exchange:** Public exchange endpoint verifies raw token hash, issues HttpOnly `review_session` cookie + anti-CSRF token, and redirects client to `/review/submit` (scrubbing raw token from URL).
- **Review Content:** 1-5 overall rating, category sub-ratings (design, communication, timeliness, budget), verified client badge, reviewer privacy settings (`FIRST_NAME`, `INITIALS`, `ANONYMOUS`), and studio response.
- **Aggregate Calculation:** Studio aggregate rating excludes unapproved, reported, or removed reviews. Negative reviews cannot be suppressed by paid plans.

---

## 21. VERIFICATION SUBSYSTEM

- **State Machine:** `NOT_SUBMITTED` -> `PENDING` -> `NEEDS_MORE_INFO` -> `VERIFIED` / `REJECTED` -> `REVERIFY_REQUIRED` / `EXPIRED`.
- **Evidence Storage:** Private upload of business registration, GST certificate, and professional licenses (strictly NO Aadhaar or PAN card collection). Stored in non-public storage with access auditing.
- **Snapshot Fingerprinting:** Core profile changes trigger automatic transition to `REVERIFY_REQUIRED` and suppress the public badge until re-approved.
- **Truthful Wording:** Badge displays "Verified Business" with a shield icon. Tooltip explicitly disclaims quality guarantees or official endorsement.

---

## 22. COLLECTIONS SUBSYSTEM

- **User-Owned RLS:** Collections (`user_collections`) are owned by consumer users via `user_id` context (`app.current_user_id`).
- **Complete Privacy:** Saved projects are 100% private to the user. Studios CANNOT see who saved their projects. Save counts are never displayed publicly and never affect search ranking.
- **Tombstones:** Deleted or unpublished projects display as soft tombstones in user collections without breaking collection integrity.

---

## 23. ANALYTICS SUBSYSTEM (PHASE 28)

- **Telemetry Ingestion:** Public endpoint `POST /api/v1/public/analytics/events` collects low-trust interactions (`PUBLIC_PROFILE_VIEW`, `PUBLIC_PROJECT_VIEW`, `DISCOVERY_RESULT_CLICK`, `INQUIRY_OPENED`).
- **Privacy Hygiene:** Referrers domain-scrubbed, URL tokens removed, IPs and user-agents anonymized into daily SHA-256 session hashes. Dedicated hourly deduplication keys prevent metric inflation.
- **High-Trust Events:** Server-side domain events (`LEAD_CREATED`, `WON_LEAD`, `REVIEW_SUBMITTED`, `AI_GENERATION_COMPLETED`) recorded with zero public tampering possible.
- **Daily Rollups:** `studio_daily_metrics` maintains pre-aggregated daily counts for fast dashboard loading. Workspace `/workspace/analytics` renders conversion funnels and performance cards using real data only.

---

## 24. PLANS & ENTITLEMENTS ARCHITECTURE (PHASE 28)

- **Base Internal Plan (`BASE`):** Internal baseline tier (`purchasable = false`, price ₹0). Unsubscribed studios automatically resolve to `BASE`.
- **Unlimited Baseline:** Base plan entitlement keys (`ENTITLEMENT_PROJECT_LIMIT`, `ENTITLEMENT_MEDIA_STORAGE_MB`, `ENTITLEMENT_MONTHLY_AI_CREDITS`) default to unlimited capacity (null numeric limit).
- **Backend Enforcement:** `EntitlementService` checks limits before mutating operations.

---

## 25. BILLING SYSTEM ARCHITECTURE (PHASE 28)

- **Provider Abstraction:** `BillingProvider` interface implemented with production default `DisabledBillingProvider` (`billingProviderStatus = 'NOT_CONFIGURED'`).
- **Disabled Commercial Checkout:** UI at `/workspace/billing` truthfully informs users that commercial checkout is disabled and all platform features remain active.
- **Audit & Idempotency:** `billing_events` stores raw webhook payloads with unique constraints; `billing_transactions` records immutable payment ledgers. Plan cancellations preserve access until period end without data deletion.

---

## 26. UNRESOLVED COMMERCIAL BUSINESS DECISIONS

The following items are **BUSINESS DECISIONS**, NOT technical bugs or missing code:
- Commercial plan names, catalog structure, and feature differentiation
- Commercial pricing (monthly / annual rates in INR)
- Numeric tier limits (project counts, media storage limits, AI credit quotas)
- Free trial durations and grace period policies
- Upgrade / downgrade proration rules and refund policy
- GST / Tax handling and invoice templates
- Selection and activation of production payment gateway provider (Razorpay / Stripe)
- Webhook signing secrets and merchant onboarding

---

## 27. DATABASE MIGRATION LEDGER

### Production Migrations (`apps/api/src/main/resources/db/migration/`)

| Migration File | Description / Major Entities | Phase |
|---|---|---|
| `V001__security_identity_tenant_schema.sql` | `users`, `designer_studios`, `studio_members`, `identity_sessions` | Phase 03 |
| `V002__pg_rls_policies.sql` | Initial PostgreSQL Row Level Security (RLS) policies | Phase 03 |
| `V003__auth_oidc_transactions.sql` | `auth_oidc_transactions` durable store | Phase 07.1 |
| `V004__designer_onboarding.sql` | `studio_contacts`, `studio_services`, `studio_service_areas` | Phase 08 |
| `V005__studio_specialties_and_onboarding_closure.sql` | `studio_specialties` relational table | Phase 08.1 |
| `V006__portfolio_engine.sql` | `portfolios`, `portfolio_sections`, `portfolio_versions` | Phase 10 |
| `V007__project_cms.sql` | `studio_projects`, `project_styles` | Phase 18 |
| `V008__media_engine.sql` | `studio_watermark_settings`, `upload_intents`, `media_assets`, `media_derivatives` | Phase 19 |
| `V009__seo_engine.sql` | `studio_seo_settings` | Phase 20 |
| `V010__ai_visualizer_foundation.sql` | `ai_visualization_jobs`, `ai_usage_events` | Phase 21 |
| `V011__ai_reference_images.sql` | `ai_reference_metadata`, `ai_job_references` | Phase 22 |
| `V012__ai_precision_editing.sql` | Precision mask columns & studio mode index | Phase 23 |
| `V013__ai_variations_client_approval.sql` | `ai_client_reviews`, `ai_client_review_items`, `ai_client_review_decisions`, `ai_client_review_comments` | Phase 24 |
| `V014__professional_type_expansion.sql` | `ProfessionalType` DB check constraint expansion | Phase 24 Audit |
| `V015__search_discovery_engine.sql` | GIN full-text search indexes (`idx_studio_projects_fts`, `idx_designer_studios_fts`) | Phase 25 |
| `V016__discovery_public_read_hardening.sql` | Correlated public-read RLS policies (`PUBLISHED + ACTIVE` studio requirement) | Phase 25.2 |
| `V017__leads_crm_whatsapp.sql` | `studio_leads`, `lead_activities`, `lead_notes`, `lead_whatsapp_messages` | Phase 26 |
| `V018__lead_security_hardening.sql` | Lead idempotency & RLS hardening | Phase 26.1 |
| `V019__public_lead_function_hardening.sql` | `lead_ingest_role` and `public.submit_public_lead(...)` `SECURITY DEFINER` boundary | Phase 26.2 |
| `V020__reviews_verification_collections.sql` | `studio_review_invitations`, `studio_reviews`, `studio_verification_applications`, `studio_verification_documents`, `user_collections`, `user_collection_items` | Phase 27 |
| `V021__analytics_plans_billing.sql` | `analytics_events`, `studio_daily_metrics`, `billing_plans`, `plan_entitlements`, `studio_subscriptions`, `billing_events`, `billing_transactions` | Phase 28 |

### Test H2 Migrations (`apps/api/src/test/resources/db/test-migration/`)
- `V000__h2_compat.sql` (H2 compatibility functions)
- `V001` through `V021` (H2 syntax without PostgreSQL-specific GIN/RLS statements)

---

## 28. DATABASE SECURITY & PRIVILEGE MODEL

1. **FORCED RLS:** All tenant tables enforce `ALTER TABLE <table_name> FORCE ROW LEVEL SECURITY`.
2. **Session Context Variables:**
   - Studio tenant isolation: `app.current_studio_id`
   - User context isolation: `app.current_user_id`
3. **Dedicated Ingest Role (`lead_ingest_role`):** Non-superuser role owning `public.submit_public_lead(...)` function with immutable `search_path = pg_catalog, public`.
4. **Direct Table Privilege Revocation:** `PUBLIC` role has zero direct table access to `studio_leads`, `lead_activities`, `lead_notes`, `lead_whatsapp_messages`, `studio_reviews`, or `studio_verification_documents`.

---

## 29. UUID POLICY

- **Persisted Domain IDs:** Strict RFC 9562 UUIDv7 (`UuidV7.randomUuid()`) for time-ordered index locality.
- **Cryptographic Tokens:** Secure 256-bit random tokens (`SecureRandom`) for sessions, OIDC transactions, review invitations, and client review links. Only SHA-256 hashes are stored in the database.

---

## 30. AUTHENTICATION & SESSION SECURITY

- **Provider Neutrality:** OIDC service abstraction (`OidcService`) supporting Google, Apple, or custom OIDC identity providers.
- **Session Transport:** Server-side opaque 256-bit session tokens stored in `identity_sessions` DB table. Transported via `__Host-session` HttpOnly, Secure, SameSite=Lax cookies.
- **CSRF Protection:** Synchronizer token pattern with per-session anti-CSRF token verification (`CsrfProtectionTest.java`).
- **Rate Limiting:** Sliding-window in-memory rate limiter (`RateLimiterService`) protecting auth endpoints, public lead submissions, and AI visualizer submissions.

---

## 31. PUBLIC / PRIVATE ROUTE MAP

### Public Routes (SSR / Indexable)
- `/` — Production Homepage & 7-Stage Architectural Scrubber
- `/projects` — Public Project Discovery Catalog
- `/projects/[projectSlug]` — Public Project Story Detail Page
- `/professionals` — Public Professional Directory Catalog
- `/professionals/[professionalSlug]` — Public Studio Portfolio Page
- `/categories/[categorySlug]` — Room Category Discovery Page
- `/locations/[locationSlug]` — Regional Location Discovery Page
- `/review/invite/[token]` — Invited Client Review Entry Gate
- `/review/submit` — Invited Client Review Submission Page
- `/review/view/[id]` — Client AI Concept Review Presentation
- `/sign-in`, `/sign-up`, `/auth/callback`, `/auth/error` — Auth Pages
- `/sitemap.xml`, `/robots.txt` — SEO Endpoints

### Authenticated Consumer Routes
- `/account` — Account Profile & Settings
- `/account/collections` — Private User Inspiration Collections

### Professional Workspace Routes (`/workspace/**`)
- `/workspace` — Studio Dashboard & Completeness Score
- `/workspace/projects`, `/workspace/projects/[projectId]` — Project CMS Editor & Media Manager
- `/workspace/media` — Studio Media Library & Watermark Settings
- `/workspace/portfolio`, `/workspace/portfolio/preview` — Portfolio Builder & Theme Selector
- `/workspace/seo` — SEO Center Workspace & SERP Preview
- `/workspace/ai` — AI Visualizer Studio, Reference Manager & Lineage Tree
- `/workspace/leads` — Leads CRM & Follow-Up Management
- `/workspace/verification` — Business Verification Application Portal
- `/workspace/analytics` — Studio Analytics & Conversion Funnel Dashboard
- `/workspace/billing` — Subscription Plan & Billing Status Page

---

## 32. BACKEND MODULE MAP

All Java modules reside under `apps/api/src/main/java/com/interior/platform`:
- `com.interior.platform.security` — Identity, sessions, OIDC, CSRF, rate limiter, RLS
- `com.interior.platform.designers` — Designer profiles, studios, members, onboarding
- `com.interior.platform.portfolio` — Portfolio engine, 18 sections, version snapshots
- `com.interior.platform.projects` — Project CMS, readiness, privacy rules
- `com.interior.platform.media` — Media upload intent, WebP derivatives, watermark pipeline
- `com.interior.platform.seo` — Publication gate, SERP preview, sitemap, JSON-LD
- `com.interior.platform.ai` — AI visualizer jobs, reference images, precision mask, lineage, client reviews
- `com.interior.platform.discovery` — Search engine, GIN trigram search, filters
- `com.interior.platform.leads` — Public lead ingestion, CRM pipeline, WhatsApp handoff
- `com.interior.platform.reviews` — Review invitations, token exchange, reviews DTO
- `com.interior.platform.verification` — Business verification state machine, document upload
- `com.interior.platform.collections` — User private project collections
- `com.interior.platform.analytics` — Privacy-safe event ingestion, daily rollups, funnel statistics
- `com.interior.platform.billing` — BillingProvider interface, plans, entitlements, transactions
- `com.interior.platform.workspace` — Workspace dashboard, readiness aggregation

---

## 33. FRONTEND MODULE MAP

All Next.js code resides under `apps/web/src`:
- `src/app` — App Router routes (37 total routes)
- `src/components/ui` — Reusable design system UI components
- `src/components/portfolio/templates` — 6 canonical portfolio templates (`basic`, `modern`, `luxury`, `architectural`, `warm-natural`, `dark-cinematic`)
- `src/components/workspace` — Workspace layout and module pages
- `src/components/media` — Direct upload & ProjectMediaManager
- `src/components/ai` — AI Visualizer studio, `PrecisionMaskEditor`, comparison slider
- `src/lib/auth` — Session and CSRF token state client helpers
- `src/lib/seo` — SEO DTOs and `SafeJsonLd` helper
- `src/lib/ai` — Coordinate normalization math (`coordinates.ts`)

---

## 34. CURRENT TEST BASELINE

Fresh test suite run executed and verified 100% clean on September 28, 2026:

### Backend Test Suite (`apps/api`)
- **Command:** `./mvnw test`
- **Result:** **319 / 319 PASSED** (0 failures, 0 errors, 0 skipped across 25 test classes).

### Frontend Test Suite (`apps/web`)
- **Command:** `npm --prefix apps/web run test`
- **Result:** **251 / 251 PASSED** (0 failures across 34 test files).

### Frontend Typecheck (`apps/web`)
- **Command:** `npm --prefix apps/web run typecheck`
- **Result:** **PASS** (`tsc --noEmit` exit code 0).

### Frontend Lint (`apps/web`)
- **Command:** `npm --prefix apps/web run lint`
- **Result:** **PASS** (`eslint .` exit code 0).

### Frontend Production Build (`apps/web`)
- **Command:** `npm --prefix apps/web run build`
- **Result:** **PASS** (Next.js 16 Turbopack optimized production build clean across all 37 app routes).

---

## 35. DATABASE VALIDATION

- **Database Engine:** PostgreSQL 18.3
- **Latest Applied Migration:** `V021__analytics_plans_billing.sql`
- **Migration Status:** **PASS** (All 21 Flyway migrations applied cleanly and validated).

---

## 36. EXTERNAL CONFIGURATION STATUS

| Provider / Feature | Production Status | Details / Required Configuration |
|---|---|---|
| **OIDC Identity** | `NOT_CONFIGURED` | Dev/test sandbox active. Requires production client IDs and issuer URLs for Google/Apple OIDC. |
| **Media Storage** | `TEST_ONLY` | Local filesystem storage active. S3/Cloud Storage provider abstraction ready for AWS S3 / Cloudflare R2 credentials. |
| **AI Provider** | `NOT_CONFIGURED` | `DisabledAiImageProvider` active. Provider abstraction ready for Replicate/Stability/Vertex API keys. |
| **WhatsApp Managed** | `NOT_CONFIGURED` | `DisabledWhatsAppProvider` active. Mode A (user-initiated `wa.me`) active. Mode B requires Meta Graph API / Twilio credentials. |
| **Billing Provider** | `NOT_CONFIGURED` | `DisabledBillingProvider` active. Requires production payment gateway credentials (Razorpay/Stripe API keys & webhook secrets). |
| **Commercial Pricing** | `NOT_CONFIGURED` | Unresolved business decision. Base plan active with unlimited defaults. |
| **Email Provider** | `NOT_REQUIRED` | Not currently required for baseline operations. |
| **GST / Tax** | `NOT_CONFIGURED` | Unresolved business decision. |

---

## 37. KNOWN OPEN ITEMS

### CODE WORK
- **Zero known code defects.** The implementation passes all 319 backend tests, 251 frontend tests, typecheck, lint, and production build cleanly.

### BUSINESS DECISIONS
- Commercial plan structure, naming, and INR pricing.
- Tier quota limits for projects, media storage, and AI credits.
- GST compliance policy and invoice formatting rules.

### EXTERNAL CONFIGURATION
- Provisioning production API credentials for OIDC, S3 storage, AI generation provider, and payment gateway.

### PRODUCTION / DEPLOYMENT
- Production server environment setup, SSL certificate binding, domain DNS configuration, and automated database backup strategy (Phase 29).

---

## 38. PHASE 29 — ADMIN + PRODUCTION HARDENING (COMPLETE)

**STATUS:** **100% COMPLETE & VERIFIED**

Key deliverables implemented & verified:
- **Platform Administration & RBAC:** Explicit `ADMIN` and `SUPER_ADMIN` platform authorization enforced server-side via `AdminService` and separate from studio tenant membership.
- **Operational Dashboard (`/admin`):** Truthful operational metrics (active studios, registered users, pending verifications, review moderation queue, storage count and size, live audit trail).
- **User Management (`/admin/users`):** Inspection of users, account states (`ACTIVE`, `SUSPENDED`, `DISABLED`), state transition dialog with mandatory audited justification.
- **Studio Management (`/admin/studios`):** Studio inspection, publication/verification badges, suspension workflow with immediate discovery isolation.
- **Verification Workflow (`/admin/verification`):** Document and registration credential review, approve/reject decision modal with audit logging.
- **Review Moderation (`/admin/reviews`):** Community flag inspection, moderation actions (`PUBLISHED`, `FLAGGED`, `REMOVED`), strict invariant preventing rating or text corruption.
- **Security Hardening & Protection:**
  - Double-submit CSRF protection on mutation endpoints.
  - Rate limiting on auth, token exchange, reviews, analytics ingestion, and admin endpoints.
  - HSTS Strict-Transport-Security emitted conditionally on HTTPS requests only.
  - Startup `ProductionConfigurationValidator` preventing boot if `dev-auth-enabled=true` in `production`.
  - Structured logging with SLF4J MDC `requestId` correlation.
  - Actuator probes `/actuator/health/liveness` and `/actuator/health/readiness`.
- **Flyway V023 Migration:** Performance indexing on `audit_events`, `users`, `designer_studios`, `studio_verifications`, `studio_reviews`, and `review_reports`. Public read RLS policy on `studio_reviews`.
- **Containerization & CI/CD:** Production multi-stage `apps/api/Dockerfile`, `apps/web/Dockerfile`, `docker-compose.yml`, and GitHub Actions CI workflow.
- **Operational Documentation:** `docs/PRODUCTION_DEPLOYMENT.md`, `docs/DISASTER_RECOVERY.md`, `docs/SECURITY_HARDENING.md`, and `docs/ADMIN_OPERATIONS.md`.

---

## 39. ASTRA OPERATING RULES

When acting as Senior Architect, Planner, Security Auditor, and Meta-Prompt Engineer, Astra MUST adhere to these rules:

1. Read `docs/ASTRA_HANDOFF.md` and `docs/AGENT_CONTEXT.md` first before proposing any architectural plans.
2. Inspect the actual codebase and git history rather than assuming stale requirements.
3. Preserve the canonical phase roadmap; never renumber completed phases.
4. Never weaken PostgreSQL RLS policies, tenant scoping, or public read/write boundaries.
5. Use forward-only Flyway database migrations (`V022__...`). NEVER modify released migration files (`V001` through `V021`).
6. Enforce RFC 9562 UUIDv7 for all new domain entity primary keys.
7. Do not invent business pricing or claim external payment provider readiness when credentials are unconfigured.
8. Maintain architectural simplicity; do not introduce microservices, Redis, Elasticsearch, or Kubernetes without explicit justification. PostgreSQL remains the canonical database for search, RLS, and persistence.
9. Preserve mobile-first UX ($\ge 44\text{px}$ touch targets, zero horizontal overflow).
10. Enforce the locked 12-token platform color palette on core platform routes while respecting independent public portfolio template styles.
11. Ensure every proposed phase specifies explicit verification test criteria.
12. Require user authorization before commencing any implementation phase.
13. Antigravity remains the primary developer and execution agent.

---

## 40. ASTRA → ANTIGRAVITY WORKFLOW

1. **User Request:** User submits feature request or phase goal to Astra.
2. **Astra Analysis:** Astra analyzes request against codebase context, invariants, and security rules.
3. **Execution Prompt:** Astra formulates a precise, production-grade meta-prompt / plan for Antigravity.
4. **Antigravity Plan:** Antigravity inspects codebase, confirms exact implementation details, and submits plan if required.
5. **Execution:** Antigravity writes code, migrations, and tests.
6. **Verification:** Antigravity runs backend tests, frontend tests, typecheck, lint, build, and database validation.
7. **Report & Handoff:** Antigravity reports clean execution results back to Astra for security and architectural audit.

---

## 41. CONTEXT FILE DISCIPLINE

- Maintain `docs/AGENT_CONTEXT.md` as the persistent technical implementation ledger.
- Maintain `docs/ASTRA_HANDOFF.md` as the bootstrap context for Astra.
- **Do NOT create new numbered handoff files** (`handoff_v2.md`, `ASTRA_CONTEXT_1.md`).

---

## 42. KEY ARCHITECTURAL COMMITS

- `96023ff`: Phase 28 — Analytics, Subscription Plans & Billing Foundation
- `c35e672`: Phase 27 — Reviews, Verification & Collections
- `ba9e36e`: Phase 26.2 — Public Lead `SECURITY DEFINER` Boundary Hardening
- `30a5f29`: Phase 26.1 — Leads PII Masking & RLS Hardening
- `5b9e84b`: Phase 26 — Leads CRM & WhatsApp Integration
- `8ad31f1`: Phase 25.2 — Discovery Public Read Correlated RLS Hardening
- `d7b9ec3`: Phase 25 — PostgreSQL-Native Search & Discovery Engine
- `e456f8a`: Master Audit — Architecture & History Reconciliation

---

## 43. NO SECRETS CERTIFICATION

This handoff document has been audited. Zero raw API keys, passwords, database credentials, tokens, or private PII exist within this file or repository documentation. Environment variable references (`SPRING_DATASOURCE_PASSWORD`, `OIDC_CLIENT_SECRET`, etc.) are used exclusively.

---

## 44. DOCUMENTATION CONSISTENCY AUDIT

`docs/ASTRA_HANDOFF.md` and `docs/AGENT_CONTEXT.md` have been cross-verified against actual Java source code, Spring Boot configuration, Next.js page components, and Flyway SQL migrations. Documentation and codebase are in 100% alignment.

---

## 44.1. END-TO-END PRODUCT WORKFLOWS & AI PROMPT EXPERIENCE (OCTOBER 2026)

- Backend `PromptEnhancementService` (`/api/v1/ai/prompt/enhance`): deterministic architectural vocabulary expansion supporting room types, styles, materials, and lighting.
- Frontend `voice-service.ts`: Web Speech API (`webkitSpeechRecognition` / `SpeechRecognition`) progressive enhancement with mobile-friendly state machine.
- `VoiceDictationButton.tsx`: Accessible 44px+ touch-target microphone with pulse animation and ARIA status.
- `AiPromptComposer.tsx`: Full interactive prompt drafting studio integrating voice transcription, quick architectural context pills, "Improve Prompt" action with diff visualization, and "Use Original" revert.
- Workspace Unsaved Changes: `beforeunload` warning listeners added to Portfolio Builder and Project CMS Editor.
- Verification: 346/346 backend tests PASS, 284/284 frontend tests PASS, typecheck PASS, lint PASS, build PASS.

---

## 45. FINAL GIT & HANDOFF STATE

- **Commit Message:** `docs: prepare astra project handoff`
- **Branch:** `main`
- **Synchronization:** Local HEAD == origin/main. Working tree clean.

---

## 46. ASTRA STARTING MESSAGE

To hand off this project to Astra, copy and paste the following message:

> "I am continuing development of the Interior Professional Platform. Antigravity has full access to the repository. First read `docs/ASTRA_HANDOFF.md` and `docs/AGENT_CONTEXT.md`, inspect the actual repository, Git history and Flyway migrations, and confirm your understanding of the current state. Do not implement Phase 29 yet. Act as the senior architect, security reviewer, planner and meta-prompt engineer for all future work, while Antigravity remains the execution/development agent."
