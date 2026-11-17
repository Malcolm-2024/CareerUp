#!/usr/bin/env bash
# ============================================================
# 1. PROJECT FOLDER AND RUN OPTIONS
# ============================================================
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
MODE="${1:---start}"
mkdir -p .local
APP_PORT="${PORT:-8080}"
DB_PORT="${CAREERUP_DB_PORT:-3307}"
APP_URL="http://127.0.0.1:$APP_PORT"

# ============================================================
# 2. STOP ONLY THIS PROJECT'S BACKEND
# ============================================================
stop_backend() {
  if [ -f .local/backend.pid ]; then
    PID="$(cat .local/backend.pid)"
    case "$PID" in ''|*[!0-9]*) return;; esac
    COMMAND="$(ps -p "$PID" -o command= 2>/dev/null)" || COMMAND=""
    case "$COMMAND" in *"$ROOT/"*careerup-1.0.0.jar*) kill "$PID" 2>/dev/null || true;; esac
  fi
}
stop_database() {
  if [ -f .local/mysql.pid ]; then
    DB_PID="$(cat .local/mysql.pid)"
    case "$DB_PID" in ''|*[!0-9]*) return;; esac
    DB_COMMAND="$(ps -p "$DB_PID" -o command= 2>/dev/null)" || DB_COMMAND=""
    case "$DB_COMMAND" in *"--datadir=$ROOT/.local/mysql-data"*) kill "$DB_PID" 2>/dev/null || true;; esac
  fi
}
if [ "$MODE" = '--stop' ]; then stop_backend; stop_database; echo 'CareerUp stopped. Demo data retained.'; exit 0; fi

# ============================================================
# 3. SAFE TEAM PULL AND REBUILD
# ============================================================
if [ "$MODE" = '--update' ]; then
  [ -d .git ] || { echo 'Use the Git checkout for team updates, not the downloaded ZIP.'; exit 1; }
  [ -z "$(git status --porcelain)" ] || { echo 'Commit or save local edits before pulling. Nothing was discarded.'; exit 1; }
  git pull --ff-only
  stop_backend
  MODE='--rebuild'
elif [ "$MODE" = '--rebuild' ]; then stop_backend; fi

# ============================================================
# 4. JAVA AND MYSQL TOOLS
# ============================================================
. "$ROOT/scripts/mac-runtime.sh"
prepare_mac_runtime

# ============================================================
# 5. SEPARATE LOCAL DEMO DATABASE (OR USE YOUR EXISTING CONNECTION)
# ============================================================
if [ "${CAREERUP_USE_EXISTING_DB:-0}" != '1' ]; then
  if [ ! -f .local/database.password ]; then
    if mysqladmin --host=127.0.0.1 --port="$DB_PORT" ping >/dev/null 2>&1; then
      echo "Port $DB_PORT is occupied by another database; choose CAREERUP_DB_PORT or stop the other demo."
      exit 1
    fi
    openssl rand -hex 24 > .local/database.password
    chmod 600 .local/database.password
  fi
  if ! mysqladmin --host=127.0.0.1 --port="$DB_PORT" ping >/dev/null 2>&1; then
    if [ ! -d .local/mysql-data/mysql ]; then
      mkdir -p .local/mysql-data
      mysqld --no-defaults --initialize-insecure --datadir="$ROOT/.local/mysql-data"
    fi
    nohup mysqld --no-defaults --datadir="$ROOT/.local/mysql-data" --port="$DB_PORT" --bind-address=127.0.0.1 --socket="$ROOT/.local/mysql.sock" --mysqlx=0 > .local/mysql.log 2>&1 </dev/null &
    echo $! > .local/mysql.pid
    for _ in $(seq 1 120); do mysqladmin --host=127.0.0.1 --port="$DB_PORT" -u root ping >/dev/null 2>&1 && break; sleep .5; done
  fi
  MYSQL=(mysql --host=127.0.0.1 --port="$DB_PORT" -u root --default-character-set=utf8mb4)
  EXISTS="$("${MYSQL[@]}" -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='careerup_sprint4';")"
  if [ "$EXISTS" = '0' ]; then
    "${MYSQL[@]}" -e 'CREATE DATABASE careerup_sprint4 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
    "${MYSQL[@]}" careerup_sprint4 < db/sprint4_schema.sql
    "${MYSQL[@]}" careerup_sprint4 < db/sprint4_seed.sql
  fi
  export DB_PASSWORD="$(cat .local/database.password)"
  printf "CREATE USER IF NOT EXISTS 'careerup_app'@'localhost' IDENTIFIED BY '%s'; GRANT SELECT,INSERT,UPDATE ON careerup_sprint4.* TO 'careerup_app'@'localhost';" "$DB_PASSWORD" | "${MYSQL[@]}"
  export DB_USER='careerup_app'
  export DB_URL="jdbc:mysql://127.0.0.1:$DB_PORT/careerup_sprint4?connectionTimeZone=UTC"
else
  : "${DB_URL:?Set DB_URL to the MySQL connection shown in IntelliJ}"
  : "${DB_USER:?Set DB_USER to your MySQL username}"
fi

# ============================================================
# 6. BUILD SOURCE ONLY WHEN NEEDED
# ============================================================
JAR="$ROOT/backend/target/careerup-1.0.0.jar"
if [ "$MODE" != '--rebuild' ] && [ ! -f "$JAR" ] && [ -f release/careerup-1.0.0.jar ]; then JAR="$ROOT/release/careerup-1.0.0.jar"; fi
if [ "$MODE" = '--rebuild' ] || [ ! -f "$JAR" ]; then
  prepare_mac_build_tools
  (cd frontend && npm ci --ignore-scripts --no-audit --no-fund && npm run build)
  mkdir -p backend/src/main/resources/static
  cp -R frontend/dist/. backend/src/main/resources/static/
  MAVEN="$ROOT/.local/apache-maven-3.9.11/bin/mvn"
  if [ ! -f "$MAVEN" ]; then
    curl -fL 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -o .local/maven.zip
    MAVEN_EXPECTED="$(sed -n 's/^maven.sha256=//p' launcher/dependencies.properties | tr -d '\r')"
    MAVEN_ACTUAL="$(shasum -a 256 .local/maven.zip | awk '{print $1}')"
    [ "$MAVEN_ACTUAL" = "$MAVEN_EXPECTED" ] || { echo 'Maven download verification failed; setup stopped.'; exit 1; }
    unzip -q -o .local/maven.zip -d .local
    chmod +x "$MAVEN"
  fi
  "$MAVEN" -B -ntp "-Dmaven.repo.local=$ROOT/.local/m2" -f backend/pom.xml package
fi

# ============================================================
# 7. START APPLICATION AND OPEN BROWSER
# ============================================================
if ! curl -fsS "$APP_URL/api/health" >/dev/null 2>&1; then
  export PORT="$APP_PORT"
  nohup "$JAVA" -jar "$JAR" --spring.profiles.active=demo > .local/backend.log 2>&1 </dev/null &
  echo $! > .local/backend.pid
  for _ in $(seq 1 120); do curl -fsS "$APP_URL/api/health" >/dev/null 2>&1 && break; sleep .5; done
fi
curl -fsS "$APP_URL/api/health" >/dev/null || { echo 'Startup failed. Read .local/backend.log'; exit 1; }
echo "CareerUp: $APP_URL (all demo passwords: CareerUp4!)"
open "$APP_URL"
