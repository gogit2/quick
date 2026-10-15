# Implementation Plan: QuickFlow

**Branch**: `001-quickflow` (spec folder only) | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-quickflow/spec.md`, decisions in `requirements/DECISIONS.md` (D-1..D-18, override the PRD).

## Summary

One Spring Boot REST backend and one Angular single-page frontend, built contract-first in seven vertical milestones. Business rules (task and habit validation, habit periods and streaks, learning status derivation, plan status and rest time, completion roll-up, dashboard metrics) live in plain Java classes driven by an injected `Clock`. Plan status is computed on every read, never stored. The contract [contracts/openapi.yaml](contracts/openapi.yaml) (35 operations, 40 schemas, every operation tagged `x-milestone`) is the single source of truth for backend-dev, frontend-dev (via a Prism mock) and the verifier.

## Technical Context

**Language/Version**: Java 17 (language level 17 only), TypeScript 5.x (Angular 19)

**Primary Dependencies**: Spring Boot 3.5.x (web, validation, data-jpa, actuator), Flyway, springdoc-openapi 2.x, H2 (file); Angular 19, Angular Material 19, RxJS

**Storage**: H2 file database via `qf.db.path`; schema only through Flyway (V1..V5); `ddl-auto=validate`

**Testing**: JUnit 5 + Spring Boot test (backend, required), curl suites per milestone, Playwright MCP 0.0.40 scenarios per milestone (frontend); Angular unit tests optional

**Target Platform**: Linux dev machine; browser Chromium 1440x900

**Project Type**: web application (backend/ + frontend/)

**Performance Goals**: typical actions under 500 ms; rest time refreshed at least once a minute (the UI ticks every second)

**Constraints**: Java 17, Node 18.20, no dependency outside the folder, ports 8091 / 8090 / 4300 / 4010

**Scale/Scope**: one user, hundreds of rows, six pages, 35 API operations

## Constitution Check

*GATE: passed before research and again after design.*

| Principle | How the plan complies |
|---|---|
| I Contract-first | `contracts/openapi.yaml` written first; all operations have `x-milestone` and examples (examples in `components/examples`); DTO class names = schema names; loops cannot edit it; Prism mocks it. |
| II Fixed stack | Exactly the stack above; H2 file per instance; Flyway migrations V1..V5 only. |
| III Separation | `backend/` and `frontend/`; rules in `domain` classes (calculators, services), controllers thin; plan item and source logic not duplicated (research R5). |
| IV Tests | JUnit per rule, curl suite per milestone, exported Swagger diff, Playwright scenario per milestone (see Testing strategy). |
| V Controllable time | `Clock` bean + `UserTime`; FE `ClockService` with override; real-time E2E with plans starting at about now + 1 minute. |
| VI test hooks | `data-testid` map per page in `acceptance.md`. |
| VII Autonomous loops | Decisions are in DECISIONS.md; everything else is an assumption entry. |

No violations; the Complexity Tracking section is not needed.

## Architecture

```
Angular SPA (:4300)  --/api proxy-->  Spring Boot (:8090 stable / :8091 self-check)  -->  H2 file db
   features/*                          controller -> service -> repository
   ClockService (override)             domain calculators (pure, Clock-driven)
```

### Backend packages (`com.quickflow`)
```
common/      ApiError, FieldError, GlobalExceptionHandler, NotFoundException, ConflictException, BadRequestException, ClockConfig, UserTime
settings/    SettingsController, SettingsService, AppSettings(+Repository), Settings, SettingsUpdate
task/        TaskController, TaskService, TaskRepository, Task(entity), Task DTOs (TaskCreate, TaskUpdate, Task), TaskQuery
habit/       HabitController, HabitService, HabitPeriods (pure), StreakCalculator (pure), Habit, HabitCompletion, DTOs
learning/    LearningController, LearningService, LearningStatusCalculator (pure), entities, DTOs
plan/        PlanController, PlanService, PlanStatusCalculator (pure), PlanItemCleanup, entities, DTOs
dashboard/   DashboardController, DashboardService, DTOs
```
Entities and DTOs share a package but not a class: DTO classes are named exactly like the contract schemas (`Task`, `TaskCreate`, ...); entities use the suffix `Entity` (`TaskEntity`) so the exported Swagger schema names equal the contract.

### Frontend (`frontend/src/app`)
```
core/        api (one service per area, types mirror the contract), clock.service.ts, settings.store.ts, notification.service.ts, error handling
shared/      confirm dialog, empty-state, page header, rest-time pipe, status chips
layout/      shell with persistent navigation (nav-dashboard .. nav-settings)
features/    dashboard/ tasks/ habits/ learning/ plans/ settings/   (lazy routes)
```
Start page: on app start the shell reads Settings and redirects `/` to `defaultStartPage`. On first start (`configured=false`) it saves the browser time zone silently.

### Clock and time design
- **Backend**: `Clock` bean (`Clock.systemUTC()`); services never call `Instant.now()` or `LocalDate.now()` directly. `UserTime` = `Clock` + zone from Settings: `today()`, `weekStart()`, `weekEnd()`, `isFuture(date)`. Tests use a mutable test `Clock`.
- **Frontend**: `ClockService.now()` returns `Date` from a signal updated every second; `?clockOffsetMs=N` or `window.__qfClock.setNow(iso)` shifts it for tests. Rest time = `endDateTime - clock.now()` while `now` is inside the window; the displayed value refreshes every second and the plan list reloads from the API every 30 seconds and on focus, so server-computed status wins on conflict.

### Plan status computation
See data-model.md. `PlanStatusCalculator.statusOf(plan, doneItems, totalItems, now)` and `restSeconds(...)` are pure; the service maps entities to the `Plan` DTO on every read.

### Error body
`ApiError {timestamp, status, error, message, path, fieldErrors[{field, message}]}` for 400, 404 and 409 (`GlobalExceptionHandler`). Controllers declare the contract's 400/404/409 responses so the exported Swagger matches.

### Pagination
None (research R9).

## Milestones

Each milestone is a vertical slice with `[BE]` and `[FE]` tasks, built in parallel and verified on the real stack. Contract operations come from `x-milestone`.

| Milestone | Scope | Operations | Migration |
|---|---|---|---|
| M01 Foundation | BE: Maven project, Actuator health, springdoc, `ApiError` + handler, `Clock`, Settings API, H2/Flyway wiring, JUnit + curl lib wiring. FE: Angular 19 + Material app, proxy, shell with six pages and persistent nav, empty states, Settings page, start-page redirect, ClockService | 2 (settings) | V1 |
| M02 Tasks | BE: Task CRUD, complete/archive/restore, search/filters/sort, overdue. FE: Tasks page (search, status/priority/due filters, sort, add/edit dialog, complete/archive/delete, empty state, overdue marker) | 8 | V2 |
| M03 Habits | BE: Habit CRUD, completions, periods and streak. FE: Habits page (cards, toggle, frequency label, streak, edit/deactivate/remove, empty state) | 8 | V3 |
| M04 Learning | BE: cards, milestones, notes, derived status. FE: Learning page (card grid, expandable card, add/complete/remove milestones, notes, empty state) | 10 | V4 |
| M05 Plans | BE: plan create/list/get/delete, item toggle with task side effect, cleanup on source deletion, computed status and rest time, acknowledge-start. FE: Todo Plans page (builder with source pickers, groups active/upcoming and completed, progress, live rest time, item toggles, start banner with catch-up, duration warning) | 6 | V5 |
| M06 Dashboard | BE: aggregate endpoint. FE: Dashboard page (greeting, metric cards, today and overdue tasks, habit checklist, active plans with live rest time, learning snapshot, quick-add actions) and consistency with plans/tasks/habits | 1 | none |
| M07 Hardening | BE+FE: validation sweep against PRD section 7 and contract, error and empty states, loading states, accessibility basics (labels, focus, contrast), full E2E journey (PRD section 11), regression of all `[smoke]` scenarios, README run notes | 0 | none |

Dependencies: M02-M04 are independent of each other but M05 needs M02-M04 and M06 needs M02-M05. The pipeline runs milestones in order, which satisfies this.

## Project Structure

### Documentation
```text
specs/001-quickflow/
├── spec.md  plan.md  research.md  data-model.md  quickstart.md
├── contracts/openapi.yaml
├── acceptance.md        # written after tasks (curl cases + Playwright scenarios)
└── tasks.md             # speckit-tasks
```

### Source code
```text
backend/
├── pom.xml  mvnw  .mvn/
├── src/main/java/com/quickflow/{common,settings,task,habit,learning,plan,dashboard}/
├── src/main/resources/{application.properties, db/migration/V1__..V5__}
└── src/test/java/com/quickflow/...        # JUnit domain + slice tests
frontend/
├── package.json  angular.json  proxy.conf.js
└── src/app/{core,shared,layout,features/...}
loops/backend-dev/outputs/curl/Mxx.sh      # per-milestone curl suites (scripts/curl-lib.sh)
loops/frontend-dev/outputs/e2e/Mxx.md      # per-milestone Playwright MCP scenarios
```

## Testing strategy (per constitution IV)

| Level | What | Where |
|---|---|---|
| JUnit unit | `PlanStatusCalculator` (all three transitions, boundaries at start and end, all-done-early), rest time, completion percent rounding, `StreakCalculator` and `HabitPeriods` (daily, weekly, week boundary, time zones), `LearningStatusCalculator`, task overdue and filter logic, validation limits BR1/BR2/BR6, dashboard metrics versus raw data | `backend/src/test` |
| JUnit slice | controllers with MockMvc and a mutable `Clock`: status codes, error body, duplicate habit completion, plan creation rules, D-3 side effect, D-5 cleanup | `backend/src/test` |
| curl | one suite per milestone, empty database, acceptance ids `AC-Mxx-nn` | `loops/backend-dev/outputs/curl/` |
| Swagger diff | exported springdoc JSON versus contract (operations, parameters, response codes, schemas, enums) | `scripts/export-swagger.sh` |
| Playwright MCP | scenario per milestone `E2E-Mxx-nn` with `data-testid`s, screenshots, no console errors; against Prism for FE self-check, against the real stack for the verifier; `[smoke]` scenarios re-run in later milestones | `loops/frontend-dev/outputs/e2e/` |
| Time-based E2E | plan starting now + about 1 minute on the real stack, or `__qfClock` override; verifies start banner, rest time, status change, history | verifier |

## Complexity Tracking

Not applicable (no constitution violations).
