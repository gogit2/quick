# M01 self-check (iter 2: T006, T007, T008)

Run: 2026-10-15, trial 0, iteration 2, claude-haiku-5-5.

## 1. `cd backend && ./mvnw -q verify`: PASS (25 tests, 0 failures)
- `settings/SettingsServiceTest`: 10, pass (existing)
- `settings/SettingsControllerTest`: 6, pass (new, T007). Real GET/PUT on a fresh H2 file per test
  (`@DirtiesContext` per method, `qf.db.path=./target/test-data/settings-<uuid>`). Covers defaults,
  PUT then GET with `configured=true`, invalid timeZone / blank displayName / invalid email → 400 with fieldErrors,
  and `/v3/api-docs.yaml` listing `/api/settings`.
- `common/GlobalExceptionHandlerTest`: 4, pass (existing)
- `common/UserTimeTest`: 5, pass (new, T006). UTC vs Pacific/Kiritimati on the same instant; Sunday 23:30 UTC
  is Monday in Berlin (week boundary); Monday-start week; isFuture.

## 2. `scripts/backend.sh start --port 8091 --db selfcheck --fresh`: healthy, then stopped.

## 3. `scripts/curl-regression.sh --base http://localhost:8091 --upto M01`: PASS
- `loops/backend-dev/outputs/curl/M01.sh`: 6 passed, 0 failed
  (AC-M01-01 defaults, -02 PUT then GET, -03 timeZone 400, -04 defaultStartPage 400, -05 displayName 400, -06 health UP).

## 4. `scripts/export-swagger.sh --base http://localhost:8091 --ms M01`: PASS
- `openapi-exported.json` / `openapi-exported.yaml` and `swagger-diff.md` (2 operations checked, no problems).

## 5. Server 8091 stopped.
