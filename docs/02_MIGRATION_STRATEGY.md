# Phase 02 — Migration and Validation Strategy

2026-09-17 • Blueprint only. No SQL migration was created or executed. [Database dictionary](02_DATABASE_SCHEMA.md) is the normative schema blueprint; implementation SQL must be qualified on real PostgreSQL18.x before any production claim. User's Phase02 instruction supersedes earlier roadmap expectations of executable schema in this phase.

## 1. Flyway ownership and naming

One ordered migration history across module-owned schemas in one database/environment. Future location under the API/database composition, shared with workers through one release artifact; modules contribute reviewed scripts, never independent racing auto-migrations. Names: V0001__identity_users_and_roles.sql, V0002__designer_tenant_membership.sql as illustrative convention only; globally unique increasing versions, snake_case description and SQL suffix. Actual version numbers assigned when scripts exist, not preallocated to94 speculative files.

Pin compatible Flyway core plus PostgreSQL database support dependency through tested Spring Boot build; selected baseline PostgreSQL18.6/Java25 retained. Do not invent untested dependency versions or install tools in this documentation phase. Applied permanent-environment migration is immutable; change via new migration. Flyway tracks checksums and ordered versions: [official versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations).

No ORM auto-DDL in staging/production. Baseline only when adopting a verified existing database, never mask unknown drift with baselineOnMigrate. clean disabled outside explicitly disposable local/test databases. Repeatables only for reviewed replaceable views/functions where dependencies/version compatibility proven; core tables/reference seeds and security policy changes versioned.

## 2. Implement progressively by owning feature

| Stage | Migration scope after separate authorization | Required gate |
|---|---|---|
| Phase03/07 security | User/identity mappings/roles/session/OIDC transient state; minimal studio membership schema needed for real two-tenant authorization; audit/outbox/security contracts used by working code | Runtime/schema-role separation, actual OIDC decision, RLS/CSRF/session/MFA tests |
| Phase08/10 onboarding/portfolio | Typed taxonomy/location reference baseline, studio contacts/service areas/slug reservation, portfolio version/sections/pointers/preview grants | Owner invariant, pointer/status/immutable graph, reserved routes, optimistic edits |
| Phase18/19 projects/media | Structured revisions/taxonomy/media/story/attestations; upload/assets/recipes/variants/jobs/watermarks/library; usage reservation foundation needed before consumption | Private originals, tenant FKs, processing/publication gates, accounting/purge races |
| Phase20/25 SEO/discovery | Curated landing pages, published project/studio search projections and measured indexes | Live eligibility, no thin pages, namespace aliases, counts and facets |
| Phase21–24 AI | Jobs/references/masks/outputs/attempts/callbacks and private review grants; billing quota/ledger already real | Unknown submission/reconciliation, double-settlement and grant revocation |
| Phase26–29 leads/operations/billing | Lead inbox/history/notes/attribution/attachments, notifications, bounded analytics, full subscription/payment webhook and admin operations | Durable lead acceptance; paid capability only after provider/ledger tests |
| V1.5 | Ten deferred tables plus explicit later additions such as invites/boards when feature spec approved | Full feedback/verification/collections/domain tests before enabling |

Do not create all84 V1 tables to “complete security.” Cross-schema FK introduction waits until both owners exist; feature requiring missing dependency remains disabled, never ships with a fake integrity placeholder. Roots needed for later references can precede feature UI. Add circular root/version FK after both tables in same transactional migration; constraints deferred only where essential. Reference/media publication guard triggers belong with owner integration migration, not silently omitted.

## 3. Constraints and write-contract qualification

Translate every catalogue bundle, column/default/null, PK, composite FK, unique/partial unique, CHECK, index, deletion and privacy rule into reviewed SQL. Do not treat prose conditions as magically enforced. In particular:

- Every T row has tenant FK and unique(studio_id,id); nested references use tenant composites. Revision pointers include parent ID. Schema names/constraints deterministic; no abbreviated FK target silently changed.
- CHECKs reject enum typos, nonpositive dimensions, negative amounts/positions, invalid budgets/paired dates/currency/hash lengths, incompatible polymorphic target combinations and out-of-range watermark/focal config.
- Cross-row invariants use owner write transactions and explicit guards/constraint triggers: active studio owner, root pointer/status agreement, immutable sealed revision and child graph, published media policy, typed entitlement values, source/recipe ready completeness.
- Changes to studio_id, identity IDs and ownership-bearing parent references are disallowed after insert; not ordinary editable fields. Platform roles and billing adjustments require dedicated privileged commands/audit.
- Root/version cyclic constraints use NO ACTION DEFERRABLE, not RESTRICT disguised as deferred. Statement order archives old published row before promoting new when partial unique current status applies; transaction rollback restores prior live version.
- Runtime no DDL/table ownership/bypass; ENABLE/FORCE RLS applied in same migration before grants. Never expose a table between creation and policy grant. Token hash fields not returned through generic entities.
- Application schema validation covers every JSON write, with database structural/size guards and matching versions. Test invalid config, asset IDs smuggled into section JSON, unsupported theme settings and oversized vendor payload.
- FKs keep tombstone user/studio identities as needed; controlled cleanup deletes children in dependency order. Do not default CASCADE entire tenants. Pure join deletion is explicit; no SET NULL of tenant owner to produce unowned data.

A blueprint check cannot establish that these SQL triggers/policies are correct; executable schema qualification is a required next implementation gate.

## 4. Deterministic seeds and environment controls

Reference data: fixed documented UUID literals/codes for roles, taxonomy kinds/terms and reserved namespace. No generated random ID per repeatable seed run. Seed updates assert expected old version, don't silently overwrite admin-curated data with ON CONFLICT updates. Retire terms rather than deleting referenced history. Geography import includes source/license/version and parent consistency; do not seed fabricated locations. Plans/prices wait for reviewed product config; provider-free pilot assignment can exist with explicit capability values, not invented paid prices.

Dev sample tenants/projects/AI doubles live in test/dev fixtures outside production migration locations. Production bootstrap rejects test provider mode for enabled features. Separate local/test/staging/production database accounts/secrets/KMS/provider account/env IDs; never copy unredacted production data to developer fixtures. Runtime does not run Flyway on every API/worker start.

Local reset only explicitly disposable database/container volume after verifying environment/name and no production endpoint; no automated recursive workspace delete. CI uses fresh PostgreSQL18 container per relevant integration suite. Actual Docker/PostgreSQL setup is prerequisite for these checks, not a fabricated Phase02 PASS.

## 5. Expand, migrate, contract

1. Inventory current schema/history, validate checksums, dependency/collision checks, estimate locks/table size, verify backup/PITR health.
2. Expand with backward-compatible nullable columns/new tables/views/contracts. Default changes evaluated for rewrite/locking; no assumption all DDL free.
3. Deploy compatible writers/readers; backfill bounded resumable keysets with checkpoint/metrics, no single massive blocking transaction.
4. Validate constraints/data parity and new reads, observe production metrics. NOT VALID/VALIDATE where PostgreSQL supports it, never leave critical unvalidated tenant constraint indefinitely.
5. Contract only after all API/worker versions and replayable events compatible; remove old columns/indexes in separately reviewed release.

Indexes on large live tables may need CREATE INDEX CONCURRENTLY in a separately configured nontransactional migration; it cannot be treated as ordinary transactional DDL. Failure can leave invalid index, requiring inspection and forward remediation. [PostgreSQL CREATE INDEX](https://www.postgresql.org/docs/18/sql-createindex.html). Set bounded lock_timeout/statement_timeout suited to migration; abort safely instead of unbounded write outage. Parallel service replicas must not race migration runner.

Deployment: native build/tests → same artifact staging migration → upgrade/backward-compatibility/security smoke → one approved production migration job → compatible rollout → monitored smoke. Never automatically repair checksum/history after failure. Application rollback only if expanded schema compatible; normally forward-fix SQL, not destructive down migrations. PITR is disaster recovery with coordinated media/provider reconciliation, not convenient routine migration rollback.

## 6. Retention, outbox and replay safety

API idempotency key is scoped to verified actor/anonymous form intent+studio+operation; stores hash/request fingerprint/outcome pointer, not raw sensitive response. Default replay>=24h; new conflicting payload same key409; result reauthorized on replay. Domain uniqueness (generation reservation/submission key, provider transaction/event, upload completion identity) persists beyond API-key expiry.

Outbox event retained until every destination dispatched, consumers have progressed or retained replay source, and recovery window policy allows purge. Completed payload proposed30days; consumer receipts kept at least queue redelivery+DLQ/manual replay+restore horizon. If old event can be replayed after receipt expiry, durable business unique keys/aggregate version must still reject duplicate effects. Never prune inbox dedup when its provider can legitimately replay into a charge/refund twice. Keep minimal provider event ID/hash/state after diagnostic payload expiry as policy requires.

No partitioning initially. Small bounded daily deletes, relevant time indexes and vacuum/metrics; bulk retention jobs use least-privileged role and legal-hold checks. Redacted payloads not retained as permanent diagnostic warehouse. Deletion journal exported independently, restores replay it before public service, per [security](02_DATA_OWNERSHIP_SECURITY.md).

## 7. Required executable migration test matrix (not run)

| Test | Expected evidence |
|---|---|
| Empty → latest schema | Correct module schemas,94 only when all respective feature migrations exist; zero unqualified auto-DDL |
| Last release → next + old app | Successful upgrade with representative fixtures; old compatible release reads/writes during rolling deploy |
| Applied migration modified | Flyway validate fails; no automatic repair |
| Constraint fixtures | Cross-tenant nested FK, duplicate slug/member/webhook/current revision, bad budget/currency/watermark/hash all fail |
| Published snapshot | Draft edits isolated; no child mutation of sealed revision; old pins stable; withdrawal live override |
| Two concurrent publishers / purge | At most one selected live pointer, no reference to purged asset, no partially sealed graph |
| One remaining credit / replay | Exactly one successful reserve; sum ledger/counters agree; callback and polling cannot double consume/refund |
| Pool leakage / roles | Alternating tenantA/B on single connection, missing context/error/timeout denied; runtime not owner/bypass; public role can't read originals/notes |
| Security-definer functions | Fixed search path, no PUBLIC execute, wrong grant/case/role denied; no dynamic SQL injection |
| Worker fencing | Stale lease cannot commit; uncertain external acceptance reconciles rather than resubmits |
| Backup completeness/PITR | All tenants restored despite RLS; independent deletion journal suppresses newer deleted content; external providers disabled and reconciled |
| Reference seeds / JSON | Deterministic seeds, no production fixtures; unsupported kinds/theme settings/prose URLs/secret payload rejected |
| Provider inbox | Wrong account/environment/signature never becomes trusted event; duplicate/out-of-order status doesn't regress |
| Query plans | Representative skew/3000-image workload under real runtime policy; expected bounded queries/indexes without N+1 |
| Purge failure | DB and object mismatch safe; all object versions/receipts reconciled; no false completed deletion |

Use actual PostgreSQL18 with selected JDBC/Flyway/Java build; no H2 substitution. Real S3/OAC/CDN/AI/payment behavior remains staging/provider qualification in its feature phase.

## 8. Phase02 verification record

Document validation checks table inventory/field contracts, seven required files, eight domain diagram blocks, complete ownership rows, all145 data-traceability IDs, preserved original requirement rows, balanced fences, local links and unchanged nonrequested documents. Exact executed results are appended after final review. Conceptual privacy/tenant/race review is recorded in [security](02_DATA_OWNERSHIP_SECURITY.md).

No application build, SQL execution, Flyway migration, RLS integration, load/security test, provider call or Mermaid rendering is claimed. No application exists. Host inspection found Java executable but no psql, Docker or Maven on PATH; the prior audit records Java18 versus selected25. These do not block this documentation phase. Executable Phase03 needs a qualified Java25/Maven build and reachable PostgreSQL18 test environment (local Docker or other approved equivalent); setup is next-phase work, not permission to begin it here.

Phase03 — Security Foundation is the next phase. Stop for user review.

## 9. Phase02 prompt coverage and recorded results

| Phase02 instruction sections | Design / verification evidence |
|---|---|
| 1–2 | Full prior-document review; domain aggregates and explicit refinements in domain model§1–2 |
| 3–6 | Schema conventions, typed columns/common bundles, UUIDv7 ADR-037 and optimistic locking |
| 7–13 | User/identity/roles, studios/members/contacts, typed business/services and geographic tables |
| 14–18 | Portfolio root/version/sections/relational links; six-theme controlled JSON contracts and brand FKs |
| 19–26 | Project root/revisions/terms/private details, publication/verification separation, budget/date rules |
| 27–34 | Media assets/typed usages/recipes/variants/watermark versions, transformations and private originals |
| 35–37 | Revision SEO/curated landing pages; owner-current slug claims and derived indexability |
| 38–43 | Generation/references/outputs/lineage/masks/attempts/callbacks; usage hold/ledger/reconciliation |
| 44–45 | V1 review sessions/items and deferred immutable typed concept_feedback |
| 46–49 | Leads/notes/history/attribution and contact/budget/follow-up/assignment constraints |
| 50–54 | Deferred customer collections/reviews/dimensions/verification; V1 reports/actions typed targets |
| 55–61 | Bounded analytics; plans/entitlements/subscriptions/usage/payment inbox; notification separation |
| 62–65 | Outbox fan-out/consumer receipts; scoped idempotency; append audit; external security telemetry |
| 66–70 | Complete94-row ownership matrix; explicit RLS/principals; tenant FKs/unique/CHECK/delete behavior |
| 71–74 | Selective D bundle, trash/restore, deletion tasks/tombstones and configurable retention classes |
| 75–80 | Query/index/pagination/N+1 plan, concurrency locks, exact transactions and domain events |
| 81–89 | Flyway/seeds/PII/encryption/roles/PITR; DB-object/provider reconciliation and webhook replay |
| 90–98 | Seven domain ER diagrams plus high-level map; seven complete design documents; Markdown schema blueprint |
| 99–102 |145-ID supplemental data coverage, ADR-037–046, release-scoped84/10 catalogue and future seams |
| 103–107 | Conceptual adversarial review; six shared themes; watermark recipe chain; blue/white/handle/mask and before/V1/V2/approved/actual examples |
| 108–109 | Summary/detail query contracts, bounded mobile payload and batched read plan |
| 110–112 | Seven requested files plus only three requested updates; checklist/report/stop-before03 |

Executed document checks on2026-09-17:

- Seven required Phase02 files exist and are nonempty; workspace now24 files, with no application/scaffold/migrations.
- Catalogue has94 unique table names,84 V1 and10 V1.5; each has columns/types/null/default, PK, FK, unique, checks/invariants, indexes, deletion and PII contract. Ownership matrix has the same94 names with none missing.
- All145 original ten-column requirements compare exactly with pre-edit baseline, including text/priority/phase/status. Supplemental Phase02 has145 unique IDs:96 concrete data-model mappings,24 deferred extension paths,13 configuration/query contracts and12 governance mappings. Zero implementation promotions.
- Domain document has eight Mermaid source blocks (seven ER plus high-level map); lifecycle document adds two state diagrams. Code fences balanced; local Markdown file links resolve. Diagram source reviewed, not visually rendered.
- Only the seven Phase02 documents and three requested Phase00 update files changed. SHA-256 comparison confirms master, Phase00 audit/roadmap and all eight Phase01 files unchanged.
- Manual graph review covers parent-qualified revision FKs, same-tenant media/AI/lead references, customer collection ownership, reserved aliases, watermark/private-source gating, optional mask fields, multiple same-type references, ledger/callback races and purpose-scoped RLS. No unresolved design contradiction identified.

All31 Phase02 completion checklist obligations are represented: domain/ownership/tenancy/RLS; immutable portfolio and six shared themes; structured projects; private originals/variants/watermark versions; multiple typed AI references/masks/history/reconciliation; before→concept→actual and private review; leads/history; SEO aliases; billing/entitlements; outbox/audit; PII/retention; indexes/migrations; strictly validated JSON; no URL-array media authority, float money or frontend ownership.

These results are document/design evidence only. Runtime migration, constraints, RLS, concurrency, storage/provider behavior, actual diagram rendering and production restore remain unexecuted future gates. Phase02 design PASS does not close the36 OPEN risks or declare any feature implemented.

