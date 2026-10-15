# Verify M03 - Habits · round 1

- **Verdict**: PASS
- **Time**: 2026-10-15T03:59:00+03:00
- **Trial**: 1 · model claude-opus-5-5
- **Stack**: backend :8090 (fresh `stable` db), frontend :4300 -> :8090

## Checks
| Check | Result | Evidence |
|---|---|---|
| Review (git diff HEAD + untracked) | pass | T031-T038 [BE] and T039-T044 [FE] all present in code (see Notes) |
| Swagger | pass | `evidence/M03/round-1/swagger-diff.md` (PASS) |
| Unit (`./mvnw -q verify`) | pass | 101 tests, 0 failures, 0 errors (surefire reports; HabitServiceTest 15, HabitControllerTest 16, StreakCalculatorTest 7, HabitPeriodsTest 5) |
| curl regression `--upto M03` | pass | `evidence/M03/round-1/curl.log`: M01 6/6, M02 14/14, M03 13/13 (on a fresh db, see Notes) |
| E2E M03 (real stack) | pass | `evidence/M03/round-1/e2e-result.md`: E2E-M03-01..07 + backend-down error state |
| Smoke earlier milestones | pass | E2E-M01-01, E2E-M02-01 (`e2e-result.md`) |

## Fixes applied
None.

## FIX tasks
None.

## Contract issues
None logged for M03.

## Notes
- Review: V3 migration matches T031 (unique (habit_id, completion_date), ON DELETE CASCADE). HabitPeriods/StreakCalculator are plain classes; HabitService uses Clock + UserTime (no `now()` without a clock). The DTO names match the contract. The controller has all 8 operations. Delete publishes SourceDeletedEvent(HABIT). Frontend: models/habits.api per contract; the completion toggle lives in a reusable HabitCompletionService (for M06); all acceptance test ids exist; the dialog shows server fieldErrors (same pattern as M02 F1).
- Assumptions A-backend-dev-M03-1..3 (weekly 409 message, inclusive from/to, completions kept on frequency change) are reasonable and accepted.
- First curl run failed only AC-M01-01 (`configured=false`). The browser run had already saved settings on first load. After `scripts/backend.sh restart --port 8090 --db stable --fresh` all suites passed. This is not a defect.
- The backend was stopped for the error-state check and restarted with `--db stable` (healthy).
- Still open from earlier milestones, non-blocking: no catch-all 5xx ApiError handler, bundle > 500 kB budget warning.
