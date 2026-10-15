# Handoff - verify

## Last iteration
- M08 round 1: **PASS**, fix F1 (`--qf-shadow-1/-2` in `frontend/src/styles/_tokens.scss`: `light-dark()` only takes colours). Report: `loops/verify/outputs/M08-round-1.md`, browser detail `loops/verify/outputs/evidence/M08/round-1/e2e-result.md`.
- Stack left running (backend :8090 fresh db after curl, frontend :4300) for the runner to stop.

## Recommended order (saves restarts)
1. `cd backend` as its own call, then `./mvnw -q verify > ../run-artifacts/verify-Mxx-rN-mvn.log 2>&1` with run_in_background (no `$?`, no `;echo`), then `cd ..` as its own call. Count tests with the grep below.
2. Swagger export (read-only). `cd frontend` + `npx ng build` as separate calls; then `cd` back to the root (the cwd persists).
3. Browser first while the db is fresh: empty states, smoke, then milestone scenarios. Plans created with the builder default start (next whole minute) start within 60 s; poll in `browser_evaluate` for "Plan <title> has started"; status flips to In progress on the next 30 s reload.
4. Backend down (M10-08 etc.): `scripts/backend.sh stop --port 8090`, then `scripts/backend.sh start --port 8090 --db stable --no-build --jar run-artifacts/backend-latest.jar`.
5. `scripts/backend.sh restart --port 8090 --db stable --fresh`, then `scripts/curl-regression.sh --base http://localhost:8090 --upto Mxx --log ...`.

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`, `echo EXIT=$?`, `git -C`, brace/escaped-brace regex in grep, `cp` into evidence, heredoc python edits. Screenshots stay in `run-artifacts/playwright/` (prefix `verify-Mxx-rN-`).
- Driving dialogs from `browser_evaluate`: wait ~800 ms after the dialog input appears before setting `.value` + `input` event, else the form ignores it. mat-checkbox: click its inner `input`.
- Contrast: paste the body of `loops/frontend-dev/outputs/e2e/contrast-check.js` as `window.__qfContrast = () => {...}` once, then use nav clicks (SPA) and `theme-option-*` clicks so it survives; a `browser_navigate` drops it.
- Count tests: `grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' backend/target/surefire-reports/*.xml | awk -F'"' '{t+=$2;e+=$4;s+=$6;f+=$8} END {print t,e,s,f}'`.
- Someone else may use the :8090 stack (display name changed to "Ahmed AbdelDaiem" mid-run); check numbers as deltas.

## Open, non-blocking (backlog)
- Spec 002: index.html `html[data-theme]` neutral background out-specifies `--qf-bg` on `html`; light active nav = saturated primary-container cyan; `mat-toolbar color="primary"` no-op in M3; settings.svg/light_mode.svg > 0.5 kB; E2E-M08-04 dark-OS branch only by review.
- Spec 001: Settings form editable with defaults when loading fails; greeting uses browser hour; dashboard checklist lists done weekly habits; Complete button on DONE tasks; builder dialog right edge at 1440 px; new plan items start unticked even if source is done.
