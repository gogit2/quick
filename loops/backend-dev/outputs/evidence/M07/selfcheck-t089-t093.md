# M07 self-check (T089-T093), 2026-10-15

## Full self-check (Loop-instructions order)
1. `cd backend && ./mvnw -q verify`: green, 188 tests, 0 failures, 0 errors (165 before M07 + 23 new).
2. `scripts/backend.sh start --port 8091 --db selfcheck --fresh`: healthy.
3. `scripts/curl-regression.sh --base http://localhost:8091 --upto M07`: 7 suites, 0 failed. M07.sh 5/5 (AC-M07-01..05).
4. `scripts/export-swagger.sh --base http://localhost:8091 --ms M07`: PASS, 35 operations, no problems, no warnings (`swagger-diff.md`).
5. `scripts/backend.sh stop --port 8091`: stopped.

## New tests
- `backend/src/test/java/com/quickflow/hardening/BusinessRulesSweepTest.java`: 14 tests, one per BR1-BR14, over MockMvc.
- `backend/src/test/java/com/quickflow/hardening/PlanLifecycleIntegrationTest.java`: 2 tests. The app starts twice on one H2 file with the clock moved in between.
- `backend/src/test/java/com/quickflow/hardening/ErrorHandlingTest.java`: 7 tests (unknown route, wrong method, malformed JSON, enum in body, catch-all 500, one INFO line per request, latency with 200 tasks).

## Production changes
- `common/GlobalExceptionHandler.java`: invalid enum in a JSON body gives 400 "Validation failed" with the field name. Spring's own errors (404 unknown route, 405, 415) keep their status with the ApiError body. Anything else is a 500 "Unexpected error", logged server-side with the stack trace, not in the body.
- `common/RequestLogFilter.java` (new): one INFO line per request: method, path, status, duration. No query string.
