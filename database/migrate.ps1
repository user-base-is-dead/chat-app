$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'env.ps1')

$env:PGOPTIONS = '-c client_min_messages=warning'
$psql = (Pg psql)
$conn = @('-h', $DbHost, '-p', $DbPort, '-U', $DbUser, '-d', $DbName, '-v', 'ON_ERROR_STOP=1', '-q')

& $psql @conn -c 'CREATE TABLE IF NOT EXISTS schema_migrations (name TEXT PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())'
if ($LASTEXITCODE -ne 0) { throw 'Could not create schema_migrations' }

$applied = & $psql @conn -tA -c 'SELECT name FROM schema_migrations'

Get-ChildItem $MigrationsDir -Filter '*.sql' | Sort-Object Name | ForEach-Object {
    if ($applied -contains $_.Name) { return }
    Write-Output "Applying $($_.Name)"
    & $psql @conn -1 -f $_.FullName
    if ($LASTEXITCODE -ne 0) { throw "Migration failed: $($_.Name)" }
    & $psql @conn -c "INSERT INTO schema_migrations (name) VALUES ('$($_.Name)')"
}

Write-Output 'Migrations up to date'
