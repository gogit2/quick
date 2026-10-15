# Handoff - frontend-dev

## Where M10 stands (iteration 4, trial 0): 12 of 12 tasks ticked, built
- All of T119-T130 are ticked. T130 closed in iter 4 (scenarios E2E-M10-01..10, E2E-M10-08 real-stack-only).
- Iter 4 fixed the two Dashboard visual defects from iter 3: the overdue chip, the DONE chip and the habit streak text sit in the `actions` slot of `ui-list-row` (gap 222-309 px).
- `outputs/evidence/M10/result.json`: passed true, real_stack_only ["E2E-M10-08"].
- `npx ng build` passes (616.77 kB, budget WARNING as before; do not raise the budget).
- Servers stopped by the script. Nothing left running.
- If the verifier needs more: E2E-M10-08 (empty states) needs a fresh database on the real stack.

## Done in iter 4
- Dashboard fix (above). Dashboard light and dark screenshots re-taken after the fix.
- Smoke regression E2E-M10-10 (M02-01, M03-01, M04-01, M05-01, M06-01) executed on the mock.
- E2E-M05-06 clock check done for E2E-M10-05 (`window.__qfClock.setNow`).

## Shared blocks (use them, do not re-create)
- `shared/ui-card.component.ts` `<ui-card [interactive]>`; `shared/ui-page-header.component.ts` `<ui-page-header title subtitle titleTestId>` with an `[actions]` slot.
- `shared/ui-chip.component.ts` `<ui-chip kind="task-status|priority|plan-status|learning-status|overdue" [value] [testId]>`.
- `shared/ui-list-row.component.ts` with `[leading]` and `[actions]` slots. Put `data-testid` on the `<ui-list-row>` element, wrapped in `<li>` inside a `<ul>`. Anything after the title that should be spaced from it goes in `[actions]`, otherwise it touches the title.
- `shared/ui-skeleton.component.ts` `<ui-skeleton variant="line|card|row" [count]>` (has the 'Loading' status text).
- `shared/empty-state.component.ts` `illustration="tasks|habits|learning|plans|generic"`.

## Traps
- Permission layer denied `cp` and `mv` of screenshots into `outputs/evidence/M10/` (iter 1, 3, 4). Screenshots stay in `run-artifacts/playwright/`. Reference those paths in evidence JSON. Do not retry the copy.
- A Bash call that chains commands with `&&`, `|`, `;` or a heredoc gets denied. Keep one plain command per call.
- A radio click on the `mat-radio-button` host does not toggle it. Click the inner `input`.
- The HMR reload after editing a component blanks the Playwright page. Navigate again before reading.
- The mock is stateless: POSTs return 201 with the body echoed, but later GETs show seed data. Plans history shows the seed plan twice. Expected.
- Theme state persists (localStorage `qf.theme`). Reset to System (`button[aria-label="System theme"]`) after dark checks.
- Playwright tools load with ToolSearch `select:mcp__playwright__browser_navigate,mcp__playwright__browser_evaluate,mcp__playwright__browser_take_screenshot,mcp__playwright__browser_console_messages,mcp__playwright__browser_resize,mcp__playwright__browser_click`. Form fields in one `browser_evaluate` work with `el.value = v` plus `input` and `change` events.

## Commands that worked
- Build: `cd /home/ahmoh/Documents/proj/demo/QuickFlow/frontend && npx ng build`.
- Servers: `scripts/mock.sh start --port 4010`, then `scripts/frontend.sh start --api http://localhost:4010`. Stop with `scripts/frontend.sh stop --port 4300`, then `scripts/mock.sh stop --port 4010`. Each as its own Bash call.
- Contrast: `loops/frontend-dev/outputs/e2e/contrast-check.js` is one function. Paste it into one `browser_evaluate` with a loop over the toolbar theme buttons and `[data-testid="nav-<page>"]`.
