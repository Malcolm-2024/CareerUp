param([switch]$Rebuild, [switch]$NoBrowser, [int]$AppPort=8080, [int]$DatabasePort=3307)
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object Text.UTF8Encoding($false)
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$local = Join-Path $root '.local'
$appUrl = "http://127.0.0.1:$AppPort"
New-Item -ItemType Directory -Force $local | Out-Null
function PortOpen([int]$port) {
    $client = New-Object Net.Sockets.TcpClient
    try { $task = $client.ConnectAsync('127.0.0.1', $port); return ($task.Wait(500) -and $client.Connected) } catch { return $false } finally { $client.Dispose() }
}
if ((PortOpen $AppPort) -and !(Test-Path (Join-Path $local 'backend.pid'))) {
    throw "Another copy is using port $AppPort. Stop that copy first, or run with -AppPort 8081 -DatabasePort 3317."
}
function FindJava {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/java.exe")) { return $env:JAVA_HOME }
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd) { return (Split-Path (Split-Path $cmd.Source)) }
    foreach ($folder in @(Get-ChildItem "$env:USERPROFILE/.jdks" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending)) {
        if (Test-Path "$($folder.FullName)/bin/java.exe") { return $folder.FullName }
    }
    throw 'Install JDK 21 or newer, or set JAVA_HOME to your JDK folder.'
}
$env:JAVA_HOME = FindJava
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$mysqlHome = if ($env:MYSQL_HOME) { $env:MYSQL_HOME } else { "$env:ProgramFiles/MySQL/MySQL Server 8.0" }
$mysql = Join-Path $mysqlHome 'bin/mysql.exe'
$mysqld = Join-Path $mysqlHome 'bin/mysqld.exe'
if (!(Test-Path $mysql) -or !(Test-Path $mysqld)) { throw 'MySQL Server 8 is required. Install it or set MYSQL_HOME to its installation directory.' }
$dbConfigPath = Join-Path $local 'database.json'
if ((PortOpen $DatabasePort) -and !(Test-Path $dbConfigPath)) {
    throw "Port $DatabasePort is already in use by another database. No data was changed. Stop the other demo, or choose another -DatabasePort."
}
if (Test-Path $dbConfigPath) { $dbConfig = Get-Content $dbConfigPath -Raw | ConvertFrom-Json }
else {
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $dbConfig = @{ password=[Convert]::ToBase64String($bytes); adminPassword='' }
    $dbConfig | ConvertTo-Json | Set-Content $dbConfigPath
}
$dataDirectory = Join-Path $local 'mysql-data'
# Older local setups retain their existing data location. Shared packages never include this file.
if ($dbConfig.dataDirectory) { $dataDirectory = $dbConfig.dataDirectory }
if (!(PortOpen $DatabasePort)) {
    if (!(Test-Path (Join-Path $dataDirectory 'mysql'))) {
        New-Item -ItemType Directory -Force $dataDirectory | Out-Null
        & $mysqld --no-defaults --initialize-insecure "--datadir=$dataDirectory" --console
        if ($LASTEXITCODE -ne 0) { throw 'Could not initialize the separate local demo database.' }
    }
    $p = Start-Process -FilePath $mysqld -ArgumentList @('--no-defaults',('"--datadir='+$dataDirectory+'"'),"--port=$DatabasePort",'--bind-address=127.0.0.1','--mysqlx=0','--console') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $local 'mysql.log') -RedirectStandardError (Join-Path $local 'mysql-error.log')
    $p.Id | Set-Content (Join-Path $local 'mysql.pid')
    for ($n=0;$n -lt 30 -and !(PortOpen $DatabasePort);$n++) { Start-Sleep -Milliseconds 500 }
    if (!(PortOpen $DatabasePort)) { throw "Demo database did not start. Check $local/mysql-error.log" }
}
$previousMysqlPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $dbConfig.adminPassword
    $dbExists = & $mysql --host=127.0.0.1 "--port=$DatabasePort" -u root -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='careerup_sprint4';"
    if ($LASTEXITCODE -ne 0) { throw "Port $DatabasePort is occupied by a server this launcher cannot administer. No existing data was changed." }
    if ($dbExists -eq '0') {
        "CREATE DATABASE careerup_sprint4 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | & $mysql --host=127.0.0.1 "--port=$DatabasePort" -u root
        if ($LASTEXITCODE -ne 0) { throw 'Database creation failed.' }
        Get-Content (Join-Path $root 'db/sprint4_schema.sql') -Raw | & $mysql --host=127.0.0.1 "--port=$DatabasePort" -u root --default-character-set=utf8mb4 careerup_sprint4
        if ($LASTEXITCODE -ne 0) { throw 'Schema import failed.' }
        Get-Content (Join-Path $root 'db/sprint4_seed.sql') -Raw | & $mysql --host=127.0.0.1 "--port=$DatabasePort" -u root --default-character-set=utf8mb4 careerup_sprint4
        if ($LASTEXITCODE -ne 0) { throw 'Seed import failed.' }
    }
    # This account is limited to this demo database. Secrets stay in ignored local state.
    "CREATE USER IF NOT EXISTS 'careerup_app'@'localhost' IDENTIFIED BY '$($dbConfig.password)'; GRANT SELECT,INSERT,UPDATE ON careerup_sprint4.* TO 'careerup_app'@'localhost';" | & $mysql --host=127.0.0.1 "--port=$DatabasePort" -u root
    if ($LASTEXITCODE -ne 0) { throw 'Demo application account setup failed.' }
} finally { $env:MYSQL_PWD = $previousMysqlPassword }
$jar = Join-Path $root 'backend/target/careerup-1.0.0.jar'
$packagedJar = Join-Path $root 'release/careerup-1.0.0.jar'
if (!$Rebuild -and !(Test-Path $jar) -and (Test-Path $packagedJar)) { $jar=$packagedJar }
if ($Rebuild -or !(Test-Path $jar)) {
    if (PortOpen $AppPort) { throw "Stop CareerUp on port $AppPort before rebuilding its running application." }
    if (!(Get-Command node.exe -ErrorAction SilentlyContinue)) { throw 'Install Node.js 22 or newer before building.' }
    Push-Location (Join-Path $root 'frontend')
    try {
        $npmCache=if($env:CAREERUP_NPM_CACHE){$env:CAREERUP_NPM_CACHE}else{Join-Path $local 'npm-cache'}
        & npm.cmd ci --ignore-scripts --no-audit --no-fund --cache $npmCache
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    } finally { Pop-Location }
    & (Join-Path $root 'scripts/Build-Frontend.ps1')
    New-Item -ItemType Directory -Force (Join-Path $root 'backend/src/main/resources/static') | Out-Null
    Copy-Item (Join-Path $root 'frontend/dist/*') (Join-Path $root 'backend/src/main/resources/static') -Recurse -Force
    $maven = Join-Path $local 'apache-maven-3.9.11/bin/mvn.cmd'
    if ($env:MAVEN_HOME -and (Test-Path (Join-Path $env:MAVEN_HOME 'bin/mvn.cmd'))) { $maven=Join-Path $env:MAVEN_HOME 'bin/mvn.cmd' }
    if (!(Test-Path $maven)) {
        $archive = Join-Path $local 'maven.zip'
        & node.exe (Join-Path $root 'scripts/download.mjs') 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' $archive
        if ($LASTEXITCODE -ne 0) { throw 'Could not download Maven.' }
        Expand-Archive $archive -DestinationPath $local -Force
    }
    $mavenRepo=if($env:CAREERUP_MAVEN_REPO){$env:CAREERUP_MAVEN_REPO}else{Join-Path $local 'm2'}
    & $maven -B -ntp "-Dmaven.repo.local=$mavenRepo" -f (Join-Path $root 'backend/pom.xml') package
    if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
}
if (!(PortOpen $AppPort)) {
    $env:DB_URL="jdbc:mysql://127.0.0.1:$DatabasePort/careerup_sprint4?connectionTimeZone=UTC"
    $env:PORT="$AppPort"
    $env:DB_USER='careerup_app'; $env:DB_PASSWORD=$dbConfig.password
    $p = Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$jar+'"'),'--spring.profiles.active=demo') -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $local 'backend.log') -RedirectStandardError (Join-Path $local 'backend-error.log')
    $p.Id | Set-Content (Join-Path $local 'backend.pid')
    for ($n=0;$n -lt 40;$n++) {
        try { $health=Invoke-RestMethod "$appUrl/api/health" -TimeoutSec 2; if ($health.status -eq 'UP') { break } } catch {}
        Start-Sleep -Milliseconds 500
    }
}
try { $health=Invoke-RestMethod "$appUrl/api/health"; if ($health.database -ne 'UP') { throw 'Database unavailable.' } } catch { throw "CareerUp did not become ready. Check $local/backend.log" }
Write-Host "CareerUp is running: $appUrl"
Write-Host 'Demo password for all five roles: CareerUp4!'
if (!$NoBrowser) { Start-Process $appUrl -WindowStyle Hidden }
