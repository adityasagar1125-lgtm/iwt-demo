param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$tomcatHome = Join-Path $projectRoot ".local\apache-tomcat-10.1.52"
$startupBat = Join-Path $tomcatHome "bin\startup.bat"
$warPath = Join-Path $projectRoot "target\ClubHub.war"
$webappsWarPath = Join-Path $tomcatHome "webapps\ClubHub.war"

if (-not (Test-Path $tomcatHome)) {
    Write-Error "Local Tomcat not found at $tomcatHome"
}

if ($Build) {
    Write-Host "Building WAR..."
    Push-Location $projectRoot
    try {
        & ".\mvnw.cmd" clean package -DskipTests
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $warPath)) {
    Write-Error "WAR not found at $warPath. Run: .\\mvnw.cmd clean package -DskipTests"
}

Copy-Item $warPath $webappsWarPath -Force

$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $tomcatHome

Write-Host "Starting Tomcat from $tomcatHome ..."
& $startupBat

Start-Sleep -Seconds 2

try {
    $status = (Invoke-WebRequest -Uri "http://localhost:8080/ClubHub/" -UseBasicParsing -TimeoutSec 10).StatusCode
    Write-Host "ClubHub is up at http://localhost:8080/ClubHub/ (HTTP $status)"
} catch {
    Write-Warning "Tomcat started, but ClubHub did not respond yet. Check logs in .local/apache-tomcat-10.1.52/logs"
}
