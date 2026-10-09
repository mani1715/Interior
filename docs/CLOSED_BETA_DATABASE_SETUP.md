# CLOSED-BETA DATABASE SETUP & RAILWAY POSTGRESQL GUIDE
## Elégance — Interior Designer Platform

**Document Version:** 1.0.0  
**Effective Date:** October 9, 2026  
**Status:** Operations Runbook for Phase 8A

---

## 1. PURPOSE & SECURITY BOUNDARIES

This guide defines the procedure to connect the Elégance platform to an externally provisioned **Railway PostgreSQL** instance for the closed beta release.

### Core Security Rules
1. **NO Hardcoded Secrets:** Never commit database credentials, connection strings, or passwords to Git.
2. **NO Dev Fixtures / Personas:** The closed-beta database must start completely empty of local test data, test personas, and development mocks.
3. **NO Superuser Runtime:** The application runtime database user must **never** possess PostgreSQL `SUPERUSER` or `BYPASSRLS` privileges.
4. **Mandatory RLS Enforcement:** All tenant tables must have both `ROW LEVEL SECURITY` enabled and `FORCE ROW LEVEL SECURITY` active.
5. **Backups are Release-Blocking:** Real customer data must not be accepted until automated daily backups are verified with a successful restore drill.

---

## 2. POSTGRESQL ENGINE REQUIREMENTS

- **Tested Version (Local Workspace):** PostgreSQL 18.3
- **Minimum Supported Version:** PostgreSQL 15 (required for native UUID functions, JSONB operations, and robust Row-Level Security).
- **Target Railway Version:** **PostgreSQL 16 or 17** (recommended for Flyway 10.x compatibility; PostgreSQL 18.x outputs an informational warning in Flyway 10.x).

---

## 3. REQUIRED POSTGRESQL EXTENSIONS

All database extensions are strictly managed through Flyway migrations:
- **`pg_trgm`**: Trigram index extension for high-performance substring and full-text discovery search. Initialized via `V015__search_discovery_engine.sql` (`CREATE EXTENSION IF NOT EXISTS pg_trgm;`).
- **UUID Functions**: Native `gen_random_uuid()` is built into PostgreSQL 13+. All domain identifiers use client-generated **UUIDv7**; no extension like `uuid-ossp` or `pgcrypto` is required.

---

## 4. ENVIRONMENT VARIABLE TEMPLATE

The backend Spring Boot service uses standard Spring Data configuration properties. Below are the required environment variables:

| Variable Name | Secret? | Example Format / Shape | Purpose |
| :--- | :---: | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | No | `production` | Activates production security profile, disables dev-auth sandbox, enforces SSL cookies. |
| `SPRING_DATASOURCE_URL` | No* | `jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require` | PostgreSQL JDBC connection URL with forced TLS. |
| `SPRING_DATASOURCE_USERNAME` | No* | `postgres` or `elegance_app` | Database username for application connection. |
| `SPRING_DATASOURCE_PASSWORD` | **YES** | `[SECURE_RANDOM_PASSWORD]` | Password for database user. |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | No | `10` (or `5`) | Conservative connection pool ceiling for single Spring Boot replica. |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | No | `2` | Minimum idle pooled connections. |

*\* Note: Although the URL and username are not secrets by themselves, connection strings containing embedded passwords must always be treated as high-severity secrets.*

### Constructing `SPRING_DATASOURCE_URL` from Railway
Railway often provides a generic `DATABASE_URL` in URI format:
```
postgresql://username:password@roundhouse.proxy.rlwy.net:12345/railway
```
For Spring Boot JDBC, extract the discrete components:
- **Host:** `roundhouse.proxy.rlwy.net`
- **Port:** `12345`
- **Database:** `railway` (or custom name)
- **JDBC URL:** `jdbc:postgresql://roundhouse.proxy.rlwy.net:12345/railway?sslmode=require`

---

## 5. DATABASE ROLE ARCHITECTURE & RLS PRESERVATION

### A. Railway Single-Role Model (Default Starter)
On default Railway PostgreSQL instances, a single administrative user (`postgres`) is created. If using this default role for beta:
- Flyway migrations and runtime queries will execute as the same role.
- Because all tenant tables have `FORCE ROW LEVEL SECURITY`, RLS policies are **strictly applied even to the table owner**.
- **Crucial Invariant:** Verify that the connected role does **not** have the `rolbypassrls` attribute set to `true`.

### B. Dual-Role Least-Privilege Model (Recommended)
If Railway permissions allow creating additional roles:
1. **Migration Role (`elegance_migrator`):** Possesses DDL privileges to execute Flyway schema changes (`CREATE TABLE`, `CREATE INDEX`, `ALTER TABLE ... FORCE ROW LEVEL SECURITY`).
2. **Runtime Application Role (`elegance_app`):** Possesses only DML privileges (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) on public tables.
   - `rolsuper = false`
   - `rolbypassrls = false`

---

## 6. EMPTY DATABASE INITIAL MIGRATION PROCEDURE

Execute the first cloud database deployment using this deterministic sequence:

1. **Provision Railway PostgreSQL:** The user provisions the PostgreSQL service via the Railway console.
2. **Retrieve Connection Details:** Obtain Host, Port, Database, Username, and Password.
3. **Verify Connectivity:** Test TCP reachability with TLS from deployment environment.
4. **Trigger Flyway Migration:**
   - Launch Spring Boot with `SPRING_PROFILES_ACTIVE=production` and the Railway datasource variables.
   - Spring Boot Flyway runner automatically acquires the database lock (`flyway_schema_history`) and applies migrations `V001` through `V033` sequentially.
5. **Verify Clean Execution:** Verify in container logs:
   ```
   Successfully applied 33 migrations to schema "public", now at version v033
   ```
6. **Execute Cloud RLS Diagnostic Script:** Run [`scripts/verify-cloud-db-rls.sql`](file:///c:/my%20projects/interior%20design/scripts/verify-cloud-db-rls.sql) against the Railway instance to ensure all 34 tenant tables have `rls_enabled=true` and `rls_forced=true`.

---

## 7. BACKUP SPECIFICATIONS & RESTORE DRILL

> [!IMPORTANT]
> **Zero Customer Data Without Backups:** Closed Beta real user onboarding cannot commence until automated daily database snapshots are active and a restore drill is successfully verified.

### Backup Requirements on Railway
- **Automated Snapshots:** Enable automated daily backups in Railway service settings.
- **Retention:** Minimum 7-day retention window.
- **Point-in-Time Recovery (PITR):** Note that Railway starter PostgreSQL uses snapshot dumps rather than continuous WAL archiving. If sub-hour PITR is required, a managed provider such as Neon or AWS RDS should be evaluated.

### Pre-Launch Restore Drill Runbook
To verify recoverability without endangering the primary database:
1. **Create Verification Marker:** Insert a dedicated non-production marker into a test record.
2. **Trigger Backup:** Manually trigger a database backup/snapshot via Railway console or `pg_dump`.
3. **Provision Drill Database:** Create a temporary secondary PostgreSQL instance (`elegance-restore-drill`).
4. **Execute Restore:** Restore the backup file into `elegance-restore-drill`.
5. **Audit Restored Database:**
   - Verify marker record exists.
   - Verify `flyway_schema_history` has 34 records up to version `V033`.
   - Run `scripts/verify-cloud-db-rls.sql` to verify RLS policies survived restoration.
6. **Teardown Drill Instance:** Deprovision the temporary restore target.
