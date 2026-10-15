# Phase 12B: PostgreSQL Backup & Restore Operational Runbook

## 1. Overview

This runbook establishes standard operational procedures for taking logical backups and executing disaster recovery restores for the Elégance Interior Designer Platform PostgreSQL database.

---

## 2. Tooling & Prerequisites

- **Backup Binary**: `pg_dump` (PostgreSQL 15–18 client).
- **Restore Binary**: `pg_restore` and `psql`.
- **Scripts**:
  - [`scripts/backup-postgresql.ps1`](file:///c:/my%20projects/interior%20design/scripts/backup-postgresql.ps1)
  - [`scripts/restore-postgresql.ps1`](file:///c:/my%20projects/interior%20design/scripts/restore-postgresql.ps1)

> **CRITICAL SECURITY NOTE**: Never commit `.dump` files or plaintext database backups to Git. Backups must be stored in secure, encrypted object storage (e.g., private R2 or S3 bucket) with restricted IAM access.

---

## 3. Logical Backup Procedure

### 3.1. Standard Execution
```powershell
powershell -ExecutionPolicy Bypass -File scripts/backup-postgresql.ps1 `
    -Database "interior_design_dev" `
    -Port 5433 `
    -OutputDir "storage/backups"
```

### 3.2. Script Execution Details
- Uses PostgreSQL Custom Compressed format (`-Fc`).
- Omits owner and privilege statements (`--no-owner --no-acl`) for portability across environments.
- Automatically generates timestamped file: `storage/backups/backup_<db>_<yyyyMMdd_HHmmss>.dump`.
- Never displays or echoes database passwords in terminal logs.

---

## 4. Logical Restore Procedure

### 4.1. Destructive Disaster Recovery Restore
To restore into a fresh or recovered database:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/restore-postgresql.ps1 `
    -BackupFile "storage/backups/backup_interior_design_dev_20261015_100815.dump" `
    -Database "interior_design_dev" `
    -Port 5433 `
    -DropAndRecreate
```

### 4.2. Restore Guarantees
- Terminates dangling connections before dropping/recreating the target database.
- Restores all table schemas, constraints, indexes, sequences, and data rows.
- Re-establishes Row-Level Security (RLS) policies and forced RLS attributes on all 59 tenant tables.

---

## 5. Post-Restore Verification Checklist

After any restore operation, run the following verification checks:

1. **Schema Integrity**:
   Verify Flyway schema history has no checksum mismatches:
   ```powershell
   Push-Location apps/api
   .\mvnw.cmd test -Dtest=PostgreSqlBootstrapVerificationTest
   Pop-Location
   ```
2. **RLS Verification**:
   Verify all 59 tenant tables have `rowsecurity=true` and `forcerowsecurity=true`:
   ```powershell
   $env:PGPASSWORD="<password>"
   psql -h <host> -p <port> -U <user> -d <db> -f scripts/verify-cloud-db-rls.sql
   ```
3. **Application Health**:
   Start the application and verify Spring Boot Actuator returns `UP`:
   `GET /api/v1/actuator/health`

---

## 6. Engineering Metrics (Baseline Restore Drill)

Obtained during local destructive restore verification:
- **Backup Duration**: ~1.8 seconds.
- **Dump File Size**: ~349 KB (compressed custom format).
- **Restore Duration**: ~4.7 seconds.
- **Verification Duration**: < 1.0 second.

*Note*: These are baseline engineering metrics on local NVMe storage. Actual production backup/restore times will depend on total database size and cloud network throughput. Contractual RPO/RTO SLAs remain: **BUSINESS/OPERATIONS DECISION REQUIRED**.
