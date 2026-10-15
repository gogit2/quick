# Verify report - M06 Dashboard, round 1

- **Verdict**: PASS
- **Time**: 2026-10-15T10:52:00+03:00
- **Trial**: 0 · Model: claude-opus-5-5
- **Stack**: backend :8090 (fresh stable db), frontend :4300 -> :8090

## Checks

| Check | Result | Evidence |
|---|---|---|
| Review (git diff HEAD + untracked) | pass | T076-T088 all present: 5 DTO records named as the contract; DashboardService composes TaskService/HabitService/PlanService/LearningService (no duplicated rules), DashboardRules (completedOn, recentMilestones) plain and unit-tested; Clock via UserTime; thin controller; FE types, DashboardApi (relative URL), page with all mandatory test ids, shared HabitCompletionService, restSecondsAt + ClockService, 30 s + focus reload, quick-add reuses the existing dialogs |
| Swagger | pass | `evidence/M06/round-1/swagger-diff.md` (PASS) |
| Unit (`./mvnw -q verify`) | pass | 165 tests, 0 failures, 0 errors (incl. DashboardConsistencyTest 2, DashboardRulesTest 4) |
| curl | pass | `evidence/M06/round-1/curl.log`: 6 suites M01..M06, 0 failed (M06 7/7); `curl-M06-only-fresh.log`: M06 alone on a fresh db 7/7 incl. the AC-M06-01 all-zero branch |
| E2E (M06 scenarios + real-stack-only) | pass | `evidence/M06/round-1/e2e-result.md`; screenshots `run-artifacts/playwright/verify-M06-r1-*.png` |
| Smoke (E2E-M01-01, M02-01, M03-01, M04-01, M05-01) | pass | `evidence/M06/round-1/e2e-result.md` |
| ng build | pass | budget warning only (553 kB > 500 kB, known) |
| Contract issues | none for M06 | - |

## Fixes applied

- **F1** `frontend/src/app/features/dashboard/dashboard-page.component.ts` + `.html` (~8 lines): a failed habit or plan item toggle left the mat-checkbox checked although the data said not done (the reload sets the same bound value, so Angular does not reset the DOM). The handlers now receive the `MatCheckboxChange` and reset `event.source.checked` to the stored value on error. Why: UI showed a habit as done while "Done today 0 of 2". Re-tested with the backend stopped: both checkboxes stay unchecked; happy-path toggles re-run OK; `ng build` OK.

## FIX tasks

None.

## Contract issues

None logged for M06.

## Notes (non-blocking)

- The greeting uses the browser's local hour (ClockService), not the Settings time zone; fine for a single-user app where both normally match.
- D-6 says a weekly habit is listed under today's habits only until it is done; the dashboard checklist lists all active habits and shows done ones as checked. Consider aligning in M07.
- When the backend is down the dashboard keeps the last numbers below `dashboard-load-error`; there is no `dashboard-error-retry` yet (E2E-M07-03 is M07).
- Still open from earlier rounds: no catch-all ApiError for 5xx, Complete button on DONE tasks (Tasks page), bundle budget warning, builder dialog right edge at 1440 px, catch-up banner wording for ended plans.
