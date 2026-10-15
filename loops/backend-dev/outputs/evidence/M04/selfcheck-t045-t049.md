# Self-check M04 · T045-T049 · iter 1 (trial 0)

Time: 2026-10-15T04:01 (+03:00)

## Command
    cd backend && ./mvnw -q verify

## Outcome
- Exit: success (quiet mode prints nothing on success). Compilation, all JUnit tests and the Spring context start (Flyway V1-V4, `ddl-auto=validate` over all entities including the new learning ones) passed.
- Surefire totals, from `backend/target/surefire-reports/*.txt`: 108 tests, 0 failures, 0 errors, 0 skipped.
  - new: `com.quickflow.learning.LearningStatusCalculatorTest` 7/7 (0, 1, n milestones; all done; one undone again).
  - existing: GlobalExceptionHandlerTest 4, UserTimeTest 5, HabitControllerTest 16, HabitPeriodsTest 5, HabitServiceTest 15, StreakCalculatorTest 7, SettingsControllerTest 6, SettingsServiceTest 10, TaskControllerTest 11, TaskQueryTest 13, TaskServiceTest 9.

## Not run in this iteration
- Server start, curl regression and Swagger export: they need the controller (T051) and the curl suite (T053). They run in the full milestone self-check after T053.
- Learning service rules have no unit test yet; they come with T052.
