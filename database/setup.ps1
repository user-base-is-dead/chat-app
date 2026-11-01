$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'env.ps1')

if (-not (Test-Path $DataDir)) {
    & (Pg initdb) -D $DataDir -U $DbUser --auth=trust -E UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'initdb failed' }
    Add-Content (Join-Path $DataDir 'postgresql.conf') @"

port = $DbPort
listen_addresses = '$DbHost'
max_connections = 200
shared_buffers = 256MB
work_mem = 16MB
maintenance_work_mem = 128MB
wal_compression = on
random_page_cost = 1.1
"@
}

& (Join-Path $PSScriptRoot 'start.ps1')

$exists = & (Pg psql) -h $DbHost -p $DbPort -U $DbUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DbName'"
if ($exists -ne '1') {
    & (Pg createdb) -h $DbHost -p $DbPort -U $DbUser $DbName
    if ($LASTEXITCODE -ne 0) { throw 'createdb failed' }
}

& (Join-Path $PSScriptRoot 'migrate.ps1')
