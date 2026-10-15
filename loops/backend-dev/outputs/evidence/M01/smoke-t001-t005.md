# M01 backend smoke (T001-T005), trial 0 iter 1

Date: 2026-10-15 (self-check port 8091, db `selfcheck`, fresh)

## Build and unit tests
- `cd backend && ./mvnw -q verify` → exit 0 (after fixing GlobalExceptionHandlerTest timestamp format)
- Tests: GlobalExceptionHandlerTest (4: 400 with fieldErrors, 404, 409, 400 with field errors), SettingsServiceTest (10: defaults, persist + configured, blank email to null, invalid/blank timeZone, blank/over-long displayName, invalid email, missing notificationsEnabled, no save on failure)

## Server
- `scripts/backend.sh start --port 8091 --db selfcheck --fresh` → healthy (Flyway V1 applied, ddl-auto=validate passed)
- `GET /actuator/health` → UP (via backend.sh health wait)
- `scripts/backend.sh stop --port 8091` → stopped

## HTTP checks
```
GET /api/settings
< {"displayName":"QuickFlow User","email":null,"timeZone":"UTC","notificationsEnabled":true,"defaultStartPage":"DASHBOARD","configured":false}

PUT /api/settings  {"displayName":"Alex","email":"alex@example.com","timeZone":"Mars/Olympus",...}
< 400 {"timestamp":"...Z","status":400,"error":"Bad Request","message":"Validation failed","path":"/api/settings","fieldErrors":[{"field":"timeZone","message":"must be a valid IANA time zone id"}]}

PUT /api/settings  {"displayName":"Alex","email":null,"timeZone":"Europe/Berlin","notificationsEnabled":true,"defaultStartPage":"TASKS"}
< {"displayName":"Alex","email":null,"timeZone":"Europe/Berlin","notificationsEnabled":true,"defaultStartPage":"TASKS","configured":true}
```
