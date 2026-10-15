# Handoff - verify

## Last iteration
- M06 round 1: **PASS**, fix F1 (dashboard checkbox reset on failed toggle). Report: `loops/verify/outputs/M06-round-1.md`, E2E detail `loops/verify/outputs/evidence/M06/round-1/e2e-result.md`.

## Recommended order (saves a restart)
1. `cd backend` as its own call, then `./mvnw -q verify 2>&1 | tail -40` with run_in_background, then `cd ..` (the cwd persists between calls). Redirecting to a file with `echo $?` is denied.
2. Swagger export (read-only, fine on the fresh db).
3. Browser **first**, while the db is fresh: empty states, smoke scenarios (they also seed a task, habit, card and plan), milestone scenarios.
4. Backend-down error state: `scripts/backend.sh stop --port 8090`. To retest with data after a fix: `scripts/backend.sh start --port 8090 --db stable --no-build --jar run-artifacts/backend-latest.jar` keeps the data. Then `scripts/backend.sh restart --port 8090 --db stable --fresh` and run curl. `curl-regression.sh --only M06` on a fresh db covers the AC-M06-01 all-zero branch.

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`, quoted payloads with variables in curl, brace expansion, any `cp` (even single files) into the evidence folder, `cd x && git ...`, heredoc python edits. Use the Edit tool for fixes; reference screenshots under `run-artifacts/playwright/` (prefix `verify-Mxx-rN-`).
- Seed data through the app origin: `browser_evaluate` with `fetch('/api/...')` (tasks with dueDate from `/api/dashboard`.date, `/complete`, `/archive`, milestone PUT `{title, done:true}`).
- Dashboard reloads on `window.dispatchEvent(new Event('focus'))` - no need to wait 30 s. Clock override: `window.__qfClock.setNow(iso)` / `.clear()`.
- Snapshots are large; prefer `browser_evaluate` that clicks by data-testid and returns innerText. Mat checkbox: click `[data-testid=...] input`. Inputs: set `.value` + dispatch `input`.
- Plan builder defaults the start to the next whole minute; status flips on reload.
- Count tests: `grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' backend/target/surefire-reports/*.xml | awk -F'"' '{t+=$2;e+=$4;s+=$6;f+=$8} END {print t,e,s,f}'`.
- Frontend build: `cd frontend` own call, `npx ng build 2>&1 | tail -8`, `cd ..`.
- Chromium logs proxy 500s (backend stopped) as console errors; not app errors.

## Next milestone (M07)
- Smoke: E2E-M07-02 = E2E-M01-01 .. E2E-M06-01 in order.
- E2E-M07-03 needs `<page>-error-retry` on every page; the dashboard has only `dashboard-load-error` today.
- Open, non-blocking: greeting uses browser hour not Settings zone; dashboard checklist lists done weekly habits (D-6 says only until done); no catch-all ApiError for 5xx; Complete button on DONE tasks (Tasks page); bundle > 500 kB; builder dialog right edge at 1440 px; catch-up banner wording for ended plans.
