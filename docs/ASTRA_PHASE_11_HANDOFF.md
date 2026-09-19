# ASTRA PHASE 11 HANDOFF

## 1. Current State
Phase 10 (Portfolio Builder Engine) is **COMPLETE**.
The shared engine, database persistence, optimistic locking, section sanitization, interactive builder (`/workspace/portfolio`), and private preview (`/workspace/portfolio/preview`) are fully operational.

## 2. Target
Implement the **BASIC** visual portfolio design (`Basic Clean Editorial`) **ONLY**.
Do NOT start Phase 12 (`MODERN`).

## 3. Start Here: Required Reading
Before writing code, Astra must inspect:
1. `docs/AGENT_CONTEXT.md` — Permanent cross-agent architectural context and phase status.
2. `docs/PORTFOLIO_TEMPLATE_CONTRACT.md` — Complete template interface, invariants, and quality rules.
3. `apps/web/src/lib/portfolio/template-contract.ts` — React props TypeScript interface.
4. `apps/web/src/lib/portfolio/template-registry.tsx` — Template registry mapping template keys to components.

## 4. Template Contract Reference
Read [`docs/PORTFOLIO_TEMPLATE_CONTRACT.md`](PORTFOLIO_TEMPLATE_CONTRACT.md).
Key Invariant: **`CONTENT != TEMPLATE`**. The template must render all data passed through `PortfolioTemplateProps` cleanly without expecting custom server endpoints or database modifications.

## 5. BASIC Directory
Create and implement the Basic template in:
`apps/web/src/components/portfolio/templates/basic/`
- Entry component: `apps/web/src/components/portfolio/templates/basic/BasicTemplate.tsx`
- Subcomponents/helpers: `apps/web/src/components/portfolio/templates/basic/components/`

## 6. Engine-Owned Files (DO NOT MODIFY)
Astra must **NOT** casually modify or rewrite:
- `apps/api/**` (Backend Java, Spring Boot, Flyway SQL migrations)
- `apps/web/src/lib/portfolio/template-contract.ts` (Contract interface)
- `apps/web/src/lib/portfolio/api.ts` (Backend API fetch client)
- `apps/web/src/lib/portfolio/types.ts` (Core portfolio domain DTOs)
- `apps/web/src/app/workspace/portfolio/page.tsx` (Workspace builder UI)
- `apps/web/src/app/workspace/portfolio/preview/page.tsx` (Private preview route)
- `apps/web/src/components/workspace/**` (Workspace shell and navigation)
- `apps/web/src/lib/auth/**` (Security and session management)

## 7. Astra-Owned Files (Allowed in Phase 11)
Astra has full creative and implementation authority over:
- `apps/web/src/components/portfolio/templates/basic/**` (Template markup, styling, components)
- `apps/web/src/components/__tests__/BasicTemplate.test.tsx` (Template unit tests)
- Updating `apps/web/src/lib/portfolio/template-registry.tsx` strictly to register `BasicTemplate` and update `status: 'AVAILABLE'`.

## 8. Current Template Status
In `apps/web/src/lib/portfolio/template-registry.tsx`:
```typescript
BASIC: {
  status: 'SCAFFOLD'  // <-- Current state
}
```

## 9. What BASIC Must Become
After implementing the real visual design and passing all quality checks:
```typescript
BASIC: {
  status: 'AVAILABLE',
  component: BasicTemplate  // <-- Graduated state
}
```

## 10. ReferenceTemplate Warning
`apps/web/src/components/portfolio/templates/ReferenceTemplate.tsx` is an **ENGINE STRUCTURAL REFERENCE & TEST FIXTURE ONLY**.
It is **NOT** an aesthetic design starting point to be lightly restyled. Astra should design and craft a bespoke, typography-driven editorial experience for `BasicTemplate`.

## 11. Data Available Through `PortfolioTemplateProps`
Astra will receive the following normalized props (defined in `template-contract.ts`):
- `portfolioId: string`
- `studioId: string`
- `studioName: string`
- `studioSlug: string`
- `professionalType?: string`
- `professionalTitle?: string`
- `studioCity?: string`
- `studioState?: string`
- `templateKey: PortfolioTemplateKey` (accepts all six keys)
- `headline?: string | null`
- `subheadline?: string | null`
- `bio?: string | null`
- `designPhilosophy?: string | null`
- `yearsOfExperience?: number | null`
- `primaryColor?: string | null`
- `secondaryColor?: string | null`
- `accentColor?: string | null`
- `fontPairing?: FontPairing` (`SYSTEM_SANS`, `CLASSIC_SERIF`, `MODERN_CLEAN`, `EDITORIAL`, `WARM_EDITORIAL`, `BOLD_CINEMATIC`)
- `navigationSettings: NavigationSettings` (`sticky`, `navItems: [{ sectionId, sectionType, label, anchor }]`, `showPrimaryCta`, `primaryCtaLabel`, `primaryCtaAnchor`)
- `publicContacts: PreviewContactDto[]` (`kind`, `contactValue`)
- `canonicalServices: PreviewServiceDto[]` (`serviceCode`, `serviceName`)
- `canonicalSpecialties: PreviewSpecialtyDto[]` (`specialtyCode`, `specialtyName`)
- `canonicalServiceAreas: PreviewServiceAreaDto[]` (`cityName`, `locality`)
- `visibleSections: PreviewSectionDto[]` (`sectionId`, `sectionType`, `displayOrder`, `schemaVersion`, `content`)
- `isMobilePreview?: boolean`

## 12. Supported Sections (Render in `displayOrder`)
The engine and database schema (V006) strictly define 18 canonical sections:
1. `HERO`: Headline, subheadline, CTA (`headlineOverride`, `subheadlineOverride`, `badgeText`, `ctaText`, `ctaLink`)
2. `ABOUT`: Studio bio, experience (`narrativeOverride`, `philosophyOverride`, `yearsOverride`, `signatureUrl`)
3. `SERVICES`: Services offered (`sectionHeadline`, `sectionSubheadline`, `customOfferings`)
4. `FEATURED_PROJECTS`: Curated case studies (`sectionHeadline`, `projectIds`, `layoutStyle`)
5. `PROJECT_GRID`: Project gallery grid (`sectionHeadline`, `columns`, `filterEnabled`)
6. `BEFORE_AFTER`: Architectural transformations (`sectionHeadline`, `pairs: [{ beforeImageUrl, afterImageUrl, title, caption }]`)
7. `BEFORE_AI_REALITY`: AI concept vs built reality (`sectionHeadline`, `pairs: [{ conceptImageUrl, realityImageUrl, title }]`)
8. `DESIGN_PROCESS`: Step-by-step methodology (`sectionHeadline`, `steps: [{ stepNumber, title, description }]`)
9. `TESTIMONIALS`: Client reviews (`sectionHeadline`, `testimonials: [{ quote, clientName, projectLocation, rating }]`)
10. `TEAM`: Studio associates and architects (`sectionHeadline`, `members: [{ name, role, photoUrl, bio }]`)
11. `AWARDS`: Industry accolades (`sectionHeadline`, `items: [{ title, issuer, year }]`)
12. `PRESS`: Media publications (`sectionHeadline`, `articles: [{ publication, title, url, publishDate }]`)
13. `SERVICE_AREAS`: Geographic areas (`sectionHeadline`, `introText`)
14. `FAQ`: Client FAQs (`sectionHeadline`, `questions: [{ question, answer }]`)
15. `CONTACT`: Studio inquiries (`contactIntro`, `formEnabled`, `buttonText`)
16. `CTA`: Action banner (`headline`, `subheadline`, `buttonText`, `buttonLink`)
17. `VIDEO`: Studio film reel (`videoUrl`, `title`, `caption`)
18. `CUSTOM_NOTE`: Editorial note (`title`, `body`)

## 13. Known Current Limitations (Respect Scope)
- **No Project CMS yet (Phase 18)**: If `FEATURED_PROJECTS` has 0 projects, show a graceful editorial empty state ("Projects in curation"). Do NOT invent fake mock projects.
- **No Media Engine yet (Phase 19)**: Image URLs may be null or placeholders. Handle missing imagery gracefully.
- **No AI Visualizer yet (Phase 24)**: Do not call AI generation endpoints.
- **No Public Publish Route yet**: Live publishing remains disabled.
- **No Fake Reviews**: If testimonials are empty, collapse or show a clean notice.

## 14. Design & Engineering Constraints
- **Mobile First**: Design for 360px–430px base viewport, scaling smoothly to desktop.
- **Support `isMobilePreview`**: When `isMobilePreview === true`, render within 375px frame without overflow.
- **SSR Compatible**: Clean hydration, no `window`/`document` usage on initial render.
- **Semantic HTML**: `<header>`, `<main>`, `<section>`, `<footer>`, single `<h1>` in Hero, sequential heading levels (`<h2>`, `<h3>`).
- **WCAG 2.2 AA**: Minimum 4.5:1 text contrast ratio, visible focus indicators.
- **Reduced Motion**: Respect `@media (prefers-reduced-motion: reduce)`.
- **No Heavy Client Libraries**: No Three.js, GSAP, or Framer Motion. Use Tailwind CSS and SVG icons from `lucide-react`.

## 15. Testing & Verification Commands (Baseline Must Not Regress)
Run in `apps/web`:
```powershell
# 1. Run all frontend unit tests (Current baseline: 94 passed across 12 files)
npm run test

# 2. TypeScript typecheck
npm run typecheck

# 3. ESLint
npm run lint

# 4. Next.js production build
npm run build
```

## 16. Git Rule
- The user's repository intake instruction supersedes the previous local-only rule. After Phase 11 is completed and verified, its final commit MUST be pushed to `https://github.com/mani1715/Interior.git`.
- The intake task itself does not authorize starting Phase 11 or creating/pushing a Phase 11 commit.

## 17. Phase 11 Completion Boundary
Implement and polish **BASIC** until all quality gates pass.
Stop and report before touching **MODERN** (Phase 12).

## 18. Repository Intake Verification — 2026-09-19 (Historical Intake Notes)

**Phase 11 readiness: NOT READY.** The historical Phase 10 PASS and the section list above are not proof of frontend/backend contract compatibility. No design or engine source changes were made during intake.

- Local `main` and fetched `origin/main` both point to `610e2808f5de1d5f0313692fa27870d600e3d5e3` (`docs: prepare astra phase-11 handoff`), with zero commits ahead/behind. Phase 10 commit `b3b70ad602ce3245ae02f2694ae6e492be6f7289` exists locally and on `origin/main`. Origin is `https://github.com/mani1715/Interior.git`. Fetch succeeded; no pull, merge, reset, or rebase was performed.
- Fresh baseline: backend 118 passed, frontend 89 passed across 11 files; frontend typecheck, lint, and production build passed. Backend tests use H2; this does not verify deployment against PostgreSQL or its RLS policies.
- Initialization mismatch: `apps/web/src/lib/portfolio/api.ts` posts to `/portfolio/init`; `PortfolioController` maps initialization to POST `/portfolio` (under the API context path).
- Mutation mismatch: frontend section updates, reordering, and snapshot creation omit the backend-required `version`. Reordering sends `orderedSectionIds`, while the backend requires `sectionIds`.
- Enum mismatch: frontend font pairings and backend `FontPairing` values do not overlap. Frontend section types also differ from backend `SectionType` and V006 constraints. No normalization adapter reconciles these contracts.
- Actual backend sections: `HERO`, `ABOUT`, `SERVICES`, `FEATURED_PROJECTS`, `PROJECT_GRID`, `BEFORE_AFTER`, `BEFORE_AI_REALITY`, `DESIGN_PROCESS`, `TESTIMONIALS`, `TEAM`, `AWARDS`, `PRESS`, `SERVICE_AREAS`, `FAQ`, `CONTACT`, `CTA`, `VIDEO`, `CUSTOM_NOTE`.
- The frontend section list is the list in section 12 above. Against the user's requested list, the backend lacks `STATS`, `PROCESS`, and `SOCIALS`, and instead includes `DESIGN_PROCESS`, `PRESS`, and `CUSTOM_NOTE`.
- Section content names also need reconciliation: for example, the backend HERO validator accepts `headlineOverride`/`subheadlineOverride`, while the reference renderer reads `title`/`subtitle`.
- `PortfolioTemplateProps` has no dedicated navigation settings, template config, or typed project/media references. Arbitrary section content must not be treated as an established asset contract.
- All six registry entries remain `SCAFFOLD`. The private preview is client-fetched; an SSR-compatible reference renderer does not establish a working public SSR publishing route. Responsive and accessibility conformance have not been demonstrated for a finished BASIC design.

Report these engine-owned blockers for resolution before starting BASIC. Do not fix them inside the template ownership boundary or change template status during intake.

## 19. Phase 10.1 Contract Alignment Resolution — 2026-09-19

**Phase 11 Readiness: READY TO PROCEED.**
Antigravity executed Phase 10.1 to resolve all engine blockers identified during Astra's intake:

1. **Initialization Route Aligned**:
   - `apps/web/src/lib/portfolio/api.ts` updated from `POST /portfolio/init` to `POST /portfolio` matching `PortfolioController.java`.
2. **Optimistic Locking & Request Shapes Aligned**:
   - `UpdateSectionRequest` requires `version: number`.
   - `ReorderSectionsRequest` requires `sectionIds: string[]` and `version: number`.
   - `CreateVersionSnapshotRequest` requires `version: number`.
   - `RestoreVersionRequest` requires `version: number`.
   - Sequential updates and 409 conflict detection tested and verified.
3. **Canonical Enums Aligned**:
   - Frontend and backend agree on the exact 18 canonical `SectionType` values and 6 `FontPairing` values.
4. **Section Content Contract Aligned**:
   - Typed content interfaces (`HeroSectionContent`, `AboutSectionContent`, etc.) aligned to `PortfolioSectionValidator.java`.
   - `ReferenceTemplate.tsx` updated to use canonical field names (`headlineOverride`, `subheadlineOverride`, `narrativeOverride`, `sectionHeadline`, `contactIntro`, etc.).
5. **Centralized Presentation Normalizer**:
   - `apps/web/src/lib/portfolio/normalize-props.ts` introduced `normalizePortfolioProps()`.
   - Safe, non-null `NavigationSettings` (`sticky`, `navItems`, `showPrimaryCta`, `primaryCtaLabel`, `primaryCtaAnchor`) automatically generated from visible sections.
6. **Template Versioning & Selectability**:
   - All 6 templates in `TEMPLATE_REGISTRY` configured with `version: '1.0.0'`, `status: 'SCAFFOLD'`, `isSelectable: false`, and `supportedSections: CANONICAL_SECTION_TYPES`.
   - Backend `PortfolioTemplateKey` exposes template version `1.0.0`.
7. **Verification Baselines Verified**:
   - Backend: 125 tests passed across 15 test classes (0 failures).
   - Frontend: 94 tests passed across 12 test files (0 failures), including dedicated `PortfolioContract.test.ts`.
   - TypeScript: 0 errors.
   - ESLint: 0 errors.
   - Production Build: Next.js Turbopack build succeeded.

