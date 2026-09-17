# Phase 02 — Taxonomy Strategy

2026-09-17 • Controlled reference-data design. [Schema](02_DATABASE_SCHEMA.md) provides keys and [migration strategy](02_MIGRATION_STRATEGY.md) defines safe seeds.

## 1. One typed vocabulary, separate dimensions

Use designers.taxonomy_terms with kind, immutable code, display label, normalized slug, active and sort_order. Unique(kind,code), unique(kind,slug) and unique(kind,id). Business type, service, category, room, work type, style, property and material remain **separate dimensions**, even when display labels overlap. No one overloaded category string and no uncontrolled tags as authority.

Consumer FK includes kind: project_terms(term_kind,term_id) references taxonomy_terms(kind,id); studio_services has CHECK term_kind='SERVICE'; studio business_type_kind='BUSINESS'; property_kind='PROPERTY'. This prevents category UUID in a service slot. Search projection's property type uses the same typed validation and is rebuildable, not source authority. Bounded tenant-authored library tags use separate media_tags; project TAG terms are controlled initially, not global user-created taxonomy spam.

Typed lookup rows are normalized entities, not EAV: each term has the same small fixed schema and a constrained kind, each usage has a specific owner/role. No arbitrary per-term attribute JSON. Separate tables per kind were considered; they add repeated administration/migration paths without different current fields. Split a kind into its own richer aggregate later only when its domain requires it.

Application enums + DB text CHECK are appropriate for lifecycles, permissions, media provenance/usage, feedback actions and the six supported theme IDs. Those codes change behavior and require deployment/tests, so admins cannot add an arbitrary executable state or theme. Avoid PostgreSQL native enums for extensible vocabulary. Plan/entitlement definitions are billing-owned tables rather than taxonomy.

## 2. Deterministic initial reference seeds

Seed codes and stable documented UUID literals through versioned Flyway migrations; labels below are English display values, not IDs. This is reference data, never fake professionals/projects/reviews.

| Kind | Initial codes / labels |
|---|---|
| BUSINESS | INTERIOR_DESIGNER; INTERIOR_STUDIO; ARCHITECT; ARCHITECTURE_STUDIO; CUSTOM_FURNITURE; MODULAR_INTERIOR_PROFESSIONAL; WOODWORK; TURNKEY_INTERIORS — preserve all eight master professional types |
| SERVICE | TV_UNIT; WARDROBE; MODULAR_KITCHEN; FULL_HOME_INTERIOR; CUSTOM_FURNITURE; FALSE_CEILING; COMMERCIAL_INTERIOR. More approved services can be added without code deployment |
| CATEGORY | LIVING_ROOM; TV_UNIT; BEDROOM; WARDROBE; MODULAR_KITCHEN; POOJA_UNIT; CROCKERY_UNIT; STUDY_UNIT; FALSE_CEILING; WALL_PANELS; SHOE_RACK; OFFICE; COMMERCIAL; CUSTOM_FURNITURE; FULL_HOME_INTERIOR — exactly15 initial categories |
| ROOM | LIVING_ROOM; BEDROOM; KITCHEN; POOJA_ROOM; STUDY; OFFICE; OTHER. Do not infer bedroom count from a room label |
| WORK | CABINETRY; FURNITURE; CEILING; WALL_FINISH; FULL_INTERIOR; OTHER. Category, work and offered service are distinct links |
| STYLE | MODERN; CONTEMPORARY; MINIMAL; LUXURY; TRADITIONAL; SCANDINAVIAN; INDUSTRIAL; JAPANDI; CLASSIC |
| PROPERTY | APARTMENT; VILLA; INDEPENDENT_HOUSE; OFFICE; RETAIL; RESTAURANT; COMMERCIAL; OTHER |
| MATERIAL | PLYWOOD; LAMINATE; ACRYLIC; VENEER; WOOD; GLASS; MARBLE; PAINT |
| SOURCE | GOOGLE_ORGANIC; INSTAGRAM; FACEBOOK; DIRECT; QR; PORTFOLIO_SHARE; PLATFORM_SEARCH; PROJECT_DISCOVERY; WHATSAPP; OTHER |
| TAG | No invented mandatory seed list; controlled editorial tags introduced when needed |

A typed term can link multiple times only in distinct allowed contexts; unique(revision,kind,term) prevents duplicate project filters. Multiple categories/styles/materials are valid; single property type is optional. Later manufacturer/product SKUs reference material concept rather than converting MATERIAL into supplier inventory.

Lead source is observed/declared attribution with rule version, never a verified claim that Google caused revenue. Referrer stripped to host and safe entry path; query tokens/phone/UTM PII sanitized. Missing source uses OTHER or deliberate DIRECT rule, not fabricated analytics.

## 3. Geography and private addresses

Countries → regions → cities with stable IDs. Country code unique; region code/slug unique in country; city slug unique in region. Do not assume city name globally unique. Public URL city disambiguation is owned by SEO landing path registry; e.g. include region when needed rather than collide two identically named cities. Unicode names retained; route normalization follows Phase01.

Studio service areas are multiple city+optional public neighborhood rows; unique NULLS NOT DISTINCT(studio,city,locality) avoids duplicate city-wide entries. Project city/locality independent of studio's served locations and remains optional. Exact residential address is encrypted project_private_details and not copied to public city/locality; lead free text similarly excluded from SEO.

No complete invented world-city dataset. Initial import must have approved provenance/license/version and validated India regions/cities before use; small verified reference fixtures are separate from production geography. No external geocoding dependency required to begin domain implementation. Store no user geolocation or precise coordinates without a separate purposeful feature requirement.

IANA timezone strings and BCP47 locales validated against deployed application registries; database bounded text, not misleading exhaustive forever SQL enum. Instants timestamptz, completion date/year separate. Currency validation uses supported ISO4217 registry and currency exponent; money stored bigint minor units, no automatic assumption INR for every record.

## 4. Editing, retirement and localization

Codes/IDs immutable; label correction audited. Retire with active=false, hide from new pickers but preserve referenced history. Never hard-delete a referenced term or silently remap existing project style/category. Merges require an explicit old→new mapping migration and SEO redirect review; stale records stay readable until resolved. Lookup label change does not rewrite published portfolio business snapshots; re-publication refreshes the display copy. Discovery labels may use current approved taxonomy names while fact IDs remain stable.

Application caches reference data by reviewed taxonomy generation/config version; invalidate through owner event. Rebuild search when labels change without exposing drafts. User-generated names/descriptions remain Unicode; no destructive transliteration of content.

English initial labels; localized UI strings in application resources. V1.5 may add term translations keyed(term_id,locale) with fallback and localized slug policy after actual translations exist; no translation table now. Content revision carries locale; UI language never manufactures translated business facts/hreflang.

## 5. Non-taxonomy registries

Platform roles: CUSTOMER, DESIGNER, DESIGNER_TEAM, MODERATOR, ADMIN, SUPER_ADMIN. Studio roles: OWNER, ADMIN, PORTFOLIO_MANAGER, PROJECT_MANAGER, MEDIA_MANAGER, LEAD_MANAGER, VIEWER; no combinatorial permission table initially, central reviewed role→action matrix in03. User can hold several platform responsibilities and memberships in multiple studios.

Themes: BASIC, MODERN, LUXURY, ARCHITECTURAL, WARM_NATURAL, DARK_CINEMATIC. Application registry defines renderers, schema compatibility, sections, palette/type/button/hero options. Stored config cannot register new executable theme code.

Entitlements: MAX_PROJECTS, STORAGE_BYTES, AI_CREDITS, CUSTOM_DOMAIN, ADVANCED_SEO, TEAM_MEMBERS, PREMIUM_THEME plus reviewed analytics capability when implemented. Value types LIMIT/BOOLEAN/ALLOWLIST; allowlists strictly controlled (e.g. theme IDs). Plans FREE/PROFESSIONAL/STUDIO are candidate marketing names, not conditional logic or finalized prices. Plan revision immutable when active; no production price seed before approved provider/product decisions.

Reserved route claims are deterministic platform data from [route namespace](01_ROUTE_NAMESPACE.md), including en/te/hi/ta/kn/ml/mr and infrastructure roots. Use RESERVED studio_slug_claims rows; ordinary studio slugs enforce3..80 rule, reserved locale entries are an explicit exception. Future namespace additions run collision audit before migration and never steal an existing tenant's slug.

