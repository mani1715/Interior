# Interior Designer Platform Context Verification Guard
$ErrorActionPreference = "Stop"

$ExpectedDirectory = "C:\my projects\interior design"
$ExpectedRemote = "https://github.com/mani1715/Interior.git"
$ExpectedBranch = "main"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  INTERIOR DESIGNER PLATFORM -- CONTEXT VERIFICATION GUARD  " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Resolve current directory
$CurrentDir = (Get-Location).Path
Write-Host ("Current Directory: " + $CurrentDir)

$NormalizedCurrent = (Resolve-Path $CurrentDir).Path.TrimEnd("\")
$ResolvedExpected = (Resolve-Path $ExpectedDirectory -ErrorAction SilentlyContinue)

if ($ResolvedExpected) {
    $NormalizedExpected = $ResolvedExpected.Path.TrimEnd("\")
} else {
    $NormalizedExpected = $ExpectedDirectory.TrimEnd("\")
}

if (-not $NormalizedCurrent.StartsWith($NormalizedExpected, [System.StringComparison]::OrdinalIgnoreCase)) {
    Write-Host ("ERROR: Current directory is NOT inside: " + $ExpectedDirectory) -ForegroundColor Red
    Write-Host ("Resolved: " + $NormalizedCurrent) -ForegroundColor Red
    Write-Host "CONTEXT VERIFICATION FAILED" -ForegroundColor Red
    exit 1
}

# 2. Check if valid git repository
$isGit = git rev-parse --is-inside-work-tree 2>$null
if ($LASTEXITCODE -ne 0 -or $isGit -ne "true") {
    Write-Host "ERROR: Not inside a valid git working tree." -ForegroundColor Red
    Write-Host "CONTEXT VERIFICATION FAILED" -ForegroundColor Red
    exit 1
}

# 3. Check git remote origin
$RemoteOrigin = (git remote get-url origin 2>$null)
if (-not $RemoteOrigin) {
    Write-Host "ERROR: No origin remote found." -ForegroundColor Red
    Write-Host "CONTEXT VERIFICATION FAILED" -ForegroundColor Red
    exit 1
}

$NormRemote = $RemoteOrigin.Trim()
if (-not $NormRemote.EndsWith(".git", [System.StringComparison]::OrdinalIgnoreCase)) {
    $NormRemote = $NormRemote + ".git"
}

$NormExpectedRemote = $ExpectedRemote.Trim()
if (-not $NormExpectedRemote.EndsWith(".git", [System.StringComparison]::OrdinalIgnoreCase)) {
    $NormExpectedRemote = $NormExpectedRemote + ".git"
}

if ($NormRemote -ne $NormExpectedRemote) {
    Write-Host ("ERROR: Remote origin " + $RemoteOrigin + " does not match expected " + $ExpectedRemote) -ForegroundColor Red
    Write-Host "CONTEXT VERIFICATION FAILED" -ForegroundColor Red
    exit 1
}

# 4. Check git branch
$CurrentBranch = (git branch --show-current 2>$null)
if ($CurrentBranch -ne $ExpectedBranch) {
    Write-Host ("ERROR: Current branch " + $CurrentBranch + " does not match expected " + $ExpectedBranch) -ForegroundColor Red
    Write-Host "CONTEXT VERIFICATION FAILED" -ForegroundColor Red
    exit 1
}

# 5. Print current HEAD
$CurrentHead = (git rev-parse HEAD 2>$null)
Write-Host ("Repository Remote: " + $RemoteOrigin) -ForegroundColor Green
Write-Host ("Branch           : " + $CurrentBranch) -ForegroundColor Green
Write-Host ("HEAD Commit      : " + $CurrentHead) -ForegroundColor Green

# 6. Print working tree status
Write-Host "Working Tree Status:" -ForegroundColor Yellow
git status --short

Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "INTERIOR PROJECT CONTEXT VERIFIED" -ForegroundColor Green
Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
exit 0
