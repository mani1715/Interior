# Phase 12C: Authentication & OIDC Environment Secret Setup

## 1. Overview

This document specifies the complete inventory of authentication, OIDC, session security, and MFA environment variables and cryptographic secrets for the Elégance Interior Designer Platform.

> **CRITICAL SECURITY REQUIREMENT**:
> OAuth client secrets, TOTP master encryption keys, and administrative secrets must NEVER be committed to Git. In production, these values must be supplied exclusively through environment variables or secure vault stores (e.g., AWS Secrets Manager, HashiCorp Vault, Cloudflare Secrets).

---

## 2. Configuration & Secret Inventory

| Variable Name | Required in Prod? | Default (Local Dev) | Example Value (Production) | Purpose & Security Constraint |
| :--- | :--- | :--- | :--- | :--- |
| `GOOGLE_CLIENT_ID` | Optional (Deferred) | *Empty* | `123456789-abc.apps.googleusercontent.com` | Google OAuth 2.0 Web Client ID. Rejected if placeholder (`your-client-id`). |
| `GOOGLE_CLIENT_SECRET` | Required if ID set | *Empty* | `<GCP_CLIENT_SECRET_TOKEN>` | Google OAuth 2.0 Client Secret for server-to-server token exchange. |
| `GOOGLE_OIDC_ISSUER` | No | `https://accounts.google.com` | `https://accounts.google.com` | Expected OIDC Issuer (`iss`) claim in ID token. |
| `GOOGLE_AUTHORIZATION_URI` | No | `https://accounts.google.com/o/oauth2/v2/auth` | `https://accounts.google.com/o/oauth2/v2/auth` | Google OAuth 2.0 authorization endpoint. |
| `GOOGLE_TOKEN_URI` | No | `https://oauth2.googleapis.com/token` | `https://oauth2.googleapis.com/token` | Google token exchange endpoint. |
| `GOOGLE_JWK_SET_URI` | No | `https://www.googleapis.com/oauth2/v3/certs` | `https://www.googleapis.com/oauth2/v3/certs` | Google RFC 7517 public key certificates endpoint. |
| `GOOGLE_REDIRECT_URI` | No | `http://localhost:8080/api/v1/auth/oidc/callback/google` | `https://elegance-design.com/api/v1/auth/oidc/callback/google` | OAuth callback URI. Must match Google Cloud Console whitelist. |
| `APP_SECURITY_MFA_ENCRYPTION_KEY` | Recommended | `0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef` | `<BASE64_32_BYTE_RANDOM_KEY>` | 256-bit AES master key used to encrypt TOTP secrets at rest via AES-256-GCM. |
| `APP_SECURITY_DEV_AUTH_ENABLED` | **MUST BE FALSE** | `true` | `false` | Development authentication toggle. Production startup **fails fast** if true. |
| `APP_SECURITY_SESSION_COOKIE_NAME` | No | `session` | `__Host-session` | Cookie prefix. `__Host-` prefix enforces HTTPS, path `/`, and domain restriction. |
| `APP_SECURITY_SESSION_COOKIE_SECURE`| **MUST BE TRUE** | `false` | `true` | Requires HTTPS transport. Production startup **fails fast** if false. |
| `APP_SECURITY_SESSION_COOKIE_SAME_SITE`| No | `Lax` | `Lax` | Mitigates CSRF. Lax allows safe top-level cross-site navigations. |

---

## 3. Cryptographic Key Generation Guidelines

### 3.1. MFA AES-256 Master Key (`APP_SECURITY_MFA_ENCRYPTION_KEY`)
The MFA service uses AES-256-GCM with authenticated tags to encrypt TOTP shared secrets in the `auth_user_mfa` table. 

Generate a cryptographically secure 256-bit (32-byte) key using either OpenSSL or PowerShell:

**OpenSSL (Linux / macOS / Git Bash)**:
```bash
openssl rand -base64 32
```

**PowerShell (Windows)**:
```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### 3.2. Session Security
- Sessions are backed by 256 bits of CSPRNG entropy (`java.security.SecureRandom`).
- Tokens are hashed with SHA-256 before persisting to PostgreSQL `identity_sessions`.
- No server-side session signing secret is required because session validation is stateful and database-backed with row-level locking.

---

## 4. Production Configuration Fail-Fast Rules

[`ProductionConfigurationValidator`](file:///c:/my%20projects/interior%20design/apps/api/src/main/java/com/interior/platform/config/ProductionConfigurationValidator.java) enforces these strict startup validations when running with `SPRING_PROFILES_ACTIVE=production`:

1. **Dev Auth Prohibition**:
   - `app.security.dev-auth-enabled` must be `false`. If `true`, startup throws `IllegalStateException`.
2. **Session Cookie Security**:
   - `app.security.session.cookie-secure` must be `true`. If `false`, startup throws `IllegalStateException`.
3. **Placeholder Secret Rejection**:
   - If `GOOGLE_CLIENT_ID` or `GOOGLE_CLIENT_SECRET` contains placeholder tokens (`your-client-id`, `placeholder`, `changeme`), startup throws `IllegalStateException`.
4. **Partial OIDC Rejection**:
   - If `GOOGLE_CLIENT_ID` is set but `GOOGLE_CLIENT_SECRET` is missing (or vice versa), startup throws `IllegalStateException`.
5. **Phase 15 Provider Activation Deferred Grace**:
   - If neither `GOOGLE_CLIENT_ID` nor `GOOGLE_CLIENT_SECRET` is configured, the validator safely logs `NOT_CONFIGURED (Phase 15 Provider Activation Pending)` and permits the platform to start with local authentication active.
