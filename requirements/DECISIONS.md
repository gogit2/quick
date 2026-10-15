# Decisions

Answers given by the user during planning (`speckit-clarify`, 2026-10-15). **These override the PRD where they conflict.**
Loops never ask questions: if something is not settled here, they record an assumption in `loops/_shared/assumptions.md`.

Format: `D-<n> (Qx): question -> decision`.

## Spec 001-quickflow

- **D-1 (Q1)**: Does the app need sign-in? -> **No.** One implicit user, no login, no authentication. The app opens straight to the start page. "Profile" in Settings is stored information only.
- **D-2 (Q2)**: When is a plan Completed? -> A plan is **Not Started** before its start; **In Progress** from its start while items remain and the end has not passed; **Completed** as soon as every item is done, or when the end time passes (whichever comes first). A plan that ends with open items is Completed and keeps its final percentage (history shows e.g. 60%). Status is always computed from stored times, item completion and the clock. If every item is already done before the start, the plan is Completed.
- **D-3 (Q3)**: What does ticking a plan item do to its source (BR13)? -> **Task items**: ticking sets the task to Done; un-ticking sets it back to Todo. **Habit and learning items**: only the plan item's own done flag changes; no habit completion is recorded and the card and its milestones are untouched.
- **D-4 (Q4)**: Which time zone defines today / overdue / weeks? -> The **user time zone saved in Settings** (default: the browser's zone on first use). Plan start and end are exact moments stored in UTC. Task due dates, habit completion dates and milestone target dates are plain calendar dates. Weeks run Monday to Sunday.
- **D-5 (Q5)**: What happens to plans when a source is archived or deleted (BR14)? -> An **archived** task stays in its plans and can still be ticked; archived tasks are not counted in "due today" or "overdue". **Deleting** a task, habit or learning card removes its items from every plan and progress is recomputed over the remaining items; a plan left with no items is deleted too, so BR10 always holds. Deleting a habit also deletes its completions; deleting a card deletes its milestones and notes.
- **D-6 (Q6)**: What does "done" and "streak" mean for a weekly habit? -> **One completion per calendar week** (Mon-Sun, user time zone) makes the habit done for the whole week; it is listed under today's habits only until then. A daily habit is done for the day. **Streak** = number of consecutive days (daily) or weeks (weekly) completed, ending at the current period (the current period not yet done does not break a streak that is complete up to the previous period). A habit can be completed for today or any earlier date, never a future date. Completing a weekly habit again in the same week is a duplicate (BR7 applies per habit and date; the week rule is checked on top).
- **D-7 (Q7)**: Is the learning card status manual or derived? -> **Derived from milestones** when the card has at least one: Not Started when none are done, In Progress when some are, Completed when all are; it updates automatically when a milestone is added, completed or removed. A card with **no milestones** has its status **set by hand**.
- **D-8 (Q8)**: How is the user notified at a plan's start? -> **In-app only**: a banner/toast appears and the plan is highlighted at its start time while the app is open. If the plan started while the app was closed, the same notice is shown **once** the next time the app opens. The Settings notifications on/off preference turns the banner off. No browser (system) notifications.
- **D-9 (Q9)**: How does estimated duration relate to start/end? -> Estimated duration is **required**, a **positive whole number of minutes**. It is informational: a plan longer than its window is still saved, and the form and plan card show a **warning**. Rest time always uses the end time.
- **D-10 (Q10)**: Where are Settings stored and what fields exist? -> **On the server**, one settings record with **5 fields**: display name, email, time zone (default: browser's), notifications on/off (default on), default start page (Dashboard, Tasks, Habits, Learning Resources, Todo Plans; default Dashboard). The app opens on the chosen start page.

## Defaults taken during planning (not asked; change here to override)

- **D-11**: New tasks default to status Todo and priority Medium. Lists are sorted by creation date, newest first, unless the user sorts. Plans are listed by priority order (1 = first), then start time.
- **D-12**: Plan `PriorityOrder` is a positive integer; equal values are allowed (ties are broken by start time).
- **D-13**: A "due-date filter" on tasks offers: due today, overdue, due this week (Mon-Sun), no due date, and an explicit date range.
- **D-14**: Task search matches the title only, case-insensitive, partial match.

## Defaults taken in speckit-plan (not asked; change here to override)

- **D-15**: Task completion percentage = completed non-archived tasks / all non-archived tasks, round half up, 0 when there are none. "Today's tasks" = non-archived tasks due today (any status); "completed today" = DONE tasks last updated today.
- **D-16**: API style: ids are integers; lists are not paginated; errors use one `ApiError` body (400 validation with fieldErrors, 404, 409). A duplicate habit completion is 409; a future date or inactive habit is 400. Updating a learning card's status while it has milestones is 409.
- **D-17**: A plan has no stored status; it is computed from the clock, times and items. Plans cannot be edited after creation in the MVP (create, mark items, acknowledge start, remove).
- **D-18**: Plan item progress is rounded half up; a plan whose items are all done before its start is Completed.

## Spec 002-visual-polish (frontend enhancement, 2026-10-15)

- **D-19**: Scope of spec 002 -> **frontend-only visual polish**, no API or contract change: spec 002 reuses `specs/001-quickflow/contracts/openapi.yaml` unchanged; no BE tasks. All existing `data-testid`s stay unchanged so every earlier scenario stays valid.
- **D-20**: Design direction -> **calm and modern**: soft neutral surfaces, one teal/indigo accent, generous spacing, rounded cards with light shadows.
- **D-21**: Theming -> **light, dark and follow-system**: a toolbar toggle (light / dark / system), choice saved in the browser (localStorage), default = system preference. Both themes must meet WCAG AA contrast.
- **D-22**: Typeface -> **Inter**, self-hosted (bundled from the `@fontsource-variable/inter` npm package, Latin subset only); no external font requests.
- **D-23**: Out of scope for 002 -> editing a plan after creation, drag-and-drop plan priority, charts/calendar views, tags (stay in the backlog).
- **D-24**: Milestones continue the numbering: M08 Design foundation, M09 Shell and navigation, M10 Pages, M11 Polish and regression.

## M11 blocked-milestone decisions (spec 002, 2026-10-15, user)

- **D-25**: The initial-bundle **raw warning budget is raised from 600 kB to 650 kB** (`frontend/angular.json`, `maximumWarning`). Reason: SC-V04 is defined on the estimated transfer size (150 kB against 500 kB, met); the 600 kB raw value had itself been raised from Angular's 500 kB default in spec 001 and was never a requirement. The error budget (1 MB) is unchanged. T134 is done when `ng build` prints no warnings under the 650 kB budget.
- **D-26**: The four spec-001 scenarios that cannot pass against the stateless Prism mock (**E2E-M01-02, E2E-M02-05, E2E-M05-02, E2E-M05-07**) are **ignored for the M11 regression**: the loop records them in `regression-m01-m05.json` with `accepted_known_failure: true` and the reason, and E2E-M11-07 counts as passed when the only failures are exactly these four. Consequence (accepted by the user): nobody re-checks those four against the restyled UI in M11; the earlier verifier runs of M01, M02 and M05 passed them on the real stack before the restyle. Scenario texts are not edited. The part of E2E-M11-02 that needs the first paint (the MCP cannot observe it) is recorded as checked by code review, as E2E-M08-04 was in M08.
