#!/usr/bin/env bash
# One verification round of a milestone on the real full stack (Opus).
#   verify.sh --milestone Mxx [--base-ref HEAD]
# Starts the stable backend (:8090, fresh `stable` db, run-artifacts/backend-latest.jar) and the
# frontend (:4300 -> :8090), runs the verify session, stops everything, applies the verdict.
# Prints one of: pass | reject | upgrade | blocked | error   (exit 0, or 3 when blocked)
source "$(dirname "$0")/lib.sh"

MS="" BASE_REF="HEAD"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --milestone|--ms) MS="$2"; shift 2;;
    --base-ref) BASE_REF="$2"; shift 2;;
    -h|--help) sed -n '2,6p' "$QF_SELF"; exit 0;;
    *) die "unknown option $1";;
  esac
done
[[ -n "$MS" ]] || die "--milestone required"

VERDICT="loops/verify/state/verdict.json"
stop_stack() {
  scripts/frontend.sh stop --port 4300 >/dev/null 2>&1 || true
  scripts/backend.sh stop --port 8090 >/dev/null 2>&1 || true
}
trap stop_stack EXIT

pre="$(loopctl verify-start --ms "$MS" --base-ref "$BASE_REF")"
read -r action round model left trial <<<"$pre"
if [[ "$action" == "stop" ]]; then log "verify $MS: $round"; echo blocked; exit 3; fi
EVID="loops/verify/outputs/evidence/$MS/round-$round"
mkdir -p "$EVID"
rm -f "$VERDICT"

# Synthetic REJECT without spending a session when the stack does not even start.
infra_reject() {
  local role="$1" text="$2" logf="$3"
  cp "$logf" "$EVID/" 2>/dev/null || true
  cat >"$VERDICT" <<EOF
{ "milestone": "$MS", "round": $round, "verdict": "REJECT",
  "checks": { "review": "n/a", "swagger": "n/a", "unit": "n/a", "curl": "n/a", "e2e": "n/a", "smoke": "n/a" },
  "fixes": [], "fix_tasks": [ { "role": "$role", "text": "$text", "evidence": "$EVID/$(basename "$logf")" } ],
  "contract_issues": [], "escalate": false, "notes": "runner: stack failed to start; no verify session was run" }
EOF
  printf '# %s round %s - REJECT (runner)\n\n%s\n\nEvidence: `%s`\n' "$MS" "$round" "$text" "$EVID/$(basename "$logf")" > "loops/verify/outputs/$MS-round-$round.md"
}

if [[ "${QF_DRY_RUN:-0}" != "1" ]]; then
  log "verify $MS round $round: building and starting the stack"
  if ! scripts/backend.sh build-jar --out run-artifacts/backend-latest.jar >"$EVID/build.log" 2>&1; then
    infra_reject backend-dev "Backend does not build (./mvnw -DskipTests package fails). Fix the build." "$EVID/build.log"
  elif ! scripts/backend.sh start --port 8090 --db stable --fresh --jar run-artifacts/backend-latest.jar >"$EVID/backend-start.log" 2>&1; then
    infra_reject backend-dev "Backend jar does not start or /actuator/health is not UP on :8090. See the server log." "$EVID/backend-start.log"
    cp run-artifacts/logs/servers/backend-8090.log "$EVID/" 2>/dev/null || true
  elif ! scripts/frontend.sh start --port 4300 --api http://localhost:8090 >"$EVID/frontend-start.log" 2>&1; then
    infra_reject frontend-dev "Frontend dev server does not start on :4300 (ng serve fails). See the log." "$EVID/frontend-start.log"
    cp run-artifacts/logs/servers/frontend-4300.log "$EVID/" 2>/dev/null || true
  fi
fi

if [[ ! -f "$VERDICT" ]]; then
  mkdir -p run-artifacts/prompts/verify
  prompt="run-artifacts/prompts/verify/${MS}-round-${round}.md"
  cat >"$prompt" <<EOF
You are the verify loop of the QuickFlow project (the project root is the current directory).
Verify milestone ${MS}, round ${round} (trial ${trial} so far), on the real stack that is already running:
backend http://localhost:8090 (fresh stable database), frontend http://localhost:4300 proxying to it.

Follow loops/_shared/PROTOCOL.md and loops/verify/Loop-instructions.md. Read, in order: CLAUDE.md,
.specify/memory/constitution.md, loops/verify/Loop-instructions.md, loops/verify/task.md, loops/verify/state/handoff.md (if it exists).

Run every check (review of git diff ${BASE_REF} + untracked files, Swagger diff, ./mvnw verify, curl regression,
this milestone's Playwright scenarios end to end with the Playwright MCP, smoke scenarios of earlier milestones).
Fix small issues yourself (<= 3 files, <= ~30 lines, no design or contract change), log each fix and re-run the affected checks.
Write loops/verify/outputs/${MS}-round-${round}.md, evidence under ${EVID}/, append to loops/verify/progress.md,
rewrite loops/verify/state/handoff.md, and write loops/verify/state/verdict.json as the very last step. Then stop.
Never ask questions and never commit.
EOF
  log "verify $MS round $round: session ($model, budget left \$$left)"
  json="$(claude_session verify "$MS" "$trial" "$round" "$model" "$left" "$prompt" .mcp.json)"
  loopctl record --role verify --ms "$MS" --json "$json" --prompt-file "$prompt" --model "$model" --round "$round" >&2
fi
stop_stack

outcome="$(loopctl verdict --ms "$MS" --file "$VERDICT")"
log "verify $MS round $round: $outcome"
echo "$outcome"
[[ "$outcome" == "blocked" ]] && exit 3
exit 0
