#!/usr/bin/env bash
# Start/stop the Spring Boot backend with a PID file and a health wait.
#   backend.sh start   --port 8091 --db selfcheck [--fresh] [--jar path] [--no-build]
#   backend.sh restart --port 8090 --db stable [--fresh] [--jar path]
#   backend.sh stop    --port 8091
#   backend.sh status  --port 8091
#   backend.sh build-jar [--out run-artifacts/backend-latest.jar]
source "$(dirname "$0")/lib.sh"

cmd="${1:-}"; shift || true
[[ "$cmd" =~ ^(-h|--help|)$ ]] && { sed -n '2,7p' "$QF_SELF"; exit 0; }
PORT=8091 DB=selfcheck FRESH=0 JAR="" BUILD=1 OUT="run-artifacts/backend-latest.jar"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --port) PORT="$2"; shift 2;;
    --db) DB="$2"; shift 2;;
    --fresh) FRESH=1; shift;;
    --jar) JAR="$2"; shift 2;;
    --no-build) BUILD=0; shift;;
    --out) OUT="$2"; shift 2;;
    -h|--help) sed -n '2,7p' "$QF_SELF"; exit 0;;
    *) die "unknown option $1";;
  esac
done
NAME="backend-$PORT"
HEALTH="http://localhost:$PORT/actuator/health"

build_jar() {
  [[ -x backend/mvnw ]] || die "backend/mvnw not found - the backend project does not exist yet"
  log "building backend jar"
  (cd backend && ./mvnw -q -DskipTests package) >&2
  local jar
  jar="$(ls -t backend/target/*.jar 2>/dev/null | grep -v -- '-plain' | head -1)"
  [[ -n "$jar" ]] || die "no jar in backend/target"
  echo "$jar"
}

start() {
  port_free "$PORT" || die "port $PORT is already in use"
  local jar="$JAR"
  if [[ -z "$jar" ]]; then
    if [[ "$BUILD" == "1" ]]; then jar="$(build_jar)"; else jar="$(ls -t backend/target/*.jar | head -1)"; fi
  fi
  [[ -f "$jar" ]] || die "jar not found: $jar"
  mkdir -p backend/data
  if [[ "$FRESH" == "1" ]]; then rm -f "backend/data/$DB".*.db "backend/data/$DB".trace.db; fi
  local logf="$SERVER_LOG_DIR/$NAME.log"
  start_bg "$NAME" "$logf" java -jar "$jar" \
    --server.port="$PORT" \
    --qf.db.path="$QF_ROOT/backend/data/$DB"
  log "$NAME starting (pid $BG_PID, db $DB, jar $jar, log $logf)"
  if wait_http "$HEALTH" 180 "$BG_PID"; then
    log "$NAME healthy at $HEALTH"
  else
    tail -40 "$logf" >&2
    stop_bg "$NAME"
    die "$NAME did not become healthy"
  fi
}

case "$cmd" in
  start) start;;
  stop) stop_bg "$NAME";;
  restart) stop_bg "$NAME"; start;;
  status) if [[ -f "$PID_DIR/$NAME.pid" ]] && kill -0 "$(cat "$PID_DIR/$NAME.pid")" 2>/dev/null; then echo "running pid $(cat "$PID_DIR/$NAME.pid")"; else echo "stopped"; exit 1; fi;;
  build-jar) jar="$(build_jar)"; mkdir -p "$(dirname "$OUT")"; cp "$jar" "$OUT"; log "copied $jar -> $OUT";;
  *) sed -n '2,7p' "$QF_SELF"; exit 2;;
esac
