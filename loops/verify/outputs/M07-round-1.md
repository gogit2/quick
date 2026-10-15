# Verify report - M07 Hardening, round 1

- Milestone: M07 - Hardening (spec `specs/001-quickflow`)
- Round: 1 (trial 0)
- Verdict: **PASS**
- Time: 2026-10-15T11:14:00+03:00
- Stack: backend :8090 (fresh `stable` db), frontend :4300 -> :8090

## Checks
| # | Check | Result | Evidence |
|---|---|---|---|
| 1 | Review (`git diff HEAD` + untracked) | pass | see "Review" below |
| 2 | Swagger (35 operations, up to M07) | pass | `evidence/M07/round-1/swagger-diff.md`, `openapi-exported.json` |
| 3 | `./mvnw -q verify` | pass: 188 tests, 0 failures, 0 errors | surefire reports in `backend/target/surefire-reports/` |
| 4 | curl regression `--upto M07` (fresh db) | pass: 7 suites, 73 cases, 0 failed (M07 5/5) | `evidence/M07/round-1/curl.log` |
| 5a | E2E-M07-01 full journey (real stack, real clock) | pass | `evidence/M07/round-1/e2e-result.md`, `run-artifacts/playwright/verify-M07-r1-E2E-M07-01-*.png` |
| 5b | E2E-M07-03 [real-stack-only] backend down, retry on all 6 pages | pass | `e2e-result.md`, `verify-M07-r1-E2E-M07-03-*.png` |
| 5c | E2E-M07-04 keyboard | pass after F1 | `e2e-result.md`, `verify-M07-r1-E2E-M07-04-focus-ring.png` |
| 5d | Empty states (fresh db) | pass | `e2e-result.md` |
| 6 | Smoke E2E-M07-02 = E2E-M01-01, M02-01, M03-01, M04-01, M05-01, M06-01 | pass | `e2e-result.md`, `verify-M07-r1-smoke-M06-01-dashboard.png` |
| - | Console / network | no app console errors, no 5xx with the backend up | `e2e-result.md` step 16 |
| - | `npx ng build` (after F1) | pass, no warnings | - |

## Review
- T089: `BusinessRulesSweepTest` (14 MockMvc tests BR1-BR14). The production change in `GlobalExceptionHandler`, which turns an invalid enum in the JSON body into a 400 "Validation failed" with the field name, is correct and matches the ApiError shape.
- T090: `PlanLifecycleIntegrationTest` covers NOT_STARTED -> IN_PROGRESS -> COMPLETED with a MutableClock, a context restart on the same H2 file, and the dashboard roll-up.
- T091: catch-all `@ExceptionHandler(Exception.class)`. Spring `ErrorResponse` exceptions keep their status (404 unknown route, 405, 415) with the ApiError body. Other exceptions return a 500 "Unexpected error" with no stack trace; the stack trace goes to the log. `RequestLogFilter` writes one INFO line per request (it uses `System.nanoTime` only for a duration, which is not business time and so does not conflict with principle V). `ErrorHandlingTest` 7 tests.
- T092: swagger PASS, contract untouched.
- T093: `curl/M07.sh` AC-M07-01..05 present, each case names its acceptance id.
- T094: `<page>-error-retry` on all six pages, in a shared `.error-state` (role=alert); Settings gained `settings-loading` / `settings-load-error`.
- T095: save buttons disabled while invalid (task, habit, card, milestone, note, plan builder); plan builder title rejects whitespace, shows max-length, and needs at least one item.
- T096: labelled nav, `ariaCurrentWhenActive="page"`, global `:focus-visible`. Material buttons still suppressed the outline; fixed (F1).
- T097: the banner fired once per plan on the real clock (two plans); rest time never negative.
- T098/T099: scenario files present. T100: README "Running the application" is correct (scripts, ports, `backend/data/stable.mv.db`, `?clockOffsetMs=`). The initial bundle warning budget went from 500 kB to 600 kB, and the 1 MB error limit is unchanged. That is acceptable.
- Constitution: no contract edits, no `now()` without the clock in main code, data-testids per principle VI, no commits.

## Fixes applied
| Id | File | What | Why | Lines |
|---|---|---|---|---|
| F1 | `frontend/src/styles.scss` | Added `.mat-mdc-button-base:focus-visible { outline: 2px solid #3f51b5; outline-offset: 2px; }` | Material MDC buttons set `outline: none`, so keyboard focus on buttons showed only a faint 12% tint. T096 / E2E-M07-04 require visible focus. | +6 |

Re-run after F1: E2E-M07-04 focus check (outline `rgb(63, 81, 181) solid 2px` on `learning-add-button`), `npx ng build` OK, and the later browser checks (E2E-M07-03) ran on the fixed CSS.

## FIX tasks
None.

## Contract issues
None logged for M07 (`loops/_shared/contract-issues.md` has no entries).

## Notes (non-blocking, for the user / backlog)
- If Settings fails to load, it still shows the editable form with defaults and an enabled Save. Saving in that state would overwrite the stored settings with the defaults. Suggest disabling Save while `loadFailed()`.
- When a plan is created from an already-DONE task, a done habit or a completed card, its items start unticked. This is consistent with BR13 (item completion is independent except for the task side effect), but users may find it surprising.
- Carried over from M06: greeting uses the browser hour, the dashboard checklist lists done weekly habits, the Complete button stays on DONE tasks, and the builder dialog's right edge at 1440 px.
- The M07 journey ran on top of the smoke data (same db), so dashboard numbers were checked as deltas. AC-M07-01 in curl proves the absolute counts on an empty db.
