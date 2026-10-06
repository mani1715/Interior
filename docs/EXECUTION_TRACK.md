# EXECUTION TRACK — ELÉGANCE INTERIOR DESIGNER PLATFORM

# CURRENT STATE

Application:
Elégance Interior Designer Platform

Repository:
C:\my projects\interior design

Branch:
main

Latest functional baseline:
ddbe990

Flyway:
V029

Production deployment:
DEFERRED

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

## Current Next Step:

- Platform review and next milestone planning.

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
