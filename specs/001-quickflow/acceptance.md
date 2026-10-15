# Acceptance: QuickFlow (spec 001)

Curl cases (`AC-Mxx-nn`) are written by backend-dev in `loops/backend-dev/outputs/curl/Mxx.sh` and run by the verifier on the real backend. Playwright scenarios (`E2E-Mxx-nn`) are written by frontend-dev in `loops/frontend-dev/outputs/e2e/Mxx.md`, executed first against the Prism mock (stateless: requests are checked, not state) and again by the verifier on the real stack **with real state**.

Tags: **[smoke]** = re-run by the verifier in every later milestone. **[real-stack-only]** = written by frontend-dev, executed only by the verifier (empty and error states cannot be shown by the mock). Every case names the story (`US-…`), requirement (`FR-…`, `BR…`, `NFR-…`, `SC-…`) or decision (`D-…`) it proves.

All `data-testid`s follow constitution VI (`<page>-<element>[-<qualifier>]`). The ids listed per milestone are **mandatory**; frontend-dev may add more.

---

## M01 Foundation

### Curl
| Id | Case | Proves |
|---|---|---|
| AC-M01-01 | `GET /api/settings` on an empty db: 200, `configured=false`, `timeZone=UTC`, `notificationsEnabled=true`, `defaultStartPage=DASHBOARD` | US-SET-01, D-10 |
| AC-M01-02 | `PUT /api/settings` valid, then `GET` returns the values with `configured=true` | US-SET-02, D-10, NFR-02 |
| AC-M01-03 | invalid time zone -> 400, `fieldErrors[].field = timeZone` | D-4, D-16 |
| AC-M01-04 | invalid `defaultStartPage` -> 400 | US-SET-02 |
| AC-M01-05 | blank `displayName` -> 400 | US-SET-01 |
| AC-M01-06 | `GET /actuator/health` -> `status UP` | runner health wait |

### Playwright
| Id | Scenario | Proves |
|---|---|---|
| E2E-M01-01 **[smoke]** | Open the app, click each of the six nav links; each page opens with its `<page>-title`, its nav item is active; reload keeps the page | US-NAV-01, FR-10, SC-007 |
| E2E-M01-02 | Tasks, Habits, Learning, Plans placeholder pages show their empty state and add action | US-NAV-02, NFR-03 |
| E2E-M01-03 | Settings shows the 5 fields, Save sends `PUT /api/settings`, `settings-saved-message` appears; real stack: reload shows the saved values | US-SET-01, US-SET-02, FR-11 |
| E2E-M01-04 | Opening `/` redirects to the start page from Settings (real stack: change it to Tasks, reload `/` -> `/tasks`) | US-SET-02, D-10 |

Test ids: `nav-dashboard nav-tasks nav-habits nav-learning nav-plans nav-settings`, `dashboard-title tasks-title habits-title learning-title plans-title settings-title`, `tasks-empty-state habits-empty-state learning-empty-state plans-empty-state` (+ `<page>-empty-add-button`), `confirm-ok confirm-cancel`, `settings-display-name settings-email settings-time-zone settings-notifications settings-start-page settings-save-button settings-saved-message`.

---

## M02 Tasks

### Curl
| Id | Case | Proves |
|---|---|---|
| AC-M02-01 | `POST /api/tasks` title only -> 201, status TODO, priority MEDIUM, `archived=false` | US-TSK-01, FR-01, D-11 |
| AC-M02-02 | blank title -> 400 with `fieldErrors[title]` | BR1 |
| AC-M02-03 | title of 201 chars -> 400 (200 chars accepted) | BR1 |
| AC-M02-04 | `GET /api/tasks/{id}` 200, unknown id 404 with `ApiError` | FR-01, D-16 |
| AC-M02-05 | `PUT` changes title, description, status, priority, due date; `updatedAt` changes | US-TSK-02 |
| AC-M02-06 | `POST .../complete` -> status DONE | US-TSK-03, BR5 |
| AC-M02-07 | archive hides the task from the default list; `archived=true` lists it | US-TSK-04, BR4 |
| AC-M02-08 | restore returns it to the default list | US-TSK-04, FR-01 |
| AC-M02-09 | `DELETE` -> 204, then GET 404 and absent from lists | US-TSK-04, BR14 |
| AC-M02-10 | `q` search is case-insensitive and partial on title | US-TSK-06, FR-02, D-14 |
| AC-M02-11 | `status` + `priority` filters combine | US-TSK-05, FR-02 |
| AC-M02-12 | `due=OVERDUE` returns a task due yesterday with `overdue=true`; a DONE one is not overdue | US-TSK-07, FR-02 |
| AC-M02-13 | `sort=DUE_DATE_ASC` order, nulls last | FR-02 |
| AC-M02-14 | invalid status value -> 400; description of 2001 chars -> 400 | BR3, BR2 |

### Playwright
| Id | Scenario | Proves |
|---|---|---|
| E2E-M02-01 **[smoke]** | Add a task via the dialog; mock: POST body has the entered title; real: the row appears with its status and priority | US-TSK-01, SC-001 |
| E2E-M02-02 | Dialog validation: empty title blocked, 201 chars blocked, server field error displayed | BR1, BR2 |
| E2E-M02-03 | Edit a task (title, priority, due date) | US-TSK-02 |
| E2E-M02-04 | Complete a task (`tasks-complete-<id>`); real: status chip shows Done | US-TSK-03 |
| E2E-M02-05 | Archive (disappears from default list, visible with `tasks-show-archived`, restore) and delete with confirm | US-TSK-04, BR4 |
| E2E-M02-06 | Search and each filter/sort send the right query parameters; real: results match | US-TSK-05, US-TSK-06, FR-02 |
| E2E-M02-07 | A task due before today is marked overdue (`tasks-overdue-<id>`) | US-TSK-07 |
| E2E-M02-08 **[real-stack-only]** | Fresh db: `tasks-empty-state` with the add action; a search with no match shows `tasks-no-results` | US-NAV-02, NFR-03 |

Test ids: `tasks-add-button tasks-row-<id> tasks-status-<id> tasks-priority-<id> tasks-overdue-<id> tasks-loading tasks-no-results tasks-search-input tasks-filter-status tasks-filter-priority tasks-filter-due tasks-sort tasks-show-archived tasks-clear-filters tasks-form-title tasks-form-description tasks-form-status tasks-form-priority tasks-form-due-date tasks-form-save tasks-form-cancel tasks-complete-<id> tasks-edit-<id> tasks-archive-<id> tasks-delete-<id> confirm-ok`.

---

## M03 Habits

### Curl
| Id | Case | Proves |
|---|---|---|
| AC-M03-01 | create a DAILY habit -> 201, `active=true`, streak 0; `GET /api/habits/{id}` returns it, unknown id 404 | US-HAB-01, US-HAB-02, FR-03 |
| AC-M03-02 | create a WEEKLY habit | US-HAB-02 |
| AC-M03-03 | blank name -> 400; 151-char name -> 400 | BR6 |
| AC-M03-04 | complete for today -> 201; habit `doneForCurrentPeriod=true`, `totalCompletions=1` | US-HAB-03, US-HAB-04, FR-04 |
| AC-M03-05 | same date again -> 409 | BR7, FR-04, D-16 |
| AC-M03-06 | future date -> 400 | D-6 |
| AC-M03-07 | WEEKLY habit second completion in the same week -> 409 | D-6 |
| AC-M03-08 | completion for yesterday raises `totalCompletions` and `currentStreak` of a daily habit | US-HAB-04, D-6 |
| AC-M03-09 | list completions newest first, `from`/`to` filter | FR-04 |
| AC-M03-10 | undo (`DELETE /completions/{date}`) -> 204, habit not done again | US-HAB-03 |
| AC-M03-11 | deactivate via `PUT active=false`; completing it -> 400 | US-HAB-05, FR-03 |
| AC-M03-12 | delete habit -> 204, then 404, completions gone | US-HAB-05, BR14 |
| AC-M03-13 | `GET /api/habits?active=false` lists only inactive habits | FR-03 |

### Playwright
| Id | Scenario | Proves |
|---|---|---|
| E2E-M03-01 **[smoke]** | Add a daily habit; card shows name, "Daily", streak and progress | US-HAB-01, SC-001 |
| E2E-M03-02 | Add a weekly habit; "Weekly" label | US-HAB-02 |
| E2E-M03-03 | Name validation (empty, over 150) | BR6 |
| E2E-M03-04 | Toggle completion: POST sent; real: card shows done and streak 1, second toggle undoes | US-HAB-03, US-HAB-04 |
| E2E-M03-05 | A 409 on completion shows "Already completed" and refreshes | BR7 |
| E2E-M03-06 | Edit, deactivate (dimmed, not completable) and delete with confirm | US-HAB-05 |
| E2E-M03-07 **[real-stack-only]** | Fresh db: `habits-empty-state` guides to add | US-NAV-02 |

Test ids: `habits-add-button habits-card-<id> habits-frequency-<id> habits-streak-<id> habits-progress-<id> habits-inactive-<id> habits-loading habits-toggle-<id> habits-edit-<id> habits-deactivate-<id> habits-delete-<id> habits-show-inactive habits-form-name habits-form-description habits-form-frequency habits-form-active habits-form-save habits-form-cancel habits-empty-state`.

---

## M04 Learning

### Curl
| Id | Case | Proves |
|---|---|---|
| AC-M04-01 | create card -> 201, status NOT_STARTED, empty milestones/notes; `GET /api/learning-cards/{id}` returns it, unknown id 404 | US-LRN-01, FR-05 |
| AC-M04-02 | blank title -> 400 | BR8 |
| AC-M04-03 | add two milestones; both belong to the card | US-LRN-03, BR9, FR-06 |
| AC-M04-04 | complete one milestone -> card IN_PROGRESS, `completedAt` set | US-LRN-04, D-7 |
| AC-M04-05 | complete both -> card COMPLETED | D-7 |
| AC-M04-06 | un-complete one -> card IN_PROGRESS, `completedAt` null | D-7 |
| AC-M04-07 | `PUT` card status while milestones exist -> 409 | D-7, D-16 |
| AC-M04-08 | add and delete a note | US-LRN-05, FR-06 |
| AC-M04-09 | milestone with blank title -> 400 | FR-06 |
| AC-M04-10 | a card without milestones accepts a manual status | D-7 |
| AC-M04-11 | delete a milestone | FR-06 |
| AC-M04-12 | delete card -> 204, then 404, its milestones are gone | US-LRN-02, BR14 |

### Playwright
| Id | Scenario | Proves |
|---|---|---|
| E2E-M04-01 **[smoke]** | Add a learning card; card appears in the grid with status and progress | US-LRN-01, SC-001 |
| E2E-M04-02 | Title validation | BR8 |
| E2E-M04-03 | Expand a card, add a milestone with a target date | US-LRN-03 |
| E2E-M04-04 | Complete a milestone (PUT done=true); real: progress and card status update | US-LRN-04, D-7 |
| E2E-M04-05 | Add and remove a note | US-LRN-05 |
| E2E-M04-06 | Remove a milestone; remove a card with confirm | US-LRN-02 |
| E2E-M04-07 **[real-stack-only]** | Fresh db: `learning-empty-state` guides to add | US-NAV-02 |

Test ids: `learning-add-button learning-card-<id> learning-status-<id> learning-progress-<id> learning-loading learning-expand-<id> learning-delete-<id> learning-form-title learning-form-description learning-form-status learning-form-save learning-form-cancel learning-milestone-title-<id> learning-milestone-target-<id> learning-milestone-add-<id> learning-milestone-<mid> learning-milestone-done-<mid> learning-milestone-delete-<mid> learning-note-text-<id> learning-note-add-<id> learning-note-<nid> learning-note-delete-<nid> learning-empty-state`.

---

## M05 Plans

### Curl (plan times are computed from `date -u` inside the script)
| Id | Case | Proves |
|---|---|---|
| AC-M05-01 | create task + habit + card, then a plan with all three starting in the future -> 201, NOT_STARTED, 3 items with resolved titles; `GET /api/plans/{id}` returns it, unknown id 404; plans in the past are accepted (BR11 only needs end after start) | US-PLN-01, FR-07 |
| AC-M05-02 | plan with no items -> 400 | BR10 |
| AC-M05-03 | `endDateTime` before/equal `startDateTime` -> 400 | BR11 |
| AC-M05-04 | unknown source id -> 400 | FR-07 |
| AC-M05-05 | plan started 2 s ago, ending in 1 h -> IN_PROGRESS, `restSeconds > 0` | US-PLN-08, FR-08, BR12 |
| AC-M05-06 | tick the TASK item -> `doneItems=1`, `completionPercent=33`; `GET /api/tasks/{id}` status DONE | US-PLN-05, US-PLN-09, BR13, D-3 |
| AC-M05-07 | un-tick it -> task back to TODO | D-3 |
| AC-M05-08 | tick the HABIT item -> no habit completion exists | BR13, D-3 |
| AC-M05-09 | tick all items -> COMPLETED, `restSeconds` absent | D-2, BR12 |
| AC-M05-10 | plan whose end passed with open items -> COMPLETED with its percentage | D-2, US-PLN-10 |
| AC-M05-11 | `group=ACTIVE_UPCOMING` and `group=COMPLETED` | US-PLN-10 |
| AC-M05-12 | list ordered by `priorityOrder` then start | US-PLN-04, D-12 |
| AC-M05-13 | `acknowledge-start` sets `startNotified=true`, idempotent | US-PLN-07, D-8 |
| AC-M05-14 | delete the task: its item disappears; a plan left empty is deleted | D-5, BR14 |
| AC-M05-15 | `DELETE /api/plans/{id}` -> 204; the source task still exists | US-PLN-06, BR13 |
| AC-M05-16 | duration longer than the window -> 201 with `durationExceedsWindow=true` | D-9 |

### Playwright
| Id | Scenario | Proves |
|---|---|---|
| E2E-M05-01 **[smoke]** | Open the builder, pick a task, a habit and a card, fill fields, save; mock: POST body has 3 items; real: plan card appears | US-PLN-01..04, SC-002 |
| E2E-M05-02 | Builder validation: no items, end before start, non-positive duration/priority | BR10, BR11 |
| E2E-M05-03 | Duration longer than the window shows `plans-duration-warning-<id>` but saves | D-9 |
| E2E-M05-04 | Active/upcoming and completed groups; the IN_PROGRESS plan shows rest time and 33% | US-PLN-08..10 |
| E2E-M05-05 | Tick an item: PUT sent; real: progress, task state and dashboard follow | US-PLN-05, FR-08 |
| E2E-M05-06 | Rest time text changes over time (mock: set the app clock to 2026-10-15T09:30:00Z with `?clockOffsetMs=` or `window.__qfClock.setNow`, inside the example plan window); real: plan starting about 1 minute ahead goes Not Started -> In Progress and counts down | US-PLN-08, FR-08, SC-004 |
| E2E-M05-07 | Start banner appears once for a started plan with `startNotified=false` (the mock example has it), `acknowledge-start` called once; with notifications off, no banner | US-PLN-07, D-8, SC-008 |
| E2E-M05-08 | Remove a plan with confirm | US-PLN-06 |
| E2E-M05-09 **[real-stack-only]** | Fresh db: `plans-empty-state` guides to create | US-NAV-02 |

Test ids: `plans-create-button plans-group-active plans-group-completed plans-card-<id> plans-status-<id> plans-priority-<id> plans-progress-<id> plans-rest-<id> plans-duration-warning-<id> plans-loading plans-form-title plans-form-duration plans-form-start plans-form-end plans-form-priority plans-pick-tasks plans-pick-habits plans-pick-learning plans-pick-task-<id> plans-pick-habit-<id> plans-pick-learning-<id> plans-form-save plans-form-cancel plans-item-<itemId> plans-item-toggle-<itemId> plans-delete-<id> plans-start-banner plans-highlight-<id> plans-empty-state`.

---

## M06 Dashboard

### Curl
| Id | Case | Proves |
|---|---|---|
| AC-M06-01 | empty db: all zeros, `completionPercent=0`, empty lists | FR-09, D-15 |
| AC-M06-02 | tasks due today / overdue / no due date / one DONE / one archived: `dueToday`, `overdue`, `total`, `completed`, `completionPercent` correct | US-DSH-01, US-DSH-02, US-DSH-04, D-15 |
| AC-M06-03 | two habits, one completed: `habits.completedToday=1`, `active=2` | US-DSH-03 |
| AC-M06-04 | an IN_PROGRESS plan appears in `plans.items` with `restSeconds` and progress | US-DSH-05 |
| AC-M06-05 | ticking a plan task item raises `doneItems` and `tasks.completed` | FR-08, FR-09, SC-005 |
| AC-M06-06 | learning counts and `recentMilestones` after completing a milestone | US-DSH-06 |
| AC-M06-07 | every dashboard number equals the count from the list endpoints | FR-09, SC-005 |

### Playwright
| Id | Scenario | Proves |
|---|---|---|
| E2E-M06-01 **[smoke]** | Dashboard shows greeting, four metric cards, today's tasks, overdue tasks, habit checklist, active plans with rest time, learning snapshot | US-DSH-01..06, FR-09 |
| E2E-M06-02 | Completion percent equals the numbers shown (mock data); real: equals the tasks page | US-DSH-04, SC-005 |
| E2E-M06-03 | Quick-add buttons open the matching dialogs and refresh the dashboard | PRD section 8 |
| E2E-M06-04 | Habit toggle and plan item toggle send the right requests; dashboard reloads | US-DSH-03, US-DSH-05 |
| E2E-M06-05 | Rest time of the active plan changes with the clock override (mock: clock set to 2026-10-15T09:30:00Z) | US-DSH-05, SC-004 |

Test ids: `dashboard-greeting dashboard-loading dashboard-metric-tasks dashboard-metric-habits dashboard-metric-plans dashboard-metric-learning dashboard-completion-percent dashboard-today-tasks dashboard-task-<id> dashboard-task-complete-<id> dashboard-overdue-tasks dashboard-today-empty dashboard-overdue-empty dashboard-habits dashboard-habit-<id> dashboard-habit-toggle-<id> dashboard-habits-empty dashboard-plans dashboard-plan-<id> dashboard-plan-progress-<id> dashboard-plan-rest-<id> dashboard-plan-item-toggle-<itemId> dashboard-plans-empty dashboard-learning dashboard-recent-milestone-<id> dashboard-quick-add-task dashboard-quick-add-habit dashboard-quick-add-learning dashboard-quick-add-plan`.

---

## M07 Hardening

### Curl
| Id | Case | Proves |
|---|---|---|
| AC-M07-01 | the full journey over HTTP: task create+complete, habit create+complete, card + 2 milestones completed, plan from the three, all items ticked -> COMPLETED 100%, dashboard counts | PRD section 11 |
| AC-M07-02 | limits: task title 200/201, description 2000/2001, habit name 150/151 | BR1, BR2, BR6 |
| AC-M07-03 | deleting each source type removes its plan items | BR14, D-5 |
| AC-M07-04 | unknown route and malformed JSON return the `ApiError` body | D-16 |
| AC-M07-05 | `GET /api/dashboard` answers in under 500 ms | NFR-01, SC-003 |

### Playwright (real stack)
| Id | Scenario | Proves |
|---|---|---|
| E2E-M07-01 | **Full journey** (`loops/frontend-dev/outputs/e2e/M07-journey.md`): create task, complete it, create habit, complete it, add a learning card with 2 milestones and complete them, build a plan from the three starting about 1 minute ahead, see the start banner and rest time, tick all items, see the plan reach 100% and move to history, check the dashboard at each step; no console errors, no 5xx | PRD section 11, SC-004..006, SC-008 |
| E2E-M07-02 **[smoke]** | Regression of E2E-M01-01, E2E-M02-01, E2E-M03-01, E2E-M04-01, E2E-M05-01, E2E-M06-01 in order | all |
| E2E-M07-03 **[real-stack-only]** | Each page shows an error state with `<page>-error-retry` when the backend is down; retry recovers | NFR-03 |
| E2E-M07-04 | Keyboard-only navigation through the six pages and the add dialogs; focus visible; `aria-label`s present | usability |

Test ids: `<page>-error-retry` for dashboard, tasks, habits, learning, plans, settings; plus all ids above.

---

## Traceability (every user story is proved)

| Story | Curl | Playwright |
|---|---|---|
| US-NAV-01 | | E2E-M01-01 |
| US-NAV-02 | | E2E-M01-02, M02-08, M03-07, M04-07, M05-09 |
| US-TSK-01 | AC-M02-01..03 | E2E-M02-01, M02-02 |
| US-TSK-02 | AC-M02-05 | E2E-M02-03 |
| US-TSK-03 | AC-M02-06 | E2E-M02-04 |
| US-TSK-04 | AC-M02-07..09 | E2E-M02-05 |
| US-TSK-05 | AC-M02-11 | E2E-M02-06 |
| US-TSK-06 | AC-M02-10 | E2E-M02-06 |
| US-TSK-07 | AC-M02-12 | E2E-M02-07 |
| US-HAB-01/02 | AC-M03-01..03 | E2E-M03-01..03 |
| US-HAB-03/04 | AC-M03-04..10 | E2E-M03-04, M03-05 |
| US-HAB-05 | AC-M03-11..12 | E2E-M03-06 |
| US-LRN-01/02 | AC-M04-01, 02, 12 | E2E-M04-01, 02, 06 |
| US-LRN-03/04 | AC-M04-03..07 | E2E-M04-03, 04 |
| US-LRN-05 | AC-M04-08 | E2E-M04-05 |
| US-PLN-01..04 | AC-M05-01..04, 12 | E2E-M05-01..03 |
| US-PLN-05, 09 | AC-M05-06..09 | E2E-M05-05 |
| US-PLN-06 | AC-M05-15 | E2E-M05-08 |
| US-PLN-07 | AC-M05-13 | E2E-M05-07 |
| US-PLN-08 | AC-M05-05 | E2E-M05-04, M05-06 |
| US-PLN-10 | AC-M05-10, 11 | E2E-M05-04 |
| US-DSH-01..06 | AC-M06-01..07 | E2E-M06-01..05 |
| US-SET-01/02 | AC-M01-01..05 | E2E-M01-03, M01-04 |
| Whole journey | AC-M07-01 | E2E-M07-01 |
