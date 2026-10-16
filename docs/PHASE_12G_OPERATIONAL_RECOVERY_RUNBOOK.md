# Elégance — Phase 12G: Operational Recovery Runbooks

**Document:** Standard Operating Procedures for Incident Response & Service Recovery  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Observability & Operational Readiness  

---

## 1. Database Outage

- **Symptom:** `DatabaseReadinessDown` alert triggers. Health check `/actuator/health/readiness` reports `DOWN`.
- **Diagnostic Steps:**
  1. Inspect PostgreSQL service status and logs (`systemctl status postgresql` or managed database console).
  2. Test network connectivity: `psql -h <host> -U postgres -d interior_design_dev -c "SELECT 1"`.
  3. Inspect HikariCP pool metrics in Actuator: `hikaricp.connections.active`, `hikaricp.connections.idle`.
- **Immediate Safe Mitigation:**
  - If database is restarting or under maintenance, enable reverse-proxy maintenance page (HTTP 503).
  - Check for runaway long-running queries: `SELECT pid, query, state, age(clock_timestamp(), query_start) FROM pg_stat_activity WHERE state != 'idle' ORDER BY age DESC;`.
- **When to Enter `READ_ONLY`:** If primary replica is damaged and a standby read replica is being promoted.
- **Recovery Verification:**
  - Verify `/actuator/health/readiness` returns `UP`.
  - Check `GET /api/v1/health` returns HTTP 200.

---

## 2. HTTP 5xx Surge

- **Symptom:** `HttpServer5xxSurge` alert triggers (>5% server errors).
- **Diagnostic Steps:**
  1. Inspect centralized JSON logs filtered by `level="ERROR"` and `service="interior-platform-api"`.
  2. Identify top error messages and normalized error codes (`interior.errors.normalized`).
  3. Trace failing requests using `requestId` from the error log.
- **Immediate Safe Mitigation:**
  - If a specific endpoint is crashing due to upstream failure (e.g. AI or Email), verify domain error handlers are returning safe 503 instead of unhandled 500.
  - If an upstream service is causing cascade failures, engage the respective kill-switch (`aiEnabled=false`, `uploadsEnabled=false`).
- **When to Enter `READ_ONLY`:** If mutating operations are generating data corruption or uncaught database exceptions.
- **Recovery Verification:**
  - 5xx error rate drops below 1% for 5 consecutive minutes.

---

## 3. Process Restart Loop

- **Symptom:** `ProcessRestartLoop` alert triggers (>3 JVM restarts within 15 minutes).
- **Diagnostic Steps:**
  1. Check JVM exit codes and container logs (`dmesg -T` or `kubectl logs --previous`).
  2. Inspect whether the kernel OOM killer terminated the process: search for `Out of memory: Kill process`.
  3. Review `ProductionConfigurationValidator` logs to ensure startup did not fail due to invalid production properties.
- **Immediate Safe Mitigation:**
  - If OOM: increase container memory ceiling or tune `-Xmx` / `-XX:MaxRAMPercentage`.
  - Ensure leak detection is active: inspect `spring.datasource.hikari.leak-detection-threshold`.
- **Recovery Verification:**
  - Process uptime exceeds 30 minutes without restarts.

---

## 4. ClamAV Outage

- **Symptom:** `ClamAvScannerUnavailable` alert triggers.
- **Diagnostic Steps:**
  1. Inspect ClamAV container/daemon: `clamdscan --version` or check TCP port 3310 connectivity.
  2. Inspect application log: search for `ClamAV malware scanner unreachable at clamav:3310`.
- **Immediate Safe Mitigation:**
  - Under `app.media.security.require-clean-scan=true` (fail-closed default), upload commits are automatically rejected safely without data loss.
  - If ClamAV daemon hung, restart the ClamAV service: `docker restart clamav` or system service restart.
  - Core platform reads, portfolios, and browsing remain 100% available.
- **When to Enter `READ_ONLY`:** Not required; only upload commits are affected while ClamAV is offline.
- **Recovery Verification:**
  - `/actuator/health` reports `MalwareScanner: REACHABLE` and `mediaUploadReady: true`.

---

## 5. Billing Webhook Anomaly

- **Symptom:** `BillingWebhookSignatureAttack` alert triggers (>10 signature failures in 5m).
- **Diagnostic Steps:**
  1. Inspect webhook logs for provider (`razorpay` or `stripe`).
  2. Verify that webhook secrets in configuration match the provider dashboard key.
  3. Check IP origins of failing webhook deliveries to differentiate provider misconfiguration from malicious replay attacks.
- **Immediate Safe Mitigation:**
  - If provider rolled keys, update secret via environment variable and restart gracefully.
  - Do not disable signature verification. Replay attacks are suppressed by `uq_billing_event_provider`.
- **Recovery Verification:**
  - Next authentic provider webhook successfully verifies with HTTP 200 `{"status":"PROCESSED"}`.

---

## 6. Storage Provider Outage

- **Symptom:** `StorageWriteFailureSustained` alert triggers.
- **Diagnostic Steps:**
  1. Inspect Cloudflare R2 / S3 endpoint accessibility and TLS certificate status.
  2. Check for R2 API rate limits or quota caps.
- **Immediate Safe Mitigation:**
  - Disable uploads temporarily via Admin Operations switch: `uploadsEnabled=false`.
  - Existing media served via CDN remains readable.
- **Recovery Verification:**
  - Perform test upload intent and commit; verify asset status transitions to `READY`.

---

## 7. Operational Mode Transition SOP (Entering & Exiting `READ_ONLY`)

### 7.1 When to Enter `READ_ONLY`
- Executing major offline database schema updates or maintenance.
- Suspected active database corruption or cascading mutation errors.
- External upstream identity/payment emergency requiring freeze of new records.

### 7.2 SOP to Activate `READ_ONLY`
1. Authenticate as Super Admin with MFA step-up verified.
2. Send API request:
   ```http
   PATCH /api/v1/admin/operations/state
   Content-Type: application/json

   {
     "maintenanceMode": "READ_ONLY",
     "maintenanceMessage": "Scheduled platform maintenance in progress. Browsing remains active.",
     "uploadsEnabled": false,
     "aiEnabled": false,
     "registrationsEnabled": false
   }
   ```
3. Verify `interior.operations.maintenance_mode` metric transitions to `1`.
4. Verify that mutating endpoints (`POST`, `PUT`, `PATCH`, `DELETE`) return HTTP 503 `MAINTENANCE_MODE_ACTIVE`.
5. Verify that public browsing (`/projects`, `/professionals`, `/health`) remains 100% operational.

### 7.3 SOP to Restore `NORMAL`
1. Send API request:
   ```http
   PATCH /api/v1/admin/operations/state
   Content-Type: application/json

   {
     "maintenanceMode": "NORMAL",
     "maintenanceMessage": null,
     "uploadsEnabled": true,
     "aiEnabled": true,
     "registrationsEnabled": true
   }
   ```
2. Verify `interior.operations.maintenance_mode` metric transitions to `0`.
3. Check audit log in `audit_events` confirms `ADMIN_OPERATIONAL_STATE_UPDATED`.
