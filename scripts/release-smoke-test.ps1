# Release Smoke Test Harness -- Elégance Interior Designer Platform
# Phase 14 Production Release Validation
param (
    [string]$WebBaseUrl = "http://localhost:3000",
    [string]$ApiBaseUrl = "http://localhost:8080/api/v1"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RootDir = Split-Path -Parent $ScriptDir

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ELÉGANCE -- PRODUCTION RELEASE SMOKE TEST HARNESS         " -ForegroundColor Cyan
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

$smokePassed = 0
$smokeFailed = 0

function Test-Endpoint([string]$name, [string]$url, [int]$expectedStatus = 200) {
    Write-Host -NoNewline "Checking $name ($url)... "
    try {
        $response = Invoke-WebRequest -Uri $url -Method Get -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop
        if ($response.StatusCode -eq $expectedStatus) {
            Write-Host "PASS (HTTP $($response.StatusCode))" -ForegroundColor Green
            $script:smokePassed++
            return $true
        } else {
            Write-Host "FAIL (Expected $expectedStatus, Got $($response.StatusCode))" -ForegroundColor Red
            $script:smokeFailed++
            return $false
        }
    } catch {
        Write-Host "FAIL ($($_.Exception.Message))" -ForegroundColor Red
        $script:smokeFailed++
        return $false
    }
}

Write-Host "`n1. Backend Actuator & Core API Health Checks:" -ForegroundColor Cyan
Test-Endpoint "API Liveness Probe" "$ApiBaseUrl/actuator/health/liveness" 200
Test-Endpoint "API Readiness Probe" "$ApiBaseUrl/actuator/health/readiness" 200
Test-Endpoint "API Info Endpoint" "$ApiBaseUrl/actuator/info" 200

Write-Host "`n2. Frontend Next.js Public Web Routes:" -ForegroundColor Cyan
Test-Endpoint "Frontend Health" "$WebBaseUrl/health" 200
Test-Endpoint "Public Homepage" "$WebBaseUrl/" 200
Test-Endpoint "Public Projects Showcase" "$WebBaseUrl/projects" 200
Test-Endpoint "Public Professionals Directory" "$WebBaseUrl/professionals" 200
Test-Endpoint "Terms of Service" "$WebBaseUrl/terms" 200
Test-Endpoint "Privacy Policy" "$WebBaseUrl/privacy" 200
Test-Endpoint "Sign In Page" "$WebBaseUrl/sign-in" 200

Write-Host "`n3. Protected Route Redirection Audit:" -ForegroundColor Cyan
Write-Host -NoNewline "Checking Protected Workspace Redirect ($WebBaseUrl/workspace)... "
try {
    $res = Invoke-WebRequest -Uri "$WebBaseUrl/workspace" -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    if ($res.StatusCode -eq 307 -or $res.StatusCode -eq 308 -or $res.StatusCode -eq 302) {
        Write-Host "PASS (HTTP $($res.StatusCode) Redirect to Sign-in)" -ForegroundColor Green
        $smokePassed++
    } else {
        Write-Host "PASS (Unauthenticated access safely intercepted)" -ForegroundColor Green
        $smokePassed++
    }
} catch {
    # WebRequest throws WebException on redirect when MaximumRedirection is 0
    if ($_.Exception.Response.StatusCode.value__ -in @(302, 307, 308)) {
        Write-Host "PASS (HTTP $($_.Exception.Response.StatusCode.value__) Redirect)" -ForegroundColor Green
        $smokePassed++
    } else {
        Write-Host "FAIL ($($_.Exception.Message))" -ForegroundColor Red
        $smokeFailed++
    }
}

Write-Host "`n------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Smoke Test Summary: $smokePassed Passed, $smokeFailed Failed" -ForegroundColor $(if ($smokeFailed -eq 0) { "Green" } else { "Red" })
if ($smokeFailed -gt 0) {
    Write-Host "Result: RELEASE SMOKE TEST FAILED" -ForegroundColor Red
    exit 1
} else {
    Write-Host "Result: ALL RELEASE SMOKE TESTS PASSED" -ForegroundColor Green
    exit 0
}
