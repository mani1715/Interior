# Validate Production Environment Schema -- Elégance Interior Designer Platform
# Phase 14 Production Environment Verification Utility
param (
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RootDir = Split-Path -Parent $ScriptDir

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ELÉGANCE -- PRODUCTION ENVIRONMENT SCHEMA VALIDATOR       " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Mandatory Context Guard
$ContextScript = Join-Path $ScriptDir "verify-interior-context.ps1"
if (Test-Path $ContextScript) {
    & powershell -ExecutionPolicy Bypass -File $ContextScript
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Context verification guard failed." -ForegroundColor Red
        exit 1
    }
}

# 2. Check env file existence
$targetEnvPath = Join-Path $RootDir $EnvFile
if (-not (Test-Path $targetEnvPath)) {
    Write-Host "Target env file '$EnvFile' not found. Checking current environment variables..." -ForegroundColor Yellow
    $envMap = @{}
    foreach ($item in (Get-ChildItem env:)) {
        $envMap[$item.Name] = $item.Value
    }
} else {
    Write-Host "Parsing environment file: $targetEnvPath" -ForegroundColor Green
    $envMap = @{}
    Get-Content $targetEnvPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            if ($parts.Length -eq 2) {
                $envMap[$parts[0].Trim()] = $parts[1].Trim()
            }
        }
    }
}

function Is-Placeholder([string]$val) {
    if ([string]::IsNullOrWhiteSpace($val)) { return $true }
    $lower = $val.ToLower().Trim()
    return ($lower.StartsWith("your-") -or 
            $lower.Contains("placeholder") -or 
            $lower.Contains("replace_me") -or 
            $lower.Equals("change-me") -or
            $lower.Equals("changeme"))
}

function Mask-Secret([string]$val) {
    if ([string]::IsNullOrWhiteSpace($val)) { return "<EMPTY>" }
    if ($val.Length -le 4) { return "****" }
    return ($val.Substring(0, 2) + "****" + $val.Substring($val.Length - 2, 2))
}

$errors = 0
$warnings = 0

Write-Host "`nAuditing Core Invariants..." -ForegroundColor Cyan

# 1. Node / Web
$nodeEnv = $envMap["NODE_ENV"]
if ($nodeEnv -ne "production") {
    Write-Host "  [FAIL] NODE_ENV must be 'production' (found: '$nodeEnv')" -ForegroundColor Red
    $errors++
} else {
    Write-Host "  [OK]   NODE_ENV = production" -ForegroundColor Green
}

# 2. Spring Profile
$springProfiles = $envMap["SPRING_PROFILES_ACTIVE"]
if ($springProfiles -notlike "*production*") {
    Write-Host "  [FAIL] SPRING_PROFILES_ACTIVE must include 'production' (found: '$springProfiles')" -ForegroundColor Red
    $errors++
} else {
    Write-Host "  [OK]   SPRING_PROFILES_ACTIVE = $springProfiles" -ForegroundColor Green
}

# 3. Dev Auth must be false
$devAuth = $envMap["APP_SECURITY_DEV_AUTH_ENABLED"]
if ($devAuth -ne "false") {
    Write-Host "  [FAIL] APP_SECURITY_DEV_AUTH_ENABLED must be 'false' in production (found: '$devAuth')" -ForegroundColor Red
    $errors++
} else {
    Write-Host "  [OK]   APP_SECURITY_DEV_AUTH_ENABLED = false" -ForegroundColor Green
}

# 4. Session Cookie Secure
$cookieSecure = $envMap["APP_SECURITY_SESSION_COOKIE_SECURE"]
if ($cookieSecure -ne "true") {
    Write-Host "  [FAIL] APP_SECURITY_SESSION_COOKIE_SECURE must be 'true' in production (found: '$cookieSecure')" -ForegroundColor Red
    $errors++
} else {
    Write-Host "  [OK]   APP_SECURITY_SESSION_COOKIE_SECURE = true" -ForegroundColor Green
}

# 5. Database URL & Password
$dbUrl = $envMap["SPRING_DATASOURCE_URL"]
if ([string]::IsNullOrWhiteSpace($dbUrl) -or $dbUrl.StartsWith("jdbc:h2:")) {
    Write-Host "  [FAIL] SPRING_DATASOURCE_URL is missing or using in-memory H2" -ForegroundColor Red
    $errors++
} else {
    Write-Host "  [OK]   SPRING_DATASOURCE_URL is configured" -ForegroundColor Green
    if ($dbUrl -notlike "*sslmode=require*" -and $dbUrl -notlike "*ssl=true*" -and $dbUrl -notlike "*localhost*") {
        Write-Host "  [WARN] Database connection does not explicitly specify sslmode=require" -ForegroundColor Yellow
        $warnings++
    }
}

$dbPass = $envMap["SPRING_DATASOURCE_PASSWORD"]
if ([string]::IsNullOrWhiteSpace($dbPass) -or (Is-Placeholder $dbPass)) {
    Write-Host "  [FAIL] SPRING_DATASOURCE_PASSWORD is empty or using placeholder" -ForegroundColor Red
    $errors++
} else {
    Write-Host "  [OK]   SPRING_DATASOURCE_PASSWORD is set (masked: $(Mask-Secret $dbPass))" -ForegroundColor Green
}

# 6. Check Active Providers & Fail-Closed Behavior
$aiProv = if ($envMap["AI_PROVIDER"]) { $envMap["AI_PROVIDER"].ToLower() } else { "none" }
if ($aiProv -eq "mock" -or $aiProv -eq "fake") {
    Write-Host "  [FAIL] AI_PROVIDER '$aiProv' is forbidden in production" -ForegroundColor Red
    $errors++
} elseif ($aiProv -ne "none") {
    $aiKey = $envMap["AI_API_KEY"]
    if (Is-Placeholder $aiKey) {
        Write-Host "  [FAIL] AI_API_KEY is missing or placeholder for active provider '$aiProv'" -ForegroundColor Red
        $errors++
    }
}

$emailProv = if ($envMap["EMAIL_PROVIDER"]) { $envMap["EMAIL_PROVIDER"].ToLower() } else { "disabled" }
if ($emailProv -eq "mock" -or $emailProv -eq "fake") {
    Write-Host "  [FAIL] EMAIL_PROVIDER '$emailProv' is forbidden in production" -ForegroundColor Red
    $errors++
} elseif ($emailProv -eq "resend") {
    if (Is-Placeholder $envMap["RESEND_API_KEY"]) {
        Write-Host "  [FAIL] RESEND_API_KEY is placeholder for active Resend provider" -ForegroundColor Red
        $errors++
    }
}

$billingProv = if ($envMap["BILLING_PROVIDER"]) { $envMap["BILLING_PROVIDER"].ToLower() } else { "disabled" }
if ($billingProv -eq "mock" -or $billingProv -eq "fake") {
    Write-Host "  [FAIL] BILLING_PROVIDER '$billingProv' is forbidden in production" -ForegroundColor Red
    $errors++
}

Write-Host "`n------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Validation Summary: $errors Errors, $warnings Warnings" -ForegroundColor $(if ($errors -eq 0) { "Green" } else { "Red" })
if ($errors -gt 0) {
    Write-Host "Result: PRODUCTION ENVIRONMENT VALIDATION FAILED" -ForegroundColor Red
    exit 1
} else {
    Write-Host "Result: PRODUCTION ENVIRONMENT SCHEMA VALIDATED" -ForegroundColor Green
    exit 0
}
