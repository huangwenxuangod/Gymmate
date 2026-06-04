$ErrorActionPreference = "Stop"

$ServerHost = $env:RACKNERD_HOST
$ServerUser = if ($env:RACKNERD_USER) { $env:RACKNERD_USER } else { "root" }
$ServerPort = if ($env:RACKNERD_PORT) { $env:RACKNERD_PORT } else { "22" }
$AppRoot = if ($env:APP_ROOT) { $env:APP_ROOT } else { "/opt/gymmate" }

if (-not $ServerHost) {
    throw "Please set RACKNERD_HOST first."
}

$workspace = Split-Path -Parent $PSScriptRoot
$archive = Join-Path $env:TEMP "gymmate-backend.tar.gz"

if (Test-Path $archive) {
    Remove-Item $archive -Force
}

tar -czf $archive `
    --exclude=".venv" `
    --exclude="runlogs" `
    --exclude="__pycache__" `
    -C (Split-Path -Parent $workspace) `
    "backend"

scp -P $ServerPort $archive "${ServerUser}@${ServerHost}:/tmp/gymmate-backend.tar.gz"
ssh -p $ServerPort "${ServerUser}@${ServerHost}" "APP_ROOT=${AppRoot} bash ${AppRoot}/backend/deploy/deploy_racknerd.sh /tmp/gymmate-backend.tar.gz"

Write-Host "Deploy finished."
