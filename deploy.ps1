# GVR Tech Notes - Windows Deploy Script
# USAGE: .\deploy.ps1

# -- CONFIG -------------------------------------------------------------------
$EC2_USER = "ec2-user"
$EC2_HOST = "16.112.109.195"
$PEM_KEY  = "C:\Users\mukte\venkyjava.pem"
$APP_DIR  = "/opt/gvr-tech-notes"
$SERVICE  = "gvr-tech-notes"
$JAR_NAME = "SBRW_Demo-0.0.1-SNAPSHOT.jar"
# -----------------------------------------------------------------------------

$ErrorActionPreference = "Stop"

function Log($msg) { Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $msg" -ForegroundColor Cyan }
function Ok($msg)  { Write-Host "[$(Get-Date -Format 'HH:mm:ss')] OK  $msg" -ForegroundColor Green }
function Err($msg) { Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ERR $msg" -ForegroundColor Red; exit 1 }

if (-not (Test-Path $PEM_KEY)) { Err "PEM key not found at: $PEM_KEY" }

Log "==========================================="
Log "  GVR Tech Notes - Deploying to AWS EC2"
Log "  Host : $EC2_HOST"
Log "==========================================="

# -- STEP 1: Build JAR --------------------------------------------------------
Log "STEP 1/5 - Building JAR..."
Set-Location $PSScriptRoot
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { Err "Maven build failed." }
Ok "Build successful"

$JAR_PATH = ".\target\$JAR_NAME"
if (-not (Test-Path $JAR_PATH)) { Err "JAR not found at $JAR_PATH" }

# -- STEP 2: Upload JAR -------------------------------------------------------
Log "STEP 2/5 - Uploading JAR to EC2..."
# Upload directly to home dir first, then move with sudo (avoids permission issues)
& scp -i $PEM_KEY -o StrictHostKeyChecking=no $JAR_PATH "${EC2_USER}@${EC2_HOST}:~/${JAR_NAME}"
if ($LASTEXITCODE -ne 0) { Err "SCP failed. Check EC2_HOST, PEM_KEY, and port 22." }
Ok "JAR uploaded"

# -- STEP 3: Upload prod properties -------------------------------------------
Log "STEP 3/5 - Uploading application-prod.properties..."
& scp -i $PEM_KEY -o StrictHostKeyChecking=no ".\src\main\resources\application-prod.properties" "${EC2_USER}@${EC2_HOST}:${APP_DIR}/application-prod.properties"
if ($LASTEXITCODE -ne 0) { Err "Failed to copy properties file." }
Ok "Properties uploaded"

# -- STEP 4: Restart service --------------------------------------------------
Log "STEP 4/5 - Restarting service on EC2..."

$bashScript  = "set -e`n"
$bashScript += "echo Stopping service...`n"
$bashScript += "sudo systemctl stop $SERVICE 2>/dev/null; true`n"
$bashScript += "echo Moving JAR from home to app dir...`n"
$bashScript += "sudo mv ~/$JAR_NAME $APP_DIR/$JAR_NAME`n"
$bashScript += "sudo chown appuser:appuser $APP_DIR/$JAR_NAME`n"
$bashScript += "sudo chmod 664 $APP_DIR/$JAR_NAME`n"
$bashScript += "echo JAR size after copy:`n"
$bashScript += "ls -lh $APP_DIR/$JAR_NAME`n"
$bashScript += "echo Starting service...`n"
$bashScript += "sudo systemctl start $SERVICE`n"
$bashScript += "sudo systemctl enable $SERVICE`n"
$bashScript += "sleep 3`n"
$bashScript += "sudo systemctl status $SERVICE --no-pager`n"

$tempScript = "$env:TEMP\deploy-restart.sh"
[System.IO.File]::WriteAllText($tempScript, $bashScript)

& scp -i $PEM_KEY -o StrictHostKeyChecking=no $tempScript "${EC2_USER}@${EC2_HOST}:/tmp/deploy-restart.sh"
& ssh -i $PEM_KEY -o StrictHostKeyChecking=no "${EC2_USER}@${EC2_HOST}" "bash /tmp/deploy-restart.sh"
if ($LASTEXITCODE -ne 0) { Err "Remote restart failed." }
Ok "Service restarted"

# -- STEP 5: Show startup logs ------------------------------------------------
Log "STEP 5/5 - Startup logs (last 50 lines)..."
Start-Sleep -Seconds 8
& ssh -i $PEM_KEY -o StrictHostKeyChecking=no "${EC2_USER}@${EC2_HOST}" "sudo journalctl -u $SERVICE -n 50 --no-pager"

Log ""
Ok "==========================================="
Ok "  Deployment complete!"
Ok "  App URL : http://$EC2_HOST"
Ok "==========================================="
