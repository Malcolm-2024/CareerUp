param([switch]$NoBrowser,[int]$AppPort=8080,[int]$DatabasePort=3307)
$ErrorActionPreference='Stop'
Push-Location (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent)
try {
    if (!(Test-Path '.git')) { throw 'This download is not a Git checkout. See START HERE.txt to import the included team branch into a clone first.' }
    $changes=& git status --porcelain
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect Git status.' }
    if ($changes) { throw 'You have local edits. Commit/push or save them before pulling. No edits were discarded.' }
    Write-Host 'Pulling the current branch (fast-forward only)...'
    & git pull --ff-only
    if ($LASTEXITCODE -ne 0) { throw 'Git pull did not succeed. Check access, branch/upstream and internet connection. The running app was not stopped.' }
    Write-Host 'Pulled successfully. Rebuilding the actual source code...'
    & "$PSScriptRoot/Stop-CareerUp.ps1" -BackendOnly
    & "$PSScriptRoot/Start-CareerUp.ps1" -Rebuild -NoBrowser:$NoBrowser -AppPort $AppPort -DatabasePort $DatabasePort
} finally { Pop-Location }
