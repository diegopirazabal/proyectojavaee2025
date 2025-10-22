param(
  [string]$WildFlyHome = "C:\\Program Files\\wildfly",
  [string]$BackendWar = "$PSScriptRoot\..\componente-periferico\backend\target\hcen-periferico.war",
  [string]$FrontendWar = "$PSScriptRoot\..\componente-central\frontend-usuario-salud\target\frontend-usuario-salud.war",
  [string]$ApiBaseUrl = "http://localhost:8080/api",
  [switch]$DevLogin = $true,
  [switch]$UseCLI = $true
)

function Invoke-JBossCli {
  param([string]$Command)
  & "${WildFlyHome}\bin\jboss-cli.bat" --connect --command=$Command 2>$null | Out-Null
}

function Start-WildFlyAdminOnly {
  Write-Host "Starting WildFly in --admin-only..."
  Start-Process -FilePath "${WildFlyHome}\bin\standalone.bat" -ArgumentList "--admin-only" -WindowStyle Minimized
  Start-Sleep -Seconds 6
}

function Stop-WildFly {
  Write-Host "Shutting down WildFly via CLI (ignore errors if not running)..."
  Invoke-JBossCli ":shutdown" | Out-Null
  Start-Sleep -Seconds 3
}

function Clean-Deployments {
  $depDir = "${WildFlyHome}\standalone\deployments"
  Write-Host "Cleaning deployment scanner files in $depDir ..."
  Get-ChildItem "$depDir\frontend-usuario-salud.war*" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
  Get-ChildItem "$depDir\hcen-periferico.war*" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
}

function Undeploy-IfPresent {
  param([string]$Name)
  Write-Host "Undeploying $Name if present..."
  Invoke-JBossCli "undeploy $Name --keep-content=false"
}

function Deploy-War {
  param([string]$Path)
  if (!(Test-Path $Path)) { throw "WAR not found: $Path" }
  Write-Host "Deploying $Path ..."
  Invoke-JBossCli "deploy \"$Path\" --force"
}

function Start-WildFlyNormal {
  $args = ""
  if ($ApiBaseUrl) { $args += " -Dhcen.apiBaseUrl=$ApiBaseUrl" }
  if ($DevLogin)   { $args += " -Dhcen.enableDevLogin=true" }
  Write-Host "Starting WildFly normal$args ..."
  Start-Process -FilePath "${WildFlyHome}\bin\standalone.bat" -ArgumentList $args -WindowStyle Minimized
  Start-Sleep -Seconds 6
}

# 1) Stop any running instance
Stop-WildFly

# 2) Clean filesystem scanner remnants
Clean-Deployments

# 3) Start admin-only and remove persistent deployments
Start-WildFlyAdminOnly
Undeploy-IfPresent -Name "frontend-usuario-salud.war"
Undeploy-IfPresent -Name "hcen-periferico.war"
Stop-WildFly

# 4) Start normal instance with frontend props
Start-WildFlyNormal

# 5) Deploy using CLI (single method)
if ($UseCLI) {
  Deploy-War -Path $BackendWar
  Deploy-War -Path $FrontendWar
} else {
  $depDir = "${WildFlyHome}\standalone\deployments"
  Write-Host "Copying WARs to $depDir ..."
  Copy-Item $BackendWar $depDir -Force
  Copy-Item $FrontendWar $depDir -Force
}

Write-Host "Done. Test API: $ApiBaseUrl/historia/{CI}/documentos"
