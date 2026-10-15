# Verify report - M01 Foundation, round 1

- **Milestone**: M01 - Foundation (`specs/001-quickflow`)
- **Round**: 1 (trial 0 failed so far)
- **Verdict**: **PASS**
- **Time**: 2026-10-15T03:30+03:00
- **Stack**: backend http://localhost:8090 (stable db), frontend http://localhost:4300 proxying to it

## Checks

| # | Check | Result | Evidence |
|---|---|---|---|
| 1 | Review (`git diff HEAD` + untracked backend/, frontend/, loop files) | pass | this report, "Review notes" |
| 2 | Swagger export vs contract (2 operations up to M01) | pass | `evidence/M01/round-1/swagger-diff.md`, `openapi-exported.yaml` |
| 3 | `./mvnw -q verify` (25 tests) + `npx ng build` | pass | `evidence/M01/round-1/mvnw-verify.log` |
| 4 | curl regression `--upto M01` (AC-M01-01..06, 6/6) | pass | `evidence/M01/round-1/curl.log` |
| 5 | Playwright MCP, real stack: E2E-M01-01..04 + real-stack-only (first run, server fieldErrors, backend-down error state) | pass | `evidence/M01/round-1/e2e-result.json`, screenshots in `run-artifacts/playwright/` |
| 5b | Smoke of earlier milestones | n/a (M01 is the first) | - |
| 6 | Contract issues of M01 | none open | `loops/_shared/contract-issues.md` |

Evidence paths are relative to `loops/verify/outputs/`.

## Review notes

Every ticked task (T001-T016) has its code:
- BE: pom (Boot 3.5.6, Java 17), wrapper, `application.properties` (file H2, `ddl-auto=validate`, Flyway, health, `/v3/api-docs`), `V1__settings.sql` (plain SQL, row id=1 with the defaults), `common/` (ClockConfig, ApiError/FieldError with the contract fields, exceptions, GlobalExceptionHandler using `Instant.now(clock)`, PlanItemSourceType, SourceDeletedEvent, UserTime), `settings/` (DTO names Settings/SettingsUpdate/StartPage equal the contract; rules in SettingsService, not in the controller), tests incl. MutableClock. No clock-less `now()` calls.
- FE: Angular 19.2 + Material 19.2, `proxy.conf.js` with `API_TARGET`, relative `/api/settings`, models mirror the contract, error interceptor (typed error + 5xx snackbar), ClockService with `?clockOffsetMs` and `window.__qfClock`, shell with the six `nav-*` ids and `active` class, lazy pages with `<page>-title`, EmptyState and ConfirmDialog with the mandated test ids, Settings page with all mandated ids, SettingsStore with first-run silent PUT of the browser zone and `/` redirect to the start page.
- Build output (`target/`, `data/`, `node_modules/`, `.angular/`, `dist/`) is git-ignored.

Non-blocking observations (no FIX task; for later milestones / the user):
1. No catch-all `@ExceptionHandler(Exception.class)`: an unexpected exception would return Spring's default error body instead of `ApiError`. D-16 only lists 400/404/409, so not a violation for M01; worth adding when 5xx paths appear.
2. Time zone validation uses `ZoneId.of`, which also accepts offset ids such as `+03:00` or `GMT+3` (not IANA region ids). The UI only offers IANA ids, so it is harmless now.
3. Settings page: when the GET fails, the form still shows defaults and Save is enabled. Save fails anyway while the backend is down, so nothing is lost.
4. Cosmetic: Google font links were removed from `index.html` (self-contained rule), so text falls back to a serif font, and the toolbar has no primary colour under the M3 theme.
5. `ng build` warns that the initial bundle is 532 kB (budget 500 kB). Warning only.
6. Maven's local repository and wrapper distribution live in `~/.m2` (outside the project). backend-dev flagged this; it is like the JDK and Node being installed outside the project, and fixing it would mean re-downloading every dependency. Recorded as assumption A-verify-M01-1 for the user to confirm.
7. Process: frontend-dev took 8 tasks in one iteration (limit 5), and its screenshots are in `run-artifacts/playwright/`, not in its evidence folder. No effect on the result.

## Fixes applied

None.

## FIX tasks

None.

## Contract issues

None logged for M01.

## Notes

- The curl suite (AC-M01-02) leaves configured settings in the db. To test first-run behaviour and empty states on a truly fresh db, I restarted the backend with `scripts/backend.sh restart --port 8090 --db stable --fresh` before the browser run. For the error state I stopped it (`stop --port 8090`) and started it again with `start --port 8090 --db stable` (db kept). It is running now with the settings saved by the E2E run (start page Tasks, zone Africa/Cairo).
- The headless permissions blocked shell output redirection and copying screenshots out of `run-artifacts/`. The test summary was taken from the surefire reports, and the screenshots are referenced where Playwright saved them.
- The browser console showed only the network lines for the deliberate 400 (server fieldError test) and the deliberate 500s (backend stopped). Normal flows had 0 console errors and 0 5xx.
