# E2E result - M04 round 1 (real stack)

Stack: frontend http://localhost:4300 -> backend :8090 (fresh `stable` db). Playwright MCP 0.0.40. 2026-10-15 ~04:05-04:10 +03:00.
Screenshots (the MCP always writes under `run-artifacts/playwright/`):
- `run-artifacts/playwright/verify-M04-r1-E2E-M04-07-empty.png`
- `run-artifacts/playwright/verify-M04-r1-E2E-M04-04-completed.png`
- `run-artifacts/playwright/verify-M04-r1-E2E-M04-06-after-remove.png`
- `run-artifacts/playwright/verify-M04-r1-smoke-E2E-M03-01.png`
- `run-artifacts/playwright/verify-M04-r1-learning-backend-down.png`

| Id | Result | Real-stack evidence |
|---|---|---|
| E2E-M04-07 [real-stack-only] | pass | Fresh db: `learning-empty-state` "No learning resources yet"; `learning-empty-add-button` opens "New learning resource" |
| E2E-M04-02 | pass | Empty -> "Title is required."; 201 chars (length checked in page) -> "Must be at most 200 characters."; 3 spaces -> "Title is required."; 0 POST/PUT/DELETE requests; cancel closes. (A 198-char title saved fine: card 1, length 198 confirmed via GET.) |
| E2E-M04-01 [smoke] | pass | "Kubernetes basics"/"Udemy course" -> card 2 first in grid (newest first), "Not started", "No milestones yet" |
| E2E-M04-03 | pass | Expand card 2; empty add -> "Milestone title is required." (no request); "Pods and deployments" + 2026-11-01 -> row shows "Target 2026-11-01", progress 0/1; second milestone 0/2 |
| E2E-M04-04 | pass | Tick m1 -> "In progress" 1/2; tick m2 -> "Completed" 2/2; untick m2 -> "In progress" 1/2 (D-7) |
| E2E-M04-05 | pass (after fix F1) | Note added with timestamp "Oct 15, 2026, 4:08:08 AM", removed -> "No notes yet.". Before F1 a stale "Note text is required." / "Milestone title is required." error showed after every successful add. Re-test after F1 (page reloaded): empty submit still blocked, after add no error and input cleared; persisted state survived reload |
| E2E-M04-06 | pass | Remove milestone 3 -> 1/2; card delete: confirm "Delete learning resource?", cancel keeps card; OK -> DELETE 204, card gone, `GET /api/learning-cards/2` 404 |
| Extra (T056 edit) | pass | Edit card 1 (no milestones): title "Clean Code book", status In progress -> saved and shown |
| Backend-down error state | pass | Backend stopped: page shows `learning-load-error` "Learning resources could not be loaded." (not the empty state) + interceptor snackbar; backend restarted --fresh afterwards |
| Smoke E2E-M01-01 | pass | All six nav links: URL, `<page>-title` present, nav item active |
| Smoke E2E-M02-01 | pass | Task "Write report" added, row shows "To do", "Medium" |
| Smoke E2E-M03-01 | pass | Habit "Read 20 minutes": "Daily", "0 days streak", "Not done today" |

Console: no app errors. The only `[ERROR]` lines are Chromium's "Failed to load resource" for my own deliberate `GET /api/learning-cards/2` after delete (404, expected) and the dev-proxy 500s while the backend was deliberately stopped.
Network: no 5xx from the backend in any scenario with the backend up (all 200/201/204, plus the expected 404).
