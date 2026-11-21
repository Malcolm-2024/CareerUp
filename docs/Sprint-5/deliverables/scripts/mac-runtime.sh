#!/usr/bin/env bash
# ============================================================
# 1. OFFICIAL MAC DOWNLOADS AND SHA256 VERIFICATION
# ============================================================
mac_property() { sed -n "s/^$1=//p" "$ROOT/launcher/mac-dependencies.properties" | tr -d '\r'; }
mac_download_tool() {
  local tool="$1" destination="$2" url expected archive actual stage
  url="$(mac_property "${tool}_${MAC_ARCH}_url")"
  expected="$(mac_property "${tool}_${MAC_ARCH}_sha256")"
  [ -n "$url" ] && [ "${#expected}" = 64 ] || { echo 'Missing verified Mac dependency information.'; return 1; }
  archive="$ROOT/.local/tools/${tool}-${MAC_ARCH}.tar.gz"
  echo "First setup: downloading $tool for this Mac. Please wait..."
  if [ ! -f "$archive" ]; then
    curl --fail --location --proto '=https' --proto-redir '=https' --retry 2 --connect-timeout 20 "$url" -o "$archive.part" || return 1
    mv "$archive.part" "$archive"
  fi
  actual="$(shasum -a 256 "$archive" | awk '{print $1}')"
  [ "$actual" = "$expected" ] || { echo "Download verification failed for $tool. Nothing was executed. Remove $archive and try again."; return 1; }
  # Vendor archives must contain only relative paths inside their top-level folder.
  tar -tzf "$archive" > "$archive.contents"
  if grep -E '(^/|(^|/)\.\.(/|$))' "$archive.contents" >/dev/null; then echo 'Unsafe archive paths; setup stopped.'; return 1; fi
  stage="${destination}.extracting"
  mkdir -p "$stage"
  tar -xzf "$archive" --strip-components=1 -C "$stage" || return 1
  mv "$stage" "$destination"
}

# ============================================================
# 2. APPLE SILICON / INTEL AND PRIVATE JAVA 21
# ============================================================
prepare_mac_runtime() {
  [ "$(uname -s)" = Darwin ] || { echo 'Use CareerUp.exe on Windows. This launcher is for macOS.'; return 1; }
  MAC_ARCH="$(uname -m)"
  # An Apple Silicon Terminal running under Rosetta still needs ARM downloads.
  local translated
  translated="$(sysctl -n sysctl.proc_translated 2>/dev/null)" || translated=0
  if [ "$MAC_ARCH" = x86_64 ] && [ "$translated" = 1 ]; then MAC_ARCH=arm64; fi
  case "$MAC_ARCH" in arm64|x86_64) ;; *) echo "Unsupported Mac processor: $MAC_ARCH"; return 1;; esac
  local version major
  version="$(sw_vers -productVersion)"; major="${version%%.*}"
  [ "$major" -ge 15 ] || { echo 'This automatic Mac package requires macOS 15 or newer. Ask the team before using it on an older Mac.'; return 1; }
  mkdir -p "$ROOT/.local/tools"
  local java_home="$ROOT/.local/tools/java-$MAC_ARCH"
  if [ ! -x "$java_home/Contents/Home/bin/java" ]; then mac_download_tool java "$java_home" || return 1; fi
  JAVA="$java_home/Contents/Home/bin/java"
  export JAVA_HOME="$java_home/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
  "$JAVA" -version || return 1

  # ============================================================
  # 3. EXISTING MYSQL 8 TOOLS OR VERIFIED PRIVATE MYSQL
  # ============================================================
  local folder mysql_home="$ROOT/.local/tools/mysql-$MAC_ARCH"
  for folder in /opt/homebrew/opt/mysql@8.4/bin /opt/homebrew/opt/mysql@8.0/bin /usr/local/mysql/bin /usr/local/opt/mysql@8.0/bin; do
    [ ! -d "$folder" ] || export PATH="$folder:$PATH"
  done
  if ! command -v mysql >/dev/null || ! command -v mysqld >/dev/null || ! mysqld --version 2>/dev/null | grep -Eq 'Ver 8\.'; then
    if [ ! -x "$mysql_home/bin/mysqld" ]; then mac_download_tool mysql "$mysql_home" || return 1; fi
    export PATH="$mysql_home/bin:$PATH"
  fi
  mysqld --version || return 1
}

# ============================================================
# 4. BUILD TOOLS FOR THE TEAM'S EDIT / PUSH / PULL DEMONSTRATION
# ============================================================
prepare_mac_build_tools() {
  local node_home="$ROOT/.local/tools/node-$MAC_ARCH"
  if ! command -v node >/dev/null || ! node -e 'process.exit(Number(process.versions.node.split(".")[0])>=22?0:1)'; then
    if [ ! -x "$node_home/bin/node" ]; then mac_download_tool node "$node_home" || return 1; fi
    export PATH="$node_home/bin:$PATH"
  fi
}
