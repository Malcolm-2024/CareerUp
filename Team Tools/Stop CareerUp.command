#!/usr/bin/env bash
cd "$(dirname "$0")/.."
bash scripts/run-mac.sh --stop
status=$?
if [ "$status" -ne 0 ]; then read -r -p "Press Return to close..." _; fi
exit "$status"
