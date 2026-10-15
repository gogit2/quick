# Unit tests and build - M02 round 1

`cd backend && ./mvnw -q verify` -> BUILD OK, 58 tests, 0 failures, 0 errors (from `backend/target/surefire-reports/*.xml`):

| Suite | tests | failures | errors |
|---|---|---|---|
| com.quickflow.task.TaskQueryTest | 13 | 0 | 0 |
| com.quickflow.task.TaskServiceTest | 9 | 0 | 0 |
| com.quickflow.task.TaskControllerTest | 11 | 0 | 0 |
| com.quickflow.common.GlobalExceptionHandlerTest | 4 | 0 | 0 |
| com.quickflow.common.UserTimeTest | 5 | 0 | 0 |
| com.quickflow.settings.SettingsServiceTest | 10 | 0 | 0 |
| com.quickflow.settings.SettingsControllerTest | 6 | 0 | 0 |

`cd frontend && npx ng build` (after fix F1) -> Application bundle generation complete; only warning: initial bundle 534.88 kB exceeds the 500 kB budget (warning, not an error; same as reported by frontend-dev).
