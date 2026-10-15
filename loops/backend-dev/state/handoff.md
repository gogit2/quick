# Handoff - backend-dev (M02, after iter 1)

## Done
- All 8 BE tasks of M02 ticked (T017-T024) in `outputs/M02-tasks.md`, each with evidence.
- Full self-check green: `./mvnw -q verify` (58 tests), curl regression `--upto M02` (M01 6/6, M02 14/14), Swagger diff PASS for 10 operations.
- Result: `built`. Nothing is open for the backend in M02. Next step belongs to the verifier.

## Next
- None for backend-dev in M02. If the runner re-invokes this loop, first check that `outputs/M02-tasks.md` still shows all T017-T024 ticked, then stop.
- For M03 (habits): `TaskService.reopen(id)` exists for plans (D-3); `SourceDeletedEvent` is published from `TaskService.delete`.

## Traps and facts
- Working commands: `cd backend && ./mvnw -q verify` (quiet mode prints nothing on success; check `target/surefire-reports/*.xml`); `scripts/backend.sh start --port 8091 --db selfcheck --fresh`; `scripts/curl-regression.sh --base http://localhost:8091 --upto M02`; `scripts/export-swagger.sh --base http://localhost:8091 --ms M02`; `scripts/backend.sh stop --port 8091`. Boot takes ~6 s.
- M01's curl suite leaves the saved time zone at `Europe/Berlin`. Any suite that uses "today" or "yesterday" must read `/api/settings` first (M02.sh does this).
- Bash: a `for` loop with `$var` in a `cat` was denied. Use the Read tool for several files, or separate plain commands.
- Timestamps: `task.created_at`/`updated_at` are `TIMESTAMP` columns mapped with `@JdbcTypeCode(SqlTypes.TIMESTAMP)`, and `hibernate.jdbc.time_zone=UTC` is set. Keep both for any new Instant column, or validation will fail.
- Overdue has one definition: `TaskMapper.isOverdue`. `TaskQuery` OVERDUE repeats it in SQL (not DONE, not archived, before today). Keep them in step.
- Test pattern: Spring tests with a fresh db file per class (`qf.db.path=./target/test-data/...-${random.uuid}`) and `@Import(MutableClockTestConfig.class)` for a movable clock. Clean tables in `@BeforeEach` (no DirtiesContext needed).
- Standalone MockMvc does not use Boot's date formatting. Use Boot's MockMvc (`@AutoConfigureMockMvc`) as in the task tests.

## Open concern (for the user, not fixed here)
- Maven resolves dependencies into `~/.m2` (outside the project). Constitution II says nothing may depend on files outside the folder. See A-verify-M01-1 in `loops/_shared/assumptions.md`.
