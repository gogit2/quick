# Research: QuickFlow

Decisions taken while planning. Format: decision, rationale, alternatives. Spec unknowns were all resolved in `requirements/DECISIONS.md` (D-1..D-14); this file covers technical choices.

## R1 Plan status: computed, never stored
- **Decision**: a plan's status and rest time are computed on every read: `COMPLETED` if all items are done or `now >= end`; else `IN_PROGRESS` if `now >= start`; else `NOT_STARTED`. `restSeconds = max(0, end - now)` only while IN_PROGRESS, otherwise null.
- **Rationale**: NFR-02 asks for consistent status after the app is closed and reopened; with no stored status there is nothing to drift, and no scheduler is needed. One pure function `PlanStatusCalculator` is trivially unit-testable with a fixed `Clock`.
- **Alternatives**: a scheduled job that writes status (can miss transitions while the server is down, needs a scheduler and tests with real time).

## R2 Time: one `Clock` bean plus user-zone helper
- **Decision**: BE injects `java.time.Clock` (system UTC in production, `Clock.fixed`/mutable in tests). A `UserTime` service combines the clock with the zone saved in Settings to answer `today()`, `weekStart()` and `isOverdue(date)`. Instants are stored and sent in UTC; due dates and completion dates are calendar dates.
- **FE**: `ClockService` (`now()` signal ticking every second, used for rest time) with a test override: `?clockOffsetMs=` query parameter or `window.__qfClock.set(iso)`; a Playwright MCP session can call `browser_evaluate` to set it. Real-time scenarios use plans starting at now + ~1 minute.
- **Alternatives**: Playwright's `page.clock` (not exposed by Playwright MCP 0.0.40); server-side time offset endpoint (adds an API only for tests).

## R3 Data access: Spring Data JPA, ids as `BIGINT` identity
- **Decision**: JPA entities per aggregate (`Task`, `Habit`+`HabitCompletion`, `LearningCard`+milestones+notes, `Plan`+`PlanItem`, `AppSettings`), Flyway migrations V1..V5 (one per milestone), `ddl-auto=validate`, H2 in file mode (`jdbc:h2:file:<qf.db.path>`), portable SQL (no H2-only functions).
- **Rationale**: simplest for a Haiku loop; matches the constitution. Unique constraint `(habit_id, completion_date)` enforces BR7 at the database too.
- **Alternatives**: JdbcTemplate (more boilerplate), Postgres (needs Docker, rejected in planning).

## R4 Polymorphic plan items without a foreign key
- **Decision**: `plan_item(source_type, source_id)` has no FK (polymorphic). Deleting a task, habit or card runs `PlanItemCleanup` in the same transaction: delete matching plan items, then delete plans left empty (D-5). Archive does nothing to items.
- **Rationale**: keeps the schema portable and BR14 explicit and testable.

## R5 Side effects of ticking a plan item (D-3)
- **Decision**: `PlanItemService.setDone` flips the item flag; for `TASK` items it also calls `TaskService.complete(id)` (done=true) or `TaskService.reopen(id)` (sets TODO). No call for HABIT and LEARNING_RESOURCE. All in one transaction; the response is the whole plan with recomputed progress.
- **Rationale**: the only completion logic is owned by the task service; the plan stores only its own flag (NFR-04).

## R6 Habit periods and streaks
- **Decision**: DAILY period = the day; WEEKLY period = Monday-Sunday in the user zone. `doneForCurrentPeriod` = a completion exists in the current period. Streak = number of consecutive completed periods ending at the current one, or ending at the previous one if the current is not done yet. Completion for a future date is a 400; duplicate date, or a second completion in the same week for a WEEKLY habit, is a 409.
- **Alternatives**: weekday-specific weekly habits (extra field, rejected D-6).

## R7 Learning status derivation
- **Decision**: `LearningStatusCalculator`: with milestones: none done = NOT_STARTED, some = IN_PROGRESS, all = COMPLETED. Recomputed and stored on every milestone change, so list queries stay simple. With no milestones the stored status is the manual value; `PUT` with status while milestones exist is 409.

## R8 Dashboard: one aggregate endpoint
- **Decision**: `GET /api/dashboard` computes everything server-side in one transaction using the same services/calculators as the other pages, so SC-005 (numbers match the data) holds by construction; tests compare it with the list endpoints. Task completion percent = completed / total non-archived tasks, round-half-up, 0 when none (D-15).
- **Alternatives**: the frontend aggregating from list endpoints (duplicates rules in the client).

## R9 Pagination: none
- **Decision**: all list endpoints return full arrays. A single user's data is small (hundreds of rows) and the PRD sets a 500 ms budget for typical actions.

## R10 Error body
- **Decision**: one `ApiError {timestamp,status,error,message,path,fieldErrors[]}` for 400/404/409 (a `@RestControllerAdvice` maps bean-validation, `NotFoundException`, `ConflictException`, `BadRequestException`). Controllers declare the 400/404/409 responses that the contract lists (annotations or one `OpenApiCustomizer`) so the exported Swagger matches the contract.

## R11 Frontend structure and mocking
- **Decision**: Angular 19 standalone components, lazy-loaded feature routes, Angular Material, signals for state. One typed API service per area. Dev proxy `proxy.conf.js` reads `API_TARGET`. Prism serves `contracts/openapi.yaml` (stateless) for FE self-checks; real state is verified by the verifier.
- **Notification catch-up**: on app start and on every clock tick the FE finds plans with `status != NOT_STARTED` and `startNotified == false`, shows one banner/toast per plan (unless notifications are off in Settings), then calls `POST /api/plans/{id}/acknowledge-start`.

## R12 Tooling versions (project-local, pinned in `tools/`)
- Playwright MCP 0.0.40, Prism 5.12.0 (newer Prism pulls ESM-only faker that does not run on Node 18), Chromium 1191 in `tools/.browsers`.
