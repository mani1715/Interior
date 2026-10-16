# Elégance — Phase 12G: Alert Catalog & Telemetry Thresholds

**Document:** Platform Alert Definitions, Severity Hierarchy & Notification Rules  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Observability & Operational Readiness  

---

## 1. Alert Philosophy

1. **Actionability:** Every alert must correspond directly to an actionable response runbook. If an alert does not require engineering action, it must be demoted or removed.
2. **Noise Suppression:** Alerts must use grouping, evaluation windows, and hysteresis (cooldown) to prevent alert floods during incident recovery.
3. **Threshold Status:** All numeric thresholds are **INITIAL ENGINEERING THRESHOLDS — TUNE AFTER REAL TRAFFIC**. No contractual SLA is implied.
4. **Provider-Neutral Definitions:** Alert logic is expressed as mathematical PromQL/metric rules independent of any commercial SaaS vendor.

---

## 2. Alert Definitions by Severity

### 2.1 P1 / CRITICAL (Immediate Pager / Operational Escalation)

| Alert Name | Metric Expression / Condition | Duration Window | Initial Threshold | Description | Runbook Reference |
|:---|:---|:---:|:---:|:---|:---|
| **`DatabaseReadinessDown`** | `up{job="interior-platform-api"} == 1 and spring_boot_health_status{component="db"} == 0` | 1 minute | Sustained DOWN | PostgreSQL database connectivity lost. Core API unable to process requests. | [Runbook: DB Outage](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#1-database-outage) |
| **`HttpServer5xxSurge`** | `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05` | 5 minutes | > 5% 5xx rate | Server-side error rate exceeds 5% of total request traffic. | [Runbook: 5xx Surge](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#2-http-5xx-surge) |
| **`ProcessRestartLoop`** | `resets(process_uptime_seconds[15m]) > 3` | 15 minutes | > 3 restarts | JVM container restarting repeatedly due to OOM or startup failure. | [Runbook: Crash Loop](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#3-process-restart-loop) |
| **`ClamAvScannerUnavailable`** | `interior_media_scans_total{outcome="unavailable"} > 5 and app_media_security_require_clean_scan == 1` | 3 minutes | > 5 failures | ClamAV daemon is unreachable while clean scanning is strictly required; media upload commits blocked fail-closed. | [Runbook: ClamAV Outage](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#4-clamav-outage) |
| **`BillingWebhookSignatureAttack`** | `rate(interior_billing_events_total{outcome="signature_failed"}[5m]) > 2` | 5 minutes | > 10 failures / 5m | Surge in invalid webhook signatures detected. Possible forged webhook attack or corrupted gateway secret. | [Runbook: Billing Signatures](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#5-billing-webhook-anomaly) |
| **`StorageWriteFailureSustained`** | `rate(interior_media_uploads_total{operation="commit", status="failure"}[5m]) > 0.1` | 5 minutes | > 5 consecutive failures | S3/R2 storage adapter failing write operations. Media persistence interrupted. | [Runbook: Storage Failure](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#6-storage-provider-outage) |

---

### 2.2 P2 / HIGH (Same-Day Operational Investigation)

| Alert Name | Metric Expression / Condition | Duration Window | Initial Threshold | Description | Runbook Reference |
|:---|:---|:---:|:---:|:---|:---|
| **`DatabasePoolSaturation`** | `hikaricp_connections_active / hikaricp_connections_max > 0.80` | 3 minutes | > 80% pool usage | HikariCP database connection pool is approaching exhaustion. Potential connection leak or slow transaction. | [Runbook: DB Pool](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#7-database-pool-exhaustion) |
| **`HttpLatencySpike`** | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 2.0` | 5 minutes | p95 > 2000ms | 95th percentile HTTP response latency exceeds 2 seconds across routes. | [Runbook: Latency](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#8-high-latency) |
| **`AiGenerationFailureElevated`** | `rate(interior_ai_generations_total{status="error"}[15m]) / rate(interior_ai_generations_total[15m]) > 0.20` | 15 minutes | > 20% failure rate | Upstream AI generation provider experiencing elevated failure rate. | [Runbook: AI Failure](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#9-ai-provider-failure) |
| **`EmailDeliveryFailureElevated`** | `rate(interior_email_deliveries_total{status="failed"}[15m]) / rate(interior_email_deliveries_total[15m]) > 0.10` | 15 minutes | > 10% failure rate | Transactional email provider rejecting dispatches. | [Runbook: Email Failure](file:///c:/my%20projects/interior%20design/docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md#10-email-delivery-failure) |

---

### 2.3 P3 / WARNING (Informational / Operational Signal)

| Alert Name | Metric Expression / Condition | Duration Window | Initial Threshold | Description |
|:---|:---|:---:|:---:|:---|
| **`RateLimitSurge`** | `rate(interior_errors_normalized_total{code="too_many_requests"}[5m]) > 5` | 5 minutes | > 25 rejections / 5m | Traffic exceeding rate limits on public enquiries or search endpoints. |
| **`MaintenanceModeActive`** | `interior_operations_maintenance_mode == 1` | 0 minutes | Immediate | Platform is currently placed in `READ_ONLY` mode by a Super Admin. |
| **`JobLockSkippedSurge`** | `rate(interior_jobs_executions_total{status="skipped_locked"}[15m]) > 1` | 15 minutes | Sustained lock skip | Multiple instances competing for scheduled job execution locks. |

---

## 3. Deduplication & Alert Delivery Status

- **Grouping:** Alerts grouped by `alertname` and `environment`.
- **Cooldown:** Default 15-minute repeat interval for unresolved alerts; immediate recovery notification when conditions normalize.
- **Delivery Destination Status:** `RULE DEFINITIONS COMPLETE; LIVE NOTIFICATION PROVIDER (PagerDuty/Slack/Email) DEFERRED TO PHASE 15 HOSTING PROVISIONING`.
