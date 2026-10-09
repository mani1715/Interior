# Phase 7E — Admin + Moderation + Audit + Operational Controls Documentation

## 1. Overview & Trust Domain Segregation
In Elégance, platform administration and tenant studio membership are completely isolated trust domains:
- **Platform Roles (`SUPER_ADMIN`, `ADMIN`, `MODERATOR`)**:
  - Bound globally to the authenticated user identity via `ActorContext.roles()`.
  - Authorized strictly for control plane endpoints under `/admin/**`.
  - **Zero automatic tenant privileges:** Platform admins cannot read or mutate tenant resources on studio-scoped routes (`/workspace/**`, `/api/projects/**`, etc.) unless acting as an enrolled studio member or via explicitly audited administrative operational endpoints.
- **Tenant Roles (`OWNER`, `STUDIO_ADMIN`, `DESIGNER`, etc.)**:
  - Scoped strictly to the specific `studioId`.
  - Have zero access to `/admin/**` endpoints, guarded by `AuthorizationService.requirePlatformRole(...)` and `SecurityInterceptor`.

## 2. Global Administrative Roles & Boundaries
- **`SUPER_ADMIN`**:
  - Highest privilege tier.
  - Required for:
    - User platform role modifications (`PATCH /admin/users/{userId}/role`).
    - Emergency user session revocations across all devices (`POST /admin/users/{userId}/revoke-sessions`).
    - Tenant subscription plan overrides (`PATCH /admin/studios/{studioId}/plan`).
  - **Self-Protection Invariants**:
    - A Super Admin cannot demote or remove their own Super Admin role.
    - The platform protects the last remaining active Super Admin from demotion or suspension.
- **`ADMIN`**:
  - Standard platform operations.
  - Operations:
    - User account suspension/reactivation (`PATCH /admin/users/{userId}/status`).
    - Studio suspension/reactivation (`PATCH /admin/studios/{studioId}/status`).
    - Studio verification decisions (`POST /admin/verification/{verificationId}/approve`, `reject`).
    - Immutable audit log inspection (`GET /admin/audit`).
    - Operational diagnostics, health overview, communication delivery audits, storage reconciliation, and stuck AI job reconciliation.
  - **Invariants**:
    - Admins cannot suspend themselves.
    - Admins cannot modify user roles (requires `SUPER_ADMIN`).
- **`MODERATOR`**:
  - Least-privilege trust boundary focused solely on user-generated content integrity.
  - Operations:
    - Review moderation (`PATCH /admin/reviews/{reviewId}/status`).
    - Studio project content moderation (`PATCH /admin/projects/{projectId}/moderation`).
  - Excluded from studio suspensions, user status/role modifications, and system operational controls.

## 3. Operational Features & Diagnostics
- **User Operations**:
  - Full details lookup, status transitions (`ACTIVE`, `SUSPENDED`), and role assignments.
  - Upon suspension, all existing user sessions are immediately invalidated and an audit event (`USER_SUSPENDED`) is written.
- **Studio Operations**:
  - Studio status updates (`ACTIVE`, `SUSPENDED`).
  - When a studio is suspended:
    - All studio public discovery profiles are excluded (`s.status = 'ACTIVE'` required in SEO & Discovery).
    - Public read RLS suppresses all associated projects from discovery queries.
- **Content Moderation**:
  - Studio projects have `moderation_status` (`APPROVED`, `FLAGGED`, `HIDDEN`) and `moderation_reason`.
  - PostgreSQL 18 Row Level Security policy `public_read_studio_projects` enforces `AND moderation_status = 'APPROVED'`. Any moderated project (`HIDDEN`) is completely inaccessible to anonymous or unauthenticated public requests at the database engine level.
  - Review moderation requires an explicit `removalReason` when rejecting or removing reviews.
- **Operational Health & Diagnostics**:
  - System diagnostics expose database connectivity status, Flyway schema migration status (`v033`), active user sessions, failed deliveries (last 24 hours), quarantine upload backlog, and failed/stuck AI jobs.
  - Communication deliveries are audited with recipient masking (`jo***@example.com` or `+91 98*****321`) to prevent PII leakage.
  - Unconfigured transactional email providers reject retry attempts truthfully with `503 Service Unavailable`, preventing silent retry failure illusions.
  - Stuck AI jobs (> 30 minutes in generating state) can be reconciled safely back to `FAILED`.

## 4. Immutable Audit Logging
- Every administrative state change writes an immutable audit record to `audit_events` with:
  - `actor_id`: User ID of the administrator.
  - `action`: Canonical action name (`USER_STATUS_UPDATE`, `USER_ROLE_UPDATE`, `STUDIO_STATUS_UPDATE`, `PROJECT_MODERATION`, `STUDIO_PLAN_OVERRIDE`, etc.).
  - `resource_type` and `resource_id`.
  - `details`: JSON payload recording previous/new values and mandatory operational reasons.
- Optimized for performance via indexes on `timestamp DESC` and `action`.

## 5. Security Invariants for Production Readiness
- **Production Gaps & Next Steps**:
  - Mandatory Multi-Factor Authentication (MFA / WebAuthn) for `SUPER_ADMIN` and `ADMIN` logins.
  - Centralized write-once immutable audit archiving (e.g. AWS CloudWatch / S3 Object Lock).
  - Production monitoring, alerting triggers on consecutive admin failures, and automated rate limiting on administrative mutations.
