# Phase 01 — SEO, Rendering, Caching and Publication Architecture

Status: selected design, not indexed pages. Requirements: SEO-001–007, PORT-003–005, PUBLIC-001–003, DOMAIN-001, PERF-001–003, MOTION-001–002, A11Y-001–002. [Routes](01_ROUTE_NAMESPACE.md), [media](01_MEDIA_AI_ARCHITECTURE.md) and [module ownership](01_MODULE_BOUNDARIES.md) are normative dependencies.

## 1. Rendering policy

Next.js App Router server components render meaningful public content, navigation, project descriptions and links in initial server HTML. Client components are isolated to interactive filters, galleries, comparison controls, masks and builder workflows. No top-level `use client` converting the product into a SPA. Metadata and structured data are server-produced. Public HTML requests explicitly omit user cookies from backend calls so caches cannot mix user state into a public projection.

| Page family | Initial rendering | Cache choice | Index policy |
|---|---|---|---|
| Studio portfolio | SSR current live pointer + immutable snapshot + live eligibility overlay | HTML no shared cache initially; public versioned content cache allowed only behind live gate | Eligible published portfolio only |
| Project detail | SSR current published project revision with theme presentation | Same live gate; immutable revision lookup reusable | Canonical published project; real/concept facts labeled |
| Category/city/service-city | Server-rendered curated content plus live eligible result set | No shared HTML cache initially; bounded public query projection caches with live eligibility filtering | Index only approved meaningful landing pages |
| Discovery defaults | SSR public projects/designers and crawlable pagination | Bounded query computation/read projections; no private fields | Meaningful defaults index; arbitrary search/facets noindex |
| Home/editorial | SSR initially; optional static rendering for pages that do not include changing private/public publication state | Build assets long cached; HTML policy reviewed per page | Core sections/text must work without JS/WebGL |
| Dashboard/account/admin | Private server shell with richer client interactions | private,no-store everywhere | noindex; authorization is mandatory regardless of robots |
| Preview/private review | Authorized SSR plus interactive review | private,no-store, no shared cache or third-party scripts | noindex,noarchive where supported; token-safe referrer policy |

SSR-first is deliberately conservative for tenant revocation and per-response CSP nonces. It remains SEO-first; SSG/ISR is not inherently required for SEO. Before introducing shared HTML/ISR, prove host/locale/version keys, invalidation across replicas and a fresh eligibility gate that cannot serve withdrawn snapshots; otherwise retain SSR. Next self-hosting needs explicit distributed-cache coordination when scaling ([official guide](https://nextjs.org/docs/app/guides/self-hosting)). No undocumented reliance on framework default caching semantics.

## 2. Published projection and metadata

Backend supplies a minimal typed public page model: tenant/public business info, selected project revisions, section data, theme ID/version, public derivative manifests, provenance, canonical host/path, locale, breadcrumbs, SEO fields and eligible trust facts. Do not pass domain entities, draft columns, original keys or signed private URLs to React even if not displayed. Theme renderer consumes the same model for all six themes; SEO semantics are not separately reinvented per theme.

Resolve title/description from designer-confirmed fields with safe text fallbacks, never invented costs/brands/location. Generate Open Graph/Twitter metadata using only approved watermarked images; AI imagery includes its label. Use context-appropriate Organization/LocalBusiness/CreativeWork/ImageObject/BreadcrumbList schema only when supported by visible truthful content and current search guidelines; no guarantee of rich results and no fabricated AggregateRating. Escape JSON-LD safely so user text cannot close a script tag. Full schema selection is validated in20 against official documentation, not bulk mark-up spam.

SEO Center health is a deterministic documented rule set with actionable checks, not a ranking forecast. Search Console data is V1.5 via authorized connection; unavailable external metrics display unavailable, never simulated numbers.

## 3. Canonicals, facets, pagination and crawlers

Use trusted registry-derived absolute canonical URLs, not arbitrary request Host. One canonical studio/project path, with approved aliases redirecting. Query tracking parameters do not create new indexable identities; canonical removes them. Arbitrary filter/search combinations are noindex by default; do not canonicalize materially different content to an unrelated page merely to suppress indexing. Curated city/category routes require useful original copy and sufficient eligible real work; empty combinations are absent/noindex, never generated en masse. Google explains [faceted crawl management](https://developers.google.com/crawling/docs/faceted-navigation) and [scaled content abuse](https://developers.google.com/search/docs/essentials/spam-policies).

Public page1 uses base canonical; valid page2+ have distinct self-canonicals and useful crawlable next/previous links. Infinite scroll enhances these links, never replaces them. Out-of-range empty pages404; bounded query complexity and no infinite URL combinations. Sorting/filter parameters have stable allowlists. Do not block robots from crawling a page that needs to expose noindex during removal. Robots.txt is a crawler hint, not privacy protection.

Drafts/auth/admin/client grants/private results are noindex and protected. Public 404/410 do not emit successful project structured data; service outage503 does not masquerade as deletion. SEO extraction tests fetch HTML with JS disabled, verify core text and links, check canonical/noindex/sitemap and social bot metadata. Include legitimate long descriptions/Telugu text and all six theme renderers. Metadata API usage alone does not prove crawlability.

## 4. Publication transaction and downstream consistency

Public pointer changes commit atomically with audit/outbox under the tenant publication/reference lock. Exact immutable project references and watermark recipe eligibility are validated. No HTTP request waits for AI generation or image processing to make a draft publishable. Project changes do not silently mutate historical portfolio snapshots; their pinned revision behavior and security overrides are specified in module boundaries.

| Change | Authoritative synchronous effect | Idempotent asynchronous follow-up |
|---|---|---|
| Portfolio published/updated | New snapshot/live pointer and public generation version | Refresh public projection, metadata inputs, sitemap/search and optional prewarm |
| Project published/updated | New current project revision; old portfolio snapshot refs unchanged | Update project SEO/discovery; mark affected portfolio draft as update available |
| Business presentation update | Draft changed unless explicitly publishing profile change | On publish, refresh all affected canonical page metadata; do not leak unreviewed facts |
| Studio/project slug rename | Registry claim+alias+current path transaction | Canonical/sitemap/internal link/search refresh and cache invalidation |
| Unpublish/suspension/moderation | Live eligibility denies affected content immediately after commit | Evict caches, remove sitemap/search references, revoke derivative delivery and track CDN purge |
| Watermark/config change | New recipe work requested; existing approved recipe remains until replacement ready | Generate variants, atomically replace manifests, invalidate retired URLs |
| Custom-domain activation/removal | Verified registry primary host/version transition | Redirect/canonical/sitemap/cache updates and monitoring |

Outbox backlog is observable; missing invalidation cannot allow private records because SSR/public read gates are authoritative. Search/sitemap projections may lag but their serving query checks live eligibility; no stale withdrawn names/descriptions/media in public output. Sitemap workers do not read drafts. Caches are hints, never final access control.

Cache key contract for versioned public data: verified tenant ID + content/version + theme-renderer version + locale + host-registry generation + relevant taxonomy/publication generation. Do not cache arbitrary user-supplied host/query strings without normalization and allowlists. Concurrent deployment versions use distinct namespaces. Invalidation consumers compare versions and ignore obsolete events; periodic reconciliation detects missed events. Admin/preview/auth/share routes never use these public caches.

## 5. Sitemaps, images, redirects and custom domains

Sitemap index references bounded shards generated from live eligible pages; honest `lastmod` is content publication time, not every request. Regenerate by outbox and scheduled reconciliation; publish validated complete artifacts atomically. Start serving from live-filtered backend/SSR endpoint for correctness; static sitemap shards later require equivalent removal handling. Image sitemaps use watermarked derivative URLs only where useful. No signed URL, token path, dashboard or private original in sitemaps.

Slug aliases point directly to current canonical; no cross-tenant redirects, cycles or long redirect chains. Use404 for unknown/temporary withdrawal and410 for approved permanent public deletion. Never recycle old studio aliases to a different professional automatically. [Route namespace](01_ROUTE_NAMESPACE.md) defines normalization/Unicode/reservations.

Custom-domain architecture is V1.5: verified host → tenant → primary published portfolio. Primary selection controls canonicals, sitemap URLs and platform alias redirects. Default platform URLs continue as aliases; during unverified/provisioning state they remain canonical. On domain loss, suspend binding and safe fallback to platform without redirecting to the lost domain. CDN/Next caches must include domain registry generation. Domain-specific robots/sitemaps include only that tenant. No dashboard session cookie is shared with arbitrary custom hosts.

## 6. Image and mobile performance

Serve precomputed responsive derivatives with srcset/sizes and intrinsic width/height. AVIF/WebP negotiated via explicit picture sources and compatible fallback; CDN Vary/cache policy must match format selection. Prioritize only true hero/LCP image, lazy-load below fold, limit gallery DOM and paginate bulk media. Original masters never become a performance shortcut. HTTP caching for public derivatives is versioned but revocable under the media policy; private images no-store. Fonts selected/licensed in04 with subset/fallback metrics and regional script coverage.

Budgets carried from master: mobile p75 LCP≤2.5s, INP≤200ms, CLS≤0.1; initial public JS≤150KiB compressed and above-fold images target≤500KiB. Additional architecture targets: warm public SSR API projection p95≤200ms and total origin TTFB target≤800ms under representative load; measure before release, don't claim achieved. Mobile tests at360/390/430 and progressive768/1024/1440+; cached data must not trade away private access safety to hit latency.

Optional3D loads only after critical content/LCP and user capability/intent decision, optimized models/textures, desktop deferred asset budget target≤2MiB compressed, explicit memory/frame-rate benchmark in05. No preload competing with hero, no text rendered only in canvas. On reduced motion, save-data/weak-device policy or WebGL failure render equivalent static sequence. Mobile default is lightweight/static; opt-in effects must pass budgets. CSS/3D feature flag off leaves complete indexable narrative, not a blank hero.

## 7. SEO security and acceptance

No unconfirmed AI project facts, fake reviews/verification, exact ranking promises, thin pages or duplicated language pages. Reporting/moderation can withdraw public work while preserving audit/evidence privately. User-provided text is encoded; controlled rich text/section configuration cannot inject HTML/CSS/JS. Robots and canonical are not security mechanisms.

Before public launch, prove publication/unpublication across each theme, CDN and search; data-only cache tenant separation; custom-host poisoning rejection; bad slug/error semantics; no-JS crawl; six-width/performance/a11y tests; and watermark/AI label visibility for cover/thumb/OG. Until those checks run, this document establishes architecture coverage, not SEO success or ranking.
