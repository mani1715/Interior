# ELÉGANCE — INCIDENT RUNBOOK: DATABASE & MANAGED POSTGRESQL
## Managed PostgreSQL Incident Response Guide
**Target System:** Elégance Interior Designer Platform  
**Database Provider:** Neon Serverless PostgreSQL  
**Document Revision:** 1.0 (Phase 15A Release)  

---

## 1. Severity Classifications & Escalation Targets

| Severity | Definition | Impact | Target Triage Time |
| :--- | :--- | :--- | :--- |
| **P1 — Critical** | Database unreachable, connection pool completely exhausted, data corruption | Full platform outage | < 15 minutes |
| **P2 — Major** | Elevated latency (> 1000ms), partial connectivity loss, slow failover | High degradation | < 30 minutes |
| **P3 — Moderate**| Minor connection spikes, single-tenant lock contention, non-blocking errors | Localized impact | < 2 hours |

---

## 2. Common Scenarios & Resolution Playbooks

### 2.1 Scenario: Backend Connection Timeout (HikariCP Pool Exhaustion)
- **Symptom:** Logs show `Connection is not available, request timed out after 20000ms`.
- **Diagnostic Steps:**
  1. Check `/actuator/health` to determine if DB liveness check fails.
  2. Query active connections:
     ```sql
     SELECT count(*), state FROM pg_stat_activity GROUP BY state;
     ```
  3. Identify long-running transactions:
     ```sql
     SELECT pid, now() - xact_start AS duration, query 
     FROM pg_stat_activity 
     WHERE state != 'idle' ORDER BY duration DESC LIMIT 5;
     ```
- **Remediation:**
  1. Terminate offending blocking PID:
     ```sql
     SELECT pg_terminate_backend(<pid>);
     ```
  2. If all connections are consumed by rogue processes, restart the backend container to reinitialize HikariCP.

---

### 2.2 Scenario: Flyway Migration Lock / Stuck Migration
- **Symptom:** Application startup hangs on Flyway migration or fails with `Waiting for changelog lock`.
- **Diagnostic Steps:**
  1. Check `pg_locks` for active advisory locks:
     ```sql
     SELECT * FROM pg_locks WHERE locktype = 'advisory';
     ```
  2. Inspect the latest record in `flyway_schema_history`:
     ```sql
     SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;
     ```
- **Remediation:**
  1. If a migration process crashed and left a lock behind, identify the inactive backend PID and terminate it via `pg_terminate_backend(pid)`.
  2. If `flyway_schema_history` marks a migration as `success = false`:
     - Resolve the underlying SQL error.
     - Remove the failed row from `flyway_schema_history`:
       ```sql
       DELETE FROM flyway_schema_history WHERE version = '<version>' AND success = false;
       ```
     - Restart the application to re-run the migration.

---

### 2.3 Scenario: Neon Compute Wake-Up Latency / Cold Start
- **Symptom:** First request after idle period takes 1–3 seconds to respond.
- **Root Cause:** Neon compute auto-suspend (scale-to-zero) was triggered after an idle timeout period.
- **Diagnostic Steps:**
  1. Inspect Neon console metrics or `neon compute list`.
  2. Check backend log for initial Hikari connection acquisition delay.
- **Remediation:**
  - For production workloads requiring zero cold starts, adjust compute settings in Neon to set `suspend_timeout_seconds` to `0` (never suspend) or implement a health-check keepalive ping every 5 minutes.

---

### 2.4 Scenario: RLS Permission Denied / Cross-Tenant Access Violation
- **Symptom:** Query returns 0 rows unexpectedly or throws `permission denied for table ...`.
- **Diagnostic Steps:**
  1. Verify whether session variable `app.current_studio_id` is set:
     ```sql
     SELECT current_setting('app.current_studio_id', true);
     ```
  2. Verify that the table has RLS policies for the executing role:
     ```sql
     SELECT * FROM pg_policies WHERE tablename = '<table_name>';
     ```
- **Remediation:**
  - Ensure the application service layer explicitly executes `set_config('app.current_studio_id', studioId, true)` at the beginning of each tenant-scoped transaction.

---

## 3. Disaster Recovery & Emergency Contacts

- **Neon Status Page:** `https://status.neon.tech`
- **Manual Failover / Point-in-Time Restore:** Follow [docs/BACKUP_AND_RESTORE_MANUAL.md](file:///c:/my%20projects/interior%20design/docs/BACKUP_AND_RESTORE_MANUAL.md).
