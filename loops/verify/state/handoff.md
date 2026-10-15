# Handoff - verify

## Last iteration
- M01 round 1: **PASS**, no fixes, no FIX tasks. Report: `loops/verify/outputs/M01-round-1.md`.

## Traps and commands that worked (headless permissions)
- No `$VAR`, no `&&`/`;` chains with redirection, no `cd x && cmd > file`. Run `cd backend` as its own call, then `./mvnw -q verify 2>&1 | tail -40`, then `cd` back to the project root. Read the results from `backend/target/surefire-reports/*.xml` (grep `tests=.*failures=`).
- `npx ng build` works the same way after its own `cd frontend` call.
- Playwright screenshots always land under `run-artifacts/playwright/` (a filename with a path is nested below it). Moving or copying them out of `run-artifacts/` is denied, so reference them where they are.
- The curl suite leaves configured settings (start page TASKS). For first-run/empty-state E2E, run `scripts/backend.sh restart --port 8090 --db stable --fresh` **after** curl and **before** the browser.
- Backend-down error state: `scripts/backend.sh stop --port 8090`, then `scripts/backend.sh start --port 8090 --db stable` (keeps the db).
- Browser zone is Africa/Cairo (first-run PUT stores it).

## Next milestone
- E2E-M01-01 is the [smoke] scenario for later milestones. Open items to watch (non-blocking, from the M01 report): no catch-all ApiError handler for 5xx, `ZoneId.of` accepts offset ids, `~/.m2` outside the project (A-verify-M01-1).
