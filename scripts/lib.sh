#!/usr/bin/env bash
# Shared helpers for the QuickFlow runner scripts. Source it; do not run it.
set -euo pipefail

QF_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
QF_SELF="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
cd "$QF_ROOT"
# shellcheck disable=SC1091
source "$QF_ROOT/loops/_shared/budget.env"

PID_DIR="$QF_ROOT/run-artifacts/pids"
SERVER_LOG_DIR="$QF_ROOT/run-artifacts/logs/servers"
mkdir -p "$PID_DIR" "$SERVER_LOG_DIR"

export PLAYWRIGHT_BROWSERS_PATH="$QF_ROOT/tools/.browsers"

log()  { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*" >&2; }
die()  { log "ERROR: $*"; exit 1; }
now_iso() { date -Iseconds; }

# wait_http URL TIMEOUT_SECONDS [PID]  -> 0 when URL answers 2xx/3xx
wait_http() {
  local url="$1" timeout="${2:-120}" pid="${3:-}" i=0
  while (( i < timeout )); do
    if curl -fsS -o /dev/null --max-time 3 "$url" 2>/dev/null; then return 0; fi
    if [[ -n "$pid" ]] && ! kill -0 "$pid" 2>/dev/null; then return 1; fi
    sleep 1; i=$((i + 1))
  done
  return 1
}

# start_bg NAME LOGFILE CMD...  -> writes PID file, returns PID in $BG_PID
start_bg() {
  local name="$1" logfile="$2"; shift 2
  local pidfile="$PID_DIR/$name.pid"
  if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    die "$name already running (pid $(cat "$pidfile")). Stop it first."
  fi
  setsid "$@" >"$logfile" 2>&1 < /dev/null &
  BG_PID=$!
  echo "$BG_PID" > "$pidfile"
}

# stop_bg NAME -> kills the whole process group, waits, then SIGKILL
stop_bg() {
  local name="$1" pidfile="$PID_DIR/$1.pid" pid
  [[ -f "$pidfile" ]] || { log "$name not running (no pid file)"; return 0; }
  pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
    for _ in $(seq 1 20); do kill -0 "$pid" 2>/dev/null || break; sleep 0.5; done
    kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null || true
  fi
  rm -f "$pidfile"
  log "$name stopped"
}

# port_free PORT -> 0 if nothing listens on PORT
port_free() { ! (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

stop_all_servers() {
  local f
  for f in "$PID_DIR"/*.pid; do
    [[ -e "$f" ]] || continue
    stop_bg "$(basename "$f" .pid)"
  done
}

# budget_for MILESTONE [upgraded:0|1]
budget_for() {
  local ms="$1" up="${2:-0}"
  if [[ "$up" == "1" ]]; then echo "$BUDGET_UPGRADE_USD"; return; fi
  if [[ " $BUDGET_LARGE_MILESTONES " == *" $ms "* ]]; then echo "$BUDGET_LARGE_USD"; else echo "$BUDGET_DEFAULT_USD"; fi
}

loopctl() { python3 -I "$QF_ROOT/scripts/loopctl.py" "$@"; }

# Run one headless Claude session.
# claude_session ROLE MS TRIAL ITER MODEL MAX_BUDGET PROMPT_FILE MCP_CONFIG -> prints path of JSON output
claude_session() {
  local role="$1" ms="$2" trial="$3" iter="$4" model="$5" max_budget="$6" prompt_file="$7" mcp="$8"
  local stamp out err settings="loops/$role/settings.json"
  [[ "$role" == "planner" ]] && settings="loops/_shared/planner-settings.json"
  stamp="$(date +%Y%m%d-%H%M%S)"
  mkdir -p "run-artifacts/logs/$role"
  out="run-artifacts/logs/$role/${ms}-t${trial}-i${iter}-${stamp}.json"
  err="run-artifacts/logs/$role/${ms}-t${trial}-i${iter}-${stamp}.stderr.log"
  if [[ "${QF_DRY_RUN:-0}" == "1" ]]; then
    python3 -I "$QF_ROOT/scripts/fake_claude.py" "$role" "$ms" > "$out" 2>"$err" || true
  else
    # Run in the background and watch the wall clock: a suspend (laptop sleep) freezes the session and its
    # connection, and `timeout` does not count suspended time. A wall-clock jump > 90 s between two polls, or the
    # session timeout in wall-clock seconds, kills the session; an `.interrupted` marker tells the runner to
    # repeat the iteration without counting a trial.
    local limit_s=$(( ${SESSION_TIMEOUT%m} * 60 )) start last now
    rm -f "$out.interrupted"
    "${QF_CLAUDE_BIN:-claude}" -p "$(cat "$prompt_file")" \
      --model "$model" \
      --output-format json \
      --settings "$settings" \
      --setting-sources project \
      --permission-mode acceptEdits \
      --permission-prompts none \
      --mcp-config "$mcp" --strict-mcp-config \
      --max-budget-usd "$max_budget" \
      > "$out" 2>"$err" < /dev/null &
    local cpid=$!
    start=$(date +%s); last=$start
    while kill -0 "$cpid" 2>/dev/null; do
      sleep "${QF_POLL_SECONDS:-5}"
      now=$(date +%s)
      if (( now - last > 90 )); then
        log "wall clock jumped $((now - last))s (machine suspended?): killing the frozen $role session"
        echo "suspend" > "$out.interrupted"; break
      fi
      if (( now - start > limit_s )); then
        log "$role session exceeded ${SESSION_TIMEOUT}: killing it"
        echo "timeout" > "$out.interrupted"; break
      fi
      last=$now
    done
    if kill -0 "$cpid" 2>/dev/null; then
      pkill -TERM -P "$cpid" 2>/dev/null || true
      kill -TERM "$cpid" 2>/dev/null || true; sleep 2
      kill -KILL "$cpid" 2>/dev/null || true
    fi
    wait "$cpid" 2>/dev/null || log "claude exited non-zero for $role $ms (see $err)"
  fi
  echo "$out"
}
