# Self-check M03 (T031-T038), backend-dev trial 0, iteration 1

Run on 2026-10-15 (self-check port 8091, fresh db `selfcheck`).

| Step | Command | Outcome |
|---|---|---|
| 1 | `cd backend && ./mvnw -q verify` | green: 101 JUnit tests, 0 failures, 0 errors (surefire reports) |
| 2 | `scripts/backend.sh start --port 8091 --db selfcheck --fresh` | healthy at http://localhost:8091/actuator/health |
| 3 | `scripts/curl-regression.sh --base http://localhost:8091 --upto M03` | M01 6/6, M02 14/14, M03 13/13, 0 failed |
| 4 | `scripts/export-swagger.sh --base http://localhost:8091 --ms M03` | PASS, 18 operations checked, no problems, no warnings (`swagger-diff.md`) |
| 5 | `scripts/backend.sh stop --port 8091` | stopped |

## JUnit per class (M03 classes in bold)
- **HabitPeriodsTest 5**, **StreakCalculatorTest 7**, **HabitServiceTest 15**, **HabitControllerTest 16**
- existing: UserTimeTest 5, SettingsControllerTest 6, SettingsServiceTest 10, TaskQueryTest 13, TaskServiceTest 9, TaskControllerTest 11, GlobalExceptionHandlerTest 4

## Files
- `outputs/evidence/M03/openapi-exported.json`, `openapi-exported.yaml`, `swagger-diff.md`
- curl suite: `outputs/curl/M03.sh` (AC-M03-01..13)
