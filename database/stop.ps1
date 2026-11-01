$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'env.ps1')

& (Pg pg_ctl) -D $DataDir -m fast -w stop
