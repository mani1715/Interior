# Premium visual redesign — implementation and QA

Date: 2026-09-28. Phase 29 has not started. Changes are local and uncommitted.

## Delivered

- Homepage: kitchen → wardrobe → living/TV cabinetry story using real, credited editorial photography. Desktop CSS perspective, forward image movement, a nearer architectural edge, masked room reveals, complementary text transitions and scroll progress. Native scrolling, scene navigation and skip link; no animation framework or WebGL dependency.
- Phones: three naturally flowing stories with lighter progressive image movement. Tablet perspective is reduced. Reduced-motion and missing-browser-API fallbacks expose all stories and links.
- Cream/charcoal editorial design: header and keyboard-accessible mobile menu, categories, live project discovery with loading/error/empty states, professional introduction, labelled AI workflow, factual trust information and final CTA.
- Public discovery cards and detail presentation; authentication/public onboarding/review styling; scoped workspace spacing and hierarchy. The six portfolio template components and their contracts were not edited.
- Project cards use the existing collection API/modal instead of a simulated save toggle. Dialog focus trap, Escape and focus restoration added.
- Detail pages consume public API data; removed fake professional fallback and the demo-only project detail renderer. Missing mapper data no longer invents budgets, locations, years or durations.
- Existing Tailwind class usage now has the Tailwind/PostCSS compilation it was missing. Utility styling is layered over the base reset; no Tailwind preflight. Existing palette aliases supplied.
- Existing SEO metadata functions and safe project/breadcrumb JSON-LD are used.

## Validation

- Frontend tests: **255/255**, 35 files. Includes four new scene/fallback/motion-continuity tests. Existing jsdom canvas/window.open warnings remain test-environment warnings.
- Lint: PASS. Typecheck: PASS, also checked by the final production build.
- Production build: PASS. Production preview started on http://127.0.0.1:3000.
- Homepage browser widths: 360, 390, 430, 768, 1024, 1366, 1440 and 1600; no horizontal overflow. Desktop scroll advances through all three stories. Phone layout has no pin and no inert stories.
- Mobile menu opens, Escape closes it, body scrolling is restored and focus returns to Open menu.
- Public route DOM-width checks at 360/390/430/768/1024/1440: projects, professionals, modular-kitchen category, sign-in, sign-up, review submission. Onboarding and workspace correctly redirect signed-out access to sign-in. Full results: visual-qa/responsive-checks.json.
- Production session browser error/warning logs were empty after these checks. Development React's existing CSP/unsafe-eval warning disappeared in the production build; security headers were not weakened.
- Desktop/mobile screenshots: visual-qa/home-desktop.png and home-mobile.png.

## Performance architecture and limits

Three source JPEGs total 909,228 bytes. Next Image supplies responsive sizes. The first image is eager/high priority; upcoming desktop rooms are prepared early to avoid revealing unloaded photography. Mobile and below-the-fold images retain lazy loading. One passive scroll listener schedules at most one requestAnimationFrame; IntersectionObserver suspends offscreen work. No perpetual rendering loop. Camera animation uses transforms/opacity plus a reveal clip; only desktop photographic planes request compositing hints.

No instrumented LCP/CLS/frame-time/GPU benchmark or physical-phone thermal/FPS measurement was performed. The browser width checks are desktop viewport emulation, not physical touch-device certification. Reduced-motion switching and fallbacks were verified with component tests, not OS-level browser emulation. Do not report universal 60 FPS or a Lighthouse score.

## Integration follow-up before production sign-off

The local live discovery API was unavailable during browser QA, so its truthful unavailable state was inspected. Real populated results, private workspace content, successful authentication, inquiries, review submission and collection persistence require a working API/provider and signed-in test session. Those flows retain existing API contracts and have regression coverage, but have not received live end-to-end sign-off here.

The existing API resolves project details with both studio and project slug. New discovery links pass `?studio=` to resolve deterministically. Bare `/projects/[slug]` links attempt exact-slug matching in discovery search; a custom slug unrelated to the title may not resolve. A public canonical-slug resolver remains an integration issue; no backend endpoint or schema was added for this visual task.

No backend, database migrations, auth/session implementation, tenant logic, RLS, portfolio schemas/contracts, workspace security or CSP changes. Backend tests were not rerun. No commit, push or deployment performed.

Asset sources: ../apps/web/public/images/editorial/README.md (from repository root: apps/web/public/images/editorial/README.md).
