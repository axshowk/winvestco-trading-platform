# start-infra.ps1
# Automates the startup of portable Winvestco infrastructure with Port Verification

$ToolsDir = "C:\tools"
$PortsToVerify = @{
    "PostgreSQL" = 5432
    "Redis"      = 6379
    "RabbitMQ"   = 5672
    "Kafka"      = 9092
}

Write-Host "===========================================" -ForegroundColor Green
Write-Host " Winvestco Infra Automation (Portable)" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green

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
Write-Host "[1/4] Starting PostgreSQL..." -ForegroundColor Yellow

# Initialize DB if data dir doesn't exist
if (-not (Test-Path "$ToolsDir\pgsql\data")) {
    Write-Host "Initializing PostgreSQL data directory..." -ForegroundColor Cyan
    Start-Process -FilePath "$ToolsDir\pgsql\bin\initdb.exe" -ArgumentList "-D $ToolsDir\pgsql\data -U postgres" -Wait -NoNewWindow
}

Start-Process -FilePath "$ToolsDir\pgsql\bin\pg_ctl.exe" -ArgumentList "start -D $ToolsDir\pgsql\data -l $ToolsDir\pgsql\logfile" -NoNewWindow

# 2. Start Redis
Write-Host "[2/4] Starting Redis..." -ForegroundColor Yellow
Start-Process -FilePath "$ToolsDir\Redis\redis-server.exe" -NoNewWindow

# 3. Start RabbitMQ
Write-Host "[3/4] Starting RabbitMQ..." -ForegroundColor Yellow
$env:ERLANG_HOME = "$ToolsDir\otp_win64"
$env:Path += ";$ToolsDir" # Add tools dir to path so wmic.bat is found
$env:RABBITMQ_NODENAME = "rabbit@localhost"
Start-Process -FilePath "$ToolsDir\rabbitmq_server-4.2.3\sbin\rabbitmq-server.bat" -NoNewWindow

# 4. Start Kafka (KRaft mode)
Write-Host "[4/4] Starting Kafka (KRaft)..." -ForegroundColor Yellow
$KafkaDir = "$ToolsDir\kafka_2.13-4.1.1"
$KraftLogsDir = "$KafkaDir\kraft-combined-logs"

if (-not (Test-Path $KraftLogsDir)) {
    Write-Host "Formatting Kafka storage for KRaft..." -ForegroundColor Cyan
    # Generate a random UUID and format
    $randomUuid = (& "cmd.exe" /c "cd /d $KafkaDir && .\bin\windows\kafka-storage.bat random-uuid" | Select-Object -Last 1).Trim()
    & "cmd.exe" /c "cd /d $KafkaDir && .\bin\windows\kafka-storage.bat format --standalone -t $randomUuid -c .\config\server.properties"
}

Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d $KafkaDir && .\bin\windows\kafka-server-start.bat .\config\server.properties" -NoNewWindow

Write-Host "`nWaiting for services to be ready..." -ForegroundColor Cyan
$allOk = $true
foreach ($service in $PortsToVerify.Keys) {
    if (-not (Wait-ForPort -Name $service -Port $PortsToVerify[$service])) {
        $allOk = $false
    }
}

Write-Host "-------------------------------------------"
if ($allOk) {
    Write-Host "SUCCESS: All infrastructure services are UP and RUNNING." -ForegroundColor Green
}
else {
    Write-Host "WARNING: Some services failed to start or took too long." -ForegroundColor Yellow
    Write-Host "Please check the logs in C:\tools\"
}
Write-Host "===========================================" -ForegroundColor Green
