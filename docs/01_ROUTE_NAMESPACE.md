# Phase 01 — URL and Route Namespace

Status: route contracts only, no routes implemented. Requirements: PUBLIC-001–003, SEO-001–007, DOMAIN-001, I18N-001–002, ROLE-001–006, COLLAB-001, DASH-001. [SEO rendering](01_SEO_RENDERING_ARCHITECTURE.md) owns indexing; [security](01_SECURITY_ARCHITECTURE.md) owns access. Route groups in Next.js are organizational and never substitute for authorization.

## 1. Platform host route ownership

Exact/static reserved routes take precedence over `/{studioSlug}`. Backend uses the same reserved registry for slug creation and rename; frontend routing is not the sole conflict check.

| Route family | Owner | Access and index policy |
|---|---|---|
| `/` | Public web/editorial | Public server HTML, index |
| `/explore`, `/designers`, `/projects` | Discovery | Public; curated default index, arbitrary search/filter variants noindex |
| `/categories`, `/categories/{categorySlug}` | Discovery/SEO | Public; meaningful-content eligibility required |
| `/cities`, `/cities/{citySlug}` | Designers taxonomy/SEO | Public; editorial eligibility required |
| `/inspiration` | Discovery/editorial | Public useful work, not private customer collections |
| `/interior-designers/{citySlug}` | SEO/discovery | Curated service-city landing page |
| `/tv-unit-designers/{citySlug}` | SEO/discovery | Curated work-city landing page |
| `/modular-kitchen-designers/{citySlug}` | SEO/discovery | Curated work-city landing page |
| `/{studioSlug}` | Portfolios | Published live snapshot, index if eligible |
| `/{studioSlug}/projects/{projectSlug}` | Projects/portfolio renderer | Published project revision and live eligibility gate |
| `/auth/sign-in`, `/auth/sign-up`, `/auth/recovery`, `/auth/verify` | Identity web | Noindex/no-store; backend OIDC actions under `/auth/oidc/*` |
| `/auth/oidc/start`, `/auth/oidc/callback`, `/auth/logout` | Spring identity | Edge routes directly to API; callback bound to provider/state; logout POST |
| `/login`, `/signup` | Identity aliases | Fixed redirect to canonical auth UI; reserved permanently |
| `/dashboard` | Professional app | Authenticated; noindex/no-store |
| `/dashboard/portfolio`, `/dashboard/projects`, `/dashboard/media` | Respective domain | Tenant/action scoped; noindex/no-store |
| `/dashboard/ai`, `/dashboard/leads`, `/dashboard/seo` | AI/leads/SEO | Tenant scoped, noindex/no-store |
| `/dashboard/reviews`, `/dashboard/analytics`, `/dashboard/billing`, `/dashboard/settings` | Respective domain | Feature scope plus entitlement and permissions; absent functionality not faked |
| `/account`, `/account/enquiries`, later `/account/collections` | Customer app | Self-owned data; noindex/no-store |
| `/preview/{opaqueToken}` | Portfolios | Grant/session protected; token exchange where feasible; noindex/no-store/no-referrer |
| `/share/{opaqueToken}` | AI collaboration | Grant-protected version review; noindex/no-store/no-referrer |
| `/preview/view/{previewId}`, `/share/view/{reviewId}` | Private renderers | Clean post-exchange URL plus required grant session; ID alone grants nothing |
| `/admin`, `/admin/{module}` | Admin | Privileged session/MFA/policy; noindex/no-store |
| `/privacy`, `/terms`, `/contact`, `/help`, `/report` | Platform editorial/support | Public; index only meaningful policy/help content; report submission protected |
| `/robots.txt`, `/sitemap.xml`, `/sitemaps/{shard}.xml` | SEO | Server-generated from live eligible registry; no private URLs |
| `/favicon.ico`, `/manifest.webmanifest`, `/_next/*`, `/.well-known/*` | Runtime/infrastructure | Explicit handlers, never studio slugs; `.well-known` minimal allowlist |
| `/api/v1/*` | Spring API | Never delegated to studio route; public API still noindex |

`/projects/{id}` is not a second canonical project-detail website. Discovery cards use the studio-scoped canonical URL. If a convenience link is needed, a validated lookup redirects to the canonical path, never renders a duplicate page. Static assets and errors do not invoke tenant resolution by string accident.

## 2. API namespaces

| Prefix | Contract |
|---|---|
| `/api/v1/public/portfolios`, `/api/v1/public/projects`, `/api/v1/public/search` | Published allowlisted DTOs; bounded anonymous traffic; no source storage keys |
| `/api/v1/public/enquiries`, `/api/v1/public/reports` | Anonymous purpose-limited POST, validation/abuse defenses; no general tenant write permission |
| `/api/v1/me` | Current verified user/profile/session list and own data |
| `/api/v1/tenants/{tenantId}/{module}` | Current membership + action + object; path ID is untrusted |
| `/api/v1/grants/*` | Token exchange/session and scoped review actions/media; live grant checks |
| `/api/v1/admin/{module}` | Separate privileged policy and step-up requirements |
| `/api/v1/webhooks/{provider}` | Exact provider handlers, raw-body signature and inbox dedup; no arbitrary provider dispatch |
| `/api/v1/events` | Bounded privacy-aware analytics batching; not authoritative payment/usage evidence |
| `/internal/health/*`, `/internal/metrics` | Private workload/probe access, not exposed by public edge |

Final endpoint payload schemas belong to feature/contract phases; these namespaces do not advertise functioning APIs. Internal cache/event dispatch operations use private authenticated endpoints or local adapters, never unauthenticated `/revalidate?secret=...` URLs logged publicly.

## 3. Slug registry and normalization

Designers owns globally unique studio slug reservations; projects owns tenant-unique project slugs; taxonomy owners manage city/category slugs. Registry distinguishes current, alias, reserved and retired entries, with owner ID and canonical target. Database constraints resolve concurrent duplicate names; UI suggests readable numeric suffixes (`studio-name-2`) but uniqueness is server enforced. Do not include phone/address/customer identifiers in generated slugs.

V1 slug policy: lowercase ASCII `[a-z0-9]` segments separated by single hyphens,3–80characters for studios and up to120 for projects; normalize Unicode NFKC, trim and collapse separators; explicit reviewed transliteration for non-Latin labels, falling back to user-chosen readable ASCII. Full Unicode display names/content remain supported. Reject encoded slashes/backslashes, traversal, controls, invisible characters, leading dots, empty/double-separator ambiguity and double decoding. Do not normalize distinct user data destructively; only normalize the route slug. Future localized/Unicode slugs require versioned normalization/confusable review and redirect plan, not a silent algorithm change.

Reserved roots (case-insensitive and normalized): `api`, `admin`, `auth`, `login`, `logout`, `signup`, `register`, `dashboard`, `account`, `explore`, `designers`, `projects`, `categories`, `cities`, `inspiration`, `preview`, `share`, `privacy`, `terms`, `contact`, `help`, `report`, `settings`, `billing`, `search`, `sitemaps`, `assets`, `media`, `internal`, `health`, `metrics`, `interior-designers`, `tv-unit-designers`, `modular-kitchen-designers`; infrastructure names beginning `_` or `.` and known root files. Reserve planned locale prefixes `en`, `te`, `hi`, `ta`, `kn`, `ml`, `mr` without generating locale pages. Registry expansions undergo collision checks before code release; never take an existing professional slug silently.

Rename transaction validates availability/owner, claims new slug, preserves old alias and emits SlugChanged with version. Resolve all aliases to final current canonical to avoid redirect chains. Alias ownership cannot be transferred to another tenant; retired owner slugs remain protected from impersonation. Project slug reuse policy prevents linking old indexed work to unrelated new work. Explicit release of a retired route requires reviewed policy rather than automatic purge.

## 4. HTTP semantics

One lowercase/trailing-slash policy and HTTPS canonical host; redirect approved equivalent variants with permanent308 (or301 for documented external integrations). Unsafe redirect targets and user-controlled `returnTo` outside allowlisted local paths are rejected. Private share/auth redirects use303 where appropriate and never cache credentials.

Unknown/malformed slug404; temporary unpublish404 (no indication whether private draft exists); permanently removed previously public page410 only when disclosure is acceptable and tombstone exists. No redirect all missing projects to homepage. Provider/DB outage503 with retry semantics, never fabricated404 that can harm indexing. Suspended tenant content404 publicly unless a deliberate public moderation notice is approved. APIs distinguish safe denied action403 from private-object existence404 per security policy.

## 5. Custom domain and locale compatibility

V1 uses platform host plus root studio slug. V1.5 verified `www.studio.example` maps `/` to that studio portfolio and `/projects/{projectSlug}` to its canonical project. Domain registry maps normalized verified host to tenant and primary/alias role; use DNS TXT challenge scoped to tenant+host, CNAME/A/ALIAS guidance appropriate to hosting, managed TLS, periodic validation and lifecycle states REQUESTED/VERIFIED/PROVISIONING/ACTIVE/SUSPENDED/REMOVING/REMOVED. Punycode/IDNA host normalization, exact allowlist and no arbitrary forwarded-host trust.

Platform default URLs remain stable aliases: when custom domain is ACTIVE and chosen primary, redirect corresponding platform public portfolio/project URLs to it; before activation, platform stays primary. Discovery remains on platform and links canonical domain. No requirement to create tenant platform subdomains in V1; future platform subdomains are another registry alias with identical canonical rules. Custom-domain dashboard/auth/admin routes do not run with platform cookies; send users to a fixed platform sign-in URL or404, never create shared domain cookies.

Custom-host enquiry endpoint accepts only a validated host/tenant-bound public form intent, verified Origin and anti-abuse controls; it grants no authenticated platform API access. Ownership loss, TLS failure and removal have monitored states and safe platform canonical fallback. Never redirect users to a domain after ownership is lost. Long-lived caches must be invalidated on domain changes; stale unknown host mappings fail closed. Reassignment needs new proof and retired binding cleanup.

English URLs stay unchanged in V1. Locale route activation is a later namespace/SEO migration with actual translated content, explicit per-language canonicals/hreflang, and published language availability. UI locale does not create translated business facts or duplicate indexed projects.
