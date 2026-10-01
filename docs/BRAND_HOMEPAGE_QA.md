# Brand homepage and separate interior journey

2026-09-29. Supersedes the homepage direction in FIVE_SCENE_HOMEPAGE_QA.md. Phase 29 has not started.

## Delivered

- `/` now renders BrandHero, a separate full-screen brand composition. The opening room establishes the platform, the scroll camera advances through layered material studies, and the living-room transition introduces real projects, professionals and visualization. No carousel or left-copy/right-photo sequence is used in this hero.
- Native scroll, CSS perspective, translateZ, restrained rotation and opacity produce the 2.5D experience. No animation library or WebGL dependency was added. A single requestAnimationFrame callback is scheduled per scroll frame; IntersectionObserver gates offscreen work.
- Desktop has a 330svh sticky story; tablet shortens it to 260svh. Phones use a separate cream typographic opening, a large naturally flowing image and platform introduction, without pinning. Reduced motion and missing animation APIs retain a useful static opening and the normal homepage sections.
- Inactive desktop copy is inert and hidden from accessibility APIs. Headlines have nonoverlapping fade intervals. A skip link bypasses the story. Only the opening visual has high fetch priority.
- `/interior-journey` preserves CinematicHero and all five original images, their exact copy, ordering and category links. It has route metadata and a real concluding discovery section for its skip anchor. Footer navigation links to it from public pages; the homepage craftsmanship chapter also links to it.
- The calmer category, real-project, professional, AI, trust and CTA sections remain below the hero. Existing public discovery, detail, inquiry/review/auth and practical workspace refinements remain intact.
- Concept imagery remains labeled as AI visualization. No sample completed projects, review counts or verification claims were invented.

## Validation

- Frontend: 262/262 tests, 36/36 files passed.
- ESLint passed. Production build and its TypeScript check passed, including the new route.
- Browser verified desktop opening, depth chapter, destination and journey navigation; mobile 390px and tablet 768px visually inspected.
- Width checks at 360, 430, 768, 1024 and 1440px: no horizontal overflow, one h1, mobile unpinned, desktop/tablet enhanced.
- Browser console returned no warnings/errors during the final desktop review.
- Screenshots and responsive measurements: `docs/visual-qa/brand-homepage/`.
- Existing journey tests still pass, preserving the uploaded-photo behavior. Original five-image checksum evidence remains in `docs/visual-qa/approved-five-scenes/`.

## Limits and repository state

Local public-project API is unavailable, so the truthful retry surface is shown. Live authenticated flows and real backend data were not end-to-end verified in this visual pass. Reduced motion was component-tested; no physical-device FPS or Core Web Vitals benchmark is claimed. Existing project slug resolution limitation documented in the earlier redesign report remains.

No backend, migration, tenancy, authorization, security-header or portfolio-template contract changes were made by this homepage separation. Earlier accumulated frontend changes remain uncommitted; nothing was pushed or deployed.

## Follow-up — upward scroll depth
Hero text now travels upward continuously with perspective, depth and a small X-axis tilt. Foreground photos travel upward at different speeds, while the room camera rises more slowly. Homepage content headings, paragraphs and photos also use native CSS view timelines for lighter upward movement; unsupported browsers remain static. Mobile has smaller travel distances and no pinning, with reduced-motion guards. Browser checked desktop transforms, lower-section view timelines and mobile overflow/pinning. Hero tests 4/4, lint and production build/typecheck pass. Screenshot: visual-qa/brand-homepage/upward-depth.png.

## Follow-up — clearer imagery and directional entrances
Replaced the professional section's wide, heavily cropped hall image with generated/hall-cabinetry-v2.png (1254-square native output). The source and exact built-in generation prompt are documented alongside the asset. Quality 90 is allowlisted in Next Image configuration; security headers are unchanged. Removed the continuous image zoom and text drift below the hero. ScrollEntrances now reveals text from below and photo frames from alternating sides with shallow perspective, settling at transform:none. A single IntersectionObserver registers initial and asynchronously loaded project content; reduced-motion changes clear the enhancement. Mobile uses shorter 40px side travel. Static content remains readable without animation APIs.

Validation: 264 tests / 37 files pass; typecheck, lint and production build pass. Browser verified alternating left/right assignments, new image load at q=90, settled native-size image, text entry and mobile unpinned/no horizontal overflow. Console clean. Final screenshot: visual-qa/brand-homepage/sharp-cabinetry-and-entrances.png.

## 2026-09-30 — Final palette and editorial polish
Completed the user-authorized public visual polish: scoped warm ivory, charcoal, deep olive and aged-bronze palette; deep-olive AI section; larger readable labels/body text; improved category and professional spacing; consistent primary actions; refined final CTA/footer; public discovery and auth controls. Workspace remains practical and template identities remain untouched. Discovery search/selection actions now match the olive CTA; removed the redundant search-input border while retaining a form-level keyboard focus outline.

Final build including TypeScript and lint pass; git diff whitespace check passes. Most recent full frontend suite: 264/264 from the preceding behavior change; CSS-only polish did not add behavior tests. Desktop/mobile homepage and mobile discovery visually reviewed during this pass; screenshots premium-ai-desktop.png and premium-ai-mobile.png are in visual-qa/brand-homepage. Final browser reconnect timed out, so the last discovery-control CSS adjustment is build/lint verified but not visually rechecked after restart.

Measured contrast ratios: body on ivory 7.43:1, muted text on ivory 5.53:1, bronze label on stone 4.61:1, light body on olive 9.67:1, caption on olive 7.88:1, primary button 12.07:1. These are selected flat-color pairs, not a full accessibility audit of imagery or every application state.

Local API remains unavailable; authenticated and live-data flows were not revalidated. Changes remain local/uncommitted and undeployed. Phase 29 has not started.

## 2026-09-30 — Project library redesign
Rebuilt /projects around an image-led editorial opening, full-width search, underlined category tabs with aria-pressed state, and compact filters/sort toolbar. Removed the duplicate popular-query chips on this page and the permanent desktop sidebar. All filter dimensions remain in a responsive shared Dialog, with focus trapping, Escape and focus restoration. API error state is compact and links to the separate AI-concept interior journey; no fake completed projects or unavailable-result counts. The filter dialog also suppresses unknown result counts. Metadata, URL filter state and API integration remain intact.

Validation: full suite 264 tests passed; final focused discovery rerun 42 passed; production build/typecheck and lint passed. Desktop/mobile browser review, overflow check, anchor scroll, filter opening, Escape and focus restoration verified. Screenshots: visual-qa/project-library/. Live API remains unavailable. No commit, deployment or Phase 29 work.
