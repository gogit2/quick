#!/usr/bin/env bash
# Run one reusable loop directly.
#
#   run-loop.sh --role backend-dev|frontend-dev --milestone Mxx [--ref US-XXX-NN]
#       Iterate (fresh headless session per iteration, ~5 tasks each) until the role's part of the
#       milestone is built (runner gate passed) or the milestone is blocked. No verification.
#   run-loop.sh --role verify --milestone Mxx
#       One verification round on the real stack (same as scripts/verify.sh).
#   run-loop.sh --role R --ref US-XXX-NN
#       Only the tasks tagged with that story, in every milestone that has some.
#   run-loop.sh --role R (--input path/to/PRD.md | --story "As a user ...") [--yes]
#       Plan a new spec first (specs/NNN-<slug>/, milestones continue the numbering), then run the role
#       over its milestones. Without --yes it stops at the approval gate after planning.
#
# Exit codes: 0 built/passed · 3 blocked · 4 awaiting approval · 2 usage error
source "$(dirname "$0")/lib.sh"

ROLE="" MS="" REF="" INPUT="" STORY="" YES=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --role) ROLE="$2"; shift 2;;
    --milestone|--ms) MS="$2"; shift 2;;
    --ref) REF="$2"; shift 2;;
    --input) INPUT="$2"; shift 2;;
    --story) STORY="$2"; shift 2;;
    --yes) YES=1; shift;;
    -h|--help) sed -n '2,18p' "$QF_SELF"; exit 0;;
    *) echo "unknown option $1" >&2; sed -n '2,18p' "$QF_SELF"; exit 2;;
  esac
done
[[ "$ROLE" =~ ^(backend-dev|frontend-dev|verify)$ ]] || { echo "--role backend-dev|frontend-dev|verify required" >&2; exit 2; }

# ---------------------------------------------------------------- planning a new spec
if [[ -n "$INPUT" || -n "$STORY" ]]; then
  args=(); [[ -n "$INPUT" ]] && args+=(--input "$INPUT"); [[ -n "$STORY" ]] && args+=(--story "$STORY"); [[ "$YES" == 1 ]] && args+=(--yes)
  st=0; "$QF_ROOT/scripts/plan-spec.sh" "${args[@]}" || st=$?
  [[ "$st" == 0 ]] || exit "$st"
fi

if [[ "$ROLE" == "verify" ]]; then
  [[ -n "$MS" ]] || { echo "--milestone required for verify" >&2; exit 2; }
  exec "$QF_ROOT/scripts/verify.sh" --milestone "$MS"
fi

# ---------------------------------------------------------------- gates (deterministic re-check)
GATE_LOG="loops/$ROLE/state/gate.log"

be_gate() {
  local ms="$1" st=0
  {
    echo "== BE gate $ms $(now_iso)"
    (cd backend && ./mvnw -q verify) || st=1
    if [[ $st == 0 ]]; then
      scripts/backend.sh start --port 8091 --db selfcheck --fresh || st=1
      if [[ $st == 0 ]]; then
        scripts/curl-regression.sh --base http://localhost:8091 --upto "$ms" --log "loops/backend-dev/outputs/evidence/$ms/gate-curl.log" || st=1
        scripts/export-swagger.sh --base http://localhost:8091 --ms "$ms" || st=1
      fi
      scripts/backend.sh stop --port 8091 || true
    fi
    echo "== BE gate result: $([[ $st == 0 ]] && echo PASS || echo FAIL)"
  } >"$GATE_LOG" 2>&1
  return $st
}

fe_gate() {
  local ms="$1" since="$2" st=0
  {
    echo "== FE gate $ms $(now_iso)"
    (cd frontend && npx ng build) || st=1
    python3 -I - "$ms" "$since" <<'PY' || st=1
import json, os, sys
ms, since = sys.argv[1], float(sys.argv[2])
p = f"loops/frontend-dev/outputs/evidence/{ms}/result.json"
try:
    r = json.load(open(p))
except Exception as e:
    sys.exit(f"{p}: missing or invalid ({e})")
if os.path.getmtime(p) < since:
    sys.exit(f"{p} was not written in this iteration")
if r.get("milestone") != ms or r.get("passed") is not True:
    sys.exit(f"{p}: milestone={r.get('milestone')} passed={r.get('passed')}")
bad = [s.get("id") for s in r.get("scenarios", []) if not s.get("passed") or s.get("console_errors", 0)]
if not r.get("scenarios") or bad:
    sys.exit(f"{p}: no scenarios or failing scenarios {bad}")
print(f"{p}: {len(r['scenarios'])} scenarios passed")
PY
    echo "== FE gate result: $([[ $st == 0 ]] && echo PASS || echo FAIL)"
  } >"$GATE_LOG" 2>&1
  return $st
}

cleanup_role_servers() {
  if [[ "$ROLE" == "backend-dev" ]]; then scripts/backend.sh stop --port 8091 >/dev/null 2>&1 || true
  else scripts/frontend.sh stop --port 4300 >/dev/null 2>&1 || true; scripts/mock.sh stop --port 4010 >/dev/null 2>&1 || true; fi
}
trap cleanup_role_servers EXIT

# ---------------------------------------------------------------- one milestone
run_milestone() {
  local ms="$1" pre action iter model left trial prompt json since gate decision res
  log "$ROLE $ms: start${REF:+ (ref $REF)}"
  while true; do
    pre="$(loopctl preflight --role "$ROLE" --ms "$ms" ${REF:+--ref "$REF"})"
    read -r action iter model left trial <<<"$pre"
    if [[ "$action" == "stop" ]]; then
      log "$ROLE $ms: $iter"
      case "$iter" in built|no-tasks) return 0;; milestone-passed) return 0;; *) return 3;; esac
    fi
    mkdir -p "run-artifacts/prompts/$ROLE"
    prompt="run-artifacts/prompts/$ROLE/${ms}-t${trial}-i${iter}.md"
    cat >"$prompt" <<EOF
You are the ${ROLE} loop of the QuickFlow project (the project root is the current directory).
This is ONE iteration: milestone ${ms}, trial ${trial}, iteration ${iter}, model ${model}${REF:+, only tasks tagged [${REF}]}.

Follow loops/_shared/PROTOCOL.md exactly. Read, in order: CLAUDE.md, .specify/memory/constitution.md,
loops/${ROLE}/Loop-instructions.md, loops/${ROLE}/task.md, loops/${ROLE}/state/handoff.md (if it exists),
then your milestone file named in task.md.

Do FIX tasks first, then at most ${TASKS_PER_ITERATION} open tasks. Run the self-check from your Loop-instructions.
Tick only finished, self-checked tasks. Append to the action log in loops/${ROLE}/progress.md, rewrite
loops/${ROLE}/state/handoff.md, and write loops/${ROLE}/state/result.json as the very last step. Then stop.
Never ask questions, never commit, never edit the contract, stop every server you started.
EOF
    rm -f "loops/$ROLE/state/result.json"
    since="$(date +%s)"
    log "$ROLE $ms: iteration $iter (trial $trial, $model, budget left \$$left)"
    json="$(claude_session "$ROLE" "$ms" "$trial" "$iter" "$model" "$left" "$prompt" "$([[ $ROLE == frontend-dev ]] && echo .mcp.json || echo loops/_shared/mcp-none.json)")"
    cleanup_role_servers
    loopctl record --role "$ROLE" --ms "$ms" --json "$json" --prompt-file "$prompt" --model "$model" --iteration "$iter" >&2
    if [[ -f "$json.interrupted" && "$(cat "$json.interrupted")" == "suspend" ]]; then
      log "$ROLE $ms: iteration $iter interrupted by a machine suspend - repeating it (no trial counted)"
      loopctl set-status --ms "$ms" --role "$ROLE" --status building >/dev/null
      continue
    fi

    gate=skip
    res="$(python3 -I -c 'import json,sys; print(json.load(open(sys.argv[1])).get("result",""))' "loops/$ROLE/state/result.json" 2>/dev/null || echo missing)"
    if [[ "$res" == "built" ]]; then
      if [[ "${QF_DRY_RUN:-0}" == "1" ]]; then gate="${QF_FAKE_GATE:-pass}"
      elif [[ "$ROLE" == "backend-dev" ]]; then be_gate "$ms" && gate=pass || gate=fail
      else fe_gate "$ms" "$since" && gate=pass || gate=fail; fi
      log "$ROLE $ms: runner gate $gate (log $GATE_LOG)"
      cleanup_role_servers
    fi
    decision="$(loopctl decide --role "$ROLE" --ms "$ms" --gate "$gate" ${REF:+--ref "$REF"})"
    log "$ROLE $ms: result=$res gate=$gate -> $decision"
    case "$decision" in
      built) return 0;;
      blocked) return 3;;
      upgrade) log "$ROLE $ms: 3 trials used - upgrading to $MODEL_UPGRADE for one retry";;
      continue) ;;
    esac
  done
}

if [[ -n "$MS" ]]; then
  run_milestone "$MS"; exit $?
fi
# no milestone given: every pending milestone in order (used with --ref / --input / --story)
rc=0
while IFS=$'\t' read -r ms status _; do
  run_milestone "$ms" || { rc=$?; break; }
done < <(loopctl ms-list --pending)
exit $rc
