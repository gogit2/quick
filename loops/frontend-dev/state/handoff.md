# Handoff - frontend-dev

## Where M11 stands (trial 3, iteration 10): 7 of 8 tasks ticked, stuck on a user decision
- Ticked: T131, T132, T133, T135, T136, T137, T138.
- BLOCKED: T134. `ng build` prints a WARNING: initial 603.78 kB vs the 600 kB budget (transfer 150 kB, far under 500 kB).
- Iteration 10 finding: the earlier claim (A-frontend-dev-M11-2) that mat-nav-list pulls @angular/forms into the initial bundle is WRONG; no forms symbols are in any dist chunk. Importing only MatNavList/MatListItem/MatListItemIcon/MatListItemTitle instead of MatListModule saved just 0.35 kB (603.43 kB), so it was reverted. The remaining size is Angular core, router, Material (sidenav, toolbar, tooltip, button, icon, list) and polyfills (34.6 kB). Closing the 3.4 kB gap needs either a real design cut (e.g. drop MatTooltip or MatToolbar from the shell, re-verify M09) or the user raising `maximumWarning` in frontend/angular.json (e.g. 620kB).
- E2E-M11-07 still fails on four M01-M05 items (evidence/M11/regression-m01-m05.json): M01-02 (mock returns data), M02-05 (restore needs archived data), M05-02 (no items error text), M05-07 (partial). Verifier or user decides; scenarios must not be edited by this loop.
- E2E-M11-02 first-paint part not checkable with the MCP.

## USER DECISIONS (read requirements/DECISIONS.md D-25 and D-26) - APPLY THEM, then finish
- D-25: angular.json initial `maximumWarning` is now 650kB (already edited). Run `cd frontend && npx ng build`; it must print NO warning; copy the 'Initial total' line to evidence/M11/bundle.txt; do NOT change the budget again.
- D-26: ignore E2E-M01-02, M02-05, M05-02, M05-07 (stateless mock cannot pass them): in regression-m01-m05.json set `accepted_known_failure: true` + reason on those four; E2E-M11-07 is PASSED when only those four fail. The first-paint part of E2E-M11-02 is recorded as checked by code review.
- Then: network check for every page, tick T134, write result.json (all executed M11 scenarios passed), result `built`.

## Next (after the user decides)
1. Budget raised: `cd frontend && npx ng build`, copy the Initial total line to evidence/M11/bundle.txt.
2. Run the per-page network check (only /settings was captured) into evidence/M11/network.txt.
3. Tick T134 once the build has no warning.

## Traps
- Playwright MCP tools load with ToolSearch `select:mcp__playwright__browser_*`; reload per session.
- Permission layer denies compound bash (for-loops, variables, heredocs, redirects into run-artifacts/). One plain command per call, or Write/Edit.
- The mock is stateless. Material dialogs need a real `browser_press_key` Escape.
- index.html has two pre-paint background literals (#fcfcfd, #121214); documented exception in frontend/README.md.

## Commands that worked
- Build: `cd frontend && npx ng build` (about 30 s).
- Servers (repo root, one per call): `scripts/mock.sh start --port 4010`, `scripts/frontend.sh start --api http://localhost:4010 --port 4300`; stop with `stop --port`. Currently stopped.
