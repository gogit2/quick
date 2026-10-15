# Loop: frontend-dev

**Role**: implement the frontend of the current milestone, feature by feature, against the backend Swagger (`contracts/openapi.yaml`); test every feature in a real browser with the **Playwright MCP**.
**Input**: a spec folder (`specs/NNN-*/`: spec, plan, `contracts/openapi.yaml` = backend Swagger, acceptance) generated from a full PRD or a single user story, plus your milestone file.
**Output**: frontend code in `frontend/`, ticked milestone file `outputs/Mxx-*.md`, scenario file `outputs/e2e/Mxx.md`, evidence `outputs/evidence/Mxx/` (screenshots, `result.json`), action log in `progress.md`, `state/handoff.md`, `state/result.json`.

Follow `loops/_shared/PROTOCOL.md` for the iteration cycle. This file adds the frontend rules.

## Allowed to edit
`frontend/**`, `loops/frontend-dev/**` (except `progress.md`'s runner table and `state/state.json`), and append-only `loops/_shared/assumptions.md` and `loops/_shared/contract-issues.md`.

## Stack rules
- Angular **19** (standalone components, signals welcome) + Angular Material, Node 18.20. Create the app in the first milestone with `npx -y @angular/cli@19 new quickflow --directory frontend --routing --style=scss --skip-git --ssr=false --defaults`, then `npx ng add @angular/material@19 --skip-confirmation`.
- All HTTP calls use relative URLs under the contract's base path. The dev server proxies them using `frontend/proxy.conf.js`, which reads the target from the env var `API_TARGET` (default `http://localhost:8090`). `scripts/frontend.sh` sets it.
- One typed API service per feature area; TypeScript types mirror the contract schemas exactly (names, enums, nullability).
- UI per PRD section 8: persistent navigation with the six pages, empty states that point to the add action, Material components.
- **`data-testid` on every element a test touches**, named `<page>-<element>[-<qualifier>]`, e.g. `tasks-add-button`, `tasks-row-<id>`, `nav-habits`, `dashboard-metric-tasks`. Ids listed in `acceptance.md` are mandatory.
- Time-dependent UI (rest time, start notification) reads time through one injectable clock service so tests can control it.
- No console errors in normal use.

## Self-check (run before ticking; full run before `built`)
1. `cd frontend && npx ng build` must succeed with no errors.
2. `scripts/mock.sh start` (Prism mock of the contract on :4010) and
   `scripts/frontend.sh start --api http://localhost:4010` (dev server on :4300).
3. Write/extend `loops/frontend-dev/outputs/e2e/Mxx.md` (the scenarios of this milestone from `acceptance.md`, step by step, with the `data-testid`s used), then **execute them with the Playwright MCP tools** (`browser_navigate`, `browser_snapshot`, `browser_click`, `browser_type`, `browser_take_screenshot`, `browser_console_messages`, …) against `http://localhost:4300`.
   - The mock is **stateless**: a POST does not change later GETs. Check that requests are sent and the UI handles the mocked responses; state-dependent flows are proven later by the verifier on the real backend.
   - Take a screenshot per scenario into `loops/frontend-dev/outputs/evidence/Mxx/`. Check `browser_console_messages` for errors.
   - Scenarios tagged `[real-stack-only]` (empty states, error states: the mock always returns data) are **written but not executed**: put their ids in `real_stack_only` in result.json. The verifier executes them on the real stack.
4. Write `loops/frontend-dev/outputs/evidence/Mxx/result.json`:
```json
{ "milestone": "Mxx", "passed": true, "finished_at": "<ISO time>",
  "scenarios": [ { "id": "E2E-M02-01", "passed": true, "steps": 7, "screenshot": "…png", "console_errors": 0, "notes": "" } ],
  "real_stack_only": ["E2E-M02-08"] }
```
5. `scripts/frontend.sh stop` and `scripts/mock.sh stop`.

## Done means
All `[FE]` tasks of the milestone ticked with evidence, `ng build` green, every executable scenario of the milestone passed with the Playwright MCP against the mock (real-stack-only ones are written and listed), `result.json` written in this iteration with `passed: true`.
