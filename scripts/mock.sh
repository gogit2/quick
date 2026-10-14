#!/usr/bin/env bash
# Start/stop a Prism mock of the contract (stateless; responses from examples/schemas).
#   mock.sh start  [--port 4010] [--spec specs/<NNN>/contracts/openapi.yaml]
#   mock.sh stop   [--port 4010]
# Default spec: the spec folder recorded in loops/_shared/milestones.json.
source "$(dirname "$0")/lib.sh"

cmd="${1:-}"; shift || true
[[ "$cmd" =~ ^(-h|--help|)$ ]] && { sed -n '2,5p' "$QF_SELF"; exit 0; }
PORT=4010 SPEC=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --port) PORT="$2"; shift 2;;
    --spec) SPEC="$2"; shift 2;;
    -h|--help) sed -n '2,5p' "$QF_SELF"; exit 0;;
    *) die "unknown option $1";;
  esac
done
NAME="mock-$PORT"
if [[ "$cmd" == start && -z "$SPEC" ]]; then SPEC="$(loopctl get-spec)/contracts/openapi.yaml"; fi

case "$cmd" in
  start)
    [[ -f "$SPEC" ]] || die "contract not found: $SPEC"
    port_free "$PORT" || die "port $PORT is already in use"
    start_bg "$NAME" "$SERVER_LOG_DIR/$NAME.log" tools/node_modules/.bin/prism mock -h 127.0.0.1 -p "$PORT" "$SPEC"
    for _ in $(seq 1 60); do
      grep -q "Prism is listening" "$SERVER_LOG_DIR/$NAME.log" 2>/dev/null && break
      kill -0 "$BG_PID" 2>/dev/null || break
      sleep 1
    done
    grep -q "Prism is listening" "$SERVER_LOG_DIR/$NAME.log" || { tail -20 "$SERVER_LOG_DIR/$NAME.log" >&2; stop_bg "$NAME"; die "mock did not start"; }
    log "$NAME ready at http://localhost:$PORT ($SPEC)";;
  stop) stop_bg "$NAME";;
  *) sed -n '2,5p' "$QF_SELF"; exit 2;;
esac
