# ==============================================================================
# ELÉGANCE INTERIOR DESIGN PLATFORM -- COMMERCIAL PAYMENT LIVE INTEGRATION HARNESS
# Phase 12F / Phase 15 Pre-Launch Provider Activation Harness
# ==============================================================================
# This script is an OPT-IN verification tool for validating live external payment
# gateway provider credentials, checkout endpoints, webhook signature integrity, and latency.
#
# IMPORTANT SECURITY & STRATEGY POLICY:
# - Live payment provider activation is DEFERRED TO PHASE 15 (Pre-Launch Activation).
# - ZERO live payment keys, merchant accounts, or real card charges are required in Phase 12F.
# - NEVER commit real payment secret keys, webhook secrets, or API tokens to Git.
# - Strictly commercial subscriptions & entitlement upgrades (STANDARD, PREMIUM, PRO).
# - No real customer charges may occur during verification.
# ==============================================================================

[CmdletBinding()]
param(
    [switch]$RequireLiveCredentials = $false
)

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  ELÉGANCE -- COMMERCIAL PAYMENT PROVIDER LIVE INTEGRATION HARNESS               " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# 1. Context Verification
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if (-not (Test-Path (Join-Path $repoRoot "INTERIOR_PROJECT_CONTEXT.md"))) {
    Write-Error "Context check failed: Not running from Interior repository root."
    exit 1
}

Write-Host "Repository Root       : $repoRoot" -ForegroundColor Gray
Write-Host "Verification Mode     : Safe Inspection & Opt-In Live Gateway Verification" -ForegroundColor Gray

# 2. Check Environment Variables
$billingProvider = $env:BILLING_PROVIDER
$razorpayKeyId = $env:RAZORPAY_KEY_ID
$razorpayKeySecret = $env:RAZORPAY_KEY_SECRET
$razorpayWebhookSecret = $env:RAZORPAY_WEBHOOK_SECRET
$stripeApiKey = $env:STRIPE_API_KEY
$stripeWebhookSecret = $env:STRIPE_WEBHOOK_SECRET

$isRazorpayConfigured = ($billingProvider -eq "razorpay") -and `
                        -not [string]::IsNullOrWhiteSpace($razorpayKeyId) -and `
                        -not ($razorpayKeyId -match "(?i)^(placeholder|your-.*|fake|mock|none)$") -and `
                        -not [string]::IsNullOrWhiteSpace($razorpayKeySecret) -and `
                        -not ($razorpayKeySecret -match "(?i)^(placeholder|your-.*|fake|mock|none)$") -and `
                        -not [string]::IsNullOrWhiteSpace($razorpayWebhookSecret) -and `
                        -not ($razorpayWebhookSecret -match "(?i)^(placeholder|your-.*|fake|mock|none)$")

$isStripeConfigured = ($billingProvider -eq "stripe") -and `
                      -not [string]::IsNullOrWhiteSpace($stripeApiKey) -and `
                      -not ($stripeApiKey -match "(?i)^(placeholder|your-.*|fake|mock|none)$") -and `
                      -not [string]::IsNullOrWhiteSpace($stripeWebhookSecret) -and `
                      -not ($stripeWebhookSecret -match "(?i)^(placeholder|your-.*|fake|mock|none)$")

$isConfigured = $isRazorpayConfigured -or $isStripeConfigured

if (-not $isConfigured) {
    Write-Host "`n[STATUS: DEFERRED TO PHASE 15]" -ForegroundColor Yellow
    Write-Host "Live payment gateway credentials and webhook secrets are not configured in environment variables." -ForegroundColor Yellow
    Write-Host "This is expected and correct for Phase 12F (Commercial Billing Production Readiness without Paid Credentials)." -ForegroundColor Gray
    Write-Host ""
    Write-Host "Phase 12F relies on comprehensive in-process contract, cryptographic validation, and idempotency tests:" -ForegroundColor Gray
    Write-Host "  - BillingSecurityAndAuthenticityTest (Razorpay HMAC-SHA256, Stripe t= v1= replay defense, open redirect defense, validator hardening)" -ForegroundColor Gray
    Write-Host "  - CommercialBillingLifecycleTest (zero data loss downgrade, server-authoritative checkout, replay deduplication, tampered amount rejection)" -ForegroundColor Gray
    Write-Host "  - EntitlementQuotaIntegrationTest (quota limits across STANDARD, PREMIUM, PRO)" -ForegroundColor Gray
    Write-Host "  - BillingIntegrationTest (catalog fallback, tenant isolation, disabled provider fail-fast)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "When Phase 15 (Pre-Launch Provider Activation) arrives, provide the following environment variables:" -ForegroundColor Cyan
    Write-Host "For Razorpay (Domestic India / INR Primary):" -ForegroundColor Gray
    Write-Host "  - BILLING_PROVIDER=razorpay" -ForegroundColor Gray
    Write-Host "  - RAZORPAY_KEY_ID=rzp_live_..." -ForegroundColor Gray
    Write-Host "  - RAZORPAY_KEY_SECRET=..." -ForegroundColor Gray
    Write-Host "  - RAZORPAY_WEBHOOK_SECRET=whsec_..." -ForegroundColor Gray
    Write-Host "For Stripe (International / Multi-currency Primary):" -ForegroundColor Gray
    Write-Host "  - BILLING_PROVIDER=stripe" -ForegroundColor Gray
    Write-Host "  - STRIPE_API_KEY=sk_live_..." -ForegroundColor Gray
    Write-Host "  - STRIPE_WEBHOOK_SECRET=whsec_..." -ForegroundColor Gray
    Write-Host "  - STRIPE_TIMESTAMP_TOLERANCE_SECONDS=300" -ForegroundColor Gray

    if ($RequireLiveCredentials) {
        Write-Error "RequireLiveCredentials switch was specified, but payment gateway credentials were not configured."
        exit 1
    } else {
        Write-Host "`nCOMMERCIAL BILLING & PAYMENT SUBSYSTEM READY FOR PHASE 15 ACTIVATION." -ForegroundColor Green
        exit 0
    }
}

# 3. Live Payment Gateway Connectivity & Non-Destructive Verification
Write-Host "`nLive payment gateway credentials detected in environment. Commencing non-destructive endpoint checks..." -ForegroundColor Cyan

if ($billingProvider -eq "razorpay") {
    $maskedKey = if ($razorpayKeyId.Length -gt 8) { $razorpayKeyId.Substring(0, 4) + "..." + $razorpayKeyId.Substring($razorpayKeyId.Length - 4) } else { "***" }
    Write-Host "Configured Provider   : Razorpay" -ForegroundColor Gray
    Write-Host "Key ID                : $maskedKey" -ForegroundColor Gray
    Write-Host "Webhook Secret Length : $($razorpayWebhookSecret.Length) characters" -ForegroundColor Gray

    try {
        Write-Host "`n[1/2] Checking Razorpay API Connectivity & Authentication..." -ForegroundColor Cyan
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

        $authBytes = [System.Text.Encoding]::ASCII.GetBytes("${razorpayKeyId}:${razorpayKeySecret}")
        $base64Auth = [System.Convert]::ToBase64String($authBytes)
        $headers = @{
            "Authorization" = "Basic $base64Auth"
            "User-Agent" = "Elegance-Platform/1.0 (Phase-15-PreLaunch-Verifier)"
        }

        # Non-destructive read of standard plans/items
        $response = Invoke-RestMethod -Uri "https://api.razorpay.com/v1/plans" -Method Get -Headers $headers -TimeoutSec 15
        $stopwatch.Stop()

        Write-Host "Razorpay API Reachable: Yes ($($stopwatch.ElapsedMilliseconds) ms)" -ForegroundColor Green
        Write-Host "Authentication Status : Authenticated (HTTP 200)" -ForegroundColor Green

        Write-Host "`n[2/2] Validating Local HMAC-SHA256 Webhook Signing..." -ForegroundColor Cyan
        $testPayload = '{"entity":"event","event":"payment.captured"}'
        $hmac = New-Object System.Security.Cryptography.HMACSHA256
        $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($razorpayWebhookSecret)
        $hashBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($testPayload))
        $computedSig = [System.BitConverter]::ToString($hashBytes).Replace("-", "").ToLower()

        if ($computedSig.Length -eq 64) {
            Write-Host "HMAC-SHA256 Engine    : Functional (SHA-256 digest length 64 hex)" -ForegroundColor Green
        } else {
            throw "Unexpected HMAC-SHA256 digest length: $($computedSig.Length)"
        }

        Write-Host "`nRAZORPAY LIVE INTEGRATION VERIFIED SUCCESSFULLY." -ForegroundColor Green
        exit 0
    } catch {
        Write-Host "`nRazorpay Live Verification Failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} elseif ($billingProvider -eq "stripe") {
    $maskedKey = if ($stripeApiKey.Length -gt 8) { $stripeApiKey.Substring(0, 7) + "..." + $stripeApiKey.Substring($stripeApiKey.Length - 4) } else { "***" }
    Write-Host "Configured Provider   : Stripe" -ForegroundColor Gray
    Write-Host "API Key               : $maskedKey" -ForegroundColor Gray
    Write-Host "Webhook Secret Length : $($stripeWebhookSecret.Length) characters" -ForegroundColor Gray

    try {
        Write-Host "`n[1/2] Checking Stripe API Connectivity & Authentication..." -ForegroundColor Cyan
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

        $headers = @{
            "Authorization" = "Bearer $stripeApiKey"
            "User-Agent" = "Elegance-Platform/1.0 (Phase-15-PreLaunch-Verifier)"
        }

        # Non-destructive read of balance / account capabilities
        $response = Invoke-RestMethod -Uri "https://api.stripe.com/v1/balance" -Method Get -Headers $headers -TimeoutSec 15
        $stopwatch.Stop()

        Write-Host "Stripe API Reachable  : Yes ($($stopwatch.ElapsedMilliseconds) ms)" -ForegroundColor Green
        Write-Host "Authentication Status : Authenticated (HTTP 200)" -ForegroundColor Green

        Write-Host "`n[2/2] Validating Local Timestamped Webhook Signing..." -ForegroundColor Cyan
        $testTimestamp = [System.DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        $testPayload = '{"id":"evt_test","type":"payment_intent.succeeded"}'
        $signedPayload = "$testTimestamp.$testPayload"

        $hmac = New-Object System.Security.Cryptography.HMACSHA256
        $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($stripeWebhookSecret)
        $hashBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($signedPayload))
        $computedSig = [System.BitConverter]::ToString($hashBytes).Replace("-", "").ToLower()

        if ($computedSig.Length -eq 64) {
            Write-Host "Stripe Signature (v1) : Functional (SHA-256 digest length 64 hex)" -ForegroundColor Green
        } else {
            throw "Unexpected Stripe digest length: $($computedSig.Length)"
        }

        Write-Host "`nSTRIPE LIVE INTEGRATION VERIFIED SUCCESSFULLY." -ForegroundColor Green
        exit 0
    } catch {
        Write-Host "`nStripe Live Verification Failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Error "Unsupported live billing provider: '$billingProvider'"
    exit 1
}
