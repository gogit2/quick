# M06 Dashboard [BE] self-check (backend-dev, trial 0, iter 2)

Tasks: T076 DTOs, T077 task/rules part of DashboardService, T078 habit/plan/learning part, T079 DashboardController, T080 JUnit, T081 curl suite.

## Commands and outcomes
1. `cd backend && ./mvnw -q verify` - green. Surefire reports: 165 tests in 20 classes, 0 failures, 0 errors. New for M06: `DashboardRulesTest` 4/4, `DashboardConsistencyTest` 2/2.
2. `scripts/backend.sh start --port 8091 --db selfcheck --fresh` - healthy.
3. `scripts/curl-regression.sh --base http://localhost:8091 --upto M06` on a fresh db - 6 suites, 0 failed (M01 6/6, M02 14/14, M03 13/13, M04 12/12, M05 16/16, M06 7/7).
4. `loops/backend-dev/outputs/curl/M06.sh` run alone on a fresh db (the empty-database case of AC-M06-01) - 7/7.
5. `scripts/export-swagger.sh --base http://localhost:8091 --ms M06` - PASS, 35 operations checked, no problems (`swagger-diff.md`, `openapi-exported.json`, `openapi-exported.yaml` in this folder).
6. `scripts/backend.sh stop --port 8091` - stopped.

## Notes
- The dashboard code of iteration 1 (DashboardService, DashboardRules, the DTOs and the controller) compiled unchanged and needed no fix.
- A plan item's done flag is independent of the task: completing a task directly does not tick its plan item (BR13 / D-3). DashboardConsistencyTest asserts this.
- A learning card whose milestones are all done is COMPLETED (D-7); AC-M06-06 keeps one milestone open so the card stays IN_PROGRESS.
- Assumption on AC-M06-01 logged as A-backend-dev-M06-1.
