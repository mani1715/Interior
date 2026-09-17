# Phase 02 — Index and Query Strategy

2026-09-17 • Query/index design, not benchmark results. [Schema](02_DATABASE_SCHEMA.md) lists table indexes; [security](02_DATA_OWNERSHIP_SECURITY.md) defines RLS/field access. No sharding, table partitioning, external search or Redis introduced.

## 1. Transactional indexes versus later search

Primary/unique constraints already produce B-tree indexes; do not duplicate them. PostgreSQL does not automatically add an index on every referencing FK. Index tenant+parent/list/time combinations when lookup, relationship enforcement or deletion planning uses them. Cross-row CHECK is not a safe substitute for FK/unique constraints. [PostgreSQL constraint reference](https://www.postgresql.org/docs/18/ddl-constraints.html).

First owner's migration includes PK, tenant composite unique keys, required uniqueness and the catalogue's core list/FK/lease indexes. Discovery GIN/trigram indexes belong to Phase25 when actual query plans and language corpus can be measured. Version tables need both composite FK target key and efficient parent/version lookup; assess overlapping indexes before removing any constraint-backed key.

| Query / purpose | Predicate and order | Index / bounded loading |
|---|---|---|
| Public studio by slug | normalized exact slug → claim owner → CURRENT + active studio/live portfolio | studio_slug_claims unique(slug), partial unique(studio_id) CURRENT, portfolios unique(studio_id). Constant-size root/version joins; no profile draft scan |
| Public project by scoped slug | (studio_id,slug) → project root/current revision | project_slug_claims unique(studio_id,slug), project PK and composite root/revision FK keys; exact lookup |
| Studio published project list | studio_id, state=PUBLISHED, deleted_at IS NULL, descending(created_at,id) | projects(studio_id,state,created_at,id). If actual order published_at adopted, persist/query a stable root projection and index exact expression; don't pretend revision timestamp uses this index |
| Dashboard drafts / portfolio publish | tenant, root, state/version | portfolio/project revision unique(parent,number), partial unique draft/published; root PK lock; section(version,position) |
| Project list with cover | page roots/revisions then COVER rows in one bounded query | project_media partial unique(project_revision_id) role=COVER; batch media active recipes/variants by returned asset IDs |
| Gallery / transformations | one authorized revision, position,id | project_media unique(revision,position); transformation(root revision,position), items(transformation,position). Separate cursor page rather than cartesian eager joins |
| Portfolio featured projects | exact version/section and ordered pinned revision IDs | portfolio_project_links(section,position), composite revision keys; batch public projections and recipe manifests |
| Tenant media library | studio,state,created_at,id + optional album/tag | media_assets(studio_id,state,created_at,id); album_items(album,position); asset_tags(tag,asset) reverse. Exclude source keys from summary DTO |
| Lead inbox | studio,status,created_at,id | leads(studio_id,status,created_at DESC,id DESC); limited page. Fetch latest history/next follow-up in bounded second query, not one query per card |
| Customer own enquiries | customer_user_id,created_at,id | leads(customer_user_id,created_at DESC,id DESC); projection excludes internal notes/assignee/attribution |
| Follow-up queue | studio,follow_up_at and active lead | partial(studio_id,follow_up_at) nonnull; V1.5 only scheduled reminders |
| AI history | studio,created_at,id; selected output summary | ai_generations(studio_id,created_at DESC,id DESC); outputs(generation,selected); at most one display summary per job, detail references fetched on demand |
| Unknown AI work | status,reconcile_after or attempt lease expiry | generation(status,reconcile_after,id), attempts(state,lease_until,id); no automatic re-submit |
| Active membership bootstrap | user,status,studio | studio_members(user_id,status,studio_id); unique(studio,user). Active OWNER count under studio row lock |
| Quota reserve | account by tenant,entitlement,period | unique account key; SELECT FOR UPDATE or conditional guarded update. Pending reservations(account,state); ledger(account,created_at,id) for reconciliation |
| Provider callback/webhook dedup | provider,account,environment,event_id | unique inbox tuple, state/time retry index; receipt payload pruned independently from durable IDs |
| Outbox / processing jobs | pending due time; claim bounded SKIP LOCKED batch | partial due-time indexes; lease expiry indexes. Fenced updates qualify id+epoch+state |
| Revoked/expired grant/session | token_hash exact, then live predicates | unique(token_hash); session user/revoked index; grant expiry index for cleanup. No raw token index |
| Source reference/purge audit | studio,asset_id across typed usage tables | reverse asset indexes on project_media/portfolio_media/references/attachments/albums/tags/evidence; direct input/mask/logo/recipe references also indexed as below |
| Tenant analytics | studio,day,metric/source | daily_metrics unique composite; raw event(studio,occurred_at,id) and retention occurred_at |
| Slug/domain retirement | owner/state or host lookup | immutable claim unique key, current partial key; future custom_domains active hostname unique and revalidate index |

Catalogue entry “FK reverse lookup” means a B-tree with the FK's tenant and target columns. Required additional reference-cleanup indexes when those owners are introduced: ai_generations(studio_id,input_asset_id), (studio_id,mask_asset_id) where mask nonnull; ai_generation_outputs(studio_id,asset_id) (existing unique asset may cover lookup); portfolio_versions(studio_id,logo_asset_id)/(studio_id,favicon_asset_id) where nonnull; watermark_versions(studio_id,logo_asset_id); media_recipes(studio_id,watermark_version_id); media_assets(studio_id,source_asset_id); portfolio_project_links(studio_id,project_id,project_revision_id); preview_grants(studio_id,portfolio_version_id). Include all retained history in reference counting, not just current pointers.

Do not automatically index every FK. Low-cardinality tiny reference tables, rare actor lookups and bounded configuration may use sequential scans; record deliberate deferral with realistic EXPLAIN at implementation. Security policy lookup paths need performance tests as well as positive/negative authorization tests.

## 2. PostgreSQL public search, Phase25

project_search_documents and studio_search_documents are rebuilt from owner-approved published projections via outbox plus periodic reconciliation. Their aggregate_version prevents older event from overwriting newer projection. Live owner views filter stale unpublished/suspended/deleted records at query time, including facets/counts/suggestions. Project IDs and typed fact joins remain relational; no arrays of private source IDs or arbitrary JSON facet map.

Create a GIN(document) tsvector index on curated search text only at Phase25. Build document in projection worker from title/business name (highest weight), description/summary and approved category/style/service/city labels. Do not attempt generated-column expressions that read changing other tables; cross-table label changes emit reindex work. English configurations and simple/Unicode text strategy require representative Telugu/English tests before choosing language-specific stemming. No promise of equivalent multilingual relevance. PostgreSQL documents GIN as a preferred text-search index: [text-search index reference](https://www.postgresql.org/docs/18/textsearch-indexes.html).

Filter city, category, room/work, style, property, service, budget and verified status through indexed source-approved relation views; choose measured B-tree combinations rather than every filter permutation. Initial city/list index useful, optional property/city combinations depend actual selectivity. Budget search only on public display range and matching currency; no comparing INR to USD without an explicit reviewed conversion service.

pg_trgm is optional for misspelled business/project names after actual query evidence; extension installation is controlled migration, no universal trigram index on every text field. Prefix/term query limits, ranking and cursor tie-breakers reviewed in25. Dedicated engine evaluation only if optimized representative search misses Phase01 p95<=300ms or multilingual/relevance needs exceed PG without harming transactions.

## 3. Pagination and N+1 control

Private lists default20/max100, cursor includes opaque signed/bounded tenant+filter+sort+last tuple. Stable order(created_at DESC,id DESC) and predicate(created_at,id)<(:time,:id); UUIDv7 is tie-breaker, not replacement for explicit created_at. Reject cursor reuse across tenant/filter, compare timestamps/IDs exactly. Never trust cursor tenant as authority.

Public SEO uses finite bounded page links with self-canonicals, as Phase01 specifies. Small approved page offsets may be acceptable initially, but cap crawlable depth/query work and use keyset-backed navigation or page-boundary projection for growing collections. Do not create infinite parameter URLs or rely permanently on million-row OFFSET. Ranking cursors include projection/rule version; state may change between pages, so no false snapshot-consistency guarantee. Admin exports stream bounded keysets/as-of views through jobs, not unbounded UI requests.

Fetch bounded parent page first, then bulk children via owner read contracts using IDs. Do not solve N+1 by globally eager-loading every gallery, section, variant and reference. Suggested request budgets: project list2–4 bounded SQL owner read calls; portfolio core1+batched sections/projects/media groups; gallery2–3; leads1+optional bounded history; AI history1+bounded selected-output query. These are test budgets, not measurements. Return summaries with cover variants/name/location/status; load full gallery, private prompts, notes and reference instructions only on authorized detail demand. No giant originals in mobile API payload.

## 4. Concurrency and database cost controls

Optimistic M version columns on editable roots/drafts/leads/settings; immutable published payloads reject writes. Stronger locks for quota, owner transfer, publishing and deletion. Root pointer FK validates tenant and parent at commit; partial unique current/draft statuses prevent duplicates. Reordering uses deferrable order uniqueness where required, or temporary offset within one transaction, never transient public duplicate ordering.

Worker claim uses short transaction SELECT FOR UPDATE SKIP LOCKED, increments lease_epoch, commits then performs external work. Completion requires matching epoch and allowed current state; old worker cannot overwrite new owner. External accepted AI work is reconciled after lost lease, not resent. Counter/account reconciliation includes retained immutable ledger; events dedup transactionally.

Bound SQL statement/lock timeouts, pool sizes across API/worker replicas, max filters/query text, batch sizes, row/payload limits; observe slow queries/locks/autovacuum/dead tuples. Index maintenance cost and write amplification matter; retire unused nonconstraint indexes only after evidence. Analytics separate write flow/table retention prevents page views from locking core rows. No initial partitioning/sharding; evaluate after measured retention/maintenance/query pressure.

## 5. Implementation evidence required

Run EXPLAIN (ANALYZE, BUFFERS) with production-shaped synthetic data (including100projects×30images per representative studio), correct runtime RLS role/context, skewed tenants and concurrent writers. Measure warm/cold summary/search, index selectivity and number of queries. Verify FK/unique/pointer race failures, publish/purge locking and one-credit contention under real PostgreSQL18, not H2. No benchmark, load-test, database query plan or performance PASS is claimed by this document.
