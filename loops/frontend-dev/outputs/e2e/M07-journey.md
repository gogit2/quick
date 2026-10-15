# E2E-M07-01 Full journey (real stack, executed by the verifier)

Scenario for PRD section 11 on the real stack: backend on `http://localhost:8090` (`scripts/backend.sh`, fresh database),
frontend on `http://localhost:4300` (`scripts/frontend.sh start --api http://localhost:8090`). The Prism mock is NOT used here.
Executed with the Playwright MCP. Frontend-dev only writes it; the verifier runs it.

Time: the plan starts about one minute after the browser time (`now` rounded up to the next whole minute) and ends one hour later.
Read the browser time first with `browser_evaluate` (`new Date()`), then type the datetime values in local time
(`YYYY-MM-DDTHH:mm`). Use the app clock override only if the real time is not usable; never change the server clock.

Test ids used: `nav-settings`, `nav-tasks`, `nav-habits`, `nav-learning`, `nav-plans`, `nav-dashboard`,
`settings-display-name`, `settings-time-zone`, `settings-save-button`, `settings-saved-message`,
`tasks-add-button`, `tasks-form-title`, `tasks-form-save`, `tasks-row-<taskId>`, `tasks-status-<taskId>`, `tasks-complete-<taskId>`,
`habits-add-button`, `habits-form-name`, `habits-form-frequency`, `habits-form-save`, `habits-card-<habitId>`, `habits-toggle-<habitId>`, `habits-streak-<habitId>`, `habits-progress-<habitId>`,
`learning-add-button`, `learning-form-title`, `learning-form-save`, `learning-card-<cardId>`, `learning-expand-<cardId>`,
`learning-milestone-title-<cardId>`, `learning-milestone-target-<cardId>`, `learning-milestone-add-<cardId>`, `learning-milestone-<milestoneId>`, `learning-milestone-done-<milestoneId>`, `learning-progress-<cardId>`, `learning-status-<cardId>`,
`plans-create-button`, `plans-form-title`, `plans-form-duration`, `plans-form-start`, `plans-form-end`, `plans-form-save`, `plans-pick-task-<taskId>`, `plans-pick-habit-<habitId>`, `plans-pick-learning-<cardId>`,
`plans-card-<planId>`, `plans-status-<planId>`, `plans-rest-<planId>`, `plans-item-<itemId>`, `plans-item-toggle-<itemId>`, `plans-start-banner`, `plans-start-banner-dismiss`, `plans-highlight-<planId>`, `plans-group-active`, `plans-group-completed`,
`dashboard-metric-tasks`, `dashboard-tasks-due-today`, `dashboard-tasks-overdue`, `dashboard-completion-percent`, `dashboard-metric-habits`, `dashboard-habits-done`, `dashboard-metric-plans`, `dashboard-plans-active`, `dashboard-plans-upcoming`, `dashboard-plans-completed`, `dashboard-metric-learning`, `dashboard-learning-in-progress`, `dashboard-learning-milestones`,
`dashboard-quick-add-task`, `dashboard-task-<taskId>`, `dashboard-plan-<planId>`, `dashboard-plan-rest-<planId>`, `dashboard-plan-progress-<planId>`, `dashboard-plan-item-toggle-<itemId>`.

Names used by the journey (unique per run, so the steps do not collide with old data): task `Journey task <run>`, habit `Journey habit <run>`, card `Journey card <run>` with milestones `Chapter A <run>` and `Chapter B <run>`, plan `Journey plan <run>`.

## Steps

1. **Settings check.** Navigate to `/settings`. `settings-display-name` is not empty; `settings-time-zone` shows a zone. Save with `settings-save-button`: `settings-saved-message` appears. Check `nav-settings` has `aria-current="page"`.
2. **Dashboard baseline.** Navigate to `/dashboard`. Record `dashboard-tasks-due-today`, `dashboard-tasks-overdue`, `dashboard-completion-percent`, `dashboard-habits-done`, `dashboard-plans-active`, `dashboard-plans-upcoming`, `dashboard-plans-completed`, `dashboard-learning-milestones`. Expect the same numbers as the database after the reset (zeros on an empty database).
3. **Create task.** `nav-tasks` → `tasks-add-button` → `tasks-form-title` = `Journey task <run>`, `tasks-form-save`. The dialog closes; a row `tasks-row-<taskId>` shows the title and `tasks-status-<taskId>` = "To do". Record `<taskId>`.
4. **Dashboard after task.** `nav-dashboard`: `dashboard-metric-tasks` shows total 1 (`completed` 0, completion 0%). The task is in `dashboard-today-tasks` only if it has a due date today (no due date is set here).
5. **Complete task.** `nav-tasks` → `tasks-complete-<taskId>`. The row shows "Done" in `tasks-status-<taskId>`. Dashboard: completion 100% (1 of 1).
6. **Create habit.** `nav-habits` → `habits-add-button` → `habits-form-name` = `Journey habit <run>`, frequency Daily, `habits-form-save`. `habits-card-<habitId>` appears with `habits-toggle-<habitId>` = "Mark done".
7. **Complete habit.** `habits-toggle-<habitId>` → label "Undo", `habits-streak-<habitId>` shows 1 day. Dashboard: `dashboard-habits-done` = 1.
8. **Create learning card with two milestones.** `nav-learning` → `learning-add-button` → `learning-form-title` = `Journey card <run>`, `learning-form-save`. Expand with `learning-expand-<cardId>`. In `learning-milestone-title-<cardId>` type `Chapter A <run>` and click `learning-milestone-add-<cardId>`; repeat with `Chapter B <run>`. `learning-progress-<cardId>` reads "0/2 milestones".
9. **Complete the milestones.** Tick `learning-milestone-done-<milestoneA>` and `learning-milestone-done-<milestoneB>`. `learning-status-<cardId>` = "Completed", `learning-progress-<cardId>` = "2/2 milestones". Dashboard: `dashboard-learning-milestones` = 2/2.
10. **Build the plan.** `nav-plans` → `plans-create-button`. Set `plans-form-title` = `Journey plan <run>`, `plans-form-start` to the value of browser time rounded up to the next whole minute plus 1 minute, `plans-form-end` to that start plus 1 hour, `plans-form-duration` = 60. Pick `plans-pick-task-<taskId>`, `plans-pick-habit-<habitId>`, `plans-pick-learning-<cardId>`. `plans-form-save` is enabled only when the form is valid and at least one item is picked. The dialog closes; `plans-card-<planId>` is in the "Active and upcoming" group (`plans-group-active`) with `plans-status-<planId>` = "Not started".
11. **Dashboard after plan.** `nav-dashboard`: `dashboard-plans-upcoming` = 1, `dashboard-plan-<planId>` is not shown yet (only in-progress plans appear in the active list).
12. **Wait for the start.** `browser_wait_for` the text "has started" (timeout 120 s) or the `plans-start-banner` test id. Assert: `plans-start-banner` contains "Journey plan <run>", `plans-highlight-<planId>` exists, `plans-status-<planId>` = "In progress". Only one banner appears; wait 10 s more and assert no second banner for the same plan.
13. **Rest time.** `plans-rest-<planId>` reads "Rest 59m .. s" or lower and decreases between two reads 5 s apart. Dismiss with `plans-start-banner-dismiss`; `plans-start-banner` is gone and `plans-highlight-<planId>` is gone.
14. **Tick all items.** On `nav-plans` tick every `plans-item-toggle-<itemId>` (one task, one habit, one learning resource). After each tick the progress bar updates. After the last tick the plan shows 100% and `plans-status-<planId>` = "Completed", and it moves to `plans-group-completed` (History). Refresh with `browser_navigate` if it has not moved after 35 s (the page reloads every 30 s).
15. **Dashboard at the end.** `nav-dashboard`: `dashboard-plans-completed` = 1, `dashboard-plans-active` = 0, `dashboard-plans-upcoming` = 0, `dashboard-learning-milestones` = 2/2, `dashboard-habits-done` = 1 (habit done today), `dashboard-completion-percent` = 100%.
16. **Final checks.** `browser_console_messages`: no error entries. `browser_network_requests`: no request with a status 5xx (and none with status 0 or failed) during the whole journey.

## Expected results (summary)
- Every step above passes; screenshots at steps 3, 5, 9, 12, 14 and 15 are stored as `outputs/evidence/M07/E2E-M07-01-step<n>.png` by the verifier.
- No console errors, no 5xx responses.
