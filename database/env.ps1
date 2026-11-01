$PgBin = $env:PG_BIN
if (-not $PgBin -and $env:ProgramFiles -and (Test-Path (Join-Path $env:ProgramFiles 'PostgreSQL'))) {
    $PgBin = Get-ChildItem (Join-Path $env:ProgramFiles 'PostgreSQL') -Directory |
        Sort-Object { [int]($_.Name -replace '\D', '') } -Descending |
        ForEach-Object { Join-Path $_.FullName 'bin' } |
        Where-Object { Test-Path (Join-Path $_ 'initdb*') } |
        Select-Object -First 1
}
if (-not $PgBin) {
    $onPath = Get-Command initdb -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($onPath) { $PgBin = Split-Path $onPath.Source }
}
if (-not $PgBin) { throw 'PostgreSQL not found. Install it, add its bin folder to PATH, or set PG_BIN.' }

function Pg($name) {
    $exe = Join-Path $PgBin "$name.exe"
    if (Test-Path $exe) { return $exe }
    Join-Path $PgBin $name
}

$DataDir = Join-Path $PSScriptRoot 'data'
$LogFile = Join-Path $PSScriptRoot 'postgres.log'
$MigrationsDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'backend/migrations'
$DbHost = '127.0.0.1'
$DbPort = 5433
$DbUser = 'miron'
$DbName = 'miron_chat'
