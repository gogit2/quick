# Handoff - frontend-dev

## Where M11 stands (trial 0, iteration 11): all 8 tasks ticked, built
- Ticked: T131-T138. T134 done after D-25 (budget 650 kB): `ng build` prints no warning, initial 603.78 kB raw / 150.28 kB transfer (evidence/M11/bundle.txt).
- Network check done on all six pages: every request on localhost (evidence/M11/network.txt).
- E2E-M11-02 first-paint part is recorded as checked by code review (D-26).
- Regression M01-M05: the four D-26 items (E2E-M01-02, M02-05, M05-02, M05-07) are marked accepted_known_failure with reason in evidence/M11/regression-m01-m05.json. E2E-M11-07 passes under D-26.
- evidence/M11/result.json: passed true.
- Not measured: the text-shift part of T134 (no layout-shift measurement with the MCP; relies on the M08 fallback metrics).

## Next
- Nothing open in frontend-dev for M11. Verifier runs the real-stack-only scenarios (E2E-M02-08, E2E-M03-07, E2E-M04-07, E2E-M05-09, E2E-M07-01, E2E-M07-03, E2E-M10-08) and the four D-26 items are not re-checked against the restyled UI.

## Traps
- Playwright MCP tools load with ToolSearch `select:mcp__playwright__browser_*`; reload per session.
- Permission layer denies compound bash (for-loops, variables, heredocs, redirects into run-artifacts/). One plain command per call, or Write/Edit.
- The mock is stateless. Material dialogs need a real `browser_press_key` Escape.
- index.html has two pre-paint background literals (#fcfcfd, #121214); documented exception in frontend/README.md.
- Screenshots from the MCP land in run-artifacts/playwright/, not in evidence/ folders (copy is denied).

## Commands that worked
- Build: `cd frontend && npx ng build` (about 10 s with cache).
- Servers (repo root, one per call): `scripts/mock.sh start --port 4010`, `scripts/frontend.sh start --api http://localhost:4010 --port 4300`; stop with `scripts/frontend.sh stop --port 4300` and `scripts/mock.sh stop --port 4010`. Both stopped at the end of iteration 11.
