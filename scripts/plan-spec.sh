#!/usr/bin/env bash
# Headless planning of a NEW spec from a full requirements file or a single user story (Sonnet).
#   plan-spec.sh (--input path/to/PRD.md | --story "As a user ...") [--yes]
# Creates specs/NNN-<slug>/ with spec, plan, data model, the FULL contract (previous contract + changes),
# tasks grouped into milestones that continue the numbering, and acceptance.md. Open questions are
# recorded as "Assumed" entries in requirements/DECISIONS.md (a headless run cannot ask).
# Then splits the tasks into the loop folders. Without --yes it stops at the approval gate (exit 4).
source "$(dirname "$0")/lib.sh"

INPUT="" STORY="" YES=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --input) INPUT="$2"; shift 2;;
    --story) STORY="$2"; shift 2;;
    --yes) YES=1; shift;;
    -h|--help) sed -n '2,8p' "$QF_SELF"; exit 0;;
    *) die "unknown option $1";;
  esac
done
[[ -n "$INPUT" || -n "$STORY" ]] || die "--input or --story required"
[[ -z "$INPUT" || -f "$INPUT" ]] || die "input file not found: $INPUT"

last_ms="$(loopctl ms-list | tail -1 | cut -f1)"; last_ms="${last_ms:-M00}"
next_ms="$(printf 'M%02d' $((10#${last_ms#M} + 1)))"
prev_spec="$(loopctl get-spec 2>/dev/null || echo none)"
what="${INPUT:+the requirements file \`$INPUT\`}${STORY:+this user story: \"$STORY\"}"

mkdir -p run-artifacts/prompts/plan
prompt="run-artifacts/prompts/plan/plan-$(date +%Y%m%d-%H%M%S).md"
cat >"$prompt" <<EOF
You are the planning step of the QuickFlow loops (headless; the project root is the current directory).
Plan a NEW spec for ${what}.

Rules: CLAUDE.md and .specify/memory/constitution.md apply. Do not write application code. Never ask questions:
where the input is ambiguous, decide and record it under a new heading "## <spec folder> (assumed - needs review)"
in requirements/DECISIONS.md as "- D-<n>: <question> -> <decision> (assumed)".

Steps (use the Spec Kit skills):
1. /speckit-specify for the input above (it creates the next specs/NNN-<slug>/ folder). Give every user story an id US-<AREA>-<NN>.
2. Clarify without asking (record assumptions as above) and encode them in spec.md.
3. /speckit-plan. contracts/openapi.yaml must be the FULL API: start from the previous contract (${prev_spec}/contracts/openapi.yaml,
   if it exists) and add/change only what this spec needs. Every operation has "x-milestone: Mxx" and examples.
4. /speckit-tasks with milestones that continue the numbering at ${next_ms}. Use exactly this shape:
   "## Milestone Mxx: <title>", a "**Goal**: ..." line, then tasks "- [ ] Tnnn [P?] [BE|FE] [US-...] description (files)".
   Task ids continue after the highest id used in earlier specs. Every task has exactly one of [BE] or [FE].
5. Write acceptance.md in the spec folder: per milestone, curl cases "AC-Mxx-nn" and Playwright scenarios "E2E-Mxx-nn"
   (steps + data-testids), each linked to a user story or requirement; mark regression-worthy ones [smoke].
6. /speckit-analyze and fix every CRITICAL/HIGH finding.
Finish with a short summary of the milestones and the assumptions you recorded.
EOF
log "planning a new spec (${MODEL_PLAN}); first new milestone $next_ms"
json="$(QF_PLAN=1 claude_session planner "PLAN" 0 0 "$MODEL_PLAN" 10 "$prompt" loops/_shared/mcp-none.json)"
python3 -I - "$json" "$prompt" <<'PY'
import csv, json, sys, os
from datetime import datetime
raw = json.load(open(sys.argv[1])) if os.path.getsize(sys.argv[1]) else {}
row = {"timestamp": datetime.now().astimezone().isoformat(timespec="seconds"), "phase": "headless-plan",
       "session_id": raw.get("session_id", ""), "model": os.environ.get("MODEL_PLAN", ""), "prompt": open(sys.argv[2]).read(),
       "notes": f"cost_usd={raw.get('total_cost_usd', 0)}; log={sys.argv[1]}"}
path = "run-artifacts/manual-prompts.csv"
new = not os.path.exists(path) or os.path.getsize(path) == 0
with open(path, "a", newline="") as fh:
    w = csv.DictWriter(fh, fieldnames=list(row)); new and w.writeheader(); w.writerow(row)
PY
new_spec="$(ls -d specs/[0-9][0-9][0-9]-* | sort | tail -1)"
[[ "$new_spec" != "$prev_spec" ]] || die "planning did not create a new spec folder (see $json)"
python3 -I scripts/split-tasks.py --spec "$new_spec"
if [[ "$YES" != 1 ]]; then
  log "APPROVAL GATE: review $new_spec and the assumed decisions in requirements/DECISIONS.md, then run scripts/pipeline.sh"
  exit 4
fi
