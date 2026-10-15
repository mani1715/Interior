# Phase 12C / Phase 15 Runbook: Google OIDC Live Cloud Validation

## 1. Executive Summary & Strategy Alignment

In accordance with the **Elégance Production Readiness Strategy**, real external credentials and live provider validations are deferred to **Phase 15 (Pre-Launch Activation)**. 

The complete OIDC protocol stack (PKCE S256, RFC 7517 JWKS caching, RS256 token verification, claims validation, user resolution, destination routing, MFA step-up, and session management) is fully implemented and tested with local mock suites.

This runbook provides the definitive operational steps for provisioning Google Cloud OAuth credentials and running the live provider validation drill during Phase 15.

> **CRITICAL SECURITY REQUIREMENT**:
> Google OAuth Client Secrets must NEVER be committed to Git or printed in build/test logs. All credentials must be injected dynamically via environment variables or secret managers (e.g. AWS Secrets Manager, HashiCorp Vault, Cloudflare Secrets).

---

## 2. Google Cloud Platform Provisioning Steps (Phase 15)

### 2.1. GCP Project Setup
1. Log in to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select the production project (e.g., `elegance-platform-prod`).
3. Navigate to **APIs & Services** > **Enabled APIs & services**.
4. Ensure the **Google Identity Services** / OAuth APIs are enabled.

### 2.2. OAuth Consent Screen Configuration
1. Navigate to **APIs & Services** > **OAuth consent screen**.
2. Select **External** (or **Internal** if restricted to corporate workspace).
3. Fill in the App Information:
   - **App name**: `Elégance` (or final branded commercial product name).
   - **User support email**: Dedicated support mailbox (e.g., `support@elegance-design.com`).
   - **Developer contact information**: Security/admin team email.
4. Scopes:
   - Click **Add or Remove Scopes**.
   - Select: `openid`, `.../auth/userinfo.email`, `.../auth/userinfo.profile`.
5. Authorized Domains:
   - Add your production apex domain (e.g., `elegance-design.com`).
6. Save and continue.

### 2.3. OAuth 2.0 Web Application Credentials
1. Navigate to **APIs & Services** > **Credentials**.
2. Click **Create Credentials** > **OAuth client ID**.
3. Set **Application type**: `Web application`.
4. Set **Name**: `Elégance Web Application (Production)`.
5. Set **Authorized JavaScript origins**:
   - `https://<YOUR_PRODUCTION_DOMAIN>` (e.g., `https://elegance-design.com`)
6. Set **Authorized redirect URIs**:
   - `https://<YOUR_PRODUCTION_DOMAIN>/api/v1/auth/oidc/callback/google`
   - *(Optional staging URI)*: `https://staging.<YOUR_PRODUCTION_DOMAIN>/api/v1/auth/oidc/callback/google`
7. Click **Create**.
8. Securely record:
   - **Client ID** (`GOOGLE_CLIENT_ID`)
   - **Client Secret** (`GOOGLE_CLIENT_SECRET`)

---

## 3. Automated Live Verification (Phase 15)

The repository provides an automated verification script: [`scripts/verify-google-oidc-live.ps1`](file:///c:/my%20projects/interior%20design/scripts/verify-google-oidc-live.ps1).

### 3.1. Verification via Environment Variables
```powershell
$env:GOOGLE_CLIENT_ID = "<YOUR_GOOGLE_CLIENT_ID>.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET = "<YOUR_GOOGLE_CLIENT_SECRET>"
$env:GOOGLE_REDIRECT_URI = "https://<YOUR_PRODUCTION_DOMAIN>/api/v1/auth/oidc/callback/google"

powershell -ExecutionPolicy Bypass -File scripts/verify-google-oidc-live.ps1
```

### 3.2. Verification via Parameters with Strict Gate Enforcement
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-google-oidc-live.ps1 `
    -ClientId "<YOUR_GOOGLE_CLIENT_ID>.apps.googleusercontent.com" `
    -ClientSecret "<YOUR_GOOGLE_CLIENT_SECRET>" `
    -RedirectUri "https://<YOUR_PRODUCTION_DOMAIN>/api/v1/auth/oidc/callback/google" `
    -RequireLive
```

### 3.3. Expected Verification Output
When live credentials are provided:
1. Google OpenID Discovery document (`/.well-known/openid-configuration`) is fetched and verified.
2. Token endpoint (`https://oauth2.googleapis.com/token`) and Authorization endpoint (`https://accounts.google.com/o/oauth2/v2/auth`) are validated.
3. Google JWKS key set (`https://www.googleapis.com/oauth2/v3/certs`) is fetched and RSA public keys (`kid`, `n`, `e`) are validated.
4. Dummy authorization code exchange is tested against Google token endpoint, confirming receipt of expected HTTP 400 `invalid_grant` (proving Google endpoint connectivity, TLS handshake, and credential format recognition).
5. Exits code 0 with `GOOGLE OIDC LIVE CONNECTIVITY & CONFIGURATION: PASS`.

---

## 4. End-to-End Live Authorization Flow Verification

Following automated endpoint checks in Phase 15, conduct a live browser test:

1. **Initiate Login**:
   - Direct browser to: `https://<YOUR_PRODUCTION_DOMAIN>/api/v1/auth/oidc/login?provider=google&returnUrl=%2Fworkspace`
   - Observe HTTP 302 redirect to `https://accounts.google.com/o/oauth2/v2/auth` with query parameters:
     - `client_id` matching your configured Google Client ID.
     - `response_type=code`
     - `scope=openid+email+profile`
     - `code_challenge` (S256 base64url)
     - `code_challenge_method=S256`
     - `state` and `nonce` (32-byte secure random tokens)
2. **Authorize in Google**:
   - Authenticate with a test Google Account and approve consent.
3. **Inspect Callback Execution**:
   - Browser redirects to `/api/v1/auth/oidc/callback/google?code=...&state=...`.
   - Backend exchanges authorization code + PKCE verifier via server-to-server TLS connection to Google token endpoint.
   - Google ID token is validated for RS256 signature against Google JWKS.
   - Identity record is created or linked in `identity_external_identities` anchored by `(issuer, subject)`.
4. **Session & Destination Validation**:
   - Check response headers for `Set-Cookie`:
     - Name: `__Host-session` (production HTTPS) or `session`
     - Attributes: `HttpOnly; SameSite=Lax; Secure; Path=/`
   - Check destination redirect:
     - User is routed to `/account` (if Customer) or `/workspace` (if Designer with studio membership).
   - Check CSRF token response:
     - `X-CSRF-TOKEN` cookie/header is issued and active.

---

## 5. Security Posture & Troubleshooting

| Issue / Failure | Root Cause | Remediation |
| :--- | :--- | :--- |
| `redirect_uri_mismatch` (Google Error 400) | Redirect URI in request doesn't match Google Cloud Console | Ensure `GOOGLE_REDIRECT_URI` matches Authorized Redirect URI in Google Cloud Console exactly. |
| `invalid_client` (Google Error 401) | Incorrect Client ID or Secret | Verify Client ID and Secret in GCP Console; check for whitespace or truncated secret. |
| `AUTH_EMAIL_COLLISION_REJECTED` (Platform 409) | Google email matches existing local user without prior account linkage | Intentional anti-takeover defense. User must authenticate via local password or account linking flow. |
| `OIDC_ID_TOKEN_SIGNATURE_INVALID` | Google key rotation in progress or corrupted token | `DefaultJwksResolver` automatically flushes cache on unknown `kid`. Check server clock synchronization (NTP). |
| `OIDC_ID_TOKEN_EXPIRED` | Clock skew between server and Google | Synchronize host OS clock using NTP / Windows Time Service. Max tolerance is 60 seconds. |
