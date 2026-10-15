# Handoff - frontend-dev

## Where M05 stands (iteration 4, trial 0): built, all 7 tasks ticked
- T069-T075 ticked. `npx ng build` passes (500 kB budget warning only). Both servers stopped by script.
- E2E-M05-01..08 executed on the Prism mock with the Playwright MCP, all passed. Results in `outputs/evidence/M05/result.json`. E2E-M05-09 is `real_stack_only`, for the verifier.
- Screenshots are in `run-artifacts/playwright/` (the screenshot tool cannot write into `outputs/evidence/`): `E2E-M05-04-groups.png`, `E2E-M05-07-start-banner.png`.

## Code changed in iteration 4
- `frontend/src/app/core/api/plans.api.ts`: `acknowledgeStart` posts with `null` (no body). The contract operation has no requestBody; a JSON body gets 415 from the mock.

## Traps for the verifier (real stack)
- The mock is stateless: the same example plan (id 1, IN_PROGRESS, startNotified false, 09:00Z-11:00Z) is returned for both groups, and PUT/DELETE do not change later GETs. So "progress 33%" after a toggle, and the card after a delete, are mock artefacts. Check the real flows on the real stack.
- Notifications off: the flag lives in `SettingsStore.settings` (signal). It was only overridden in the browser for the test; it was not persisted.
- Start notice: a plan is announced when its cached server status is not NOT_STARTED and the app clock is at or after its start. Clock skew can delay it up to 30 s (sync interval). Assumption A-frontend-dev-M05-3.
- Empty-state add button is `plans-empty-add-button` (assumption A-frontend-dev-M05-1). Habit picker shows active habits only (A-frontend-dev-M05-2).
- `[attr.data-testid]` on `mat-checkbox` sits on the host element. Click the inner `input`.
- The browser `console_messages` list is cumulative for the session; a 415 from before the body fix stays in it.

## Commands that worked
- `scripts/mock.sh start` and `scripts/frontend.sh start --api http://localhost:4010`, one per Bash call. Stop with `scripts/frontend.sh stop`, then `scripts/mock.sh stop`.
- `npx ng build` from `frontend/`.
- Playwright: navigate, then evaluate in a separate call (a parallel navigate and evaluate loses the context). Install XHR hooks after each navigation. Set the clock with `window.__qfClock.setNow('2026-10-15T09:30:00Z')`.
