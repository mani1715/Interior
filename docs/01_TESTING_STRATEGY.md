# Phase 01 — Testing Strategy and Architecture Review

Status: test architecture plus Phase01 document-validation evidence. Application tests are future requirements; none are represented as passing against nonexistent code. Product matrix: [00_REQUIREMENTS_MATRIX.md](00_REQUIREMENTS_MATRIX.md). Foundation: [production architecture](01_PRODUCTION_ARCHITECTURE.md). Risks: [00_RISK_REGISTER.md](00_RISK_REGISTER.md).

## 1. Pyramid and tooling selection

Prefer many fast rule/unit checks, fewer realistic integration tests, and a small reliable set of critical E2E journeys. Do not write tests that merely mirror implementation or replace actual authorization/provider contracts with mocks. Coverage percentages alone are not release gates.

| Layer | Scope | Planned approach |
|---|---|---|
| Frontend unit | Formatting, nontrivial state transforms, mask coordinates, view-model mapping | Vitest compatible pinned version at first web scaffold; deterministic inputs |
| Frontend component | Forms/errors/keyboard/modal/focus/theme section behavior | Testing Library + DOM environment; meaningful behavior not snapshots alone |
| Frontend integration | Server/client route composition, generated transport DTOs, no-store behavior | Test harness with actual route responses and bounded synthetic fixtures |
| Web E2E | Real browser/server/API journeys and no-JS public crawl | Playwright, Chromium plus representative WebKit/mobile tests; actual API/test database |
| Backend unit | Quota/state/provenance/slug/money rules | JUnit from compatible Boot-managed test baseline, clocks/ports injected |
| Backend application | Use-case transaction and authorization orchestration | Tests with real policies, controlled provider adapters and rollback assertions |
| Repository/integration | PostgreSQL RLS/FKs/locking/migrations/outbox/session data | Testcontainers PostgreSQL18.6; no H2 substitute for security/SQL semantics |
| API/contract | DTO validation/errors/status/idempotency/OpenAPI client compatibility | HTTP integration plus schema/consumer contract checks |
| Architecture | Module import allowlist, layering, provider adapter isolation | ArchUnit plus graph checks; domain cannot import controllers/vendor SDKs |
| Provider contract | S3/SQS/identity/AI/payment/email behavior | Local emulator where appropriate and dedicated real staging/sandbox tests |
| Security | Tenant/role/grant attack matrix, upload/header/webhook controls | Adversarial integration/E2E plus scans and targeted manual review |

Tool versions beyond the selected runtime baseline are pinned when real dependencies are introduced, verified against Java25/Boot4.1.1 and Next16.3.3/React19.3.0/TS6.0.3. No unsupported plugin version is invented or installed now. Application test doubles live only in test/dev profiles; production bootstrap rejects fake providers for enabled real capabilities.

## 2. Critical E2E matrix

| Journey | Required assertions | Execution phase |
|---|---|---|
| Professional signup/recovery | OIDC binding, email/phone action verification, session rotation/logout/device revoke, no role injection | 03/07/08 |
| Create project | Tenant-scoped draft, fact/taxonomy validation, optimistic edit conflict, recoverable phone form | 18 |
| Upload media | Size/content validation, resumable completion, checksum/version race resistance, quarantine invisible | 19 |
| Publish portfolio | Stable snapshot, exact referenced revisions, only READY authorized derivative manifests | 10 integrated18–20 |
| Watermark visible | Logo/name fallback, all five positions, opacity/scale constraints, thumbnail/cover/OG/AI-label correctness | 19/20 |
| Clean original unavailable publicly | Direct bucket/CDN/guessed key/optimizer/changed URL path denied; user authorized download isolated | 19 |
| AI concept generation | Owned main/reference/mask, private result, capability honesty, no duplicate charge on replay/timeout/crash | 21–24 |
| Lead capture | Durable before confirmation, correct studio attribution, inbox tenancy, spam protection, safe WhatsApp | 26 |
| Cross-tenant attack | DesignerA substitutes B's project/asset/reference/job/lead/review/billing/analytics IDs; queries/counts deny | Every protected feature |
| Admin permissions | Designer cannot escalate; Moderator limited; Admin MFA/step-up; Super Admin routine-use restrictions; audit | 03/07/29 |
| Client share | Anonymous scoped access works; comment privilege separate; expiry/revoke blocks page and media; approvals bind version | 24/V1.5 |
| Publish/unpublish/rename | Live eligibility, search/sitemap/canonical refresh, aliases and CDN takedown bound; no draft leakage | 20/25 |
| Billing | Browser redirect grants nothing; signed webhook verified, wrong tenant/amount/currency rejected, replay/refund once | 28 |
| Delete/restore | Grant revoke, no stale worker resurrection, reference-aware purge, tombstones replayed after backup restore | 19/29 |

Use separate tenants, customers, scoped team members and privileged actors. Table-driven action/resource/classification/state tests cover nested and batch IDs, list counts, pagination cursor reuse, export/download and job status, not only detail GET. Include two concurrent requests for quotas/claim/slug/publish/delete and worker redelivery. Passing a happy-path demo never establishes isolation.

## 3. Security verification gates

PR gates: secret scan, dependency/lockfile/SBOM vulnerability scan, SAST, unsafe logging/bundle checks, module graph and relevant authorization regression tests. Container/native decoder scan when those artifacts exist. Current critical/high findings require triage with owner; never blanket-suppress detectors. CI action/dependency pinning and license checks included.

Runtime suite: IDOR/BOLA and RLS pool-context cleanup; missing context; migration/runtime role separation; MFA/session timeout/revocation; CSRF including logout; stored/reflected XSS through titles/descriptions/config/JSON-LD; parameterized SQL/injection attempts; upload polyglots/magic mismatch/oversize/decompression/frame bombs/SVG; SSRF including redirects/DNS/private metadata endpoints; rate-limit/account enumeration/OTP abuse; malformed/tampered webhooks and wrong-environment accounts; exact CSP/CORS/referrer/frame/header behavior. Browser bundle must contain no secrets or private source pointers.

Real staging tests are indispensable for S3 bucket/OAC origin policy, presigned constraints/versioning, CDN invalidation/cache headers, SQS redelivery/visibility, provider callback signatures and identity assurance. An emulator passing cannot close those risks. Production-data penetration testing requires separately scoped authorization; synthetic staging tests are the default.

## 4. Mobile, themes, accessibility and performance

Test every major screen at360/390/430/768/1024/1440+ CSS px. Cover 200% zoom/reflow, screen reader landmarks/form errors, keyboard-only dialogs/nav, focus return and touch/non-drag alternatives. axe-style automated checks supplement manual review. Palette test checks actual text/control pair contrast; Charcoal/Bronze approximately5.35:1, white/Bronze3.08:1 is not accepted for normal text. Theme overrides are included.

Shared realistic dataset across BASIC/MODERN/LUXURY/ARCHITECTURAL/WARM_NATURAL/DARK_CINEMATIC: empty portfolio, sparse studio, long names/Telugu text, missing optional sections,100projects/30images each, panoramas/portrait photos, real/before/AI/actual mix, unavailable videos, revoked/deleted assets. Assert distinctly designed navigation/hero/grid/gallery/detail/CTA/type/spacing/motion, not six color snapshots. Each theme keeps a separate phase/design spec. Run version switching/restore with unsupported config feedback and data preservation.

Performance lab: constrained mobile network/CPU/device memory, cold/warm caches, interrupted uploads and long generation polling; measure initial JS/images, LCP/CLS/input responsiveness and request count. Field p75 CWV monitored after sufficient traffic, never inferred from one Lighthouse run. Verify no3D request blocks critical content; GPU/WebGL unavailable and reduced-motion paths render full narrative. Load tests include concurrent quotas, search facets and large media datasets; cap DB connections across all replicas/workers. Metrics targets are defined in production/SEO/deployment docs and must be measured, not assumed.

## 5. Contract, migration, chaos and recovery

Contract-first OpenAPI validation and generated-client drift check in CI; generation rerun must produce no unexplained changes. Verify nullable/optional/error enums, status codes, paginated filters, safe monetary/date types and backwards-compatible v1 changes. Unit-only DTO tests cannot substitute for deployed HTTP contracts.

Migrations: empty database → latest; last released schema → new; invalid/duplicate/checksum mismatch fails; old application remains compatible through expand/contract; tenant FK/RLS/index constraints enforced. Test backfill resumability/lock limits with realistic volumes. Test failed migration blocks deployment and cannot be marked repaired automatically.

Failure drills mirror all rows of [deployment failure matrix](01_DEPLOYMENT_ARCHITECTURE.md): stop DB/cache/storage/CDN/queue/providers/worker mid-operation, inject timeout after provider accepted but before DB commit, duplicate messages, late callbacks, invalid output, failed watermark and notification timeouts. Assert durable acknowledgment, safe pending/503 states, bounded retry and reconciliation, not silent loss. Financial reconciliation verifies reservation/settlement/refund sum invariants under races.

Restore drill: isolated recovered DB/media, providers disabled, tombstones and suspended tenants applied before service exposure, checksum/reference validation, safe job/payment reconciliation, search/cache rebuild and measured RPO/RTO. Recovered backups must contain all tenants despite RLS. Backup success logs alone do not pass recovery acceptance.

## 6. Phase01 architecture quality review

| Review question | Design result | Evidence / future qualification |
|---|---|---|
| No unnecessary microservices? | PASS at design level | Three application types; one modular Java domain/database |
| No circular synchronous modules? | PASS subject to recorded graph check | Explicit dependency table; event contracts separate; enforce later with import tests |
| Clear ownership and layering? | PASS at design level | Facades/application/domain/ports/adapters and owner-only persistence |
| No frontend-only authority? | PASS at design level | Spring action/object policy + tenant-qualified SQL/RLS |
| No public original/reference architecture? | PASS at design level | Private buckets, separate OAC derivative origin, no optimizer path |
| SEO core server-accessible? | PASS at design level | SSR public HTML and live eligibility, no-JS test plan |
| Themes share data? | PASS at design level | Versioned document/registry, independent renderers |
| AI/payment/notification vendors abstracted? | PASS at design level | Named application ports and adapters |
| Search scales later? | PASS at design level | Rebuildable PostgreSQL projection plus SearchProvider |
| Custom domains possible? | PASS at design level | Verified host registry, canonical lifecycle and route isolation |
| Mobile/accessibility and3D fallback supported? | PASS at design level | Six widths, budgets, semantic tokens, static fallback |
| Long jobs queued? | PASS at design level | Transactional outbox, SQS, job leases/dedup/DLQ/reconcile |
| Observability and backup included? | PASS at design level | Metrics/alerts/redaction and restoration/deletion plan |
| Environments separated? | PASS at design level | Accounts/credentials/data/provider guards; no prod locally |

Design PASS means no identified unresolved contradiction in the described architecture, not that controls work. High risks remain R-01/02/08/10/11/17/22/24/25/31 and recovery risks until feature qualification; [security review](01_SECURITY_ARCHITECTURE.md) assigns gates. No critical architecture decision blocks Phase02. Current runtime/tool gaps block claiming a runnable build, not the architecture documentation phase.

## 7. Phase01 prompt coverage

All69 source sections map below. Codes: **P**=[production](01_PRODUCTION_ARCHITECTURE.md); **M**=[modules](01_MODULE_BOUNDARIES.md); **S**=[security](01_SECURITY_ARCHITECTURE.md); **A**=[media/AI](01_MEDIA_AI_ARCHITECTURE.md); **E**=[SEO](01_SEO_RENDERING_ARCHITECTURE.md); **D**=[deployment](01_DEPLOYMENT_ARCHITECTURE.md); **T**=this file; **R**=[routes](01_ROUTE_NAMESPACE.md); **ADR**=[decision log](00_ARCHITECTURE_DECISIONS.md); **RM**=[matrix](00_REQUIREMENTS_MATRIX.md). This is architecture coverage, not new product implementation status.

| Source section | Obligation | Evidence |
|---|---|---|
| 1 | Read six approved documents before decisions | P1 and phase execution record |
| 2 | Complete platform architecture | P3–9, M2 |
| 3 | Modular monolith, selective workers | P4, M7 |
| 4 | Evaluate conceptual system layout | P4, D1 |
| 5 | Stable frontend, public versus private rendering | P2/5, E1 |
| 6 | Meaningful frontend feature structure | P5 |
| 7 | Locked palette and semantic token architecture | P6 |
| 8 | Six shared-data independent theme renderers | P6, M5 |
| 9 | Validated config, no arbitrary JS/HTML/CSS | P6, S7 |
| 10 | Supported Java/Spring and module calls | P2, M2 |
| 11 | Layering with inward dependency | M1/3 |
| 12 | REST/version/DTO/pagination/filter/idempotency | P7 |
| 13 | Safe error envelope and request IDs | P7, S7 |
| 14 | PostgreSQL IDs/time/delete/migrations/transactions | P8 |
| 15 | Systematic multi-tenant ownership | S3, M3 |
| 16 | Authentication/session/token/CSRF/recovery/MFA | S2/4 |
| 17 | Roles plus ownership permissions | S3 |
| 18 | Policy-enforced logical object storage separation | A1 |
| 19 | Keys/metadata/IDs/signed delivery/CDN | A1/2/3 |
| 20 | Non-destructive configurable watermark regeneration | A3 |
| 21 | Background validation/EXIF/orientation/variants | A2 |
| 22 | Compare queue choices and durability | A4 |
| 23 | AI capability provider interface/provenance | A5 |
| 24 | Job lifecycle/credits/retries/refund | A6 |
| 25 | Private owned input/reference/mask roles | A7 |
| 26 | PostgreSQL search abstraction | P9, M2/3 |
| 27 | Rendering/SEO/indexing/error/canonical strategy | E1–5 |
| 28 | Slug collisions/renames/Unicode | R3/4 |
| 29 | Future custom domains | R5, E5, D1 |
| 30 | Redis evaluation/failure | P1/9, D7 |
| 31 | Asynchronous privacy-aware analytics | M6, D7 |
| 32 | Payment/subscription/plan/entitlement/usage separation | P9, M2/5 |
| 33 | Notification channel ports and events | M4/6 |
| 34 | Admin MFA/shorter sessions/step-up/roles/alerts | S4 |
| 35 | Sensitive-operation tamper-resistant audit | S8 |
| 36 | Logs/metrics/traces/errors/job correlation | D5 |
| 37 | Local/test/staging/production env strategy | D2/3 |
| 38 | Reproducible local infrastructure plan | D3 |
| 39 | CI/CD/migration safety | D4 |
| 40 | Test pyramid and critical E2E | T1/2 |
| 41 | Security scans and attack tests | T3 |
| 42 | Monorepo with native tooling | P5, ADR |
| 43 | Proposed meaningful repository layout | P5 |
| 44 | Worker language evaluation | A2, ADR |
| 45 | OpenAPI typed frontend contracts | P7, T5 |
| 46 | UTC instants and separate business timezone | P8 |
| 47 | Non-floating money/currency | P8 |
| 48 | Structured location and private addresses | P8 |
| 49 | Route ownership/reserved slugs | R1–3 |
| 50 | Entity-specific deletion/retention | P8, D6 |
| 51 | CSP/HSTS/nosniff/referrer/permissions/frames | S7 |
| 52 | Endpoint/identity/IP-aware rate limits | S5 |
| 53 | Lightweight feature flags | P9 |
| 54 | Precomputed responsive images and delivery | A3, E6 |
| 55 | Deferred3D/mobile/WebGL fallback budgets | E6 |
| 56 | Stable publishing and project interactions | M5, E4 |
| 57 | SEO/cache events and reconciliation | E4 |
| 58 | Media states and publish eligibility | A2, M5 |
| 59 | Private AI output explicit publish with watermark/label | A7 |
| 60 | Watermark never authorization | S1/6, A3 |
| 61 | Eight files plus ADR/matrix updates | T8, ADR, RM |
| 62 | Eight meaningful Mermaid diagrams | P3/4, M2/5, S2, A2/6, D1 |
| 63 | Major structured ADRs | ADR Phase01 entries |
| 64 | All startup/scale cost centers | D8 |
| 65 | All required outage/failure behaviors | D7 |
| 66 | Architecture-only minimal foundation scope | P1, T8 |
| 67 | Architecture threat review and unresolved risks | S9 |
| 68 | Architecture quality review | T6 |
| 69 | Structured completion report and stop | Final Phase01 report; no Phase02 work |

## 8. Phase01 validation evidence

Executed 2026-09-17: PowerShell file/link/source-coverage inspection plus JavaScript parsing/comparison of requirement rows and depth-first cycle detection over the full module dependency table. Results: all eight required Phase 01 documents nonempty; 17 total workspace files (14 docs plus README/.gitignore/.editorconfig); zero broken local Markdown links; 69 source-section coverage rows with none missing; eight Mermaid blocks; 16 logical modules/59 allowed synchronous edges, acyclic. All 145 original requirement rows compare exactly with the pre-edit baseline (including text/priority/phase/implementation status), and 145 unique supplemental architecture-coverage rows exist. No product source/schema/migrations/scaffold introduced.

Final structural checks **PASS**: all 12 locked palette values and all six canonical theme IDs are present; Markdown code fences balance; all 17 new ADR-020–036 entries have Status, Context, Decision, Alternatives, Consequences and Risks; all ten requested failure categories and all ten requested architecture threat areas are represented. The graph checker validated every allowlisted edge, not just the simplified Mermaid diagram. Requirement count remains 145, with zero product implementation status promotions.

Manual consistency review covers state-name mapping, draft/public versus media readiness, private grant revocation versus bounded public CDN takedown, publication project-revision behavior, new frontend API transport choice, optional Redis and every required failure mode. Phase00 documents retain their historical evidence; roadmap status explicitly records the narrower Phase01 scope rather than leaving “Phase01 not started” as current status.

No application build, unit/integration/E2E/penetration/load test or Mermaid rendering is claimed. No application exists, current Java differs from target and Docker/Maven are unavailable. Diagram source is structurally and semantically reviewed; actual visual rendering can be checked in the documentation viewer. A future implementation phase must establish and execute real toolchain/tests rather than treating this phase's PASS as executable verification.
