# Runs destructive PostgreSQL tests only in a fresh, loopback-only cluster.
# Requires the installed PostgreSQL 18 tools. Never connects to development port 5433.
param([string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin')
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$port = 55433
$started = $false
$testRoot = Join-Path $root ('storage/qa-postgres-' + [Guid]::NewGuid().ToString('N'))
$data = Join-Path $testRoot 'data'
function Assert-Command($step) {
    if ($LASTEXITCODE -ne 0) { throw "$step failed with exit code $LASTEXITCODE" }
}
Push-Location $root
try {
    & powershell -ExecutionPolicy Bypass -File scripts/verify-interior-context.ps1
    Assert-Command 'Repository context guard'
    $probe = [Net.Sockets.TcpClient]::new()
    $occupied = $false
    try { $probe.Connect('localhost', $port); $occupied = $true }
    catch [Net.Sockets.SocketException] { }
    finally { $probe.Dispose() }
    if ($occupied) { throw "Test port $port is occupied. Refusing to reuse an existing database instance." }
    New-Item -ItemType Directory -Path $testRoot | Out-Null
    & "$PostgresBin/initdb.exe" -D $data -U postgres --encoding=UTF8 --locale=C --auth=trust
    Assert-Command 'Initialize empty test cluster'
    & "$PostgresBin/pg_ctl.exe" -D $data -l (Join-Path $testRoot 'server.log') -o "-p $port -h localhost" -w start
    Assert-Command 'Start test cluster'
    $started = $true
    $actual = & "$PostgresBin/psql.exe" -h localhost -p $port -U postgres -d postgres -Atc 'SHOW data_directory'
    Assert-Command 'Check test cluster identity'
    if ([IO.Path]::GetFullPath($actual.Trim()) -ne [IO.Path]::GetFullPath($data)) {
        throw 'Test database identity mismatch; refusing to run tests.'
    }
    & "$PostgresBin/createdb.exe" -h localhost -p $port -U postgres interior_phase12b_bootstrap
    Assert-Command 'Create bootstrap test database'
    Set-Location (Join-Path $root 'apps/api')
    & .\mvnw.cmd '-Dinterior.tests.postgres=true' '-Dtest=PostgreSqlBootstrapVerificationTest' test
    Assert-Command 'Migration bootstrap test'
    & "$PostgresBin/createdb.exe" -h localhost -p $port -U postgres -T interior_phase12b_bootstrap interior_design_rls_test
    Assert-Command 'Create migrated RLS test database'
    & .\mvnw.cmd '-Dinterior.tests.postgres=true' '-Dtest=PostgreSql*Test' test
    Assert-Command 'PostgreSQL suite'
    $total = 0; $skipped = 0; $failures = 0
    $reports = Get-ChildItem target/surefire-reports -Filter 'TEST-*PostgreSql*Test.xml'
    foreach ($report in $reports) {
        [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        $total += [int]$xml.testsuite.tests
        $skipped += [int]$xml.testsuite.skipped
        $failures += [int]$xml.testsuite.failures + [int]$xml.testsuite.errors
    }
    if ($total -lt 47 -or $skipped -ne 0 -or $failures -ne 0) {
        throw "Incomplete database validation: tests=$total skipped=$skipped failures/errors=$failures"
    }
    Write-Host "POSTGRESQL QA PASSED: $total tests; zero skipped; zero failures/errors."
} finally {
    if ($started) {
        & "$PostgresBin/pg_ctl.exe" -D $data -m fast -w stop
        Assert-Command 'Stop test cluster'
    }
    Pop-Location
    Write-Host "Isolated test data retained at $testRoot"
}
