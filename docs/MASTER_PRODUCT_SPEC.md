# Master Product Specification

Version 1.0 • Phase 00 • 2026-09-17 • Status: planning baseline

This is the source of truth for the Interior Designer Portfolio + Discovery + AI Visualization Platform. No capabilities described here are implemented in the empty workspace. The [audit](00_REPOSITORY_AUDIT.md) records evidence, the [requirements matrix](00_REQUIREMENTS_MATRIX.md) assigns stable IDs, the [decision log](00_ARCHITECTURE_DECISIONS.md) distinguishes decisions from recommendations, the [risk register](00_RISK_REGISTER.md) records risks, and the [roadmap](00_IMPLEMENTATION_ROADMAP.md) defines execution gates. New decisions must update these documents together; changes may not silently remove requirements.

## 1. Product Overview

Purpose: “Build your interior portfolio. Get discovered on Google. Visualize ideas with AI. Convert your work into customers.” Serve interior designers/studios, architects/architecture studios, custom furniture businesses, modular interior professionals, woodwork/cabinetry professionals and turnkey interior businesses.

The eight pillars are premium portfolios, SEO, real-project discovery, AI interior visualization, client collaboration, lead/business tools, trust/verification and secure media. This is a combined product, not merely a directory, dashboard or image generator. India and mobile site workflows are initial priorities, with localization-ready architecture.

## 2. Product Principles

Preserve useful production functionality when it exists. Prefer a modular monolith with explicit ownership and replaceable external adapters. Public work remains browseable without login. Original photographs and AI inputs are private by default; publication is an explicit, authorized transition. SEO, mobile usability, accessibility and performance outrank effects. Watermarking and AI labeling are publication requirements, not optional UI decorations.

### Locked visual identity

Elegant, modern, architectural, warm, premium, professional and timeless. No random purple SaaS palette, neon gradients, excessive glassmorphism or undocumented colors. Theme palettes may vary only within their controlled design specifications.

| Token | Value | Use |
|---|---|---|
| Deep Charcoal | #1F1F1F | Primary text, headings, navigation, dark sections and dark CTA |
| Warm Ivory | #FAF8F5 | Main application/public background |
| Sage Neutral | #E7E1D8 | Alternate sections and soft/card surfaces |
| Warm Bronze | #B88A5A | Primary brand accent, primary CTA, selection and restrained highlights |
| Forest Green | #2E5D4B | Success and verification |
| Muted Blue | #3E6D8C | Information, links and suitable secondary actions |
| Terracotta | #C76F4A | Limited warm accent |
| Soft Rose | #E6C9C3 | Subtle backgrounds only |
| White | #FFFFFF | Light surfaces |
| Light Grey | #F4F4F4 | Neutral surface |
| Medium Grey | #D9D9D9 | Decorative borders; not automatically sufficient for essential control boundaries |
| Dark Grey | #6B6B6B | Secondary text where contrast passes |

Do not make every button bronze. Use Deep Charcoal text on Warm Bronze primary CTAs: calculated contrast is approximately 5.35:1; white is approximately 3.08:1 and fails normal-text AA. Verify every actual state/background combination, including theme overrides. Error/danger semantics need a separately documented accessible color and icon/text treatment in Phase 04; do not treat Terracotta as automatically valid error text. Never communicate state with color alone.

Editorial headings plus readable UI typography: evaluate Playfair Display or equivalent with Inter or equivalent. Confirm license, self-hosting/subsetting strategy, fallback metrics, readability and Telugu/other script coverage before pinning fonts. Themes can have distinct controlled typography. Prefer CSS effects initially; Motion and Three.js/React Three Fiber require a measured benefit and budget justification.

## 3. Users & Roles

| Actor | Allowed product scope | Boundary |
|---|---|---|
| Public visitor | Browse/search/filter designers, projects and portfolios; view transformations; call, WhatsApp, enquire, request similar work, share | No login wall for browsing; no private source access |
| Customer | Save/compare ideas, collections, manage own enquiries; private concept review, like/comment/approve/request changes; eventually review work | Own data or explicit review grant only |
| Professional | Business profile, branding/theme, portfolio/projects/media, AI/references/history, leads, client sharing, SEO, analytics, reviews, billing, domains, permitted team | Organization membership plus resource/action permission |
| Team member | Future portfolio/project/media/lead manager scopes | Tenant-scoped, least privilege; never assume a sole permanent owner |
| Moderator | Assigned report/review/content moderation | No billing, secret access or blanket private-media access |
| Admin | Scoped operational administration | Strong MFA, audited access, separate privileged policies |
| Super Admin | Highest platform privilege and role administration | Step-up, tightly controlled grants, monitored recovery/break-glass |

Identity and organization membership are separate; one user may belong to multiple studios or be a customer and professional. Organization ownership transfer and revocation must be possible. Platform roles never arise from editable profile attributes. Team UI is V1.5; tenant membership and permission model are V1 architecture.

## 4. Information Architecture

Proposed route contracts (none currently exist):

| Surface | Routes | Rendering / privacy |
|---|---|---|
| Public platform | `/`, `/explore`, `/designers`, `/projects`, `/categories`, `/cities`, `/inspiration` | Server-rendered crawlable content |
| Portfolio | `/{studioSlug}` | Published snapshot only |
| Project | `/{studioSlug}/projects/{projectSlug}` | Published project, tenant-unique slug |
| Curated discovery | `/interior-designers/{city}`, `/tv-unit-designers/{city}`, `/modular-kitchen-designers/{city}` | Editorial eligibility gate; not every permutation |
| Identity | `/auth/sign-in`, `/auth/sign-up`, recovery/verification flows | Noindex; safe redirect allowlist |
| Professional | `/dashboard` and `/dashboard/{module}` | Authenticated, tenant-scoped, noindex, private caching |
| Customer | `/account`, own enquiries/collections | Authenticated/private; V1.5 enrichment |
| Preview/review | `/preview/{opaqueToken}`, `/share/{opaqueToken}` | Authorized grant, noindex, no-store; never public fallback |
| Administration | `/admin` and scoped subroutes | Privileged server policy and MFA |

Reserve platform slugs (`admin`, `api`, `auth`, `dashboard`, `account`, `explore`, `designers`, `projects`, `categories`, `cities`, `inspiration`, `preview`, `share`, and future route namespaces). Maintain normalized uniqueness and slug aliases with controlled redirects. Never derive tenant authority from URL text alone. Discovery detail links resolve to the portfolio's canonical project rather than duplicate full project pages.

Dashboard: Overview, Portfolio, Projects, Media, AI Studio, Leads, Reviews, SEO, Analytics, Billing, Settings. V1.5-only controls must not pretend to work in V1. Proposed mobile navigation is Home / Projects / Create / AI Studio / More. Create exposes Add Project, Upload Photos, AI Visualization and Add Lead with permission-aware availability.

## 5. Public Platform

The home page should show real interior transformations instead of generic SaaS artwork. Planned sections: premium hero, featured projects, explore by space, AI demonstration, Before/After, Before → AI → Reality, featured designers, how it works, city discovery, professional CTA and footer.

Possible hero narrative: unfinished interior → structure → shutters → material → handles → lighting → finished interior. A static or lightweight sequence must communicate the same story. No critical content, navigation or indexable text may depend on WebGL. Optional desktop effects include depth/parallax, cabinetry, material transitions, doors/drawers and cinematic project transitions. Mobile/weak devices receive optimized alternatives; reduced motion disables nonessential effects. No implementation of this hero belongs in Phase 00.

## 6. Portfolio Studio

Each professional gets a premium website, initially `platform.com/{studioSlug}`, later a verified custom domain. Supported content: logo, favicon, studio name, hero, about, services, experience, team, locations/service areas, projects, videos, transformations, testimonials, reviews, awards, process, FAQ, contact, WhatsApp, socials and enquiry CTA.

Use controlled modular sections: Hero, About, Featured Projects, Project Grid, Services, Before/After, Before → AI → Reality, Video, Statistics, Process, Testimonials, Reviews, Team, Awards, Service Areas, FAQ, Contact, Social and CTA. Allow ordered sections, visibility, validated settings, preview, draft save, publish and version restore. Do not allow arbitrary executable HTML, CSS or scripts.

Model immutable `PortfolioVersion` snapshots with schema/theme versions, referenced content revisions, asset IDs and validated section settings. `Portfolio` holds draft/live pointers. DRAFT is mutable authoring, PREVIEW is an authorized rendering state, PUBLISHED points to an immutable approved version. Restoring V6 after V8 creates a new draft/version, preserving V7/V8 and audit history; it does not erase history. Optimistic concurrency prevents lost edits. Publish validates permissions, ownership confirmation, required facts, media readiness, theme compatibility and SEO, then atomically updates live pointers and emits cache/search/sitemap events. Unpublish and deletion must revoke public delivery references and invalidate caches. Historical references cannot resurrect deleted/private assets.

## 7. Portfolio Themes

All six themes are V1, developed in independent Phases 11–16 with their own design specifications. A theme registry defines ID, supported schema versions, section capabilities, typography, tokens and renderers. The same structured portfolio/project data feeds each theme; switching must preserve data and report unsupported presentation settings. No theme forks the domain schema or duplicates authorization/SEO logic.

| Theme / phase | Distinct design direction |
|---|---|
| BASIC / 11 | Fast, professional local-business/carpenter portfolio; simple navigation, clear hero and project grid, minimal animation; Ivory, Charcoal, Bronze, White, Sage |
| MODERN / 12 | Project-first bold imagery, modern typography, asymmetric grids and restrained parallax; Charcoal, White, Muted Blue and warm neutrals |
| LUXURY / 13 | Cinematic studio narrative, editorial serif, full-screen photos, generous whitespace and restrained Bronze; near-black, Ivory and luxury neutrals |
| ARCHITECTURAL / 14 | Technical/editorial grid, case studies, drawings/plans, process/team/awards; White, Charcoal, cool greys and minimal accent |
| WARM / NATURAL / 15 | Welcoming residential presentation, organic rhythm, comfortable type and natural materials; Ivory, wood/Bronze, Forest Green and Sage/beige |
| DARK CINEMATIC / 16 | Immersive photography-first dark presentation and cinematic transitions; near-black, Bronze, Muted Blue, restrained Terracotta |

Every theme must differ in navigation, hero, type hierarchy, section rhythm, project cards/grid, gallery, project detail, CTAs, spacing, motion and image treatment. Phase 17 compares the same dataset across themes and rejects simple color reskins. Basic means restrained excellence, not an intentionally poor free tier.

## 8. Project System

Structured case studies contain name, slug, studio, location, property type, category, style, scope, budget range with currency, completion date/year with precision, approximate duration, materials, description, services, cover/gallery/videos, Before/AI Concept/Actual Result associations, tags, SEO fields, publish status and verification state. Unknown facts remain unknown; exact private addresses are separate from public locality.

Extensible seeded taxonomy: Living Room, TV Unit, Bedroom, Wardrobe, Modular Kitchen, Pooja Unit, Crockery Unit, Study Unit, False Ceiling, Wall Panels, Shoe Rack, Office, Commercial, Custom Furniture and Full Home Interior. Room, work type, service, style and property type are distinct filter dimensions, not a single overloaded category string.

Projects have drafts, published revisions and archived/unpublished states. Media joins carry order, role, captions and paired transformation grouping. Before → AI Concept → Actual Result is a signature narrative, with accessible static labels and optional scroll/slider enhancement. A concept is never represented as completed real work. Project CMS provides create/edit/preview/publish, validation, recoverable drafts and explicit empty/loading/error states.

## 9. Media System

First-class `MediaAsset`, `MediaVariant`, `MediaUsage`, album and tag records; never only URL arrays inside projects. Features: single/bulk upload, albums, tags/search, reorder, cover, captions/alt text, crop/focal point, Before/After pairing, reuse, trash/restore and storage accounting. Asset references retain ownership, provenance and processing status.

Supported classification labels: REAL_PROJECT, BEFORE, AFTER, AI_CONCEPT, AI_INPUT, REFERENCE_COLOUR, REFERENCE_MATERIAL, REFERENCE_HANDLE, REFERENCE_DESIGN, CLIENT_PRIVATE, PORTFOLIO_PUBLIC, LOGO, COVER. Separate content kind, usage role and visibility so an AI_INPUT cannot become public merely by assigning COVER. Publication creates an approved derivative/usage; it never changes the original's private storage policy.

Upload path: authenticate → authorize tenant/action and reserve quota → size/extension allowlist → MIME, magic-byte and content checks → sandboxed safe decoding with pixel/frame/decompression limits → malware processing where appropriate → metadata policy → server-generated key → immutable private original → processing. Direct uploads land in private quarantine with short-lived, constrained grants; server completion verifies the actual object before acceptance. Remove EXIF/geolocation from delivery variants. Original retention remains governed by deletion policy, not permanent preservation against user rights.

Recommended states: UPLOADING, QUARANTINED, VALIDATING, PROCESSING, READY, FAILED, TRASHED, PURGED. No unvalidated object can be served publicly. Jobs are idempotent by asset/checksum/recipe version. A failed worker retains actionable status and retry path. Logos, videos and future drawings get safe format-specific pipelines; do not accept executable/SVG content without sanitization or rasterization.

## 10. Watermark System

Every public portfolio/project photograph must visibly display its studio watermark. Upload a logo once; configure logo, business name or both, position (bottom-right/default, bottom-left, top-right, center, tiled), size and opacity with preview. If no usable logo exists, use the business-name watermark; never bypass watermarking. Keep the result subtle but visible, with bounded settings that cannot make it invisible.

Pipeline: private immutable original → normalized processing → responsive derivatives → watermark → approved public derivative → CDN. Private clean masters are never overwritten. Public AI concepts additionally bear visible “AI Concept Visualization” identification. Public covers, thumbnails, OG images, exports and transformed image endpoints must not bypass the same photograph policy. Brand logos/favicons are assets, not project photographs, and do not require a second logo stamped on them.

Version the logo/configuration and processing recipe. Changes generate new variant keys; publication waits for required variants and retires/invalidate old references according to policy. Watermarking failure blocks public publication. No public route, image optimizer, URL parameter or predictable alternate key can serve the clean source. CDN origin cannot read originals. Tests include direct origin, guessed keys, optimized image routes and cross-tenant signed access. Watermarks deter misuse but do not establish ownership or guarantee copyright protection.

## 11. SEO System

SEO is a core capability in V1: server-rendered meaningful HTML, stable clean URLs, canonical URLs, unique metadata, Open Graph, truthful structured data, XML sitemap, image sitemap where useful, robots/indexing controls, breadcrumbs, internal links, responsive images/alt text, pagination, Core Web Vitals and duplicate prevention. Rendering must be verified for crawlers/social previews; metadata support alone is not a complete SEO implementation. [Next.js metadata documentation](https://nextjs.org/docs/app/api-reference/functions/generate-metadata).

Index only eligible published pages with meaningful original work, complete business/project context and editorially useful location/category content. Never automatically generate every city/category combination. Empty/low-value combinations remain absent or noindex. Arbitrary filter/search combinations are not indexable landing pages by default. Use finite crawlable pagination for approved listings; pages with distinct content have appropriate self-canonicals. Do not use robots.txt as privacy control or block a page whose noindex still needs crawling. Google documents both [faceted crawling concerns](https://developers.google.com/crawling/docs/faceted-navigation) and [scaled-content abuse](https://developers.google.com/search/docs/essentials/spam-policies).

Choose one canonical host per portfolio/project. On verified custom-domain activation, update canonicals, redirects and sitemap URLs consistently; provide safe reversal on domain removal. No fabricated review/rating markup or unsupported business facts. Slug changes maintain verified redirects; unpublishing removes sitemap/search entries and public caches.

SEO Center V1 provides understandable completion/health checks: business info, sitemap eligibility, project URLs, missing descriptions, weak titles and incomplete About. A health score explains its rule version and components; it is not a ranking prediction. Google impressions, clicks and queries are V1.5 integration work requiring authorized account connection and real data. Show unavailable rather than fabricated metrics. No Google-ranking guarantees.

## 12. AI Interior Visualizer

Core V1 workflow: real site photo + described change + optional typed references + optional selected region → queued AI editing → realistic labeled concept. Example intent: alternate blue/white shutters and silver handles on an unfinished TV unit, preserving structure, room, TV, wall, floor, dimensions, lighting and perspective as far as possible. Preservation is a quality goal, not a dimensional or construction guarantee.

Keep inputs, masks, references and generated results private by default. Provider interface describes editing, reference and mask capabilities, limits, cost estimate and model/version. Do not silently ignore unsupported masks/references; choose a validated capable provider or explain limitations before charging. No provider selection is locked in Phase 00.

Job lifecycle: CREATED → RESERVED → QUEUED → RUNNING → SUCCEEDED / FAILED / CANCELED; uncertain provider outcomes require RECONCILING before charging/refunding/retry. Reserve credits atomically, record idempotency key and provider request ID, settle once on defined billable success, release reservations on confirmed non-billable failure. A timeout is not proof the provider failed; reconcile before retrying a potentially billable request. Configure bounded retries/backoff, deadlines, concurrency, abuse moderation, quotas and operational kill switch. Keep credentials server-side.

History is append-only version lineage: instruction/model/parameters/input references/mask hashes, output assets, cost/usage and parent generation. Support variants, regenerate, edited instructions, color/material/handle changes, save, authorized publish-to-project, private share and retention-aware delete. Never overwrite prior results automatically. Publishing does not turn concepts into real-work claims.

Precision tools: brush, erase, rectangle, undo, reset, mask generation and selected-region editing when supported. Map touch/canvas coordinates to oriented source dimensions; store explicit mask convention and resolution. Test EXIF rotation and resize alignment. AI auto-selection is future scope. Outside-mask preservation and reference adherence require a representative evaluation set; reject providers that cannot satisfy the selected workflow reasonably.

V1.5 AI portfolio assistant suggests category/room, title, description, alt text, tags/style and cover. Designers must review/confirm before use or publication. Never infer factual cost, exact address, brand, customer or completion date without supplied evidence.

## 13. Reference System

Each reference is private and includes reference type, apply-to target and optional instruction. Support color, laminate, wood texture, marble, material, handle, door style and inspiration. Multiple references can target upper shutters, lower shutters or all handles independently. Reference priority over vague wording is explicit where appropriate; conflicting references/text require an understandable resolution, not hidden precedence.

Generic color entry prompts: “Looking for a specific shade? Colour names can be interpreted differently by AI. Upload a colour or material reference for a closer visual match.” Every result is labeled AI Concept Visualization and explains that physical colors/materials may differ. Never promise exact color/material reproduction. Provider requests transmit only authorized required assets, with documented retention/data-use terms reviewed before integration.

## 14. Client Collaboration

V1 prepares private concept/version sharing and grant infrastructure; V1.5 adds full client like/comment/approve/request-changes workflows, mood boards, material boards and collections sharing. Client approval references an immutable concept version; a new revision does not inherit approval. Feedback may compare Concept 2 with Concept 1's handles.

Secure review need not force account creation: high-entropy, hashed, scoped, revocable, expiring tokens grant only designated concepts/actions; optional PIN or verified recipient for sensitive work. Read and feedback permissions are distinct. Revocation/expiry applies to API and media access, not only the page. Grant-protected media must use a delivery gateway that checks the live grant; a previously issued storage presigned URL alone does not provide immediate revocation. General owner-authorized downloads may use short-lived signed delivery with a documented expiry window. No search indexing, shared cache or third-party referrer leakage; logs redact tokens. Approval records preserve actor/grant, time, version and event history. Native full client portal/project management remains V2.

## 15. Discovery

Project-first discovery links actual work to its creator. Search/filter by location, category, room, furniture/work type, style, budget, property type, service and verified professional. Example intents: TV units in Guntur, modern wardrobes, 3BHK interiors, modular kitchens. Distinguish real projects from labeled AI concepts; only published/allowed records enter public results. Apply the same visibility rules to counts, suggestions and facets.

Start with PostgreSQL-backed indexed search and stable pagination; measure before external search infrastructure. Publish/unpublish events update the search projection and invalidate caches. V1.5 supports customer saves, comparisons and inspiration collections such as My New House grouped into TV units, wardrobes, kitchens and pooja, with controlled designer sharing. V2 visual similarity search retrieves similar real projects/designers from an inspiration image; prepare provenance/index deletion hooks but do not store embeddings prematurely.

## 16. Leads/CRM

Eligible projects show “I want something like this.” Capture name, phone, location, budget, property type, message, reference project and attribution/source; deliver to the owning studio. General enquiries and call/WhatsApp actions remain available. Public forms need input limits, spam controls, consent wording, rate limits and secure attachment handling; successful submission must durably store before confirming.

Lead model: customer/contact, phone, WhatsApp, locality, budget, reference project, requirements, notes, attachments, status, follow-up and source. Pipeline: NEW → CONTACTED → SITE VISIT → DESIGN → QUOTATION → NEGOTIATION → WON / LOST. V1 is capture, authorized inbox and basic status handling; V1.5 extends follow-ups/CRM, not a full enterprise CRM in V1. Customer own-enquiry access must not expose studio notes or other leads.

WhatsApp actions: WhatsApp Designer, Ask About This Design, Share Concept and Continue on WhatsApp. Use user-initiated links with safe prefilled text; do not embed private tokens or personal details without intentional sharing. Future official WhatsApp Business integration is an isolated server-side adapter with secrets protected.

V1.5 sharing growth tools: stable portfolio/project QR destinations; digital visiting card containing studio, profession, call, WhatsApp, portfolio, location and Instagram/social links; branded exports for Instagram portrait/story, WhatsApp status and share cards using project image, logo, name, QR and portfolio URL. Exports inherit publication, AI label and watermark requirements. QR use includes visiting cards, invoices, brochures, site boards, offices, exhibitions and social sharing.

## 17. Reviews & Verification

V1 includes content ownership attestation (“I own this content or have permission to publish it”) and reporting for copied content/copyright, fake projects, impersonation and inappropriate material. Establish moderation actions, appeals and audit evidence; duplicate-image detection is a future assist, not proof of infringement.

V1.5 verification distinguishes phone, identity, business, address and project checks, with explicit pending/verified/rejected/expired/revoked states, evidence access limits and issuer/time. No fake badges. Reviews should relate to real clients/projects where possible and cover design, work quality, communication, timeline, budget adherence and after-sales. Anti-abuse moderation and conflicts disclosure apply. Private evidence is never public portfolio content.

## 18. Analytics

V1 foundation records portfolio/project views, privacy-appropriate approximate unique visitors, WhatsApp clicks and enquiries, plus source attribution; no fingerprinting requirement. V1.5 adds saved-project metrics, lead conversion, top projects, traffic-source analysis, Google integrations and QR reporting. Sources include GOOGLE ORGANIC, INSTAGRAM, FACEBOOK, DIRECT, QR, PORTFOLIO SHARE, PLATFORM SEARCH and PROJECT DISCOVERY. Distinguish observed clicks from confirmed leads/sales; deduplicate events and document attribution windows. Tenant-specific aggregates require authorization. Retention/consent choices must precede third-party analytics activation.

## 19. Billing

No prices finalized. Configurable FREE, PROFESSIONAL, AI/credit and STUDIO capabilities govern project count, storage, AI credits, team seats, analytics, SEO, custom domains and themes. Server-side entitlements use a versioned configuration/ledger, never scattered hardcoded limits. Phase 28 provides V1 usage/plan foundation and billing adapter; paid launch requires real webhook, reconciliation and cancellation tests. Razorpay is a candidate for India, not a selected provider. A free pilot can avoid live payments but must not claim paid billing is complete.

Concurrent requests reserve quota transactionally. Plan downgrade defines over-limit read access and blocks new consumption safely; do not delete projects automatically. Provider webhooks require signature/time validation where supported, deduplication, replay handling, idempotent state transitions and authoritative reconciliation. Refunds/disputes are audited. V2 quotations, estimates, client invoices and client payments are distinct from platform subscriptions/AI credits.

## 20. Admin

Administration covers users, professionals, customers, projects, media, generations, leads, reviews, verification, reports, subscriptions, payments, SEO, categories, cities, storage, AI usage, analytics, security events, moderation and audit logs. Phase 29 completes the platform; privileged policies begin in Phase 03 and apply whenever admin functionality is introduced.

Use least-privilege actions, MFA/step-up for sensitive operations, reason recording and tamper-resistant audit. Sensitive tenant/private-media inspection needs explicit permission and purpose; it is not implied by any staff role. Avoid unrestricted impersonation. Super Admin role changes and emergency access require strong recovery procedures and monitoring.

## 21. Security

Security is an implementation property, not a benefit guaranteed by Java. Prefer same-origin browser-to-backend secure sessions with HttpOnly, Secure, appropriately SameSite cookies, CSRF validation on mutating cookie-authenticated requests, rotation at login/privilege change, idle/absolute expiry and server revocation. Consider standards-based OIDC identity integration behind a replaceable adapter; final provider is deferred. If passwords are hosted, use vetted adaptive password hashing, safe recovery, brute-force defenses and no credential logging. Email/phone verification is required as appropriate to actions; privileged MFA is mandatory.

Each service operation authenticates, resolves verified tenant membership, checks action and object scope and returns only authorized fields. All tenant-owned records carry tenant ID; queries, foreign keys, unique constraints, workers and caches preserve that boundary. PostgreSQL row-level security can provide defense in depth after connection-context safety is demonstrated; it cannot replace application policy. Explicitly test two-tenant object substitution and revoked membership.

Validate input, use parameterized database access, encode output, restrict rich text, enforce request-size limits, strict CORS and secure headers/CSP, allowlist external fetch destinations against SSRF, and fail with safe actionable errors. Secrets live in environment-specific secret management, never frontend bundles or committed environment files. Use minimal IAM, isolated environments, dependency/secret scans, signed webhooks, payment idempotency, audit and alerting. Background workers recheck authorization/publication state where relevant; queue messages are not trusted authority. Do not rely on hidden UI controls.

## 22. Privacy

Interior photos may reveal people, family portraits, addresses, documents, screens and belongings. Explain this before upload/publication; keep AI inputs, references, approvals and masters private. Public locality differs from private addresses. Provide explicit publish review, secure sharing, data export, asset deletion and account deletion workflows. Minimize logs/provider payloads and establish vendor data-use/retention terms before sending real private images.

Recommended policy defaults to validate before launch: trash recovery 30 days; unused AI inputs/results 90 days with notice; security logs 90 days with restricted access; backups 35 days. Saved project assets remain until deletion/account policy, not auto-purged after 90 days. These are product proposals, not legal conclusions. Determine jurisdiction-specific retention and billing exceptions with qualified review before collection/paid launch. Deletion revokes live URLs/grants, purges derivatives/search/exports under platform control, tracks provider deletion where supported, and expires from backups on schedule. Previously downloaded/shared images cannot be recalled. A restore must replay deletion tombstones.

## 23. Performance

Design for 100 projects × 30 images without loading all 3,000 at once. Produce responsive widths, optimized formats with compatibility fallback, thumbnails and watermarked public versions; reserve dimensions, lazy-load below fold and prioritize the actual hero/LCP resource. Paginate galleries/media library; cache published snapshots with tenant/host/version-specific keys. Private responses never enter shared cache. Optimize fonts, split code and defer heavy 3D.

Proposed launch budgets: mobile field p75 LCP ≤2.5 s, INP ≤200 ms and CLS ≤0.1; initial public-route JS ≤150 KiB compressed excluding explicitly deferred optional 3D; initial above-fold image payload target ≤500 KiB on representative mobile routes. These are measurable product targets, not measured results or universal guarantees. Phase 04 sets representative devices/network fixtures; Phase 17/29 validates and documents exceptions. Use real-user monitoring when traffic supports it, plus lab testing before launch.

## 24. Accessibility

Target WCAG 2.2 AA across major journeys: semantic landmarks, keyboard access, visible focus, labels/instructions, descriptive alt text, contrast, screen-reader announcements, reduced motion, accessible menus/modals/forms and errors. Provide focus containment/return for dialogs, non-drag alternatives for reordering/masks where feasible, static alternatives to comparison sliders, captions/transcripts for meaningful video and usable zoom/reflow. Touch target design target is 44 × 44 CSS px; verify applicable WCAG criteria rather than presenting that product target as the AA minimum. [WCAG 2.2](https://www.w3.org/TR/WCAG22/) is the conformance reference.

## 25. Mobile

Start designs at 360–430 px and progressively enhance to 768, 1024 and 1440+ px. Every major screen must be checked at 360, 390, 430, 768, 1024 and 1440+ px. No horizontal overflow or compressed desktop tables. Use thumb-friendly primary actions, intentional bottom navigation, appropriate sheets/full-screen creation and editing, cards, readable responsive typography/galleries, phone keyboard inputs, safe-area support and judicious sticky actions.

Site workflows include photo/bulk uploads, project drafts, AI concepts, color/material references, presenting to clients and enquiries. Handle slow/interrupted uploads, resume/retry, draft preservation, memory-constrained images and clear progress/cancellation. Mask controls need touch zoom/pan without accidental edits. Desktop can expose richer navigation without altering core task semantics.

## 26. Storage

Use S3-compatible object storage with physically or policy-separated quarantine, private originals, private derived assets and published derivatives. Public delivery via CDN is restricted to approved derivatives; direct origin access is denied. Short-lived signed private access follows server authorization and has scoped keys/purpose, never bucket listing rights. Generated object keys avoid user names/filenames; encrypt transit and storage, track checksum/size/owner and protect encryption keys.

Classification policy: originals, AI_INPUT, references and CLIENT_PRIVATE always private; BEFORE/AFTER/REAL_PROJECT photographs public only through explicit publication and watermarking; AI_CONCEPT public only with both watermark and AI label; logo/cover follow usage-specific validation and publishing. Private shares use controlled private variants, not clean original URLs by default. No cross-designer asset reuse without explicit rights and authorization.

Account for original, derived, retained and pending upload bytes; storage quota and cost accounting are distinct views. Reuse does not duplicate masters. Trash is recoverable until purge; infrastructure backup is separate. Lifecycle jobs must consider active references and deletion/legal retention policy.

## 27. Observability

Structured logs, request/correlation IDs, trace propagation through outbox/queue/provider calls, error tracking and performance telemetry are required. Monitor APIs/DB, queue age/dead letters, AI failures/latency/cost, storage growth, authentication anomalies, rate limits, payment/webhook failures, publishing/watermark errors and backup status. Set actionable alert owners and runbooks before launch. Never log passwords, tokens, secrets, private image URLs, raw prompts or unnecessarily sensitive lead/customer data. Audit records and product analytics have different access/retention purposes.

## 28. Backup/Recovery

Managed PostgreSQL backups and PITR where available; object durability/versioning where useful; encrypted copies, backup monitoring and tested restoration. Proposed launch RPO ≤15 minutes for transactional data and RTO ≤4 hours, subject to selected vendor and measured drills. Restore database/object references together, reconcile AI/payment idempotency ledgers, rebuild search/cache projections and reapply deletion tombstones. Test recovery before launch and quarterly thereafter. Document disaster roles, credentials, dependencies and rollback. User-facing trash does not prove infrastructure recoverability.

## 29. Internationalization

English first; Telugu early candidate; Hindi, Tamil, Kannada, Malayalam, Marathi and other languages later. Externalize UI strings, use locale-aware dates/numbers/currencies, Unicode-safe names/search and fonts covering relevant scripts. Separate UI locale from user-authored content language. Introduce localized URLs/hreflang and self-canonicals only for real translated content; never bulk-duplicate SEO pages as a localization shortcut. Multilingual UI ships in V1.5.

## 30. Future Architecture

Recommended target, not installed stack: Next.js/TypeScript frontend and maintainable token-based CSS (Tailwind candidate); Java 25 with compatible supported Spring Boot modular monolith; PostgreSQL; S3-compatible storage/CDN; durable queue/workers; provider adapters; structured monitoring. Redis only when measured distributed caching/rate-limit/session needs justify it. Phase 01 pins maintained compatible versions and licenses. [Spring's compatibility reference](https://docs.spring.io/spring-boot/system-requirements.html) supports evaluating Java 25 with current Boot; do not infer all third-party libraries are compatible.

Suggested future workspace: `apps/web`, `services/api`, `services/workers`, `packages/contracts`, `infra`, `docs`. This is a layout proposal, not a Phase 00 scaffold. Next.js handles presentation/server rendering; the Java domain API is the source of authorization/business rules. Avoid independent business logic in two backends. Use OpenAPI contracts, typed clients, versioned schema migrations and consistent validation/error conventions.

Module ownership: identity, users, designers/organizations, portfolios, projects, media, SEO, AI, leads, reviews/trust, billing/entitlements, notifications, analytics, admin and audit. Collaboration/share grants may begin in AI with explicit ownership, then become a module as workflows grow. Cross-module writes use service interfaces; transactions emit an outbox for durable follow-up. Media and AI run as worker processes with the same domain contracts, not premature independent microservices.

Prepare organization/membership, immutable versions, assets/usages/variants, generation lineage, references/masks, grants/feedback, leads/events, review/evidence, entitlements/usage ledger, domains, audit/outbox and analytics projection relationships in Phase 02. Database constraints must prevent cross-tenant relationships. Search and analytics projections are rebuildable. Future extraction of media/AI/search/analytics requires measured scaling/ownership need.

Custom domains (V1.5): verified ownership challenge, unique active domain mapping, DNS guidance, TLS lifecycle, allowlisted host-to-tenant resolution, canonical transition/redirects, revalidation, failure handling and safe removal/reassignment. Never trust arbitrary Host headers or activate unverified domains.

## 31. Development Phases

Release scope and numbered execution phases are separate. The [roadmap](00_IMPLEMENTATION_ROADMAP.md) preserves all Phases 00–29 with dependencies and exit gates; no phase is collapsed.

| Release | Scope |
|---|---|
| V1 | Public website, mobile design system, authentication/onboarding/dashboard, builder, all six separately implemented themes, structured projects/CMS, media library, automatic watermarking/private originals, public portfolios, SEO engine/SEO Center foundation, AI editing/references/color-material workflow/precision where supported/history, discovery/search/filter, WhatsApp/contact, basic lead capture/inbox, admin/security and analytics foundation. Billing/entitlement groundwork supports safe AI cost control. |
| V1.5 | Full client concept approval, mood/material boards, collections/saves/comparison, advanced CRM, verification/reviews, AI portfolio assistant, advanced analytics, custom domains, multilingual UI, team UI, social export, QR portfolio/projects and digital visiting cards. |
| V2 | Quotations, estimates, invoices, client payments, project management, tasks, schedules, full client portal, supplier/material catalogues, visual similarity search and advanced AI recommendations. |
| Future | 2D floor planning, 3D room design, AR, LiDAR/room scanning, contractor/material marketplaces, procurement, advanced costing, native apps, AI auto-selection and duplicate-image moderation assistance. |

V1 private share plumbing is not a claim that V1.5 approvals are complete. Role permission foundations precede V1.5 team accounts. Paid subscriptions are separate from V2 client transaction tooling. V1.5/V2/Future items without a numbered phase get explicit later milestones, never silent omissions. All remain traceable in the matrix.

## 32. Acceptance Principles

### Engineering invariants

1. Understand existing functionality before changing/removing it; preserve working production behavior.
2. Never substitute mocks for production logic, advertise fake APIs as complete or hardcode production business data.
3. Never expose frontend secrets or depend on frontend-only authorization.
4. Do not swallow failures or use empty catches; provide actionable errors without sensitive leakage.
5. Extend safe existing systems rather than duplicate; justify every new dependency and avoid unnecessary microservices.
6. Never trade away security, SEO, mobile usability, accessibility or performance for speed, desktop aesthetics or 3D.
7. Never expose original masters publicly or automatically overwrite generation history.
8. Confirm AI-suggested facts; never promise exact material/color results, rankings or copyright protection from watermarks.
9. No thin SEO spam, theme reskins, undocumented colors, excessive glassmorphism or compressed desktop mobile layouts.
10. TODOs/placeholders are incomplete; only report tests actually run, never weaken controls to pass tests, and assess dependent modules before unrelated changes.

### Phase working method

Before every future implementation phase: read this specification, inspect current repository and previous phase documentation, identify affected modules, dependencies/migrations and security/SEO/mobile/testing impact. After implementation: build, tests, lint/static analysis, inspect errors/warnings, check mobile and desktop, authorization, errors/loading/empty states, accessibility, performance, security and applicable SEO; update documentation with actual evidence. Never claim phase completion with unresolved critical errors. Record inapplicable checks and reasons rather than fabricating passes.

### Release gates

Use tenant-isolation/privileged-auth tests throughout, media privacy/watermark tests before public imagery, crawl/metadata/canonical tests before indexing, mobile/accessibility/performance tests for each theme and workflow, provider-failure/quota/ledger tests before billable AI, and actual restore/webhook exercises before production. No phase's UI completion waives a later integration gate. All six themes and every V1 requirement must be verified before describing the product as full V1.

### Phase 00 gate

Exactly the six required documentation artifacts must exist and agree; all 66 brief sections must map to requirements/process controls; release boundaries, separate themes, watermarking, private originals, AI references, mobile widths, locked colors, pervasive security, core SEO and enhancement-only 3D must remain explicit. No production code is required or authorized in Phase 00. Stop after the Phase 00 completion report.
