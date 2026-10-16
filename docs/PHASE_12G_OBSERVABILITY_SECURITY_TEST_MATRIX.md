# Elégance — Phase 12G: Observability, Security & Verification Test Matrix

**Document:** Comprehensive Observability, Telemetry, Health, and Resilience Test Matrix  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Observability & Operational Readiness  

---

## 1. Test Suite Coverage Summary

| Test Class | Focus Area | Test Count | Status |
|:---|:---|:---:|:---:|
| `ObservabilityAndResilienceTest` | Structured JSON logging, secret sentinel redaction, email PII masking, log injection defense, correlation ID generation & MDC thread cleanup, bounded metric cardinality, health indicators & minimal public probe, actuator security validation, database / ClamAV / maintenance mode recovery drills | 23 | **PASSED** |
| `ProductionHardeningTest` | Production profile validator, secure session cookies, HSTS headers, ClamAV fail-closed validation | 8 | **PASSED** |
| `PlatformOperationsServiceTest` | Operational incident controls, maintenance mode transitions, audit events, provider matrix | 7 | **PASSED** |
| `MaintenanceModeInterceptorTest` | HTTP 503 fail-closed rejection during READ_ONLY, super admin exemption on control plane | 4 | **PASSED** |
| **Total Automated Observability / Resilience Tests** | | **42** | **100% PASSED** |

---

## 2. Detailed Test Cases & Invariants Tested (`ObservabilityAndResilienceTest`)

### 2.1 Request Correlation & MDC Thread Hygiene
- **`testValidCorrelationIdAccepted`:** Asserts that valid bounded correlation IDs (`^[a-zA-Z0-9_-]{1,64}$`) in `X-Request-Id` are accepted, set in MDC, echoed in response headers (`X-Request-Id` and `X-Request-ID`), and that MDC is 100% cleared upon completion.
- **`testInvalidCorrelationIdReplacedWithUuid`:** Evaluates malicious correlation headers containing CRLF characters and SQL injection attempts (`malicious\r\nCRLF;DROP TABLE`). Asserts that the invalid string is discarded and replaced with a secure random UUID.
- **`testQueryStringSanitizationInAccessLogs`:** Evaluates request access logging with sensitive query parameters (`token`, `password`, `signature`, `code`). Proves that query parameters are redacted (`token=[REDACTED]`) before appearing in logs.
- **`testMdcCleanedUpOnException`:** Verifies that when downstream controllers or filters throw an unhandled exception, `RequestCorrelationFilter` executes `finally` block and completely clears MDC, preventing context leakage across thread reuse.

### 2.2 Structured JSON Logging, Log Injection & Secret Redaction
- **`testLogInjectionNeutralization`:** Evaluates user-controlled strings containing `\r\n` characters attempting to forge additional log lines. Proves `JsonLoggingLayout` strips newlines into single spaces, preserving JSON structure.
- **`testSentinelSecretsRedaction`:** Evaluates synthetic logs containing sentinel credentials:
  - `Authorization: Bearer SECRET_SENTINEL_123`
  - `Cookie session=SECRET_SESSION_123`
  - `code=SECRET_CODE_123`
  - `api-key=SECRET_API_123`
  - `signature=SECRET_SIGNED_123`
  - `password=SECRET_PASS_999`
  Asserts that none of these sentinel values appear in formatted logs (all replaced with `[REDACTED]`).
- **`testEmailPiiMasking`:** Proves that customer email addresses (`private.person@example.invalid`) are masked (`p***@example.invalid`).
- **`testMessageLengthBounding`:** Verifies that 10,000-character payloads are safely truncated to 4096 characters to prevent log buffer exhaustion.
- **`testJsonLayoutStructure`:** Confirms production output adheres to valid single-line JSON format with ISO-8601 UTC timestamp, service name, environment, level, thread, logger, and correlation ID.

### 2.3 Metric Cardinality & Domain Metrics
- **`testPlatformMetricsBoundedCardinality`:** Asserts that metric tags use normalized, bounded categories (`operation`, `status`, `scanner`, `outcome`, `type`, `provider`, `event_type`, `job_key`, `code`). Proves user IDs, studio IDs, UUIDs, and emails are never used as labels.
- **`testMaintenanceModeGaugeToggle`:** Proves `interior.operations.maintenance_mode` gauge accurately toggles between `0` (NORMAL) and `1` (READ_ONLY).

### 2.4 Health Architecture & Optional Provider Isolation
- **`testPublicHealthControllerMinimalResponse`:** Proves `/health` (`/api/v1/health`) returns minimal `{ "status": "UP", "timestamp": "..." }` and contains ZERO internal hostnames, database versions, or scanner ports.
- **`testStorageHealthIndicatorSafe`:** Proves `StorageHealthIndicator` reports `AVAILABLE` without exposing bucket names or S3 endpoints.
- **`testTransactionalEmailHealthIndicatorDisabled`:** Proves that when email is disabled by design, health returns `UP` with `status: NOT_CONFIGURED`, never failing application health.
- **`testAiProviderHealthIndicatorDisabled`:** Proves that when AI visualizer is disabled or unconfigured, health returns `UP` with `status: NOT_CONFIGURED`.
- **`testBillingHealthIndicatorDisabled`:** Proves that when commercial billing is disabled, health returns `UP` with `status: NOT_CONFIGURED`.
- **`testPlatformOperationalStateHealthIndicator`:** Proves incident control indicator surfaces maintenance mode and kill-switch states.

### 2.5 Actuator Security & Production Configuration Validator
- **`testProductionValidatorPermitsSafeActuatorEndpoints`:** Proves that exposing approved endpoints (`health,info,metrics,prometheus`) passes production validation.
- **`testProductionValidatorBlocksDangerousActuatorEndpoints`:** Proves that attempting to expose dangerous endpoints (`env`, `beans`, `configprops`, `heapdump`, `threaddump`, `mappings`, `*`) throws `IllegalStateException`, terminating startup fail-closed.

### 2.6 Failure & Operational Recovery Drills
- **`testDatabaseOutageAndRecoveryDrill`:**
  - *Outage:* Simulated PostgreSQL connection failure causes provider matrix to report `database: UNAVAILABLE`.
  - *Recovery:* Restoring query execution restores `database: AVAILABLE` with zero state corruption.
- **`testClamAvOutageAndRecoveryDrill`:**
  - *Outage:* Simulated ClamAV daemon downtime causes `mediaUploadReady: false`, blocking upload commits fail-closed while platform liveness remains `UP`.
  - *Recovery:* Daemon availability restores `mediaUploadReady: true` with version detection.
- **`testMaintenanceModeTransitionDrill`:**
  - Proves transition to `READ_ONLY` updates metric gauge to `1`, disables mutations, and records `ADMIN_OPERATIONAL_STATE_UPDATED` in `audit_events`.
  - Restoring `NORMAL` resets metric gauge to `0`.
- **`testOperationalJobCoordinatorLockingAndMetrics`:**
  - Proves advisory locking coordinates job runs, records execution metrics, and releases locks on failure.
