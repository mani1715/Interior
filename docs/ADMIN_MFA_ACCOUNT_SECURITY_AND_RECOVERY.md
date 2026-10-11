# Phase 9D — Admin MFA, Account Security, Session Assurance & Recovery Architecture

## 1. Architectural Overview & Context

In **Phase 9D**, Elégance hardened platform identity, administrator control-plane safety, and account integrity without introducing local passwords, SMS OTP, or security questions. Primary authentication remains 100% anchored in **OIDC PKCE + State + Nonce** with server-side opaque cryptographic sessions stored in PostgreSQL.

Multi-Factor Authentication (MFA) was introduced as an orthogonal cryptographic assurance layer:
1. **Standards-Compliant TOTP (RFC 6238)**: 6-digit numeric time-based one-time codes computed using HMAC-SHA1 over 30-second steps. Clock drift tolerance: ±1 step (30 seconds). Replay protection enforces strictly increasing time counters.
2. **Encrypted Secrets at Rest**: Authenticator shared secrets are generated with 160 bits (20 bytes) of secure random entropy and encrypted at rest using AES-256-GCM authenticated encryption with explicit key versioning (`v1:AES256GCM:<iv>:<ciphertext>:<tag>`).
3. **Backup Recovery Codes**: High-entropy 16-character alphanumeric backup codes (`XXXX-XXXX-XXXX-XXXX`), presented in batches of 10. Only SHA-256 hashes are stored in the database. Redemptions are atomic and strictly one-time.
4. **Session Assurance Elevation**: Sessions track `mfa_verified_at` and elevate assurance to `MFA_VERIFIED`.
5. **Fresh Step-Up Assurance**: High-risk administrative actions (e.g. changing administrative roles, resetting another administrator's MFA, or terminating sessions of another administrator) enforce a 15-minute freshness window (`stepUpFreshnessSeconds=900`).
6. **Mandatory Administrative Policy**: MFA is required for `SUPER_ADMIN` and `ADMIN` (and configurable for `MODERATOR`). Privileged control-plane operations reject requests without active MFA and an elevated session.

---

## 2. Database Schema (Flyway V037)

Created in `apps/api/src/main/resources/db/migration/V037__admin_mfa_and_account_security.sql` and matched in test migrations:

### `identity_mfa_methods`
- `id uuid DEFAULT uuidv7() PRIMARY KEY`
- `user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE`
- `method_type text NOT NULL CHECK (method_type IN ('TOTP', 'WEBAUTHN'))`
- `encrypted_secret text NOT NULL`
- `key_version int NOT NULL DEFAULT 1`
- `status text NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED'))`
- `pending_expires_at timestamptz NULL`
- `confirmed_at timestamptz NULL`
- `last_used_counter bigint NULL`
- `failed_attempts int NOT NULL DEFAULT 0`
- `locked_until timestamptz NULL`
- `created_at timestamptz NOT NULL DEFAULT now()`
- `updated_at timestamptz NOT NULL DEFAULT now()`
- `CONSTRAINT uq_user_mfa_method UNIQUE (user_id, method_type)`

### `identity_recovery_codes`
- `id uuid DEFAULT uuidv7() PRIMARY KEY`
- `user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE`
- `code_hash text NOT NULL`
- `used_at timestamptz NULL`
- `created_at timestamptz NOT NULL DEFAULT now()`
- `CONSTRAINT uq_user_code_hash UNIQUE (user_id, code_hash)`

### `identity_sessions` Alteration
- Added `mfa_verified_at timestamptz NULL` to record when MFA verification was completed on the session.

---

## 3. Endpoints & API Contract

### User MFA Endpoints (`/auth/mfa` and `/api/v1/auth/mfa`)
- `GET /auth/mfa/status` — Retrieves MFA status, method type, remaining backup code count, session elevation status, and step-up expiry.
- `POST /auth/mfa/totp/enroll` — Generates a new 160-bit random secret, Base32 key, and `otpauth://` URI with a 10-minute setup TTL.
- `POST /auth/mfa/totp/confirm` — Validates the 6-digit confirmation code, activates TOTP, generates 10 backup codes, elevates the current session, and revokes other non-MFA sessions.
- `POST /auth/mfa/challenge` — Verifies a 6-digit TOTP code during session step-up or re-authentication, resetting lockout counters on success.
- `POST /auth/mfa/recovery-code` — Atomically redeems a single-use backup recovery code.
- `POST /auth/mfa/recovery-codes/regenerate` — Requires fresh step-up; invalidates existing recovery codes and issues 10 new codes.
- `POST /auth/mfa/disable` — Requires fresh step-up; disables TOTP, deletes unused recovery codes, revokes other sessions, and demotes session assurance.

### Account Session Endpoints (`/account` and `/api/v1/account`)
- `POST /account/sessions/revoke-others` — Terminates all active sessions for the user except the calling session.

### Admin Control Plane Endpoints (`/admin`)
- `POST /admin/users/{userId}/mfa/reset` — Super Admin emergency reset for locked/compromised accounts. Requires valid reason (min 10 characters), enforces self-bypass prevention (Super Admins cannot reset themselves via this route), immediately revokes all target sessions, and dispatches an audit alert.

---

## 4. Verification & Validation Metrics

| Suite / Check | Command | Discovered / Total | Result |
|---|---|---|---|
| PostgreSQL Suite | `mvn test -Dtest=*PostgreSql*Test` | 10 classes, 36 tests | **36 / 36 PASS** |
| Full Backend Suite | `mvn test` | 74 test classes | **508 / 508 PASS** |
| Frontend Unit Suite | `npm --prefix apps/web run test` | 52 test files, 368 tests | **368 / 368 PASS** |
| TypeScript Check | `npm --prefix apps/web run typecheck` | Whole project | **0 Errors** |
| ESLint Check | `npm --prefix apps/web run lint` | Whole project | **0 Warnings / Errors** |
| Next.js Production Build | `npm --prefix apps/web run build` | 28 static/dynamic routes | **Clean Build** |
| Flyway Validation | `mvn flyway:validate` | V001 → V037 | **38 / 38 Valid** |

---

## 5. Security Invariants Preserved
- No local passwords or password reset forms created.
- No third-party phone/SMS OTP or email-only administrator fallbacks.
- Replay attacks on TOTP codes are prevented via monotonic time counters.
- Recovery codes cannot be reused; second attempt updates 0 rows.
- Rate limiting and lockout defense: 5 failed attempts trigger a 15-minute lockout.
- Non-admin identities remain unencumbered unless voluntarily opting into MFA.
