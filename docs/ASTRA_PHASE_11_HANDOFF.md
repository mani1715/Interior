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
- `templateKey: 'BASIC'`
- `headline?: string | null`
- `subheadline?: string | null`
- `bio?: string | null`
- `designPhilosophy?: string | null`
- `yearsOfExperience?: number | null`
- `primaryColor?: string | null`
- `secondaryColor?: string | null`
- `accentColor?: string | null`
- `fontPairing?: FontPairing`
- `publicContacts: PreviewContactDto[]` (`kind`, `contactValue`)
- `canonicalServices: PreviewServiceDto[]` (`serviceCode`, `serviceName`)
- `canonicalSpecialties: PreviewSpecialtyDto[]` (`specialtyCode`, `specialtyName`)
- `canonicalServiceAreas: PreviewServiceAreaDto[]` (`cityName`, `locality`)
- `visibleSections: PreviewSectionDto[]` (`sectionId`, `sectionType`, `displayOrder`, `schemaVersion`, `content`)
- `isMobilePreview?: boolean`

## 12. Supported Sections (Render in `displayOrder`)
1. `HERO`
2. `ABOUT`
3. `SERVICES`
4. `FEATURED_PROJECTS`
5. `PROJECT_GALLERY`
6. `BEFORE_AFTER`
7. `DESIGN_PHILOSOPHY`
8. `PROCESS`
9. `TESTIMONIALS`
10. `PRESS`
11. `AWARDS`
12. `TEAM`
13. `FAQ`
14. `CONTACT_FORM`
15. `LOCATION_MAP`
16. `INSTAGRAM_FEED`
17. `CONSULTATION_CTA`
18. `FOOTER`

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
# 1. Run all frontend unit tests (Baseline: 89 passed)
npm run test

# 2. TypeScript typecheck
npm run typecheck

# 3. ESLint
npm run lint

# 4. Next.js production build
npm run build
```

## 16. Git Rule
- **Local commits ONLY**: e.g., `git commit -m "phase-11: implement basic portfolio template"`
- **DO NOT PUSH TO REMOTE**: Standing platform invariant.

## 17. Phase 11 Completion Boundary
Implement and polish **BASIC** until all quality gates pass.
Stop and report before touching **MODERN** (Phase 12).
