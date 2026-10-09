# EXECUTION TRACK — ELÉGANCE INTERIOR DESIGNER PLATFORM

# CURRENT STATE

Application:
Elégance Interior Designer Platform

Repository:
C:\my projects\interior design

Branch:
main

Latest functional baseline:
Release Candidate Consistency Gate Complete (READY FOR PHASE 8 CLOSED-BETA INFRASTRUCTURE SETUP)

Flyway:
V033

Phase 7 Hardening & Release Tracks:
- Phase 7A: Auth, Sessions, Tenant Switching & Studio Onboarding (PASS)
- Phase 7B: Project Lifecycle, Publishing Gates, Enquiries & CRM Workflow (PASS)
- Phase 7C: In-App Notifications & Authoritative Communication Delivery (PASS)
- Phase 7D: AI, Media Storage, Upload Lifecycle & Failure Recovery (PASS)
- Phase 7E: Admin, Moderation, Audit & Operational Controls (PASS)
- Phase 7F: Final Security, API, Concurrency & End-to-End Release Audit (PASS)
- Final Whole-Product QA & Release Candidate Consistency Gate: Visual, UX, Accessibility, Performance & Locked Invariant Source Verification (PASS - READY FOR PHASE 8 CLOSED-BETA INFRASTRUCTURE SETUP)

Production deployment:
DEFERRED (Ready for Phase 8 Closed Beta Infrastructure Setup)

---

# COMPLETED CORE PLATFORM

High-level delivered milestones across Phases 00–29 and hardening rounds:
- Public discovery (search & filtering by city, style, trade, room category)
- Professional onboarding (7-step onboarding wizard, studio creation)
- Project CMS (stories, readiness criteria, privacy safeguards)
- Media & storage engine (quarantined upload, WebP derivatives, watermarking)
- Six portfolio templates (BASIC, MODERN, LUXURY, ARCHITECTURAL, WARM_NATURAL, DARK_CINEMATIC)
- AI visualizer (full-image, precision canvas inpainting, 13 reference purposes, client approval bundles)
- Client collaboration & pinpoint annotations
- Leads CRM & WhatsApp handoff
- Client reviews (invited via 256-bit cryptographic tokens)
- Business verification & admin moderation
- User collections (private inspiration moodboards)
- Privacy-safe product analytics
- Account & workspace settings
- Studio notifications
- Team management
- Multi-studio switching & additional studio creation
- Role & invitation security closure
- Realtime SSE events stream
- PostgreSQL Row-Level Security (RLS) forced across all tenant tables

---

# PORTFOLIO EVOLUTION

## Completed:

- **Phase 0: Technical Audit**
  - Architecture and content hierarchy audit (`Studio → Project → Room / Space → Photos`).
- **Phase 1 Design: Owner Room + Photo UX (Astra)**
  - Spatial index layout, multi-upload flow, photo cards, photo inspector, dual covers, focal-point reticle modal.
- **Phase 1 Implementation: Room + Photo Foundation (Antigravity)**
  - Canonical content model: `Studio → Project → Room → Photos`
  - Flyway migration `V028__project_rooms_and_photo_presentation.sql`
  - Domain records, repository, and service: `project_rooms` with UUIDv7, composite tenant keys, safe room deletion (`ON DELETE SET NULL (room_id)`).
  - Media presentation attributes: `is_room_cover`, `focal_x`, `focal_y`, `motion_enabled`.
  - Room CRUD & reorder REST APIs under `/api/v1/projects/{projectId}/rooms`.
  - Owner organizer UI: `ProjectRoomManager`, spatial index sidebar/pills, multi-upload queue (up to 50 files, max 3 concurrent), "Project Photos" unassigned group.
  - Photo Inspector slide-over sheet and interactive `FocalPointModal`.
  - Verified: 405/405 backend tests, 320/320 frontend tests, 28/28 PostgreSQL RLS tests.

- **Phase 2 Implementation: Public Room Gallery & Full-Screen Photo Viewer (Antigravity)**
  - Backend Public Room Projection: `PublicRoomDto`, extended `PublicMediaDto` and `PublicProjectDetailDto` in `SeoService`.
  - Frontend Unified Normalization: `normalizeProjectSpaceShowcase` mapping room groupings, additional views, dimensions, and focal points.
  - Public Room Gallery: `ProjectSpaceShowcase` with sticky room navigation bar, photo count badges, multi-photo responsive story grids, and no-JS server fallback.
  - Full-Screen Viewer: `GalleryViewer` evolved with room-scoped boundary enforcement, zoom in/out/fit (up to 3x), pan dragging and clamp, contact sheet (This Room vs All Rooms), and enquiry handoff to `EnquirySheet`.
  - Verified: 406/406 backend tests PASS, 324/324 frontend Vitest tests PASS, TypeScript typecheck PASS, ESLint PASS, Next.js production build PASS.

- **Phase 3 Implementation: Cinematic Portfolio (Antigravity)**
  - Schema & Persistence: Flyway migration `V029__project_presentation_mode.sql` adding `presentation_mode varchar(32) NOT NULL DEFAULT 'STANDARD'` with CHECK constraint (`STANDARD`, `CINEMATIC`) and index.
  - Domain records & services: Updated `StudioProjectRecord`, `CreateProjectRequest`, `UpdateProjectRequest`, `ProjectDetailResponse`, `JdbcProjectRepository`, `ProjectService`, `SeoService`, `PublicProjectDetailDto` with default-safe `ProjectPresentationMode`.
  - Frontend Type Foundations: Added `ProjectPresentationMode` across `projects/types.ts`, `seo/types.ts`, `showcase-types.ts`, and `showcase-normalizer.ts`.
  - Owner Workspace Presentation Settings: Added "Presentation Mode" switcher in Project Editor (`/workspace/projects/[projectId]`) with Standard and Cinematic cards, adaptive eligibility notes, and live preview links.
  - Eligibility Engine: Deterministic `evaluateCinematicEligibility` and `evaluateProjectCinematicEligibility` enforcing viewport threshold ($\ge 1280 \times 800$), aspect ratio branches (1.2–2.2 Focus Push, 2.2–3.0 Gentle Drift), focal safe zone $[0.15, 0.85]$, per-photo `motion_enabled`, template scale caps (up to 1.04), and max 3 sticky chapters per project.
  - Standard Integration Fixes:
    1. Single-photo portrait containment: `.singlePortrait` constraint (`max-width: 680px`, `object-fit: contain`) preventing harsh cropping or viewport dominance.
    2. Real room anchors: HTML anchor navigation (`<a href={`#room-${room.id}`}>`) with intersection observer tracking, native browser back/forward history, and deep-linking support without scroll hijacking.
    3. Transform ownership: Moving figures tagged with `data-cinematic-controlled="true"` decouple from template transforms, hover zoom, and `PortfolioMotion`.
  - Progressive Enhancement: Sticky pinned chapter (`CinematicChapter`) with scroll-bound linear transforms and pause-during-viewer; automatic fallback to Standard for mobile, tablet, reduced-motion, or non-qualifying spaces.
  - Verified: 406/406 backend tests PASS, 347/347 frontend Vitest tests PASS, TypeScript typecheck PASS, ESLint PASS, Next.js production build PASS.

- **Phase 4 Implementation: Homepage Refinement (Antigravity)**
  - Reordered and refined homepage to prioritize visitor discovery first across 11 approved sections:
    1. Public Header (`PublicHeader` with 76px desktop, 64px mobile bar, direct `/projects` mobile shortcut, and focus-trapped drawer)
    2. Brand Hero (`BrandHero` with single `<h1>Find your kind of space.<br /><em>Meet its creators.</em></h1>`, clear CTAs `/projects` and `/professionals`, 180svh desktop motion constraint, and static editorial layout for tablet/short-desktop/mobile)
    3. Immediate Project Search (prominent semantic search form with `/projects?q=` and quick category chips)
    4. Projects to Explore (`EditorialProjects` with heading "Projects to explore", live `fetchDiscoveryProjects({ limit: 3 })`, studio attribution, AI badge, and empty/error states)
    5. Professional Discovery (`EditorialProfessionals` with heading "Find the people for your space.", live `fetchDiscoveryProfessionals({ limit: 3 })`, and editorial visual fallback)
    6. Cinematic Portfolio Introduction (progressive enhancement explanation, room card preview with badge, and CTA)
    7. Inspiration / Categories (3:2 visual category cards, additional room links, and quiet AI concept credit)
    8. AI Visualizer (secondary supporting tool role, 3-step room workflow, truthful legal disclaimer, and `/workspace/ai` CTA noting sign-in requirement)
    9. Trust Explanations (3 confidence pillars: Work with attribution, Reviews with context, Verification explained)
    10. Visitor + Professional Calls to Action (dual audience layout: homeowner discovery and professional portfolio registration)
    11. Footer (`Footer` with platform navigation, legal links, and quiet copyright)
- **Phase 7A: Authentication, Session Lifecycle, Role Routing & Workspace Access (Antigravity)**
  - Backend Opaque Session Security: 256-bit entropy opaque sessions hashed with SHA-256 in `identity_sessions`, idle (2hr) and absolute (7d) expiration, `__Host-session` cookies (`Path=/; HttpOnly; SameSite=Lax; Secure`), and `Cache-Control: no-store, private` headers on authenticated responses.
  - Authoritative Current User (`GET /auth/me`): Accurate status resolution (`ACTIVE`, `PENDING`, `SUSPENDED`, `DELETED`) propagated into `ActorContext`. Suspended accounts are immediately denied across `AuthorizationService` (`requireActiveUser`) and presented with a dedicated support notice in `WorkspaceShell`.
  - Deterministic Role-Aware Routing: `resolveAuthDestination` routes `SUPER_ADMIN`/`ADMIN` to `/admin`, active studio members to `/workspace`, incomplete designers to `/onboarding/professional`, and customers to `/account`.
  - Open Redirect Defense: `sanitizeRedirectUrl` strictly denies external schemes, protocol-relative (`//`), backslashes (`\`, `/\`, `\\`), encoded bypasses (`%2f`, `%5c`), CRLF injection, and script schemes (`javascript:`, `data:`).
  - Multi-Studio Tenancy: Per-tab studio isolation via `sessionStorage` (`elegance_active_studio_id`) and `X-Studio-Id` header, validated by backend `TenantResolutionFilter`.
  - Professional Onboarding & Team Invites: Transactional studio provisioning (`DESIGNER_ADMIN`), idempotency guarantees, and single-use cryptographic invitation tokens with email matching checks.
- **Phase 7B: Project Lifecycle, Publishing Gates, Enquiries & Client Workflow (Antigravity)**
  - Project State & Readiness Engine: Dedicated publish gate (`checkPublishability`, `publishProject`, `unpublishProject`), enforcing readiness criteria (`title`, `categoryCode`, `shortDescription`, `city`, `state`), active studio membership invariants, and non-archived state.
  - Publish & Unpublish Invariants: Optimistic concurrency control (`version`), explicit unpublishing (`POST /{projectId}/unpublish`), idempotent publish operations preventing duplicate audit entries, and soft-archive semantics preserving room/photo/lead integrity.
  - Hardened Portfolio Visibility: `findPortfolioProjects` query strictly enforces `project_status = 'READY' AND visibility_status = 'PORTFOLIO' AND archived_at IS NULL`.
  - Anti-Abuse Public Enquiry Intake: `PublicLeadService` honeypot fields, tiered IP rate limits (5 per 15 min), phone flood controls (3 per hr), and cross-studio project attribution verification.
  - CRM Lead Lifecycle & Assignment: Strict state machine (`NEW` -> `CONTACTED` -> `QUALIFIED` -> `SITE_VISIT_PLANNED` -> `IN_DISCUSSION` -> `WON` / `LOST` / `ARCHIVED`), verified studio member assignment enforcement, and studio-isolated project linking in `LeadService.updateLead`.
  - Client Review Verification Gate: Verified client review invitations unlocked strictly when CRM lead achieves status `WON` (`ReviewInvitationService`), protected by single-use 30-day cryptographic tokens.
  - Multi-Tenant RLS & Security: 438/438 full backend tests PASS, 20/20 PostgreSQL RLS tests PASS, 368/368 frontend Vitest tests PASS, TypeScript typecheck PASS, ESLint PASS, Next.js production build PASS.

- **Phase 7C: CRM, Notifications & Communication Delivery Completion (Antigravity)**
  - Authoritative In-App Notification Engine: Single unified model in `notifications` table, real-time SSE stream (`/api/v1/workspace/notifications/stream`) with keepalive heartbeats, unread counting, batch mark-all-read, bounded pagination limits, and self-action spam suppression.
  - Expanded Notification Domain: Flyway V031 adding `NEW_LEAD`, `LEAD_ASSIGNED`, `CLIENT_FEEDBACK_RECEIVED`, `CLIENT_APPROVED_CONCEPT`, `CLIENT_REQUESTED_CHANGES`, `TEAM_INVITATION`, `REVIEW_INVITATION_READY`, and `PROJECT_PUBLISH_STATE_CHANGED`.
  - External Communication Delivery Tracking: New `communication_deliveries` table protected by PostgreSQL RLS with `FORCE ROW LEVEL SECURITY`. Bounded retries, idempotency key checks, and delivery statuses (`PENDING`, `SENT`, `FAILED`, `NOT_CONFIGURED`, `DELIVERED`).
  - Fail-Safe Transaction Boundaries: External provider exceptions (SMTP/SES) never abort or roll back primary database transactions; failures are cleanly captured in `communication_deliveries`.
  - Security-Hardened Email Templates: `EmailTemplateService` with strict XSS escaping via `HtmlUtils.htmlEscape` and host-header injection mitigation using canonical `app.baseUrl`.
  - Truthful Provider Architecture: In-app is fully operational; transactional email correctly reports `NOT_CONFIGURED` with `DisabledEmailProvider`; WhatsApp supports client-initiated `Mode A (Direct wa.me)`.
  - Comprehensive Verification: 453/453 backend tests PASS (0 failures, 0 skipped), 23/23 PostgreSQL RLS tests PASS (0 failures, 0 skipped), 368/368 frontend Vitest tests PASS, TypeScript typecheck PASS, ESLint PASS, Next.js production build PASS.

- **Phase 7D: AI, Media Storage, Upload Lifecycle & Failure Recovery Completion (Antigravity)**
  - Two-Phase Upload Intent Lifecycle: Strict state machine (`PENDING`, `UPLOADED`, `COMMITTING`, `COMMITTED`, `CANCELLED`, `FAILED`, `EXPIRED`) with client cancellation (`POST /media/upload-intent/{id}/cancel`) and double-commit idempotency returning existing media asset.
  - Multi-Dimensional Storage & Photo Quota Enforcement: Hard checks on committed + pending storage bytes and portfolio photo counts across studio subscription tiers. Quota reservation on upload intent creation and release on cancellation/expiry.
  - Slot-Preserving Photo Replacement: Dedicated `POST /api/v1/workspace/media/{mediaId}/replace` endpoint that preserves room association, cover badges, captions, focal reticles, and motion settings while only validating net storage byte increases without consuming additional photo slots.
  - Adversarial Media & Upload Hardening: SSRF defense service (`SsrfProtectionService`) blocking loopbacks, RFC1918, link-local, AWS/cloud metadata (`169.254.169.254`), and non-HTTP schemes with redirect checks. Decompression bomb protection in `ImageProcessingService` capping max dimension at 10,000px and total pixels at 50MP.
  - AI Pipeline Decoupling & Recovery: Asynchronous AI generation jobs protected with timeout recovery (`reconcileStuckProcessingJobs`) resolving stuck jobs to `FAILED` with `PROCESSING_TIMEOUT`. Clean provenance enforcement (`AI_CONCEPT` visibility and mandatory badge watermarks upon portfolio enrollment).
  - Multi-Tenant RLS & Storage Reconciliation: Automated reconciliation (`POST /api/v1/workspace/media/reconcile` and `POST /api/v1/workspace/ai/jobs/reconcile`), Flyway `V032__media_lifecycle_and_upload_hardening.sql`, and PostgreSQL RLS across all tables (`media_upload_intents`, `media_assets`, `ai_generation_jobs`).
  - Comprehensive Verification: 468/468 backend tests PASS (0 failures, 0 errors, 0 skipped), 17/17 PostgreSQL RLS tests PASS (0 failures, 0 skipped), 368/368 frontend Vitest tests PASS, TypeScript typecheck PASS, ESLint PASS.

## Current Next Step:

- Backend Core Phase 7E — Admin, Moderation, Audit & Operational Controls Completion.

---

# OUT OF SCOPE CURRENTLY

- True 3D room reconstruction
- Guided camera capture
- 360 tours
- Photogrammetry / NeRF / Gaussian splats
- WebGL room reconstruction
- Deployment
- Live payments / commercial checkout
- Production external provider configuration
