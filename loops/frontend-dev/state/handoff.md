# Handoff - frontend-dev

## Where M09 stands (iteration 2, trial 0): built, 7 of 7 tasks ticked
- T112-T116 ticked in iteration 1. T117 (responsive container) and T118 (scenarios, screenshots, evidence) ticked in iteration 2.
- Scenarios: `outputs/e2e/M09.md` executed on the Prism mock: E2E-M09-01..08 all pass, plus regression E2E-M01-01 and E2E-M08-02.
- Evidence: `outputs/evidence/M09/` (6 screenshots + result.json, passed: true, real_stack_only: []).
- `npx ng build` passes. Bundle WARNING remains: initial 615.54 kB vs 600 kB budget (M11 E2E-M11-05 needs no warning and under 500 kB).
- Servers stopped by script (4300, 4010). Nothing left running.

## Fixed in iteration 2 (keep these, do not revert)
- `shell.component.scss`: removed `.sidenav { position: relative }`. It overrode Material's absolute drawer and pushed the page content down.
- `shell.component.scss` `:host` is a flex column; `.container` uses `flex: 1 1 auto`. The banner no longer pushes the collapse toggle below the viewport.
- Rail content offset: Material sets the content margin inline and ignores CSS width changes. `mat-sidenav-container` gets `side-layout` / `rail-layout` classes (shell.component.html) and a `margin-left` rule on `.mat-drawer-content` uses `--qf-content-offset`.

## Not verified yet
- Snackbar styling (not in M09 acceptance; not exercised).
- Tooltip hover on the rail (checked in iteration 1 only).
- Contrast: dark run was on /tasks, light run on /settings (script is page-wide, not shell-only).

## Known issues to keep in mind
- Active nav background is the Material primary container (bright cyan, rgb(0,251,251) in light). Same visual nit as M08. Tune tokens later, not here.
- The 390 px toolbar is crowded (menu, theme toggle x3, avatar). Looks acceptable in the screenshot; revisit in M11's screenshot review.
- Initial bundle over budget (see above). Do not raise the budget again without a reason.

## Traps
- MCP `browser_navigate` and `browser_click` return very large snapshots. Prefer `browser_evaluate` with `element.click()` on data-testids.
- Reloading inside an evaluate you are waiting on destroys the context: use `setTimeout(() => location.reload(), 50)` and read in a second call.
- `browser_take_screenshot` only writes into `run-artifacts/playwright/` (a relative filename). Move the files into `outputs/evidence/M09/` with Bash afterwards.
- A compound Bash command that contains shell variables was denied by the permission check. Keep each Bash call to one simple command.
- Measure the content left edge at >= 1280 px: `main.content` is centred (max-width 1200 px), so a 72 px rail gives left 156, not 72.

## Commands that worked
- Build: `cd frontend && npx ng build`.
- Servers: `scripts/mock.sh start --port 4010`, `scripts/frontend.sh start --api http://localhost:4010`; stop with `scripts/frontend.sh stop --port 4300`, `scripts/mock.sh stop --port 4010`.
- Viewport: `mcp__playwright__browser_resize` (load via ToolSearch first).
