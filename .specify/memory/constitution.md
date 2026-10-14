# QuickFlow Constitution

## Core Principles

### I. Contract-First (NON-NEGOTIABLE)
`specs/NNN-*/contracts/openapi.yaml` is the single source of truth for the API.
- Every operation MUST carry `x-milestone: Mxx` and at least one response example.
- Each spec's contract MUST be the full API: the previous contract plus the new story's changes.
- Backend DTO class names MUST equal the contract schema names; paths, enums, status codes and the error body MUST match exactly.
- Dev loops (`backend-dev`, `frontend-dev`) MUST NOT edit anything under `contracts/`. They log problems in `loops/_shared/contract-issues.md` and keep building to the contract as written. Only the verifier (small, backward-compatible fixes) or the user may change it.
- Rationale: backend and frontend are built in parallel and can only agree through one fixed contract.

### II. Fixed Stack
- Backend: Java 17 language level only (no newer features), Spring Boot 3.5, Maven wrapper (`backend/mvnw`), Spring Data JPA, springdoc-openapi, Actuator health.
- Database: H2 in file mode, one database file per instance. Schema changes ONLY through Flyway migrations (`db/migration/V<n>__<desc>.sql`); `spring.jpa.hibernate.ddl-auto=validate`; SQL MUST stay plain and portable; applied migrations are never edited.
- Frontend: Angular 19 (standalone components) + Angular Material, Node 18.20. All API calls use relative URLs through the dev proxy `frontend/proxy.conf.js` (target from `API_TARGET`).
- Nothing may depend on a file outside the project folder or on a global install; pinned test tools live in `tools/`.

### III. Separation and Ownership
- Backend code lives in `backend/`, frontend code in `frontend/`. Each loop edits only its own folders (see its Loop-instructions) and its own `loops/<role>/` folder.
- Business rules (validation, status transitions, rest time, completion roll-up, dashboard metrics) MUST live in plain, unit-testable classes, separate from controllers and UI components.
- Completion-state logic MUST NOT be duplicated between a plan item and its source entity beyond what FR-08 and Business Rule 13 require.
- Only the runner commits, tags or pushes. Agents MUST NOT run `git commit/push/reset/checkout/tag/stash/clean`.

### IV. Test Everything at Its Own Level (NON-NEGOTIABLE)
- Backend: JUnit tests are REQUIRED for every business rule (task/habit/plan validation, rest time, plan status lifecycle, completion roll-up, dashboard metrics). `./mvnw verify` MUST pass.
- Backend: every milestone has a curl suite `loops/backend-dev/outputs/curl/Mxx.sh` built on `scripts/curl-lib.sh`. It covers the happy path, validation errors, not found and the milestone's business rules, passes on an empty database, and every case names the acceptance id it proves. `scripts/curl-regression.sh` runs all suites up to the milestone.
- Backend: the Swagger exported from the code MUST match the contract (`scripts/export-swagger.sh`).
- Frontend: every feature is tested with the Playwright MCP through a scenario file `loops/frontend-dev/outputs/e2e/Mxx.md`, with screenshots and no console errors. `ng build` MUST pass.
- The Prism mock is stateless; state-dependent flows are proven by the verifier on the real stack.

### V. Controllable Time
- Backend: time MUST come from an injected `java.time.Clock`; `now()` calls without a clock are forbidden. Plan status and rest time are computed from stored times and the clock, so they are identical after a restart.
- Frontend: all time-dependent UI (rest time, start notification) MUST read time through one injectable clock service that supports a test-time override (Playwright MCP 0.0.40 has no clock control).
- Time-based E2E on the real stack uses plans that start at about now + 1 minute.

### VI. Stable UI Test Hooks
Every element a test touches MUST have a `data-testid` named `<page>-<element>[-<qualifier>]` in lower-case kebab-case, for example `tasks-add-button`, `nav-habits`, `dashboard-metric-tasks`, `tasks-row-<id>`. Ids listed in `acceptance.md` are mandatory and MUST NOT be renamed without updating it.

### VII. Autonomous, Self-Contained Loops
- A headless loop NEVER asks the user. It reads `requirements/DECISIONS.md`; if a question is not settled there, it records an assumption in `loops/_shared/assumptions.md` and continues.
- All loop memory is in files (`task.md`, `progress.md`, `state/`, `outputs/`). A task is ticked only when its code exists and its self-check passed.
- Servers are started and stopped only through `scripts/backend.sh`, `scripts/frontend.sh` and `scripts/mock.sh`, and always stopped afterwards.

## Technical Constraints

| Port | Use |
|---|---|
| 8091 | backend self-check (backend-dev) |
| 8090 | stable backend (verify) |
| 4300 | frontend dev server |
| 4010 | Prism mock of the contract |

- Single-user application, no authentication unless `requirements/DECISIONS.md` says otherwise.
- Common error body, status codes and pagination follow the contract; validation limits follow PRD section 7.
- Typical API actions SHOULD respond in under 500 ms.

## Development Workflow

- Work is split into milestones (vertical slices). Each milestone has `[BE]` and `[FE]` tasks, which are built in parallel. A milestone passes only when the verifier approves it on the real full stack. Then the runner commits and tags it.
- The verifier may fix small issues itself (at most 3 files, about 30 lines, no design or contract change) and MUST log each fix. Anything larger becomes a FIX task.
- A milestone gets at most 3 trials, then one retry on a stronger model, then it is blocked for the user. Costs stay within the per-milestone budget in `loops/_shared/budget.env`.
- Requirements precedence: `requirements/DECISIONS.md` over the PRD; `task-brief.txt` requirements are mandatory.

## Governance

This constitution supersedes other practices in this repository. Amendments are made only by the user, in an interactive planning session, with a version bump and a note in the Sync Impact Report: MAJOR for removed or redefined principles, MINOR for new principles or materially expanded rules, PATCH for wording. The verifier checks every milestone against this constitution, and any violation is a REJECT. Runtime guidance for agents is in `CLAUDE.md` and `loops/_shared/PROTOCOL.md`.

**Version**: 1.0.0 | **Ratified**: 2026-10-15 | **Last Amended**: 2026-10-15
