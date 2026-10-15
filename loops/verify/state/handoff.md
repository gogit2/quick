# Handoff - verify

## Last iteration
- M02 round 1: **PASS** with one small fix (F1: task dialog now shows server fieldErrors via `setErrors({server:true})`). No FIX tasks. Report: `loops/verify/outputs/M02-round-1.md`.

## Traps and commands that worked (headless permissions)
- No `$VAR`, no `&&`/`;` chains with redirection, no `cd x && cmd > file`. Run `cd backend` as its own call, then `./mvnw -q verify 2>&1 | tail -40`, then `cd` back to the project root. Read the results from `backend/target/surefire-reports/*.xml` (grep `tests=.*failures=`).
- `npx ng build` works the same way after its own `cd frontend` call.
- Bash commands with shell variables (e.g. `Y=$(date ...)`) are denied. Seed test data with `browser_evaluate` + `fetch('/api/...')` through the proxy instead (compute dates with `Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Cairo'})`).
- Playwright screenshots always land under `run-artifacts/playwright/` (use a `verify-Mxx-rN/` prefix in the filename). Copying them out is denied; reference them where they are.
- The curl suite leaves data and settings. For empty-state E2E run `scripts/backend.sh restart --port 8090 --db stable --fresh` **after** curl and **before** the browser.
- Backend-down error state: `scripts/backend.sh stop --port 8090`, then `scripts/backend.sh start --port 8090 --db stable` (keeps the db).
- Server-side validation from the UI: client validators block it, so in dev mode use `ng.getComponent(dialogHost)`, `clearValidators()` + `updateValueAndValidity()`, then click save.
- mat-select: `browser_select_option` fails; click the select, then the `mat-option` (or do it in `browser_evaluate`).

## Next milestone
- Smoke scenarios for later milestones: E2E-M01-01, E2E-M02-01.
- Open, non-blocking: no catch-all ApiError handler for 5xx (M01), Complete button visible on DONE tasks, GET /api/tasks 400 for bad enum not in the contract, bundle > 500 kB budget warning.
