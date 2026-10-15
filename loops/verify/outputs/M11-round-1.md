# Verify M11 - Polish and regression · round 1

- **Milestone**: M11 (spec `specs/002-visual-polish`, contract `specs/001-quickflow/contracts/openapi.yaml`)
- **Round**: 1 (trial 0 failed so far)
- **Verdict**: **PASS** (with 2 small fixes)
- **Time**: 2026-10-15T17:40:00+03:00
- **Stack**: backend :8090 (fresh `stable` db, `run-artifacts/backend-latest.jar`), frontend :4300 -> :8090

## Checks

| Check | Result | Evidence |
|---|---|---|
| Review (`git diff HEAD` + untracked; M11 code is in commit 5e41e68, reviewed as `git diff 8f8e52d..HEAD -- frontend`) | pass | this report, "Review" below |
| Swagger | pass | `evidence/M11/round-1/swagger-diff.md`, `openapi-exported.yaml` |
| Unit (`./mvnw -q verify`) | pass, 188 tests, 0 errors, 0 failures | `run-artifacts/verify-M11-r1-mvn.log` |
| Build (`npx ng build`) | pass, no warnings, 603.78 kB raw / 150.28 kB transfer (after fixes 603.87 / 150.30) | `evidence/M11/round-1/ng-build.txt`, `ng-build-after-fixes.txt` |
| curl regression `--upto M11` (fresh db) | pass, 7 suites, 0 failed (M08-M11 have no BE suite, D-19) | `evidence/M11/round-1/curl.log` |
| E2E M11 (E2E-M11-01..07, real stack) | pass | `evidence/M11/round-1/browser-results.json` |
| Real-stack-only (E2E-M10-08 empty, E2E-M07-03 backend down, E2E-M07-01 journey) | pass | `browser-results.json`, screenshots below |
| Smoke of earlier milestones | pass | `browser-results.json` |
| Static: colour grep | pass, only `_palette.scss` and `_tokens.scss` (re-run after fixes) | - |
| Static: lost test ids | pass, the only `-` line with a test id (`dashboard-completion-percent`) is re-added unchanged on the reformatted line | - |

Screenshots (MCP saves them in `run-artifacts/playwright/`): `verify-M11-r1-empty-plans-dark.png`, `verify-M11-r1-E2E-M07-03-dashboard-error.png`, `verify-M11-r1-E2E-M07-01-step12.png`, `verify-M11-r1-E2E-M07-01-step15.png`, `verify-M11-r1-drawer-390-light.png`, `verify-M11-r1-settings-dark-390.png`, `verify-M11-r1-focus-settings-dark.png`, `verify-M11-r1-smoke-dashboard-light-1440.png`, `verify-M11-r1-dashboard-light-fixed.png`.

## Review

- T131: `styles/_motion.scss` (transitions on allowed properties, reduced-motion mixin for the OS query and `html[data-reduce-motion='true']`), `reduceMotionRequested()` in theme.service.ts, `provideAnimationsAsync('noop')` in app.config.ts, `qf-ready` via `afterNextRender` + rAF. Present. The rail width transition was dead in the browser (see F1).
- T132/T133: `_base.scss` focus ring on inputs/selects/switch/radio/checkbox; `--qf-on-danger` token replaces the `#fff` fallback in confirm-dialog. Present.
- T134: budget 650 kB (D-25); `MatSnackBar` lazily imported in `error.interceptor.ts` (checked live: 5xx snackbar still shows with the backend down).
- T135: dashboard `&ngsp;` spacing. T138: `frontend/README.md` Design system section, `e2e/M11.md`.
- D-26: the four mock-limited scenarios are marked `accepted_known_failure`; the M11 changes touch no logic of those flows.
- Constitution: no contract change, no colour literal outside the token files, no test id renamed, time logic untouched.

## Fixes applied

| # | File | What | Why | Lines |
|---|---|---|---|---|
| F1 | `frontend/src/app/layout/shell.component.scss` | `.sidenav` transition moved into `&.mat-drawer { transition: width + transform var(--qf-motion-base) var(--qf-ease) }` | Material's `.mat-drawer-transition .mat-drawer { transition: transform 400ms }` won the cascade (same specificity, later), so the rail collapse did not animate at all and the mobile drawer slid in 400 ms, against T131 / SC-V08 (< 300 ms, tokens). After: computed `width 0.2s, transform 0.2s`; collapse 240 -> 72 px in ~207 ms; with `?reduceMotion=1` 0s | +8 / -1 |
| F2 | `frontend/src/app/features/dashboard/dashboard-page.component.html` | Milestone date span in "Completed in the last 7 days" gets the `actions` slot attribute (same pattern as the habit streak row) | The date sat inline in the row content and Angular stripped the whitespace, so it rendered as "Chapter B 88115Oct 15". After: date right-aligned in the actions slot, inside the row at 390 px, no h-scroll | 1 |

Re-run after the fixes: `ng build` (no warnings), colour grep, E2E-M11-01 (motion), E2E-M11-06 dashboard at 390 px, console clean.

## FIX tasks

None.

## Contract issues

None for M11 (spec 002 is frontend-only, D-19).

## Notes

- E2E-M07-01 step 12: the banner and highlight appeared 6.6 s after the start, but the card status changed to "In progress" (and the rest time appeared) only with the page's 30 s refresh, ~26 s after the start. This comes from spec 001 (unchanged in M11) and is within the scenario's refresh allowance; noted for the backlog.
- Task status chip reads "Todo" (scenario text says "To do"); spec 001 wording, unchanged since M02.
- Light theme active nav item is still the saturated `--qf-accent-soft` cyan (backlog from M10).
- Not measured: Inter text shift on load (no layout-shift API in the MCP); relies on the M08 fallback metrics.
- Stack left running (backend :8090 fresh db after curl, frontend :4300) for the runner to stop.
