# M05 round 1 - Playwright MCP on the real stack (http://localhost:4300 -> :8090)

Screenshots: `run-artifacts/playwright/verify-M05-r1-*.png` (the MCP screenshot tool writes only there).

| Scenario | Result | Evidence |
|---|---|---|
| E2E-M05-09 [real-stack-only] fresh db empty state | PASS: `plans-empty-state` with "No plans yet" + `plans-empty-add-button`, `plans-create-button` in header | verify-M05-r1-E2E-M05-09-empty.png |
| E2E-M01-01 [smoke] nav | PASS: 6 nav links, each `<page>-title`, active item, reload on /tasks keeps page | - |
| E2E-M02-01 [smoke] add task | PASS: `tasks-row-1` "Write report / To do / Medium" | - |
| E2E-M03-01 [smoke] add habit | PASS: `habits-card-1` "Morning run / Daily / 0 days streak" | - |
| E2E-M04-01 [smoke] add learning card | PASS: `learning-card-1` "Angular course / Not started" | - |
| E2E-M05-02 builder validation | PASS: duration 0 and priority 0 -> mat-errors, end before start -> `plans-form-window-error`, no items -> `plans-form-items-error`, no POST | verify-M05-r1-E2E-M05-02-validation.png |
| E2E-M05-01 [smoke] build plan from task+habit+card | PASS: POST /api/plans 201, `plans-card-1` "Morning focus", Not started, 0 of 3 items, 3 items with resolved titles | - |
| E2E-M05-03 duration > window | PASS: dialog shows `plans-form-duration-warning`, saves (201), card shows `plans-duration-warning-2` | verify-M05-r1-E2E-M05-04-06-groups-rest.png |
| E2E-M05-04 groups | PASS: `plans-group-active` (plans 1, 2 ordered by priority) and `plans-group-completed` (past plan 3, Completed 0%) | verify-M05-r1-E2E-M05-04-06-groups-rest.png |
| E2E-M05-06 real clock | PASS: plan 1 start 03:50:00Z; status Not started until the 30 s reload at 03:50:28Z, then In progress with `plans-rest-1` "59m 31s" -> "59m 06s" counting down | verify-M05-r1-E2E-M05-04-06-groups-rest.png |
| E2E-M05-07 start banner | PASS: catch-up banner "Plan Yesterday review has started" (plan 3), then after dismiss "Plan Morning focus has started" with `plans-highlight-1`; exactly one POST acknowledge-start per plan (3 and 1, 200); after reload no repeat. Notifications off (settings PUT): new IN_PROGRESS plan 4 -> no banner, no highlight, startNotified=true (silent acknowledge) | verify-M05-r1-E2E-M05-07-start-banner.png |
| E2E-M05-05 tick items | PASS: tick task item -> PUT 200, "1 of 3 items (33%)", GET /api/tasks/1 DONE; untick -> 0%, task TODO; tick all 3 -> 100%, Completed, rest hidden, card moves to History; habit totalCompletions 0 (BR13); Tasks page row shows Done. Dashboard part n/a (M06) | - |
| E2E-M05-08 delete with confirm | PASS: confirm dialog (`confirm-cancel`/`confirm-ok`), cancel keeps card, Delete removes it, GET /api/plans/2 404, learning card 1 still exists | - |
| Backend down (extra) | PASS: `plans-load-error` "Plans could not be loaded." + interceptor snackbar; no fake empty state | verify-M05-r1-plans-backend-down.png |

Console: no app errors. Only 404s from the verifier's own fetch probes (/api/dashboard is M06, /api/plans/2 after delete) and proxy 500s while the backend was stopped on purpose.
Network: no 5xx except while the backend was stopped.
