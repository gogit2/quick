# Verify report - M05 Plans, round 1

- **Milestone**: M05 - Plans
- **Round**: 1 (trial 0)
- **Verdict**: **PASS**
- **Time**: 2026-10-15T06:55:00+03:00
- **Stack**: backend :8090 (fresh `stable` db, restarted `--fresh` before curl), frontend :4300 -> :8090

## Checks

| # | Check | Result | Evidence |
|---|---|---|---|
| 1 | Review (`git diff HEAD` + untracked) | pass | this report, "Review notes" |
| 2 | Swagger export vs contract (M05) | pass | `evidence/M05/round-1/swagger-diff.md`, `openapi-exported.yaml` |
| 3 | `./mvnw -q verify` | pass (159 tests, 0 failures, 0 errors) | backend/target/surefire-reports |
| 3b | `npx ng build` | pass (500 kB budget warning only, known) | - |
| 4 | curl regression up to M05 (fresh db) | pass: M01 6/6, M02 14/14, M03 13/13, M04 12/12, M05 16/16 | `evidence/M05/round-1/curl.log` |
| 5 | Playwright MCP, M05 scenarios on real stack | pass: E2E-M05-01..09 | `evidence/M05/round-1/e2e-result.md`, `run-artifacts/playwright/verify-M05-r1-*.png` |
| 5b | Smoke of earlier milestones | pass: E2E-M01-01, E2E-M02-01, E2E-M03-01, E2E-M04-01 | `evidence/M05/round-1/e2e-result.md` |
| 6 | Contract issues of M05 | none open | `loops/_shared/contract-issues.md` |

## Review notes
- Backend T060-T068 all present: `V5__plans.sql` (no status column), `PlanStatusCalculator` (pure; D-2, BR12, round-half-up), `PlanMapper`, `PlanService` (injected Clock everywhere, BR10/BR11/unknown/duplicate source -> 400 fieldErrors, TASK items call `TaskService.complete/reopen` in one transaction, acknowledge idempotent), `PlanItemCleanup` (removes items, deletes plans left empty, same transaction), thin `PlanController` with the 6 operations. DTO names equal contract schemas. Tests: PlanStatusCalculatorTest, PlanServiceTest, PlanControllerTest, PlanItemCleanupTest.
- Frontend T069-T075 all present: plans page with groups, cards, empty/loading/error states; builder dialog (validation, window warning, pickers, server fieldErrors); plan card with item toggles, delete confirm, live rest time via ClockService; `NotificationService` (catch-up on app start, once per plan per session, silent acknowledge when notifications off); shell banner. All acceptance test ids present.
- `restSeconds` is serialized as `null` (not omitted) outside IN_PROGRESS; the contract declares it `nullable: true`, so this conforms.

## Fixes applied
None.

## FIX tasks
None.

## Contract issues
None for M05.

## Notes (non-blocking, for later milestones)
- Status on the plans page flips from Not started to In progress up to 30 s after the start (page reload interval; status is server-computed). The start highlight (notification sync) appeared ~10 s before the card status changed. Acceptable for E2E-M05-06; M07 journey should allow up to 30 s.
- The catch-up banner also announces plans that already ended (e.g. a past plan created with startNotified=false shows "Plan ... has started"). Consistent with D-8 catch-up and assumption A-frontend-dev-M05-3; wording could say "started" for past plans.
- Cards in the History group never get the start highlight (only the active group passes `highlighted`); harmless.
- Builder dialog at 1440 px: the form's right edge is slightly clipped (form min-width 560 px inside the dialog). Cosmetic; candidate for M07 polish.
- Still open from earlier: no catch-all 5xx ApiError handler (M01), Complete button visible on DONE tasks, bundle > 500 kB budget warning.
- Two headless permission denials during this round (a curl with quoted variable payload and a `cp` with a glob); the API setup was done through `browser_evaluate` fetch instead, and screenshots stay in `run-artifacts/playwright/`.
