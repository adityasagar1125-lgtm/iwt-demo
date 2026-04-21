$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$tomcatHome = Join-Path $projectRoot ".local\apache-tomcat-10.1.52"
$shutdownBat = Join-Path $tomcatHome "bin\shutdown.bat"

if (-not (Test-Path $tomcatHome)) {
    Write-Error "Local Tomcat not found at $tomcatHome"
}

$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $tomcatHome

Write-Host "Stopping Tomcat..."
& $shutdownBat

Start-Sleep -Seconds 2

try {
    Invoke-WebRequest -Uri "http://localhost:8080/ClubHub/" -UseBasicParsing -TimeoutSec 5 | Out-Null
    Write-Warning "Tomcat still responding. If started with 'catalina.bat run', stop that terminal manually (Ctrl+C)."
} catch {
    Write-Host "Tomcat stopped (port 8080 no longer responding)."
}
