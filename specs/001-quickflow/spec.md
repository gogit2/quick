# Feature Specification: QuickFlow

**Feature Branch**: `001-quickflow` (no git branch is used; the spec folder is `specs/001-quickflow`)

**Created**: 2026-10-15

**Status**: Draft

**Input**: User description: "QuickFlow: one spec for the WHOLE PRD in requirements/PRD.md (tasks, habits, learning resources with milestones and notes, time-boxed Todo Plans with live rest time and progress, dashboard, settings, six pages with persistent navigation)."

**Traceability**: user stories are `US-<AREA>-<NN>` (AREA = NAV, TSK, HAB, LRN, PLN, DSH, SET, numbered in PRD section 5 order). Functional requirements keep the PRD ids `FR-01..FR-09`; business rules keep `BR1..BR14` (PRD section 7). Sub-items are written `FR-02.a`. Open points are `[NEEDS CLARIFICATION: ...]` markers and are resolved by `speckit-clarify` into `requirements/DECISIONS.md`.

## User Scenarios & Testing *(mandatory)*

### User Story NAV - One workspace with six pages (Priority: P1)

A person opens QuickFlow and finds six pages (Dashboard, Tasks, Habits, Learning Resources, Todo Plans, Settings) reachable at any time from persistent navigation.

**Why this priority**: it is the frame every other story lives in, and it is the first thing a milestone can be demonstrated through.

**Independent Test**: open the app, visit each of the six pages from the navigation, reload on each, and confirm the page, the active navigation item and an empty-state message.

**Acceptance Scenarios**:

1. **Given** any page, **When** the user selects a navigation item, **Then** that page opens and its item is highlighted. (US-NAV-01)
2. **Given** a list page (Tasks, Habits, Learning Resources, Todo Plans) with no items, **When** it opens, **Then** an empty state explains what the page is for and offers the "add" action. (US-NAV-02)
3. **Given** the user reloads the browser on any page, **When** it finishes loading, **Then** the same page is shown. (US-NAV-01)

User stories:
- **US-NAV-01**: As a user, I want persistent navigation to all six pages, each independently reachable.
- **US-NAV-02**: As a user, I want empty states that guide me to the relevant "add" action.

---

### User Story TSK - Manage tasks (Priority: P1)

A person captures work as tasks, edits and completes them, archives or deletes the ones they no longer want, and finds them again by search, filters and sorting, including seeing what is overdue.

**Why this priority**: tasks are the core of the product and the first content plans and the dashboard are built from.

**Independent Test**: create a task, edit it, filter and search, complete it, archive and restore it, delete another one.

**Acceptance Scenarios**:

1. **Given** the Tasks page, **When** the user adds a task with a title, **Then** it appears with status Todo and default priority. (US-TSK-01)
2. **Given** an existing task, **When** the user changes its title, description, status, priority or due date, **Then** the change is saved and the updated time changes. (US-TSK-02)
3. **Given** a task, **When** the user marks it completed, **Then** its status is Done. (US-TSK-03)
4. **Given** a task, **When** the user archives it, **Then** it disappears from the default list and can be restored; deleting it removes it for good. (US-TSK-04)
5. **Given** tasks of mixed status and priority, **When** the user filters by status and/or priority, searches by title, filters by due date or sorts by due date or creation date, **Then** only matching tasks are shown in the chosen order. (US-TSK-05, US-TSK-06)
6. **Given** a task whose due date is before today and that is not Done, **When** the list or dashboard is shown, **Then** it is marked overdue. (US-TSK-07)
7. **Given** a task with an empty title or a title over 200 characters, **When** the user saves it, **Then** it is rejected with a clear message. (BR1)

User stories:
- **US-TSK-01**: As a user, I want to create a task.
- **US-TSK-02**: As a user, I want to edit a task.
- **US-TSK-03**: As a user, I want to mark a task as completed.
- **US-TSK-04**: As a user, I want to delete or archive a task.
- **US-TSK-05**: As a user, I want to filter tasks by status and priority.
- **US-TSK-06**: As a user, I want to search my tasks.
- **US-TSK-07**: As a user, I want to see overdue tasks.

---

### User Story HAB - Track recurring habits (Priority: P2)

A person defines daily or weekly habits, checks them off for today, sees how they are doing, and removes habits they no longer track.

**Why this priority**: second content type; independent of tasks, and needed by plans and the dashboard.

**Independent Test**: create a daily and a weekly habit, complete one for today, try to complete it again, deactivate one, delete one.

**Acceptance Scenarios**:

1. **Given** the Habits page, **When** the user adds a habit with a name and chooses Daily or Weekly, **Then** it appears as an active habit with that frequency label. (US-HAB-01, US-HAB-02)
2. **Given** an active habit, **When** the user marks it complete for today, **Then** today is recorded and its progress and streak indicator update. (US-HAB-03, US-HAB-04)
3. **Given** a habit already completed for a date, **When** the user completes it again for that date, **Then** no duplicate is created. (BR7)
4. **Given** a habit, **When** the user deactivates it, **Then** it stops counting as active; **When** the user removes it, **Then** it and its completions are no longer returned. (US-HAB-05, BR14)

User stories:
- **US-HAB-01**: As a user, I want to create a recurring habit.
- **US-HAB-02**: As a user, I want to choose daily or weekly recurrence.
- **US-HAB-03**: As a user, I want to mark a habit complete for today.
- **US-HAB-04**: As a user, I want to see my current habit completion progress.
- **US-HAB-05**: As a user, I want to remove a habit I no longer track.

---

### User Story LRN - Track learning resources (Priority: P2)

A person keeps learning cards (a course, a book, a topic), breaks each into milestones, ticks them off and keeps free-form notes on the card.

**Why this priority**: third content type, needed by plans and the dashboard's learning snapshot.

**Independent Test**: add a card, add three milestones, complete one, add and remove a note, remove the card.

**Acceptance Scenarios**:

1. **Given** the Learning Resources page, **When** the user adds a card with a title and optional description or source, **Then** it appears in the card grid with status Not Started. (US-LRN-01, BR8)
2. **Given** a card, **When** the user expands it, **Then** its milestones and notes are shown. (PRD section 8)
3. **Given** a card, **When** the user adds a milestone (title, optional target date), **Then** it belongs to that card only. (US-LRN-03, BR9)
4. **Given** a milestone, **When** the user marks it complete, **Then** it shows as done. (US-LRN-04)
5. **Given** a card, **When** the user adds a note, **Then** the note is stored with its text and time; **When** the user removes it, **Then** it is gone. (US-LRN-05)
6. **Given** a card, **When** the user removes it, **Then** the card and its milestones and notes are no longer returned. (US-LRN-02, BR14)

User stories:
- **US-LRN-01**: As a user, I want to add a learning resource as a card (e.g. a course, book, or topic).
- **US-LRN-02**: As a user, I want to remove a learning card I no longer need.
- **US-LRN-03**: As a user, I want to add milestones under a learning card to break it into steps.
- **US-LRN-04**: As a user, I want to mark milestones complete.
- **US-LRN-05**: As a user, I want to attach free-form notes to a learning card.

---

### User Story PLN - Time-boxed Todo Plans (Priority: P1)

A person assembles existing tasks, habits and learning resources into a plan with an estimated duration, a start and end date-time and a priority order. QuickFlow tracks the plan's progress, notifies the user when it starts, shows the live remaining ("rest") time while it runs, and keeps a history of past plans and how much of each was completed.

**Why this priority**: it is the product's differentiator and ties all other content together.

**Independent Test**: create a plan from one task, one habit and one learning card that starts about a minute from now; see the start notification, the live rest time and progress; tick items; see the plan finish and appear in history.

**Acceptance Scenarios**:

1. **Given** existing tasks, habits and learning cards, **When** the user opens the plan builder and selects at least one of them and sets a title, estimated duration, start, end and priority order, **Then** the plan is created in Not Started. (US-PLN-01 to US-PLN-04, BR10, BR11)
2. **Given** a plan with no items or an end not after its start, **When** the user saves it, **Then** it is rejected with a clear message. (BR10, BR11)
3. **Given** two plans, **When** their priority orders differ, **Then** the plans are listed in that order within their group. (US-PLN-04)
4. **Given** a plan whose start time arrives, **When** the app is open, **Then** the user is notified and the plan is highlighted and becomes In Progress. (US-PLN-07, FR-08)
5. **Given** an In Progress plan, **When** time passes, **Then** the rest time counts down and refreshes at least once a minute until the end; outside the start-end window no rest-time display is active. (US-PLN-08, BR12)
6. **Given** a plan with N items, **When** the user marks an item done or not done, **Then** the plan's completion percentage is done items divided by N, immediately, and the dashboard reflects it. (US-PLN-05, US-PLN-09, FR-08)
7. **Given** a task-type plan item, **When** it is marked done, **Then** the underlying task is completed; for other item types the source is not changed beyond its own completion action. (BR13) [NEEDS CLARIFICATION: Q3 - behaviour of BR13 for habit and learning items, and when an item is marked not done again]
8. **Given** the Todo Plans page, **When** it opens, **Then** plans are grouped into active/upcoming and completed, each with percent complete and rest time, and past plans show how much was completed. (US-PLN-10)
9. **Given** a plan, **When** the user removes it, **Then** it is no longer returned; the items' sources are untouched. (US-PLN-06, BR14)
10. **Given** the app is closed and reopened, **When** it loads, **Then** every plan's status is consistent with the stored times and item completion. (FR-08, reliability)

User stories:
- **US-PLN-01**: As a user, I want to create a plan by selecting existing tasks, habits, and/or learning resources.
- **US-PLN-02**: As a user, I want to set an estimated time for a plan.
- **US-PLN-03**: As a user, I want to set a start date/time and an end date/time for a plan.
- **US-PLN-04**: As a user, I want to set a priority order for a plan relative to other plans.
- **US-PLN-05**: As a user, I want to mark individual items inside a plan as done, independent of the plan's overall status.
- **US-PLN-06**: As a user, I want to remove a plan.
- **US-PLN-07**: As a user, I want to be notified when a plan's scheduled start time arrives.
- **US-PLN-08**: As a user, I want to see a live "rest time" indicator for an in-progress plan, updating continuously from start until end.
- **US-PLN-09**: As a user, I want to see a plan's completion progress, based on how many of its items are marked done.
- **US-PLN-10**: As a user, I want a history of past plans and how much of each was completed.

---

### User Story DSH - Dashboard (Priority: P1)

On opening the app the person sees today at a glance: today's tasks, overdue tasks, today's habits, a completion percentage, active plans with live rest time and progress, and a snapshot of learning progress, with quick-add actions.

**Why this priority**: it is the landing page and proves that the other features add up consistently.

**Independent Test**: with known data (tasks due today and overdue, habits, a running plan, a learning card with milestones), the dashboard numbers equal the data on the other pages; after ticking a plan item or completing a task they update.

**Acceptance Scenarios**:

1. **Given** the app opens, **When** the dashboard loads, **Then** a greeting header, metric cards (tasks, habits, plans, learning), today's tasks, today's habit checklist, active plans and quick-add actions are shown. (US-DSH-01, PRD section 8)
2. **Given** overdue tasks exist, **When** the dashboard loads, **Then** they are listed separately. (US-DSH-02)
3. **Given** active habits, **When** the dashboard loads, **Then** today's habits are listed with completion state. (US-DSH-03)
4. **Given** tasks, **When** the dashboard loads, **Then** the task completion percentage matches the underlying data. (US-DSH-04, FR-09)
5. **Given** an In Progress plan, **When** the dashboard is open, **Then** it shows live rest time and progress and updates when an item is ticked. (US-DSH-05)
6. **Given** learning cards, **When** the dashboard loads, **Then** a snapshot shows cards in progress and recently completed milestones. (US-DSH-06)

User stories:
- **US-DSH-01**: As a user, I want to see today's tasks immediately after opening the app.
- **US-DSH-02**: As a user, I want to see overdue tasks.
- **US-DSH-03**: As a user, I want to see today's habits.
- **US-DSH-04**: As a user, I want to see a simple completion percentage.
- **US-DSH-05**: As a user, I want to see active/in-progress plans and their rest time at a glance.
- **US-DSH-06**: As a user, I want to see a snapshot of learning progress.

---

### User Story SET - Settings (Priority: P3)

A person views and edits their profile information and application preferences (such as notification behaviour and default view).

**Why this priority**: required as a page by the PRD, but not on the critical path of any other feature.

**Independent Test**: change a preference, reload, and confirm it is kept and applied.

**Acceptance Scenarios**:

1. **Given** the Settings page, **When** it opens, **Then** profile information and application preferences are shown. (US-SET-01)
2. **Given** a changed preference (e.g. default view or notification behaviour), **When** the user saves and reopens the app, **Then** it is kept and applied. (US-SET-02) [NEEDS CLARIFICATION: Q10 - where settings are stored and which fields exist]

User stories:
- **US-SET-01**: As a user, I want to see and edit my profile information.
- **US-SET-02**: As a user, I want to set application preferences (e.g. notification behaviour, default view).

*(US-SET-01/02 are derived from PRD section 8 "Settings Page"; PRD section 5 has no Settings stories.)*

---

### Edge Cases

- Task title exactly 200 characters, description exactly 2,000 characters; one more is rejected (BR1, BR2).
- Invalid status or priority values are rejected (BR3).
- Archived tasks are excluded from the default list, filters still find them when asked, and they do not count as "today" or "overdue" on the dashboard unless decided otherwise. [NEEDS CLARIFICATION: Q5 - archived or deleted sources inside a plan and on the dashboard]
- A source (task, habit, learning card) is deleted or archived while a plan still contains it (BR14). [NEEDS CLARIFICATION: Q5]
- A habit is completed twice for the same date, or for a future or past date (BR7). [NEEDS CLARIFICATION: Q6 - what "today", completion and streak mean for weekly habits]
- A plan whose end time passes before all items are done, or whose items are all done before its start. [NEEDS CLARIFICATION: Q2 - when a plan counts as Completed]
- A plan whose estimated duration is longer or shorter than its start-end window. [NEEDS CLARIFICATION: Q9]
- The app is closed during a plan's start time: the start notification must not be lost on the next open. [NEEDS CLARIFICATION: Q8 - notification mechanism]
- Day boundaries ("today", "overdue", weekly periods) when the user changes time zone. [NEEDS CLARIFICATION: Q4 - time zone]
- Card status moves to Completed while a milestone is still open, or all milestones are done but the status says In Progress. [NEEDS CLARIFICATION: Q7]
- Searching with an empty string, text with special characters, or no results shows the whole list or a "no results" state rather than an error.
- Two plans overlapping in time, or with the same priority order.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-01 Task management**: the system MUST store tasks with Id, Title, Description, Status (Todo, In Progress, Done), Priority (Low, Medium, High), DueDate, CreatedAt, UpdatedAt and IsArchived, and let the user create, read, update, archive, restore and delete them. (US-TSK-01..04)
- **FR-02 Task search and filtering**: the system MUST support free-text search by title, status filter, priority filter, due-date filter, and sorting by due date or creation date. (US-TSK-05, US-TSK-06, US-TSK-07)
- **FR-03 Habit management**: the system MUST store habits with Id, Name, Description, Frequency (Daily, Weekly), CreatedAt and IsActive, and let the user create, update, deactivate and delete them. (US-HAB-01, 02, 05)
- **FR-04 Habit completion**: the system MUST record completion events (Id, HabitId, CompletionDate), let the user mark a habit complete for a date, and prevent duplicate completions for the same habit and date. (US-HAB-03, US-HAB-04)
- **FR-05 Learning resources**: the system MUST store learning cards with Id, Title, Description/source, CreatedAt and Status (Not Started, In Progress, Completed), and let the user add and remove them. (US-LRN-01, 02)
- **FR-06 Learning milestones and notes**: each card MUST support milestones (title, IsDone, optional target date) and free-form notes (text, timestamp); the user MUST be able to add, complete and remove milestones and add and remove notes. (US-LRN-03..05)
- **FR-07 Plan composition**: the system MUST store plans with Id, Title, plan items (reference to an existing task, habit or learning resource by id and type, plus its own IsDone), EstimatedDuration, StartDateTime, EndDateTime, PriorityOrder, Status (Not Started, In Progress, Completed) and CreatedAt; the user MUST be able to create a plan from one or more existing items, set duration, start, end and priority order, mark items done or not done, and remove a plan. (US-PLN-01..06)
- **FR-08 Plan progress and rest time**: the system MUST notify the user when a plan's start is reached; from start to end continuously compute and display the remaining rest time (refreshed at least once a minute); compute completion percentage as items done divided by total items; update a plan's status automatically from the current time and item completion; and reflect item-level changes immediately in the plan's progress and in the dashboard. (US-PLN-07..10) [NEEDS CLARIFICATION: Q2, Q3, Q8]
- **FR-09 Dashboard**: the dashboard MUST show tasks due today, overdue tasks, tasks completed today, active habits and habits completed today, task completion percentage, active plans with live rest time and progress, and a learning snapshot (cards in progress, milestones completed recently). (US-DSH-01..06)
- **FR-10 Navigation and shell**: the app MUST provide persistent navigation to the six pages, each independently reachable, empty states that point to the add action, and add and remove actions directly on every list-style page. (US-NAV-01, 02)
- **FR-11 Settings**: the app MUST provide a Settings page for profile information and application preferences. (US-SET-01, 02)
- **FR-12 Users and access**: QuickFlow serves one individual. [NEEDS CLARIFICATION: Q1 - is there sign-in, or is the app a single implicit user with no authentication?]

### Business Rules

- **BR1**: a task title is required, maximum 200 characters.
- **BR2**: a task description is optional, maximum 2,000 characters.
- **BR3**: a task cannot have an invalid status or priority.
- **BR4**: an archived task is excluded from the default task list.
- **BR5**: a completed task has status Done.
- **BR6**: a habit name is required, maximum 150 characters.
- **BR7**: a habit has at most one completion record for a given date.
- **BR8**: a learning card title is required.
- **BR9**: a milestone belongs to exactly one learning card.
- **BR10**: a plan references at least one existing task, habit or learning-resource item.
- **BR11**: a plan's end must be after its start.
- **BR12**: a plan's rest-time display is active only between its start and end.
- **BR13**: marking a plan item done does not change the original task, habit or learning card outside the plan, except where the item directly represents that entity's own completion action (completing a task-type item completes the task). [NEEDS CLARIFICATION: Q3]
- **BR14**: deleted resources are never returned by normal queries.

### Non-Functional Requirements

- **NFR-01 Performance**: typical in-app actions (create, update, delete, filter) feel instant, under 500 ms; live rest-time indicators refresh at least once a minute while a plan is in progress.
- **NFR-02 Reliability**: state persists between sessions; plan status transitions are computed consistently from stored start and end times and item completion, including after the app is closed and reopened.
- **NFR-03 Usability**: every list-style page supports adding and removing items directly from that page; empty states guide the user to the add action.
- **NFR-04 Maintainability**: domain and business rules are testable and separated from presentation; completion-state logic is not duplicated between a plan item and its source beyond FR-08 and BR13.
- **NFR-05 Testing**: unit tests cover business rules, rest-time computation, plan status lifecycle and completion roll-up, and dashboard metrics versus underlying data; end-to-end coverage includes create task, complete task, create habit, complete habit, add learning card with milestones, build a plan from existing items, mark plan items done, verify plan achievement and dashboard update.

### Key Entities

- **Task**: a unit of work with title, description, status, priority, due date, created/updated time and archived flag.
- **Habit**: a recurring practice with name, description, frequency (daily/weekly), created time and active flag. Has many **Habit Completions** (habit, completion date), unique per habit and date.
- **Learning Card**: a resource being studied, with title, description/source, status and created time. Has many **Learning Milestones** (title, done flag, optional target date) and many **Learning Notes** (text, created time).
- **Plan**: a scheduled, prioritised block of work with title, estimated duration, start, end, priority order, status and created time. Has many **Plan Items**.
- **Plan Item**: a reference to one existing task, habit or learning card (by type and id) with its own done flag, belonging to exactly one plan.
- **Settings**: profile information and application preferences of the user. [NEEDS CLARIFICATION: Q10]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: a user can create a task, habit or learning card in under 30 seconds from the page that lists it.
- **SC-002**: a user can build a plan from existing items, with times and priority, in under 2 minutes.
- **SC-003**: create, update, delete, filter and search actions show their result in under 500 ms.
- **SC-004**: while a plan is in progress, its displayed rest time is never more than 1 minute out of date, and after closing and reopening the app its status matches its stored times and item completion in 100% of cases.
- **SC-005**: after any item, task or habit change, the plan progress and dashboard figures equal the underlying data in 100% of checks.
- **SC-006**: the full end-to-end journey (create and complete a task, create and complete a habit, add a learning card with milestones, build a plan from them, tick its items, see the plan reach 100% and the dashboard update) completes without errors.
- **SC-007**: all six pages are reachable from the navigation on every page, and every list-style page shows a helpful empty state when it has no items.
- **SC-008**: a user is notified of a plan's start within 1 minute of its scheduled start while the app is open.

## Assumptions

- QuickFlow is used by a single individual; team features, real-time chat, external calendar sync, file attachments, social features and advanced AI are out of scope (PRD non-goals).
- Reasonable defaults apply wherever the PRD is silent and no marker is shown: new tasks default to Todo and Medium priority; lists are sorted by creation date, newest first, unless the user sorts; plans are listed by priority order.
- Defaults are subject to `requirements/DECISIONS.md`, which overrides this spec after `speckit-clarify`.
- Future enhancements in PRD section 14 (tags, recurring tasks, analytics, calendar integration, email/push reminders, drag-and-drop reordering, multi-user) are out of scope.

## Open Points for Clarification

The following are resolved with the user in `speckit-clarify` and recorded in `requirements/DECISIONS.md`:

| Id | Topic |
|---|---|
| Q1 | Single user: sign-in or no authentication (FR-12) |
| Q2 | When a plan counts as Completed (all items done, end time passed, or either) (FR-08) |
| Q3 | BR13 for habit and learning items, and behaviour when an item is un-ticked |
| Q4 | Time zone and the meaning of "today" / "overdue" |
| Q5 | Archived or deleted sources inside plans and on the dashboard (BR14) |
| Q6 | Weekly habits: what "today", completion and streak mean |
| Q7 | Learning card status: set by hand or derived from milestones |
| Q8 | Start notification: in-app only, or also browser notification; what if the app was closed |
| Q9 | Estimated duration versus the start-end window |
| Q10 | Settings: stored where, which profile and preference fields |
