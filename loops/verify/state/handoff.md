# Handoff - verify

## Last iteration
- M09 round 1: **PASS**, fix F1 (snackbar tokens in `frontend/src/styles/_material.scss`: Material 19.2.19 reads `--mdc-snackbar-container-shape/-color/-supporting-text-color`, not `--mat-snack-bar-*`). Report: `loops/verify/outputs/M09-round-1.md`, browser detail `loops/verify/outputs/evidence/M09/round-1/e2e-result.md`.
- Stack left running (backend :8090 fresh db after curl, frontend :4300) for the runner to stop.

## Recommended order (saves restarts)
1. `cd backend` as its own call, then `./mvnw -q verify > ../run-artifacts/verify-Mxx-rN-mvn.log 2>&1` with run_in_background (redirect is denied if combined with `cd` in one call), then `cd ..` separately. Count tests with the grep below.
2. Swagger export (read-only). `cd frontend` + `npx ng build` as separate calls, then `cd ..`. Do not run other commands in parallel with a `cd`; they may run in the old cwd.
3. Browser first while the db is fresh: empty states, smoke, then milestone scenarios. Drive pages with `browser_evaluate` (SPA nav clicks, set `.value` + `input` event, click save) to keep the snapshots small.
4. Start banner: the :8090 stack can be shared with another client that acknowledges plans first. To be sure: create a plan (builder default start = next whole minute), wait in `browser_evaluate` until `Date.parse(start)+300`, then `browser_navigate` to reload. The first sync then acknowledges it and shows the banner.
5. Snackbar on demand: `ng.getComponent(document.querySelector('main router-outlet').nextElementSibling).snackBar.open('msg','Dismiss',{duration:120000})` (pages that inject MatSnackBar: dashboard, plans, tasks, habits, learning).
6. A change to global styles makes the dev server do a full page reload, so banner state and `window.__qfContrast` are lost.
7. Backend down: `scripts/backend.sh stop --port 8090`, then `scripts/backend.sh start --port 8090 --db stable --no-build --jar run-artifacts/backend-latest.jar`.
8. `scripts/backend.sh restart --port 8090 --db stable --fresh`, then `scripts/curl-regression.sh --base http://localhost:8090 --upto Mxx --log ...`.

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`, `echo EXIT=$?`, `git -C`, brace/escaped-brace regex in grep, `cp` into evidence, heredoc python edits. Screenshots stay in `run-artifacts/playwright/` (prefix `verify-Mxx-rN-`).
- Contrast: paste the body of `loops/frontend-dev/outputs/e2e/contrast-check.js` as `window.__qfContrast = () => {...}` once; it survives SPA nav and `theme-option-*` clicks but not `browser_navigate`.
- Count tests: `grep -ho 'tests="[0-9]*" errors="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' backend/target/surefire-reports/*.xml | awk -F'"' '{t+=$2;e+=$4;s+=$6;f+=$8} END {print t,e,s,f}'`.
- Material 19.2 token names: check `node_modules/@angular/material/fesm2022/<comp>.mjs` for the `--mdc-*`/`--mat-*` name before trusting an override.

## Open, non-blocking (backlog)
- Spec 002: `ng build` budget WARNING since M09 (615 kB > 600 kB; transfer 150.7 kB). M11 T134 / E2E-M11-05 must remove it. Light active nav = saturated primary-container cyan. index.html `html[data-theme]` neutral background out-specifies `--qf-bg`. `mat-toolbar color` gone (OK). settings.svg/light_mode.svg > 0.5 kB. E2E-M08-04 dark-OS branch only checked by review.
- Spec 001: Settings form editable with defaults when loading fails; greeting uses browser hour; dashboard checklist lists done weekly habits; Complete button on DONE tasks; builder dialog right edge at 1440 px; new plan items start unticked even if source is done.
