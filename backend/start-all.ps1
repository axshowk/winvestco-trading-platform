# start-all.ps1
# Automates the startup of the entire Winvestco Platform (Infra + Backend Services)
# Uses Actuator Health endpoints to verify service readiness.

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = $ScriptDir
$MvnCmd = "C:\TOOLS\Maven\apache-maven-3.9.9\bin\mvn.cmd"

# Function to check if service is healthy via Actuator
function Test-ServiceHealthy {
    param([int]$Port)
    try {
        $uri = "http://localhost:$Port/actuator/health"
        $response = Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 2 -ErrorAction Stop
        if ($response -and $response.status -eq "UP") {
            return $true
        }
    }
    catch {
        # Service might not be ready yet (Connection refused, 404, 503 Service Unavailable, etc.)
        return $false
    }
    return $false
}

# Function to wait for a service to be ready
function Wait-ForService {
    param([string]$Name, [int]$Port, [int]$TimeoutSeconds = 180)
    Write-Host "Waiting for $Name on port $Port (Actuator Health)..." -NoNewline
    $start = Get-Date
    while ((Get-Date) -lt $start.AddSeconds($TimeoutSeconds)) {
        if (Test-ServiceHealthy -Port $Port) {
            Write-Host " [UP]" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 3
        Write-Host "." -NoNewline
    }
    Write-Host " [TIMEOUT]" -ForegroundColor Red
    Write-Warning "$Name did not become healthy within $TimeoutSeconds seconds. Continuing anyway..."
}

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "   WINVESTCO PLATFORM - START ALL Script   " -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan

# 1. Start Infrastructure
Write-Host "`n[1/4] Starting Infrastructure..." -ForegroundColor Yellow
if (Test-Path "$BackendDir\start-infra.ps1") {
    & "$BackendDir\start-infra.ps1"
}
else {
    Write-Error "start-infra.ps1 not found in $BackendDir"
}

# 2. Start Eureka Server
Write-Host "`n[2/4] Starting Eureka Server (Service Discovery)..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/k title Eureka Server && cd ""$BackendDir\eureka-server"" && ""$MvnCmd"" spring-boot:run" -WindowStyle Minimized
Wait-ForService -Name "Eureka Server" -Port 8761 -TimeoutSeconds 180

# 3. Start API Gateway
Write-Host "`n[3/4] Starting API Gateway..." -ForegroundColor Yellow
Start-Process -FilePath "cmd.exe" -ArgumentList "/k title API Gateway && cd ""$BackendDir\api-gateway"" && ""$MvnCmd"" spring-boot:run" -WindowStyle Minimized
Wait-ForService -Name "API Gateway" -Port 8090 -TimeoutSeconds 180

# 4. Start Microservices
Write-Host "`n[4/4] Starting Backend Microservices..." -ForegroundColor Yellow

$Services = @(
    @{ Name = "User Service"; Dir = "user-service"; Port = 8088 },
    @{ Name = "Market Service"; Dir = "market-service"; Port = 8084 },
    @{ Name = "Portfolio Service"; Dir = "portfolio-service"; Port = 8086 },
    @{ Name = "Funds Service"; Dir = "funds-service"; Port = 8085 },
    @{ Name = "Ledger Service"; Dir = "ledger-service"; Port = 8082 },
    @{ Name = "Order Service"; Dir = "order-service"; Port = 8081 },
    @{ Name = "Trade Service"; Dir = "trade-service"; Port = 8083 },
    @{ Name = "Notification Service"; Dir = "notification-service"; Port = 8089 },
    @{ Name = "Payment Service"; Dir = "payment-service"; Port = 8087 },
    @{ Name = "Report Service"; Dir = "report-service"; Port = 8091 },
    @{ Name = "Schedule Service"; Dir = "schedule-service"; Port = 8095 },
    @{ Name = "Risk Service"; Dir = "risk-service"; Port = 8092 }
)

foreach ($svc in $Services) {
    Write-Host "Launching $($svc.Name)..." -ForegroundColor Cyan
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k title $($svc.Name) && cd ""$BackendDir\$($svc.Dir)"" && ""$MvnCmd"" spring-boot:run" -WindowStyle Minimized
    Wait-ForService -Name $svc.Name -Port $svc.Port -TimeoutSeconds 180
}

Write-Host "`n===========================================" -ForegroundColor Green
Write-Host "   All startup commands issued!            " -ForegroundColor Green
Write-Host "   Please check open terminal windows for  " -ForegroundColor Green
Write-Host "   logs and individual service status.     " -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green
Write-Host "Frontend:    http://localhost:5173" -ForegroundColor Gray
Write-Host "Eureka:      http://localhost:8761" -ForegroundColor Gray
Write-Host "API Gateway: http://localhost:8090" -ForegroundColor Gray
