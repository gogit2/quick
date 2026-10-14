#!/usr/bin/env bash
# Run the curl suites in order against a running backend.
#   curl-regression.sh --base http://localhost:8091 (--upto Mxx | --only Mxx) [--log file]
# Exit code 0 only if every suite passes.
source "$(dirname "$0")/lib.sh"
BASE_URL="http://localhost:8091" UPTO="" ONLY="" LOGF=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base) BASE_URL="$2"; shift 2;;
    --upto) UPTO="$2"; shift 2;;
    --only) ONLY="$2"; shift 2;;
    --log) LOGF="$2"; shift 2;;
    -h|--help) sed -n '2,4p' "$QF_SELF"; exit 0;;
    *) die "unknown option $1";;
  esac
done
SUITE_DIR="loops/backend-dev/outputs/curl"
mapfile -t suites < <(ls "$SUITE_DIR"/M[0-9][0-9]*.sh 2>/dev/null | sort)
[[ ${#suites[@]} -gt 0 ]] || die "no curl suites in $SUITE_DIR"
[[ -n "$LOGF" ]] && mkdir -p "$(dirname "$LOGF")" && : > "$LOGF"
fail=0 ran=0
for s in "${suites[@]}"; do
  ms="$(basename "$s" .sh)"; ms="${ms:0:3}"
  if [[ -n "$ONLY" && "$ms" != "$ONLY" ]]; then continue; fi
  if [[ -n "$UPTO" && "$ms" > "$UPTO" ]]; then continue; fi
  ran=$((ran + 1))
  log "suite $s against $BASE_URL"
  set +e
  if [[ -n "$LOGF" ]]; then
    BASE="$BASE_URL" bash "$s" >>"$LOGF" 2>&1; st=$?
    grep -E '^(  FAIL|=== SUITE)' "$LOGF" | tail -n 20 >&2
  else
    BASE="$BASE_URL" bash "$s"; st=$?
  fi
  set -e
  [[ "$st" == "0" ]] || { fail=$((fail + 1)); log "suite $ms FAILED"; }
done
[[ $ran -gt 0 ]] || die "no suite matched (upto=$UPTO only=$ONLY)"
log "curl regression: $ran suites, $fail failed"
[[ $fail == 0 ]]
