# Verify report - M04 Learning, round 1

- **Milestone**: M04 - Learning
- **Round**: 1 (trial 0)
- **Verdict**: **PASS**
- **Time**: 2026-10-15T04:12:00+03:00
- **Stack**: backend :8090 (fresh `stable` db), frontend :4300 -> :8090

## Checks

| Check | Result | Evidence |
|---|---|---|
| Review (`git diff HEAD` + untracked) | pass | T045-T059 all present: V4 migration, `LearningStatusCalculator` (pure, D-7), entities/DTOs named as the contract, `LearningService` (Clock for createdAt/completedAt, 409 on status with milestones, 404 for foreign milestone/note, SourceDeletedEvent on delete), thin controller (10 ops); FE models, `learning.api.ts`, page, dialog, details component with all acceptance test ids |
| Swagger | pass | `evidence/M04/round-1/swagger-diff.md` (Result: PASS) |
| Unit (`./mvnw -q verify`) | pass | 125 tests, 0 errors, 0 failures (surefire) |
| curl regression `--upto M04` | pass | `evidence/M04/round-1/curl.log`: M01 6/6, M02 14/14, M03 13/13, M04 12/12 |
| E2E M04 (real stack) | pass (after fix F1) | `evidence/M04/round-1/e2e-result.md`; E2E-M04-01..07 + edit + backend-down error state |
| Smoke | pass | E2E-M01-01, E2E-M02-01, E2E-M03-01 in `e2e-result.md` |
| `ng build` (after fix) | pass | 500 kB budget warning only (known) |

## Fixes applied

- **F1** `frontend/src/app/features/learning/learning-card-details.component.ts` (~8 lines): after a successful milestone or note add, the form was cleared with `FormGroup.reset()`, which leaves the `FormGroupDirective` in the submitted state, so Material showed "Milestone title is required." / "Note text is required." right after every successful add. Now the template passes the `ngForm` directive and the handlers call `formDir.resetForm()`. Re-tested on the real stack: empty submit still blocked with the error, after a valid add no error and the input is empty; `ng build` passes.

## FIX tasks

None.

## Contract issues

None open for M04.

## Notes

- Assumptions A-backend-dev-M04-1 (any status in PUT with milestones -> 409) and A-backend-dev-M04-2 (deleting the last milestone keeps the last derived status) are consistent with the contract and D-7; accepted.
- Chromium logs expected 4xx as console `[ERROR] Failed to load resource`; not app errors.
- Still open, non-blocking (from earlier rounds): no catch-all 5xx ApiError handler, Complete button on DONE tasks, GET /api/tasks 400 on bad enum not in the contract, bundle > 500 kB budget warning.
