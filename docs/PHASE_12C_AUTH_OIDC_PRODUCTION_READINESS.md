# Elégance Platform: Phase 12C Authentication & OIDC Production Readiness

## 1. Executive Summary & Strategy Alignment

In accordance with the Elégance Production Readiness Strategy, the platform authentication system has been fully implemented, cryptographically verified, and made 100% production-ready without provisioning real Google Cloud accounts or live production credentials. Real Google provider activation and live cloud authorization verification are formally scheduled for **Phase 15 (Pre-Launch Activation)**.

### Architectural Tenets
1. **Confidential Server-Side Authorization Code Flow**: The browser never handles confidential credentials. All token exchanges occur server-to-server.
2. **PKCE S256 Enforcement**: PKCE RFC 7636 with SHA-256 code challenge is enforced on all authorization requests, neutralizing authorization code interception.
3. **Cryptographic ID Token Signature Verification**: ID tokens are verified using standard RSA SHA-256 (`RS256`) against provider JWKS (`RFC 7517`), with in-memory caching and automatic key rotation recovery.
4. **Platform-Authoritative Roles & Least Privilege**: Roles (`CUSTOMER`, `DESIGNER`, `MODERATOR`, `ADMIN`, `SUPER_ADMIN`) are strictly database-authoritative. External OIDC claims cannot grant elevated roles. All new external identities register as `CUSTOMER`.
5. **Anti-Account-Takeover Email Defense**: When an incoming OIDC identity presents an email address that matches an unlinked local user, the authentication is rejected and audited with `AUTH_EMAIL_COLLISION_REJECTED`.
6. **Defense-in-Depth Session Model**: Database-backed opaque 256-bit entropy sessions stored as SHA-256 hashes in `identity_sessions`. Cookies are delivered with `__Host-session`, `HttpOnly`, `SameSite=Lax`, `Path=/`, and `Secure` in production.
7. **Privileged Step-Up Multi-Factor Authentication**: RFC 6238 TOTP (SHA1, 30s period, 6 digits, ±1 window) with AES-256-GCM secret encryption at rest and 10 single-use hashed backup recovery codes is mandatory for all administrative access.

---

## 2. Component & Source Inventory

| Subsystem / Component | Source File | Purpose | Production Status | Test Verification |
| :--- | :--- | :--- | :--- | :--- |
| **OIDC Orchestrator** | `OidcService.java` | Coordinates authorization redirects, state/nonce, token exchange, claims validation, and user resolution | READY (Provider-neutral) | `LocalOidcIntegrationTest`, `OidcSecurityTest` |
| **Token Exchange Client** | `DefaultOidcTokenClient.java` | Server-to-server HTTP POST exchange of authorization code & PKCE verifier for ID tokens | READY (Java 21 HttpClient) | `LocalOidcIntegrationTest` |
| **JWKS Key Resolver** | `DefaultJwksResolver.java` | Fetches, parses, and caches RFC 7517 RSA public keys; recovers dynamically on unknown `kid` | READY (Thread-safe cache) | `LocalOidcIntegrationTest` |
| **ID Token Validator** | `DefaultIdTokenValidator.java` | Cryptographically verifies RS256 signature, validates `iss`, `aud`, `exp`, `iat`, `nonce`, `email_verified` | READY (Native java.security) | `LocalOidcIntegrationTest`, `OidcSecurityTest` |
| **Transaction Store** | `OidcTransactionStore.java` | Multi-instance durable atomic single-use state/nonce/verifier storage in `auth_oidc_transactions` | READY (PostgreSQL backed) | `LocalOidcIntegrationTest`, `OidcSecurityTest` |
| **Session Service** | `SessionSecurityService.java` | Generates 256-bit opaque tokens, SHA-256 token hashing, idle/absolute timeouts, CSRF tokens, cookie handling | READY (PostgreSQL backed) | `OidcDestinationRoutingTest`, `SessionSecurityServiceTest` |
| **MFA Service** | `MfaService.java`, `TotpService.java` | RFC 6238 TOTP, AES-256-GCM secret encryption, 15-minute step-up freshness, 5-attempt lockout, recovery codes | READY (PostgreSQL backed) | `PostgreSqlAdminMfaSecurityTest`, `TotpServiceTest` |
| **Destination Router** | `apps/web/src/lib/auth/redirect.ts` | Role-aware post-auth destination (`/admin`, `/workspace`, `/onboarding/professional`, `/account`) | READY (Frontend engine) | `redirect.test.ts`, `OidcDestinationRoutingTest` |
| **Startup Validator** | `ProductionConfigurationValidator.java` | Fails fast if dev-auth enabled, session cookie insecure, or OIDC configuration is partial/placeholder | READY (Spring runner) | `ProductionConfigurationValidatorOidcTest` |

---

## 3. Google OIDC Protocol Compatibility Audit

| Requirement / Claim | Google OIDC Standard Contract | Elégance Platform Compliance | Verification Status |
| :--- | :--- | :--- | :--- |
| **Discovery** | `https://accounts.google.com/.well-known/openid-configuration` | Discovers endpoints via configuration or standard well-known schema | VERIFIED (`scripts/verify-google-oidc-live.ps1`) |
| **Authorization Endpoint** | `https://accounts.google.com/o/oauth2/v2/auth` | Generates valid authorization URL with `response_type=code` | VERIFIED (`LocalOidcIntegrationTest`) |
| **Token Endpoint** | `https://oauth2.googleapis.com/token` | Executes `application/x-www-form-urlencoded` POST with code & PKCE | VERIFIED (`LocalOidcIntegrationTest`) |
| **JWKS Endpoint** | `https://www.googleapis.com/oauth2/v3/certs` | Fetches, parses RSA keys (`kty="RSA"`), verifies RS256 signature | VERIFIED (`LocalOidcIntegrationTest`) |
| **Scopes** | `openid email profile` | Configured default: `List.of("openid", "email", "profile")` | VERIFIED (`AuthSecurityProperties`) |
| **Issuer (`iss`)** | `https://accounts.google.com` or `accounts.google.com` | Normalizes and validates issuer without protocol prefix ambiguity | VERIFIED (`DefaultIdTokenValidator`) |
| **Audience (`aud`)** | Matches Google Client ID | Enforces strict string equality against configured client ID | VERIFIED (`DefaultIdTokenValidator`) |
| **Subject (`sub`)** | Unique Google account identifier | Primary key anchor in `identity_external_identities(issuer, subject)` | VERIFIED (`V001__security_identity_tenant_schema.sql`) |
| **Email (`email_verified`)**| Boolean claim indicating verified Google mailbox | Fails closed: rejects authentication if `email_verified == false` | VERIFIED (`LocalOidcIntegrationTest`) |
| **State & Nonce** | Echoed back in authorization callback and ID token | Cryptographically random 32-byte Base64Url strings; single-use | VERIFIED (`LocalOidcIntegrationTest`) |
| **PKCE S256** | Supported on Google authorization-code flow | SHA-256 Base64Url challenge sent and verified | VERIFIED (`OidcSecurityTest`) |
| **UserInfo Endpoint** | `https://openidconnect.googleapis.com/v1/userinfo` | **Not required**: All identity claims are extracted directly from ID token | MINIMAL CLAIMS ADOPTED |

---

## 4. Destination Routing Matrix

| User Persona & Role | Database State | Destination | Security Policy / Gate |
| :--- | :--- | :--- | :--- |
| **Customer** | Status `ACTIVE`, Role `CUSTOMER` | `/account` | Basic user session, CSRF token issued |
| **Customer (Deep Link)** | Status `ACTIVE`, valid internal returnUrl | `returnUrl` (e.g. `/gallery`) | Sanitized against open redirect |
| **Designer (With Studio)** | Status `ACTIVE`, Role `DESIGNER`, `activeStudioId != null` | `/workspace` | Tenancy isolated to user's authorized studio |
| **Designer (No Studio)** | Status `ACTIVE`, Role `DESIGNER`, No studio membership | `/onboarding/professional` | Guided onboarding flow |
| **Designer (Multi-Studio)** | Status `ACTIVE`, Multiple studio memberships | `/workspace` | Defaults to primary studio; switcher enabled |
| **Platform Administrator** | Status `ACTIVE`, Role `ADMIN` | `/admin` | Requires TOTP MFA enrollment / step-up challenge |
| **Super Administrator** | Status `ACTIVE`, Role `SUPER_ADMIN` | `/admin` | Mandatory TOTP MFA; cannot reset own MFA via API |
| **Platform Moderator** | Status `ACTIVE`, Role `MODERATOR` | `/admin` (or review queue) | Enforces configured moderator MFA policy |
| **Suspended Account** | Status `SUSPENDED` | Rejected (`403 Forbidden`) | Session not issued; access denied |
| **Deleted Account** | Status `DELETED` | Rejected (`403 Forbidden`) | Cannot re-authenticate permanently closed account |

---

## 5. Security & Isolation Controls

1. **Open Redirect Defense**: All return URLs undergo strict sanitization in `OidcService.sanitizeReturnUrl` and `apps/web/src/lib/auth/redirect.ts`. Double slashes (`//`), backslashes (`\`), CRLF injection, protocol-relative URLs, and non-relative URLs are normalized to `/`.
2. **Session Fixation Resistance**: Upon successful authentication, a new cryptographic session record and token are created. The temporary OIDC state transaction is destroyed.
3. **Session Timeouts**:
   - Idle Timeout: 1,800 seconds (30 minutes).
   - Absolute Timeout: 43,200 seconds (12 hours).
   - Last-Seen Update Throttle: 300 seconds (5 minutes) to protect database write throughput.
4. **Token Security**: Tokens and secrets are never logged in application logs, Actuator metrics, audit event metadata, or error responses.
5. **Brute Force Lockout**: TOTP verification enforces a lockout of 15 minutes after 5 consecutive failed attempts.
