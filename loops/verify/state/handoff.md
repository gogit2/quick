# Handoff - verify

## Last iteration
- M03 round 1: **PASS**, no fixes, no FIX tasks. Report: `loops/verify/outputs/M03-round-1.md`, E2E detail `loops/verify/outputs/evidence/M03/round-1/e2e-result.md`.

## Recommended order (saves a restart)
1. Start `./mvnw -q verify` in the background (own `cd backend` call first), then cd back.
2. Swagger export (read-only, fine on the fresh db).
3. Browser **first**, while the db is still fresh: empty states, then the milestone scenarios, then the smoke scenarios.
4. Then `scripts/backend.sh restart --port 8090 --db stable --fresh` and run curl. AC-M01-01 needs `configured=false`, and the frontend PUTs /api/settings on first load.
5. Backend-down error state last: `scripts/backend.sh stop --port 8090`, then `scripts/backend.sh start --port 8090 --db stable`.

## Traps and commands that worked (headless permissions)
- No `$VAR`, no brace expansion (`{a,b}.java` is denied), no `&&`/`;` chains with redirection. Run `cd backend` as its own call, then `./mvnw -q verify 2>&1 | tail -40`. Read the counts with `grep -ho 'tests="[0-9]*" errors=... failures="[0-9]*"' backend/target/surefire-reports/*.xml`.
- Seed or poke data with `browser_evaluate` + `fetch('/api/...')` through the proxy. This is how you make a real 409 (complete behind the UI, then click the stale button).
- Count string lengths for limit tests exactly (e.g. 15 x "abcdefghij" + "k" = 151), then confirm the length in the page.
- Playwright screenshots always land under `run-artifacts/playwright/` (prefix `verify-Mxx-rN-`); reference them there.
- Chromium logs each expected 4xx as a console `[ERROR] Failed to load resource`. That is not an app error; say so in the report.
- mat-select: click the select, then the option.

## Next milestone
- Smoke scenarios: E2E-M01-01, E2E-M02-01, E2E-M03-01.
- Open, non-blocking: no catch-all ApiError handler for 5xx (M01), Complete button visible on DONE tasks, GET /api/tasks 400 for bad enum not in the contract, bundle > 500 kB budget warning.
