# Handoff - backend-dev (M06, after iter 2)

## Done
- M06 backend complete: T076-T081 ticked with evidence in `outputs/evidence/M06/`.
- `./mvnw -q verify` green (165 tests, 0 failures). curl-regression --upto M06 PASS on a fresh db (6 suites, 0 failed). M06.sh alone on a fresh db 7/7. export-swagger M06 PASS (35 operations, no diff). Server 8091 stopped.
- Result: built. Nothing is open for backend-dev in M06.

## Next
- Nothing for backend-dev. The runner decides on the verifier and the commit.

## Traps and facts
- curl suites need an empty db. Run `scripts/backend.sh start --port 8091 --db selfcheck --fresh` before every regression run. A second run on the same db fails by design.
- AC-M06-01's all-zero check only runs when the db has no tasks, so it is not reached in the regression (A-backend-dev-M06-1). Run M06.sh alone on a fresh db to exercise it.
- Dashboard: a plan item's done flag is independent of its task, except that ticking a task item completes the task (BR13). Completing a task directly does not tick the item.
- A learning card whose milestones are all done is COMPLETED (D-7). Keep an open milestone when a test needs IN_PROGRESS.
- Dashboard shares the same owning-service lists as the list endpoints (TaskService, HabitService, PlanService, LearningService). Do not add separate counting queries.
- Bash: `cd X && cmd > file` and `${PIPESTATUS[0]}` were denied. Plain `cd backend && ./mvnw ... | tail` worked. The Grep tool is not available; use Bash grep.
