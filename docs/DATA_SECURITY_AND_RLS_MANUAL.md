# ELÉGANCE — DATA SECURITY & ROW-LEVEL SECURITY (RLS) MANUAL
## Multi-Tenant Security & Catalog Isolation
**Target Platform:** Elégance Interior Designer Platform  
**Database Engine:** PostgreSQL 18.x on Neon  
**Document Revision:** 1.0 (Phase 15A Release)  

---

## 1. Multi-Tenant Architecture & Security Model

The Elégance platform is a multi-tenant platform serving independent interior design studios. Strict data separation is enforced at the database engine level via **PostgreSQL Row-Level Security (RLS)** with forced table-level constraints.

```
+-------------------------------------------------------------------------+
|                              PostgreSQL 18                              |
|                                                                         |
|  Session Context:                                                       |
|  SET LOCAL app.current_studio_id = '01a14a07-...'                       |
|                                                                         |
|       |                                                                 |
|       v                                                                 |
|  +-------------------------------------------------------------------+  |
|  |             Row-Level Security Filter (Per Table)                 |  |
|  |             USING (studio_id = current_setting('...')::uuid)      |  |
|  +-------------------------------------------------------------------+  |
|       |                                                                 |
|       v                                                                 |
|  +-----------------------+     +-----------------------+                |
|  | Studio Alpha Rows     |     | Studio Beta Rows      |                |
|  | (Visible to Alpha)    |     | (Blocked by RLS)      |                |
|  +-----------------------+     +-----------------------+                |
+-------------------------------------------------------------------------+
```

---

## 2. Table Classification Matrix

Out of 77 non-system tables in schema `public`:
- **59 Tenant Tables:** Forced Row-Level Security enabled. Every row must match the active `studio_id` or an explicit admin override.
- **18 Global / Identity Tables:** Do not use tenant RLS; scoped by user identity or global lookup.

### 2.1 Representative Tenant Tables (Forced RLS)
- `designer_studios`
- `studio_projects`
- `project_rooms`
- `studio_leads`
- `lead_activities`
- `lead_notes`
- `lead_whatsapp_messages`
- `media_assets`
- `media_derivatives`
- `media_security_scans`
- `studio_reviews`
- `studio_subscriptions`
- `gallery_folders`
- `gallery_folder_media`
- `audit_events`

### 2.2 Global / Identity Tables (No Tenant RLS)
- `users`
- `identity_roles`
- `identity_user_roles`
- `identity_sessions`
- `identity_external_identities`
- `identity_mfa_methods`
- `identity_recovery_codes`
- `billing_plans`
- `plan_entitlements`
- `portfolios`
- `portfolio_versions`
- `portfolio_sections`
- `platform_feedback`
- `studio_slug_claims`

---

## 3. Session Context Variables

The application sets PostgreSQL transaction-local configuration parameters at the beginning of each database transaction:

| Parameter | Type | Purpose | Example |
| :--- | :--- | :--- | :--- |
| `app.current_studio_id` | `uuid` | The active designer studio tenant ID | `'01a14a12-dd7e-7638-b6ac-de3fb8ba1953'` |
| `app.current_user_id` | `uuid` | The authenticated user ID | `'01a14a12-dd7e-7638-b6ac-de3fb8ba1954'` |
| `app.is_admin` | `boolean` | Super-admin platform override flag | `'true'` or `'false'` |

### Setting Context via SQL:
```sql
SELECT set_config('app.current_studio_id', '01a14a12-dd7e-7638-b6ac-de3fb8ba1953', true);
```
The third parameter (`is_local = true`) guarantees that the setting automatically resets when the transaction commits or rolls back, preventing connection-reuse context leaks in connection pools.

---

## 4. Role Isolation & `BYPASSRLS` Semantics

In PostgreSQL:
1. `FORCE ROW LEVEL SECURITY` ensures that table owners cannot accidentally bypass RLS during normal queries.
2. Roles with the `BYPASSRLS` attribute (such as cloud master roles like `neondb_owner`) bypass RLS regardless of `FORCE ROW LEVEL SECURITY`.
3. Application runtime roles must be provisioned with `NOSUPERUSER` and `NOBYPASSRLS`.

### Public Lead Ingestion Role (`lead_ingest_role`):
- Created with `NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT`.
- Owns the `submit_public_lead` `SECURITY DEFINER` function.
- Granted strictly `USAGE` on schema `public` and targeted `SELECT`/`INSERT` on `studio_leads` and `lead_activities`. Direct access to other tables is revoked.

---

## 5. Security Verification & Auditing

Execute non-destructive verification using `scripts/verify-cloud-db-rls.sql`:
```bash
node .local/neon-query.cjs scripts/verify-cloud-db-rls.sql
```
Expected output:
- `total_tables`: 77
- `rls_enabled_tables`: 59
- `rls_forced_tables`: 59
- `rls_missing_tables`: 18 (exact global system tables)
