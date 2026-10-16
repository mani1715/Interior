# Elégance — Phase 12G: Observability, Logging, Monitoring, Health & Operational Recovery Production Readiness

**Document:** Complete Platform Observability, Telemetry, Incident Controls & Verification Report  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Architecture & Production Readiness  
**Target Architecture Commit:** Verified on `main`  
**Current Flyway Migration Baseline:** `V040` (`V040__support_and_operations_foundation.sql`)  

---

## 1. Executive Summary & Production Readiness Decision

Phase 12G elevates the Elégance Interior Designer Platform to comprehensive operational production readiness without creating any external monitoring accounts, SaaS dependencies, or live cloud credentials.

### Key Capabilities Verified:
1. **Structured Application Logging:**
   - Production profile emits single-line, machine-parseable JSON formatted by zero-dependency `JsonLoggingLayout`.
   - Local development and test profiles preserve human-readable colorized console logs.
   - Bounded package log levels strictly disable SQL query dumps (`org.hibernate.SQL=WARN`) and bind parameter logging (`org.hibernate.orm.jdbc.bind=WARN`) in production.
2. **Correlation ID & MDC Thread Hygiene:**
   - Strict correlation ID parsing via `RequestCorrelationFilter`: valid `^[a-zA-Z0-9_-]{1,64}$` accepted; invalid or CRLF-injected values securely replaced with UUIDs.
   - Header propagation across `X-Request-Id` and `X-Request-ID`.
   - MDC propagation to asynchronous tasks (`submitAsyncJob` in `AiVisualizerService`, `CommunicationRetryScheduler`, `OperationalJobCoordinator`).
   - Strict `MDC.clear()` in `finally` blocks prevents context leakage across thread reuse.
3. **Log Injection & Secret Redaction Standard:**
   - Strips carriage returns and newlines from user-controlled strings, neutralizing log forging attacks.
   - Verified zero secret leakage against sentinel secrets: Bearer tokens, Session cookies, OIDC authorization codes, API keys, Webhook signatures, and Passwords.
   - Customer email PII masked to first character (`p***@domain.com`).
   - Log message lengths strictly bounded to 4096 characters to prevent memory exhaustion.
4. **Metrics Foundation & Cardinality Control:**
   - Spring Boot Actuator with Micrometer and Prometheus registry enabled (`/actuator/prometheus`).
   - Centralized `PlatformMetrics` records domain counters (`interior.media.uploads`, `interior.media.scans`, `interior.auth.events`, `interior.ai.generations`, `interior.email.deliveries`, `interior.billing.events`, `interior.jobs.executions`, `interior.errors.normalized`) and gauges (`interior.operations.maintenance_mode`).
   - Cardinality strictly bounded: zero user IDs, studio IDs, UUIDs, emails, or freeform error messages used as metric labels.
5. **Health Architecture (Liveness, Readiness & Optional Provider Isolation):**
   - Public health endpoint (`/health` and `/api/v1/health`) returns minimal `{ "status": "UP" }` without leaking infrastructure internals.
   - Distinct Liveness (`/actuator/health/liveness`) and Readiness (`/actuator/health/readiness`) probes.
   - Optional Provider Isolation Invariant: disabled/unconfigured services (AI, Email, Billing) report `UP` with `status: NOT_CONFIGURED`, never taking down core platform liveness or readiness.
6. **Actuator Exposure Hardening:**
   - Production profile restricts Actuator exposure strictly to `health,info,metrics,prometheus`.
   - `ProductionConfigurationValidator` enforces fail-closed rejection if dangerous endpoints (`env`, `beans`, `configprops`, `heapdump`, `threaddump`, `mappings`, `*`) are exposed.
7. **Failure Drills & Operational Runbooks:**
   - Validated local simulated drills: Database outage and recovery, ClamAV outage and recovery, Maintenance mode transition (`NORMAL` <-> `READ_ONLY`).
   - Comprehensive SOPs and runbooks created in `docs/PHASE_12G_OPERATIONAL_RECOVERY_RUNBOOK.md`.
8. **Frontend Error & Telemetry Observability:**
   - Audit revealed zero secret leakage across frontend console statements.
   - Next.js Global Error Boundary (`error.tsx`) surfaces safe Reference ID (`error.digest`) without leaking backend stack traces.
   - Client Web Vitals reporter (`<WebVitals />`) instrumented for Core Web Vitals (LCP, INP, CLS) without third-party data transmission.

---

## 2. Telemetry & Metrics Specification

### 2.1 Prometheus Metrics Endpoint
- **Path:** `/actuator/prometheus` (or `/api/v1/actuator/prometheus`)
- **Format:** OpenMetrics / Prometheus exposition text format
- **Access Policy:** Internal network / authenticated operational monitoring agent only (enforced via reverse-proxy ingress / VPC in Phase 15).

### 2.2 Domain Metric Inventory

| Metric Name | Type | Labels / Tags | Description |
|:---|:---:|:---|:---|
| `interior.media.uploads` | Counter | `operation` (intent, commit), `status` (success, failure, quota_exceeded) | Media asset upload intent and commit lifecycle |
| `interior.media.scans` | Counter | `scanner` (clamav, disabled, fake), `outcome` (clean, infected, unavailable, error) | Malware scanning outcomes on uploaded media |
| `interior.auth.events` | Counter | `type` (oidc_success, oidc_failure, mfa_stepup, session_rejected, rate_limited) | Authentication and session security events |
| `interior.ai.generations` | Counter | `provider` (disabled, configurable), `operation` (generate, edit), `status` (success, error, timeout) | AI visualizer operations and outcomes |
| `interior.email.deliveries` | Counter | `provider` (resend, ses, disabled), `status` (sent, failed, retry, not_configured) | Transactional operational email dispatches |
| `interior.billing.events` | Counter | `provider` (razorpay, stripe, disabled), `event_type` (webhook, checkout), `outcome` (success, signature_failed, replay_rejected) | Commercial billing and payment gateway events |
| `interior.jobs.executions` | Counter | `job_key` (stuck_ai, comm_retry, session_cleanup), `status` (succeeded, failed, skipped_locked) | Operational scheduled background jobs |
| `interior.errors.normalized` | Counter | `code` (validation_failed, unauthorized, access_denied, not_found, internal_error, etc.) | Normalized server error categories |
| `interior.operations.maintenance_mode` | Gauge | None | 0 = NORMAL operational mode, 1 = READ_ONLY maintenance mode |

---

## 3. Failure & Recovery Drills

| Drill Scenario | Failure Injected | Health Indicator State | API Client Behavior | Recovery Action | Verified State |
|:---|:---|:---|:---|:---|:---|
| **Database Outage** | PostgreSQL port 5432 unreachable | `DataSourceHealthIndicator`: DOWN; Provider Matrix `database: UNAVAILABLE` | Mutating & authenticated endpoints return safe HTTP 500 / 503 without stack traces | Restore PostgreSQL daemon / connection | `database: AVAILABLE`; Core readiness returns `UP` |
| **ClamAV Outage** | ClamAV port 3310 unreachable | `MalwareScannerHealthIndicator`: UNAVAILABLE, `mediaUploadReady: false` | Media upload commits rejected fail-closed; public browsing 100% available | Restore ClamAV daemon | `mediaUploadReady: true`, engine version detected |
| **Maintenance Mode Drill** | Super Admin sets `maintenanceMode = 'READ_ONLY'` | `PlatformOperationalStateHealthIndicator`: `maintenanceMode = READ_ONLY` | Mutating requests rejected with HTTP 503 `MAINTENANCE_MODE_ACTIVE`; public read active | Super Admin sets `maintenanceMode = 'NORMAL'` | Normal mutations re-enabled; audit event recorded; metric gauge returns to 0 |

---

## 4. Quality Gates & Test Execution

- **Dedicated Observability Test Suite (`ObservabilityAndResilienceTest`):**
  - **23 passed, 0 failed, 0 skipped** (100% PASS).
- **Total Observability & Incident Control Tests:**
  - **42 passed across 4 test classes**.
- **Frontend Regression Suite (`apps/web`):**
  - Vitest: **52/52 test files passed, 373/373 tests passed**.
  - TypeScript (`npx tsc --noEmit`): **Clean (Exit 0, 0 errors)**.
  - ESLint (`npm run lint`): **Clean (Exit 0, 0 warnings/errors)**.
  - Next.js Production Build (`npm run build`): **Compiled cleanly, 29/29 static routes prerendered**.
- **Live Integration Verification Harness:**
  - `scripts/verify-observability-live-integration.ps1`: **Executed cleanly (Exit 0)**.
