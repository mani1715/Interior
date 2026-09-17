# Phase 00 — Architecture Decisions

**Phase01 update, 2026-09-17:** ADR-001–019 below are the preserved Phase00 history. Their Recommended/Deferred wording records that point in time. **ADR-020–036 are the current authoritative decisions wherever they refine those entries.** Selected architecture does not authorize spending, deployment or claim working software. Product scope, release boundaries and locked colors are unchanged. Implementation qualification gates remain explicit.

2026-09-17. No stack is installed. **Accepted constraint** means mandated product behavior; **Recommended** means a reasoned Phase 00 direction awaiting Phase 01 validation; **Deferred** means selection is intentionally open. No vendor, pricing, deployment or dependency installation is authorized by this document. No decision blocks beginning Phase 01; decisions below have later implementation gates.

## ADR-001 — Greenfield baseline

- **DECISION:** Treat the supplied workspace as empty, preserving the possibility of later source import.
- **STATUS:** Accepted evidence.
- **CONTEXT:** Full visible/hidden file inventory is empty; no Git repository.
- **OPTIONS:** Invent an existing stack; scaffold immediately; document the actual baseline.
- **RECOMMENDATION:** Documentation only in Phase 00; establish version control/build foundations in 01.
- **REASONING:** No code can be reused or refactored without evidence.
- **RISKS:** A separate repository may exist but was not supplied.
- **REVERSIBILITY:** High; re-audit introduced source before changing architecture.

## ADR-002 — Frontend architecture

- **DECISION:** Next.js App Router with TypeScript and small client-interactive boundaries.
- **STATUS:** Recommended; exact maintained version pinned in 01 after compatibility/security review.
- **CONTEXT:** Public portfolios need HTML rendering; dashboards and masks need interactivity.
- **OPTIONS:** Client-only SPA; Next.js; separate marketing/site framework and dashboard.
- **RECOMMENDATION:** One frontend with separated public, authenticated and admin route policies; token-based CSS with Tailwind a candidate.
- **REASONING:** Reduces duplicate UI/SEO plumbing. Next.js supports server-first layouts and interactive client components ([official reference](https://nextjs.org/docs/app/getting-started/server-and-client-components)).
- **RISKS:** Cache leaks, framework upgrades, excessive client bundles.
- **REVERSIBILITY:** Medium; domain API and structured content remain framework-independent.

## ADR-003 — Backend/runtime

- **DECISION:** Java 25 with a supported compatible Spring Boot line; modular monolith.
- **STATUS:** Recommended; dependency/JDK distribution/support verification in 01 before pinning.
- **CONTEXT:** Empty workspace and stated Java preference; transactional tenant/media/credit concerns.
- **OPTIONS:** Java/Spring monolith; TypeScript full stack; microservices.
- **RECOMMENDATION:** Java domain API and reusable worker modules, with Next.js limited to presentation/BFF delegation.
- **REASONING:** Consistent authorization and transaction ownership. Current Spring Boot system requirements list Java compatibility covering Java 25; third-party compatibility still needs validation ([Spring reference](https://docs.spring.io/spring-boot/system-requirements.html)).
- **RISKS:** Two-language operations, dependency migration friction and team familiarity.
- **REVERSIBILITY:** Medium/high cost for runtime replacement; explicit interfaces reduce coupling. Java 21 is a fallback only if a documented dependency/support constraint requires it.

## ADR-004 — Data and tenancy

- **DECISION:** PostgreSQL, organization ownership, memberships and tenant-qualified relationships.
- **STATUS:** Recommended; tenancy is an accepted constraint; detailed schema in 02.
- **CONTEXT:** Teams, transactional publish/credits and cross-tenant privacy.
- **OPTIONS:** Shared schema with tenant IDs; per-tenant schemas/databases; document store.
- **RECOMMENDATION:** Shared relational schema, tenant-qualified unique/foreign keys and service authorization; evaluate RLS defense in depth.
- **REASONING:** Practical initial operations and atomic domain transactions. RLS policy and backup behavior require care ([PostgreSQL documentation](https://www.postgresql.org/docs/17/ddl-rowsecurity.html)).
- **RISKS:** Tenant omission, privileged connection bypass, pool context leakage, incomplete backups.
- **REVERSIBILITY:** Medium; stable tenant IDs permit later partitioning. Prove isolation with adversarial two-tenant tests.

## ADR-005 — Storage and CDN

- **DECISION:** Private S3-compatible originals plus isolated approved derivative delivery.
- **STATUS:** Privacy boundary accepted; storage/CDN vendor deferred to 01/19 infrastructure gate.
- **CONTEXT:** Watermark mandate and sensitive room photographs.
- **OPTIONS:** Public originals; application file disk; private object storage/CDN.
- **RECOMMENDATION:** Private quarantine/master/private variants, public derivative CDN with restricted origin; short-lived authorized private access.
- **REASONING:** Prevents unwatermarked origin bypass. CloudFront OAC is one supported pattern, not a mandated vendor ([AWS reference](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html)).
- **RISKS:** Egress cost, leaked signed links, stale public caches after unpublish.
- **REVERSIBILITY:** Medium; opaque asset IDs and storage adapter permit migration, but bulk transfer has cost.

## ADR-006 — Watermarks

- **DECISION:** Versioned server-side processing, clean immutable originals and publication gating.
- **STATUS:** Accepted constraint; processing library deferred to 19 benchmark/security review.
- **CONTEXT:** Every public project/portfolio photograph must be watermarked.
- **OPTIONS:** Browser overlay; destructive overwrite; separate immutable variants.
- **RECOMMENDATION:** Separate responsive variants, logo/name fallback, bounded visible watermark configuration, additional AI label; cover all public image endpoints.
- **REASONING:** Browser overlays are bypassable; overwrites destroy valuable masters.
- **RISKS:** CPU load, stale logo versions, invisible settings, preview/public mismatch.
- **REVERSIBILITY:** High for recipes; reprocess originals. Original deletion follows privacy policy.

## ADR-007 — Portfolio versions and themes

- **DECISION:** Schema-versioned shared content, independent theme renderers and immutable published snapshots.
- **STATUS:** Accepted constraint; registry contract in 10.
- **CONTEXT:** Six materially different themes plus restore history.
- **OPTIONS:** One layout with colors; per-theme databases; common data with distinct renderers.
- **RECOMMENDATION:** Theme capability manifest, validated sections, draft/live pointers and immutable version restoration.
- **REASONING:** Supports real design differences without duplicate CMS or authorization.
- **RISKS:** Theme schema drift, unsupported sections, deleted assets in old snapshots.
- **REVERSIBILITY:** High with version migrations and compatibility checks.

## ADR-008 — SEO rendering and routes

- **DECISION:** Server-rendered published content, one canonical project URL and editorial landing-page eligibility.
- **STATUS:** Accepted constraint; rendering/cache implementation recommended.
- **CONTEXT:** Core discoverability and root studio slug examples.
- **OPTIONS:** SPA-only rendering; uncontrolled generated facets; SSR/SSG with controlled indexable routes.
- **RECOMMENDATION:** Static/cached published snapshots where safe; SSR where necessary; reserved slug namespaces; noindex private/preview and unqualified facets.
- **REASONING:** Indexable content and controlled duplicates. Arbitrary facet expansion wastes crawling ([Google guidance](https://developers.google.com/crawling/docs/faceted-navigation)).
- **RISKS:** Stale public data, route collisions, canonical drift, thin content.
- **REVERSIBILITY:** Medium; preserve aliases and migration redirects.

## ADR-009 — Authentication and privilege

- **DECISION:** Server-enforced object/tenant permissions and secure browser sessions; privileged MFA.
- **STATUS:** Controls accepted; identity vendor/self-hosting deferred to 03, evaluated in 01.
- **CONTEXT:** Public visitors, professionals, clients, teams and platform staff have different authority.
- **OPTIONS:** Browser-stored long-lived bearer tokens; backend session; managed OIDC with backend session.
- **RECOMMENDATION:** Standards-based identity adapter plus same-origin HttpOnly/Secure session; CSRF, revocation, rotation, recovery and admin step-up.
- **REASONING:** Central authorization avoids UI-only controls and keeps credentials out of JS storage. Deny-by-default and resource checks follow [OWASP authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html).
- **RISKS:** Identity provider lock-in/outage, recovery abuse, session-cache leaks.
- **REVERSIBILITY:** Medium; map external identities to stable internal users.

## ADR-010 — Queue, workers and outbox

- **DECISION:** Durable asynchronous media/AI processing with transactional outbox and idempotent consumers.
- **STATUS:** Pattern recommended; queue vendor deferred before 19/21.
- **CONTEXT:** Long-running processing and billable provider retries cannot block browser requests.
- **OPTIONS:** In-process fire-and-forget; durable managed queue; broker operated in-house.
- **RECOMMENDATION:** Managed durable queue if deployment supports it; bounded retries, dead-letter handling, visibility/lease renewal and reconciliation.
- **REASONING:** Persist work and state together, then deliver at least once; do not claim exactly-once external execution.
- **RISKS:** Duplicate delivery, orphan reservations, stuck workers and poison jobs.
- **REVERSIBILITY:** Medium; portable job contracts and outbox isolate transport.

## ADR-011 — AI provider abstraction and cost

- **DECISION:** Capability-aware provider interface, immutable history and transactional credit reservations.
- **STATUS:** Accepted constraints; provider/model deferred to 21 evaluation.
- **CONTEXT:** Editing with references/masks, uncertain quality, cost and timeout outcomes.
- **OPTIONS:** Hardwire one model; abstract only HTTP; capability/domain adapter with evaluation fixtures.
- **RECOMMENDATION:** Test real interior edits and mask/reference preservation, pricing/terms/retention, latency and failure behavior before selecting provider. Credentials stay server-side.
- **REASONING:** Providers differ in supported controls; feature gating must be truthful.
- **RISKS:** Quality drift, API deprecation, double charges, privacy leakage.
- **REVERSIBILITY:** High interface-level; low for perfectly reproducing old outputs. Persist model/version and lineage.

## ADR-012 — Search

- **DECISION:** PostgreSQL-backed discovery first, replaceable public search projection.
- **STATUS:** Recommended; revisit on measured relevance/latency/scale at 25.
- **CONTEXT:** Structured filters and no existing search load.
- **OPTIONS:** DB indexes/full-text; external search engine; immediate vector search.
- **RECOMMENDATION:** Publish-only indexed projection, stable pagination; defer embeddings to V2 visual similarity.
- **REASONING:** Avoid unproven infrastructure and privacy duplication.
- **RISKS:** Multilingual relevance, large facet queries, delayed unpublish propagation.
- **REVERSIBILITY:** High; rebuild external projections from source records/outbox.

## ADR-013 — Analytics

- **DECISION:** Minimal first-party events and tenant aggregates; external integrations later.
- **STATUS:** Recommended; consent/retention reviewed before collection.
- **CONTEXT:** Lead attribution and portfolio outcomes without exposing client data.
- **OPTIONS:** Raw session recording; broad third-party trackers; purpose-limited first-party metrics.
- **RECOMMENDATION:** Event schema, deduplication, bot filtering, defined attribution and retention; no fabricated Google data.
- **REASONING:** Preserves trust and keeps product analytics distinct from audit records.
- **RISKS:** Inaccurate unique visitors, attribution ambiguity, accidental PII.
- **REVERSIBILITY:** High for sinks, low for collected data; minimize initially.

## ADR-014 — Entitlements and payments

- **DECISION:** Configured capabilities and usage ledger, provider-neutral subscription adapter.
- **STATUS:** Model recommended; prices and provider deferred to 28 before paid launch.
- **CONTEXT:** Plans, storage/projects/AI/team limits; India-first market.
- **OPTIONS:** Hardcoded plan checks; centralized entitlements; provider-owned business authorization.
- **RECOMMENDATION:** Centralized server checks, atomic reservation/settlement and verified idempotent webhooks; evaluate Razorpay and alternatives for actual needs.
- **REASONING:** Prevents quota races and payment coupling. No unsupported fee/compliance assumptions are made.
- **RISKS:** Double processing, refunds/disputes, downgrade behavior, payment-state drift.
- **REVERSIBILITY:** Medium; internal immutable ledger supports provider migration.

## ADR-015 — Design system and motion

- **DECISION:** Locked palette, accessible mobile-first tokens, six theme contracts; CSS-first enhancement.
- **STATUS:** Accepted direction; fonts/libraries deferred to 04/05 evaluation.
- **CONTEXT:** Premium identity cannot compromise small screens or SEO.
- **OPTIONS:** Heavy generic motion/3D; static-only forever; progressively enhanced story.
- **RECOMMENDATION:** Charcoal on Bronze CTA, tested semantics, editorial serif/UI sans after license/script review; optional lazy 3D with equivalent static presentation.
- **REASONING:** Measured contrast approximately 5.35:1 versus white on Bronze 3.08:1. Product budgets gate effects.
- **RISKS:** Weak-device failures, font layout shift and theme accessibility drift.
- **REVERSIBILITY:** High for effects/tokens; changing locked colors needs an explicit product decision.

## ADR-016 — Domains/localization

- **DECISION:** Prepare domain/locale contracts now; deliver custom domains and multilingual UI in V1.5.
- **STATUS:** Scope accepted; hosting/TLS automation deferred to V1.5-DOMAIN.
- **CONTEXT:** Professional website identity and English/Telugu priorities.
- **OPTIONS:** Hardcode platform host/English strings; prematurely implement every locale/domain; explicit abstraction.
- **RECOMMENDATION:** Verified host registry, domain lifecycle, canonical handoff, locale-aware content/formatting and deliberate translated SEO.
- **REASONING:** Prevents later URL/schema redesign without premature operations.
- **RISKS:** Host-header injection, domain takeover, certificate failure and duplicate language pages.
- **REVERSIBILITY:** Medium; URL changes require redirects and ownership validation.

## ADR-017 — Recovery, privacy and operations

- **DECISION:** Explicit retention, deletion tombstones, backup/restore objectives and observability from foundation.
- **STATUS:** Principles accepted; durations/RPO/RTO are recommendations requiring launch validation.
- **CONTEXT:** Immutable versions and private originals still need deletion and recovery.
- **OPTIONS:** Indefinite retention; ad hoc deletion; documented lifecycle with recoverability limits.
- **RECOMMENDATION:** Master-spec proposed retention, encrypted PITR/backups, RPO 15 minutes/RTO 4 hours target, restore drills and privacy-safe telemetry.
- **REASONING:** Trash is not backup; restoring deleted content is unacceptable.
- **RISKS:** Provider constraints, cost, regulatory retention conflict, untested restore.
- **REVERSIBILITY:** Policy changes possible for future data; contractual deletion commitments must be honored.

## ADR-018 — Roadmap dependency reconciliation

- **DECISION:** Preserve all 30 phases and separate UI phase completion from release readiness.
- **STATUS:** Accepted planning rule.
- **CONTEXT:** Homepage/themes precede media/SEO backend phases; later phases mix release generations.
- **OPTIONS:** Renumber/collapse phases; implement unsafe placeholder production APIs; documented integration gates.
- **RECOMMENDATION:** Define contracts in 01/02; isolated development fixtures only; integrate real media/SEO at 18–20 before public release. Phase 24 full approvals and Phase 27 review/verification/collections remain V1.5; V1 moderation foundation is required regardless.
- **REASONING:** Honors requested sequence and prevents misleading completeness claims.
- **RISKS:** Early visual polish can mask missing integration; V1.5 scope creep.
- **REVERSIBILITY:** High by explicit roadmap amendment, never silent omission.

## ADR-019 — Cache and Redis

- **DECISION:** Public version-aware caching; no shared private-response cache; Redis only for demonstrated needs.
- **STATUS:** Recommended; evaluate sessions/distributed limits in 03 and load in 25/29.
- **CONTEXT:** Cache key mistakes can leak tenant or preview data.
- **OPTIONS:** Cache all responses; no caching; explicit public/private policies.
- **RECOMMENDATION:** Include verified host, tenant, version and locale in relevant public keys; invalidate on publish/unpublish/domain changes. Shared rate-limit/session state must work across replicas even without Redis.
- **REASONING:** Meets performance requirements without blindly adding a cache product.
- **RISKS:** Stampedes, stale data, cross-tenant leaks and premature complexity.
- **REVERSIBILITY:** High; cache is disposable, database is authoritative.

## Decision gates and source review

Before Phase 01: **no user decision is required**. Use the empty-workspace baseline. During 01, choose maintained runtime/build versions and proposed environment topology. Before implementation dependencies: identity in 03/07; fonts in 04; queue/storage/CDN in 19; AI model/provider and retention terms in 21; payment/provider/pricing before paid launch in 28; jurisdiction/retention and operational objectives before production in 29. Custom-domain vendor automation is a V1.5 gate.

Official sources checked 2026-09-17; live version documentation can change. Revalidate before pinning. Additional security references: [OWASP file uploads](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html), [multi-tenant security](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html), [WCAG 2.2](https://www.w3.org/TR/WCAG22/). These inform recommendations; no compliance certification is claimed.

---

# Phase 01 — Finalized Architecture Decisions

Details and dated version sources: [production architecture](01_PRODUCTION_ARCHITECTURE.md). Acceptance means architecture selection, not completed implementation. Reversible choices still require traceable amendments.

## ADR-020 — Frontend and current version baseline

- **STATUS:** Accepted; refines ADR-002/015.
- **CONTEXT:** Public crawlability and rich private tools need one maintainable web application.
- **DECISION:** Next.js16.3.3 App Router, React/React DOM19.3.0, TypeScript6.0.3, Node24.21.0 LTS; strict generated wire types; CSS custom properties/CSS Modules. Pin exact lockfile/digests at first scaffold and recheck security updates/peer compatibility.
- **ALTERNATIVES:** Client-only SPA; separate public/dashboard stacks; TypeScript7 immediately; Tailwind mandatory.
- **CONSEQUENCES:** Server-first public routes, small interactive boundaries, no CSS runtime. TS6 deliberately avoids initial compiler-API transition complexity documented by Microsoft; TS7 remains a future measured upgrade.
- **RISKS:** New React/Next/tooling combination still needs a real build; cache/security patch discipline. No compatibility test is claimed from browsing alone.

## ADR-021 — Backend and layering

- **STATUS:** Accepted; refines ADR-003/004/018.
- **CONTEXT:** Tenant publishing, quotas and audit need consistent transactions; microservices add unjustified failure modes.
- **DECISION:** Temurin25.0.4.1, Spring Boot 3.4.3 (supersedes earlier theoretical 4.1.1 mention; verified on Java 25 runtime with Java 21 compiler bytecode target), Maven 3.9.9 checked-in wrapper; Spring MVC/Security/JDBC modular monolith. Explicit facade/application/domain/ports/adapters boundaries and acyclic allowlist.
- **CANONICALIZATION NOTE (Phase 04.1):** The actual working implementation in `apps/api/pom.xml` is Spring Boot 3.4.3 running on JDK 25 with `<maven.compiler.release>21</maven.compiler.release>` to maintain ASM bytecode compatibility with Spring Framework 6.2.x. This working configuration is canonical.
- **ALTERNATIVES:** TypeScript-only backend; Java21 without compatibility need; independently deployed domain services.
- **CONSEQUENCES:** One Java domain codebase with separate API/worker executable composition. Application owns transactions and policy; no random controller repository/provider calls.
- **RISKS:** Current machine Java18/Maven absence prevents executable verification; Spring ecosystem/driver/native dependencies need pin-and-build gate. Java choice itself does not provide security.

## ADR-022 — Monorepo and tooling ownership

- **STATUS:** Accepted; finalizes Phase00 proposed paths.
- **CONTEXT:** Shared API contracts and coordinated releases, different language toolchains.
- **DECISION:** Proposed apps/web, apps/api, apps/worker, backend/modules, packages/contracts, infrastructure, scripts and docs. npm workspaces for JS only; Maven reactor for Java. Create directories only when real files exist.
- **ALTERNATIVES:** Separate repositories; forcing Java through npm/Nx; premature packages/ui/config and empty folder scaffold.
- **CONSEQUENCES:** Atomic contract changes without inappropriate shared build ecosystem. Phase01 adds only README/.gitignore/.editorconfig besides docs.
- **RISKS:** Cross-language CI needs explicit artifact dependencies; version control/remote still must be established later. No Git initialization/deployment performed.

## ADR-023 — Database, migration and ownership

- **STATUS:** Accepted; refines ADR-004/017.
- **CONTEXT:** Cross-tenant consistency, restoration, location and financial correctness.
- **DECISION:** PostgreSQL18.6; module-owned schemas; UUIDv4 IDs; tenant-qualified FK/query constraints and tested RLS defense in depth; Flyway SQL migrations; UTC instants/separate IANA zone; integer-minor-unit money/currency; structured location and entity-specific deletion.
- **ALTERNATIVES:** Document DB; schema-per-tenant; predictable public numeric IDs; UUIDv7 immediately; ORM auto-DDL; all-record soft deletion.
- **CONSEQUENCES:** Detailed schema in02, runtime/migration roles separate, expand/contract and atomic outbox/ledger transactions. Flyway's current [database support](https://documentation.red-gate.com/fd/supported-database-versions-143754067.html) includes PostgreSQL18; actual selected dependency version must be tested.
- **RISKS:** RLS context/pooling mistakes, backup filtering, provider engine availability and large migration locks. No entities/migrations created in01.

## ADR-024 — Authentication and tenant authorization

- **STATUS:** Accepted protocol/policy; OIDC vendor remains deferred to03. Refines ADR-009.
- **CONTEXT:** Browser app first, future native/API, strong staff protection and multiple tenant memberships.
- **DECISION:** Managed OIDC authorization code/PKCE; Spring-owned random opaque session in Secure/HttpOnly host-only cookie, PG-backed revocation, CSRF. Resource/tenant/action checks independent of roles; privileged MFA/step-up/shorter sessions.
- **ALTERNATIVES:** JWT/access-refresh pair in browser storage; ad hoc passwords; using roles without ownership.
- **CONSEQUENCES:** Vendor-independent internal users, session/device management, no frontend secrets. Future native OAuth tokens reuse backend policy rather than browser cookies.
- **RISKS:** Provider assurance/recovery/revocation contract must pass03; sessions cannot remain authoritative in unavailable cache. Full [security policy](01_SECURITY_ARCHITECTURE.md).

## ADR-025 — Object storage

- **STATUS:** Accepted; finalizes storage portion of ADR-005.
- **CONTEXT:** Originals, references and client assets cannot be made public through folder conventions.
- **DECISION:** S3-compatible abstraction with AWS S3 initial adapter; separate private quarantine/masters/private processed/published derivative buckets, IAM and generated opaque keys. No original mutation; deletion policy still applies.
- **ALTERNATIVES:** Public original bucket; application disk; one unguarded uploads prefix.
- **CONSEQUENCES:** Provider portability at API boundary, private signed owner access, upload checksum/version verification and explicit public manifests.
- **RISKS:** IAM misconfiguration, signed-link leakage, storage cost/orphan growth; prove policy in real staging19.

## ADR-026 — CDN and cache-safe public delivery

- **STATUS:** Accepted; finalizes CDN portion of ADR-005 and refines ADR-008/019.
- **CONTEXT:** Fast public imagery must not expose private originals or defeat withdrawal.
- **DECISION:** CloudFront/WAF with OAC restricted to derivative bucket. Versioned immutable objects, bounded revocable public-cache policy, no shared private responses; SSR public HTML uses live eligibility initially.
- **ALTERNATIVES:** Open S3 URLs; caching personalized/preview HTML; indefinite browser immutable cache for revocable photos.
- **CONSEQUENCES:** Page safety independent of delayed invalidation; CDN takedown tracked and bounded, private reviews live-gated. Next CSP nonces do not get reused in cached HTML.
- **RISKS:** Public downloaded copies cannot be recalled; CDN outage/TTL/purge limits and fixed costs require qualification19/20/29. Custom-domain edge onboarding later.

## ADR-027 — Queue and durable events

- **STATUS:** Accepted; finalizes ADR-010.
- **CONTEXT:** Media/AI/notifications/projections need resilient asynchronous work without broker operations burden.
- **DECISION:** SQS Standard with purpose-specific queues/DLQs, transactional outbox and per-destination dispatch tracking, DB job leases/fencing/dedup. No exactly-once external-call claim.
- **ALTERNATIVES:** PG polling-only, Redis queues, RabbitMQ, Kafka, in-process fire-and-forget; comparison in [media/AI](01_MEDIA_AI_ARCHITECTURE.md).
- **CONSEQUENCES:** Queue is transport, database authoritative; retries/unknown AI outcomes differ; fan-out is explicit rather than competing-consumer broadcast error.
- **RISKS:** Duplicates/out-of-order delivery, poison work, lost leases and provider double submission; crash/replay tests mandatory.

## ADR-028 — AI abstraction and cost settlement

- **STATUS:** Accepted architecture; provider/model deferred to21. Refines ADR-011.
- **CONTEXT:** Site-preserving editing with typed references/masks and billable ambiguous timeouts.
- **DECISION:** Capability-aware ImageGenerationProvider, private inputs/outputs, immutable lineage, durable submission intent, atomic reserve/settle/release and RECONCILING. User credits settle on validated durable usable result once.
- **ALTERNATIVES:** Hardwired vendor calls; retry every timeout; overwrite output; charge via client state.
- **CONSEQUENCES:** Confirmed failure releases credits; vendor-charged unusable output is platform cost under initial policy. Provider quality/retention/idempotency evaluated before integration.
- **RISKS:** Unknown outcomes may need manual adjudication; quality and exact material preservation cannot be guaranteed; no refund on an unverified timeout.

## ADR-029 — Search

- **STATUS:** Accepted; finalizes ADR-012.
- **CONTEXT:** Structured project-first filters and no measured external-search need.
- **DECISION:** PostgreSQL indexed published projection behind SearchProvider; serve-time live eligibility checks; stable pagination, full-text/trigram where useful.
- **ALTERNATIVES:** Elasticsearch/OpenSearch initially; vector store before visual-search phase.
- **CONSEQUENCES:** One authoritative DB, rebuildable projection and explicit future engine threshold; UI collections/reviews retain V1.5 scope.
- **RISKS:** Relevance/locale/load limits; counts and stale hits must not leak withdrawn/private records. Evaluate against p95 budget in25.

## ADR-030 — Redis/cache

- **STATUS:** Accepted; finalizes ADR-019.
- **CONTEXT:** Cost-sensitive start and sensitive session/credit truth.
- **DECISION:** No startup Redis. PG sessions, low-volume atomic sensitive limit counters, edge coarse throttling; explicit public-version caches/CDN. Redis may later accelerate disposable data only.
- **ALTERNATIVES:** Redis required everywhere; per-process-only sessions/limits; cache as primary ledger.
- **CONSEQUENCES:** Removes one mandatory service. Cache outage cannot grant permissions, lose balances or break tenant isolation; safe fallback or503.
- **RISKS:** PG counter/session load must be measured; introduce distributed cache when warranted without hiding revocation or changing authority.

## ADR-031 — API and generated contracts

- **STATUS:** Accepted.
- **CONTEXT:** Java/TypeScript DTO drift and consistent client error handling.
- **DECISION:** REST `/api/v1`, OpenAPI3.1.0 contract-first, generated TypeScript schema types with openapi-typescript and typed openapi-fetch transport; explicit Java records plus conformance tests; safe error envelope/request IDs/cursors/idempotency.
- **ALTERNATIVES:** Hand-copy hundreds of DTOs; expose database entities; GraphQL without a demonstrated requirement; ad hoc response shapes.
- **CONSEQUENCES:** Generation pinned/tested at scaffold, checked drift in CI, small explicit transport with server/browser credentials separation. [Official typed fetch documentation](https://openapi-ts.dev/openapi-fetch/) describes schema-derived types.
- **RISKS:** Type safety is not runtime validation; generator/schema edge features need fixtures, breaking changes require versioning.

## ADR-032 — Themes and publication

- **STATUS:** Accepted; refines ADR-007/008/018.
- **CONTEXT:** Six distinct sites sharing content, previews and restore history.
- **DECISION:** Validated schema/versioned document and theme config, independent enum-registered renderers; no executable user customization. Published snapshots pin revisions; live security eligibility overrides historical visibility.
- **ALTERNATIVES:** Per-theme backend models, color reskins, arbitrary custom HTML/CSS/JS, mutable live drafts.
- **CONSEQUENCES:** Each11–16 theme phase remains separate,17 comparative QA. New project publication does not silently rewrite portfolio snapshots; explicit republish updates featured content.
- **RISKS:** Version compatibility and stale feature summaries need clear UI; withdrawal/domain/verification overrides must not be frozen in a snapshot.

## ADR-033 — Watermark strategy

- **STATUS:** Accepted; refines ADR-006.
- **CONTEXT:** Every public photograph must have visible studio branding, with clean private original retained.
- **DECISION:** Precomputed responsive derivatives, per-size watermark, versioned logo/name/recipe and AI label; publish only after readiness. No real-time per-page watermark transformation.
- **ALTERNATIVES:** CSS overlay; destructive original; unbounded dynamic source transforms.
- **CONSEQUENCES:** Logo change regenerates versions without re-upload; name fallback and visibility bounds; OG/thumb/export obey same policy.
- **RISKS:** Worker/codec failure blocks publishing; old recipe cache retirement and watermark legibility require real-image checks. Watermark is never authorization/copyright guarantee.

## ADR-034 — Worker language and media engine

- **STATUS:** Accepted Java orchestration and native adapter approach; exact decoder packaging gate19.
- **CONTEXT:** Image processing needs modern codecs; AI orchestration is I/O-bound; avoid a third application language.
- **DECISION:** Shared Java modules in worker executable; networkless unprivileged libvips-based decoder subprocess/sandbox with fixed arguments, resource limits and no credentials. Native package pins after codec/license/security benchmark.
- **ALTERNATIVES:** Python worker estate; Node Sharp worker adding orchestration ecosystem; in-process untrusted native decoding in API; managed image processor.
- **CONSEQUENCES:** Existing application rules/ledgers reused; [libvips CLI](https://www.libvips.org/API/8.16/using-cli.html) permits a language-independent adapter. Managed processing can replace adapter via ADR if sandbox economics fail.
- **RISKS:** Sandbox isolation, codec vulnerabilities/AVIF costs, execution environment support must be proven19; no claim that CLI alone is safe.

## ADR-035 — Deployment, CI, backup and telemetry

- **STATUS:** Accepted reference topology; size/region qualifications before provisioning. Refines ADR-017.
- **CONTEXT:** Startup needs reliable managed infrastructure without Kubernetes or domain service fleet.
- **DECISION:** AWS ECS Fargate/RDS/S3/CloudFront/SQS, separate env accounts/secrets, GitHub Actions native builds and artifact promotion, OpenTelemetry with CloudWatch initial sink, PITR/restore/tombstones. No infrastructure created now.
- **ALTERNATIVES:** Self-managed cluster; multi-region active/active; many monitoring vendors; ad hoc manual schema changes.
- **CONSEQUENCES:** Explicit fixed/variable cost model, HA target for full paid launch, staging provider limits, migration/rollback gates and failure matrix.
- **RISKS:** Baseline fixed AWS spend, provider region/version availability and real RPO/RTO unmeasured; budget/provisioning/restore qualification required before production.

## ADR-036 — Billing, analytics, notification and flags

- **STATUS:** Accepted boundaries; later external vendors gated by feature phase. Refines ADR-013/014.
- **CONTEXT:** Usage/payment facts must not be mixed with page-view telemetry or client state.
- **DECISION:** Separate payment/subscription/plan/entitlement/usage, centralized capability checks, signed webhook inbox, minimal asynchronous analytics, provider-isolated notifications, lightweight audited DB feature flags.
- **ALTERNATIVES:** Plan-name conditionals everywhere; browser-granted credits; synchronous page-view writes to core rows; hardwired email/WhatsApp vendor; flag SaaS by default.
- **CONSEQUENCES:** Durable business events and bounded best-effort pre-ack analytics; provider failure never loses leads; future notifications retain release scope; flags cannot bypass authorization.
- **RISKS:** Consent/retention, notification duplication, webhook reconciliation and flags/entitlement confusion need feature tests. Prices remain unselected and V2 client finance is not pulled into V1.

## Phase 01 remaining gates

Selected infrastructure vendors are not open questions. Genuinely deferred: identity provider03; email provider before real identity notifications07; AI provider/model21; payment provider28 before paid launch; SMS when phone OTP enabled; official WhatsApp later; custom-domain TLS onboardingV1.5. Conditional Redis/search/telemetry vendors are chosen only if needed. Decoder and local-emulator exact packages, digest pins and runtime builds are implementation qualification—not product features silently declared done.

No decision blocks Phase02 schema/domain planning. This document supersedes only recommendation details, not the master product requirements or release plan.

# Phase 02 — Data Architecture Decisions

2026-09-17. ADR-037–046 refine the approved architecture. In a conflict, these explicitly scoped Phase02 decisions supersede the corresponding Phase01 details; master product scope stays authoritative. Complete definitions: [domain](02_DOMAIN_MODEL.md), [schema](02_DATABASE_SCHEMA.md), [security](02_DATA_OWNERSHIP_SECURITY.md), [migrations](02_MIGRATION_STRATEGY.md). Status means accepted design for this phase's review, not deployed controls.

## ADR-037 — UUIDv7 resource identifiers

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** The current prompt prefers UUIDv7; Phase01 ADR-023 selected v4 before detailed schema review.
- **DECISION:** Use PostgreSQL18 native uuidv7() for new resource IDs; Java transports UUID values without custom generator. Insert-returning or transactional preallocation. Cryptographic256-bit random secrets separate.
- **ALTERNATIVES:** Retain v4; install a Java UUID generator; public numeric IDs.
- **CONSEQUENCES:** Explicitly supersedes only UUIDv4 default in ADR-023/01 production§8. Fixed deterministic reference IDs exempt; no existing rows to migrate.
- **RISKS:** Creation timestamp inferable; IDs never secrets; locality benefit unbenchmarked. PostgreSQL18 compatibility still needs executable test.

## ADR-038 — Hybrid versioned portfolio and project storage

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** Six renderers and publication isolation require both flexible presentation and relational integrity.
- **DECISION:** Normalized roots/revisions/section order/media/project links; strict small schema-versioned JSON for typed section content/theme/business presentation; freeze published payload and children.
- **ALTERNATIVES:** One JSON blob; fully relational table per section; six theme schemas.
- **CONSEQUENCES:** Immutable pins and optimistic drafts; real project revision separate from portfolio; new-draft restore. JSON resource IDs prohibited; typed FKs.
- **RISKS:** Guard triggers and schema evolution must be qualified; restore cannot bypass live takedown.

## ADR-039 — Typed controlled taxonomy

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** Business/services/categories/style/property/material/location need extensibility without conflating dimensions.
- **DECISION:** One small taxonomy_terms table with kind+stable code, composite typed FKs; geographic country/region/city separate; code enums for behavior/state.
- **ALTERNATIVES:** PostgreSQL enums for editorial vocabulary; repeated lookup tables; untyped strings/EAV.
- **CONSEQUENCES:** Deterministic reference seeds, active/retired history, future translations/product catalogue seam. All eight business types retained.
- **RISKS:** Wrong-kind FK and label/search invalidation need tests; no unlicensed fabricated city seeds.

## ADR-040 — Private media originals and typed usages

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** One photograph may be cover/gallery/story/reference; unsafe polymorphic ownership cannot ensure FKs.
- **DECISION:** MediaAsset/Recipe/Variant plus first-class typed consumer usage joins; original always private, private_only restrictions, explicit live-public manifest, immutable watermark versions.
- **ALTERNATIVES:** URL arrays; generic owner_type/owner_id relation; duplicate masters per usage; browser watermark.
- **CONSEQUENCES:** No generic media_usages table: shared domain interface and reverse UNION view over constrained joins. Versioned source/config/recipe/AI label, finite responsive variants.
- **RISKS:** Reverse-reference inventory must include every new usage; IAM/decoder/stamp/private-only gates untested.

## ADR-041 — RLS and role-specific access contracts

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** Tenant filtering alone can be omitted and connection pools can leak context.
- **DECISION:** FORCE RLS on tenant-private tables; parameterized transaction-local verified context; composite tenant FKs. Separate self/customer/grant/case/public functions and narrow DB roles.
- **ALTERNATIVES:** RLS as sole authorization; blanket admin BYPASSRLS; frontend tenant authority.
- **CONSEQUENCES:** Application policy primary. No runtime table ownership/superuser; public raw-table grants forbidden. Trusted app context is not proof against compromised app credentials.
- **RISKS:** Pool/definer/policy composition tests mandatory03; backup must prove all-tenant completeness.

## ADR-042 — Usage accounts, reservations and append ledger

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** Concurrent AI/storage/project/seat usage requires atomic safe accounting.
- **DECISION:** Locked accounts with immutable signed-delta ledger and unique scoped reservations; integer credits/occupancy, bigint minor-unit money, fixed-decimal vendor cost.
- **ALTERNATIVES:** Mutable user monthly_ai_count; analytics-derived usage; floats; provider browser return grants.
- **CONSEQUENCES:** Same-DB reserve/job/outbox and settle/output/audit transactions; period-bound holds, replay keys and no negative spend. No network inside transaction.
- **RISKS:** Counter drift/double refund/unknown acceptance require concurrency and provider qualification; limits/prices unselected.

## ADR-043 — Canonical AI reconciliation naming and lineage

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** Phase02 asks RECONCILIATION_REQUIRED while Phase01 used RECONCILING; both describe same required uncertainty.
- **DECISION:** Use QUEUED/PROCESSING/SUCCEEDED/FAILED/CANCELLED/RECONCILIATION_REQUIRED; created/reserved internal atomic submission steps. Persist attempts/intent before external call; never blind retry uncertain work.
- **ALTERNATIVES:** Keep multiple inconsistent machines; convert timeout to FAILED; auto-refund expired holds.
- **CONSEQUENCES:** Explicit spelling refinement of ADR-028; preserves unknown outcome evidence, private multi-reference/masks/outputs and immutable parent lineage. User settles once on durable usable output.
- **RISKS:** Provider lacking lookup/idempotency may need manual evidence; stale workers cannot force success/charge.

## ADR-044 — Purposeful trash and reference-aware purge

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** History/privacy/account closure need recovery without mass cascade or data resurrection.
- **DECISION:** D overlay only recoverable roots; explicit deletion requests/tasks/tombstones; RESTRICT FKs and ordered owner purge; minimal pseudonymous actor/studio shells where retained history requires.
- **ALTERNATIVES:** Soft-delete every row; cascade entire studio; delete DB row and assume S3 removed.
- **CONSEQUENCES:** No slug recycling; restore revalidates; independent journal replayed before recovered DB goes public. Retention configurable pending review.
- **RISKS:** Backups/provider deletion/object versions and legal holds need real drills/policy; no legal periods asserted.

## ADR-045 — Revision-local SEO and owner-based aliases

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** Metadata should follow exact content; canonical URLs must not permit arbitrary cross-owner targets.
- **DECISION:** Common SEO value contract stored on portfolio/project revisions and curated landing_pages; canonical derived from slug/domain registries; robots preference can only restrict.
- **ALTERNATIVES:** Unsafe polymorphic SEO EAV; duplicate studio/portfolio SEO; arbitrary canonical URL override; alias chains.
- **CONSEQUENCES:** Permanent scoped claims point to owner→current slug; unique constraints and transaction locking; live quality/publication gate for indexability.
- **RISKS:** Custom-host verification/SSR/cache removal tested later; no ranking guarantee.

## ADR-046 — Release-scoped schema blueprint

- **STATUS:** Accepted Phase02 design.
- **CONTEXT:** Current repo is documentation-only;94-table architecture spans normalized relationships and operations across full roadmap.
- **DECISION:** Seven Markdown design artifacts;84 V1 tables introduced incrementally,10 V1.5 extension tables deferred; no executable migrations now, no speculative V2 estate.
- **ALTERNATIVES:** Install every table in security phase; premature scaffold; omit later data seams.
- **CONSEQUENCES:** Refines roadmap executable expectation per current user instruction. Requirements retain original status; future migrations derive/test catalogue.
- **RISKS:** Structural design validation is not production SQL/RLS/restore evidence; first executable work needs qualified toolchain.
