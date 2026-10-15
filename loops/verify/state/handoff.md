# Handoff - verify

## Last iteration
- M07 round 1: **PASS**, fix F1 (focus ring on Material buttons, `frontend/src/styles.scss`). Report: `loops/verify/outputs/M07-round-1.md`, browser detail `loops/verify/outputs/evidence/M07/round-1/e2e-result.md`.
- M07 is the last milestone of `specs/001-quickflow`. The stack is left running (backend :8090 fresh db after curl, frontend :4300) for the runner to stop.

## Recommended order (saves restarts)
1. `cd backend` as its own call, `./mvnw -q verify 2>&1 | tail -40` in the background, then `cd ..`.
2. Swagger export (read-only).
3. Browser first while the db is fresh: empty states, smoke, then the milestone scenarios. Plans created with the builder default start (next whole minute) start within 60 s; `browser_wait_for` the text "Plan <title> has started".
4. Backend down: `scripts/backend.sh stop --port 8090`, navigate pages with nav clicks (no reload needed), then `scripts/backend.sh start --port 8090 --db stable --no-build --jar run-artifacts/backend-latest.jar` keeps the data and lets you test retry.
5. `scripts/backend.sh restart --port 8090 --db stable --fresh`, then `scripts/curl-regression.sh --upto Mxx`.

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`, brace expansion, `cp` into the evidence folder, `cd x && git ...`, heredoc python edits. Use the Edit tool; screenshots stay in `run-artifacts/playwright/` (prefix `verify-Mxx-rN-`).
- `browser_evaluate` driving by data-testid (click, set `.value` + dispatch `input`, mat-checkbox: click its `input`) is fast; snapshots are large.
- Focus checks: `el.focus()` from evaluate does not set keyboard modality; press Tab with `browser_press_key` first, then read `getComputedStyle(document.activeElement).outline`.
- Chromium logs proxy 500s as console errors while the backend is stopped; those are not app errors.
- Count tests: `grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' backend/target/surefire-reports/*.xml | awk -F'"' '{t+=$2;e+=$4;s+=$6;f+=$8} END {print t,e,s,f}'`.

## Open, non-blocking (backlog)
- Settings: form stays editable with defaults and Save enabled when loading fails (could overwrite the stored settings).
- Greeting uses the browser hour, not the Settings zone; the dashboard checklist lists done weekly habits; the Complete button shows on DONE tasks; the builder dialog's right edge at 1440 px; new plan items start unticked even if the source is already done (BR13-consistent).
