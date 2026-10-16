# Elégance — Phase 12G: Live Monitoring Activation Runbook

**Document:** Step-by-Step Production Activation Guide for Future Telemetry & Monitoring  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Observability & Operational Readiness  

---

## 1. Activation Prerequisites (Phase 15 Pre-Flight)

Before activating live monitoring:
1. Target cloud hosting environment provisioned (Phase 15).
2. Operational monitoring provider selected from `docs/PHASE_12G_MONITORING_PROVIDER_MATRIX.md`.
3. Staging deployment operational and connected to staging database and storage.

---

## 2. Step-by-Step Activation Procedure

### Step 1: Server-Side Environment Variable Configuration
Set the following environment variables in your secure hosting secrets manager (e.g. Railway / Render / AWS Parameter Store):

```env
# OpenTelemetry / Prometheus Exporter (Enable when OTel Collector / Grafana Agent is live)
OTEL_METRICS_EXPORT_ENABLED=true
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp-gateway.production:4318/v1/metrics
OTEL_EXPORTER_OTLP_TRACING_ENDPOINT=https://otlp-gateway.production:4318/v1/traces

# Actuator Exposure (Maintain strict allowlist: NEVER expose env, beans, or heapdump)
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus

# Production Logging Levels (Enforce info defaults and zero SQL bind logging)
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_INTERIOR_PLATFORM=INFO
LOGGING_LEVEL_ORG_HIBERNATE_SQL=WARN
LOGGING_LEVEL_ORG_HIBERNATE_ORM_JDBC_BIND=WARN
```

### Step 2: Frontend Exception Tracking Configuration (Optional Sentry Activation)
If Sentry is selected for frontend error tracking:
```env
NEXT_PUBLIC_SENTRY_DSN=https://<public_key>@sentry.io/<project_id>
SENTRY_AUTH_TOKEN=<build_upload_token_only>
```

### Step 3: Run Live Integration Verification Harness
Execute the non-credentialed verification harness against your staging deployment:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-observability-live-integration.ps1 -BaseUrl "https://staging.elegance.internal/api/v1" -ActuatorBaseUrl "https://staging.elegance.internal/actuator"
```
Verify that all checks return `PASS`:
- Minimal public health (`/health` returns `UP` with zero host details).
- Liveness probe (`/actuator/health/liveness` returns `UP`).
- Readiness probe (`/actuator/health/readiness` returns `UP`).
- Correlation ID propagation (`X-Request-Id` echoed).
- Safe error handling (404/500 returns normalized JSON without stack trace).

### Step 4: Uptime Monitor & Synthetic Health Probes
Configure external uptime monitoring (e.g. Better Stack or Cloudflare Health Checks):
- **Target URL:** `https://<domain>/api/v1/health`
- **Expected Status:** HTTP 200
- **Expected Body Contains:** `"status":"UP"`
- **Interval:** 60 seconds
- **Locations:** Global / AP-South (India)

### Step 5: Incident Routing & Alert Destinations
1. Connect alert webhooks to operational communications:
   - Critical P1 alerts: On-call SMS / PagerDuty.
   - High P2 / Warning P3 alerts: Slack channel `#platform-alerts`.
2. Send test alert to confirm alert deduplication and delivery.
3. Sign off on Phase 15 Monitoring Activation.
