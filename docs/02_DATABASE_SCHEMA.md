# Phase 02 — Database Schema Blueprint

2026-09-17 • Design only; no executable migrations or schema deployed. Read with [domain model](02_DOMAIN_MODEL.md), [ownership/security](02_DATA_OWNERSHIP_SECURITY.md), [lifecycles](02_ENTITY_LIFECYCLES.md), [taxonomy](02_TAXONOMY_STRATEGY.md), [indexes](02_INDEX_QUERY_STRATEGY.md) and [migrations](02_MIGRATION_STRATEGY.md).

## Inventory and scope

**94 proposed tables: 84 V1, 10 deferred V1.5 extension tables.** V1 means needed across the whole approved V1 roadmap, not 84 tables to install in Phase03. Implement each owner's migrations with its feature. Ten later tables are explicit design contracts only. Other future capabilities have extension paths in the domain model rather than speculative tables. Views, indexes, application registries and Flyway's own history table are not counted. This Markdown dictionary is the reviewed schema blueprint; optional SQL is deliberately omitted because no PostgreSQL runtime or scaffold exists.

## Dictionary notation and shared columns

Each catalogue entry is normative in combination with these conventions; no implicit audit columns beyond the declared bundles.

- `name type` means NOT NULL, **no default** unless `=value` follows. `?` means nullable, default NULL. Semicolon separates columns. PostgreSQL types are literal. `now()` means transaction timestamp; all instants use timestamptz and UTC on the wire.
- **I** expands to `id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY; created_at timestamptz NOT NULL DEFAULT now()`.
- **T** expands to `studio_id uuid NOT NULL REFERENCES designers.designer_studios(id) ON DELETE RESTRICT; UNIQUE(studio_id,id)`. It does not confer membership or public access.
- **M** expands to `updated_at timestamptz NOT NULL DEFAULT now(); version bigint NOT NULL DEFAULT 0 CHECK(version>=0)`. Owner use cases atomically increment version, update timestamp and reject stale If-Match (412); SQL defaults alone do not update timestamps.
- **D** expands to `deleted_at timestamptz NULL; purge_after timestamptz NULL; CHECK((deleted_at IS NULL AND purge_after IS NULL) OR (deleted_at IS NOT NULL AND purge_after>=deleted_at))`. Trash is an overlay, not permission to purge referenced history.
- Every FK targets the named table's `id` unless explicit columns appear. **T:foo** means composite `(studio_id,foo) REFERENCES target(studio_id,id)`; never two unrelated single-column FKs. Optional composite FK uses MATCH SIMPLE plus explicit paired-null checks where necessary. Parent roots' `(studio_id,id)` unique keys support these relationships.
- All FKs default **ON DELETE RESTRICT, ON UPDATE RESTRICT**. Circular root/version pointers explicitly use **NO ACTION DEFERRABLE INITIALLY DEFERRED** (RESTRICT cannot supply equivalent end-of-transaction deferral). They include tenant and parent ID, so a pointer cannot target another project's/portfolio's revision.
- All UNIQUE and PK constraints create their supporting B-tree; listed indexes are additional unless stated covered. Partial UNIQUE is an index and not an FK target. Plain UUID PKs remain globally unique. Cross-module FK declarations do not authorize runtime cross-module SQL or introduce Java dependency edges.
- Non-I rate limiter has its explicitly declared composite PK and no synthetic resource ID. No public integer identifiers. Fixed reference seed UUIDs are deterministic constants; user-generated entities default UUIDv7. Tokens/keys use independent cryptographic randomness, not UUIDv7.
- Classification defaults INTERNAL. Listed PII/SENSITIVE/HIGHLY_SENSITIVE fields override that; PUBLIC means publication-eligible, never automatically public. User-authored free text must be treated as potentially sensitive even when intended for public display.
- **BUDGET**: both bounds NULL or both nonnegative with min<=max; currency NULL iff bounds NULL; currency uppercase three-letter configured currency, never float; exact amounts excluded if HIDDEN. Optional budget can remain unknown. Java integer overflow checked, large integers serialized as decimal strings.
- **DATE_PRECISION**: completion_year NULL or1900..9999; completion_date requires matching completion_year; year-only accepted without inventing January1. No speculative date requirement for drafts.
- All statuses/codes enumerated here or in [lifecycles](02_ENTITY_LIFECYCLES.md) get text CHECK constraints (not PostgreSQL enums). NULL never substitutes for a defined state.
- JSONB CHECK only assures object/array and size where stated. Full application schema validation is required on every write and publication; no PostgreSQL built-in JSON Schema validator is presumed. Cross-row invariants require a named transaction/constraint trigger, never an invalid cross-row CHECK.
- Runtime role may not modify immutable request/snapshot/ledger payloads after sealing. Narrow stored write contracts or column privileges plus guard triggers enforce this; lifecycle metadata can change through owner transitions. FK checks alone cannot enforce publication/privacy/status.

## Table catalogue

### 01. users.users

**Release:** V1. **Purpose:** Stable person identity; account lifecycle independent of studios.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. display_name text; email text?; phone text?; status text='PENDING'; email_verified_at timestamptz?; phone_verified_at timestamptz?; last_login_at timestamptz?; locale text='en'; timezone text='Asia/Kolkata' |
| PK | id (UUIDv7, bundle I) |
| FK | — |
| Unique | — |
| Check / transition invariants | status in user lifecycle; bounded display/contact; verified timestamp requires corresponding contact |
| Indexes | (status,id) for lifecycle jobs |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII: display_name,email,phone,last_login_at |

### 02. identity.external_identities

**Release:** V1. **Purpose:** OIDC account binding, never email-based auto-merge.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. user_id uuid; issuer text; subject text; linked_at timestamptz=now() |
| PK | id (UUIDv7, bundle I) |
| FK | user_id → users.users |
| Unique | (issuer,subject) |
| Check / transition invariants | issuer allowlisted; subject nonempty |
| Indexes | (user_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE: issuer,subject; PII: user_id |

### 03. identity.roles

**Release:** V1. **Purpose:** Platform role registry.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. code text; name text |
| PK | id (UUIDv7, bundle I) |
| FK | — |
| Unique | (code) |
| Check / transition invariants | code in CUSTOMER,DESIGNER,DESIGNER_TEAM,MODERATOR,ADMIN,SUPER_ADMIN |
| Indexes | unique code sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 04. identity.user_roles

**Release:** V1. **Purpose:** Multiple current platform responsibilities; audit preserves grant history.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. user_id uuid; role_id uuid; granted_by uuid?; granted_at timestamptz=now(); revoked_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | user_id,granted_by → users.users; role_id → identity.roles |
| Unique | (user_id,role_id) |
| Check / transition invariants | revoked_at null or >=granted_at |
| Indexes | (role_id,user_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII: user references |

### 05. identity.sessions

**Release:** V1. **Purpose:** Authoritative revocable browser sessions.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. user_id uuid; token_hash bytea; csrf_hash bytea; auth_time timestamptz; assurance text; step_up_at timestamptz?; last_seen_at timestamptz; idle_expires_at timestamptz; absolute_expires_at timestamptz; revoked_at timestamptz?; device_label text? |
| PK | id (UUIDv7, bundle I) |
| FK | user_id → users.users |
| Unique | (token_hash) |
| Check / transition invariants | hash lengths32; expires>=auth_time; idle<=absolute; strict assurance enum |
| Indexes | (user_id,revoked_at); (absolute_expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | HIGHLY_SENSITIVE: token_hash,csrf_hash; PII: device_label; no raw token |

### 06. identity.login_transactions

**Release:** V1. **Purpose:** One-time OIDC state/nonce/PKCE binding.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. state_hash bytea; nonce_hash bytea; verifier_ciphertext bytea; key_version text; browser_binding_hash bytea; issuer text; return_path text; expires_at timestamptz; consumed_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | — |
| Unique | (state_hash) |
| Check / transition invariants | hash lengths32; expires>created_at; local allowlisted return_path |
| Indexes | (expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | HIGHLY_SENSITIVE: temporary encrypted verifier and hashes; purge promptly after expiry |

### 07. identity.rate_limit_buckets

**Release:** V1. **Purpose:** Distributed low-volume sensitive action limits.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: none. id bytea; scope text; window_start timestamptz; window_end timestamptz; request_count integer=0 |
| PK | (id,scope,window_start); id is bytea hash, not a public resource ID |
| FK | — |
| Unique | — |
| Check / transition invariants | PK(id,scope,window_start); count>=0; end>start; id is keyed identifier hash |
| Indexes | (window_end) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE: keyed identity/IP digest; short retention |

### 08. designers.taxonomy_terms

**Release:** V1. **Purpose:** Controlled business/service/category/room/work/style/property/material/source vocabulary.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. kind text; code text; label text; slug text; active boolean=true; sort_order integer=0 |
| PK | id (UUIDv7, bundle I) |
| FK | — |
| Unique | (kind,code); (kind,slug); (kind,id) |
| Check / transition invariants | kind allowlist; sort_order>=0; bounded normalized code/slug |
| Indexes | (kind,active,sort_order,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC: approved labels; INTERNAL: editorial history |

### 09. designers.countries

**Release:** V1. **Purpose:** Country reference data.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. code text; name text; active boolean=true |
| PK | id (UUIDv7, bundle I) |
| FK | — |
| Unique | (code) |
| Check / transition invariants | code uppercase ISO alpha2 |
| Indexes | unique code sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC |

### 10. designers.regions

**Release:** V1. **Purpose:** Country-qualified state/region.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. country_id uuid; code text; name text; slug text; active boolean=true |
| PK | id (UUIDv7, bundle I) |
| FK | country_id → designers.countries |
| Unique | (country_id,code); (country_id,slug) |
| Check / transition invariants | nonempty name/code/slug |
| Indexes | unique country prefix sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC |

### 11. designers.cities

**Release:** V1. **Purpose:** Region-qualified city; no customer address.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. region_id uuid; name text; slug text; active boolean=true |
| PK | id (UUIDv7, bundle I) |
| FK | region_id → designers.regions |
| Unique | (region_id,slug) |
| Check / transition invariants | normalized slug |
| Indexes | unique region prefix sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC |

### 12. designers.designer_studios

**Release:** V1. **Purpose:** Tenant root, operational business profile and publication/reference lock.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. business_name text; description text?; business_type_id uuid; business_type_kind text='BUSINESS'; experience_since_year smallint?; status text='PENDING'; timezone text='Asia/Kolkata'; default_locale text='en'; publication_epoch bigint=0; verification_epoch bigint=0 |
| PK | id (UUIDv7, bundle I) |
| FK | (business_type_kind,business_type_id) → taxonomy_terms(kind,id) |
| Unique | — |
| Check / transition invariants | kind='BUSINESS'; year 1900..9999 if set; epochs>=0; studio lifecycle |
| Indexes | (status,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII until confirmed public: business_name,description |

### 13. designers.studio_members

**Release:** V1. **Purpose:** Tenant membership; multiple OWNERs allowed, at least one active owner.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. user_id uuid; membership_role text; status text='ACTIVE'; invited_at timestamptz?; joined_at timestamptz?; revoked_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; user_id → users.users |
| Unique | (studio_id,id); (studio_id,user_id) |
| Check / transition invariants | role allowlist OWNER/ADMIN/PORTFOLIO_MANAGER/PROJECT_MANAGER/MEDIA_MANAGER/LEAD_MANAGER/VIEWER; active-owner invariant deferred trigger under studio lock |
| Indexes | (user_id,status,studio_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII: user_id |

### 14. designers.studio_contacts

**Release:** V1. **Purpose:** Explicit business contact intent; drafts do not automatically change public snapshots.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. kind text; value text; public_consent boolean=false; sort_order integer=0 |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; — |
| Unique | (studio_id,id); (studio_id,kind,value) |
| Check / transition invariants | kind EMAIL/PHONE/WHATSAPP/WEBSITE/INSTAGRAM/FACEBOOK/OTHER_SOCIAL; sort>=0; URL schemes allowlisted |
| Indexes | (studio_id,sort_order,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII: value; PUBLIC only explicit snapshot publication |

### 15. designers.studio_services

**Release:** V1. **Purpose:** Studio offerings from service taxonomy.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. service_id uuid; term_kind text='SERVICE' |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (term_kind,service_id) → taxonomy_terms(kind,id) |
| Unique | (studio_id,id); (studio_id,service_id) |
| Check / transition invariants | term_kind='SERVICE' |
| Indexes | (service_id,studio_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC when published |

### 16. designers.studio_service_areas

**Release:** V1. **Purpose:** Multiple public cities and optional named localities.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. city_id uuid; locality text? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; city_id → designers.cities |
| Unique | (studio_id,id); NULLS NOT DISTINCT(studio_id,city_id,locality) |
| Check / transition invariants | locality max160, public neighborhood only |
| Indexes | (city_id,studio_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC when published |

### 17. designers.studio_slug_claims

**Release:** V1. **Purpose:** Single registry for reserved roots, current slugs and permanent aliases.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. studio_id uuid?; slug text; state text; normalization_version smallint=1 |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designer_studios |
| Unique | (slug); partial(studio_id) WHERE state='CURRENT' |
| Check / transition invariants | state RESERVED/CURRENT/ALIAS/RETIRED; RESERVED iff studio_id null; Phase01 normalized slug rules with reserved short locale exceptions |
| Indexes | (studio_id,state) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC routes; permanent tombstones, no reuse |

### 18. portfolios.portfolios

**Release:** V1. **Purpose:** One portfolio root per studio; draft and live version pointers.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M,D. draft_version_id uuid?; published_version_id uuid?; state text='UNPUBLISHED' |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,id,draft_version_id) and (studio_id,id,published_version_id) → portfolio_versions(studio_id,portfolio_id,id), deferred NO ACTION |
| Unique | (studio_id,id); (studio_id) |
| Check / transition invariants | state UNPUBLISHED/PUBLISHED/ARCHIVED; published iff live pointer nonnull; pointer status/one draft deferred trigger |
| Indexes | unique studio sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 19. portfolios.portfolio_versions

**Release:** V1. **Purpose:** One mutable draft; immutable published/archived snapshots, same schema across six themes.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. portfolio_id uuid; version_number integer; status text='DRAFT'; content_schema_version integer; theme_id text; renderer_version text; theme_config jsonb; business_snapshot jsonb; logo_asset_id uuid?; favicon_asset_id uuid?; meta_title text?; meta_description text?; robots_preference text='AUTO'; published_at timestamptz?; created_by uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:portfolio_id → portfolios; T:logo_asset_id,favicon_asset_id → media.media_assets; created_by → users.users |
| Unique | (studio_id,id); (studio_id,portfolio_id,id); (portfolio_id,version_number); partial(portfolio_id) WHERE status='DRAFT'; partial(portfolio_id) WHERE status='PUBLISHED' |
| Check / transition invariants | six theme IDs only; version>0; schema>0; robots AUTO/NOINDEX; strict JSON contracts; published payload immutable |
| Indexes | (studio_id,portfolio_id,status) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII/public-intent: business_snapshot; no private contact copying |

### 20. portfolios.portfolio_sections

**Release:** V1. **Purpose:** Ordered typed modular content inside one version.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. portfolio_version_id uuid; section_key text; type text; position integer; visible boolean=true; schema_version integer; content jsonb; settings jsonb |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:portfolio_version_id → portfolio_versions |
| Unique | (studio_id,id); (studio_id,portfolio_version_id,id); (portfolio_version_id,section_key); (portfolio_version_id,position) DEFERRABLE |
| Check / transition invariants | position>=0; registered section type/schema; JSON limits and no asset/project IDs inside JSON |
| Indexes | (portfolio_version_id,position) covered by unique |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC-intent content; draft private |

### 21. portfolios.portfolio_project_links

**Release:** V1. **Purpose:** Pin exact published project revisions within sections.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. portfolio_version_id uuid; section_id uuid; project_id uuid; project_revision_id uuid; position integer |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,portfolio_version_id,section_id) → portfolio_sections(studio_id,portfolio_version_id,id); (studio_id,project_id,project_revision_id) → projects.project_revisions(studio_id,project_id,id) |
| Unique | (studio_id,id); (section_id,position); (section_id,project_revision_id) |
| Check / transition invariants | position>=0; publication checks project revision status/live root |
| Indexes | FK project revision reverse lookup |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC when live eligible |

### 22. portfolios.portfolio_media

**Release:** V1. **Purpose:** Typed first-class section media usage; no image URLs in JSON.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. portfolio_version_id uuid; section_id uuid; asset_id uuid; role text; position integer; caption text?; alt_text text?; focal_x numeric(5,4)?; focal_y numeric(5,4)? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,portfolio_version_id,section_id) → portfolio_sections(studio_id,portfolio_version_id,id); T:asset_id → media.media_assets |
| Unique | (studio_id,id); (section_id,position) |
| Check / transition invariants | role HERO/PHOTO/VIDEO/TEAM_PORTRAIT/OG; position>=0; focal both null or both0..1 |
| Indexes | (studio_id,asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC when live eligible; captions user input |

### 23. portfolios.preview_grants

**Release:** V1. **Purpose:** Revocable preview access to a frozen preview revision, not live editable content.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. portfolio_version_id uuid; token_hash bytea; expires_at timestamptz; revoked_at timestamptz?; created_by uuid; bound_version bigint |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:portfolio_version_id → portfolio_versions; created_by → users.users |
| Unique | (studio_id,id); (token_hash) |
| Check / transition invariants | hash32; expires>created; read-only; invalidate on draft version change via bound_version |
| Indexes | (expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | HIGHLY_SENSITIVE: hash; preview private |

### 24. projects.projects

**Release:** V1. **Purpose:** Project aggregate and current draft/live pointers; publication independent of verification.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M,D. draft_revision_id uuid?; published_revision_id uuid?; state text='DRAFT'; verification_epoch bigint=0 |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,id,draft_revision_id) and (studio_id,id,published_revision_id) → project_revisions(studio_id,project_id,id), deferred NO ACTION |
| Unique | (studio_id,id); — |
| Check / transition invariants | state DRAFT/READY/PUBLISHED/ARCHIVED; current PUBLISHED iff pointer present; exact root/status checks deferred; draft edits of a published root leave root PUBLISHED |
| Indexes | (studio_id,state,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 25. projects.project_revisions

**Release:** V1. **Purpose:** Normalized draft facts copied to immutable published revisions.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. project_id uuid; revision_number integer; status text='DRAFT'; name text; description text?; scope text?; property_type_id uuid?; property_kind text='PROPERTY'; city_id uuid?; public_locality text?; budget_min_minor bigint?; budget_max_minor bigint?; currency text?; budget_visibility text='HIDDEN'; completion_year smallint?; completion_date date?; duration_days integer?; meta_title text?; meta_description text?; robots_preference text='AUTO'; created_by uuid?; published_at timestamptz?; content_locale text='en' |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_id → projects; (property_kind,property_type_id) → designers.taxonomy_terms(kind,id); city_id → designers.cities; created_by → users.users |
| Unique | (studio_id,id); (studio_id,project_id,id); (project_id,revision_number); partial(project_id) WHERE status='DRAFT'; partial(project_id) WHERE status='PUBLISHED' |
| Check / transition invariants | revision>0; typed taxonomy; BUDGET; DATE_PRECISION; duration>=0; robots AUTO/NOINDEX; payload immutable after publication |
| Indexes | (studio_id,project_id,status) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC-intent facts; budget hidden excluded from public DTO |

### 26. projects.project_terms

**Release:** V1. **Purpose:** Separate multiple categories, rooms, work types, styles, services, materials and tags by kind.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. project_revision_id uuid; term_id uuid; term_kind text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_revision_id → project_revisions; (term_kind,term_id) → designers.taxonomy_terms(kind,id) |
| Unique | (studio_id,id); (project_revision_id,term_kind,term_id) |
| Check / transition invariants | term_kind CATEGORY/ROOM/WORK/STYLE/SERVICE/MATERIAL/TAG |
| Indexes | (term_kind,term_id,project_revision_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC eligible facts |

### 27. projects.project_media

**Release:** V1. **Purpose:** Revision-owned first-class media usage; reuse master without duplicating bytes.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. project_revision_id uuid; asset_id uuid; role text; position integer; visibility text='PRIVATE'; caption text?; alt_text text?; focal_x numeric(5,4)?; focal_y numeric(5,4)?; crop jsonb? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_revision_id → project_revisions; T:asset_id → media.media_assets |
| Unique | (studio_id,id); (studio_id,project_revision_id,id); (project_revision_id,position); partial(project_revision_id) WHERE role='COVER'; (project_revision_id,asset_id,role) |
| Check / transition invariants | role GALLERY/COVER/BEFORE/AFTER/AI_CONCEPT/VIDEO/OG; visibility PRIVATE/CLIENT_SHARED/PUBLIC; position>=0; focal bounds; validated crop rectangle only |
| Indexes | (studio_id,asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC only eligible PUBLIC usage; PRIVATE otherwise |

### 28. projects.project_transformations

**Release:** V1. **Purpose:** A coherent before/concept/actual story on a project revision.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. project_revision_id uuid; title text; position integer |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_revision_id → project_revisions |
| Unique | (studio_id,id); (studio_id,project_revision_id,id); (project_revision_id,position) |
| Check / transition invariants | position>=0 |
| Indexes | unique revision prefix sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC when published |

### 29. projects.project_transformation_items

**Release:** V1. **Purpose:** Multiple concept iterations and final actual photos linked by explicit story.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. project_revision_id uuid; transformation_id uuid; project_media_id uuid; stage text; position integer |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,project_revision_id,transformation_id) → project_transformations(studio_id,project_revision_id,id); (studio_id,project_revision_id,project_media_id) → project_media(studio_id,project_revision_id,id) |
| Unique | (studio_id,id); (transformation_id,position); (transformation_id,project_media_id) |
| Check / transition invariants | stage BEFORE/AI_CONCEPT/AFTER; provenance validated; position>=0 |
| Indexes | (project_media_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC when eligible; approval is private separate feedback |

### 30. projects.project_private_details

**Release:** V1. **Purpose:** Exact site/client address separated from all public revisions.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. project_id uuid; address_ciphertext bytea; key_version text; retention_until timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_id → projects |
| Unique | (studio_id,id); (project_id) |
| Check / transition invariants | ciphertext nonempty; no public/locality automatic copy |
| Indexes | unique project sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE: encrypted address |

### 31. projects.project_slug_claims

**Release:** V1. **Purpose:** Tenant-scoped permanent current/alias/retired slug claims.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. project_id uuid; slug text; state text; normalization_version smallint=1 |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_id → projects |
| Unique | (studio_id,id); (studio_id,slug); partial(project_id) WHERE state='CURRENT' |
| Check / transition invariants | state CURRENT/ALIAS/RETIRED; normalized3..120 ASCII slug |
| Indexes | (project_id,state) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC route tombstones |

### 32. projects.publication_attestations

**Release:** V1. **Purpose:** Evidence of rights confirmation for exact published revision.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. project_revision_id uuid; actor_user_id uuid; statement_version text; attested_at timestamptz=now() |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_revision_id → project_revisions; actor_user_id → users.users |
| Unique | (studio_id,id); (project_revision_id,actor_user_id,statement_version) |
| Check / transition invariants | append-only; nonempty policy version |
| Indexes | (studio_id,attested_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII: actor; INTERNAL evidence |

### 33. media.media_assets

**Release:** V1. **Purpose:** Private immutable original identity and provenance; public availability is separate.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M,D. uploaded_by uuid?; content_kind text; private_only boolean=true; state text='UPLOADING'; storage_provider text='S3'; storage_zone text='QUARANTINE'; object_key text; object_version text?; original_filename text?; detected_mime text?; byte_size bigint?; width integer?; height integer?; orientation smallint?; duration_ms bigint?; checksum bytea?; active_public_recipe_id uuid?; failure_code text?; provenance text; source_asset_id uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; uploaded_by → users.users; T:source_asset_id → media_assets; (studio_id,id,active_public_recipe_id) → media_recipes(studio_id,asset_id,id), deferred NO ACTION |
| Unique | (studio_id,id); (storage_provider,storage_zone,object_key,object_version) NULLS NOT DISTINCT |
| Check / transition invariants | byte_size>=0; dimensions>0; orientation1..8; content_kind PHOTO/BRAND_IMAGE/VIDEO/DRAWING/MASK/REFERENCE/EVIDENCE; masters PRIVATE zones only; provenance REAL/AI_GENERATED/REFERENCE/MASK/BRAND/VIDEO/DRAWING/EVIDENCE; private_only mandatory references/masks/evidence; READY requires pinned checksum/object version/detected type/size; private_only initially true, may be cleared only by explicit photographic/brand publication eligibility command; references/masks/evidence never cleared |
| Indexes | (studio_id,state,created_at,id); partial(purge_after) WHERE deleted_at IS NOT NULL |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE: bytes,object identity,filename; object_key INTERNAL-restricted |

### 34. media.upload_sessions

**Release:** V1. **Purpose:** Bounded upload/resume and exact quarantine version completion.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. asset_id uuid; quota_reservation_id uuid; upload_kind text; provider_upload_id text?; expected_bytes bigint; accepted_object_version text?; accepted_checksum bytea?; expires_at timestamptz; state text='OPEN'; completed_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:asset_id → media_assets; T:quota_reservation_id → billing.usage_reservations |
| Unique | (studio_id,id); (asset_id) |
| Check / transition invariants | bytes>0 within server policy; kind SINGLE/MULTIPART; state OPEN/COMPLETED/CANCELLED/EXPIRED; completed requires checksum/version |
| Indexes | (state,expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE: provider upload reference; no presigned grants persisted |

### 35. media.watermark_settings

**Release:** V1. **Purpose:** Studio pointer to desired immutable watermark configuration.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. current_version_id uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,id,current_version_id) → watermark_versions(studio_id,settings_id,id), deferred NO ACTION |
| Unique | (studio_id,id); (studio_id) |
| Check / transition invariants | pointer must belong to this studio/settings |
| Indexes | unique studio sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 36. media.watermark_versions

**Release:** V1. **Purpose:** Immutable logo/name/position/opacity/scale settings.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. settings_id uuid; version_number integer; logo_asset_id uuid?; business_name text; mode text='NAME'; position text='BOTTOM_RIGHT'; opacity numeric(4,3)=0.700; scale numeric(4,3)=0.150; style_version text; created_by uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:settings_id → watermark_settings; T:logo_asset_id → media_assets; created_by → users.users |
| Unique | (studio_id,id); (studio_id,settings_id,id); (settings_id,version_number) |
| Check / transition invariants | version>0; mode LOGO/NAME/BOTH; five positions; opacity0.350..1.000; scale0.050..0.300; name nonempty; logo mode requires logo at request but name fallback mandatory; no enabled=false bypass |
| Indexes | (studio_id,logo_asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC-intent business_name; logo private master |

### 37. media.media_recipes

**Release:** V1. **Purpose:** Finite immutable derivative manifest per asset/config/crop/processor generation.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. asset_id uuid; recipe_hash bytea; processing_version text; watermark_version_id uuid?; ai_label_version text?; crop jsonb?; delivery_class text='PRIVATE'; state text='PENDING'; required_variants jsonb; ready_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:asset_id → media_assets; T:watermark_version_id → watermark_versions |
| Unique | (studio_id,id); (studio_id,asset_id,id); (asset_id,recipe_hash) |
| Check / transition invariants | delivery PRIVATE/PUBLIC; state PENDING/PROCESSING/READY/FAILED/RETIRED; public photographs require watermark; AI public requires label; strict bounded variant contract/crop; readiness gate validates all rows |
| Indexes | (studio_id,state,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL; no URLs |

### 38. media.media_variants

**Release:** V1. **Purpose:** Actual meaningful responsive/OG/video derivative objects, never per CDN request.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. asset_id uuid; recipe_id uuid; variant_type text; format text; width integer; height integer; byte_size bigint; checksum bytea; storage_provider text='S3'; storage_zone text; object_key text; object_version text; state text='PROCESSING'; watermark_verified boolean=false; ai_label_verified boolean=false |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,asset_id,recipe_id) → media_recipes(studio_id,asset_id,id) |
| Unique | (studio_id,id); (recipe_id,variant_type,format,width,height); (storage_provider,storage_zone,object_key,object_version); (studio_id,asset_id,id) |
| Check / transition invariants | dimensions>0 bytes>=0; checksum32; state PROCESSING/READY/FAILED/RETIRED; zone PRIVATE_PROCESSED/PUBLISHED_DERIVATIVE; recipe-ready checks stamps |
| Indexes | (studio_id,asset_id,recipe_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL storage identity; PUBLIC bytes only with live usage/manifest |

### 39. media.processing_jobs

**Release:** V1. **Purpose:** Retryable media work with fenced DB leases.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. asset_id uuid; recipe_id uuid?; operation text; dedup_key text; state text='QUEUED'; attempts integer=0; lease_epoch bigint=0; lease_owner text?; lease_until timestamptz?; next_attempt_at timestamptz=now(); failure_code text? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:asset_id → media_assets; (studio_id,asset_id,recipe_id) → media_recipes(studio_id,asset_id,id) |
| Unique | (studio_id,id); (studio_id,dedup_key) |
| Check / transition invariants | state QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED; attempts/epoch>=0; operation registered |
| Indexes | partial(next_attempt_at,id) WHERE state='QUEUED'; (lease_until) WHERE state='RUNNING' |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 40. media.media_albums

**Release:** V1. **Purpose:** Recoverable user media grouping.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M,D. name text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; — |
| Unique | (studio_id,id); — |
| Check / transition invariants | bounded nonempty name |
| Indexes | (studio_id,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII possible: name |

### 41. media.media_album_items

**Release:** V1. **Purpose:** Asset reuse in multiple albums.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. album_id uuid; asset_id uuid; position integer |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:album_id → media_albums; T:asset_id → media_assets |
| Unique | (studio_id,id); (album_id,asset_id); (album_id,position) |
| Check / transition invariants | position>=0 |
| Indexes | (studio_id,asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 42. media.media_tags

**Release:** V1. **Purpose:** Tenant-authored library tags.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. name text; normalized_name text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; — |
| Unique | (studio_id,id); (studio_id,normalized_name) |
| Check / transition invariants | bounded normalized name |
| Indexes | unique tenant prefix sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII possible: user text |

### 43. media.media_asset_tags

**Release:** V1. **Purpose:** Normalized media tagging.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. asset_id uuid; tag_id uuid |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:asset_id → media_assets; T:tag_id → media_tags |
| Unique | (studio_id,id); (asset_id,tag_id) |
| Check / transition invariants | — |
| Indexes | (tag_id,asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 44. billing.plans

**Release:** V1. **Purpose:** Versioned capability packages; no price assumptions.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. code text; revision integer; name text; state text='DRAFT'; effective_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | — |
| Unique | (code,revision); partial(code) WHERE state='ACTIVE' |
| Check / transition invariants | revision>0; state DRAFT/ACTIVE/RETIRED; active definition immutable |
| Indexes | unique code sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC approved offering, INTERNAL draft |

### 45. billing.entitlements

**Release:** V1. **Purpose:** Typed stable capability definitions.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. code text; value_type text; unit text |
| PK | id (UUIDv7, bundle I) |
| FK | — |
| Unique | (code) |
| Check / transition invariants | value_type BOOLEAN/LIMIT/ALLOWLIST; code allowlist initially documented |
| Indexes | unique code sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC capability descriptions |

### 46. billing.plan_entitlements

**Release:** V1. **Purpose:** Version-specific capability values.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. plan_id uuid; entitlement_id uuid; limit_value bigint?; enabled boolean?; allowed_values jsonb? |
| PK | id (UUIDv7, bundle I) |
| FK | plan_id → plans; entitlement_id → entitlements |
| Unique | (plan_id,entitlement_id) |
| Check / transition invariants | exactly one typed value; limits>=0; schema-validated bounded enum string set; unlimited explicit policy, not negative numbers |
| Indexes | (entitlement_id,plan_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL config |

### 47. billing.subscriptions

**Release:** V1. **Purpose:** Tenant plan assignment, including provider-free pilot.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. plan_id uuid; state text='PENDING'; provider text?; provider_account text?; environment text; provider_customer_ref text?; provider_subscription_ref text?; current_period_start timestamptz?; current_period_end timestamptz?; cancel_at_period_end boolean=false; last_provider_revision text?; reconciled_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; plan_id → plans |
| Unique | (studio_id,id); partial(studio_id) WHERE state IN ('PENDING','TRIALING','ACTIVE','PAST_DUE','PAUSED'); (provider,provider_account,environment,provider_subscription_ref) WHERE provider_subscription_ref IS NOT NULL |
| Check / transition invariants | provider binding all-or-none required fields; periods paired end>start; lifecycle checks; live provider disabled until28 |
| Indexes | (studio_id,created_at,id); (state,current_period_end) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE: provider customer/subscription mapping |

### 48. billing.usage_accounts

**Release:** V1. **Purpose:** Locked authoritative quota projections backed by ledger; renewable periods and lifetime resources.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. entitlement_id uuid; period_key text; period_start timestamptz?; period_end timestamptz?; limit_units bigint; granted_units bigint=0; consumed_units bigint=0; reserved_units bigint=0 |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; entitlement_id → entitlements |
| Unique | (studio_id,id); (studio_id,entitlement_id,period_key) |
| Check / transition invariants | all units>=0; dates paired; nonrenewable period_key='LIFETIME'; consumed may exceed downgraded limit but new reservation denied |
| Indexes | unique account key sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 49. billing.usage_reservations

**Release:** V1. **Purpose:** One quota hold per business command; unknown AI work cannot auto-expire.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. account_id uuid; operation_key text; units bigint; settled_units bigint=0; state text='HELD'; expires_at timestamptz?; reconciliation_hold boolean=false; resolved_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:account_id → usage_accounts |
| Unique | (studio_id,id); (studio_id,operation_key,account_id); (studio_id,account_id,id) |
| Check / transition invariants | units>0; settled between0..units; state HELD/CONSUMED/RELEASED; unknown holds never time-only release |
| Indexes | (account_id,state); partial(expires_at) WHERE state='HELD' |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 50. billing.usage_ledger

**Release:** V1. **Purpose:** Append-only integer quota/credit deltas; reversals are new entries.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. account_id uuid; reservation_id uuid?; operation_key text; entry_type text; granted_delta bigint=0; consumed_delta bigint=0; reserved_delta bigint=0; reverses_entry_id uuid?; reason_code text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:account_id → usage_accounts; (studio_id,account_id,reservation_id) → usage_reservations(studio_id,account_id,id); T:reverses_entry_id → usage_ledger |
| Unique | (studio_id,id); (account_id,operation_key,entry_type) |
| Check / transition invariants | GRANT/RESERVE/CONSUME/RELEASE/REFUND/ADJUST; nonzero delta; type-specific sign equations; no updates/deletes by runtime |
| Indexes | (account_id,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL financial-adjacent accounting; retained separately |

### 51. billing.payment_transactions

**Release:** V1. **Purpose:** Platform subscription/credit payment facts and refund/dispute linkage, not V2 client invoices.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. subscription_id uuid?; parent_transaction_id uuid?; provider text; provider_account text; environment text; provider_transaction_ref text; kind text; amount_minor bigint; currency text; state text='PENDING'; reconciled_at timestamptz?; failure_code text? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:subscription_id → subscriptions; T:parent_transaction_id → payment_transactions |
| Unique | (studio_id,id); (provider,provider_account,environment,provider_transaction_ref) |
| Check / transition invariants | amount>=0; currency^[A-Z]{3}$; kind CHARGE/REFUND/DISPUTE/ADJUSTMENT; state PENDING/SUCCEEDED/FAILED/RECONCILIATION_REQUIRED; refund total under locked parent<=charge |
| Indexes | (studio_id,created_at,id); (parent_transaction_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE provider references; no PAN/card credentials |

### 52. billing.webhook_events

**Release:** V1. **Purpose:** Authenticated billing provider inbox; permanent business dedup outlives payload.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. studio_id uuid?; provider text; provider_account text; environment text; provider_event_id text; event_type text; received_at timestamptz=now(); processed_at timestamptz?; state text='RECEIVED'; payload_hash bytea; payload_schema_version integer; redacted_payload jsonb; failure_code text?; reconcile_after timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios |
| Unique | (provider,provider_account,environment,provider_event_id) |
| Check / transition invariants | hash32; bounded allowlisted payload<=16KiB; state RECEIVED/PROCESSED/REJECTED/RECONCILIATION_REQUIRED; tenant resolved by trusted provider mapping |
| Indexes | (state,reconcile_after,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE redacted binding data; never public or studio raw inbox |

### 53. ai.ai_generations

**Release:** V1. **Purpose:** Immutable request/history plus mutable execution status; no AI→projects synchronous dependency.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M,D. requested_by uuid?; parent_generation_id uuid?; input_asset_id uuid; mask_asset_id uuid?; usage_reservation_id uuid; provider text; provider_account text; environment text; model text; model_version text; operation text; instruction text; parameters_schema_version integer; parameters jsonb; status text='QUEUED'; mask_width integer?; mask_height integer?; input_checksum bytea; mask_checksum bytea?; mask_convention text?; coordinate_version integer?; started_at timestamptz?; completed_at timestamptz?; cancel_requested_at timestamptz?; reconcile_after timestamptz?; failure_code text? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; requested_by → users.users; T:parent_generation_id → ai_generations; T:input_asset_id,mask_asset_id → media.media_assets; T:usage_reservation_id → billing.usage_reservations |
| Unique | (studio_id,id); (usage_reservation_id) |
| Check / transition invariants | self parent forbidden; parent immutable and existing before child; strict AI status/parameter schemas; mask metadata all-or-none, dimensions match oriented input; references/inputs private READY |
| Indexes | (studio_id,created_at DESC,id DESC); (status,reconcile_after,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE: instruction,parameters,asset bindings; no provider secrets |

### 54. ai.ai_generation_references

**Release:** V1. **Purpose:** Multiple same-kind references with independent targets and instructions.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. generation_id uuid; asset_id uuid; reference_type text; subtype text?; apply_target text; instruction text?; position integer; checksum bytea |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:generation_id → ai_generations; T:asset_id → media.media_assets |
| Unique | (studio_id,id); (generation_id,position) |
| Check / transition invariants | type COLOUR/MATERIAL/HANDLE/DESIGN; position>=0; checksum32; no unique(type); immutable after queue; private reference required |
| Indexes | (studio_id,asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE references and instructions |

### 55. ai.ai_generation_outputs

**Release:** V1. **Purpose:** Multiple immutable output asset variants per generation.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. generation_id uuid; asset_id uuid; variant_number integer; selected boolean=false |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:generation_id → ai_generations; T:asset_id → media.media_assets |
| Unique | (studio_id,id); (generation_id,variant_number); (asset_id); (studio_id,generation_id,id) |
| Check / transition invariants | variant>0; asset provenance AI_GENERATED; all original outputs private; selected not approval |
| Indexes | (generation_id,selected) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE until explicit project derivative publication |

### 56. ai.ai_attempts

**Release:** V1. **Purpose:** Durable provider submission intent, receipts and uncertain charge reconciliation.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. generation_id uuid; attempt_number integer; submission_key text; provider_request_id text?; state text='INTENT_RECORDED'; lease_epoch bigint=0; lease_owner text?; lease_until timestamptz?; submitted_at timestamptz?; last_checked_at timestamptz?; provider_cost numeric(20,8)?; cost_currency text?; provider_usage jsonb?; failure_code text?; reconciliation_evidence text? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:generation_id → ai_generations |
| Unique | (studio_id,id); (generation_id,attempt_number); (submission_key); partial(generation_id) WHERE state IN ('INTENT_RECORDED','SUBMITTED','UNKNOWN','RESULT_STAGED') |
| Check / transition invariants | attempt>0; cost>=0 and currency paired; safe bounded usage<=4KiB; no retry unknown intent; request ID bound provider/account/env through generation; lease>=0 |
| Indexes | (state,lease_until,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE receipts/evidence; no raw vendor bodies |

### 57. ai.provider_callbacks

**Release:** V1. **Purpose:** Authenticated AI callback inbox independent of billing.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. studio_id uuid?; generation_id uuid?; provider text; provider_account text; environment text; provider_event_id text; payload_hash bytea; safe_payload jsonb; received_at timestamptz=now(); processed_at timestamptz?; state text='RECEIVED' |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,generation_id) → ai_generations(studio_id,id) |
| Unique | (provider,provider_account,environment,provider_event_id) |
| Check / transition invariants | hash32; safe schema<=8KiB; tenant/generation both null or both present; callback cannot grant authority |
| Indexes | (state,received_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE provider receipt; no output URLs retained in generic payload |

### 58. ai.client_review_sessions

**Release:** V1. **Purpose:** V1 private concept share grant; full feedback deferred.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. token_hash bytea; expires_at timestamptz; revoked_at timestamptz?; created_by uuid?; recipient_user_id uuid?; can_comment boolean=false; can_approve boolean=false; review_cookie_epoch bigint=0 |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; created_by,recipient_user_id → users.users |
| Unique | (studio_id,id); (token_hash) |
| Check / transition invariants | hash32; expires>created; V1 actions read-only; cookie epoch>=0; membership grant issuer validated |
| Indexes | (studio_id,created_at,id); (expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | HIGHLY_SENSITIVE hash; PII recipient |

### 59. ai.client_review_items

**Release:** V1. **Purpose:** Grant scope is exact immutable output set.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. session_id uuid; output_id uuid; position integer |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:session_id → client_review_sessions; T:output_id → ai_generation_outputs |
| Unique | (studio_id,id); (session_id,output_id); (session_id,position); (studio_id,session_id,id) |
| Check / transition invariants | position>=0; newly generated output never inherits inclusion/approval |
| Indexes | (output_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE private concept association |

### 60. leads.leads

**Release:** V1. **Purpose:** Durable tenant enquiry and basic pipeline; customer view is allowlisted.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M,D. customer_user_id uuid?; contact_name text; phone text?; email text?; whatsapp text?; city_id uuid?; locality text?; budget_min_minor bigint?; budget_max_minor bigint?; currency text?; property_type_id uuid?; property_kind text='PROPERTY'; message text; reference_project_id uuid?; status text='NEW'; assigned_member_id uuid?; follow_up_at timestamptz?; follow_up_timezone text?; consent_version text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; customer_user_id → users.users; city_id → designers.cities; (property_kind,property_type_id) → designers.taxonomy_terms(kind,id); T:reference_project_id → projects.projects; T:assigned_member_id → designers.studio_members |
| Unique | (studio_id,id); — |
| Check / transition invariants | phone or email required; BUDGET; exact lead lifecycle; property kind; followup instant/zone paired; assignee active enforced by service |
| Indexes | (studio_id,status,created_at DESC,id DESC); (customer_user_id,created_at DESC,id DESC); (studio_id,follow_up_at) WHERE follow_up_at IS NOT NULL |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII: contacts,name,locality; SENSITIVE: message; assignments/notes not customer-visible |

### 61. leads.lead_status_history

**Release:** V1. **Purpose:** Append-only CRM transition facts, including initial NEW.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. lead_id uuid; from_status text?; to_status text; actor_user_id uuid?; changed_at timestamptz=now(); reason_code text?; lead_version bigint |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:lead_id → leads; actor_user_id → users.users |
| Unique | (studio_id,id); (lead_id,lead_version) |
| Check / transition invariants | valid states/transition; initial null→NEW only |
| Indexes | (lead_id,changed_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII actor; INTERNAL workflow |

### 62. leads.lead_notes

**Release:** V1. **Purpose:** Private studio notes, never merged into customer messages.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. lead_id uuid; author_user_id uuid?; body text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:lead_id → leads; author_user_id → users.users |
| Unique | (studio_id,id); — |
| Check / transition invariants | body1..4000; audit edits |
| Indexes | (lead_id,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE body |

### 63. leads.lead_attribution

**Release:** V1. **Purpose:** Single immutable consent-aware initial acquisition record.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. lead_id uuid; source_id uuid; source_kind text='SOURCE'; campaign text?; entry_path text?; referrer_host text?; utm_source text?; utm_medium text?; utm_campaign text?; attribution_rule_version text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:lead_id → leads; (source_kind,source_id) → designers.taxonomy_terms(kind,id) |
| Unique | (studio_id,id); (lead_id) |
| Check / transition invariants | source_kind='SOURCE'; bounded strings; scrub query/tokens/PII; no full referrer URL |
| Indexes | (studio_id,source_id,created_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL; treat accidental user strings as PII until sanitized |

### 64. leads.lead_attachments

**Release:** V1. **Purpose:** Tenant-owned private attachment usages.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. lead_id uuid; asset_id uuid; position integer; submitted_by_customer boolean=false |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:lead_id → leads; T:asset_id → media.media_assets |
| Unique | (studio_id,id); (lead_id,asset_id); (lead_id,position) |
| Check / transition invariants | position>=0; private-only asset; own-enquiry customer only their submissions |
| Indexes | (studio_id,asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE private attachment |

### 65. reviews.content_reports

**Release:** V1. **Purpose:** V1 report/moderation cases with constrained typed target.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. reporter_user_id uuid?; reporter_contact_ciphertext bytea?; key_version text?; target_project_id uuid?; target_asset_id uuid?; targets_studio boolean=false; reason text; description text?; status text='OPEN'; assigned_moderator_id uuid?; resolution_code text?; resolved_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; reporter_user_id,assigned_moderator_id → users.users; T:target_project_id → projects.projects; T:target_asset_id → media.media_assets |
| Unique | (studio_id,id); — |
| Check / transition invariants | exactly one target(project,asset,studio); reason COPYRIGHT/COPIED_IMAGE/FAKE_PROJECT/IMPERSONATION/INAPPROPRIATE/OTHER; state OPEN/INVESTIGATING/ACTIONED/DISMISSED/APPEALED/CLOSED; encryption pair |
| Indexes | (status,created_at,id); (studio_id,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE reporter/description; no automatic disclosure to reported studio |

### 66. reviews.report_actions

**Release:** V1. **Purpose:** Append-only decisions, appeals and evidence link; no overwritten moderation history.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. report_id uuid; actor_user_id uuid?; action text; reason text; evidence_asset_id uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:report_id → content_reports; actor_user_id → users.users; T:evidence_asset_id → media.media_assets |
| Unique | (studio_id,id); — |
| Check / transition invariants | action ASSIGN/RESTRICT/DISMISS/APPEAL/REOPEN/RESOLVE; evidence private-only |
| Indexes | (report_id,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE case material |

### 67. seo.landing_pages

**Release:** V1. **Purpose:** Explicit curated city/category/service editorial pages; not all permutations.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. path text; city_id uuid?; term_id uuid?; term_kind text?; title text; description text; body jsonb; schema_version integer; status text='DRAFT'; robots_preference text='AUTO'; quality_rule_version text; published_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | city_id → designers.cities; (term_kind,term_id) → designers.taxonomy_terms(kind,id) |
| Unique | (path) |
| Check / transition invariants | path registered namespace; body bounded structured content; taxonomy pair; status DRAFT/PUBLISHED/ARCHIVED; robots AUTO/NOINDEX |
| Indexes | (status,path) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC eligible editorial content |

### 68. discovery.project_search_documents

**Release:** V1. **Purpose:** Rebuildable published-project search projection; no draft fields.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. project_id uuid; project_revision_id uuid; aggregate_version bigint; title text; summary text; document tsvector; city_id uuid?; property_type_id uuid?; property_kind text='PROPERTY'; budget_min_minor bigint?; budget_max_minor bigint?; currency text?; has_real_work boolean; projected_at timestamptz=now() |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,project_id,project_revision_id) → projects.project_revisions(studio_id,project_id,id); city_id → designers.cities; (property_kind,property_type_id) → designers.taxonomy_terms(kind,id) |
| Unique | (studio_id,id); (project_id) |
| Check / transition invariants | aggregate_version>=0; BUDGET only published display range; property_kind='PROPERTY'; live gate before result/count/facet |
| Indexes | Phase25 GIN(document); (city_id,created_at,id) only measured |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC approved projection; stale results filtered by root eligibility |

### 69. notifications.notifications

**Release:** V1. **Purpose:** User-facing in-app notification state.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. user_id uuid; template_code text; template_version integer; safe_arguments jsonb; read_at timestamptz?; dedup_key text |
| PK | id (UUIDv7, bundle I) |
| FK | user_id → users.users |
| Unique | (user_id,dedup_key) |
| Check / transition invariants | template-specific allowlist<=4KiB; safe local link only; no private media or tokens |
| Indexes | (user_id,created_at DESC,id DESC) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII recipient; INTERNAL safe template data |

### 70. notifications.delivery_intents

**Release:** V1. **Purpose:** External delivery state separate from in-app messages.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. user_id uuid?; channel text; destination_ciphertext bytea; key_version text; template_code text; template_version integer; arguments_ciphertext bytea; dedup_key text; state text='QUEUED'; provider text?; provider_message_id text?; attempts integer=0; next_attempt_at timestamptz=now(); expires_at timestamptz?; lease_epoch bigint=0; lease_until timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | user_id → users.users |
| Unique | (channel,dedup_key) |
| Check / transition invariants | EMAIL/SMS/WHATSAPP channels feature-gated; states QUEUED/SENDING/DELIVERED/FAILED/UNKNOWN; attempts,epoch>=0 |
| Indexes | (state,next_attempt_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE encrypted destination/content; OTP only identity-owned hashed authority |

### 71. notifications.preferences

**Release:** V1. **Purpose:** Optional user/channel preference; critical security policy separate.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. user_id uuid; category text; channel text; enabled boolean=true |
| PK | id (UUIDv7, bundle I) |
| FK | user_id → users.users |
| Unique | (user_id,category,channel) |
| Check / transition invariants | registered category/channel; cannot suppress mandatory security notice |
| Indexes | unique user prefix sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII user association |

### 72. audit.audit_events

**Release:** V1. **Purpose:** Append-only sensitive action evidence, not general logs.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. studio_id uuid?; actor_user_id uuid?; actor_kind text; action text; resource_type text; resource_id uuid?; reason_code text; outcome text; request_id uuid; safe_metadata jsonb; metadata_schema_version integer |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; actor_user_id → users.users |
| Unique | — |
| Check / transition invariants | registered resource/action; bounded safe diff<=4KiB; logical resource_id deliberately survives target purge, never authorization FK |
| Indexes | (studio_id,created_at,id); (resource_type,resource_id,created_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII actor; INTERNAL redacted evidence; runtime INSERT only |

### 73. audit.outbox_events

**Release:** V1. **Purpose:** Durable minimal domain events; shared infra envelope under audit schema, not business audit stream.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. studio_id uuid?; producer text; aggregate_type text; aggregate_id uuid; aggregate_version bigint; event_type text; schema_version integer; payload jsonb; request_id uuid |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios |
| Unique | (producer,aggregate_id,aggregate_version,event_type) |
| Check / transition invariants | aggregate_version>=0; typed payload<=8KiB; IDs only, no contact/prompt/token; historical aggregate ID intentionally logical |
| Indexes | (created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 74. audit.outbox_dispatches

**Release:** V1. **Purpose:** One relay delivery per destination; explicit fan-out.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. event_id uuid; destination text; state text='PENDING'; attempts integer=0; next_attempt_at timestamptz=now(); lease_epoch bigint=0; lease_until timestamptz?; dispatched_at timestamptz?; failure_code text? |
| PK | id (UUIDv7, bundle I) |
| FK | event_id → outbox_events |
| Unique | (event_id,destination) |
| Check / transition invariants | state PENDING/SENDING/DISPATCHED/DEAD; attempts,epoch>=0 |
| Indexes | partial(next_attempt_at,id) WHERE state='PENDING'; (lease_until) WHERE state='SENDING' |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 75. audit.consumer_receipts

**Release:** V1. **Purpose:** Idempotent consumer commit marker; stored atomically with projection effect.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. consumer text; event_id uuid; processed_at timestamptz=now(); aggregate_version bigint |
| PK | id (UUIDv7, bundle I) |
| FK | event_id logical event UUID; no FK because analytics/external transport events may originate outside outbox |
| Unique | (consumer,event_id) |
| Check / transition invariants | version>=0; no arbitrary payload |
| Indexes | (processed_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 76. audit.idempotency_keys

**Release:** V1. **Purpose:** Scoped request replay guard, not global bearer authorization.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. studio_id uuid?; actor_scope_hash bytea; operation text; key_hash bytea; request_hash bytea; state text='PROCESSING'; result_resource_id uuid?; response_code smallint?; expires_at timestamptz |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios |
| Unique | NULLS NOT DISTINCT(studio_id,actor_scope_hash,operation,key_hash) |
| Check / transition invariants | hashes32; scoped authorized result type from operation; no sensitive body; state PROCESSING/COMPLETED; resource logical replay pointer reauthorized |
| Indexes | (expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE hashes; anonymous scope includes server-issued form intent/contact hash |

### 77. audit.deletion_requests

**Release:** V1. **Purpose:** Durable export/deletion coordination; explicit user or studio root.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. user_id uuid?; studio_id uuid?; kind text; state text='REQUESTED'; requested_by uuid?; retention_policy_version text; restrict_at timestamptz?; purge_after timestamptz?; completed_at timestamptz?; blocked_reason_code text?; whole_studio boolean=false; target_project_id uuid?; target_asset_id uuid?; target_portfolio_id uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | user_id,requested_by → users.users; studio_id → designers.designer_studios; T:target_project_id → projects.projects; T:target_asset_id → media.media_assets; T:target_portfolio_id → portfolios.portfolios |
| Unique | one active request per (kind,target type,target ID), via separate partial unique indexes on user_id, studio_id WHERE whole_studio, target_project_id, target_asset_id, target_portfolio_id |
| Check / transition invariants | exactly one target user/whole_studio/project/asset/portfolio; studio_id required for tenant targets and null for user target; kind DELETE/EXPORT; lifecycle defined; no legal period assumption |
| Indexes | (state,purge_after,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE privacy request |

### 78. audit.deletion_tasks

**Release:** V1. **Purpose:** Reconciliable DB/object/provider cleanup or export steps.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. request_id uuid; owner_module text; resource_id uuid; operation text; destination text; object_identity_ciphertext bytea?; key_version text?; state text='PENDING'; attempts integer=0; next_attempt_at timestamptz=now(); completed_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | request_id → deletion_requests; resource_id intentionally logical after purge |
| Unique | (request_id,owner_module,resource_id,operation,destination) |
| Check / transition invariants | state PENDING/RUNNING/SUCCEEDED/FAILED/BLOCKED; attempts>=0; encrypt pair; no downloaded bytes |
| Indexes | (state,next_attempt_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE restricted cleanup/export identity |

### 79. audit.deletion_tombstones

**Release:** V1. **Purpose:** Post-purge non-PII suppression keys, retained beyond restore horizon.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. request_id uuid; owner_module text; resource_type text; resource_id uuid; purge_completed_at timestamptz; retain_until timestamptz |
| PK | id (UUIDv7, bundle I) |
| FK | request_id → deletion_requests; resource_id logical, no FK to deleted target |
| Unique | (owner_module,resource_type,resource_id) |
| Check / transition invariants | retain_until>=purge_completed_at; all backup copies horizon enforced by policy |
| Indexes | (retain_until) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL identifiers; external durable export required |

### 80. analytics.analytics_events

**Release:** V1. **Purpose:** Bounded raw first-party events separate from money and audit.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. source text; event_id uuid; studio_id uuid; project_id uuid?; event_type text; occurred_at timestamptz; session_digest bytea?; source_code text?; schema_version integer; consent_mode text |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,project_id) → projects.projects(studio_id,id) |
| Unique | (source,event_id) |
| Check / transition invariants | registered event/consent; no raw IP/full referrer; bounded timestamp skew; retention policy |
| Indexes | (studio_id,occurred_at,id); (occurred_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII pseudonymous session_digest; never fingerprint |

### 81. analytics.daily_metrics

**Release:** V1. **Purpose:** Small rebuildable daily tenant metrics; advanced dimensions deferred.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. day date; metric text; source_code text; count bigint; rule_version integer |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; — |
| Unique | (studio_id,id); (studio_id,day,metric,source_code,rule_version) |
| Check / transition invariants | count>=0; approximate_uniques explicitly not additive across days |
| Indexes | unique tenant/day sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL aggregated; low count disclosure policy |

### 82. admin.feature_flags

**Release:** V1. **Purpose:** Audited deployment capability/kill switch config, not permission or plan.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. code text; enabled boolean=false; config_schema_version integer; safe_config jsonb; changed_by uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | changed_by → users.users |
| Unique | (code) |
| Check / transition invariants | registered code; bounded strict config<=4KiB; unknown off |
| Indexes | unique code sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL |

### 83. admin.access_cases

**Release:** V1. **Purpose:** Narrow staff-purpose authorization for tenant/private inspection.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M. studio_id uuid; actor_user_id uuid; action_scope text; resource_type text; resource_id uuid?; reason text; approved_by uuid?; expires_at timestamptz; revoked_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; actor_user_id,approved_by → users.users |
| Unique | — |
| Check / transition invariants | scope allowlist; exact type/ID validated by owning facade; expiry>created; sensitive scope requires step-up/audit |
| Indexes | (actor_user_id,studio_id,expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE staff reason; no broad superuser bypass |

### 84. ai.concept_feedback

**Release:** V1.5. **Purpose:** V1.5 append-only typed feedback on scoped output; new versions never inherit approval.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. session_id uuid; review_item_id uuid; actor_user_id uuid?; actor_grant_fingerprint bytea; action text; comment text?; supersedes_feedback_id uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; (studio_id,session_id,review_item_id) → client_review_items(studio_id,session_id,id); actor_user_id → users.users; T:supersedes_feedback_id → concept_feedback |
| Unique | (studio_id,id); — |
| Check / transition invariants | action LIKE/COMMENT/APPROVE/REQUEST_CHANGE; comment required for COMMENT/REQUEST_CHANGE; grant-authorized actor evidence immutable |
| Indexes | (review_item_id,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE feedback/actor |

### 85. reviews.reviews

**Release:** V1.5. **Purpose:** V1.5 review root; moderated revision pointer and legitimate-client evidence.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. project_id uuid?; customer_user_id uuid; current_revision_id uuid?; published_revision_id uuid?; status text='DRAFT'; verification_request_id uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_id → projects.projects; customer_user_id → users.users; (studio_id,id,current_revision_id) and (studio_id,id,published_revision_id) → review_revisions(studio_id,review_id,id) deferred NO ACTION; T:verification_request_id → verification_requests |
| Unique | (studio_id,id); NULLS NOT DISTINCT(studio_id,customer_user_id,project_id) |
| Check / transition invariants | review state; publish gate legitimate client/conflict disclosure; self-review denied by service |
| Indexes | (studio_id,status,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII author; public only approved projection |

### 86. reviews.review_revisions

**Release:** V1.5. **Purpose:** Append-only reviewed text/rating history; edit requires re-moderation.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. review_id uuid; revision_number integer; rating smallint; body text; disclosed_conflict text?; moderation_status text='PENDING'; moderated_by uuid?; moderated_at timestamptz?; reason_code text? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:review_id → reviews; moderated_by → users.users |
| Unique | (studio_id,id); (studio_id,review_id,id); (review_id,revision_number) |
| Check / transition invariants | rating1..5; revision>0; body bounds; moderation PENDING/APPROVED/REJECTED; locked content after submission |
| Indexes | (review_id,revision_number) covered |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII/public-intent text; private moderator data |

### 87. reviews.review_dimensions

**Release:** V1.5. **Purpose:** V1.5 normalized optional rating dimensions.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. review_revision_id uuid; dimension text; rating smallint |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:review_revision_id → review_revisions |
| Unique | (studio_id,id); (review_revision_id,dimension) |
| Check / transition invariants | rating1..5; dimension DESIGN/QUALITY/COMMUNICATION/TIMELINE/BUDGET_ADHERENCE/AFTER_SALES |
| Indexes | unique revision prefix sufficient |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PUBLIC approved ratings |

### 88. reviews.verification_requests

**Release:** V1.5. **Purpose:** V1.5 studio/project verification history and expiry, independent of publication.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. project_id uuid?; type text; status text='PENDING'; submitted_at timestamptz?; reviewed_at timestamptz?; reviewed_by uuid?; reason_code text?; expires_at timestamptz?; supersedes_request_id uuid? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:project_id → projects.projects; reviewed_by → users.users; T:supersedes_request_id → verification_requests |
| Unique | (studio_id,id); — |
| Check / transition invariants | type PHONE/IDENTITY/BUSINESS/ADDRESS/PROJECT; PROJECT iff project_id nonnull; valid verification lifecycle |
| Indexes | (studio_id,type,status); (project_id); (status,expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | SENSITIVE decision metadata |

### 89. reviews.verification_evidence

**Release:** V1.5. **Purpose:** V1.5 restricted evidence assets with explicit retention deadline.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. verification_request_id uuid; asset_id uuid; retain_until timestamptz; encrypted_metadata bytea?; key_version text? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:verification_request_id → verification_requests; T:asset_id → media.media_assets |
| Unique | (studio_id,id); (verification_request_id,asset_id) |
| Check / transition invariants | asset private_only; retain_until policy-set; metadata encryption pair |
| Indexes | (retain_until); (studio_id,asset_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | HIGHLY_SENSITIVE evidence |

### 90. discovery.collections

**Release:** V1.5. **Purpose:** V1.5 customer-owned project ideas; studios cannot read by membership.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,M,D. owner_user_id uuid; name text; description text? |
| PK | id (UUIDv7, bundle I) |
| FK | owner_user_id → users.users |
| Unique | (owner_user_id,id) |
| Check / transition invariants | bounded name/description; private default |
| Indexes | (owner_user_id,created_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII private interests |

### 91. discovery.collection_items

**Release:** V1.5. **Purpose:** V1.5 ordered project-only saves; later boards need a separate spec.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. owner_user_id uuid; collection_id uuid; project_studio_id uuid; project_id uuid; position integer; note text? |
| PK | id (UUIDv7, bundle I) |
| FK | (owner_user_id,collection_id) → collections(owner_user_id,id); (project_studio_id,project_id) → projects.projects(studio_id,id) |
| Unique | (collection_id,project_id); (collection_id,position) |
| Check / transition invariants | position>=0; live eligibility filters unavailable project; ownership is customer, not project studio |
| Indexes | (project_studio_id,project_id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII interests/notes |

### 92. discovery.collection_grants

**Release:** V1.5. **Purpose:** V1.5 intentional named-studio access to one customer collection.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I. owner_user_id uuid; collection_id uuid; studio_id uuid; expires_at timestamptz; revoked_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | (owner_user_id,collection_id) → collections(owner_user_id,id); studio_id → designers.designer_studios |
| Unique | (collection_id,studio_id) |
| Check / transition invariants | expires>created; read-only; scope excludes customer other data |
| Indexes | (studio_id,expires_at) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | PII sharing association |

### 93. designers.custom_domains

**Release:** V1.5. **Purpose:** V1.5 verified normalized host registry and canonical generation.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T,M. hostname text; challenge_hash bytea; state text='REQUESTED'; primary_host boolean=false; verified_at timestamptz?; revalidate_at timestamptz?; certificate_ref text?; registry_generation bigint=0; removed_at timestamptz? |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; — |
| Unique | (studio_id,id); partial(hostname) WHERE state<>'REMOVED'; partial(studio_id) WHERE primary_host=true AND state='ACTIVE' |
| Check / transition invariants | IDNA exact host; lifecycle; hash32; primary requires ACTIVE; generation>=0; reassignment fresh proof |
| Indexes | (state,revalidate_at,id) |
| Deletion behavior | RESTRICT all referenced parents; controlled owner purge only. |
| PII classification | INTERNAL challenge; PUBLIC active hostname |

### 94. discovery.studio_search_documents

**Release:** V1. **Purpose:** Rebuildable published business search; mutable onboarding data never leaks.

| Contract | Definition |
|---|---|
| Columns / types / nullability / defaults | Bundles: I,T. portfolio_version_id uuid; aggregate_version bigint; business_name text; summary text; document tsvector; projected_at timestamptz=now() |
| PK | id (UUIDv7, bundle I) |
| FK | studio_id → designers.designer_studios; T:portfolio_version_id → portfolios.portfolio_versions |
| Unique | (studio_id,id); (studio_id) |
| Check / transition invariants | version>=0; only approved public snapshot fields; serve-time live portfolio/studio gate |
| Indexes | Phase25 GIN(document); trigram business_name only after workload evaluation |
| Deletion behavior | RESTRICT all referenced parents; rebuild/drop projection rows before controlled owner purge. |
| PII classification | PUBLIC approved projection |
