#!/usr/bin/env bash
# Milestone pipeline with a strict barrier:  backend-dev || frontend-dev  ->  verify  ->  commit + tag  ->  next.
#
#   pipeline.sh [--from Mxx] [--only Mxx] [--ref US-XXX-NN]
#   pipeline.sh (--input path/to/PRD.md | --story "As a user ...") [--yes]   plan a new spec first
#
# Resumable: milestones already passed are skipped; state lives in loops/_shared/milestones.json.
# Exit codes: 0 all passed · 3 a milestone is blocked (decision needed) · 4 awaiting approval
source "$(dirname "$0")/lib.sh"

FROM="" ONLY="" REF="" INPUT="" STORY="" YES=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --from) FROM="$2"; shift 2;;
    --only) ONLY="$2"; shift 2;;
    --ref) REF="$2"; shift 2;;
    --input) INPUT="$2"; shift 2;;
    --story) STORY="$2"; shift 2;;
    --yes) YES=1; shift;;
    -h|--help) sed -n '2,9p' "$QF_SELF"; exit 0;;
    *) die "unknown option $1";;
  esac
done

exec 9>"run-artifacts/pipeline.lock"
flock -n 9 || die "another pipeline is running"
trap stop_all_servers EXIT
git rev-parse --git-dir >/dev/null 2>&1 || die "not a git repository"

if [[ -n "$INPUT" || -n "$STORY" ]]; then
  args=(); [[ -n "$INPUT" ]] && args+=(--input "$INPUT"); [[ -n "$STORY" ]] && args+=(--story "$STORY"); [[ "$YES" == 1 ]] && args+=(--yes)
  st=0; scripts/plan-spec.sh "${args[@]}" || st=$?
  [[ "$st" == 0 ]] || exit "$st"
fi

PIPE_LOG_DIR="run-artifacts/logs/pipeline"; mkdir -p "$PIPE_LOG_DIR"

commit_milestone() {
  local ms="$1" title="$2" round
  round="$(loopctl get --ms "$ms" verify_rounds)"
  loopctl set-status --ms "$ms" --status passed --tag "$ms"
  loopctl summary >/dev/null
  git add -A
  git commit -q -m "$ms: $title (verified, round $round)" -m "Built by the backend-dev and frontend-dev loops, verified by the verify loop.
Sessions: run-artifacts/sessions.csv · Report: loops/verify/outputs/$ms-round-$round.md" \
    -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
  git tag -a "$ms" -m "$ms: $title"
  log "$ms committed and tagged"
}

run_ms() {
  local ms="$1" title="$2" roles pids rc outcome r
  log "===== $ms $title"
  while true; do
    [[ "$(loopctl get --ms "$ms" status)" == "blocked" ]] && return 3
    roles="$(loopctl roles-to-run --ms "$ms")"
    declare -A pids=()
    for r in $roles; do
      scripts/run-loop.sh --role "$r" --milestone "$ms" ${REF:+--ref "$REF"} >"$PIPE_LOG_DIR/$ms-$r.log" 2>&1 &
      pids[$r]=$!
      log "$ms: $r started (pid ${pids[$r]}, log $PIPE_LOG_DIR/$ms-$r.log)"
    done
    rc=0
    for r in "${!pids[@]}"; do
      wait "${pids[$r]}" || { st=$?; log "$ms: $r exited $st"; rc=$st; }
    done
    if [[ "$rc" != 0 || "$(loopctl get --ms "$ms" status)" == "blocked" ]]; then
      loopctl set-status --ms "$ms" --status blocked --reason "$(loopctl get --ms "$ms" blocked_reason || echo 'a loop stopped')"
      return 3
    fi
    log "$ms: both loops built - verifying"
    outcome="$(scripts/verify.sh --milestone "$ms" --base-ref HEAD 2>>"$PIPE_LOG_DIR/$ms-verify.log" | tail -1)" || true
    log "$ms: verify -> $outcome"
    case "$outcome" in
      pass) commit_milestone "$ms" "$title"; return 0;;
      reject|upgrade|error) continue;;
      *) return 3;;
    esac
  done
}

started=0
while IFS=$'\t' read -r ms status title; do
  [[ -n "$ONLY" && "$ms" != "$ONLY" ]] && continue
  if [[ -n "$FROM" && $started == 0 && "$ms" < "$FROM" ]]; then continue; fi
  started=1
  if [[ "$status" == "blocked" ]]; then
    log "$ms is blocked: $(loopctl get --ms "$ms" blocked_reason). Resolve it (see README 'Blocked milestones') and re-run."
    exit 3
  fi
  st=0; run_ms "$ms" "$title" || st=$?
  if [[ "$st" != 0 ]]; then
    loopctl status
    log "PIPELINE STOPPED: $ms is blocked - $(loopctl get --ms "$ms" blocked_reason)"
    exit 3
  fi
done < <(loopctl ms-list --pending)

loopctl summary
loopctl status
log "pipeline finished"
