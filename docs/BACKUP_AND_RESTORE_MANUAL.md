# ELÉGANCE — BACKUP AND RESTORE MANUAL
## Managed PostgreSQL 18 Backup, Point-in-Time Recovery & Restore
**Target Platform:** Elégance Interior Designer Platform  
**Database Engine:** PostgreSQL 18.x on Neon  
**Document Revision:** 1.0 (Phase 15A Release)  

---

## 1. Backup Strategy Overview

The Elégance platform utilizes a dual-tier backup and disaster recovery architecture:

1. **Continuous Point-in-Time Recovery (PITR):** Provided natively by Neon's Write-Ahead Log (WAL) architecture. Enables non-destructive, instant recovery to any arbitrary second within the retention window.
2. **Periodic Logical Backups (`pg_dump`):** Portable, self-contained custom-format (`-Fc`) snapshots stored in encrypted off-site cloud storage.

---

## 2. Neon Native Continuous Backup & PITR

### 2.1 Architecture
Neon separates compute from storage. All changes written to WAL are continuously streamed to Neon's multi-tenant storage nodes and replicated across availability zones. Storage maintains historical page versions, allowing point-in-time branch creation without copying data.

### 2.2 Point-in-Time Recovery via Branching
To recover data from a specific point in time (e.g., prior to an accidental data modification or migration defect):

1. **Identify the Recovery Timestamp:**
   - Determine the exact UTC timestamp or LSN prior to the event (e.g., `2026-10-17T12:00:00Z`).
2. **Create a Restored Branch via Neon CLI:**
   ```bash
   neon branch create --name recovery-point --parent production --timestamp "2026-10-17T12:00:00Z"
   ```
3. **Inspect and Verify Data on the Restored Branch:**
   - Connect to the recovery branch endpoint to verify table counts and tenant data.
4. **Promote the Restored Branch:**
   - Swap the database connection URL in application configuration to point to the newly restored branch, or export specific affected rows back into production.

---

## 3. Logical Backup Procedure (`pg_dump`)

### 3.1 Executing Scheduled Backups
Run `pg_dump` in custom format (`-Fc`) with owner and privilege stripping:

```powershell
# Windows PowerShell Backup Execution
$env:PGPASSWORD = $env:DB_PASSWORD
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" `
    -h $env:DB_HOST `
    -U $env:DB_USER `
    -d $env:DB_NAME `
    -p 5432 `
    -Fc `
    --no-owner `
    --no-privileges `
    -f "backups/elegance-db-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss').dump"
```

### 3.2 Backup Integrity Verification
Verify the archive Table of Contents without performing a restore:

```bash
pg_restore -l "backups/elegance-db-backup-<timestamp>.dump"
```
Ensure that core tenant tables (`designer_studios`, `studio_projects`, `project_rooms`, `studio_leads`, `media_assets`) are present in the catalog list.

---

## 4. Disaster Recovery & Restoral Procedure (`pg_restore`)

### 4.1 Target Environment Preparation
1. Create a clean destination database or schema:
   ```sql
   CREATE DATABASE elegance_recovery;
   ```
2. Ensure required extensions and roles are present:
   ```sql
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   ```

### 4.2 Executing the Restore
Restore the custom dump file using `pg_restore`:

```powershell
$env:PGPASSWORD = $env:DB_PASSWORD
& "C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" `
    -h $env:DB_HOST `
    -U $env:DB_USER `
    -d "elegance_recovery" `
    -p 5432 `
    --clean `
    --if-exists `
    --no-owner `
    --no-privileges `
    "backups/elegance-db-backup-<timestamp>.dump"
```

### 4.3 Post-Restore Verification Checklist
1. **Catalog Count:** Verify all 77 user tables exist (`SELECT count(*) FROM pg_tables WHERE schemaname = 'public'`).
2. **Flyway History:** Verify `flyway_schema_history` reports 40 completed migrations.
3. **Row-Level Security:** Verify 59 tenant tables have `rowsecurity = true` and `forcerowsecurity = true`.
4. **Smoke Query:** Execute sample tenant queries with `app.current_studio_id` set to verify RLS enforcement.
