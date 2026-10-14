#!/usr/bin/env bash
# Helpers for curl suites (loops/backend-dev/outputs/curl/Mxx.sh). Source it.
#   BASE (env) defaults to http://localhost:8091
#   case_begin "AC-M02-01 description" ; req METHOD PATH [JSON_BODY] ; expect_status 201
#   expect_json '<jq boolean expression>' ; VAR=$(json '<jq path>') ; case_end ; suite_end
set -uo pipefail
BASE="${BASE:-http://localhost:8091}"
_QF_PASS=0 _QF_FAIL=0 _QF_CASE="" _QF_CASE_OK=1
_QF_BODY_FILE="$(mktemp)"; _QF_STATUS=0
trap 'rm -f "$_QF_BODY_FILE"' EXIT

case_begin() { _QF_CASE="$1"; _QF_CASE_OK=1; echo "--- CASE $_QF_CASE"; }

# req METHOD PATH [BODY]
req() {
  local method="$1" path="$2" body="${3:-}"
  local args=(-sS -o "$_QF_BODY_FILE" -w '%{http_code}' -X "$method" -H 'Accept: application/json')
  [[ -n "$body" ]] && args+=(-H 'Content-Type: application/json' --data "$body")
  echo "> curl -X $method $BASE$path ${body:+-d '$body'}"
  : > "$_QF_BODY_FILE"
  _QF_STATUS="$(curl "${args[@]}" "$BASE$path" 2>/dev/null)" || true
  _QF_STATUS="${_QF_STATUS:-000}"
  echo "< $_QF_STATUS $(head -c 600 "$_QF_BODY_FILE")"
}

_fail() { echo "  FAIL: $*"; _QF_CASE_OK=0; }
expect_status() { [[ "$_QF_STATUS" == "$1" ]] || _fail "expected status $1, got $_QF_STATUS"; }
expect_json() { jq -e "$1" "$_QF_BODY_FILE" >/dev/null 2>&1 || _fail "jq check failed: $1"; }
json() { jq -r "$1" "$_QF_BODY_FILE"; }
body() { cat "$_QF_BODY_FILE"; }

case_end() {
  if [[ "$_QF_CASE_OK" == "1" ]]; then _QF_PASS=$((_QF_PASS + 1)); echo "  PASS: $_QF_CASE"
  else _QF_FAIL=$((_QF_FAIL + 1)); echo "  FAILED: $_QF_CASE"; fi
}

suite_end() {
  echo "=== SUITE $(basename "$0"): $_QF_PASS passed, $_QF_FAIL failed"
  [[ "$_QF_FAIL" == "0" ]]
}
