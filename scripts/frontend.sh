#!/usr/bin/env bash
# Start/stop the Angular dev server (ng serve) with a PID file and a health wait.
#   frontend.sh start  [--port 4300] [--api http://localhost:8090]
#   frontend.sh stop   [--port 4300]
#   frontend.sh status [--port 4300]
# The dev server proxies API calls to $API_TARGET via frontend/proxy.conf.js.
source "$(dirname "$0")/lib.sh"

cmd="${1:-}"; shift || true
[[ "$cmd" =~ ^(-h|--help|)$ ]] && { sed -n '2,6p' "$QF_SELF"; exit 0; }
PORT=4300 API="http://localhost:8090"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --port) PORT="$2"; shift 2;;
    --api) API="$2"; shift 2;;
    -h|--help) sed -n '2,6p' "$QF_SELF"; exit 0;;
    *) die "unknown option $1";;
  esac
done
NAME="frontend-$PORT"

start() {
  [[ -f frontend/package.json ]] || die "frontend/package.json not found - the frontend project does not exist yet"
  [[ -f frontend/proxy.conf.js ]] || die "frontend/proxy.conf.js missing (required by the constitution)"
  port_free "$PORT" || die "port $PORT is already in use"
  [[ -d frontend/node_modules ]] || (cd frontend && npm ci --no-audit --no-fund >&2)
  local logf="$SERVER_LOG_DIR/$NAME.log"
  ( cd frontend && export API_TARGET="$API" && start_bg "$NAME" "$logf" npx ng serve --port "$PORT" --host 127.0.0.1 --proxy-config proxy.conf.js --no-open --no-hmr )
  local pid; pid="$(cat "$PID_DIR/$NAME.pid")"
  log "$NAME starting (pid $pid, api $API, log $logf)"
  if wait_http "http://127.0.0.1:$PORT/" 240 "$pid"; then
    log "$NAME ready at http://localhost:$PORT"
  else
    tail -40 "$logf" >&2; stop_bg "$NAME"; die "$NAME did not start"
  fi
}

case "$cmd" in
  start) start;;
  stop) stop_bg "$NAME";;
  status) if [[ -f "$PID_DIR/$NAME.pid" ]] && kill -0 "$(cat "$PID_DIR/$NAME.pid")" 2>/dev/null; then echo "running"; else echo "stopped"; exit 1; fi;;
  *) sed -n '2,6p' "$QF_SELF"; exit 2;;
esac
