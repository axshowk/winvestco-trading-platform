# start-infra.ps1
# Automates the startup of portable Winvestco infrastructure with Port Verification

$ToolsDir = "C:\tools"
$PortsToVerify = @{
    "PostgreSQL" = 5432
    "Redis"      = 6379
    "RabbitMQ"   = 5672
    "Kafka"      = 9092
    "Prometheus" = 9090
    "Loki"       = 3100
    "Grafana"    = 3000
    "Jaeger"     = 16686
}

Write-Host "===========================================" -ForegroundColor Green
Write-Host " Winvestco Infra Automation (Portable)" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green

# Use absolute path for workspace to find config files
$WorkspaceRoot = "c:\winvestco-trading-platform"

# Function to wait for a port
function Wait-ForPort {
    param([string]$Name, [int]$Port, [int]$TimeoutSeconds = 30)
    Write-Host "Verifying $Name on port $Port..." -NoNewline
    $start = Get-Date
    while ((Get-Date) -lt $start.AddSeconds($TimeoutSeconds)) {
        $tcp = New-Object System.Net.Sockets.TcpClient
        try {
            $tcp.Connect("127.0.0.1", $Port)
            $tcp.Close()
            Write-Host " [OK]" -ForegroundColor Green
            return $true
        }
        catch {
            Start-Sleep -Milliseconds 500
        }
    }
    Write-Host " [FAILED]" -ForegroundColor Red
    return $false
}

# 1. Start PostgreSQL
# ... skipped for brevity if needed but I'll write the full block for clarity ...
Write-Host "[1/8] Starting PostgreSQL..." -ForegroundColor Yellow
if (-not (Test-Path "$ToolsDir\pgsql\data")) {
    Write-Host "Initializing PostgreSQL data directory..." -ForegroundColor Cyan
    Start-Process -FilePath "$ToolsDir\pgsql\bin\initdb.exe" -ArgumentList "-D $ToolsDir\pgsql\data -U postgres" -Wait -NoNewWindow
}
Start-Process -FilePath "$ToolsDir\pgsql\bin\pg_ctl.exe" -ArgumentList "start -D $ToolsDir\pgsql\data -l $ToolsDir\pgsql\logfile" -NoNewWindow

# 2. Start Redis
Write-Host "[2/8] Starting Redis..." -ForegroundColor Yellow
Start-Process -FilePath "$ToolsDir\Redis\redis-server.exe" -NoNewWindow

# 3. Start RabbitMQ
Write-Host "[3/8] Starting RabbitMQ..." -ForegroundColor Yellow
$env:ERLANG_HOME = "$ToolsDir\otp_win64"
$env:Path += ";$ToolsDir" 
$env:RABBITMQ_NODENAME = "rabbit@localhost"
Start-Process -FilePath "$ToolsDir\rabbitmq_server-4.2.3\sbin\rabbitmq-server.bat" -NoNewWindow

# 4. Start Kafka (KRaft mode)
Write-Host "[4/8] Starting Kafka (KRaft)..." -ForegroundColor Yellow
$KafkaDir = "$ToolsDir\kafka_2.13-4.1.1"
$KraftLogsDir = "$KafkaDir\kraft-combined-logs"
if (-not (Test-Path $KraftLogsDir)) {
    Write-Host "Formatting Kafka storage for KRaft..." -ForegroundColor Cyan
    $randomUuid = (& "cmd.exe" /c "cd /d $KafkaDir && .\bin\windows\kafka-storage.bat random-uuid" | Select-Object -Last 1).Trim()
    & "cmd.exe" /c "cd /d $KafkaDir && .\bin\windows\kafka-storage.bat format --standalone -t $randomUuid -c .\config\server.properties"
}
Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d $KafkaDir && .\bin\windows\kafka-server-start.bat .\config\server.properties" -NoNewWindow

# 5. Start Prometheus
Write-Host "[5/8] Starting Prometheus..." -ForegroundColor Yellow
$PromConfig = "$WorkspaceRoot\observability\prometheus\prometheus.yml"
Start-Process -FilePath "$ToolsDir\prometheus-2.47.0.windows-amd64\prometheus.exe" -ArgumentList "--config.file=$PromConfig" -NoNewWindow

# 6. Start Loki
Write-Host "[6/8] Starting Loki..." -ForegroundColor Yellow
$LokiConfig = "$WorkspaceRoot\observability\loki\loki-config.yml"
Start-Process -FilePath "$ToolsDir\loki.exe" -ArgumentList "-config.file=$LokiConfig" -NoNewWindow

# 7. Start Grafana
Write-Host "[7/8] Starting Grafana..." -ForegroundColor Yellow
# Using Start-Process with WorkingDirectory to ensure it finds its /public and /conf folders
$GrafanaDir = "$ToolsDir\grafana-10.2.0"
Start-Process -FilePath "$GrafanaDir\bin\grafana-server.exe" -WorkingDirectory $GrafanaDir -NoNewWindow

# 8. Start Jaeger
Write-Host "[8/8] Starting Jaeger..." -ForegroundColor Yellow
$env:COLLECTOR_OTLP_ENABLED = "true"
Start-Process -FilePath "$ToolsDir\jaeger-1.52.0-windows-amd64\jaeger-all-in-one.exe" -NoNewWindow

Write-Host "`nWaiting for services to be ready..." -ForegroundColor Cyan
$allOk = $true
foreach ($service in $PortsToVerify.Keys) {
    if (-not (Wait-ForPort -Name $service -Port $PortsToVerify[$service])) {
        $allOk = $false
    }
}

Write-Host "-------------------------------------------"
if ($allOk) {
    Write-Host "SUCCESS: All infrastructure + observability services are UP and RUNNING." -ForegroundColor Green
    Write-Host "Grafana: http://localhost:3000 (admin/winvestco)" -ForegroundColor Green
    Write-Host "Prometheus: http://localhost:9090" -ForegroundColor Green
    Write-Host "Jaeger: http://localhost:16686" -ForegroundColor Green
}
else {
    Write-Host "WARNING: Some services failed to start or took too long." -ForegroundColor Yellow
    Write-Host "Please check the logs in C:\tools\"
}
Write-Host "===========================================" -ForegroundColor Green
