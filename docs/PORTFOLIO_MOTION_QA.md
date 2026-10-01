# Six-template portfolio presentation pass

Completed locally on 2026-09-30. Phase 29 remains unstarted. No commit, push or deployment.

## Delivered

- BASIC, MODERN, LUXURY, ARCHITECTURAL, WARM_NATURAL and DARK_CINEMATIC use the shared progressive scroll presentation.
- Photo groups enter from alternating sides with CSS perspective/rotation and shallow reversible vertical parallax. Text rises into position. This is CSS 3D presentation of photographs, not reconstructed 3D rooms.
- Motion strength differs by theme; compact previews use 32% of desktop movement. Native scrolling is preserved.
- Before/after and concept/reality pairs move as one plane. Their labels and source content remain intact.
- Single comparison groups span their grid, improving photographic scale. Shared heading wrapping, body leading and caption spacing refine readability. BASIC anchor offset clears its sticky navigation.
- No content is initially hidden. Reduced-motion preferences disable enhancement and changes to that preference clean up transforms. Disclosure text stays static. Print removes transforms. Observers/listeners are cleaned up on unmount; animation frames run on scroll/resize for nearby elements.
- No portfolio props, API, schemas, backend, migrations, auth or security changes.

## Verification

- Full frontend suite: 266/266 passed (37 files).
- Focused final portfolio suite: 7/7 passed after final motion-strength adjustment.
- ESLint: pass. Production build including TypeScript: pass.
- Browser inspection: all six desktop themes; all six at 390px with one h1 and no horizontal overflow. BASIC imagery loaded successfully after lazy loading; all other sample images loaded during checks.
- Confirmed comparison images share one animated parent and scrolling changes its transform. Confirmed Dark Cinematic mobile menu opens and its section link closes/navigates correctly.
- Confirmed BASIC section anchor top ~160px clears sticky header bottom ~133px in the inspected desktop viewport.
- Reduced-motion, SSR visibility, contrast fallback and comparison grouping covered by automated tests. No browser OS reduced-motion toggle or cross-browser performance benchmark was performed.

## Preview and limits

An ignored local-only Vite harness lives at `.local/portfolio-qa/`; run `node .local/portfolio-qa/server.mjs` from the repository root to view http://127.0.0.1:3002/. The selector previews all six real template components with explicitly labelled synthetic studio content and concept imagery. It is not a production route or a live designer portfolio. HTTPS media validation is unchanged; local asset substitution exists only in this QA harness.

Screenshots: `docs/visual-qa/portfolio-motion/dark-desktop.png`, `dark-mobile.png`, and `luxury-desktop.png`.

Live API/auth data was unavailable, so authenticated builder persistence and real published designer content were not end-to-end verified. This report confirms frontend implementation and the listed checks, not deployment or blanket production approval.

## Typography revision after user review — 2026-09-30
The user rejected the oversized lettering. All six themes now cap main headings at 48px and section headings at 30px (32px/24px on phones). Browser measurements at 1280px: 45.53px/29.09px; at 390px: 32px/24px for every theme, with no horizontal overflow. Theme-specific section/hero padding is reduced, introductory text and enquiry buttons are aligned, service/quote text is restrained, and hover image zoom is removed. Scroll travel/rotation reduced to 44px horizontal, 36px total vertical, 2 degrees and 22px text entrance before theme/mobile scaling. Existing content and theme font selections are preserved.
Final focused portfolio tests: 7/7; lint and production build including TypeScript pass. Desktop and phone screenshots: refined-luxury-desktop.png and refined-mobile.png in docs/visual-qa/portfolio-motion/. Previous 266-test full suite remains the last full-suite result; it was not rerun for this CSS-focused revision. Live API/auth limits remain unchanged.
