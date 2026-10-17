# Phase 14 — Database Migration Runbook

> **Status:** APPROVED FOR PRODUCTION RELEASE CANDIDATE  
> **Platform:** Elégance Interior Designer Platform  
> **Flyway Version:** V040  
> **Engine:** PostgreSQL 16+ with Row-Level Security (RLS)  

---

## 1. Migration Architecture & Philosophy

The platform uses **Flyway** for strictly ordered, version-controlled, immutable database schema migrations.

### Core Policies
- **Forward-Only Migrations:** Migrations are never edited once committed or applied. Bug fixes or schema changes are applied via new versions (`V041__...sql`).
- **Idempotency & Transactions:** All DDL scripts are wrapped in database transactions where supported by PostgreSQL.
- **Tenant Isolation Safeguards:** Every table containing multi-tenant data must have Row-Level Security (RLS) enabled and verified. Migration scripts must preserve the `current_setting('app.current_user_id', true)` and `current_setting('app.current_studio_id', true)` policy conventions.
- **Automated Execution vs Out-of-Band:**
  - In staging and small deployments, Flyway runs on backend boot (`spring.flyway.enabled=true`).
  - In high-availability zero-downtime production environments, Flyway migrations are executed via an init-container or maintenance task prior to routing traffic to new application instances.

---

## 2. Migration Status Baseline

The current baseline includes 40 Flyway migrations:
- **Baseline Version:** `V001__initial_schema.sql`
- **Current Version:** `V040__subscription_tier_features.sql`
- **Total Migrations:** 40 applied files
- **Multi-Tenant RLS Tables:** 59 tables enforced with tenant security policies

---

## 3. Pre-Migration Checklist

Before executing any database migration in staging or production:

1. [ ] **Verify Database Backup:** Confirm a fresh full database snapshot/dump exists and is restorable.
2. [ ] **Locking Verification:** Check if the pending migration contains table rewrites or locks (e.g. `ALTER TABLE ... ADD COLUMN ... DEFAULT` without fast-default, or full table index creation without `CONCURRENTLY`).
3. [ ] **Check Active Connections:** Query `pg_stat_activity` to ensure no long-running transactions will block DDL locks:
   ```sql
   SELECT pid, age(clock_timestamp(), query_start), usename, query 
   FROM pg_stat_activity 
   WHERE state != 'idle' AND query NOT LIKE '%pg_stat_activity%';
   ```
4. [ ] **Validate Flyway Checksums:** Verify that existing migration files have not been modified:
   ```bash
   mvn flyway:validate -Dflyway.url=jdbc:postgresql://<host>:<port>/<db> -Dflyway.user=<user> -Dflyway.password=<pass>
   ```

---

## 4. Execution Procedures

### Option A: Automated Boot Migration (Standard)
1. Ensure the PostgreSQL container or managed instance is running and accepting connections.
2. Set environment variables:
   ```env
   SPRING_FLYWAY_ENABLED=true
   SPRING_FLYWAY_LOCATIONS=classpath:db/migration
   SPRING_FLYWAY_VALIDATE_ON_MIGRATE=true
   ```
3. Start the backend container. Spring Boot runs `Flyway.migrate()` on startup before initializing Hibernate JPA entities or opening HTTP ports.
4. Verify logs for successful migration:
   ```
   o.f.c.i.c.SqlScriptFlywayCallback      : Successfully applied 1 migration to schema "public" (execution time 00:00.042s)
   ```

### Option B: Standalone Migration Container (Zero-Downtime)
For high-traffic production where multiple API nodes are rolling:
1. Run a one-off Flyway task using the backend container or Flyway CLI:
   ```bash
   docker run --rm \
     --network elegance-net \
     -e SPRING_DATASOURCE_URL="jdbc:postgresql://db:5432/interior_platform_prod" \
     -e SPRING_DATASOURCE_USERNAME="interior_user" \
     -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
     interior-platform-api:latest \
     java -cp app.jar -Dloader.main=org.flywaydb.commandline.Main org.springframework.boot.loader.PropertiesLauncher migrate
   ```
2. Verify migration success status in `flyway_schema_history`:
   ```sql
   SELECT version, description, type, installed_on, execution_time, success 
   FROM flyway_schema_history 
   ORDER BY installed_rank DESC LIMIT 5;
   ```

---

## 5. Rollback & Failure Recovery Procedure

### 5.1 If a Migration Fails During Boot
1. PostgreSQL will automatically roll back the transaction containing the failing DDL statements.
2. The `flyway_schema_history` table will record `success = false` for the failed version.
3. The Spring Boot backend will abort startup (`SpringApplication.run` throws `FlywayException`).

### 5.2 Repair Steps
1. Identify the root cause from the application log stack trace.
2. Fix the underlying database conflict or syntax error.
3. Clean the failed entry from `flyway_schema_history`:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '<failed_version>' AND success = false;
   ```
   *Alternatively, run Flyway repair:*
   ```bash
   mvn flyway:repair ...
   ```
4. Re-run migration.

### 5.3 Emergency Database Restore
If a migration was partially applied outside of a single transaction and corrupted schema state:
1. Stop backend traffic.
2. Restore database from pre-migration snapshot (see `docs/PHASE_14_BACKUP_RESTORE_RUNBOOK.md`).
3. Re-deploy previous stable application container tag.
