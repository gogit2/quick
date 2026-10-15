#!/usr/bin/env bash
# Guard: no data-testid of the baseline may disappear from the frontend (new ones are fine).
#   testid-check.sh              check against specs/*/testids.baseline.txt (the newest one)
#   testid-check.sh --write      (re)generate the baseline from the current code (planning step only)
# A "test id" is every kebab-case token found on a line that mentions `testid`/`testId` in frontend/src/app
# (static ids and the literal prefix of dynamic ones such as 'tasks-row-').
source "$(dirname "$0")/lib.sh"
extract() {
  grep -rIiE 'testid' frontend/src/app --include='*.ts' --include='*.html' \
    | grep -oE "[a-z]+(-[a-z0-9]+)+-?" | grep -E '^(nav|dashboard|tasks|habits|learning|plans|settings|confirm|theme|user|menu|app)-' \
    | sort -u
}
BASE="$(ls -t specs/*/testids.baseline.txt 2>/dev/null | head -1 || true)"
if [[ "${1:-}" == "--write" ]]; then
  OUT="${2:-specs/002-visual-polish/testids.baseline.txt}"
  extract > "$OUT"; echo "wrote $OUT ($(wc -l < "$OUT") ids)"; exit 0
fi
[[ -n "$BASE" ]] || { echo "testid-check: no baseline, skipped"; exit 0; }
CUR="$(mktemp)"; extract > "$CUR"
MISSING="$(comm -23 "$BASE" "$CUR" || true)"
if [[ -n "$MISSING" ]]; then
  echo "testid-check: FAIL - these data-testid values of the baseline ($BASE) are gone from frontend/src/app:"; echo "$MISSING"; exit 1
fi
echo "testid-check: PASS ($(wc -l < "$BASE") baseline ids all present, $(comm -13 "$BASE" "$CUR" | wc -l) new)"
