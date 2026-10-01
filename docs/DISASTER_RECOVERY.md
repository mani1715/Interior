# Disaster Recovery & Backup Plan

## Overview

This document outlines the backup, restoration, and business continuity strategy for the Interior Design Platform.

---

## 1. Database Backup & Retention

### Primary Store: PostgreSQL 16+
- **Backup Method**: Continuous Archiving with Write-Ahead Logging (WAL) + Daily Full Physical Backups (e.g. pgBackRest or AWS RDS Automated Snapshots).
- **RPO (Recovery Point Objective)**: < 15 minutes.
- **RTO (Recovery Time Objective)**: < 60 minutes.

### Backup Schedule:
- **Daily Physical Snapshot**: Executed daily at 02:00 UTC during off-peak hours.
- **WAL Archiving**: Continuously streamed to immutable object storage (AWS S3 Glacier or Cloudflare R2).
- **Retention Window**:
  - Daily snapshots retained for 30 days.
  - Weekly snapshots retained for 12 weeks.
  - Monthly snapshots retained for 12 months.

### Logical Backup Command (Ad-Hoc / Pre-Deployment)
```bash
pg_dump -h <host> -U <user> -Fc -d interiordb -f "backup_interiordb_$(date +%Y%m%d_%H%M%S).dump"
```

---

## 2. Asset & Object Storage Backup

- Media files (portfolio imagery, verification PDF documents, attachments) are stored with unique UUIDs.
- Object storage bucket versioning must be enabled (`BucketVersioningConfiguration: Enabled`).
- Cross-region replication (CRR) should replicate media to a secondary fallback region.

---

## 3. Database Restoration Procedure

1. **Stop Application Backend**: Scale `platform-api` instances to 0 to prevent incoming writes.
2. **Provision Target Instance**: Provision fresh PostgreSQL instance or drop/recreate target database `interiordb`.
3. **Restore Logical / Physical Dump**:
   ```bash
   pg_restore -h <host> -U <user> -d interiordb --clean --if-exists "backup_interiordb_<timestamp>.dump"
   ```
4. **Verify Flyway Schema History**:
   ```sql
   SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   ```
5. **Verify Row-Level Security State**:
   ```sql
   SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('designer_studios', 'projects', 'studio_reviews');
   ```
6. **Start Application Backend**: Launch Spring Boot container; inspect startup logs for clean connection and Flyway validation.

---

## 4. Emergency Contacts & Escalation Runbook

1. Identify incident severity (Sev-1: data corruption/outage; Sev-2: degraded functionality).
2. Declare operational incident in engineering channels.
3. Switch DNS to maintenance page if total database failure occurs.
4. Execute restoration steps from latest verified snapshot.
5. Perform post-mortem and update this document.
