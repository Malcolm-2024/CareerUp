param([switch]$BackendOnly)
$ErrorActionPreference='Stop'
$targets=if($BackendOnly){@('backend')}else{@('backend','mysql')}
foreach ($name in $targets) {
    $pidFile=Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) ".local/$name.pid"
    if (Test-Path $pidFile) {
        $ownedId=[int](Get-Content $pidFile)
        $process=Get-Process -Id $ownedId -ErrorAction SilentlyContinue
        $expected=if($name -eq 'backend'){'java'}else{'mysqld'}
        if ($process -and $process.ProcessName -eq $expected -and $process.StartTime -le (Get-Item $pidFile).LastWriteTime) { Stop-Process -Id $ownedId }
    }
}
Write-Host 'Stopped launcher-owned CareerUp processes. Demo data is retained.'
