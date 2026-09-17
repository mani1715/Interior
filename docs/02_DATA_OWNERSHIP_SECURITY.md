# Phase 02 — Data Ownership and Security

2026-09-17 • Design review only. [Schema](02_DATABASE_SCHEMA.md) supplies tenant-qualified keys; [domain model](02_DOMAIN_MODEL.md) supplies transactions. No live penetration test or RLS execution has occurred.

## 1. Authority and database principals

Application authorization is primary: verified actor → current user state → platform role → current studio membership → action → object owner → lifecycle/classification → entitlement. UUIDs, URL slugs, client tenant headers, watermarks and queue messages confer no authority. Read counts, nested IDs, batch items and cursors obey the same scope.

| Principal | Grants and exclusions |
|---|---|
| migration_runner / no-login schema owners | DDL during controlled deployment; no application connection uses this role. Own functions/tables; policy changes reviewed. No general human everyday use |
| api_runtime | Owner-module DML contracts on permitted tables; no schema ownership, SUPERUSER, BYPASSRLS, TRUNCATE, role creation or arbitrary security-definer EXECUTE. Scoped transaction context; audit append only |
| worker_media / worker_ai / worker_notifications / worker_projection | Separate pools and narrow domain contracts; only needed table/column access and object IAM. Fenced job tenant/purpose checked. No blanket bypass, no browser request can select these pools |
| public_reader | EXECUTE on fixed public DTO/read functions only, no private/base SELECT or DML. Functions expose eligible published fields/derivatives only |
| customer_reader / grant_reader | Narrow own-enquiry/collection and live review gateway contracts; cannot inherit tenant editor role. Safe column allowlist rather than all-row ORM exposure |
| admin_case_runtime | Separate connection pool; EXECUTE on reviewed case-scoped functions with actor/action/tenant/resource/expiry/MFA validation and audit. No blanket cross-tenant SELECT |
| relay_runtime / lifecycle_worker | Minimal outbox/dispatch/cleanup contracts; no lead/prompt/original SELECT. Lifecycle checks tenant/request state and records durable progress |
| analytics_readonly | Tenant-approved aggregate views only; no source PII, sessions, raw prompts or provider payload |
| backup_operator | Separately secured complete-backup privileges with row-security completeness checks; never share app credentials. Restore only isolated and providers disabled |

Schema-qualified functions set fixed safe search_path, qualify objects, revoke PUBLIC EXECUTE, reject unsafe dynamic SQL and accept typed bounded inputs. Security-definer function owners are narrowly privileged no-login roles, **not superuser/table owner/BYPASSRLS**; explicit role-specific RLS policies apply. Public function owner can select only approved published snapshots and minimal eligibility columns, with separate policy role not inherited by api_runtime. A malicious caller cannot obtain private columns by passing SQL strings. Use security-barrier views where helpful; avoid default owner-bypass view surprises. No assumption that granting a view alone fixes RLS.

## 2. Exact RLS context and policy design

ENABLE and FORCE ROW LEVEL SECURITY on all tenant-private tables, including child/junction rows. Normal policy for T rows:
`studio_id = NULLIF(current_setting('app.studio_id', true), '')::uuid` in **USING and WITH CHECK**, scoped TO the named tenant role. Missing/empty context denies; malformed trusted context aborts transaction. Do not OR an unconditional public/admin policy into that runtime role.

At BEGIN, verified application code uses parameterized `SELECT set_config('app.studio_id', :verifiedStudioUuid, true)`, plus app.actor_id, app.purpose and optional app.case_id/app.grant_id. Last argument true is transaction-local. Membership is resolved before setting studio authority via designers' narrow membership lookup, using authenticated user ID and requested studio ID; status checked again for mutations. users and membership bootstrap are not solved by granting all studios. studio root RLS compares id rather than studio_id.

No session-wide SET, no connection state reused outside transaction. Commit/rollback clears local settings; pool return additionally rolls back unfinished work and resets session settings. Setup query, authorization and business query must share the same physical connection/transaction. Transaction pooling supported only if all statements stay inside one transaction. Async/reactive/thread switch must not drop context. Tests require alternating tenants on a one-connection pool, error/cancel/timeout paths, no context, forged context attempts and worker reuse.

RLS itself does **not authenticate arbitrary application custom settings**. A compromised trusted application connection could set another studio ID; prevent SQL injection, reject client-supplied authority and enforce owner checks. RLS protects omitted predicates, not a hostile DB administrator. FKs/unique errors can reveal existence; map private lookup failures to generic404/conflict without disclosing another tenant's ID. PostgreSQL notes owner/bypass behavior and policy combination explicitly: [row security reference](https://www.postgresql.org/docs/18/ddl-rowsecurity.html).

Special row-policy classes below are exhaustive across catalogue:

- **T**: row tenant equality with explicit module grants; studio root uses id. RLS doesn't replace MEDIA_MANAGER versus LEAD_MANAGER action policy.
- **U**: users/self-owned rows use verified actor_id; identity service functions may bind/login/revoke under purpose-specific role without opening user directory. user_roles reads own grants; grants/revoke only privileged function.
- **G**: global reference/config rows use ordinary grants (public approved taxonomy read, privileged audited writes); no meaningless tenant RLS.
- **S**: system-only records use explicit system DB role; user API has no general table read. Optional studio_id does not make unresolved webhooks public. System selects by known provider/job identity, resolves tenant then invokes tenant command.
- **C**: customer collections use owner_user_id, not referenced project's tenant; granted studio reads through collection_grants live scope/expiry contract.
- **R**: case/grant-specific functions; no general tenant member access to reports, evidence or staff access cases.

Public projection functions recheck ACTIVE studio, root publication pointer, nondeleted/nonrestricted root, allowed revision, active READY media recipe and current permission at serving time. portfolio-pinned archived project revision may show old public summary only while project root is live eligible. Search/sitemap filters, counts and facets use that same owner-provided eligibility view. Anonymous lead/report POST uses fixed command functions after abuse and target validation, never a public general INSERT policy.

Customer lead functions require customer_user_id=actor_id; return submitted contact/message/status projection only, never notes, attribution diagnostics, staff assignments or other enquiries. Anonymous leads do not automatically become owned by a matching email; verified explicit claim flow is later work.

Grant gateway resolves hash, scope, expiry, revoke, recipient binding if configured, resource state and exact item on **each new media/API request**. Review cookie carries grant ID/epoch under authenticated encryption or server verification; it is not independent authority. No presigned source URL substitute. Session and grant tokens are256-bit random; store SHA-256 hash, rotate/revoke, no query/log/referrer leaks. Client feedback role has no ability to enlarge review_items.

Workers claim durable work using narrow scheduler role, then tenant-scoped transaction rechecks purpose, aggregate state, membership for user-initiated edits, publication_epoch and fencing. Reconciliation/deletion may use explicit system authority after user revocation, but never continue unauthorized publication. Admin sets no magic is_admin=true context; case function checks persisted access_cases plus assurance and logs sensitive access. Migration and backup elevated identities stay outside runtime pools.

## 3. Complete catalogue ownership/access matrix

Abbreviations: **No**=no direct access. **Published DTO**=only live allowlisted public representation, never raw table. **Self DTO**=authenticated own fields only. **Role scoped**=active membership plus action policy, never blanket all-member access. **Case**=explicit staff purpose/step-up/audit, not unrestricted admin. **Owner worker**=module-specific system role with state checks. Every row below is an entity, including deferred ones; controls apply before its feature is enabled.

| Entity | Tenant owner | Public access | Customer access | Studio access | Admin access | System access | RLS | PII class |
|---|---|---|---|---|---|---|---|---|
| users.users | user_id / own id | No | Self DTO | Self DTO | Case | Owner worker | U | PII: display_name,email,phone,last_login_at |
| identity.external_identities | user_id / own id | No | Self DTO | Self DTO | Case | Owner worker | U | SENSITIVE: issuer,subject; PII: user_id |
| identity.roles | platform | No | No | Approved read | Case | Owner worker | G | INTERNAL |
| identity.user_roles | user_id / own id | No | Self DTO | Self DTO | Case | Owner worker | U | PII: user references |
| identity.sessions | user_id / own id | No | Self DTO | Self DTO | Case | Owner worker | U | HIGHLY_SENSITIVE: token_hash,csrf_hash; PII: device_label; no raw token |
| identity.login_transactions | platform | No | No | No | Case | Owner worker | S | HIGHLY_SENSITIVE: temporary encrypted verifier and hashes; purge promptly after expiry |
| identity.rate_limit_buckets | platform | No | No | No | Case | Owner worker | S | SENSITIVE: keyed identity/IP digest; short retention |
| designers.taxonomy_terms | platform | Published DTO | No | Approved read | Case | Owner worker | G | PUBLIC: approved labels; INTERNAL: editorial history |
| designers.countries | platform | Published DTO | No | Approved read | Case | Owner worker | G | PUBLIC |
| designers.regions | platform | Published DTO | No | Approved read | Case | Owner worker | G | PUBLIC |
| designers.cities | platform | Published DTO | No | Approved read | Case | Owner worker | G | PUBLIC |
| designers.designer_studios | id (studio) | Published DTO | No | Role scoped | Case | Owner worker | T | PII until confirmed public: business_name,description |
| designers.studio_members | studio_id | No | No | Role scoped | Case | Owner worker | T | PII: user_id |
| designers.studio_contacts | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PII: value; PUBLIC only explicit snapshot publication |
| designers.studio_services | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC when published |
| designers.studio_service_areas | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC when published |
| designers.studio_slug_claims | studio_id or reserved platform | Published DTO | No | Role scoped | Case | Owner worker | T + G reserved | PUBLIC routes; permanent tombstones, no reuse |
| portfolios.portfolios | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | INTERNAL |
| portfolios.portfolio_versions | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PII/public-intent: business_snapshot; no private contact copying |
| portfolios.portfolio_sections | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC-intent content; draft private |
| portfolios.portfolio_project_links | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC when live eligible |
| portfolios.portfolio_media | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC when live eligible; captions user input |
| portfolios.preview_grants | studio_id | No | Exact live grant | Role scoped | Case | Owner worker | T + grant contract | HIGHLY_SENSITIVE: hash; preview private |
| projects.projects | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | INTERNAL |
| projects.project_revisions | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC-intent facts; budget hidden excluded from public DTO |
| projects.project_terms | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC eligible facts |
| projects.project_media | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC only eligible PUBLIC usage; PRIVATE otherwise |
| projects.project_transformations | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC when published |
| projects.project_transformation_items | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC when eligible; approval is private separate feedback |
| projects.project_private_details | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE: encrypted address |
| projects.project_slug_claims | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC route tombstones |
| projects.publication_attestations | studio_id | No | No | Role scoped | Case | Owner worker | T | PII: actor; INTERNAL evidence |
| media.media_assets | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE: bytes,object identity,filename; object_key INTERNAL-restricted |
| media.upload_sessions | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE: provider upload reference; no presigned grants persisted |
| media.watermark_settings | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL |
| media.watermark_versions | studio_id | No | No | Role scoped | Case | Owner worker | T | PUBLIC-intent business_name; logo private master |
| media.media_recipes | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL; no URLs |
| media.media_variants | studio_id | Live derivative bytes only | No | Role scoped | Case | Owner worker | T | INTERNAL storage identity; PUBLIC bytes only with live usage/manifest |
| media.processing_jobs | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL |
| media.media_albums | studio_id | No | No | Role scoped | Case | Owner worker | T | PII possible: name |
| media.media_album_items | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL |
| media.media_tags | studio_id | No | No | Role scoped | Case | Owner worker | T | PII possible: user text |
| media.media_asset_tags | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL |
| billing.plans | platform | Published DTO | No | Approved read | Case | Owner worker | G | PUBLIC approved offering, INTERNAL draft |
| billing.entitlements | platform | Published DTO | No | Approved read | Case | Owner worker | G | PUBLIC capability descriptions |
| billing.plan_entitlements | platform | Published DTO | No | Approved read | Case | Owner worker | G | INTERNAL config |
| billing.subscriptions | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE: provider customer/subscription mapping |
| billing.usage_accounts | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL |
| billing.usage_reservations | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL |
| billing.usage_ledger | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL financial-adjacent accounting; retained separately |
| billing.payment_transactions | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE provider references; no PAN/card credentials |
| billing.webhook_events | platform | No | No | No | Case | Owner worker | S | SENSITIVE redacted binding data; never public or studio raw inbox |
| ai.ai_generations | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE: instruction,parameters,asset bindings; no provider secrets |
| ai.ai_generation_references | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE references and instructions |
| ai.ai_generation_outputs | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE until explicit project derivative publication |
| ai.ai_attempts | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE receipts/evidence; no raw vendor bodies |
| ai.provider_callbacks | platform | No | No | No | Case | Owner worker | S | SENSITIVE provider receipt; no output URLs retained in generic payload |
| ai.client_review_sessions | studio_id | No | Exact live grant | Role scoped | Case | Owner worker | T + grant contract | HIGHLY_SENSITIVE hash; PII recipient |
| ai.client_review_items | studio_id | No | Exact live grant | Role scoped | Case | Owner worker | T + grant contract | SENSITIVE private concept association |
| leads.leads | studio_id | No | Self DTO | Role scoped | Case | Owner worker | T | PII: contacts,name,locality; SENSITIVE: message; assignments/notes not customer-visible |
| leads.lead_status_history | studio_id | No | No | Role scoped | Case | Owner worker | T | PII actor; INTERNAL workflow |
| leads.lead_notes | studio_id | No | No | Role scoped | Case | Owner worker | T | SENSITIVE body |
| leads.lead_attribution | studio_id | No | No | Role scoped | Case | Owner worker | T | INTERNAL; treat accidental user strings as PII until sanitized |
| leads.lead_attachments | studio_id | No | Self DTO | Role scoped | Case | Owner worker | T | SENSITIVE private attachment |
| reviews.content_reports | studio_id | No | No | Case outcome / own submission only | Case | Owner worker | R | SENSITIVE reporter/description; no automatic disclosure to reported studio |
| reviews.report_actions | studio_id | No | No | Case outcome / own submission only | Case | Owner worker | R | SENSITIVE case material |
| seo.landing_pages | platform | Published DTO | No | Approved read | Case | Owner worker | G | PUBLIC eligible editorial content |
| discovery.project_search_documents | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC approved projection; stale results filtered by root eligibility |
| notifications.notifications | user_id / delivery recipient | No | Self DTO | Self DTO | Case | Owner worker | U | PII recipient; INTERNAL safe template data |
| notifications.delivery_intents | user_id / delivery recipient | No | No | Self DTO | Case | Owner worker | S | SENSITIVE encrypted destination/content; OTP only identity-owned hashed authority |
| notifications.preferences | user_id / delivery recipient | No | Self DTO | Self DTO | Case | Owner worker | U | PII user association |
| audit.audit_events | platform | No | No | No | Case | Owner worker | S | PII actor; INTERNAL redacted evidence; runtime INSERT only |
| audit.outbox_events | platform | No | No | No | Case | Owner worker | S | INTERNAL |
| audit.outbox_dispatches | platform | No | No | No | Case | Owner worker | S | INTERNAL |
| audit.consumer_receipts | platform | No | No | No | Case | Owner worker | S | INTERNAL |
| audit.idempotency_keys | platform | No | No | No | Case | Owner worker | S | SENSITIVE hashes; anonymous scope includes server-issued form intent/contact hash |
| audit.deletion_requests | platform | No | No | No | Case | Owner worker | S | SENSITIVE privacy request |
| audit.deletion_tasks | platform | No | No | No | Case | Owner worker | S | SENSITIVE restricted cleanup/export identity |
| audit.deletion_tombstones | platform | No | No | No | Case | Owner worker | S | INTERNAL identifiers; external durable export required |
| analytics.analytics_events | platform | No | No | No | Case | Owner worker | T / system ingest | PII pseudonymous session_digest; never fingerprint |
| analytics.daily_metrics | studio_id | No | No | Tenant aggregate DTO | Case | Owner worker | T / system ingest | INTERNAL aggregated; low count disclosure policy |
| admin.feature_flags | platform | No | No | No | Case | Owner worker | S | INTERNAL |
| admin.access_cases | staff actor + target studio | No | No | No | Case | Owner worker | R | SENSITIVE staff reason; no broad superuser bypass |
| ai.concept_feedback | studio_id | No | Exact live grant | Role scoped | Case | Owner worker | T + grant contract | SENSITIVE feedback/actor |
| reviews.reviews | studio_id | Approved DTO | Own author DTO | Published read | Case | Owner worker | R | PII author; public only approved projection |
| reviews.review_revisions | studio_id | Approved DTO | Own author DTO | Published read | Case | Owner worker | R | PII/public-intent text; private moderator data |
| reviews.review_dimensions | studio_id | Approved DTO | Own author DTO | Published read | Case | Owner worker | R | PUBLIC approved ratings |
| reviews.verification_requests | studio_id | Live badge DTO | No | Own submission/outcome | Case | Owner worker | R | SENSITIVE decision metadata |
| reviews.verification_evidence | studio_id | No | No | Case outcome / own submission only | Case | Owner worker | R | HIGHLY_SENSITIVE evidence |
| discovery.collections | owner_user_id | No | Owner | Explicit unexpired grant | Case | Owner worker | C | PII private interests |
| discovery.collection_items | owner_user_id | No | Owner | Explicit unexpired grant | Case | Owner worker | C | PII interests/notes |
| discovery.collection_grants | owner_user_id | No | Owner | Explicit unexpired grant | Case | Owner worker | C | PII sharing association |
| designers.custom_domains | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | INTERNAL challenge; PUBLIC active hostname |
| discovery.studio_search_documents | studio_id | Published DTO | No | Role scoped | Case | Owner worker | T | PUBLIC approved projection |

Media originals, AI references, masks and verification evidence are **never public**, regardless of an asset field or publication role. Public images are approved separate variants. Owner original downloads may have Phase01's<=60s expiry window; grant-media revocation is immediate on subsequent requests, public CDN takedown is tracked with<=5min target, and already downloaded bytes cannot be recalled.

## 4. Field classification and encryption

| Class | Examples | Treatment |
|---|---|---|
| PUBLIC | Confirmed published studio name/business contacts, public city/category, eligible project narrative, watermarked photo | Publish review and live eligibility; do not assume all users' contact details are business-public |
| INTERNAL | IDs, versions, job/recipe state, entitlement codes, source hashes, outbox IDs | Role-limited DTOs; object keys never returned by generic endpoints |
| PII | User name/email/phone, lead contact, customer/actor associations, pseudonymous session digest | Minimize, protect exports, separate own/studio DTOs, scrub logs |
| SENSITIVE | Exact home address, room-photo originals, prompts/references, client comments, lead notes, provider linkage, staff case reasons | Restricted purpose access; no public search/cache/queue payload |
| HIGHLY_SENSITIVE | Verification document, token/CSRF hashes, temporary PKCE verifier | Dedicated narrow contracts, short/configured retention, audited access; hashes are still protected |

Database/object encryption and TLS expected at infrastructure layer. Application envelope encryption with key version for exact address, verification metadata, external notification destination/content and report anonymous contact; keys reside in KMS/secret management, not same row. Source media remains private encrypted object storage rather than image BLOB in PG. Searchable business contacts need not be blindly application-encrypted; lead contacts use infrastructure encryption plus scoped column access, with keyed normalized digests only for abuse/dedup purpose if later justified. No deterministic ciphertext used as a general search index. Key rotation rewraps/re-encrypts by bounded jobs with recoverability tests.

Logs/security telemetry outside primary transactional schema contain redacted action/category/time, coarse risk signal and short-lived keyed network hashes when necessary. Auth failures/MFA/limiter signals have proposed90-day security policy pending review, not indefinite IP histories. Durable role changes/security actions remain audited. No raw token, provider secret, prompt, address, phone or presigned URL in logs/outbox.

## 5. Deletion, retention and restore

Soft deletion only on declared D bundles: project/portfolio roots, media assets/albums, generations, leads and later collections. Versions are preserved/archived until controlled purge; no soft-delete on every join, taxonomy, audit or ledger. Slugs stay reserved across trash/purge to prevent old URLs being assigned to strangers. Deleted actor users may remain minimal pseudonymous shells while audit/ledger FKs require them; RESTRICT prevents silent destruction.

Recoverable trash default proposal30days; restore revalidates current permissions, references, watermark and policy. Snapshot restore never overrides a takedown. Tenant deletion locks root, sets DELETING, revokes membership/sessions/grants/new jobs, then owner-specific tasks traverse dependencies and purge/anonymize. One person's deletion cannot delete a studio with another owner; resolve ownership transfer and retention first. Export is a separate authenticated workflow, encrypted private artifact, expiring download, no queue PII.

| Data class | Retention / deletion principle |
|---|---|
| Published portfolio/project versions | Keep current/history while account policy permits; unpublish removes access immediately; purge only after reference/grant checks |
| Originals and saved project assets | Keep until explicit delete/account policy, not unused-AI timer; all object versions/backups subject to reviewed lifecycle |
| Unused AI inputs/outputs/references | Proposed90days with notice; active saved project/review references exempt until purpose ends; reconcile billable attempts before destructive cleanup |
| Leads/notes/attachments | Configurable business-purpose period TBD; customer privacy requests assessed, remove PII independently from aggregate metrics |
| Reviews | Published content and moderation history under purpose/appeal policy TBD; do not keep anonymous contact forever |
| Verification evidence | Shortest justified evidence retention, explicit retain_until set by reviewed type policy; not permanent badge image storage |
| Billing/usage/provider receipts | Required contractual/legal periods TBD before paid launch; restrict/pseudonymize rather than blanket cascade |
| Audit/security | Proposed90days security telemetry; sensitive audit retention separately TBD; tamper-evident export and narrow reads |
| Analytics | Proposed raw30days and aggregates13months; daily pruning; session digests shorter where policy requires |
| Outbox/dedup/idempotency | Completed payload candidate30days after all destinations/consumers safe; API replay>=24h; provider/job business dedup survives relevant financial/job retention; see migration plan |
| Tokens/login/session | Expired one-time login material promptly removed; sessions bounded and revoked, not indefinite browsing history |
| Backups/tombstones | Proposed35days backup; tombstones retain beyond all restorable copies plus restore safety margin; durable independent export replays post-backup deletions |

All durations above are product proposals/configuration, **not legal retention conclusions**. Launch privacy/provider review establishes policy version and justified deadlines. A legal hold blocks physical purge but never keeps public access active. Required evidence is segregated and access-restricted.

DB/object consistency: reserve known object intent before upload, pin accepted version/checksum, reconcile DB pending/object inventory. Object exists without committed asset → quarantine hold then age-gated orphan cleanup only after checking active upload/job/recipe references and backup restore state. Asset says READY but object missing → fail delivery closed, mark reconciliation failure, regenerate from intact private master if permitted, otherwise show unavailable; never substitute another source. Jobs use current deletion epoch/fencing so old work cannot resurrect purged content.

Cleanup steps and receipts persist in deletion_tasks; only mark complete after all required object versions, derivatives, CDN, provider and projections are handled or explicitly recorded as policy exception. Tombstones contain resource IDs rather than erased PII; replicated independent deletion journal is necessary because restoring an old DB alone loses newer tombstones. Restore in isolation, apply journal first, keep providers disabled, reconcile pending bills/AI against external authoritative receipts, rebuild public projections and validate all tenants before exposing traffic.

## 6. Conceptual adversarial review and implementation gates

These are logical checks against the proposed schema, **not executed SQL/security tests**.

| Attack / race | Design result | Required later test |
|---|---|---|
| A references B project/revision | Composite tenant+project+revision FK denies; root version pointer cannot cross parent | Insert mismatched graph under real runtime role, 03/18 |
| A attaches B media or logo | Every usage/recipe/logo/reference FK includes studio; membership/action check plus RLS | Nested IDs and bulk mixed tenants, 03/19 |
| A reads B generation/reference | T RLS/qualified repositories; exact private grant item; no public reference path | One-connection pool, missing context, wrong job/output, 21–24 |
| A reads/assigns B lead | T FK to same studio project/member; customer projection separate | Lead lists/counts/notes/attachment/assignee substitution, 26 |
| READY original served publicly | Original zones never CDN-readable; live usage+approved manifest+variant required | S3/CDN/optimizer guessed source and swapped variant,19 |
| Private reference relabeled COVER | private_only immutable constraint for reference/evidence/mask plus provenance; publish command rejects | Reference asset with public role,19/22 |
| Review token DB/log leak or revoked cookie reuse | Random hashed tokens, scrub paths; every request live session/grant/epoch/items | Revoke/expire then same cookie/media request,24 |
| Staff claims another role/tenant | Separate narrow case functions validate action/target/MFA/reason/expiry; runtime no bypass | Expired/mismatched case; direct table read denied;03/29 |
| Replayed/wrong-account payment webhook | Verified provider/account/env + unique event, locked state/ledger transition | Duplicate/concurrent/out-of-order/refund collisions,28 |
| Two AI requests consume last credit | Locked usage account, conditional reserve, unique operation/ledger key | Parallel requests admit at most available units;21/28 |
| Publish vs purge / two publishers | Same studio/reference lock, optimistic root version, deferred pointer constraints | Concurrent publish/delete/reprocess rollback;10/18/19 |
| Callback vs poll or worker crash after submit | Durable attempt/submission key, lease fencing, held credit and reconciliation | Uncertain acceptance with no request ID; no duplicate submit/settle;21 |
| Malicious report target / collection owner | Typed XOR report FKs; collection customer owner distinct from project tenant | Wrong target tenant, studio trying customer's unrelated collections;27 |
| Snapshot/outbox restore revives deleted data | Live eligibility overrides history; independent deletion journal + provider reconciliation | PITR preceding deletion, pending provider transaction;29 |

Design review found no remaining unsupported prevention requirement. Controls remain unimplemented, and risks stay OPEN until the named tests pass.

