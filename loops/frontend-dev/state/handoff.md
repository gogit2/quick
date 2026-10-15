# Handoff - frontend-dev

## Where M02 stands (iteration 2, trial 0): all FE tasks ticked
- Ticked: T025, T026, T027, T028, T029, T030. `npx ng build` passes (500 kB budget warning only).
- E2E-M02-01..07 executed on the Prism mock with the Playwright MCP, 0 console errors. E2E-M02-08 written and listed as `real_stack_only`.
- Evidence: `outputs/e2e/M02.md`, `outputs/evidence/M02/result.json`. Screenshots are in `run-artifacts/playwright/` (copy into `outputs/evidence/M02/` was denied by the permission layer).

## Gaps the verifier should check on the real stack (the mock cannot show them)
1. E2E-M02-02: server `fieldErrors` shown under the title (send a 201-char title or a bad field to the real backend).
2. E2E-M02-04: after Complete, the status chip reads "Done".
3. E2E-M02-05: Restore on a real archived task moves it back to the default list (`POST /api/tasks/{id}/restore`).
4. E2E-M02-08: fresh db shows `tasks-empty-state` with `tasks-empty-add-button`; no-match search shows `tasks-no-results`.
5. Snackbar on a non-5xx action error (complete/archive/delete) is code-only so far.

## Decisions and traps
- The empty state uses the shared EmptyStateComponent, so its button is `tasks-empty-add-button`. The header button is `tasks-add-button`.
- Due filter has no explicit date range UI (D-13 lists one). Not in T027; add it only if asked.
- Search is debounced 300 ms in the page; filters reload immediately. Requests go through a `switchMap` Subject (`reload`).
- Complete is always enabled, including on DONE tasks.
- 5xx errors: the interceptor shows the snackbar; the page shows its own snackbar for other failures.
- Mock is stateless and always returns `archived: false`. The row label therefore never shows "Restore" on the mock.
- `browser_take_screenshot` writes only into `run-artifacts/playwright/`. Copying files with Bash `cp` is denied here.
- `browser_snapshot` output is very large; `browser_evaluate` is cheaper for text checks, and `performance.getEntriesByType('resource')` shows query params. Mat-select options can be clicked from `evaluate` (`.mat-mdc-select-trigger`, then `mat-option` by text). The slide toggle is a `button[role=switch]`, not an input.
- Bash compound commands mixing `mkdir`, `cp`, and `ls` were denied this iteration. Use Write for files and keep Bash commands simple.

## Commands that worked
- `cd frontend && npx ng build`
- `scripts/mock.sh start`, `scripts/frontend.sh start --api http://localhost:4010`
- `scripts/frontend.sh stop`, `scripts/mock.sh stop` (both clean; `frontend.sh status` says `stopped`)
- Next milestone: M03 is for the next runner iteration. This loop has no further M02 work.
