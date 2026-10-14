# Loop: backend-dev

**Role**: implement the backend of the current milestone, phase by phase, to the contract; test every phase with curl; produce Swagger from the code.
**Input**: a spec folder (`specs/NNN-*/`: spec, plan, data model, `contracts/openapi.yaml`, acceptance) generated from a full PRD or a single user story, plus your milestone file.
**Output**: backend code in `backend/`, ticked milestone file `outputs/Mxx-*.md`, curl suite `outputs/curl/Mxx.sh`, evidence `outputs/evidence/Mxx/` (curl log, exported Swagger, Swagger diff, test report), action log in `progress.md`, `state/handoff.md`, `state/result.json`.

Follow `loops/_shared/PROTOCOL.md` for the iteration cycle. This file adds the backend rules.

## Allowed to edit
`backend/**`, `loops/backend-dev/**` (except `progress.md`'s runner table and `state/state.json`), and append-only `loops/_shared/assumptions.md` and `loops/_shared/contract-issues.md`.

## Stack rules
- Spring Boot 3.5.x, **Java 17 language level** (no newer features), Maven wrapper (`backend/mvnw`). If the wrapper does not exist yet (first milestone), create the project with `mvn -N wrapper:wrapper` and a hand-written `pom.xml`.
- Persistence: Spring Data JPA + **Flyway only** (`src/main/resources/db/migration/V<n>__<desc>.sql`), `spring.jpa.hibernate.ddl-auto=validate`, H2 file database. Never edit an applied migration of an earlier milestone; add a new one. Plain, portable SQL.
- The database URL comes from the property `qf.db.path` (default `./data/dev`) → `jdbc:h2:file:${qf.db.path};AUTO_SERVER=FALSE`. `scripts/backend.sh` sets it.
- API: springdoc-openapi; `/v3/api-docs` and `/v3/api-docs.yaml` must work. Spring Boot Actuator `/actuator/health` must work (used for health waits).
- Base path, DTO names, enums, status codes and the error body must match `contracts/openapi.yaml` exactly.
- Layers: `controller` (thin) → `service` (business rules) → `repository`. Business rules (validation, rest time, status transitions, completion roll-up, dashboard metrics) live in plain, unit-testable classes. Time comes from an injected `java.time.Clock` bean, never `now()` directly.
- CORS is not needed (the FE uses a dev proxy).

## Self-check (run before ticking; full run before `built`)
1. `cd backend && ./mvnw -q verify` (compiles, runs JUnit tests). **JUnit tests for every business rule you add are required.**
2. `scripts/backend.sh start --port 8091 --db selfcheck --fresh` (builds the jar, waits for health).
3. Write/extend `loops/backend-dev/outputs/curl/Mxx.sh` for this milestone's acceptance cases (see conventions), then run
   `scripts/curl-regression.sh --base http://localhost:8091 --upto Mxx` (this milestone's suite and all earlier ones).
4. `scripts/export-swagger.sh --base http://localhost:8091 --ms Mxx` → writes the exported Swagger and a diff against the contract into `outputs/evidence/Mxx/`. Every operation of this milestone must be present and match. Fix differences in code, or log a contract issue.
5. `scripts/backend.sh stop --port 8091`.

## Curl suite conventions (`outputs/curl/Mxx.sh`)
```bash
#!/usr/bin/env bash
# Mxx curl suite: <milestone title>. Each case names the acceptance id it proves.
source "$(dirname "$0")/../../../../scripts/curl-lib.sh"
case_begin "AC-M02-01 create task returns 201"
req POST /api/tasks '{"title":"Write report","priority":"HIGH"}'
expect_status 201
expect_json '.title == "Write report"'
TASK_ID=$(json '.id')
case_end
# ...
suite_end
```
- Every suite must pass on an **empty** database and when run after the earlier suites: create the data you need, never assume ids.
- Cover the happy path, the validation errors (400 + error body), not found (404) and the business rules of the milestone.

## Done means
All `[BE]` tasks of the milestone ticked with evidence, `./mvnw verify` green, curl regression green up to this milestone, Swagger diff clean for this milestone's operations.
