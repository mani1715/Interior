# PRODUCTION OPERATIONS AND SUPPORT RUNBOOK — ELÉGANCE

## 1. Overview & Architecture

This runbook defines operational incident response, support ticketing triage, platform maintenance controls, and background job coordination for the Elégance Interior Designer Platform.

### Core Guiding Principles:
- **No Direct Mutation in Production**: No raw ad-hoc SQL updates to operational records without transactional migrations or auditable procedures.
- **Fail-Closed Security**: When operations or checks fail, permissions default to denying mutating actions.
- **Support Privacy**: Support messages separate customer correspondence from internal staff investigation notes (`is_internal = true`).
- **Audit Logging**: Every operational toggle, kill switch change, maintenance window, and ticket status transition generates immutable audit logs in `admin_audit_logs`.

---

## 2. Customer Support Workflow & Triage

Support requests are managed via `/api/v1/support/requests` (end users) and `/api/v1/admin/support/requests` (platform administrators).

### 2.1 Ticket Lifecycle & State Machine
- `OPEN`: Newly submitted ticket awaiting triage.
- `IN_PROGRESS`: Ticket assigned or under investigation by platform staff.
- `WAITING_ON_CUSTOMER`: Staff has replied and is waiting for additional user clarification.
- `RESOLVED`: Issue addressed and verified.
- `CLOSED`: Terminal ticket state.

### 2.2 Rate Limiting & Protection
- Max **5 support requests per user per hour**.
- Max **30 messages per user per minute** across active tickets.
- File attachments, live chat sessions, screen sharing, and user impersonation are **strictly forbidden** to eliminate attack surfaces and unauthorized data exfiltration.

### 2.3 Internal Notes vs User Replies
- Ordinary users only see messages where `is_internal = false`.
- Platform admins can append private investigation notes (`is_internal = true`) that remain hidden from customers and are excluded from GDPR user data exports (`support_requests.json`).
- Adding an administrative customer-facing reply (`is_internal = false`) automatically dispatches an in-app notification (`NOTIFICATION_INBOX`) to the user.

---

## 3. Platform Operational State & Maintenance Mode

Operational controls are persisted in `platform_operational_state` and exposed via `/api/v1/admin/operations/state`.

### 3.1 Operational Modes
- `NORMAL`: All normal platform capabilities are available.
- `READ_ONLY`: Maintenance mode active.
  - All mutating requests (POST, PUT, PATCH, DELETE) are intercepted by `MaintenanceModeInterceptor`.
  - Non-admin mutating requests return **HTTP 503 Service Unavailable** with JSON payload:
    ```json
    {
      "code": "MAINTENANCE_MODE_ACTIVE",
      "message": "Platform is currently undergoing scheduled maintenance. Please try again shortly."
    }
    ```
  - Platform administrators with super admin privileges and a fresh MFA step-up session are exempt, allowing operational remediation.
  - Safe read-only requests (GET, HEAD, OPTIONS) continue to function.

### 3.2 Granular Kill Switches
- `uploads_enabled`: When set to `false`, new media upload intent creation (`POST /api/v1/media/upload-intent`) is immediately halted.
- `ai_enabled`: When set to `false`, AI generation routines (`POST /api/v1/ai/...`) are stopped, returning HTTP 503.
- `registrations_enabled`: When set to `false`, new user registration/onboarding flows are blocked.

### 3.3 Access Requirements
Modifying platform operational state requires:
1. `SUPER_ADMIN` role or platform admin authority.
2. Verified multi-factor authentication (MFA) step-up token valid within the last 15 minutes.
3. Explicit mandatory operational reason recorded in the audit trail.

---

## 4. Background Job Observability & Coordination

Automated platform cleanup and retry routines are coordinated via `operational_job_runs` with PostgreSQL advisory locking to prevent concurrency collisions across multi-node instances.

### 4.1 Coordinated Jobs
1. `SESSION_CLEANUP`: Purges expired user sessions and invalidated refresh tokens.
2. `UPLOAD_EXPIRY`: Cleans up expired media upload intents and cleans quarantined unconfirmed uploads.
3. `STORAGE_RECONCILIATION`: Verifies orphaned object storage references against the database ledger.
4. `COMMUNICATION_RETRY`: Retries backoff queues for transactional email and in-app notifications.
5. `AI_STUCK_RECOVERY`: Reclaims or fails AI generation tasks exceeding SLA time limits.
6. `PRIVACY_CLEANUP`: Purges expired privacy exports and completes anonymization sweeps.

### 4.2 Manual Trigger & Diagnostic API
Admins can inspect job status or trigger runs:
- `GET /api/v1/admin/operations/jobs`: Returns history and last execution metrics.
- `POST /api/v1/admin/operations/jobs/{jobKey}/run`: Manually dispatches a single job run under advisory lock.

---

## 5. Request Tracing & Correlation

Every incoming HTTP request is intercepted by `RequestCorrelationFilter`:
- Validates or generates `X-Request-Id` and `X-Correlation-Id` (sanitized alphanumeric/UUID regex `^[a-zA-Z0-9_-]{1,64}$`).
- Automatically binds correlation identifiers to SLF4J MDC (`requestId`, `correlationId`).
- Propagates headers in all downstream HTTP responses.
- Enables end-to-end log correlation during triage.
