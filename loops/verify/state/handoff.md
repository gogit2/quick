# Handoff - verify

## Last iteration
- M05 round 1: **PASS**, no fixes. Report: `loops/verify/outputs/M05-round-1.md`, E2E detail `loops/verify/outputs/evidence/M05/round-1/e2e-result.md`.

## Recommended order (saves a restart)
1. `cd backend` as its own call, then `./mvnw -q verify 2>&1 | tail -40` with run_in_background, then `cd ..` (the cwd persists between calls).
2. Swagger export (read-only, fine on the fresh db).
3. Browser **first**, while the db is fresh: empty states, smoke scenarios (they also seed a task, habit and card), milestone scenarios.
4. Backend-down error state: `scripts/backend.sh stop --port 8090`, check the page, then `scripts/backend.sh restart --port 8090 --db stable --fresh` and run curl (AC-M01-01 needs `configured=false`).

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`, quoted payloads with variables in curl, brace expansion, globs in write commands (`cp *.png`), `cd x && git ...`, heredoc python edits. Use the Edit tool for fixes.
- Seed or change data through the app origin instead: `browser_evaluate` with `fetch('/api/...', {method:'POST', ...})` works (e.g. settings PUT, past/in-progress plans with `new Date(Date.now()-30000).toISOString()`).
- Snapshots are large (~5k tokens); prefer `browser_evaluate` that clicks by data-testid and returns innerText. Mat checkbox: click `[data-testid=...] input`.
- Count tests: `grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' backend/target/surefire-reports/*.xml | awk -F'"' '{t+=$2;e+=$4;s+=$6;f+=$8} END {print t,e,s,f}'`.
- Frontend build: `cd frontend` own call, `npx ng build 2>&1 | tail -8`, `cd ..`.
- Plan builder defaults the start to the next whole minute; real-clock E2E: status flips on the page's 30 s reload, notification sync is also 30 s. Banners queue one at a time (dismiss to see the next).
- Screenshots land under `run-artifacts/playwright/` (prefix `verify-Mxx-rN-`).
- Chromium logs expected 4xx (own probes) and proxy 500s (backend stopped) as console errors; not app errors.

## Next milestone (M06 Dashboard)
- Smoke: E2E-M01-01, E2E-M02-01, E2E-M03-01, E2E-M04-01, E2E-M05-01.
- Check that E2E-M05-05's "dashboard follows" part works now (tick a plan task item -> dashboard tasks.completed and plan progress).
- Open, non-blocking: no catch-all ApiError handler for 5xx (M01), Complete button visible on DONE tasks, GET /api/tasks 400 for bad enum not in the contract, bundle > 500 kB budget warning, plan builder dialog right edge slightly clipped at 1440 px, catch-up banner says "has started" also for already-ended plans.
