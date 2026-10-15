# M05 self-check (iter 3, tasks T065-T068)

Date: 2026-10-15 (UTC+3 local). Model: claude-haiku-5-5.

| Step | Command | Outcome |
|---|---|---|
| 1 | `cd backend && ./mvnw -q verify` | green on the final run: 159 tests, 0 failures, 0 errors (plan: PlanStatusCalculatorTest 10, PlanServiceTest 13, PlanItemCleanupTest 5, PlanControllerTest 6). Two failures found on the first run and fixed: a test-only lazy-init (restart check now runs in a real transaction) and a wrong expectation (fixture start at the fixed clock time gives IN_PROGRESS; fixture moved to 11:00). |
| 2 | `scripts/backend.sh start --port 8091 --db selfcheck --fresh` | healthy |
| 3 | `scripts/curl-regression.sh --base http://localhost:8091 --upto M05` | exit 0 on a fresh db: M01 6/6, M02 14/14, M03 13/13, M04 12/12, M05 16/16; 5 suites, 0 failed. Log: `curl-regression-M05.log`. A second run against the same db without `--fresh` failed, as expected, because the suites assume an empty database. |
| 4 | `scripts/export-swagger.sh --base http://localhost:8091 --ms M05` | PASS, 34 operations checked, no problems, no warnings (`swagger-diff.md`, `openapi-exported.yaml`) |
| 5 | `scripts/backend.sh stop --port 8091` | stopped |

Tasks ticked: T065 (PlanItemCleanup + test), T066 (PlanController), T067 (PlanServiceTest, PlanControllerTest), T068 (curl suite M05.sh and the full self-check above).
