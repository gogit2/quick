# Handoff - verify

## Last iteration
- M11 round 1: **PASS** with fixes F1 (sidenav `&.mat-drawer` transition with motion tokens; Material's 400 ms `.mat-drawer-transition .mat-drawer` rule had overridden it) and F2 (dashboard recent-milestone date in the `actions` slot). Report: `loops/verify/outputs/M11-round-1.md`. M11 is the last milestone of spec 002.
- Stack left running (backend :8090 fresh db after curl, frontend :4300) for the runner to stop.

## Recommended order (saves restarts)
1. `cd backend` as its own call, then `./mvnw -q verify > ../run-artifacts/verify-Mxx-rN-mvn.log 2>&1` with run_in_background (no `; echo $?` - denied), then `cd ..` separately.
2. Swagger export (read-only). `cd frontend` + `npx ng build > ../loops/verify/outputs/evidence/...` as separate calls, then `cd ..`.
3. Browser first while the db is fresh: empty states, backend-down error state, then the journey/milestone scenarios. Multi-page checks run fastest as one `browser_evaluate` that clicks `nav-<page>` and polls for test ids (SPA navigation keeps window state).
4. Start banner: create the plan for the next minute + 1; poll for `plans-start-banner` in an evaluate (< 110 s). The card status/rest appear only with the 30 s page refresh.
5. Backend down: `scripts/backend.sh stop --port 8090`, then `scripts/backend.sh start --port 8090 --db stable --no-build --jar run-artifacts/backend-latest.jar`. Fresh for curl: stop, then the same start with `--fresh`.

## Traps and commands that worked (headless permissions)
- Denied: `$?`/`$VAR`, `git -C`, `cp` into evidence, heredoc edits. Write evidence with the Write tool.
- Screenshot `filename` must be a bare name; files land in `run-artifacts/playwright/`.
- Form fields: set `.value` + `input` event ~700 ms after the dialog opened (works for tasks/habits/learning/plans forms incl. datetime-local).
- `performance.getEntriesByType('resource')` with `responseStatus` gives the 5xx list with times; separate the intentional backend-down window by timestamp.
- Material 19.2 rules can silently win over component SCSS at equal specificity (drawer transition, snackbar tokens); check the computed style, not the source.
- Small-fix limit is 3 files in total per round.

## Open, non-blocking (backlog)
- Spec 002: empty-state add button `mat-flat-button` (T122). Light active nav = saturated `--qf-accent-soft` cyan. index.html `html[data-theme]` neutral background out-specifies `--qf-bg`. settings.svg/light_mode.svg > 0.5 kB. E2E-M08-04 dark-OS branch and E2E-M11-02 first paint only by review. Inter text shift not measured.
- Spec 001: plan card status/rest update only on the 30 s refresh after the start (banner is immediate); Settings form editable with defaults when loading fails; greeting uses browser hour; dashboard checklist lists done weekly habits; Complete button on DONE tasks; new plan items start unticked even if the source is done; Habits "Show inactive" starts on; task status reads "Todo".
