# PostgreSQL Logical Backup Script -- Elégance Interior Designer Platform
# Phase 12B Production Readiness
param (
    [string]$Database = $(if ($env:DB_NAME) { $env:DB_NAME } else { "interior_design_dev" }),
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }),
    [int]$Port = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 5433 }),
    [string]$User = $(if ($env:DB_USER) { $env:DB_USER } else { "postgres" }),
    [string]$Password = $(if ($env:PGPASSWORD) { $env:PGPASSWORD } elseif ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }),
    [string]$OutputDir = "storage/backups",
    [string]$Format = "custom" # custom (-Fc) or directory or plain
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RootDir = Split-Path -Parent $ScriptDir

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ELEGANCE -- POSTGRESQL LOGICAL BACKUP UTILITY              " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Mandatory Context Guard
$ContextScript = Join-Path $ScriptDir "verify-interior-context.ps1"
if (Test-Path $ContextScript) {
    & powershell -ExecutionPolicy Bypass -File $ContextScript
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Context verification failed." -ForegroundColor Red
        exit 1
    }
}

# 2. Locate pg_dump binary
$pgDumpPath = $null
if (Get-Command pg_dump -ErrorAction SilentlyContinue) {
    $pgDumpPath = (Get-Command pg_dump).Source
} else {
    $candidates = @(
        "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe",
        "C:\Program Files\PostgreSQL\17\bin\pg_dump.exe",
        "C:\Program Files\PostgreSQL\16\bin\pg_dump.exe"
    )
    foreach ($cand in $candidates) {
        if (Test-Path $cand) {
            $pgDumpPath = $cand
            break
        }
    }
}

if (-not $pgDumpPath) {
    Write-Host "ERROR: pg_dump utility not found. Please install PostgreSQL client tools or add to PATH." -ForegroundColor Red
    exit 1
}

# 3. Prepare output path
$resolvedOutputDir = Join-Path $RootDir $OutputDir
if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path $resolvedOutputDir "backup_${Database}_${timestamp}.dump"

Write-Host "`nBackup Parameters:" -ForegroundColor Yellow
Write-Host "  Database   : $Database"
Write-Host "  Host:Port  : ${DbHost}:${Port}"
Write-Host "  User       : $User"
Write-Host "  Password   : [MASKED]"
Write-Host "  Binary     : $pgDumpPath"
Write-Host "  Target File: $backupFile"
Write-Host "  Format     : $Format (PostgreSQL Custom Compressed -Fc)"

# 4. Execute pg_dump
$env:PGPASSWORD = $Password
$sw = [System.Diagnostics.Stopwatch]::StartNew()

try {
    Write-Host "`nStarting pg_dump execution..." -ForegroundColor Yellow
    
    # -Fc: Custom compressed format
    # --no-owner: Portable across environments
    # --no-acl: Prevent permission conflicts on restore
    & $pgDumpPath -h $DbHost -p $Port -U $User -d $Database -Fc --no-owner --no-acl -f $backupFile
    
    $sw.Stop()
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: pg_dump failed with exit code $LASTEXITCODE." -ForegroundColor Red
        if (Test-Path $backupFile) { Remove-Item $backupFile -Force }
        exit 1
    }
    
    if (-not (Test-Path $backupFile)) {
        Write-Host "ERROR: Expected backup file was not created." -ForegroundColor Red
        exit 1
    }
    
    $fileInfo = Get-Item $backupFile
    $sizeKb = [math]::Round($fileInfo.Length / 1KB, 2)
    
    Write-Host "`n============================================================" -ForegroundColor Green
    Write-Host "  BACKUP COMPLETED SUCCESSFULLY                              " -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  Output File: $backupFile" -ForegroundColor Green
    Write-Host "  File Size  : $sizeKb KB ($($fileInfo.Length) bytes)" -ForegroundColor Green
    Write-Host "  Duration   : $($sw.ElapsedMilliseconds) ms" -ForegroundColor Green
    
    # Output file path for callers
    return $backupFile
} catch {
    Write-Host "ERROR: Unexpected exception during backup: $_" -ForegroundColor Red
    exit 1
} finally {
    $env:PGPASSWORD = $null
}
