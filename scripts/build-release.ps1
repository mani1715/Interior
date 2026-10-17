# Build Release Package -- Elégance Interior Designer Platform
# Phase 14 Production Release Packaging & OCI Image Build
param (
    [switch]$SkipContainers = $false
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RootDir = Split-Path -Parent $ScriptDir

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ELÉGANCE -- PRODUCTION RELEASE BUILD HARNESS              " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Mandatory Context Guard
$ContextScript = Join-Path $ScriptDir "verify-interior-context.ps1"
if (Test-Path $ContextScript) {
    & powershell -ExecutionPolicy Bypass -File $ContextScript
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Context verification guard failed. Aborting release build." -ForegroundColor Red
        exit 1
    }
}

# 2. Extract Git Commit SHA
$gitSha = (git rev-parse HEAD).Trim()
$shortSha = $gitSha.Substring(0, 7)
Write-Host "Release Target Git Commit SHA: $gitSha ($shortSha)" -ForegroundColor Green

# 3. Build Backend API JAR
Write-Host "`n[1/4] Building Backend Executable JAR via Maven Wrapper..." -ForegroundColor Yellow
Push-Location "$RootDir/apps/api"
try {
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Maven clean package failed." -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

$jarPath = Join-Path $RootDir "apps/api/target/platform-api-0.1.0-SNAPSHOT.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: Packaged JAR not found at $jarPath" -ForegroundColor Red
    exit 1
}
$jarHash = (Get-FileHash -Path $jarPath -Algorithm SHA256).Hash
Write-Host "Backend JAR Build Success: $jarPath" -ForegroundColor Green
Write-Host "Backend SHA-256 Checksum: $jarHash" -ForegroundColor Cyan

# 4. Build Frontend Web Standalone Bundle
Write-Host "`n[2/4] Building Frontend Next.js Standalone Bundle..." -ForegroundColor Yellow
Push-Location $RootDir
try {
    & npm --prefix apps/web run build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Next.js standalone build failed." -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

$standalonePath = Join-Path $RootDir "apps/web/.next/standalone"
if (-not (Test-Path $standalonePath)) {
    Write-Host "ERROR: Next.js standalone output directory not found at $standalonePath" -ForegroundColor Red
    exit 1
}
Write-Host "Frontend Standalone Build Success: $standalonePath" -ForegroundColor Green

# 5. Build OCI Container Images
if (-not $SkipContainers) {
    Write-Host "`n[3/4] Building Backend OCI Container Image..." -ForegroundColor Yellow
    & docker build -t "interior-platform-api:latest" -t "interior-platform-api:$shortSha" -f "$RootDir/apps/api/Dockerfile" "$RootDir/apps/api"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Backend Docker build failed." -ForegroundColor Red
        exit 1
    }
    Write-Host "Backend Docker image built: interior-platform-api:$shortSha" -ForegroundColor Green

    Write-Host "`n[4/4] Building Frontend OCI Container Image..." -ForegroundColor Yellow
    & docker build -t "interior-platform-web:latest" -t "interior-platform-web:$shortSha" -f "$RootDir/apps/web/Dockerfile" "$RootDir"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Frontend Docker build failed." -ForegroundColor Red
        exit 1
    }
    Write-Host "Frontend Docker image built: interior-platform-web:$shortSha" -ForegroundColor Green
} else {
    Write-Host "`n[3/4 & 4/4] Skipping container build (-SkipContainers requested)." -ForegroundColor Gray
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  PRODUCTION RELEASE BUILD COMPLETE & VERIFIED              " -ForegroundColor Green
Write-Host "  Commit SHA : $gitSha" -ForegroundColor Cyan
Write-Host "  Jar SHA256 : $jarHash" -ForegroundColor Cyan
Write-Host "  Policy     : IMMUTABLE RELEASE PACKAGE - NO DEPLOYMENT   " -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
exit 0
