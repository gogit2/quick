# M03 round 1 - Playwright MCP on the real stack (http://localhost:4300 -> :8090)

Screenshots are in `run-artifacts/playwright/` (the MCP writes there; copying is not allowed), prefix `verify-M03-r1-`.

| Scenario | Result | Evidence / observation |
|---|---|---|
| E2E-M03-07 [real-stack-only] empty state | pass | Fresh db: `habits-empty-state` + `habits-empty-add-button`; the button opens the "New habit" dialog. `verify-M03-r1-E2E-M03-07-empty-state.png` |
| E2E-M03-03 name validation | pass | Empty -> "Name is required.", 151 chars -> "Must be at most 150 characters.", no POST. `verify-M03-r1-E2E-M03-03-name-151.png` |
| E2E-M03-01 [smoke] add daily habit | pass | POST 201, card `habits-card-1` shows name, "Daily", "0 days streak", "Not done today". `verify-M03-r1-E2E-M03-01-02-cards.png` |
| E2E-M03-02 add weekly habit | pass | Card 2 shows "Weekly", "0 weeks streak", "Not done this week". Same screenshot. |
| E2E-M03-04 toggle completion | pass | POST /api/habits/1/completions 201 -> "1 day streak", "Done today", button "Undo"; Undo -> GET completions?from=2026-10-15&to=2026-10-15 200 + DELETE /completions/2026-10-15 204 -> back to 0 / Not done. `verify-M03-r1-E2E-M03-04-done.png` |
| E2E-M03-05 real 409 | pass | Habit 2 completed by fetch behind the UI, then stale "Mark done": POST 409 -> snackbar "Already completed", list refreshed to "1 week streak / Done this week". `verify-M03-r1-E2E-M03-05-already-completed.png` |
| E2E-M03-06 edit, deactivate, delete | pass | Edit prefilled, PUT 200 (name + description). Deactivate PUT 200: card class `inactive`, opacity 0.5, `habits-inactive-1` badge, toggle disabled, button "Activate". Show-inactive OFF -> GET ?active=true hides it. Delete confirm -> DELETE 204, GET /api/habits/1 404, only card 2 left. `verify-M03-r1-E2E-M03-06-deactivated.png`, `verify-M03-r1-E2E-M03-06-deleted.png` |
| Backend down error state (habits) | pass | backend stopped: `habits-load-error` "Habits could not be loaded.", no empty state, global snackbar. Backend restarted (db kept). `verify-M03-r1-habits-backend-down.png` |
| Smoke E2E-M01-01 | pass | 6 nav links: URL, `<page>-title`, active class; reload on /settings keeps page + active. `verify-M03-r1-smoke-E2E-M01-01-reload-settings.png` |
| Smoke E2E-M02-01 | pass | "Pay rent" added, row shows "To do" + "Medium". `verify-M03-r1-smoke-E2E-M02-01-add-task.png` |

Console: no application errors. The only `[ERROR]` lines are Chromium's network log of the intended 409 (E2E-M03-05), a 404 from the verifier's own `fetch('/api/habits/1')` probe after delete, and the proxy 500s while the backend was stopped on purpose.
Network: no 5xx during the scenarios with the backend up.
