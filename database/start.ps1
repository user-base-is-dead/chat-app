$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'env.ps1')

& (Pg pg_ctl) -D $DataDir status *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Output "Database already running on port $DbPort"
    return
}

$pgCtl = Start-Process -FilePath (Pg pg_ctl) -ArgumentList @('-D', "`"$DataDir`"", '-l', "`"$LogFile`"", '-w', 'start') -NoNewWindow -PassThru
$pgCtl.WaitForExit()

& (Pg pg_ctl) -D $DataDir status *> $null
if ($LASTEXITCODE -ne 0) { throw "Database failed to start, see $LogFile" }
Write-Output "Database started on port $DbPort"
