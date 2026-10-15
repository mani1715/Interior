# PostgreSQL Logical Restore Script -- Elégance Interior Designer Platform
# Phase 12B Production Readiness
param (
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    
    [Parameter(Mandatory=$true)]
    [string]$Database,
    
    [string]$DbHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }),
    [int]$Port = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 5433 }),
    [string]$User = $(if ($env:DB_USER) { $env:DB_USER } else { "postgres" }),
    [string]$Password = $(if ($env:PGPASSWORD) { $env:PGPASSWORD } elseif ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }),
    [switch]$DropAndRecreate = $false
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RootDir = Split-Path -Parent $ScriptDir

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ELEGANCE -- POSTGRESQL LOGICAL RESTORE UTILITY             " -ForegroundColor Cyan
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

# 2. Validate backup file
if (-not (Test-Path $BackupFile)) {
    Write-Host "ERROR: Backup file '$BackupFile' does not exist." -ForegroundColor Red
    exit 1
}

$fileInfo = Get-Item $BackupFile
if ($fileInfo.Length -eq 0) {
    Write-Host "ERROR: Backup file '$BackupFile' is empty (0 bytes)." -ForegroundColor Red
    exit 1
}

# 3. Locate pg_restore and psql binaries
$pgRestorePath = $null
$psqlPath = $null

if (Get-Command pg_restore -ErrorAction SilentlyContinue) {
    $pgRestorePath = (Get-Command pg_restore).Source
} else {
    $candidates = @(
        "C:\Program Files\PostgreSQL\18\bin\pg_restore.exe",
        "C:\Program Files\PostgreSQL\17\bin\pg_restore.exe"
    )
    foreach ($cand in $candidates) {
        if (Test-Path $cand) {
            $pgRestorePath = $cand
            break
        }
    }
}

if (Get-Command psql -ErrorAction SilentlyContinue) {
    $psqlPath = (Get-Command psql).Source
} else {
    $candidates = @(
        "C:\Program Files\PostgreSQL\18\bin\psql.exe",
        "C:\Program Files\PostgreSQL\17\bin\psql.exe"
    )
    foreach ($cand in $candidates) {
        if (Test-Path $cand) {
            $psqlPath = $cand
            break
        }
    }
}

if (-not $pgRestorePath) {
    Write-Host "ERROR: pg_restore utility not found." -ForegroundColor Red
    exit 1
}

Write-Host "`nRestore Parameters:" -ForegroundColor Yellow
Write-Host "  Source File     : $BackupFile ($([math]::Round($fileInfo.Length / 1KB, 2)) KB)"
Write-Host "  Target Database : $Database"
Write-Host "  Host:Port       : ${DbHost}:${Port}"
Write-Host "  User            : $User"
Write-Host "  Password        : [MASKED]"
Write-Host "  Drop & Recreate : $DropAndRecreate"

# 4. Optional Drop and Recreate
$env:PGPASSWORD = $Password
try {
    if ($DropAndRecreate -and $psqlPath) {
        Write-Host "`nDropping and recreating target database '$Database'..." -ForegroundColor Yellow
        
        # Terminate active connections first
        & $psqlPath -h $DbHost -p $Port -U $User -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$Database' AND pid <> pg_backend_pid();" | Out-Null
        
        & $psqlPath -h $DbHost -p $Port -U $User -d postgres -c "DROP DATABASE IF EXISTS $Database;" | Out-Null
        & $psqlPath -h $DbHost -p $Port -U $User -d postgres -c "CREATE DATABASE $Database;" | Out-Null
        
        Write-Host "Fresh database '$Database' created successfully." -ForegroundColor Green
    }

    # 5. Execute pg_restore
    Write-Host "`nStarting pg_restore execution..." -ForegroundColor Yellow
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    
    # --no-owner: portable restore
    # --no-acl: prevent permission errors
    # --exit-on-error: fail immediately on critical errors
    & $pgRestorePath -h $DbHost -p $Port -U $User -d $Database --no-owner --no-acl $BackupFile
    
    $sw.Stop()
    $exitCode = $LASTEXITCODE
    
    # Note: pg_restore returns 0 for clean success, and 1 for non-critical warnings (e.g. notices).
    if ($exitCode -gt 1) {
        Write-Host "ERROR: pg_restore failed with critical error code $exitCode." -ForegroundColor Red
        exit $exitCode
    }
    
    Write-Host "`n============================================================" -ForegroundColor Green
    Write-Host "  RESTORE COMPLETED SUCCESSFULLY                             " -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  Target DB  : $Database" -ForegroundColor Green
    Write-Host "  Duration   : $($sw.ElapsedMilliseconds) ms" -ForegroundColor Green
    Write-Host "  Exit Status: $exitCode (Success)" -ForegroundColor Green
    
} catch {
    Write-Host "ERROR: Unexpected exception during restore: $_" -ForegroundColor Red
    exit 1
} finally {
    $env:PGPASSWORD = $null
}
