# M07 round 1 - browser results (Playwright MCP, real stack)

Stack: backend :8090 (fresh `stable` db at start), frontend :4300 -> :8090. Browser 1440x900, local time +03:00, 2026-10-15 ~11:04-11:12.
Screenshots are written by the Playwright tool to `run-artifacts/playwright/` (prefix `verify-M07-r1-`); copying into this folder is not permitted in the headless session.

## Empty states (fresh db, before any data)
Tasks `tasks-empty-state`, Habits `habits-empty-state`, Learning `learning-empty-state`, Plans `plans-empty-state` shown; Dashboard all zeros ("Completed 0% (0 of 0)"); Settings loads defaults (QuickFlow User, Africa/Cairo). No `.error-state`. PASS.

## E2E-M07-02 [smoke] (in order)
| Scenario | Result | Evidence |
|---|---|---|
| E2E-M01-01 | PASS | the six nav links route to /dashboard../settings, `<page>-title` present, exactly one `aria-current="page"` and it is the active link |
| E2E-M02-01 | PASS | Add task "Pay rent": save disabled when empty, enabled with title; dialog closes; `tasks-row-1` "To do · Medium" |
| E2E-M03-01 | PASS | "Read 20 pages" Daily -> `habits-card-1` "0 days streak · Not done today · Mark done" |
| E2E-M04-01 | PASS | "Kubernetes basics" -> `learning-card-1` "Not started · No milestones yet" |
| E2E-M05-01 | PASS | Plan "Morning block" from task 1 + habit 1 + card 1, default start 11:07 (next minute), end 12:07, 60 min; Not started -> at 11:07 banner "Plan Morning block has started", In progress, "Rest 59m 32s" |
| E2E-M06-01 | PASS | after seeding a due-today task, an overdue task and a done milestone: greeting, 4 metric cards (Due today 1, Overdue 1, Done today 0 of 1, Active 1, Milestones 1/1), today/overdue lists, habit checklist, recent milestone "Pods chapter", active plan with rest. Screenshot `run-artifacts/playwright/verify-M07-r1-smoke-M06-01-dashboard.png` |

## E2E-M07-01 full journey (`loops/frontend-dev/outputs/e2e/M07-journey.md`)
The smoke data above was already in the db, so dashboard numbers are checked as deltas.
| Step | Result | Observed |
|---|---|---|
| 1 Settings | PASS | name "QuickFlow User", zone Africa/Cairo, save -> "Settings saved", `nav-settings` aria-current=page |
| 2 Baseline | PASS | due 1, overdue 1, 0%, habits 0, active 1, upcoming 0, completed 0, milestones 1/1 |
| 3 Create task | PASS | `tasks-row-4` "Journey task J1", status "To do" |
| 4 Dashboard | PASS | total 4, 0% |
| 5 Complete task | PASS | status "Done"; dashboard 25% (1 of 4) |
| 6-7 Habit | PASS | `habits-card-2` "Mark done" -> "Undo", "1 day streak"; dashboard habits done 1 |
| 8-9 Card + 2 milestones | PASS | `learning-card-2`: "0/2 milestones" -> ticked both -> "Completed", "2/2 milestones"; dashboard milestones 3/3 |
| 10 Plan | PASS | "Journey plan J1" start 11:09 (browser 11:08:38), end 12:09, 60 min, picks task 4/habit 2/card 2, save enabled only after picks; `plans-card-2` in `plans-group-active`, "Not started" |
| 11 Dashboard | PASS | upcoming 1, `dashboard-plan-2` not shown |
| 12 Start | PASS | `browser_wait_for` "Plan Journey plan J1 has started"; one banner; `plans-highlight-2`; status "In progress"; still one banner 10 s later. Screenshot `verify-M07-r1-E2E-M07-01-step12-banner.png` |
| 13 Rest | PASS | "Rest 59m 38s" -> "Rest 59m 28s" 10 s later; dismiss removes banner and highlight |
| 14 Tick all | PASS | 33% -> 67% -> 100%, "Completed", moved to History (`plans-group-completed`). Screenshot `verify-M07-r1-E2E-M07-01-step14-history.png` |
| 15 Dashboard | PASS | completed 1, active 1 (smoke plan), upcoming 0, milestones 3/3, habits done 1, 25% (1 of 4: the smoke tasks are still open). Screenshot `verify-M07-r1-E2E-M07-01-step15-dashboard.png` |
| 16 Final | PASS | `browser_console_messages`: only vite DEBUG + Angular LOG, no errors. `browser_network_requests`: every API call 200/201, no 5xx, no failed request |

## E2E-M07-04 keyboard
- Real key presses: Tab from `nav-dashboard` -> `nav-tasks` -> `nav-habits`, Enter opens /habits (aria-current moves). Enter on `habits-add-button` opens "New habit", focus in `habits-form-name`, save disabled; 5x Tab cycles name -> description -> frequency -> cancel -> name (focus trapped, disabled Save skipped); Escape closes and focus returns to `habits-add-button`.
- Learning: Tab to `learning-add-button`, Enter -> focus `learning-form-title`, save disabled; Escape -> focus back on the button.
- No button without text or aria-label on the page.
- **Found**: Material buttons had `outline: none` under keyboard focus (only a 12% state-layer tint), so T096 "visible focus outlines" did not hold for buttons. Fixed F1 (styles.scss); re-check: `learning-add-button` keyboard focus outline `rgb(63, 81, 181) solid 2px`. Screenshot `verify-M07-r1-E2E-M07-04-focus-ring.png`.
- Tasks/Plans/dashboard quick-add dialogs were walked by frontend-dev (same dialog code); not repeated.
- PASS (after F1).

## E2E-M07-03 [real-stack-only] backend down
- `scripts/backend.sh stop --port 8090`, then each page via nav: dashboard, tasks, habits, learning, plans, settings all show `.error-state` (role=alert) with `<page>-error-retry`; clicking retry while down keeps the error (request re-sent). Screenshots `verify-M07-r1-E2E-M07-03-settings-error.png`, `verify-M07-r1-E2E-M07-03-tasks-error.png`.
- Backend started again (same db): Settings retry -> form filled with saved values, error gone. Second stop/start cycle: Tasks retry -> 4 rows, error gone.
- Console errors during this scenario are only Chromium "Failed to load resource: 500" for the proxy while the backend was down (expected); no Angular exception.
- PASS.
