# Elégance Platform — Authentication, Session Lifecycle, and Access Control Architecture

## 1. Executive Summary & Core Guarantees

Phase 7A completes the enterprise-grade identity, authentication, session lifecycle, role resolution, and workspace routing architecture for the **Elégance Interior Designer Platform**.

The architecture adheres to three non-negotiable security principles:
1. **Zero Client Trust**: All authorization checks, role claims, and studio memberships are authoritatively evaluated and enforced server-side via PostgreSQL-backed sessions and tenant isolation guards.
2. **Deterministic Role-Aware Routing**: Post-authentication destinations are mathematically resolved based on canonical platform roles, verified studio memberships, and designer onboarding status, while strictly defending against open redirect exploits.
3. **Defense-in-Depth Session Isolation**: Sessions use 256-bit cryptographically secure opaque tokens hashed with SHA-256 in the database, protected by `__Host-session` cookie flags, per-session synchronizer CSRF tokens, anti-caching headers (`Cache-Control: no-store, private`), and per-tab studio context headers (`X-Studio-Id`).

---

## 2. Authentication Provider Model

### 2.1 Production Model: OIDC with PKCE
- **Protocols Supported**: OpenID Connect (OIDC) Core 1.0 with Authorization Code Grant + Proof Key for Code Exchange (PKCE, RFC 7636).
- **Social & Enterprise Providers**: Google, Microsoft, Apple, GitHub, and enterprise SAML/OIDC identity providers.
- **State & Nonce Guards**: Cryptographically random `state` (tied to session) and `nonce` (validated against ID token) prevent replay and login CSRF attacks.
- **Provider Metadata Endpoint**: `GET /api/v1/auth/providers` returns enabled providers and login configurations.
- **Login Initiation**: `GET /api/v1/auth/login?provider={id}&returnUrl={sanitizedUrl}` generates code verifier, code challenge, state, and redirects to upstream IdP.
- **Callback Processing**: `GET /api/v1/auth/callback` exchanges code for tokens, verifies signature/claims, resolves or provisions the local user, creates an opaque session, and issues the session cookie.

### 2.2 Dev / Test Sandbox Model
- **Dev Auth Guard**: `DevAuthService` is exclusively activated under `dev` and `test` Spring profiles. In `prod`, it is disabled and returns 404/disabled status.
- **Dev Persona Login**: `POST /api/v1/auth/dev-login` supports instant switching between canonical personas:
  - `ananya`: Studio Owner / Designer Admin (`DESIGNER`, `DESIGNER_ADMIN`)
  - `vikram`: Customer / Homeowner (`CUSTOMER`)
  - `rohit`: Studio Team Member (`DESIGNER_TEAM`, `DESIGNER_MEMBER`)
  - `priya`: Platform Super Administrator (`SUPER_ADMIN`)

---

## 3. Server-Side Session Lifecycle & Security

### 3.1 Token Entropy & Database Storage
- **Opaque Token Generation**: 256-bit entropy generated using `java.security.SecureRandom`.
- **Database Table**: `identity_sessions`
- **One-Way Token Hashing**: The raw session token is NEVER stored in plaintext. It is hashed using SHA-256 before insertion/lookup:
  $$\text{token\_hash} = \text{SHA-256}(\text{raw\_token})$$
- **Session Attributes**:
  - `user_id`: Foreign key to `identity_users(id)`
  - `expires_at`: Absolute session expiration timestamp (default: 7 days)
  - `idle_timeout_at`: Rolling activity timeout timestamp (default: 2 hours)
  - `revoked`: Boolean flag for immediate revocation
  - `csrf_token`: Cryptographically unique CSRF synchronizer token
  - `ip_address` & `user_agent`: Recorded for audit logging and anomaly detection.

### 3.2 Cookie Flags
The session cookie is delivered to the browser with maximum security attributes:
```http
Set-Cookie: __Host-session=<token>; Path=/; Secure; HttpOnly; SameSite=Lax
```
- `__Host-` prefix: Prevents cookie tossing from subdomains, guarantees `Secure` flag, and enforces root `Path=/`.
- `HttpOnly`: Completely invisible to JavaScript, preventing XSS-based session hijacking.
- `SameSite=Lax`: Balances defense against cross-site request forgery with smooth top-level incoming navigations from authentication providers.

### 3.3 Anti-Caching Headers
All authenticated API responses automatically include:
```http
Cache-Control: no-store, private
Pragma: no-cache
```
Enforced in `SecurityInterceptor.java` to prevent intermediate proxies, browser caches, and shared devices from storing sensitive tenant or user data.

### 3.4 CSRF Protection
- **Header**: `X-CSRF-Token`
- **Validation**: Enforced on all mutating HTTP methods (`POST`, `PUT`, `PATCH`, `DELETE`).
- **Exemptions**: Safe read-only methods (`GET`, `HEAD`, `OPTIONS`) and public webhooks with dedicated HMAC signature verification.

---

## 4. Current User API (`GET /auth/me`) & Status Resolution

The `/api/v1/auth/me` endpoint returns the authoritative user identity, global roles, studio memberships, active studio context, and account status.

### 4.1 Account Status Enforcement
The backend `ActorContext` carries the real database `status` (`ACTIVE`, `PENDING`, `SUSPENDED`, `DELETED`).
- **Session Validation**: `validateSession` in `SessionSecurityService` checks the user status. Suspended, pending, or deleted accounts are denied session validation.
- **Authorization Service**: `requireActiveUser(actor)` runs across all role, permission, and studio tenancy checks in `AuthorizationService`. Any suspended account is immediately denied access with `AccessDeniedException("Account is suspended")`.
- **Frontend Workspace Shell**: Detects `user.status === 'SUSPENDED'` and renders a dedicated safe suspension notice explaining how to contact support, preventing any workspace actions.

---

## 5. Deterministic Role-Based Routing & Open Redirect Defense

Post-login redirection is governed by `resolveAuthDestination(user, returnUrl)` in `@/lib/auth/redirect.ts`.

### 5.1 Routing Hierarchy
1. **Explicit Custom Destination**: If a valid, non-default `returnUrl` was requested (e.g. `/workspace/projects/new`), the user is routed directly to it.
2. **Platform Admin / Super Admin**: Users with `ADMIN` or `SUPER_ADMIN` roles are routed to `/admin`.
3. **Active Studio / Professional Member**: Users with an active studio or studio memberships are routed to `/workspace`.
4. **Incomplete Professional Designer**: Users with the `DESIGNER` global role who have not completed studio creation are routed to `/onboarding/professional`.
5. **Customer / Default**: Standard clients and homeowners are routed to `/account`.

### 5.2 Open Redirect Defense (`sanitizeRedirectUrl`)
The sanitizer validates the destination against multiple attack vectors:
- Rejects absolute URLs (`http://`, `https://`)
- Rejects protocol-relative URLs (`//attacker.com`)
- Rejects Windows network share paths (`\\attacker.com`, `\\\\attacker.com`)
- Rejects backslash tricks (`/\attacker.com`, `/workspace\attack`)
- Rejects URL-encoded bypasses (`%2f`, `%5c`)
- Rejects script execution schemes (`javascript:`, `data:`)
- Rejects CRLF header injection (`\r`, `\n`)
- Gracefully falls back to `/account` or caller-specified fallback if any malformed pattern is detected.

---

## 6. Multi-Studio Tenancy & Context Switching

### 6.1 Studio Scope Header (`X-Studio-Id`)
- Every workspace request sends `X-Studio-Id: <uuid>`.
- Client-side storage uses `sessionStorage` (`elegance_active_studio_id`), ensuring tab isolation when a designer works on multiple studios in different browser tabs.
- Backend `TenantResolutionFilter` and `SecurityInterceptor` verify that the authenticated actor is an active member of the specified studio before setting the tenancy context.

### 6.2 Studio Role Matrix (V027 Schema)
- `DESIGNER_ADMIN`: Full ownership, billing, team management, project deletion, portfolio publishing.
- `DESIGNER_MEMBER`: Collaborative project management, moodboard creation, media uploading, lead communication.

---

## 7. Professional Onboarding & Studio Invitations

### 7.1 Professional Onboarding (`POST /designers/onboarding/complete`)
- Single transactional method executing:
  1. Validates unique slug and business metadata.
  2. Creates the studio record in `studios`.
  3. Inserts studio membership with role `DESIGNER_ADMIN`.
  4. Upgrades user global role to `DESIGNER` in `identity_user_roles`.
  5. Sets active studio context and returns refreshed session.
- **Idempotency Guard**: Re-running onboarding for an already onboarded studio owner safely resolves to the existing studio without duplicating records.

### 7.2 Studio Team Invitations
- **Validation**: `GET /api/v1/team/invitations/validate?token={rawToken}` validates cryptographic token, expiration (7 days), and returns studio metadata.
- **Acceptance**: `POST /api/v1/team/invitations/accept`
  - Validates authenticated user email matches invitation email.
  - Grants `DESIGNER_MEMBER` or `DESIGNER_ADMIN` membership in `studio_memberships`.
  - Marks invitation as `ACCEPTED` (single-use guarantee).
  - Automatically activates the newly joined studio context.

---

## 8. Verification & Test Summary

| Test Category | Suite / Class | Result | Key Guarantees Verified |
| :--- | :--- | :--- | :--- |
| **Session Security** | `SessionSecurityServiceTest` | **PASS** | 256-bit entropy, SHA-256 hashing, rolling idle timeout, absolute expiry, revocation |
| **Authorization Policy** | `AuthorizationPolicyTest` | **PASS** | Role matching, tenant isolation, MFA assurance, suspended account rejection |
| **Dev Authentication** | `DevAuthSecurityTest` | **PASS** | Persona loading, profile gating (dev/test only), cookie emission |
| **Tenant Isolation** | `TenantIsolationTest` | **PASS** | Strict cross-studio access denial, X-Studio-Id validation |
| **Studio Team Management** | `StudioTeamIntegrationTest` | **PASS** | Invite lifecycle, email match guard, role upgrade/downgrade, member removal |
| **Onboarding Closure** | `OnboardingClosureIntegrationTest` | **PASS** | Transactional studio provisioning, idempotency, DESIGNER_ADMIN assignment |
| **Onboarding Service** | `ProfessionalOnboardingServiceTest` | **PASS** | Slug uniqueness, validation constraints, profile updates |
| **CSRF Defense** | `CsrfProtectionTest` | **PASS** | Mutating method enforcement, token generation and per-session validation |
| **Redirect Sanitization (API)** | `RedirectSanitizerTest` | **PASS** | Rejection of CRLF, protocol-relative, and external URLs |
| **Redirect & Role Routing (Web)** | `redirect.test.ts` | **PASS** (16/16) | Open redirect defense vectors, deterministic role routing (/admin, /workspace, /account) |
| **Frontend Authentication UI** | `Auth.test.tsx` | **PASS** | PublicHeader auth awareness, sign-up least privilege, account view, error recovery |
| **Full Frontend Test Suite** | 52 test files | **PASS** (368/368) | Complete frontend test suite across UI components, templates, and flows |
| **Frontend Typecheck** | `tsc --noEmit` | **PASS** | Zero TypeScript compilation errors |
| **Frontend Linter** | `eslint .` | **PASS** | Zero linting errors |
| **Production Web Build** | `next build` (Turbopack) | **PASS** | All static and dynamic routes compiled successfully in 14.2s |
