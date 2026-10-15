# Handoff - backend-dev (M07, after iter 1)

## Done
- M07 backend complete: T089-T093 ticked with evidence in `outputs/evidence/M07/`.
- `cd backend && ./mvnw -q verify` green (188 tests, 0 failures). curl-regression --upto M07 PASS on a fresh db (7 suites, 0 failed). export-swagger M07 PASS (35 operations, no problems). Server 8091 stopped.
- Production changes: GlobalExceptionHandler (catch-all 500 without stack trace, Spring 404/405/415 keep status in the ApiError body, bad enum in a JSON body → field name), common/RequestLogFilter (one INFO line per request).
- Result: built. Nothing is open for backend-dev in M07.

## Next
- Nothing for backend-dev. The runner decides on the verifier and the commit.

## Traps and facts
- curl suites need an empty db. Run `scripts/backend.sh start --port 8091 --db selfcheck --fresh` before every regression run.
- PlanLifecycleIntegrationTest starts two Spring contexts on one H2 file. Close the first before starting the second.
- BusinessRulesSweepTest and ErrorHandlingTest share one context per class and one db. Tests that depend on time set the MutableClock themselves.
- An unexpected exception is logged with its stack trace (log.error) but the body says only "Unexpected error". Expect the stack trace in test output; it is not a failure.
- Bash: a compound with `;`, `>` and a pipe was denied several times. Plain `cd backend && ./mvnw -q verify 2>&1 | grep ...` worked. A bare `./mvnw` from the project root does not exist; run it from backend/.
