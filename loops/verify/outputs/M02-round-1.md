# Verify report - M02 Tasks, round 1

- **Milestone**: M02 - Tasks (`specs/001-quickflow`)
- **Round**: 1 (trial 0 failed so far)
- **Verdict**: **PASS** (after 1 small verifier fix)
- **Time**: 2026-10-15T03:46+03:00
- **Stack**: backend http://localhost:8090 (stable db), frontend http://localhost:4300 proxying to it

## Checks

| # | Check | Result | Evidence |
|---|---|---|---|
| 1 | Review (`git diff HEAD` + untracked backend/, frontend/, loop files) | pass | this report, "Review notes" |
| 2 | Swagger export vs contract (10 operations up to M02) | pass | `evidence/M02/round-1/swagger-diff.md`, `openapi-exported.yaml` |
| 3 | `./mvnw -q verify` (58 tests, 0 failures) + `npx ng build` | pass | `evidence/M02/round-1/unit-and-build.md` |
| 4 | curl regression `--upto M02` (M01 6/6, M02 14/14) | pass | `evidence/M02/round-1/curl.log` |
| 5 | Playwright MCP, real stack: E2E-M02-01..07 + real-stack-only E2E-M02-08 (empty state on fresh db, no-results, backend-down error state) | pass (E2E-M02-02 after fix F1) | `evidence/M02/round-1/e2e-result.json`, screenshots in `run-artifacts/playwright/verify-M02-r1/` |
| 5b | Smoke of earlier milestones (E2E-M01-01) | pass | `evidence/M02/round-1/e2e-result.json` |
| 6 | Contract issues of M02 | none open | `loops/_shared/contract-issues.md` |

Evidence paths are relative to `loops/verify/outputs/`. The backend was restarted `--fresh` after curl and before the browser run.

## Review notes

Every ticked task has its code:
- BE (T017-T024): `V2__tasks.sql` (plain SQL, indexes on status/due_date/archived); `task/` with TaskEntity, TaskRepository (JpaSpecificationExecutor), enums, DTOs `Task`/`TaskCreate`/`TaskUpdate` named as in the contract with BR1/BR2 limits; `TaskMapper.isOverdue` (due < today in user zone, not DONE, not archived); `TaskService` uses the injected Clock and `UserTime` (no clock-less `now()`), publishes `SourceDeletedEvent` on delete, internal `reopen`; `TaskQuery` specification for q/status/priority/due/dueFrom/dueTo/archived and the four sorts (nulls last); thin `TaskController` with 8 operations. Tests: TaskServiceTest 9, TaskControllerTest 11, TaskQueryTest 13.
- BE shared changes: `GlobalExceptionHandler` maps `MethodArgumentTypeMismatchException` (bad enum in a query param) to 400 "Validation failed" (A-backend-dev-M02-3); `hibernate.jdbc.time_zone=UTC` so `Instant` columns are stored zone-independently. Both fine.
- FE (T025-T030): models mirror the contract; `tasks.api.ts` uses relative `/api/tasks` URLs and drops unset params; Tasks page with all mandated test ids (`tasks-add-button`, rows/status/priority/overdue, `tasks-loading`, `tasks-no-results`, toolbar ids, row action ids, `confirm-ok`), empty state via the shared component (`tasks-empty-state`), load-error notice that does not claim "no tasks"; dialog with `tasks-form-*` ids, client validation, server fieldErrors.
- Assumptions A-backend-dev-M02-1..3 are reasonable and consistent with DECISIONS/contract.

Non-blocking observations (no FIX task):
- The "Complete" button stays visible on a DONE task (re-completing is a no-op 200). Cosmetic.
- `GET /api/tasks` can now return 400 for a bad enum query param, which the contract does not list for that operation; swagger diff does not flag it. Harmless; could be added to the contract later.
- Initial bundle 534.88 kB > 500 kB budget (warning only).

## Fixes applied

| Id | File | What | Why | Lines |
|---|---|---|---|---|
| F1 | `frontend/src/app/features/tasks/task-form-dialog.component.ts` | After a 400, call `setErrors({ server: true })` on each control named in `fieldErrors` | Server field errors were stored in `serverErrors` but never shown: `<mat-error>` only renders for an invalid control, and the control was valid on the client. E2E-M02-02 requires the server field error to be displayed. | +4 |

Re-run after F1: E2E-M02-02 server error case (now shows "size must be between 0 and 200" under Title, screenshot `E2E-M02-02-server-field-error.png`), then the full rest of the E2E run, and `npx ng build` (pass). Backend untouched, so swagger/unit/curl were not affected.

## FIX tasks

None.

## Contract issues

None for M02.

## Notes

- Screenshots cannot be copied out of `run-artifacts/` (permissions), so they stay under `run-artifacts/playwright/verify-M02-r1/`.
- Test data was seeded with in-page `fetch` (shell variables in Bash are denied in headless mode).
