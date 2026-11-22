param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$Notes,
    [string]$LanHost = '192.168.31.73'
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
. (Join-Path $Root 'database/env.ps1')

$Package = 'com.civenth.app'
$ApiUrl = "http://${LanHost}:8080"
$WebUrl = "http://${LanHost}:5173"
$Releases = Join-Path $Root 'backend/releases'
$Name = "civenth-$Version.apk"
$Target = Join-Path $Releases $Name
$Output = Join-Path $PSScriptRoot 'app/build/outputs/apk/preview/app-preview.apk'
$Tools = Get-ChildItem (Join-Path $env:LOCALAPPDATA 'Android/Sdk/build-tools') -Directory | Sort-Object { [version]$_.Name } | Select-Object -Last 1
$Aapt = Join-Path $Tools.FullName 'aapt.exe'
$Signer = Join-Path $Tools.FullName 'apksigner.bat'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

function Invoke-Sql($query) {
    $result = & (Pg 'psql') -h $DbHost -p $DbPort -U $DbUser -d $DbName -Atc $query
    if ($LASTEXITCODE -ne 0) { throw "Database query failed: $query" }
    $result
}

function Read-Apk($path) {
    $badging = (& $Aapt dump badging $path | Select-Object -First 1)
    if ($badging -notmatch "name='([^']+)' versionCode='(\d+)' versionName='([^']+)'") { throw "Cannot read $path" }
    [pscustomobject]@{ Package = $Matches[1]; Code = [int]$Matches[2]; Name = $Matches[3] }
}

function Read-Cert($path) {
    $line = (& $Signer verify --print-certs $path | Select-String 'certificate SHA-256 digest' | Select-Object -First 1)
    if (-not $line) { throw "Signature check failed for $path" }
    ($line.ToString() -split ':')[-1].Trim()
}

if (Test-Path $Target) { throw "$Name already exists. Pick a new version name." }
if ($Version -notmatch '^\d+(\.\d+)+$') { throw "Version must look like 5.0" }

$Code = [int](Invoke-Sql "select coalesce(max(version_code), 0) + 1 from app_releases where platform = 'android'")
$Previous = Invoke-Sql "select download_url from app_releases where platform = 'android' order by version_code desc limit 1"
$PreviousFile = if ($Previous) { Join-Path $Releases (Split-Path $Previous -Leaf) } else { $null }
Write-Host "Releasing Civenth $Version (versionCode $Code)"

$Staged = Join-Path ([IO.Path]::GetTempPath()) "civenth-release-$Code.apk"
$built = $false
foreach ($try in 1..2) {
    Push-Location $PSScriptRoot
    try {
        & .\gradlew.bat :app:assemblePreview "-PciventhApiUrl=$ApiUrl" "-PciventhWebUrl=$WebUrl" "-PciventhVersionCode=$Code" "-PciventhVersionName=$Version" -q
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
    } finally {
        Pop-Location
    }
    Copy-Item $Output $Staged -Force
    $info = Read-Apk $Staged
    if ($info.Package -eq $Package -and $info.Code -eq $Code -and $info.Name -eq $Version) {
        $built = $true
        break
    }
    Write-Host "Built file says $($info.Package) $($info.Name) ($($info.Code)). Another build may have run at the same time. Building again."
}
if (-not $built) { throw "Could not get an APK with versionCode $Code. Nothing was published." }

if ($PreviousFile -and (Test-Path $PreviousFile)) {
    $old = Read-Cert $PreviousFile
    $new = Read-Cert $Staged
    if ($old -ne $new) { throw "Signing key differs from the last release. Phones would refuse the update. Nothing was published." }
}

Copy-Item $Staged $Target
$url = "$ApiUrl/v1/app/download/$Name"
$safeNotes = $Notes.Replace("'", "''")
Invoke-Sql "insert into app_releases (platform, version_code, version_name, notes, download_url) values ('android', $Code, '$Version', '$safeNotes', '$url')" | Out-Null

try {
    $live = $null
    foreach ($wait in 1..8) {
        Start-Sleep -Seconds 2
        $live = Invoke-RestMethod "http://127.0.0.1:8080/v1/app/version?platform=android"
        if ($live.latest.versionCode -eq $Code) { break }
    }
    if ($live.latest.versionCode -ne $Code) { throw "Server is not offering $Version. It may have rejected the file." }
    $download = Join-Path ([IO.Path]::GetTempPath()) "civenth-check-$Code.apk"
    Invoke-WebRequest "http://127.0.0.1:8080/v1/app/download/$Name" -OutFile $download -UseBasicParsing
    if ((Get-FileHash $download).Hash -ne (Get-FileHash $Target).Hash) { throw 'Downloaded file does not match the release file.' }
    Remove-Item $download
} catch {
    Invoke-Sql "delete from app_releases where platform = 'android' and version_code = $Code" | Out-Null
    Remove-Item $Target -ErrorAction SilentlyContinue
    throw "Release rolled back: $($_.Exception.Message)"
}

Copy-Item $Target (Join-Path $env:USERPROFILE 'Downloads/Civenth.apk') -Force
Remove-Item $Staged
Write-Host "Published Civenth $Version (versionCode $Code): $url"
