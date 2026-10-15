# Handoff - verify

## Last iteration
- M04 round 1: **PASS**, one fix (F1: `resetForm()` instead of `reset()` in learning-card-details so no stale "required" error after a successful add). Report: `loops/verify/outputs/M04-round-1.md`, E2E detail `loops/verify/outputs/evidence/M04/round-1/e2e-result.md`.

## Recommended order (saves a restart)
1. `cd backend` as its own call, then `./mvnw -q verify 2>&1 | tail -40` with run_in_background, then `cd ..`.
2. Swagger export (read-only, fine on the fresh db).
3. Browser **first**, while the db is fresh: empty states, milestone scenarios, smoke scenarios.
4. Backend-down error state: `scripts/backend.sh stop --port 8090`, check the page, then `scripts/backend.sh restart --port 8090 --db stable --fresh` and run curl (AC-M01-01 needs `configured=false`).

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`, brace expansion, `cd x && git ...`, heredoc python edits. Use the Edit tool for fixes. Count tests with `grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' backend/target/surefire-reports/*.xml | awk ...`.
- Frontend build: `cd frontend` own call, `npx ng build 2>&1 | tail -8`, `cd ..`.
- Count long strings exactly: generate them in `browser_evaluate` with `'x'.repeat(201)` and set value + dispatch `input`. Typing by hand gave 198 chars once.
- Watch for stale mat-error after successful submits in other forms (Material shows errors when the form directive is submitted; `reset()` does not clear that, `resetForm()` does). Worth checking in M05 plan forms.
- Screenshots land under `run-artifacts/playwright/` (prefix `verify-Mxx-rN-`).
- Chromium logs expected 4xx and proxy 500s (backend stopped) as console errors; not app errors.

## Next milestone
- Smoke: E2E-M01-01, E2E-M02-01, E2E-M03-01, E2E-M04-01.
- Open, non-blocking: no catch-all ApiError handler for 5xx (M01), Complete button visible on DONE tasks, GET /api/tasks 400 for bad enum not in the contract, bundle > 500 kB budget warning.
