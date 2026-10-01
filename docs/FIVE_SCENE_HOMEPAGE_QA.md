> Historical homepage report: the five-scene experience now lives at /interior-journey. See BRAND_HOMEPAGE_QA.md for the current homepage and validation.

# Approved five-scene homepage — QA and handoff

2026-09-29. This supersedes the three-scene homepage portion of VISUAL_REDESIGN_QA.md. Existing public-page refinements remain in place. Phase 29 has not started.

## Delivered

The homepage uses exactly the five user-provided images, in this order:

1. Bedroom wardrobe / cupboards — Storage designed around the way you live.
2. Modular kitchen — Crafted kitchens for modern living.
3. Hall cabinetry — Storage that becomes part of the design.
4. TV unit — Living spaces crafted with balance and purpose.
5. Custom feature niche — Every corner can be crafted with intention.

The exact supplied labels, headlines, supporting paragraphs and CTA wording are preserved. CTAs use existing category routes: wardrobes, modular-kitchens, living-room, tv-units and custom-furniture. No new backend route or taxonomy was invented.

Desktop uses a native-scroll sticky stage with perspective movement, shallow camera push-in, a masked incoming room and restrained depth. Text leaves before the next headline enters, avoiding doubled headlines. Scene links jump into the settled part of each scene. The final room remains visible as the stage naturally releases into category discovery.

Tablet uses a wider image beneath two columns of text with reduced perspective. Phones have five unpinned stories, landscape 3:2 image frames, per-image focal positions, separate text and much lighter camera movement. Reduced-motion and missing-animation-API fallbacks retain all five stories and their working links.

The calmer category and professional sections also use these uploaded assets. Obsolete Pexels attribution was removed from the current homepage. User-supplied concept imagery is clearly labelled AI Concept Visualization and kept separate from actual public project listings.

## Source integrity

All five PNG source files are byte-for-byte matches with their ZIP entries. Only their filenames changed. Total source size: 9,415,910 bytes. Responsive image optimization is performed by Next Image; no source image was regenerated or distorted. Mapping and provenance: apps/web/public/images/approved/README.md. Checksums: docs/visual-qa/approved-five-scenes/asset-integrity.json.

## Verification

- Production build and its TypeScript check: PASS.
- ESLint: PASS.
- Frontend test suite: 258/258, 35 files. Seven cinematic-hero tests cover exact asset order, headline, all scene anchors, final-scene settling, phone fallback, reduced-motion changes and continuous room coverage. Transition tests assert that no two text layers are visible together.
- Browser: all five scenes checked at 360×800, 390×844, 430×932, 768×1024, 1024×768, 1366×768, 1440×900 and 1600×1000. All 40 checks passed image-loaded and no-horizontal-overflow checks.
- Visually inspected desktop, tablet and all five mobile crops. Actual scroll midpoint inspected and text timing corrected. Final release inspected: category section begins directly after hero without a blank spacer.
- Production browser warnings/errors: none recorded in the inspected session.
- Evidence: docs/visual-qa/approved-five-scenes/ contains 01–05 desktop/mobile screenshots, responsive-checks.json and asset-integrity.json.

Reduced-motion is verified by component tests and CSS inspection, not an OS preference switch. Device widths are browser emulation; physical-device FPS, thermal behavior and measured LCP/CLS are not certified. No perpetual render loop, no new motion dependency; animation work is scheduled by requestAnimationFrame and gated by IntersectionObserver. The first image has high fetch priority; the current/next desktop scene is prepared early, while mobile retains lazy loading.

Live public discovery was unavailable locally and retains its truthful error state. Existing API/authentication integration and bare-project-slug caveats from VISUAL_REDESIGN_QA.md still apply; this image-focused change does not resolve or hide them. No backend, auth, migrations, tenant security, portfolio contracts or CSP changes were made in this iteration. No commit, push or deployment was performed.

Local production preview: http://127.0.0.1:3000.

