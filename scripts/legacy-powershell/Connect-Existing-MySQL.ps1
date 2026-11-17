param([string]$Database='CareerUpDatabase',[string]$User='root',[int]$DatabasePort=3306,[int]$AppPort=8080,[switch]$NoBrowser)
# ============================================================
# 1. CONNECTION FROM INTELLIJ'S DATABASE PANEL
# ============================================================
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$env:DB_URL="jdbc:mysql://127.0.0.1:$DatabasePort/${Database}?connectionTimeZone=UTC"
$env:DB_USER=$User
if (!(Test-Path Env:DB_PASSWORD)) {
    $secret=Read-Host 'Enter the MySQL password you use in IntelliJ' -AsSecureString
    $env:DB_PASSWORD=[Net.NetworkCredential]::new('', $secret).Password
}
$env:PORT="$AppPort"
# ============================================================
# 2. RUN THE BUILT APP WITHOUT RECREATING TABLES
# ============================================================
$jar=Join-Path $root 'backend/target/careerup-1.0.0.jar'
if (!(Test-Path $jar)) { $jar=Join-Path $root 'release/careerup-1.0.0.jar' }
if (!(Test-Path $jar)) { throw 'Build the app first with Start-CareerUp.ps1.' }
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{(Get-Command java.exe -ErrorAction SilentlyContinue).Source}
if(!$java){$jdk=Get-ChildItem "$env:USERPROFILE/.jdks" -Directory | Sort-Object Name -Descending | Select-Object -First 1;$java=Join-Path $jdk.FullName 'bin/java.exe'}
Write-Host "Connecting to your existing MySQL database: $Database on port $DatabasePort."
Write-Host 'No schema/seed import, DROP or TRUNCATE is performed. New demo accounts/posts are added once.'
Write-Host "Open http://127.0.0.1:$AppPort after startup. Keep this terminal open; Ctrl+C stops the app."
& $java -jar $jar --spring.profiles.active=demo
