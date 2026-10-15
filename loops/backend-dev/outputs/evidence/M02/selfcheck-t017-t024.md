# M02 self-check (backend-dev, trial 0, iter 1) - T017-T024

Full milestone self-check, all run from the project root on 2026-10-15.

| Step | Command | Outcome |
|---|---|---|
| 1 | `cd backend && ./mvnw -q verify` | PASS. 58 tests, 0 failures, 0 errors (M01 25 + M02 33) |
| 2 | `scripts/backend.sh start --port 8091 --db selfcheck --fresh` | healthy at 03:39:07 |
| 3 | `scripts/curl-regression.sh --base http://localhost:8091 --upto M02` | PASS. M01 6/6, M02 14/14 |
| 4 | `scripts/export-swagger.sh --base http://localhost:8091 --ms M02` | PASS, 10 operations checked, no problems or warnings. See `swagger-diff.md` and `openapi-exported.yaml` in this folder |
| 5 | `scripts/backend.sh stop --port 8091` | stopped |

## JUnit (per class)
- `TaskServiceTest` 9: defaults TODO/MEDIUM, complete, archive hides and restore, overdue rule, update replaces fields and changes updatedAt, missing status/priority kept, reopen, delete publishes SourceDeletedEvent.
- `TaskQueryTest` 13: search, status, status+priority, due TODAY/OVERDUE/THIS_WEEK/NO_DUE_DATE, dueFrom/dueTo range, archived=true, sort CREATED_DESC/CREATED_ASC/DUE_DATE_ASC/DUE_DATE_DESC with nulls last.
- `TaskControllerTest` 11: validation 400 (title blank and 201 chars, description 2001, invalid body enum, invalid query enum), 404 ApiError, update, lifecycle endpoints, search and filter params, api-docs.

## Notes
- Curl output was printed to the terminal during the run, not saved to a file. Re-run `scripts/curl-regression.sh` to capture a log if needed.
- Swagger export was run against a fresh database. The curl suite creates its own data and passes after M01.
