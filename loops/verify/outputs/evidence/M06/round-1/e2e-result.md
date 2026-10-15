# M06 round 1 - E2E on the real stack (http://localhost:4300 -> :8090, fresh stable db)

Screenshots (copying into this folder was denied by the headless permissions): `run-artifacts/playwright/verify-M06-r1-*.png`.

| Scenario | Result | Evidence |
|---|---|---|
| Empty states [real-stack-only] (fresh db, before any data) | PASS: `dashboard-today-empty`, `dashboard-overdue-empty`, `dashboard-habits-empty`, `dashboard-plans-empty`, `dashboard-recent-milestones-empty`; completion 0% (0 of 0); greeting "Good morning, QuickFlow User" | verify-M06-r1-empty-dashboard.png |
| E2E-M01-01 [smoke] nav | PASS: 6 nav links, each `<page>-title`, active item | - |
| E2E-M02-01 [smoke] add task | PASS: `tasks-row-1` "Write report / To do / Medium" | - |
| E2E-M03-01 [smoke] add habit | PASS: `habits-card-1` "Morning run / Daily / 0 days streak" | - |
| E2E-M04-01 [smoke] add learning card | PASS: `learning-card-1` "Angular course / Not started" | - |
| E2E-M05-01 [smoke] build plan | PASS: builder (task 1, habit 1, card 1), `plans-card-1` "Morning focus", Not started, 0 of 3 items; start 07:48Z (next whole minute) | - |
| E2E-M06-01 [smoke] dashboard | PASS (seeded via fetch: task due today, overdue, DONE today, archived, 2nd habit, milestone completed): greeting, 4 metric cards; Tasks due 2 / overdue 1 / 25% (1 of 4, archived excluded); today list (DONE row shows "Done", open row has `dashboard-task-complete-2`); overdue row with marker; habit checklist 2 rows with streak; active plan `dashboard-plan-1` with rest + progress + 3 item toggles once IN_PROGRESS; learning snapshot 1/1 milestones, `dashboard-recent-milestone-1` "Finish module 1 Oct 15". Every number equal to GET /api/dashboard and to the list endpoints | verify-M06-r1-E2E-M06-01-dashboard.png |
| E2E-M06-02 completion % | PASS: 25% -> 50% (2 of 4) after ticking plan task item -> 75% (3 of 4) after Complete -> 60% (3 of 5) after quick-add; GET /api/tasks gives the same counts | - |
| E2E-M06-03 quick-add | PASS: Habit "New habit", Learning "New learning resource", Plan "Create plan" dialogs open and cancel; Task quick-add saved (POST 201) and the dashboard reloaded (GET /api/dashboard) | - |
| E2E-M06-04 toggles | PASS: habit 2 toggle -> POST /api/habits/2/completions 201, reload, "Done today 1 of 2", streak 1 day; untoggle -> GET completions + DELETE .../2026-10-15 204, 0 of 2; plan item 1 -> PUT /api/plans/1/items/1 200, progress 33, task 1 DONE, tasks.completed +1 (also closes the M05-05 "dashboard follows" check) | - |
| E2E-M06-05 rest time | PASS real clock: "59m 51s" -> "59m 48s" over 3 s; clock override 08:18Z -> "29m 59s", 08:40Z -> "7m 58s"; 15:00Z -> "Good evening"; `__qfClock.clear()` back to real time | - |
| Backend down [real-stack-only] | PASS after fix F1: `dashboard-load-error` "The dashboard could not be loaded." + interceptor snackbar, last numbers kept; failed habit / plan item toggle now resets the checkbox (before the fix it stayed checked while "Done today 0 of 2") | verify-M06-r1-backend-down.png (taken before the fix) |
| Restart persistence | PASS: backend restarted without --fresh, dashboard numbers identical | - |

Console: no errors during the scenarios; only proxy 500s while the backend was stopped on purpose.
Network: no 5xx except while the backend was stopped. The start banner "Plan Morning focus has started" appeared once with one acknowledge-start (200).
