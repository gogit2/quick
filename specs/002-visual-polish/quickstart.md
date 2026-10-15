# Quickstart: validating the visual polish

Prerequisites as in spec 001 ([quickstart](../001-quickflow/quickstart.md)). Design tokens and preferences: [data-model.md](data-model.md). API contract: unchanged ([contracts/README.md](contracts/README.md)).

## 1. Static checks (fast, no browser)
```bash
(cd frontend && npx ng build)            # production build: no warnings, note "Initial total" raw + transfer size
scripts/testid-check.sh                  # no data-testid of spec 001 disappeared (baseline in testids.baseline.txt)
scripts/colour-check.sh                  # no colour literal outside frontend/src/styles/_palette.scss and _tokens.scss
```
Expected: build green with no warnings; transfer size under 500 kB (about 140 kB today); both scripts print `PASS`.

## 2. Look and feel (Playwright MCP, mock or real stack)
```bash
scripts/mock.sh start && scripts/frontend.sh start --api http://localhost:4010
```
For each of the six pages, in `light`, `dark` and `system`, at 1440, 1024 and 390 px (`browser_resize`):
1. Take a screenshot into `loops/frontend-dev/outputs/evidence/M0x/`.
2. Run the contrast script with `browser_evaluate` (`loops/frontend-dev/outputs/e2e/contrast-check.js`): expect 0 failures.
3. `document.documentElement.scrollWidth <= window.innerWidth` at 390 px: expect true.
4. Switch the theme with the toolbar toggle: the page changes at once; reload: the same theme is applied with no flash (the inline script sets it before paint).
5. `browser_network_requests`: every request goes to `localhost`.

## 3. Regression
```bash
scripts/pipeline.sh                      # M08..M11: FE only; verifier re-runs all [smoke] scenarios and, in M11, E2E-M01..M07
```
Expected: every scenario of spec 001 still passes with no console errors; `scripts/curl-regression.sh --upto M11` still passes (the backend did not change).
