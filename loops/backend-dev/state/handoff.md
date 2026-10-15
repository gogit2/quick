# Handoff - backend-dev (M05, after iter 3)

## Done
- M05 backend complete: T060-T068 ticked with evidence in `outputs/evidence/M05/`.
- `./mvnw -q verify` green (159 tests). curl-regression --upto M05 PASS on a fresh db (5 suites). export-swagger M05 PASS (34 operations, no diff). Server 8091 stopped.
- Result: built. Nothing is open for backend-dev in M05.

## Next
- Nothing for backend-dev. The runner decides on the verifier and the commit.

## Traps and facts
- curl suites need an empty db. Run `scripts/backend.sh start --port 8091 --db selfcheck --fresh` before every regression run.
- PlanItemCleanup runs inside the delete transaction (synchronous @EventListener). A plan left with no items is deleted there.
- Bash: writing redirects to paths outside the project (for example /tmp) was denied. Redirects inside the project worked.
- Tests that build a service with `new` have no transaction proxy: wrap calls in TransactionTemplate.
