# PRIVACY, DATA EXPORT, ACCOUNT CLOSURE, AND ANONYMIZATION ARCHITECTURE
## Phase 9E Technical Specification & Implementation Guide

### 1. Executive Summary
Phase 9E productionizes data subject rights, self-service export, account/studio closure, safe anonymization, and legal compliance holds across the Elégance platform.

This implementation satisfies:
1. **Self-Service User Data Export**: Generates a standardized ZIP containing machine-readable JSON files (`profile.json`, `collections.json`, `manifest.json`) and CSV logs (`customer_inquiries.csv`).
2. **Self-Service Studio Data Export**: Available to studio administrators (`DESIGNER_ADMIN`), providing `studio_profile.json`, `projects.json`, and `team_members.csv`.
3. **Sole-Administrator Studio Ownership Blocker**: Invariant preventing account closure or deletion if the identity is the sole admin of an active studio (`ACCOUNT_CLOSURE_BLOCKED_BY_STUDIO_OWNERSHIP`).
4. **Legal Hold Framework**: Privileged administrative mechanism preventing destructive deletion or closure during active litigation, compliance inquiries, or financial disputes (`privacy_legal_holds`).
5. **Secure Object Storage Lifecycle & Redaction**: Strict stripping of auth tokens, password/MFA secrets, and internal moderator notes before packaging. CSV injection protection ensures safe viewing in desktop spreadsheet editors.
6. **Accounting & Audit Ledger Immutability**: Financial transactions (`billing_transactions`), cost ledgers (`ai_cost_ledger`), and audit logs (`audit_events`) remain intact in pseudonymized state to preserve legal records.

---

### 2. Database Schema (Flyway V038)
Flyway migration `V038__privacy_data_export_and_deletion.sql` introduces three relational tables with PostgreSQL 18 Row Level Security:

#### 2.1 `privacy_requests`
Tracks life-cycle state of export, closure, and deletion requests.
- **Fields**: `id`, `user_id`, `studio_id` (nullable), `request_type`, `status`, `confirmed_at`, `completed_at`, `failure_reason`, `metadata`, `created_at`, `updated_at`.
- **Request Types**: `USER_DATA_EXPORT`, `STUDIO_DATA_EXPORT`, `ACCOUNT_CLOSURE`, `STUDIO_CLOSURE`, `ACCOUNT_DELETION`.
- **Statuses**: `REQUESTED`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`.
- **RLS Policy**: `user_isolation_privacy_requests` restricts rows to matching `user_id`, matching `studio_id`, or platform admins.

#### 2.2 `data_export_jobs`
Manages asynchronous preparation and secure download tokens for generated ZIP archives.
- **Fields**: `id`, `privacy_request_id`, `user_id`, `studio_id`, `export_type`, `storage_key`, `file_size_bytes`, `download_token_hash`, `download_count`, `status`, `expires_at`, `completed_at`.
- **Status Lifecycle**: `QUEUED` -> `PROCESSING` -> `READY` -> `EXPIRED` (default TTL: 7 days).
- **Security**: Raw download tokens are 64-character entropy strings hashed via SHA-256 in the database.

#### 2.3 `privacy_legal_holds`
Guards against destructive data erasure.
- **Fields**: `id`, `subject_type` (`USER` or `STUDIO`), `subject_id`, `reason`, `placed_by`, `placed_at`, `released_at`, `active`, `metadata`.
- **RLS Policy**: `admin_isolation_privacy_legal_holds` restricts access exclusively to platform administrators (`app.is_admin = 'true'`).

---

### 3. Core Domain & Services

#### 3.1 `PrivacyExportService`
- Compiles user and studio data packages into memory-efficient ZIP streams.
- Implements CSV Formula Injection Defense:
  ```java
  public static String sanitizeCsvField(String field) {
      if (field == null) return "";
      String trimmed = field.trim();
      if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
          trimmed = "'" + trimmed;
      }
      if (trimmed.contains(",") || trimmed.contains("\"") || trimmed.contains("\n") || trimmed.contains("\r")) {
          return "\"" + trimmed.replace("\"", "\"\"") + "\"";
      }
      return trimmed;
  }
  ```
- Uploads packages to `StorageService` under keys `exports/users/{userId}/{jobId}.zip` or `exports/studios/{studioId}/{jobId}.zip`.
- Enforces single-use or time-bounded token verification on download.

#### 3.2 `PrivacyService`
- **Sole Admin Studio Check**: Queries PostgreSQL to ensure an active studio is never left ownerless:
  - If user is sole admin: throws `ACCOUNT_CLOSURE_BLOCKED_BY_STUDIO_OWNERSHIP`.
- **Legal Hold Check**: Rejects closure or deletion if an active hold exists (`LEGAL_HOLD_ACTIVE`).
- **Account Closure**:
  - Sets user status to `SUSPENDED` and records `deactivated_at`.
  - Revokes all active user sessions (`identity_sessions`).
- **Account Deletion & Anonymization**:
  - Overwrites personal identity row: `display_name = 'Deleted User'`, `email = NULL`, `phone = NULL`, `avatar_url = NULL`, `status = 'DELETED'`.
  - Deletes OIDC links (`identity_external_identities`), MFA credentials (`identity_mfa_methods`), and recovery code hashes (`identity_recovery_codes`).
  - Purges private moodboards (`user_collections`) and notifications inbox (`notifications`).
  - Preserves financial rows and audit ledger (`audit_events`) with tombstoned user references.
- **Studio Closure**:
  - Suspends studio (`status = 'SUSPENDED'`, `publication_status = 'DRAFT'`).
  - Archives active studio projects (`project_status = 'ARCHIVED'`, `visibility_status = 'PRIVATE'`).

---

### 4. Controller Endpoints
- `GET /privacy/requests` — User's recent privacy and export requests.
- `POST /privacy/export/user` — Trigger personal archive compilation.
- `POST /privacy/export/studio/{studioId}` — Trigger studio archive compilation.
- `GET /privacy/export/download/{jobId}?token=...` — Download secure ZIP archive.
- `POST /privacy/close-account` — Self-service account suspension and session termination.
- `POST /privacy/request-deletion` — Permanent account deletion and anonymization.
- `POST /privacy/studio/{studioId}/close` — Studio unpublishing and project archiving.
- `GET /admin/privacy/legal-holds` — Admin query for active legal holds.
- `POST /admin/privacy/legal-holds` — Admin placement of legal hold.
- `DELETE /admin/privacy/legal-holds/{holdId}` — Admin release of legal hold.

---

### 5. Verification Matrix
- **PostgreSQL Suites**: 11 / 11 classes passing (39 tests total including `PostgreSqlPrivacySecurityTest`).
- **Web Frontend**: TypeScript compile clean, ESLint clean, 52 / 52 test files passing (368 Vitest tests), Next.js production build succeeded with zero prerender errors.
