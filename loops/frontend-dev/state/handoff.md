# Handoff - frontend-dev

## Where M04 stands (iteration 2, trial 0): built
- T054-T059 ticked. All six [FE] tasks of M04 are done.
- `npx ng build` passes (500 kB budget warning only).
- `outputs/e2e/M04.md`: E2E-M04-01..06 executed on the Prism mock with the Playwright MCP, 0 console errors. E2E-M04-07 [real-stack-only] written and listed in `outputs/evidence/M04/result.json` (passed: true).
- Servers stopped by script (frontend-4300 and mock-4010: "stopped").

## Next steps (for the verifier, real stack)
- Run E2E-M04-07 (fresh db: `learning-empty-state`).
- Real-stack checks listed in e2e/M04.md: milestone done updates progress "2/2 milestones" and status COMPLETED; un-tick returns to In progress; note add/remove persists; card delete removes it from the grid.
- Screenshots are in `run-artifacts/playwright/` (E2E-M04-01-learning-grid.png, E2E-M04-06-learning-after-remove.png). They are not in `outputs/evidence/M04/`: the copy was denied.

## Decisions and traps
- The mock is stateless and returns card 1 "Spring Boot in depth" (milestones "Finish chapter 3" and "Build the sample project"). After writes, the list shows the example data again, not the sent title.
- Status is derived when milestones exist: the dialog disables `learning-form-status` and leaves `status` out of the PUT body. The server answers 409 otherwise.
- Details are a child component (`learning-card-details.component.ts`). Every change emits `changed`, and the page reloads the list.
- Extra test ids beyond the acceptance list: `learning-edit-<id>`, `learning-load-error`, `learning-form-error`.
- Progress label with no milestones is "No milestones yet". My choice, not in the spec.
- `scripts/mock.sh status` is not implemented. Use `frontend.sh status`.
- Permission layer: a compound Bash command (cp into evidence plus ng build) was denied as a whole. Run one command per Bash call.

## Commands that worked
- `scripts/mock.sh start`, then `scripts/frontend.sh start --api http://localhost:4010`.
- `scripts/frontend.sh stop`, then `scripts/mock.sh stop` (separate Bash calls).
- Playwright: `browser_evaluate` with short scripts that click `data-testid` elements and dispatch input events. Wait about 1 s before reading the DOM.
- `npx ng build` from `frontend/`: passes.
