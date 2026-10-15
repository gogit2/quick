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
