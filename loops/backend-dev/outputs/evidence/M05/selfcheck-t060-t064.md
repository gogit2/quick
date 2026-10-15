# M05 self-check (T060-T064), iter 2

- Command: `cd backend && ./mvnw -q verify` (plain form; `${PIPESTATUS}`, redirections and combined commands were denied by the permission check)
- Outcome: green, exit without failures. Surefire reports in `backend/target/surefire-reports/`:
  - `com.quickflow.plan.PlanStatusCalculatorTest`: 10 run, 0 failures, 0 errors
  - all earlier suites (M01-M04) still green: 15 classes in total
- Spring context boots in the controller tests, so `ddl-auto=validate` accepted `PlanEntity` / `PlanItemEntity` against `V5__plans.sql`.
- Not run yet (belongs to later tasks): server on 8091, curl suite `M05.sh`, `export-swagger.sh --ms M05`. Those are the full self-check for `built`.
- Behaviour tests of PlanService (create rules, ticking, acknowledge, list ordering) are T067, still open.
