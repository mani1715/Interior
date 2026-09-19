# Portfolio Template Contract & Engine Specification

This specification defines the formal contract between the shared **Portfolio Engine** (built in Phase 10) and the **Visual Portfolio Themes** to be created by Astra in subsequent phases:
- **Phase 11**: Basic Clean Editorial (`BASIC`)
- **Phase 12**: Modern Minimalist (`MODERN`)
- **Phase 13**: Luxury Atelier (`LUXURY`)
- **Phase 14**: Architectural Monograph (`ARCHITECTURAL`)
- **Phase 15**: Warm & Natural Organic (`WARM_NATURAL`)
- **Phase 16**: Dark Cinematic Moodboard (`DARK_CINEMATIC`)
- **Phase 17**: Antigravity Integration, Theme Engine Consistency, Responsive QA & Regression

---

## 1. Fundamental Invariant: Content != Template

The platform strictly decouples **persisted portfolio domain content and structure** from **visual presentation templates**:

1. An interior designer or studio can switch between any of the six aesthetic themes at any time from the workspace builder.
2. Switching themes updates only `portfolios.template_key`.
3. **No section data, text, configuration, or display order is destroyed, mutated, or lost during a template switch.**
4. When a designer switches from `BASIC` to `LUXURY` and then back to `BASIC`, the entire content tree remains intact and identical.
5. Content is persisted in normalized relational tables (`portfolios`, `portfolio_sections`) and delivered to the template as a clean, immutable props object.

---

## 2. Component Interface: `PortfolioTemplateProps`

All theme components implemented by Astra must strictly implement this React contract (defined in `apps/web/src/lib/portfolio/template-contract.ts`):

```typescript
export interface PortfolioTemplateProps {
  portfolioId: string;
  studioId: string;
  studioName: string;
  studioSlug: string;
  professionalType?: string;
  professionalTitle?: string;
  studioCity?: string;
  studioState?: string;
  templateKey: PortfolioTemplateKey;
  headline?: string | null;
  subheadline?: string | null;
  bio?: string | null;
  designPhilosophy?: string | null;
  yearsOfExperience?: number | null;
  primaryColor?: string | null;
  secondaryColor?: string | null;
  accentColor?: string | null;
  fontPairing?: FontPairing;
  publicContacts: PreviewContactDto[];
  canonicalServices: PreviewServiceDto[];
  canonicalSpecialties: PreviewSpecialtyDto[];
  canonicalServiceAreas: PreviewServiceAreaDto[];
  visibleSections: PreviewSectionDto[];
  isMobilePreview?: boolean;
}
```

### Supporting DTOs
```typescript
export interface PreviewContactDto {
  kind: string; // 'EMAIL' | 'PHONE' | 'WHATSAPP' | 'WEBSITE' | 'INSTAGRAM'
  contactValue: string;
}

export interface PreviewServiceDto {
  serviceCode: string;
  serviceName: string;
}

export interface PreviewSpecialtyDto {
  specialtyCode: string;
  specialtyName: string;
}

export interface PreviewServiceAreaDto {
  cityName: string;
  locality: string;
}

export interface PreviewSectionDto {
  sectionId: string;
  sectionType: SectionType;
  displayOrder: number;
  schemaVersion: number;
  content: Record<string, any>;
}
```

---

## 3. Template Directory Structure & Paths

Templates reside under `apps/web/src/components/portfolio/templates/`:

```
apps/web/src/components/portfolio/templates/
├── ReferenceTemplate.tsx          # Neutral reference & test scaffold ONLY
├── basic/                        # Phase 11: Astra Basic Clean Editorial
│   ├── BasicTemplate.tsx         # Primary template component
│   └── components/               # Template-specific subcomponents
├── modern/                       # Phase 12: Astra Modern Minimalist
├── luxury/                       # Phase 13: Astra Luxury Atelier
├── architectural/                # Phase 14: Astra Architectural Monograph
├── warm-natural/                 # Phase 15: Astra Warm & Natural
└── dark-cinematic/               # Phase 16: Astra Dark Cinematic
```

- **Shared Registry**: `apps/web/src/lib/portfolio/template-registry.tsx`
- **Template Contract Definition**: `apps/web/src/lib/portfolio/template-contract.ts`
- **Portfolio Domain Types**: `apps/web/src/lib/portfolio/types.ts`

---

## 4. The Six Canonical Template Keys & Lifecycles

| Template Key | Visual Identity & Direction | Target Phase | Implementation Status |
|---|---|---|---|
| `BASIC` | Clean editorial typography, balanced whitespace, clear information hierarchy | Phase 11 | `SCAFFOLD` → `AVAILABLE` |
| `MODERN` | Asymmetrical grid, high contrast, sans-serif dominance, architectural edge | Phase 12 | `SCAFFOLD` |
| `LUXURY` | Opulent serifs, warm gold/bronze accents, spacious pacing, magazine aesthetic | Phase 13 | `SCAFFOLD` |
| `ARCHITECTURAL` | Structured drafting grid, technical precision, geometric discipline | Phase 14 | `SCAFFOLD` |
| `WARM_NATURAL` | Earthy terracotta/sand tones, biophilic textures, organic curves, tactile warmth | Phase 15 | `SCAFFOLD` |
| `DARK_CINEMATIC` | Deep obsidian backdrop, dramatic lighting focal points, gallery mood | Phase 16 | `SCAFFOLD` |

### Graduating a Template: `SCAFFOLD` → `AVAILABLE`
All templates initially have `status: 'SCAFFOLD'`. When Astra finishes a template in its assigned phase and passes all verification gates:
1. Astra creates the template component in its respective directory.
2. In `apps/web/src/lib/portfolio/template-registry.tsx`, Astra imports the component and updates:
   ```typescript
   BASIC: {
     key: 'BASIC',
     name: 'Clean Editorial',
     tagline: 'Simple, typography-driven portfolio for emerging designers',
     description: '...',
     status: 'AVAILABLE', // <-- Changed from 'SCAFFOLD'
     designer: 'Astra (Phase 11)',
     phase: 'Phase 11',
     component: BasicTemplate, // <-- Real template component
   }
   ```
3. `ReferenceTemplate.tsx` is **ENGINE TEST/REFERENCE ONLY**. It is **NOT** the final Basic design. Astra must create the true editorial Basic design in `basic/BasicTemplate.tsx`.

---

## 5. The 18 Canonical Section Types

The engine supports 18 modular section types:
1. `HERO`: Studio headline, subheadline, primary CTA.
2. `ABOUT`: Studio bio, years in practice, design leadership.
3. `SERVICES`: Canonical trade services offered.
4. `FEATURED_PROJECTS`: Curated case studies (gracefully tolerates 0 projects).
5. `PROJECT_GALLERY`: Secondary project visual grid.
6. `BEFORE_AFTER`: Architectural transformation comparisons.
7. `DESIGN_PHILOSOPHY`: Spatial philosophy and aesthetic statement.
8. `PROCESS`: Step-by-step design/turnkey methodology.
9. `TESTIMONIALS`: Client testimonials (gracefully tolerates 0 reviews).
10. `PRESS`: Editorial features and media mentions.
11. `AWARDS`: Industry accolades and honors.
12. `TEAM`: Studio associates and lead architects.
13. `FAQ`: Frequently asked client questions.
14. `CONTACT_FORM`: Inquiries and studio contact channels.
15. `LOCATION_MAP`: Service regions and studio address.
16. `INSTAGRAM_FEED`: Visual social preview placeholder.
17. `CONSULTATION_CTA`: Prominent call-to-action banner.
18. `FOOTER`: Copyright, branding, studio credentials.

### Section Rendering Rules
- **Sort Order**: Templates must render sections sorted ascending by `displayOrder`.
- **Visibility**: The backend preview endpoint automatically filters out hidden sections (`isVisible == false`). `visibleSections` contains only active sections.
- **Empty States**:
  - *Projects (Phase 18)*: Zero projects must render a graceful placeholder (e.g. "Projects currently in curation"). It must never throw an error or leave an ugly broken container.
  - *Testimonials*: Empty reviews must render cleanly or collapse gracefully.
- **Strict Contact Privacy**:
  - `publicContacts` strictly contains contacts where `publicConsent == true`.
  - Templates must never attempt to access private phone numbers or emails.
- **Security Invariant**:
  - Section JSON content is validated and sanitized by the backend.
  - Templates must **NEVER** use `dangerouslySetInnerHTML` or evaluate unsanitized HTML/JS.

---

## 6. Frontend Architectural & Quality Requirements

### A. SSR & Hydration Safety
- Components must be compatible with Next.js SSR and client hydration.
- No direct usage of browser-only globals (`window`, `document`, `localStorage`) during initial render.
- Zero hydration mismatch warnings.

### B. Semantic HTML & Heading Structure
- Use landmark elements: `<header>`, `<main>`, `<section>`, `<article>`, `<nav>`, `<footer>`.
- Enforce strict heading hierarchy: exactly one `<h1>` in the Hero section; all subsequent sections use `<h2>` with subsection `<h3>`/`<h4>`.
- Form inputs must have accessible `<label>` elements or `aria-label`.

### C. Accessibility (WCAG 2.2 AA)
- Color contrast ratio: minimum 4.5:1 for standard text, 3:1 for large text and UI components.
- Interactive elements (buttons, links, toggles) must have visible focus indicators (`focus-visible:ring`).
- Icon-only buttons must have descriptive `aria-label`.

### D. Mobile-First & Responsive Preview
- Base responsive layout designed for mobile viewports (360px–430px) and scale up gracefully to tablet and desktop.
- Strictly support the `isMobilePreview: boolean` prop:
  - When `isMobilePreview === true`, render within a compact mobile preview frame (`375px`) without horizontal overflow or clipped text.

### E. Motion & Performance
- Respect user preferences via `@media (prefers-reduced-motion: reduce)`: disable heavy parallax, complex scroll transitions, or infinite auto-scrollers when reduced motion is preferred.
- Keep bundle size minimal: do NOT install heavy third-party animation or 3D libraries (Three.js, Framer Motion, GSAP). Use lightweight Tailwind CSS transitions and vector SVG icons (`lucide-react`).

---

## 7. Ownership Boundaries: Astra vs Engine

### What Astra May Edit (Phases 11–16)
- Files inside `apps/web/src/components/portfolio/templates/<theme-name>/**`
- Adding visual components, styling modules, typography tokens, and decorative SVGs for the theme.
- Registering the finished theme in `apps/web/src/lib/portfolio/template-registry.tsx` (changing its status from `SCAFFOLD` to `AVAILABLE`).
- Writing theme-specific unit tests under `apps/web/src/components/__tests__/` (e.g., `BasicTemplate.test.tsx`).

### What Astra Must NEVER Edit (Engine-Owned)
- **Backend Java code** (`apps/api/**`)
- **Database migrations** (`V001` through `V006`)
- **Authentication, sessions, CSRF, and authorization** (`AuthorizationService`, `SessionSecurityService`)
- **Portfolio engine API client or endpoints** (`portfolio/api.ts`, `PortfolioController.java`)
- **Section schema validation** (`PortfolioSectionValidator.java`)
- **Optimistic concurrency & version snapshots**
- **The `PortfolioTemplateProps` contract** (`apps/web/src/lib/portfolio/template-contract.ts`)
- **Global workspace shell and navigation** (`apps/web/src/components/workspace/**`, `apps/web/src/app/workspace/**`).

---

## 8. How to Verify a Template Implementation

Astra must run and pass all of the following commands before completing a template phase:

1. **Unit & Integration Tests**:
   ```powershell
   npm run test
   ```
   All existing 89 tests plus any new template tests must pass (0 failures).

2. **TypeScript Typecheck**:
   ```powershell
   npm run typecheck
   ```
   Must exit with code 0 (0 type errors).

3. **ESLint**:
   ```powershell
   npm run lint
   ```
   Must exit with code 0 (0 lint warnings/errors).

4. **Production Build**:
   ```powershell
   npm run build
   ```
   Next.js Turbopack build must compile and optimize all routes without error.
