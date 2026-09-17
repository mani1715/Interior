# Phase 00 — Requirements Traceability Matrix

2026-09-17. Authority: [master specification](MASTER_PRODUCT_SPEC.md). Requirements cover product behavior, nonfunctional controls and delivery governance. IDs are stable; split rather than silently delete requirements when detail grows. Each row is a major independently trackable obligation; subfields and examples are normative in the master spec, not separate count inflation.

Priority notation: **V1-Must**, **V1.5-Must**, **V2-Planned**, **Future-Planned**, **All-Must**, **P00-Must**. Every V1-Must is required for full V1. All-Must applies throughout. Target phases are execution milestones; V1.5 portions of numbered phases do not become V1 because of the number. Dependencies name modules/contracts or prior phases; `Brief` refers to the supplied requirements. Status **Planned** means not implemented; **Specified** means a governance/Phase 00 documentation obligation is recorded, not that future engineering work passed. Phase 00 completion does not mark product rows complete.

| Requirement ID | Module | Requirement | Priority | Target Phase | Dependencies | Security Impact | SEO Impact | Mobile Impact | Status |
|---|---|---|---|---|---|---|---|---|---|
| PROD-001 | Product | Deliver all eight pillars to all eight professional business types; preserve combined product vision | V1-Must | 01–29 | Brief | Trust boundaries | Core discoverability | Site workflows | Planned |
| ROLE-001 | Identity | Public browsing/search/filter/transformation/contact/share requires no login | V1-Must | 05–06, 20, 25–26 | Public projection | Public fields only | Crawlable work | No login friction | Planned |
| ROLE-002 | Identity | Customer owns saves/compare/collections/enquiries and permitted concept feedback | V1.5-Must | 24, 26–27 | Users, grants | Own records only | Private noindex | Phone review | Planned |
| ROLE-003 | Identity | Professionals manage profile, portfolio, projects, media, AI, leads, SEO, analytics and permitted business settings | V1-Must | 07–10, 18–29 | Tenant policy | Action authorization | Publish authority | Dashboard UX | Planned |
| ROLE-004 | Identity | Organization memberships and future portfolio/project/media/lead manager roles; no permanent single-owner assumption | V1-Must | 02–03 | Identity/domain | Tenant isolation | Publish scopes | Scoped actions | Planned |
| ROLE-005 | Identity | Team invitations, scoped UI, seats and ownership transfer | V1.5-Must | V1.5-TEAM | ROLE-004, billing | Revoke memberships | Publish role safety | Manage on phone | Planned |
| ROLE-006 | Identity | Separate Moderator, Admin and Super Admin scopes with stronger privileged protection | V1-Must | 03, 07, 29 | Audit, MFA | Privilege control | Moderation changes | Usable MFA | Planned |
| MOBILE-001 | Mobile | Design first at 360–430 px; verify 360/390/430/768/1024/1440+ widths on major screens | V1-Must | 04–29 | Design system | Auth UI parity | Mobile usability | Mandatory widths | Planned |
| MOBILE-002 | Mobile | No overflow; touch targets, typography/galleries, safe areas, keyboard inputs and accessible responsive forms | V1-Must | 04–29 | MOBILE-001 | Safe input | Rendering quality | Core ergonomics | Planned |
| MOBILE-003 | Mobile | Intentional mobile navigation/Create actions, sheets/full-screen flows and cards over wide tables | V1-Must | 04, 09 | Roles | Authorized actions | None direct | Thumb workflows | Planned |
| MOBILE-004 | Mobile | Support site uploads/drafts/reference capture/client display with progress, cancellation and interruption recovery | V1-Must | 18–24, 26 | Media, AI | Private drafts | None direct | Weak network | Planned |
| DESIGN-001 | Design | Preserve all 12 locked color values and specified roles; Bronze primary accent, not every button | V1-Must | 04, 11–17 | Brief | Semantic clarity | Consistent brand | Contrast | Planned |
| DESIGN-002 | Design | Verify contrast; Charcoal on Bronze CTA; accessible danger semantics; never color-only states | V1-Must | 04, 17 | WCAG | Error clarity | Accessible content | Sunlight/readability | Planned |
| DESIGN-003 | Design | Evaluate editorial serif/readable sans fonts for licensing, loading, script support and readability | V1-Must | 04 | Locale strategy | License review | Layout/font load | Readability | Planned |
| DESIGN-004 | Design | No random purple/neon/excess glassmorphism; theme-specific controlled typography/tokens | V1-Must | 04, 11–17 | Theme contracts | No custom scripts | Brand/performance | Visual usability | Planned |
| PUBLIC-001 | Public | Provide home/explore/designers/projects/categories/cities/inspiration surfaces | V1-Must | 05–06, 20, 25 | Route contracts | Published only | Public IA | Browse flow | Planned |
| PUBLIC-002 | Public | Home includes transformation hero, projects/spaces, AI demo, two transformation formats, designers, process, cities, CTA/footer | V1-Must | 05 | Content, media | Publish rights | Useful HTML | Lightweight story | Planned |
| PUBLIC-003 | Public | Root studio/project routes reserve namespaces and maintain safe slug aliases | V1-Must | 02, 08, 20 | Slug registry | Tenant resolution | Stable canonical | Stable links | Planned |
| MOTION-001 | Motion | 3D is optional; no critical/indexable content depends on WebGL; static/weak-device/reduced-motion fallbacks | V1-Must | 05, 17 | Performance budget | No data authority | HTML preserved | Low GPU fallback | Planned |
| MOTION-002 | Motion | Justify Motion/Three.js/R3F; gate parallax/material/cabinet/door/drawer transitions on budgets | V1-Must | 04–05 | MOTION-001 | Dependency review | CWV protection | Deferred payload | Planned |
| PORT-001 | Portfolio | Premium studio website with all business/hero/about/services/team/locations/media/trust/process/FAQ/contact/social fields | V1-Must | 08, 10–17 | Tenant, project data | Approved public fields | Business context | Full phone site | Planned |
| PORT-002 | Portfolio | Controlled modular section registry with all specified section types and supported settings | V1-Must | 10 | Schema, design | No executable HTML | Semantic sections | Mobile authoring | Planned |
| PORT-003 | Portfolio | Reorder, show/hide, preview, draft save, publish and DRAFT/PREVIEW/PUBLISHED states | V1-Must | 10 | PORT-002, policy | Private preview | Published only | Touch/reorder alternative | Planned |
| PORT-004 | Portfolio | Immutable version history and non-destructive restore with concurrent-edit protection | V1-Must | 02, 10 | Version schema | Authorized restore | Stable snapshots | Draft recovery | Planned |
| PORT-005 | Portfolio | Validate facts/media/rights at publish; invalidate on unpublish; history cannot resurrect private/deleted assets | V1-Must | 10, 18–20 | Media, SEO | Publication gate | Cache/search updates | Honest status | Planned |
| THEME-001 | Themes | One structured data model and versioned theme/capability registry; retain data when switching | V1-Must | 10, 17 | PORT-002 | Shared policy | Shared SEO contract | Responsive parity | Planned |
| THEME-002 | Themes | Six themes differ in navigation/hero/type/rhythm/cards/grid/gallery/detail/CTA/spacing/motion/images | V1-Must | 11–17 | THEME-001 | Safe renderers | Data consistency | Per-theme QA | Planned |
| THEME-003 | Themes | Separate Basic specification/phase: clean, fast, professional local-business portfolio | V1-Must | 11 | 10, 04 | Public projection | Minimal JS SEO | Simple/fast | Planned |
| THEME-004 | Themes | Separate Modern specification/phase: bold project-first asymmetric presentation | V1-Must | 12 | 10, 04 | Public projection | Structured content | No grid overflow | Planned |
| THEME-005 | Themes | Separate Luxury specification/phase: editorial full-screen storytelling and Bronze restraint | V1-Must | 13 | 10, 04 | Public projection | Text remains HTML | Image budget | Planned |
| THEME-006 | Themes | Separate Architectural specification/phase: technical case studies, plans, process/team/awards | V1-Must | 14 | 10, 04 | Safe drawings | Case study content | Readable plans | Planned |
| THEME-007 | Themes | Separate Warm/Natural specification/phase: residential organic materials/typography | V1-Must | 15 | 10, 04 | Public projection | Shared semantics | Comfortable text | Planned |
| THEME-008 | Themes | Separate Dark Cinematic specification/phase: immersive dark photography with fallback | V1-Must | 16 | 10, 04 | Public projection | No hidden text | Dark contrast | Planned |
| THEME-009 | Themes | Compare shared dataset across all themes and reject color-only reskins | V1-Must | 17 | 11–16 | Preview isolation | Cross-theme crawl | Six widths | Planned |
| PROJECT-001 | Projects | Structured name/slug/studio/location/property/category/style/scope/budget/date/duration/material/service/description facts | V1-Must | 02, 18 | Tenants/taxonomy | Private address split | Rich case studies | Phone CMS | Planned |
| PROJECT-002 | Projects | Cover/gallery/video, transformation joins, tags, SEO, publication and verification fields | V1-Must | 18–20 | Assets, revisions | Asset authorization | Media metadata | Responsive gallery | Planned |
| PROJECT-003 | Projects | Extensible 15-category seed taxonomy plus separate room/work/style/property/service dimensions | V1-Must | 02, 18, 25 | Taxonomy ownership | Admin changes | Discovery relevance | Filter usability | Planned |
| PROJECT-004 | Projects | Before/After and Before → AI Concept → Actual Result storytelling works without animation | V1-Must | 18, 24 | Asset provenance | AI truth labels | Descriptive HTML | Accessible slider/static | Planned |
| MEDIA-001 | Media | First-class assets/variants/usages, not image URL arrays; ownership/checksum/provenance/status | V1-Must | 02, 19 | Storage schema | Tenant media boundary | Reliable delivery | Efficient gallery | Planned |
| MEDIA-002 | Media | Single/bulk uploads, albums, tags/search, ordering, covers, captions/alt text, crop/focal points | V1-Must | 19 | MEDIA-001 | Upload authorization | Alt text/images | Batch/progress UI | Planned |
| MEDIA-003 | Media | Reuse and Before/After association; trash/restore and storage tracking | V1-Must | 19 | Usage references | Reference-aware delete | Broken image prevention | Recovery controls | Planned |
| MEDIA-004 | Media | All 13 specified classification labels with separate kind/usage/visibility authorization | V1-Must | 02, 19 | Policy model | No role-based privacy bypass | Published subset | Clear publish state | Planned |
| MEDIA-005 | Media | AI input/references/client approval assets/originals private; explicit derivative publication only | V1-Must | 19, 21–24 | Storage policy | Core confidentiality | No private indexing | Private preview | Planned |
| MEDIA-006 | Media | Quarantine/validate/process/ready/failure/trash/purge states and idempotent versioned processing | V1-Must | 19 | Queue, outbox | Fail closed | Only ready images | Retry visibility | Planned |
| WM-001 | Watermark | Automatically watermark every public project/portfolio photograph while retaining clean original | V1-Must | 19 | MEDIA-001 | Private master | Public image variants | Efficient delivery | Planned |
| WM-002 | Watermark | Logo/name/both, five positions, size/opacity and preview; visible business-name fallback | V1-Must | 19 | Branding | Prevent invisible bypass | Consistent identity | Preview controls | Planned |
| WM-003 | Watermark | Responsive watermarked CDN variants include covers/thumbs/OG; AI concept also visibly labeled | V1-Must | 19–20, 24 | Processing recipes | No optimizer bypass | Social/image assets | Small variants | Planned |
| WM-004 | Watermark | Logo/recipe versioning, reprocessing/invalidation and fail-closed publication readiness | V1-Must | 19 | Version/jobs | Old asset retirement | Fresh public cache | Status feedback | Planned |
| AI-001 | AI | Edit real site photo using instruction with optional references/selected region, preserving surroundings as far as possible | V1-Must | 21–23 | Private media | Authorized inputs | Private initially | On-site workflow | Planned |
| AI-002 | AI | Provider capability abstraction, server credentials, limits, private job/results and immutable provenance | V1-Must | 21 | API, workers | Secrets/privacy | No accidental index | Async status | Planned |
| AI-003 | AI | Credit reservation/settlement, rate/quota/concurrency/size limits and cost logging/alerts | V1-Must | 03, 21, 28 | Ledger | Abuse/cost control | None direct | Clear costs | Planned |
| AI-004 | AI | Timeout/retry/idempotency/reconciliation, moderation and provider failure recovery | V1-Must | 21 | Queue, ledger | No duplicate charge | None direct | Recoverable failure | Planned |
| AI-005 | AI | History/variants/regenerate/instruction-color-material-handle changes/save without overwriting prior results | V1-Must | 24 | Generation lineage | Authorized history | Publish selected only | Comparison/history | Planned |
| AI-006 | AI | Explicit publish to project/private sharing and retention-aware deletion | V1-Must | 24 | Grants, WM, privacy | Separate publish grant | Labeled eligible concept | Clear decisions | Planned |
| AI-007 | AI | Clearly label AI Concept Visualization; physical colors/materials may differ; no exact reproduction promise | V1-Must | 21–24 | Product copy | Honest provenance | No fake actual work | Visible warning | Planned |
| AI-008 | AI | Evaluate room preservation and reference/mask quality; never silently ignore unsupported controls | V1-Must | 21–23 | Provider evaluation | Informed charging | Honest demos | Capability guidance | Planned |
| REF-001 | References | Color/laminate/wood/marble/material/handle/door/inspiration uploads with type, apply-to and optional instruction | V1-Must | 22 | Media, AI | Private assets | None direct | Phone reference capture | Planned |
| REF-002 | References | Multiple targeted references, explicit precedence over vague text and conflict handling | V1-Must | 22 | REF-001 | Scoped provider payload | None direct | Simple targeting | Planned |
| REF-003 | References | Generic color names prompt shade/reference guidance; never promise exact physical match | V1-Must | 22 | AI-007 | Honest expectations | Honest copy | Inline guidance | Planned |
| MASK-001 | Precision | Brush/erase/rectangle/undo/reset/mask generation and selected-region editing where supported | V1-Must | 23 | Provider masks | Validate mask/input | None direct | Touch editing | Planned |
| MASK-002 | Precision | Preserve oriented coordinates/resolution; test outside-mask preservation and mobile memory | V1-Must | 23 | MASK-001 | Bound resource usage | None direct | Zoom/pan/rotation | Planned |
| COLLAB-001 | Collaboration | Secure private concept shares need not force account; scoped revocable expiring token grants | V1-Must | 24 | Identity, media | Token/media revocation | Noindex/no-store | Simple client entry | Planned |
| COLLAB-002 | Collaboration | Like/comment/approve/request changes tied to immutable concept version and review actor | V1.5-Must | 24, V1.5-COLLAB | COLLAB-001 | Scoped writes/audit | Private feedback | Client review | Planned |
| COLLAB-003 | Collaboration | Mood boards/material boards and controlled sharing | V1.5-Must | V1.5-COLLAB | Assets, grants | Private board scope | Opt-in only | Board browsing | Planned |
| SEO-001 | SEO | SSR/SSG/server-rendered semantic crawlable public HTML with clean URLs and metadata/OG | V1-Must | 05–06, 20 | Published snapshots | No private data | Core indexing | Minimal JS | Planned |
| SEO-002 | SEO | Canonicals, truthful structured data, XML/image sitemaps, robots/index controls and breadcrumbs | V1-Must | 20 | SEO-001 | Draft exclusion | Technical SEO | Clear hierarchy | Planned |
| SEO-003 | SEO | Internal links, alt text, responsive images, CWV and finite crawlable pagination | V1-Must | 19–20, 25 | Media/discovery | Eligible records | Crawl/performance | Fast listings | Planned |
| SEO-004 | SEO | City/category/project architecture with meaningful-content gate; no thin/spam facet pages or ranking guarantees | V1-Must | 06, 20, 25 | Taxonomy/editorial | No invented trust | Anti-duplicate/spam | Useful results | Planned |
| SEO-005 | SEO | SEO Center foundation with understandable health checks, missing descriptions/title/about guidance | V1-Must | 20 | Business/project metadata | Tenant-only dashboard | Improve content | Actionable checklist | Planned |
| SEO-006 | SEO | Authorized Google impressions/clicks/queries, page/portfolio/project traffic and lead attribution | V1.5-Must | V1.5-ANALYTICS | Analytics/OAuth | Connection scope | Real search data | Readable metrics | Planned |
| SEO-007 | SEO | Single canonical host and safe domain/slug changes, unpublish/cache/search/sitemap cleanup | V1-Must | 20 | Route/version events | No stale private page | URL continuity | Stable links | Planned |
| ASSIST-001 | AI Assistant | Suggest category/room/title/description/alt/tags/style/cover with mandatory designer confirmation | V1.5-Must | V1.5-ASSIST | Projects, AI | No unreviewed publish | Useful reviewed copy | Review UI | Planned |
| ASSIST-002 | AI Assistant | Never invent cost/location/material brand/customer/completion facts | V1.5-Must | V1.5-ASSIST | ASSIST-001 | Factual integrity | Truthful metadata | Fact confirmation | Planned |
| DISC-001 | Discovery | Discover real projects first and their creators; filter location/category/room/work/style/budget/property/service/verified | V1-Must | 06, 25 | Projects/search | Published-only filters | Curated landing pages | Fast filter sheets | Planned |
| DISC-002 | Discovery | Search counts/suggestions/pagination obey visibility and reflect publish/unpublish | V1-Must | 25 | Outbox/projection | No private inference | Fresh results | Stable pagination | Planned |
| DISC-003 | Discovery | Customer saves/compares/organizes inspiration collections and shares intentionally with designers | V1.5-Must | 27, V1.5-TRUST | Customer/grants | Collection scope | Private by default | Inspiration boards | Planned |
| LEAD-001 | Leads | Want Similar CTA captures name/phone/location/budget/property/message/reference project/source for owning studio | V1-Must | 26 | Published project | Spam/PII boundaries | Conversion | Phone form | Planned |
| LEAD-002 | Leads | Basic contact/enquiry inbox with customer/contact/requirements/notes/attachments/status/source fields | V1-Must | 26 | Media, identity | Tenant/customer access | Lead attribution | Mobile inbox | Planned |
| LEAD-003 | Leads | NEW/CONTACTED/SITE VISIT/DESIGN/QUOTATION/NEGOTIATION/WON/LOST pipeline and follow-ups | V1.5-Must | 26, V1.5-CRM | LEAD-002 | Private notes | None direct | Cards/actions | Planned |
| LEAD-004 | Leads | Public call/WhatsApp, Ask About Design, Share Concept and Continue on WhatsApp links | V1-Must | 26 | Contact data, shares | No secret leakage | Contact conversions | Native handoff | Planned |
| LEAD-005 | Leads | Isolate future official WhatsApp Business integration behind server-side adapter | Future-Planned | FUTURE | Notifications/provider | Secret/webhook controls | None direct | Message workflow | Planned |
| TRUST-001 | Trust | Confirm ownership/permission before publishing | V1-Must | 18–20 | Publish policy | Content rights | Authentic work | Clear attestation | Planned |
| TRUST-002 | Trust | Report copied/copyright/fake/impersonation/inappropriate content and support moderation/appeals | V1-Must | 18, 27, 29 | Admin/audit | Abuse/takedown | Trust/cleanup | Accessible reporting | Planned |
| TRUST-003 | Trust | Phone/identity/business/address/project verification with explicit evidence-backed states; no fake badges | V1.5-Must | 27 | Private evidence | Evidence privacy | Truthful trust | Clear states | Planned |
| TRUST-004 | Trust | Project/client-linked reviews covering design/quality/communication/timeline/budget/after-sales with abuse controls | V1.5-Must | 27 | Customer/projects | Moderation | Truthful ratings | Review forms | Planned |
| SHARE-001 | Sharing | Portfolio and optional project QR with stable destinations for print/site/social use | V1.5-Must | V1.5-SHARE | Canonical routes | Public data only | QR attribution | Camera access | Planned |
| SHARE-002 | Sharing | Digital visiting card with studio/profession/call/WhatsApp/portfolio/location/social/share profile | V1.5-Must | V1.5-SHARE | Public profile | Published contacts | Share metadata | Mobile-first card | Planned |
| SHARE-003 | Sharing | Branded Instagram portrait/story, WhatsApp status/share-card exports with image/logo/name/QR/URL | V1.5-Must | V1.5-SHARE | Media/WM/QR | Rights/AI labeling | Brand/referrals | Export/share flow | Planned |
| ANALYTICS-001 | Analytics | Foundation for portfolio/project views, approximate unique visitors, WhatsApp clicks and enquiries | V1-Must | 28 | Events, consent | Minimal data | Traffic visibility | Lightweight tracking | Planned |
| ANALYTICS-002 | Analytics | Saved projects, lead conversion, top projects, traffic/Google/QR analysis | V1.5-Must | V1.5-ANALYTICS | Core events, connections | Tenant aggregates | Organic insights | Useful summaries | Planned |
| ANALYTICS-003 | Analytics | Attribution for organic/Instagram/Facebook/direct/QR/portfolio share/platform search/project discovery | V1-Must | 26, 28 | Lead/events | Minimize PII | Source measurement | Low overhead | Planned |
| DASH-001 | Dashboard | Overview/Portfolio/Projects/Media/AI/Leads/Reviews/SEO/Analytics/Billing/Settings navigation with release-aware controls | V1-Must | 09 | Roles/design | No false authority | Private noindex | Separate mobile nav | Planned |
| ADMIN-001 | Admin | Admin coverage for every user/content/AI/lead/trust/billing/SEO/taxonomy/storage/usage/security/moderation/audit area | V1-Must | 29 | Domain modules | Scoped privileged actions | Moderation/indexing | Responsive operations | Planned |
| ADMIN-002 | Admin | MFA/step-up, least privilege, reasoned audit and controlled emergency/role administration | V1-Must | 03, 29 | Identity/audit | Stronger staff controls | Noindex admin | Usable challenge | Planned |
| BILL-001 | Billing | Configurable Free/Professional/AI-credit/Studio capabilities; no finalized prices | V1-Must | 02, 21, 28 | Entitlements | Server checks | Theme/SEO features | Honest usage UI | Planned |
| BILL-002 | Billing | Central project/storage/AI/team/analytics/SEO/domain/theme quotas with transactional reservation | V1-Must | 19, 21, 28 | Usage ledger | Race prevention | No unsafe deletion | Quota feedback | Planned |
| BILL-003 | Billing | Subscription adapter, verified idempotent webhooks, reconciliation, cancellation/refund and downgrade policy before paid launch | V1-Must | 28 | Provider selection | Payment integrity | None direct | Checkout/recovery | Planned |
| DOMAIN-001 | Domains | Custom domain ownership/DNS/TLS/tenant mapping/canonical/SEO/lifecycle | V1.5-Must | V1.5-DOMAIN | 02, 20, 29 | Host/ownership safety | Single canonical | Transparent routing | Planned |
| I18N-001 | Internationalization | Localization-ready data/UI; English first, Telugu priority; future Hindi/Tamil/Kannada/Malayalam/Marathi | V1-Must | 02, 04 | Unicode/fonts | Correct validation | Content language | Script readability | Planned |
| I18N-002 | Internationalization | Multilingual UI with real translated-content SEO/hreflang strategy; no duplicated locale spam | V1.5-Must | V1.5-I18N | I18N-001, SEO | Locale not authority | Correct indexing | Regional UI | Planned |
| ARCH-001 | Architecture | Evaluate compatible Next.js/TypeScript/UI and Java25/Spring/Postgres stack before pinning | V1-Must | 01 | Audit/ADRs | Supported dependencies | Server rendering | Lean frontend | Planned |
| ARCH-002 | Architecture | Modular monolith with identity/users/designers/portfolio/projects/media/SEO/AI/leads/reviews/billing/notifications/analytics/admin/audit boundaries | V1-Must | 01–02 | Domain contracts | Policy ownership | Shared publish events | Consistent APIs | Planned |
| ARCH-003 | Architecture | Durable queue/workers/outbox, replaceable provider adapters, Redis only where justified | V1-Must | 01, 19, 21 | Infrastructure | Idempotent scoped jobs | Reliable cache updates | Async UX | Planned |
| ARCH-004 | Architecture | Media/AI/search/analytics extraction only when scale justifies; no premature microservices | All-Must | 01–29 | Measured demand | Reduce attack surface | Consistent data | Stable APIs | Specified |
| SEC-001 | Security | Secure authentication/session lifecycle/password hashing/verification and privileged MFA | V1-Must | 03, 07 | Identity selection | Account protection | Private routes | Accessible sign-in | Planned |
| SEC-002 | Security | Server authorization, tenant/object RBAC, least privilege and revocation on APIs/jobs/storage/caches | V1-Must | 02–29 | Tenant identity | Cross-tenant defense | Public projection | Parity across clients | Planned |
| SEC-003 | Security | Rate/brute-force limits, input validation/output encoding/SQL safety/CSRF/XSS protection | V1-Must | 03–29 | API/session model | Request safety | Trusted HTML | Safe forms | Planned |
| SEC-004 | Security | Secure headers, strict CORS, request limits, secure cookies and error handling | V1-Must | 03–29 | Web/API | Browser boundaries | Crawl-safe errors | Clear errors | Planned |
| SEC-005 | Security | Server secrets, environment isolation, audit, dependency/secret scanning, verified webhooks and idempotency | V1-Must | 01, 03, 21, 28–29 | CI/operations | Supply chain/secrets | No leaked previews | None direct | Planned |
| SEC-006 | Security | Upload auth/size/extension/MIME/magic/content checks, safe decoding, malware handling, metadata and generated keys | V1-Must | 03, 19 | Quarantine/media | Hostile file defense | Safe derivatives | Bounded upload | Planned |
| SEC-007 | Security | Never trust filename/browser MIME/extension alone or accept arbitrary executable uploads | V1-Must | 19 | SEC-006 | Content validation | Safe assets | Clear rejection | Planned |
| SEC-008 | Security | Short-lived signed private access, restricted CDN/origin policies and no cross-studio media | V1-Must | 19 | Storage/tenant policy | Private originals | No private indexing | Expiry recovery | Planned |
| PRIV-001 | Privacy | Warn about household personal details; AI inputs/references private by default and publish explicit | V1-Must | 19, 21 | Media classifications | Private household data | Deliberate publishing | Clear controls | Planned |
| PRIV-002 | Privacy | Delete, retention, secure share, export, account deletion and privacy-aware logs | V1-Must | 03, 19, 24, 29 | Lifecycle/audit | Data minimization | Purge public records | Account controls | Planned |
| PRIV-003 | Privacy | Provider data-use/retention review; tombstones survive backup restores and history references | V1-Must | 21, 29 | Vendor policy/recovery | Prevent data revival | Deleted page cleanup | Honest deletion status | Planned |
| STORE-001 | Storage | S3-compatible private originals/quarantine/private variants and isolated published derivative CDN | V1-Must | 01, 19 | IAM/vendor | Storage boundary | Fast images | Responsive transfer | Planned |
| STORE-002 | Storage | Versioning/checksums/reference-aware lifecycle and accounting without destroying clean masters | V1-Must | 19 | Assets/billing | Integrity/least privilege | Stable URLs | Restore behavior | Planned |
| BACKUP-001 | Recovery | DB backups/PITR/media durability/versioning, monitoring, periodic restore tests and disaster docs | V1-Must | 01, 29 | Infra/storage | Recovery/integrity | Availability | Availability | Planned |
| BACKUP-002 | Recovery | User-facing trash/restore is distinct from infrastructure backup; measured RPO/RTO | V1-Must | 19, 29 | Lifecycle/backup | Deleted-data safety | Broken-link recovery | User restore | Planned |
| OBS-001 | Observability | Structured logs/correlation/errors/performance/API/DB/queue metrics and tracing | V1-Must | 01, 03, 19, 29 | Runtime/telemetry | Redacted logs | Performance signals | Mobile latency | Planned |
| OBS-002 | Observability | AI latency/failure/cost, storage, auth/rate limits, payments/webhooks and backups alerts with owners | V1-Must | 21, 28–29 | Domain metrics | Anomaly response | Availability | Recoverable service | Planned |
| OBS-003 | Observability | Never log passwords/tokens/secrets or unnecessary personal data | V1-Must | 03–29 | Logging policy | Confidentiality | No private leakage | None direct | Planned |
| PERF-001 | Performance | Responsive optimized thumbnail/watermarked derivatives via CDN; 3,000-image portfolios remain usable | V1-Must | 19, 29 | Media pipeline | No original shortcut | Image SEO/CWV | Low bandwidth | Planned |
| PERF-002 | Performance | Lazy below-fold/LCP priority/dimensions/cache/fonts/code splitting/minimal public JS/deferred 3D | V1-Must | 04–05, 17, 19–20 | Web budgets | Private cache isolation | CWV | Device budgets | Planned |
| PERF-003 | Performance | Measure mobile CWV and payload budgets; document exceptions and field/lab distinction | V1-Must | 04, 17, 29 | Monitoring/fixtures | No unsafe optimization | LCP/INP/CLS | Realistic devices | Planned |
| A11Y-001 | Accessibility | Target WCAG2.2 AA: keyboard/focus/semantics/labels/alt/reduced motion/contrast/screen-reader support | V1-Must | 04–29 | Design/content | Accessible security | Semantic content | Zoom/reflow | Planned |
| A11Y-002 | Accessibility | Accessible dialogs/menus/forms/errors, comparison alternatives, video alternatives and non-drag actions | V1-Must | 04, 10, 17–24 | UI primitives | Clear permissions/errors | Understandable media | Touch/assistive UX | Planned |
| FUTURE-001 | Future | V2 quotations/estimates/invoices/client payments, separate from platform billing | V2-Planned | V2-BUSINESS | Leads/billing/clients | Financial controls | Private documents | Business forms | Planned |
| FUTURE-002 | Future | V2 project management/tasks/schedules/full client portal | V2-Planned | V2-BUSINESS | Tenants/collaboration | Client scoping | Private portal | Site management | Planned |
| FUTURE-003 | Future | V2 supplier/material catalogues, inspiration-image similarity of real projects and advanced AI recommendations | V2-Planned | V2-DISCOVERY | Discovery/AI/media | Provenance/consent | Catalog eligibility | Image upload/search | Planned |
| FUTURE-004 | Future | 2D floor planning, 3D room design, AR, LiDAR and room scanning | Future-Planned | FUTURE | New feasibility/spec | Spatial privacy | Not homepage 3D | Device support | Planned |
| FUTURE-005 | Future | Contractor/material marketplaces, procurement, advanced costing and native apps | Future-Planned | FUTURE | New feasibility/spec | Transaction/privacy | New content policies | Native architecture | Planned |
| FUTURE-006 | Future | AI auto-selection and duplicate-image moderation assistance | Future-Planned | FUTURE | AI/masks/trust | Evidence not proof | Authenticity | Easier selection | Planned |
| GOV-001 | Governance | Inspect full available repository and preserve useful functionality before changes | All-Must | 00–29 | Current inventory | Detect conflicts | Preserve behavior | Preserve behavior | Specified |
| GOV-002 | Governance | Never replace production logic with mocks, fake-complete APIs or hardcoded business data | All-Must | 01–29 | Engineering rules | No bypasses | No fake pages | Honest states | Specified |
| GOV-003 | Governance | No swallowed errors/empty catches; justify dependencies; extend safe systems without duplicates | All-Must | 01–29 | Review/build | Safe failure | Availability | Error recovery | Specified |
| GOV-004 | Governance | Never trade security/SEO/mobile/accessibility/performance for speed or visual effects | All-Must | 01–29 | Quality gates | Mandatory controls | Mandatory SEO | Mandatory usability | Specified |
| GOV-005 | Governance | Do not expose secrets/originals, overwrite history, publish unconfirmed AI facts or promise colors/rankings | All-Must | 01–29 | Domain invariants | Confidentiality/integrity | Truthful SEO | Clear expectations | Specified |
| GOV-006 | Governance | No theme reskins/thin pages/random colors/excess glassmorphism/compressed desktop mobile | All-Must | 04–29 | Design/product brief | No deceptive states | Useful content | Intentional design | Specified |
| GOV-007 | Governance | No TODO completion claims/false test passes/security-disabled tests/unexamined unrelated edits | All-Must | 00–29 | Evidence/review | Preserve controls | Regression safety | Regression safety | Specified |
| GOV-008 | Governance | Before each phase read master/current/prior docs; assess modules/dependencies/migrations/security/SEO/mobile/tests | All-Must | 01–29 | Phase reports | Threat review | SEO assessment | Mobile assessment | Specified |
| GOV-009 | Governance | After each phase build/test/lint/review warnings and device/auth/state/a11y/performance/security/SEO; update docs | All-Must | 01–29 | Actual implementation | Tested controls | Tested rendering | Six-width QA | Specified |
| GOV-010 | Governance | Preserve V1/V1.5/V2/Future boundaries and all Phases00–29; no critical-error completion | All-Must | 00–29 | Roadmap/matrix | Release gates | Release gates | Release gates | Specified |
| GOV-011 | Governance | Create audit/master/matrix/decisions/risks/roadmap, no major feature work in Phase00 | P00-Must | 00 | Supplied brief | Redact secrets | Plan core SEO | Plan core mobile | Specified |
| GOV-012 | Governance | Validate six files/coverage/consistency/invariants, issue structured count/report and stop before Phase01 | P00-Must | 00 | GOV-011 | No unsafe actions | Traceability | Traceability | Specified |

## Counts

Validated total: **145 requirements with 145 unique IDs**. Release/priority distribution: V1-Must 108; V1.5-Must 17; V2-Planned 3; Future-Planned 4; All-Must 11; P00-Must 2. These are grouped obligations, not a claim of only three individual V2 features. Status distribution: 132 Planned; 13 Specified governance/architecture obligations; zero implemented product requirements.

| Major module group | Included modules | Count |
|---|---|---|
| Product and architecture governance | Product, Architecture, Governance | 17 |
| Identity, security and administration | Identity, Security, Admin, Privacy | 19 |
| Design and experience quality | Design, Mobile, Motion, Accessibility, Performance | 15 |
| Public platform and portfolios | Public, Dashboard, Portfolio, Themes, Projects | 22 |
| Media and recovery | Media, Watermark, Storage, Recovery | 14 |
| AI and collaboration | AI, AI Assistant, References, Precision, Collaboration | 18 |
| SEO and discovery | SEO, Discovery, Domains, Internationalization | 13 |
| Leads, trust and sharing | Leads, Trust, Sharing | 12 |
| Business measurement and operations | Analytics, Billing, Observability | 9 |
| Later product expansion | Future | 6 |
| **Total** | | **145** |

## Source-section coverage

The supplied brief has 66 numbered sections. This table maps every source section, including delivery instructions, to stable requirements. It is a coverage aid, not additional counted requirements. Detailed field enumerations remain in the master specification.

| Brief section | Subject | Requirement IDs |
|---|---|---|
| 1 | Vision and pillars | PROD-001 |
| 2 | Primary users | ROLE-001–006 |
| 3 | Mobile first | MOBILE-001–004 |
| 4 | Locked colors | DESIGN-001 |
| 5 | Color usage/contrast | DESIGN-002 |
| 6 | Typography | DESIGN-003–004 |
| 7 | Public website | PUBLIC-001–003 |
| 8 | 3D/motion | MOTION-001–002 |
| 9 | Portfolio Studio | PORT-001 |
| 10 | Builder/version states | PORT-002–005 |
| 11 | Separate theme architecture | THEME-001–002, THEME-009 |
| 12 | Basic | THEME-003 |
| 13 | Modern | THEME-004 |
| 14 | Luxury | THEME-005 |
| 15 | Architectural | THEME-006 |
| 16 | Warm/Natural | THEME-007 |
| 17 | Dark Cinematic | THEME-008 |
| 18 | Structured projects | PROJECT-001–003 |
| 19 | Media library | MEDIA-001–003 |
| 20 | Automatic watermarks | WM-001–004 |
| 21 | Media classifications | MEDIA-004–006, STORE-001 |
| 22 | AI Visualizer | AI-001–002, AI-008 |
| 23 | References | REF-001–002 |
| 24 | Color guidance | REF-003, AI-007 |
| 25 | Precision editing | MASK-001–002, FUTURE-006 |
| 26 | Generation management | AI-005–006 |
| 27 | Before/AI/Reality | PROJECT-004 |
| 28 | Client review | COLLAB-001–002 |
| 29 | SEO engine | SEO-001–004, SEO-007 |
| 30 | SEO Center | SEO-005–006 |
| 31 | AI portfolio assistant | ASSIST-001–002 |
| 32 | Discovery | DISC-001–002 |
| 33 | Visual search | FUTURE-003 |
| 34 | Want Similar | LEAD-001 |
| 35 | Leads/CRM | LEAD-002–003 |
| 36 | WhatsApp | LEAD-004–005 |
| 37 | Collections | DISC-003 |
| 38 | Reviews/trust | TRUST-003–004 |
| 39 | Ownership/reporting | TRUST-001–002, FUTURE-006 |
| 40 | QR | SHARE-001 |
| 41 | Digital card | SHARE-002 |
| 42 | Social export | SHARE-003 |
| 43 | Analytics | ANALYTICS-001–003 |
| 44 | Dashboard | DASH-001, MOBILE-003 |
| 45 | Admin | ADMIN-001–002 |
| 46 | Monetization | BILL-001–003 |
| 47 | Custom domains | DOMAIN-001 |
| 48 | Internationalization | I18N-001–002 |
| 49 | Technical direction | ARCH-001, ARCH-003, STORE-001 |
| 50 | Modular backend | ARCH-002–004 |
| 51 | Security | SEC-001–005 |
| 52 | Upload security | SEC-006–007 |
| 53 | Storage security | SEC-008, STORE-001–002 |
| 54 | AI security/cost | AI-002–004 |
| 55 | Privacy | PRIV-001–003 |
| 56 | Recovery | BACKUP-001–002 |
| 57 | Observability | OBS-001–003 |
| 58 | Performance | PERF-001–003 |
| 59 | Accessibility | A11Y-001–002 |
| 60 | Release scope including boards and later business/spatial capabilities | GOV-010, COLLAB-003, FUTURE-001–006 |
| 61 | Execution phases | GOV-010 |
| 62 | Thirty engineering rules | GOV-001–007, ARCH-004, SEC-001–008 |
| 63 | Working method | GOV-008–009 |
| 64 | Phase00 tasks A–G | GOV-001, GOV-011 |
| 65 | Phase00 validation | GOV-012 |
| 66 | Completion report and stop | GOV-012 |

## Engineering-rule coverage detail

Brief §62 rules 1→GOV-001; 2–4→GOV-002; 5–6→SEC-002/005 and GOV-005; 7–10→GOV-003; 11→ARCH-004; 12–16→GOV-004; 17–21→GOV-005; 22–26→GOV-006; 27–30→GOV-007. Thus grouping governance rows does not discard individual rules. Brief §63 preflight and all 15 post-implementation checks are enumerated in master §32 and enforced by GOV-008/009.

## Update protocol

Product rows remain Planned until implementation evidence is linked. Add In progress, Blocked with reason, Implemented pending verification, or Verified only with truthful evidence. Deferred release scope is preserved with its milestone, never counted as delivered. Governance is continuously applicable even after this documentation passes. Requirements counts and final validation evidence are recorded in the audit after mechanical and semantic checks.

## Phase 01 architecture coverage — 2026-09-17

All 145 requirement IDs, original requirement text, priorities, target phases and implementation statuses above are preserved. The supplemental status below tracks **architecture coverage independently of product delivery**. No product requirement is Complete/Implemented/Verified from documentation. 132 product rows remain Planned and 13 governance/architecture rows remain Specified. The Phase00 audit/counts remain historical. Core architecture decisions are now finalized in ADR-020–036; executable runtime/dependency qualification and feature tests remain future gates.

The scope of later features is prepared through tenant/data/provider/route extension boundaries; their full feature design is not claimed complete. This phase adds no new product requirements and does not merge the six themes or release tiers. Phase01's 69 instruction sections have a separate coverage table in [the testing strategy](01_TESTING_STRATEGY.md).

| Requirement ID | Phase 01 architecture status | Evidence |
|---|---|---|
| PROD-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| ROLE-001 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| ROLE-002 | Extension boundary specified; later feature design pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| ROLE-003 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| ROLE-004 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| ROLE-005 | Extension boundary specified; later feature design pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| ROLE-006 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| MOBILE-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| MOBILE-002 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| MOBILE-003 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| MOBILE-004 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| DESIGN-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| DESIGN-002 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| DESIGN-003 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| DESIGN-004 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| PUBLIC-001 | Architecture specified; implementation pending | [route namespace](01_ROUTE_NAMESPACE.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PUBLIC-002 | Architecture specified; implementation pending | [route namespace](01_ROUTE_NAMESPACE.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PUBLIC-003 | Architecture specified; implementation pending | [route namespace](01_ROUTE_NAMESPACE.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| MOTION-001 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| MOTION-002 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| PORT-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PORT-002 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PORT-003 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PORT-004 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PORT-005 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| THEME-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-002 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-003 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-004 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-005 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-006 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-007 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-008 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| THEME-009 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| PROJECT-001 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PROJECT-002 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PROJECT-003 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| PROJECT-004 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| MEDIA-001 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| MEDIA-002 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| MEDIA-003 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| MEDIA-004 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| MEDIA-005 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| MEDIA-006 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| WM-001 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| WM-002 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| WM-003 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| WM-004 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| AI-001 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| AI-002 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| AI-003 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| AI-004 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| AI-005 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| AI-006 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| AI-007 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| AI-008 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| REF-001 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| REF-002 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| REF-003 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| MASK-001 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| MASK-002 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| COLLAB-001 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| COLLAB-002 | Extension boundary specified; later feature design pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| COLLAB-003 | Extension boundary specified; later feature design pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| SEO-001 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SEO-002 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SEO-003 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SEO-004 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SEO-005 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SEO-006 | Extension boundary specified; later feature design pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SEO-007 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| ASSIST-001 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md) |
| ASSIST-002 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md) |
| DISC-001 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| DISC-002 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| DISC-003 | Extension boundary specified; later feature design pending | [module boundaries](01_MODULE_BOUNDARIES.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| LEAD-001 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| LEAD-002 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| LEAD-003 | Extension boundary specified; later feature design pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| LEAD-004 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| LEAD-005 | Extension boundary specified; later feature design pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| TRUST-001 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| TRUST-002 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| TRUST-003 | Extension boundary specified; later feature design pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| TRUST-004 | Extension boundary specified; later feature design pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md) |
| SHARE-001 | Extension boundary specified; later feature design pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SHARE-002 | Extension boundary specified; later feature design pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| SHARE-003 | Extension boundary specified; later feature design pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| ANALYTICS-001 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| ANALYTICS-002 | Extension boundary specified; later feature design pending | [module boundaries](01_MODULE_BOUNDARIES.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| ANALYTICS-003 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| DASH-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| ADMIN-001 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| ADMIN-002 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| BILL-001 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| BILL-002 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| BILL-003 | Architecture specified; implementation pending | [module boundaries](01_MODULE_BOUNDARIES.md), [security architecture](01_SECURITY_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| DOMAIN-001 | Extension boundary specified; later feature design pending | [route namespace](01_ROUTE_NAMESPACE.md), [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md) |
| I18N-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| I18N-002 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [route namespace](01_ROUTE_NAMESPACE.md) |
| ARCH-001 | Stack decision finalized; executable qualification pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| ARCH-002 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| ARCH-003 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| ARCH-004 | Modular-monolith constraint reaffirmed | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| SEC-001 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| SEC-002 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| SEC-003 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| SEC-004 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| SEC-005 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| SEC-006 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| SEC-007 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| SEC-008 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| PRIV-001 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| PRIV-002 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| PRIV-003 | Architecture specified; implementation pending | [security architecture](01_SECURITY_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| STORE-001 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| STORE-002 | Architecture specified; implementation pending | [media ai architecture](01_MEDIA_AI_ARCHITECTURE.md), [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md) |
| BACKUP-001 | Architecture specified; implementation pending | [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| BACKUP-002 | Architecture specified; implementation pending | [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| OBS-001 | Architecture specified; implementation pending | [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| OBS-002 | Architecture specified; implementation pending | [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| OBS-003 | Architecture specified; implementation pending | [deployment architecture](01_DEPLOYMENT_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| PERF-001 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| PERF-002 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| PERF-003 | Architecture specified; implementation pending | [seo rendering architecture](01_SEO_RENDERING_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| A11Y-001 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| A11Y-002 | Architecture specified; implementation pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [testing strategy](01_TESTING_STRATEGY.md) |
| FUTURE-001 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| FUTURE-002 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| FUTURE-003 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| FUTURE-004 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| FUTURE-005 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| FUTURE-006 | Extension boundary specified; later feature design pending | [production architecture](01_PRODUCTION_ARCHITECTURE.md), [module boundaries](01_MODULE_BOUNDARIES.md) |
| GOV-001 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-002 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-003 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-004 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-005 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-006 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-007 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-008 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-009 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-010 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-011 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |
| GOV-012 | Phase01 process coverage recorded; future obligations continue | [testing strategy](01_TESTING_STRATEGY.md), [production architecture](01_PRODUCTION_ARCHITECTURE.md) |

## Phase 02 data-model coverage — 2026-09-17

All **145 requirements assessed and mapped**, separately from Phase01 architecture and product implementation. Coverage includes concrete entities, configuration/query contracts, explicit deferred extension paths and governance obligations. Counts: 96 Data model specified; 24 Deferred extension path; 13 Configuration / query contract; 12 Governance / no new persistence. No product implementation status changed: **132 Planned,13 Specified,zero implemented**. The [domain model](02_DOMAIN_MODEL.md) explains release seams; the [schema dictionary](02_DATABASE_SCHEMA.md) defines all94 tables (84V1,10V1.5); [ownership/security](02_DATA_OWNERSHIP_SECURITY.md), [lifecycles](02_ENTITY_LIFECYCLES.md), [taxonomy](02_TAXONOMY_STRATEGY.md), [queries](02_INDEX_QUERY_STRATEGY.md) and [migrations](02_MIGRATION_STRATEGY.md) provide constraints and later qualification gates. An entity mapping is not proof the full UI/feature is implemented.

| Requirement ID | Phase 02 data coverage | Concrete entity / configuration / extension path |
|---|---|---|
| PROD-001 | Data model specified | All domain aggregates; eight BUSINESS types; V1.5/V2/Future extension seams in domain model§2/8. |
| ROLE-001 | Data model specified | Published root/revision projections and RLS public-reader contract; anonymous enquiry/report commands. |
| ROLE-002 | Deferred extension path | users.users, customer-owned collection/items/grants, own-lead DTO, exact review items and later feedback. |
| ROLE-003 | Data model specified | studio_members action policy across owner modules; tenant-qualified FK and RLS matrix. |
| ROLE-004 | Data model specified | studio_members unique(studio,user), active OWNER invariant; platform roles separate. |
| ROLE-005 | Deferred extension path | Existing memberships/usage accounts; V1.5 hashed invitation extension and locked ownership transfer. |
| ROLE-006 | Data model specified | roles/user_roles, sessions assurance/step_up_at, admin.access_cases and audit_events. |
| MOBILE-001 | Configuration / query contract | No new persistence; responsive implementation constraint; bounded summary/cursor read contracts. |
| MOBILE-002 | Configuration / query contract | No new persistence; form validation and safe public media DTO fields, captions/alt text. |
| MOBILE-003 | Configuration / query contract | No new persistence; server role/action responses, feature flags and navigation config. |
| MOBILE-004 | Configuration / query contract | Draft versions, upload_sessions, idempotency_keys, processing_jobs, AI cancel/status/lineage. |
| DESIGN-001 | Configuration / query contract | Locked palette stays code/theme registry; theme_config stores only approved option IDs. |
| DESIGN-002 | Configuration / query contract | No dedicated table; accessible settings registry; watermark bounds don't imply text contrast passes. |
| DESIGN-003 | Configuration / query contract | No font table; licensed application typography registry with allowed config IDs. |
| DESIGN-004 | Configuration / query contract | theme_config versioned supported options; no executable CSS/JS/HTML. |
| PUBLIC-001 | Data model specified | Published portfolio/project roots, curated landing_pages, public search documents. |
| PUBLIC-002 | Data model specified | Portfolio sections, transformations and approved media; home editorial composition in code. |
| PUBLIC-003 | Data model specified | studio_slug_claims reserved/current/alias/retired; project_slug_claims tenant uniqueness. |
| MOTION-001 | Configuration / query contract | No new table; feature_flags and controlled theme motion option; equivalent static data retained. |
| MOTION-002 | Configuration / query contract | No new table; dependency/performance gate and approved presentation settings. |
| PORT-001 | Data model specified | Business snapshot plus contacts/services/areas, full section registry; typed logo/favicon/media links; deferred real review widgets. |
| PORT-002 | Data model specified | portfolio_sections type/schema/order/visibility + strict content/settings; media/project relational links. |
| PORT-003 | Data model specified | portfolios draft/live pointers, version row locking, bound preview grants. |
| PORT-004 | Data model specified | portfolio_versions immutable published payload/children; new-draft restore; M optimistic version. |
| PORT-005 | Data model specified | publication_attestations, live root/media gates, publication_epoch, audit/outbox and deletion tasks. |
| THEME-001 | Data model specified | One portfolio version/section schema, registry theme/schema/renderer versions. |
| THEME-002 | Data model specified | Shared version data; layout difference is renderer contract, no duplicate tables. |
| THEME-003 | Data model specified | BASIC registry/config applied to same version/section/project model. |
| THEME-004 | Data model specified | MODERN registry/config applied to same version/section/project model. |
| THEME-005 | Data model specified | LUXURY registry/config applied to same version/section/project model. |
| THEME-006 | Data model specified | ARCHITECTURAL registry/config, structured case facts, media DRAWING pipeline gate. |
| THEME-007 | Data model specified | WARM_NATURAL registry/config applied to shared content. |
| THEME-008 | Data model specified | DARK_CINEMATIC registry/config and controlled motion; same public media gates. |
| THEME-009 | Data model specified | Shared synthetic fixture/revision data; design distinction tested in17, not a persisted QA feature. |
| PROJECT-001 | Data model specified | project_revisions normalized facts, typed project_terms, private details, money/date checks. |
| PROJECT-002 | Data model specified | project_media, transformations/items, project terms, revision SEO; independent verification later. |
| PROJECT-003 | Data model specified | taxonomy_terms distinct CATEGORY/ROOM/WORK/STYLE/PROPERTY/SERVICE,15 category seeds. |
| PROJECT-004 | Data model specified | project_transformations/items scoped to same revision; BEFORE/AI_CONCEPT/AFTER with immutable provenance. |
| MEDIA-001 | Data model specified | media_assets/recipes/variants and typed usage joins; original object identity not URLs. |
| MEDIA-002 | Data model specified | upload_sessions, albums/items, tags/asset_tags, project/portfolio media captions/alt/order/focal/crop. |
| MEDIA-003 | Data model specified | Typed reverse-reference view, D trash/purge_after, deletion tasks and usage ledger. |
| MEDIA-004 | Data model specified | All13 labels mapped to provenance/typed usage/privacy in domain model§4. |
| MEDIA-005 | Data model specified | Original private zone; private_only reference/mask/evidence; review grants and explicit public manifest. |
| MEDIA-006 | Data model specified | Assets/upload/recipe/variant/job states, checksum and dedup recipe, fenced processing_jobs. |
| WM-001 | Data model specified | Private source assets; versioned recipes/variants and live public eligibility. |
| WM-002 | Data model specified | watermark_settings/versions logo/name/mode/five positions/opacity/scale; mandatory fallback. |
| WM-003 | Data model specified | Responsive variant tuples, watermark_verified/ai_label_verified; public covers/OG same gate. |
| WM-004 | Data model specified | Versioned logo/recipe/config, active_public_recipe pointer, processing jobs and invalidation outbox. |
| AI-001 | Data model specified | ai_generations input/instruction/mask/parameters; multiple typed references. |
| AI-002 | Data model specified | ai_attempts model/provider/account/env/submission keys; private media output provenance; no secrets. |
| AI-003 | Data model specified | usage_accounts/reservations/ledger, rate buckets and bounded provider cost metadata. |
| AI-004 | Data model specified | Durable intent, RECONCILIATION_REQUIRED, callbacks inbox/unique keys, cancellation and reconciliation evidence. |
| AI-005 | Data model specified | parent_generation_id, immutable request/outputs, variant_number; regenerate creates new job. |
| AI-006 | Data model specified | Typed project AI_CONCEPT media and client_review_sessions/items; D retention plus deletion workflow. |
| AI-007 | Data model specified | AI_GENERATED provenance and recipe ai_label_version; caveat copy is application registry. |
| AI-008 | Data model specified | Persist model/version/capability-approved params; quality/capability evaluation is feature gate, not a fake result table. |
| REF-001 | Data model specified | ai_generation_references(type,subtype,apply_target,instruction,asset_id,checksum). |
| REF-002 | Data model specified | unique(generation,position), no unique(type); explicit precedence parameters, multiple same type different targets. |
| REF-003 | Data model specified | No separate table; required guidance copy/capability rules backed by private reference model. |
| MASK-001 | Data model specified | Private mask asset; generation mask checksum/dimensions/convention/coordinate_version; editing undo transient client state. |
| MASK-002 | Data model specified | Input checksum and oriented dimensions tie mask to source; quality/memory tests later. |
| COLLAB-001 | Data model specified | client_review_sessions hashed expiring/revocable scope; client_review_items and live media gateway. |
| COLLAB-002 | Deferred extension path | Deferred concept_feedback immutable LIKE/COMMENT/APPROVE/REQUEST_CHANGE per review item and actor. |
| COLLAB-003 | Deferred extension path | Explicit later board/item/private media/grant seam; no premature board table. |
| SEO-001 | Data model specified | Revision-local title/description/OG usages, published projections; rendering is code. |
| SEO-002 | Data model specified | Trusted slug/domain registry canonical derivation, robots preference+live eligibility; sitemap from live roots. |
| SEO-003 | Data model specified | Typed alt/responsive variants and bounded crawlable/keyset query contract. |
| SEO-004 | Data model specified | landing_pages explicit curated path/content/quality_rule_version, typed geography/taxonomy. |
| SEO-005 | Data model specified | Rule-versioned computed health from required facts; no persistent fabricated rank/score. |
| SEO-006 | Deferred extension path | Later scoped encrypted Google connection/import extension; current analytics/attribution anchors. |
| SEO-007 | Data model specified | Permanent alias claims, domain registry extension, publication_epoch and outbox/deletion receipts. |
| ASSIST-001 | Deferred extension path | Later suggestion batch binds exact revision/field/evidence; human confirmation into existing draft and audit. |
| ASSIST-002 | Deferred extension path | Existing immutable facts/attestations; later suggestion must not invent unknown values. |
| DISC-001 | Data model specified | Public search documents plus typed project terms/geography/budgets; current verification gate when later introduced. |
| DISC-002 | Data model specified | Projection aggregate_version, live source gate for hits/counts/facets, outbox and scoped pagination. |
| DISC-003 | Deferred extension path | Deferred collections/items/grants: customer owner independent of saved project's studio; project-focused. |
| LEAD-001 | Data model specified | leads with contacts/location/budget/property/reference-project tenant FK; source attribution. |
| LEAD-002 | Data model specified | leads, lead_notes, lead_attachments, lead_status_history; own-customer DTO omits internals. |
| LEAD-003 | Deferred extension path | Full pipeline+follow_up_at/zone+assignee FK; advanced actions/reminders V1.5. |
| LEAD-004 | Data model specified | Published consented studio contacts and safe link composition; no token/PII URL persistence. |
| LEAD-005 | Deferred extension path | Future notifications channel adapter, webhook/idempotency contract; no Business API secrets in DB. |
| TRUST-001 | Data model specified | publication_attestations exact project revision+actor+policy version, immutable. |
| TRUST-002 | Data model specified | content_reports typed XOR target and tenant FKs; report_actions appeals/history/audit. |
| TRUST-003 | Deferred extension path | Deferred verification_requests/evidence, type/status/expiry, private-only media and live badge. |
| TRUST-004 | Deferred extension path | Deferred reviews/revisions/dimensions, customer/project linkage and approval history. |
| SHARE-001 | Deferred extension path | Later QR rendering uses stable canonical owner/slug/domain, source QR code analytics; no new table necessary. |
| SHARE-002 | Deferred extension path | Later digital card composes approved public business snapshot/contact/service areas. |
| SHARE-003 | Deferred extension path | Later export recipe uses existing media checksum/watermark/AI label contracts; no bypass. |
| ANALYTICS-001 | Data model specified | Bounded analytics_events/daily_metrics, event ID/source uniqueness, consent/session digest minimization. |
| ANALYTICS-002 | Deferred extension path | Later collection and lead-history metrics plus authorized Google imports; current raw/aggregate foundation. |
| ANALYTICS-003 | Data model specified | lead_attribution SOURCE taxonomy, safe campaign/entry/referrer fields; analytics rule versions. |
| DASH-001 | Configuration / query contract | No dedicated table; permission-scoped module summaries and feature flags; bounded mobile query plan. |
| ADMIN-001 | Data model specified | Owner aggregates queried through case-scoped facades; no duplicate admin copies. |
| ADMIN-002 | Data model specified | sessions assurance/step-up, roles/user_roles, access_cases and append-only audit. |
| BILL-001 | Data model specified | Versioned plans/entitlements/plan_entitlements with strict typed values; prices unselected. |
| BILL-002 | Data model specified | usage_accounts/reservations/ledger for credit periods and lifetime occupancy; atomic holds. |
| BILL-003 | Data model specified | subscriptions/payment_transactions/provider-scoped webhook inbox; reconciled transitions and append ledger. |
| DOMAIN-001 | Deferred extension path | Deferred custom_domains typed tenant registry, active unique normalized host/challenge/expiry/generation; canonical derived. |
| I18N-001 | Data model specified | Unicode labels/content, locale/timezone fields, typed currency; later taxonomy translation extension. |
| I18N-002 | Deferred extension path | Actual translated revision relationship/UI resources/hreflang later; no duplicate content table now. |
| ARCH-001 | Data model specified | PostgreSQL18 uuidv7 compatibility decision; Flyway/test toolchain qualification pending. |
| ARCH-002 | Data model specified | Module-owned schemas and typed relationships retain Phase01 facade DAG. |
| ARCH-003 | Data model specified | outbox_events/dispatches/consumer_receipts, fenced jobs, provider inboxes; no Redis authority. |
| ARCH-004 | Data model specified | Owner contracts and rebuildable projection seams; no microservice/shard tables. |
| SEC-001 | Data model specified | external_identities, sessions/login_transactions, roles/grants; no raw credentials; provider decision03. |
| SEC-002 | Data model specified | Tenant composites, T/U/C/R policy classes, current membership and worker fences. |
| SEC-003 | Data model specified | rate_limit_buckets, bounded validated text/JSON; query parameterization/CSRF policy not SQL-only. |
| SEC-004 | Data model specified | Session hash/expiry and error mapping; header/CORS policy in application, no extra tables. |
| SEC-005 | Data model specified | Scoped audit/idempotency/provider webhook inbox; secrets only managed configuration. |
| SEC-006 | Data model specified | Upload sessions/quarantine/state/checksum/type/dimensions/jobs; decoder qualification later. |
| SEC-007 | Data model specified | Detected MIME/checksum/version authoritative; filenames metadata only. |
| SEC-008 | Data model specified | Private storage identity, live share scopes and tenant FKs; public derivative manifest only. |
| PRIV-001 | Data model specified | Private-only source/reference/mask/evidence classification and explicit approved usages; consent guidance config. |
| PRIV-002 | Data model specified | D trash, deletion_requests/tasks/tombstones, private grants, retention matrix and export workflow. |
| PRIV-003 | Data model specified | Independent durable deletion journal, provider attempt/cleanup receipts, reference-aware restore. |
| STORE-001 | Data model specified | Private original storage zones and approved separate variant objects; no public URL authority. |
| STORE-002 | Data model specified | Pinned object versions/checksums, immutable recipes, reverse usage refs and usage reservations. |
| BACKUP-001 | Data model specified | PITR/restore roles and complete-tenant checks; DB-object/provider reconciliation; no fake backup success table. |
| BACKUP-002 | Data model specified | D recovery fields distinct from deletion tombstones/infrastructure recovery drill. |
| OBS-001 | Data model specified | Job/attempt/outbox request IDs/status/lease metadata; telemetry sink external, no raw log warehouse. |
| OBS-002 | Data model specified | Reconciliation deadlines/costs/usage/status query paths; operational alerts configured, not fake rows. |
| OBS-003 | Data model specified | Bounded safe event payloads, encrypted sensitive fields, classified field allowlist for redaction. |
| PERF-001 | Data model specified | Finite responsive variants, typed galleries, batch cover reads, bounded pagination for3000 images. |
| PERF-002 | Data model specified | Published version/manifest cache identity; layout/font/JS behavior application contract. |
| PERF-003 | Data model specified | Query/index/EXPLAIN test plan and documented budgets; no measurement claim. |
| A11Y-001 | Configuration / query contract | Alt/caption and structured sections support semantic output; accessibility implementation remains later. |
| A11Y-002 | Configuration / query contract | Video/caption extension through typed media pipeline; ordered semantic transformations and non-drag API reorder. |
| FUTURE-001 | Deferred extension path | V2 separate tenant financial documents/lines/client-payments anchored lead/project; not platform subscriptions. |
| FUTURE-002 | Deferred extension path | V2 operations/tasks/schedules/client grants anchored stable project root, not portfolio revision. |
| FUTURE-003 | Deferred extension path | Later supplier/product link from MATERIAL; consented media similarity projection with deletion hooks. |
| FUTURE-004 | Deferred extension path | Later spatial asset/job schema after feasibility; reuse private media/tenant identity. |
| FUTURE-005 | Deferred extension path | Later procurement/marketplace/native spec; reuse tenant identity/provider boundaries without invented tables. |
| FUTURE-006 | Deferred extension path | Later mask/evidence recipe with model provenance and human review; never automatic rights proof. |
| GOV-001 | Governance / no new persistence | All14 existing documents reread; preserved existing requirement rows and unrelated file hashes. |
| GOV-002 | Governance / no new persistence | Blueprint only, no fake application/schema endpoints or seeded professionals. |
| GOV-003 | Governance / no new persistence | Normalized typed contracts; explicit defaults/FKs and error/reconciliation paths; no duplicate per-theme tables. |
| GOV-004 | Governance / no new persistence | Publication/security/mobile/SEO query constraints and future runtime gates retained. |
| GOV-005 | Governance / no new persistence | Private source/secret boundaries, immutable history/confirmed publication and truth labels. |
| GOV-006 | Governance / no new persistence | Theme registry and curated landing rules; actual visual QA still separate. |
| GOV-007 | Governance / no new persistence | Only document checks claimed; no unexecuted SQL/RLS or production completion claim. |
| GOV-008 | Governance / no new persistence | Prior docs and112-section Phase02 instruction reviewed; dependencies/privacy/SEO/mobile test impact recorded. |
| GOV-009 | Governance / no new persistence | Document structural/semantic validation applicable; builds/browser/runtime checks not run with absent app. |
| GOV-010 | Governance / no new persistence | 84V1/10V1.5 proposed table scope; other future seams explicit; stop before03. |
| GOV-011 | Governance / no new persistence | Historical Phase00 six-document obligation preserved; no extra unrelated files. |
| GOV-012 | Governance / no new persistence | 145 original IDs/statuses preserved; Phase02 report, user review gate respected. |
