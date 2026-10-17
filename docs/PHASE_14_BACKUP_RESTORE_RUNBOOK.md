# Phase 14 — Database Backup & Restore Runbook

> **Status:** APPROVED FOR PRODUCTION RELEASE CANDIDATE  
> **Platform:** Elégance Interior Designer Platform  
> **Database:** PostgreSQL 16+  
> **Storage Target:** Encrypted Object Storage / S3 / R2  

---

## 1. Overview & Recovery Objectives

A robust backup and disaster recovery process is critical for preserving multi-tenant interior designer portfolio projects, floor plans, client communications, contracts, and billing records.

### Service Level Objectives
- **Recovery Point Objective (RPO):** <= 1 hour (Continuous WAL archiving or hourly snapshots; daily full backup).
- **Recovery Time Objective (RTO):** <= 30 minutes for complete database recovery from snapshot.

---

## 2. Backup Strategy

| Backup Type | Frequency | Retention | Storage Location | Encryption |
|---|---|---|---|---|
| **Automated Snapshot** (Managed DB / RDS / Cloud) | Daily at 02:00 UTC | 30 Days | Managed Provider Storage | AES-256 |
| **Logical Dump** (`pg_dump`) | Every 6 hours | 14 Days | Off-site S3 / Cloudflare R2 | Client-side GPG / Server-side AES-256 |
| **Pre-Release Snapshot** | Immediately before any deployment | 7 Days | Local / Cloud backup bucket | AES-256 |
| **Write-Ahead Log (WAL) Archives** | Continuous | 7 Days | Object storage bucket | Provider default |

---

## 3. Backup Procedures

### 3.1 Pre-Release Manual Snapshot
Run immediately before applying new migrations or deploying new backend versions:

```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="backup_elegance_pre_release_${BACKUP_DATE}.dump"

echo "Creating pre-release database dump: ${BACKUP_FILE}..."

PGPASSWORD="${SPRING_DATASOURCE_PASSWORD}" pg_dump \
  -h "${DB_HOST:-localhost}" \
  -p "${DB_PORT:-5432}" \
  -U "${SPRING_DATASOURCE_USERNAME:-postgres}" \
  -d "${DB_NAME:-interior_platform_prod}" \
  -F c \
  -b \
  -v \
  -f "/backups/${BACKUP_FILE}"

echo "Database dump complete. Size: $(du -h /backups/${BACKUP_FILE} | cut -f1)"
```

### 3.2 Automated Scheduled Backup Script
```bash
#!/usr/bin/env bash
set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
ARCHIVE_NAME="elegance_db_${TIMESTAMP}.dump.gz"

pg_dump -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -F c \
  | gzip > "/tmp/${ARCHIVE_NAME}"

# Upload to secure object storage
aws s3 cp "/tmp/${ARCHIVE_NAME}" "s3://${BACKUP_BUCKET}/db-backups/${ARCHIVE_NAME}" \
  --sse aws:kms

rm -f "/tmp/${ARCHIVE_NAME}"
echo "Backup successfully uploaded: s3://${BACKUP_BUCKET}/db-backups/${ARCHIVE_NAME}"
```

---

## 4. Restore Procedures

### 4.1 Verification & Preparation
1. Identify the target backup archive to restore.
2. Ensure no active writes are occurring by stopping the backend API containers:
   ```bash
   docker stop elegance-api
   ```
3. Terminate active database sessions to avoid lock conflicts during restore:
   ```sql
   SELECT pg_terminate_backend(pid) 
   FROM pg_stat_activity 
   WHERE datname = 'interior_platform_prod' AND pid <> pg_backend_pid();
   ```

### 4.2 Restoring from Custom-Format Dump (`pg_restore`)
```bash
#!/usr/bin/env bash
set -euo pipefail

RESTORE_FILE="$1" # Path to .dump file

if [ -z "$RESTORE_FILE" ]; then
  echo "Usage: ./restore.sh <path-to-dump-file>"
  exit 1
fi

echo "Restoring database from: $RESTORE_FILE..."

# Option A: Clean restore into existing database
PGPASSWORD="${SPRING_DATASOURCE_PASSWORD}" pg_restore \
  -h "${DB_HOST:-localhost}" \
  -p "${DB_PORT:-5432}" \
  -U "${SPRING_DATASOURCE_USERNAME:-postgres}" \
  -d "${DB_NAME:-interior_platform_prod}" \
  --clean \
  --if-exists \
  --no-owner \
  --no-acl \
  -v \
  "$RESTORE_FILE"

echo "Database restoration completed."
```

### 4.3 Post-Restore Verification
1. Verify database connectivity:
   ```sql
   SELECT count(*) FROM flyway_schema_history;
   SELECT count(*) FROM users;
   SELECT count(*) FROM studios;
   SELECT count(*) FROM projects;
   ```
2. Verify Row-Level Security (RLS) is intact:
   ```sql
   SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = true;
   ```
3. Start backend API container:
   ```bash
   docker start elegance-api
   ```
4. Check health endpoint:
   ```bash
   curl -f http://localhost:8080/actuator/health/readiness
   ```
5. Confirm application responds `{"status":"UP"}`.

---

## 5. Backup Testing & Drills

- **Quarterly Drill:** The engineering team executes a blind restore into an isolated staging environment once every 3 months.
- **Automated Check:** Integrity tests verify that backups are non-empty and uncorrupted upon creation.
