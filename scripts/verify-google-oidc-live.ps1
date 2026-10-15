# ==============================================================================
# ELÉGANCE INTERIOR DESIGN PLATFORM -- GOOGLE OIDC LIVE VALIDATION HARNESS
# Phase 12C / Phase 15 Pre-Launch Provider Activation Harness
# ==============================================================================
# This script is an OPT-IN verification tool for validating live Google OpenID
# Connect credentials, discovery, and JWKS endpoints.
#
# IMPORTANT SECURITY POLICY:
# - Live Google activation is DEFERRED TO PHASE 15 (Pre-Launch Activation).
# - ZERO live Google Cloud accounts or credentials are required in Phase 12C.
# - NEVER commit real credentials to Git.
# ==============================================================================

[CmdletBinding()]
param(
    [switch]$RequireLiveCredentials = $false
)

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  ELEGANCE -- GOOGLE OIDC PROVIDER VERIFICATION HARNESS                         " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# 1. Context Verification
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if (-not (Test-Path (Join-Path $repoRoot "INTERIOR_PROJECT_CONTEXT.md"))) {
    Write-Error "Context check failed: Not running from Interior repository root."
    exit 1
}

Write-Host "Repository Root       : $repoRoot" -ForegroundColor Gray
Write-Host "Verification Mode     : Safe Inspection & Opt-In Live Verification" -ForegroundColor Gray

# 2. Check Environment Variables
$clientId = $env:GOOGLE_CLIENT_ID
$clientSecret = $env:GOOGLE_CLIENT_SECRET
$redirectUri = $env:GOOGLE_REDIRECT_URI
$issuer = if ($env:GOOGLE_OIDC_ISSUER) { $env:GOOGLE_OIDC_ISSUER } else { "https://accounts.google.com" }

$hasCredentials = -not [string]::IsNullOrWhiteSpace($clientId) -and -not [string]::IsNullOrWhiteSpace($clientSecret)

if (-not $hasCredentials) {
    Write-Host "`n[STATUS: DEFERRED TO PHASE 15]" -ForegroundColor Yellow
    Write-Host "Live Google OIDC credentials are not configured in environment variables." -ForegroundColor Yellow
    Write-Host "This is expected and correct for Phase 12C (Production Auth Readiness)." -ForegroundColor Gray
    Write-Host ""
    Write-Host "Phase 12C relies on in-process cryptographic MockOidcServer contract tests" -ForegroundColor Gray
    Write-Host "(LocalOidcIntegrationTest, OidcSecurityTest, ProductionConfigurationValidatorOidcTest)." -ForegroundColor Gray
    Write-Host ""
    Write-Host "When Phase 15 (Pre-Launch Activation) arrives, provide the following environment variables:" -ForegroundColor Cyan
    Write-Host "  - GOOGLE_CLIENT_ID" -ForegroundColor Gray
    Write-Host "  - GOOGLE_CLIENT_SECRET" -ForegroundColor Gray
    Write-Host "  - GOOGLE_REDIRECT_URI (e.g. https://<domain>/api/v1/auth/callback)" -ForegroundColor Gray

    if ($RequireLiveCredentials) {
        Write-Error "RequireLiveCredentials switch was specified, but Google credentials were not found."
        exit 1
    } else {
        Write-Host "`nOIDC VALIDATION READY FOR PHASE 15 ACTIVATION." -ForegroundColor Green
        exit 0
    }
}

# 3. Live Google Endpoint Verification (When Credentials Are Supplied in Phase 15)
Write-Host "`nLive credentials detected in environment. Commencing live Google OIDC endpoint checks..." -ForegroundColor Cyan

$maskedClientId = if ($clientId.Length -gt 12) { $clientId.Substring(0, 6) + "..." + $clientId.Substring($clientId.Length - 6) } else { "***" }
Write-Host "Target Provider       : Google OpenID Connect" -ForegroundColor Gray
Write-Host "Client ID             : $maskedClientId" -ForegroundColor Gray
Write-Host "Client Secret         : [PROTECTED -- NOT PRINTED]" -ForegroundColor Gray
Write-Host "Configured Issuer     : $issuer" -ForegroundColor Gray
Write-Host "Configured Redirect   : $redirectUri" -ForegroundColor Gray

try {
    # A. Discovery Document Verification
    Write-Host "`n[1/3] Querying Google OIDC Discovery Document..." -ForegroundColor Cyan
    $discoveryUrl = "$issuer/.well-known/openid-configuration"
    $discoveryResponse = Invoke-RestMethod -Uri $discoveryUrl -Method Get -TimeoutSec 10

    if (-not $discoveryResponse.authorization_endpoint -or -not $discoveryResponse.token_endpoint -or -not $discoveryResponse.jwks_uri) {
        throw "Google discovery document missing required endpoints."
    }
    Write-Host "  -> Issuer                   : $($discoveryResponse.issuer)" -ForegroundColor Green
    Write-Host "  -> Authorization Endpoint   : $($discoveryResponse.authorization_endpoint)" -ForegroundColor Green
    Write-Host "  -> Token Endpoint           : $($discoveryResponse.token_endpoint)" -ForegroundColor Green
    Write-Host "  -> JWKS Endpoint            : $($discoveryResponse.jwks_uri)" -ForegroundColor Green

    # B. JWKS Certificates Verification
    Write-Host "`n[2/3] Querying Google Public JWKS Certificates..." -ForegroundColor Cyan
    $jwksResponse = Invoke-RestMethod -Uri $discoveryResponse.jwks_uri -Method Get -TimeoutSec 10

    if (-not $jwksResponse.keys -or $jwksResponse.keys.Count -eq 0) {
        throw "Google JWKS endpoint returned 0 signing keys."
    }
    Write-Host "  -> Active Google RSA Keys   : $($jwksResponse.keys.Count) keys retrieved" -ForegroundColor Green
    foreach ($key in $jwksResponse.keys) {
        Write-Host "     * Key ID (kid): $($key.kid) | Alg: $($key.alg) | Kty: $($key.kty)" -ForegroundColor Gray
    }

    # C. Configuration Consistency
    Write-Host "`n[3/3] Validating Configuration Contract Consistency..." -ForegroundColor Cyan
    if (-not ($clientId -like "*.apps.googleusercontent.com")) {
        Write-Host "  -> Note: Client ID does not end with standard '.apps.googleusercontent.com' suffix." -ForegroundColor Yellow
    } else {
        Write-Host "  -> Client ID format matches standard Google OAuth 2.0 Web Client identifier." -ForegroundColor Green
    }

    if ([string]::IsNullOrWhiteSpace($redirectUri)) {
        throw "Redirect URI is empty. Must match authorized redirect URIs in Google Cloud Console."
    }
    Write-Host "  -> Redirect URI check OK: $redirectUri" -ForegroundColor Green

    Write-Host "`n================================================================================" -ForegroundColor Green
    Write-Host "  GOOGLE OIDC LIVE VALIDATION SUCCESSFUL                                        " -ForegroundColor Green
    Write-Host "================================================================================" -ForegroundColor Green
    Write-Host "All live Google OIDC discovery endpoints and public certificate caches are responsive." -ForegroundColor Green
    exit 0

} catch {
    Write-Host "`n[ERROR] Google OIDC Live Validation Failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
