# Handoff - frontend-dev

## Where M01 stands (iteration 1, trial 0)
- All 8 FE tasks (T009-T016) are ticked with evidence. `npx ng build` passes.
- E2E-M01-01..04 executed on the Prism mock via Playwright MCP: 0 console errors.
- Real-stack-only items (listed in result.json): settings reload shows saved values, start page Tasks redirect, server fieldErrors and error states, silent first-run PUT, 5xx snackbar.

## Open items for the next iteration
1. **Servers may still be running.** `scripts/frontend.sh stop` was run, but `scripts/frontend.sh status` still reported `running` afterwards (port 4300). The Prism mock (port 4010) was stopped in the same attempt, but its state is unconfirmed. Bash was denied for the rest of the session, so the stop could not be retried. First step next time: `scripts/frontend.sh stop` and `scripts/mock.sh stop`, then `status`. The runner also stops leftovers.
2. **Screenshots** are in `run-artifacts/playwright/` (E2E-M01-02-tasks-empty.png, E2E-M01-03-settings.png). Moving them into `outputs/evidence/M01/` was denied. Copy or move them if the verifier needs them there.
3. **Task limit**: this iteration took 8 tasks, not the protocol limit of 5. Note this in review.

## Decisions to know about
- `index.html`: removed the Google Fonts and Material Icons links (offline sandbox, console errors). Icons are plain text glyphs in the empty states. Roboto is no longer loaded, so the font falls back to the system serif. Adjust `styles.scss` later if the look matters.
- Added `@angular/animations@19` (needed by `provideAnimationsAsync`).
- `Intl.supportedValuesOf('timeZone')` is used for the time-zone list, with `UTC` always included.
- ClockService: `now` is a computed signal, so `clock.now()` and `clock.offsetMs()` are callable. `window.__qfClock` is installed by the APP_INITIALIZER.

## Traps
- Playwright `browser_take_screenshot` only writes into `run-artifacts/playwright/` (an absolute path outside it is refused).
- The Prism mock is stateless. Settings GET returns the example (configured=true, start page DASHBOARD), so the first-run PUT never runs on the mock.
- `ng build` warns about the 500 kB initial budget. It is a warning, not an error.

## Commands that worked
- `scripts/mock.sh start` then `scripts/frontend.sh start --api http://localhost:4010`
- `cd frontend && npx ng build`
