# M04 self-check (iteration 2: T050-T053)

Run on 2026-10-15 (local time +03:00), backend port 8091, database `selfcheck --fresh`.

| Step | Command | Result |
|---|---|---|
| 1 | `cd backend && ./mvnw -q verify` | green, 125 tests, 0 failures (surefire reports). Learning: LearningServiceTest 7, LearningControllerTest 10, LearningStatusCalculatorTest 7 |
| 2 | `scripts/backend.sh start --port 8091 --db selfcheck --fresh` | healthy |
| 3 | `scripts/curl-regression.sh --base http://localhost:8091 --upto M04` | 4 suites, 0 failed (M01 6/6, M02 14/14, M03 13/13, M04 12/12) |
| 4 | `scripts/export-swagger.sh --base http://localhost:8091 --ms M04` | PASS, 28 operations checked, no problems, no warnings (`outputs/evidence/M04/swagger-diff.md`) |
| 5 | `scripts/backend.sh stop --port 8091` | stopped |

Code added in this iteration:
- `LearningNoteCreate` (contract schema LearningNoteCreate; it was missing after iteration 1).
- `LearningService.addNote`, `deleteNote`; `cards.flush()` after `addMilestone` and `addNote`, so the returned DTO has its database id (before, the id was null until commit).
- `LearningController` with the 10 contract operations.
- `LearningServiceTest`, `LearningControllerTest`.
- `outputs/curl/M04.sh` (AC-M04-01..12).
