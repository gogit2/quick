# Handoff - backend-dev (M01, after iter 2)

## Done
- All 8 BE tasks of M01 ticked (T001-T008) in `outputs/M01-foundation.md`, each with evidence.
- Full self-check green: `./mvnw -q verify` (25 tests), curl suite `outputs/curl/M01.sh` 6/6 on a fresh db, Swagger export matches the contract for M01.
- Result: `built`. Nothing is open for the backend in M01. Next step belongs to the verifier.

## Next
- None for backend-dev in M01. If the runner re-invokes this loop, first check that `outputs/M01-foundation.md` still shows all T001-T008 ticked, then stop.

## Traps and facts
- Working commands: `cd backend && ./mvnw -q verify`; `scripts/backend.sh start --port 8091 --db selfcheck --fresh`; `scripts/curl-regression.sh --base http://localhost:8091 --upto M01`; `scripts/export-swagger.sh --base http://localhost:8091 --ms M01`; `scripts/backend.sh stop --port 8091`. Boot takes ~6 s here.
- H2 rejects implicitly relative file paths in the URL. Test properties need `./target/...`.
- Spring tests share one context, so DB state leaks between tests. `SettingsControllerTest` uses `@DirtiesContext(BEFORE_EACH_TEST_METHOD)` with a random db path. Use the same pattern for any future HTTP-level test that saves data.
- `UserTime` uses `LocalDate.ofInstant(clock.instant(), zone())`. Do not use `clock.withZone(...)`: `MutableClock` ignores the zone and always returns UTC.
- Serialization: Boot writes `Instant` as ISO. Standalone MockMvc does NOT: use `Jackson2ObjectMapperBuilder.json().featuresToDisable(WRITE_DATES_AS_TIMESTAMPS)` + `setMessageConverters` (see GlobalExceptionHandlerTest).
- An invalid `defaultStartPage` string fails in Jackson, so it returns 400 "Malformed request body" with `fieldErrors: []`. AC-M01-04 only needs the 400.
- Bash: `cd` combined with output redirection, and absolute-path `mvnw -f`, were denied in this session. The plain `cd backend && ./mvnw -q verify` form worked. Use the Read tool for files.

## Open concern (for the user, not fixed here)
- Maven resolves dependencies into `~/.m2` (outside the project). The constitution (II) says nothing may depend on files outside the folder. Decide whether to set `maven.repo.local` in `backend/.mvn/maven.config`. The wrapper distribution still goes to `~/.m2/wrapper` unless `MAVEN_USER_HOME` is set.
