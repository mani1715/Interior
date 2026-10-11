# DISASTER RECOVERY RUNBOOK — ELÉGANCE INTERIOR DESIGNER PLATFORM

## 1. Objectives & Metrics

- **RPO (Recovery Point Objective)**: $\le 1$ hour (Point-in-Time Recovery from continuous PostgreSQL WAL logs and daily automated snapshots).
- **RTO (Recovery Time Objective)**: $\le 4$ hours (Full restore of application containers, database, and object storage connectivity).
- **Fail-Closed Default**: In the event of network partition or partial service degradation, data mutations fail closed to prevent split-brain corruption or insecure uploads.

---

## 2. Threat Scenarios & Remediation

### Scenario A: Corrupted Deployment / Breaking Application Regression
1. **Immediate Action**: Enable `READ_ONLY` mode via `POST /api/v1/admin/operations/state` (requires MFA step-up).
2. **Container Rollback**: Redeploy previous known-good release commit via deployment orchestration.
3. **Database Migration Rollback**: If a schema migration failed, evaluate forward-fix vs `flyway repair` using idempotent DDL.
4. **Smoke Test**: Verify `/api/v1/health` and perform read-only health checks.
5. **Restore Operational State**: Switch operational state back to `NORMAL`.

### Scenario B: Database Outage or Data Corruption
1. **Isolate Traffic**: Place platform into maintenance mode.
2. **PITR Restoration**: Restore PostgreSQL from the most recent continuous WAL segment immediately prior to the corrupting event.
3. **Schema Verification**: Validate migrations using `Flyway validate` to verify schema version consistency (V000–V040).
4. **Advisory Lock Flush**: Confirm all background coordinator locks (`operational_job_runs`) are released.
5. **Reconciliation Job**: Trigger `STORAGE_RECONCILIATION` and `AI_STUCK_RECOVERY` to synchronize external asset references with restored database records.

### Scenario C: Malicious File or Scanner Compromise
1. **Immediate Kill Switch**: Set `uploads_enabled = false` via Admin Operations API.
2. **Quarantine Audit**: Query `media_security_scans` for all files in `QUARANTINED` or `FAILED` status within the incident window.
3. **Purge Quarantined Storage**: Remove unconfirmed objects from isolated quarantine storage buckets.
4. **Engine / Rule Refresh**: Verify scanner definitions and update security checksum rules.
5. **Re-enable Uploads**: Set `uploads_enabled = true`.

---

## 3. Disaster Recovery Verification Checklist

- [ ] Database backup validated with test restoration at least quarterly.
- [ ] Flyway migration integrity checked against all schema versions (V000 through V040).
- [ ] Row-Level Security (RLS) policies verified active across all tenant-scoped tables.
- [ ] Admin step-up authentication operational and non-bypassable.
- [ ] Read-only maintenance mode prevents customer data writes with HTTP 503 response.
