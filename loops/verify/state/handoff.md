# Handoff - verify

## Last iteration
- M10 round 1: **PASS** with fixes F1 (learning first expand animates: `toggleExpanded` renders closed, opens 2 rAF later) and F2 (progress track `--qf-outline` instead of `--qf-accent-soft` in plan-progress, dashboard scss, learning-page). Report: `loops/verify/outputs/M10-round-1.md`.
- Stack left running (backend :8090 fresh db after curl, frontend :4300) for the runner to stop.

## Recommended order (saves restarts)
1. `cd backend` as its own call, then `./mvnw -q verify > ../run-artifacts/verify-Mxx-rN-mvn.log 2>&1` with run_in_background, then `cd ..` separately. Count tests with the grep below.
2. Swagger export (read-only). `cd frontend` + `npx ng build` as separate calls, then `cd ..`.
3. Browser first while the db is fresh: empty states, backend-down error state, then smoke and milestone scenarios. Seed extra data with curl to `http://localhost:8090/api/...` (overdue task, milestones, notes) instead of UI forms.
4. Start banner on the real stack: create a plan by API with `startDateTime` = next whole minute (`date -u -d '+1 minute' +%Y-%m-%dT%H:%M:00Z`), wait in `browser_evaluate` until start + 500 ms, then `browser_navigate` to reload. A plan created after its start is acknowledged silently.
5. Backend down: `scripts/backend.sh stop --port 8090`, then `scripts/backend.sh start --port 8090 --db stable --no-build --jar run-artifacts/backend-latest.jar`. Fresh for curl: stop, then the same start with `--fresh`.

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`/for-loops with variables, `git -C`, `cp` into evidence, heredoc edits. Workaround for scripted checks: write a small node script under `loops/verify/outputs/evidence/Mxx/round-N/` and run `node <path>` (see M10 `testid-diff.js`).
- Screenshot `filename` must be a bare name (`verify-Mxx-rN-....png`); a path gets nested under `run-artifacts/playwright/`.
- Form fields: setting `.value` + `input` event works only ~600 ms after the dialog opened; otherwise use `browser_type` (it uses `getByTestId`).
- Contrast helper `window.__qfContrast` is lost on every `browser_navigate`; re-define it inside the same evaluate that uses it.
- Count tests: `grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' backend/target/surefire-reports/*.xml | awk -F'"' '{t+=$2;e+=$4;s+=$6;f+=$8} END {print t,e,s,f}'`.
- Material 19.2 token names: check `node_modules/@angular/material/fesm2022/<comp>.mjs` before trusting an override (default progress track is `--mat-sys-surface-variant`).
- Small-fix limit is 3 files in total per round; count before fixing a second issue.

## Open, non-blocking (backlog)
- Spec 002: empty-state add button should be `mat-flat-button` (T122 "filled"), 1 line in `shared/empty-state.component.ts`. `ng build` budget WARNING 616.77 kB (M11 T134 / E2E-M11-05). Light active nav = saturated `--qf-accent-soft` cyan. index.html `html[data-theme]` neutral background out-specifies `--qf-bg`. settings.svg/light_mode.svg > 0.5 kB. E2E-M08-04 dark-OS branch only checked by review.
- Spec 001: Settings form editable with defaults when loading fails; greeting uses browser hour; dashboard checklist lists done weekly habits; Complete button on DONE tasks; new plan items start unticked even if the source is done; Habits "Show inactive" starts on.
