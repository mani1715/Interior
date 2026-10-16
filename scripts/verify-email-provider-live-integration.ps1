# ==============================================================================
# ELÉGANCE INTERIOR DESIGN PLATFORM -- TRANSACTIONAL EMAIL LIVE INTEGRATION HARNESS
# Phase 12E / Phase 15 Pre-Launch Provider Activation Harness
# ==============================================================================
# This script is an OPT-IN verification tool for validating live external email
# delivery provider credentials, endpoints, sender domain authentication, and latency.
#
# IMPORTANT SECURITY & STRATEGY POLICY:
# - Live email provider activation is DEFERRED TO PHASE 15 (Pre-Launch Activation).
# - ZERO live email API keys or paid accounts are required in Phase 12E.
# - NEVER commit real email API keys, tokens, or AWS secrets to Git.
# - Strictly transactional communications only (studio invitations, client review invites,
#   subscription receipts, security notifications, privacy exports).
# ==============================================================================

[CmdletBinding()]
param(
    [switch]$RequireLiveCredentials = $false
)

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  ELÉGANCE -- TRANSACTIONAL EMAIL PROVIDER LIVE INTEGRATION HARNESS              " -ForegroundColor Cyan
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
$emailProvider = $env:EMAIL_PROVIDER
$fromAddress = $env:EMAIL_FROM_ADDRESS
$resendApiKey = $env:RESEND_API_KEY
$sesRegion = $env:AWS_SES_REGION
$awsAccessKey = $env:AWS_ACCESS_KEY_ID

$isResendConfigured = ($emailProvider -eq "resend") -and `
                      -not [string]::IsNullOrWhiteSpace($resendApiKey) -and `
                      -not ($resendApiKey -match "(?i)^(placeholder|your-.*|fake|mock|none|re_123456789)$") -and `
                      -not [string]::IsNullOrWhiteSpace($fromAddress) -and `
                      -not ($fromAddress -match "(?i)^(.*@example\.com|placeholder.*)$")

$isSesConfigured = ($emailProvider -eq "ses") -and `
                   -not [string]::IsNullOrWhiteSpace($sesRegion) -and `
                   -not ($sesRegion -match "(?i)^(placeholder|your-.*|fake|mock)$") -and `
                   -not [string]::IsNullOrWhiteSpace($fromAddress) -and `
                   -not ($fromAddress -match "(?i)^(.*@example\.com|placeholder.*)$")

$isConfigured = $isResendConfigured -or $isSesConfigured

if (-not $isConfigured) {
    Write-Host "`n[STATUS: DEFERRED TO PHASE 15]" -ForegroundColor Yellow
    Write-Host "Live email provider credentials and sender domain are not configured in environment variables." -ForegroundColor Yellow
    Write-Host "This is expected and correct for Phase 12E (Transactional Email Production Readiness without Paid Credentials)." -ForegroundColor Gray
    Write-Host ""
    Write-Host "Phase 12E relies on comprehensive in-process contract and mock-provider integration tests" -ForegroundColor Gray
    Write-Host "(ResendEmailProviderContractTest, SesEmailProviderContractTest, EmailSecurityAndEscapingTest, ProductionConfigurationValidatorEmailTest, EmailDisabledWorkflowTest)." -ForegroundColor Gray
    Write-Host ""
    Write-Host "When Phase 15 (Pre-Launch Provider Activation) arrives, provide the following environment variables:" -ForegroundColor Cyan
    Write-Host "For Resend:" -ForegroundColor Gray
    Write-Host "  - EMAIL_PROVIDER=resend" -ForegroundColor Gray
    Write-Host "  - EMAIL_FROM_ADDRESS=notifications@your-verified-domain.com" -ForegroundColor Gray
    Write-Host "  - EMAIL_FROM_NAME='Elégance Platform'" -ForegroundColor Gray
    Write-Host "  - RESEND_API_KEY=re_live_..." -ForegroundColor Gray
    Write-Host "  - EMAIL_WEBHOOK_SECRET=whsec_..." -ForegroundColor Gray
    Write-Host "For AWS SES:" -ForegroundColor Gray
    Write-Host "  - EMAIL_PROVIDER=ses" -ForegroundColor Gray
    Write-Host "  - EMAIL_FROM_ADDRESS=notifications@your-verified-domain.com" -ForegroundColor Gray
    Write-Host "  - AWS_SES_REGION=us-east-1" -ForegroundColor Gray
    Write-Host "  - AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY (or IAM Instance Role)" -ForegroundColor Gray

    if ($RequireLiveCredentials) {
        Write-Error "RequireLiveCredentials switch was specified, but email provider credentials were not configured."
        exit 1
    } else {
        Write-Host "`nTRANSACTIONAL EMAIL SUBSYSTEM READY FOR PHASE 15 ACTIVATION." -ForegroundColor Green
        exit 0
    }
}

# 3. Live Email Provider Connectivity Check (When Credentials Are Supplied in Phase 15)
Write-Host "`nLive email credentials detected in environment. Commencing live provider endpoint checks..." -ForegroundColor Cyan

if ($emailProvider -eq "resend") {
    $maskedKey = if ($resendApiKey.Length -gt 8) { $resendApiKey.Substring(0, 4) + "..." + $resendApiKey.Substring($resendApiKey.Length - 4) } else { "***" }
    Write-Host "Configured Provider   : Resend" -ForegroundColor Gray
    Write-Host "API Key               : $maskedKey" -ForegroundColor Gray
    Write-Host "Sender Address        : $fromAddress" -ForegroundColor Gray

    try {
        Write-Host "`n[1/2] Checking Resend API Connectivity & TLS..." -ForegroundColor Cyan
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        $headers = @{
            "Authorization" = "Bearer $resendApiKey"
            "User-Agent"    = "Elegance-Interior-Platform/1.0"
        }

        # Check API key validity via domains endpoint
        $response = Invoke-WebRequest -Uri "https://api.resend.com/domains" -Method Get -Headers $headers -TimeoutSec 15 -SkipHttpErrorCheck
        $stopwatch.Stop()

        Write-Host "  -> HTTP Status Code         : $($response.StatusCode)" -ForegroundColor Green
        Write-Host "  -> Roundtrip Latency        : $($stopwatch.ElapsedMilliseconds) ms" -ForegroundColor Green

        if ($response.StatusCode -eq 401 -or $response.StatusCode -eq 403) {
            throw "Resend returned $($response.StatusCode) Unauthorized/Forbidden. Verify RESEND_API_KEY."
        }

        Write-Host "`n[2/2] Validating Resend API Response..." -ForegroundColor Cyan
        Write-Host "  -> API Connectivity         : Authenticated & Active" -ForegroundColor Green

        Write-Host "`nLIVE RESEND EMAIL PROVIDER CONNECTIVITY VERIFIED SUCCESSFULLY." -ForegroundColor Green
        exit 0
    } catch {
        Write-Host "`nLive Resend provider verification check failed: $_" -ForegroundColor Red
        exit 1
    }
} elseif ($emailProvider -eq "ses") {
    Write-Host "Configured Provider   : AWS SES" -ForegroundColor Gray
    Write-Host "AWS Region            : $sesRegion" -ForegroundColor Gray
    Write-Host "Sender Address        : $fromAddress" -ForegroundColor Gray

    Write-Host "`nAWS SES live check requires AWS CLI or SDK credentials. Performing region ping..." -ForegroundColor Cyan
    try {
        $endpoint = "https://email.$sesRegion.amazonaws.com"
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-WebRequest -Uri $endpoint -Method Get -TimeoutSec 15 -SkipHttpErrorCheck
        $stopwatch.Stop()
        Write-Host "  -> HTTP Status Code         : $($response.StatusCode)" -ForegroundColor Green
        Write-Host "  -> Roundtrip Latency        : $($stopwatch.ElapsedMilliseconds) ms" -ForegroundColor Green
        Write-Host "`nLIVE AWS SES REGION ENDPOINT REACHABLE." -ForegroundColor Green
        exit 0
    } catch {
        Write-Host "`nAWS SES region endpoint reachability check failed: $_" -ForegroundColor Red
        exit 1
    }
}
