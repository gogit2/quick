# Verify report - M08 Design foundation, round 1

- **Milestone**: M08 (spec `specs/002-visual-polish`), trial 0
- **Round**: 1
- **Verdict**: **PASS** (after fix F1)
- **Time**: 2026-10-15T12:36:00+03:00
- **Stack**: backend :8090 (fresh `stable` db), frontend :4300 -> :8090

## Checks
| Check | Result | Evidence |
|---|---|---|
| Review (`git diff HEAD` + untracked) | pass (after F1) | T101-T111 all present in code (see Review notes) |
| Colour hygiene grep | pass | only `_palette.scss` (4 lines) and `_tokens.scss` (2 lines) |
| Lost test ids | pass | `git diff HEAD -- frontend/src/app` removes no `data-testid` / `testId` line |
| `npx ng build` | pass | no warnings; initial 587.8 kB raw / 144.2 kB transfer; exactly one Inter file `media/inter-latin-wght-normal-*.woff2` (48.3 kB) |
| Swagger | pass | `loops/verify/outputs/evidence/M08/round-1/` (export-swagger.sh, Result PASS) |
| Backend tests `./mvnw -q verify` | pass | 188 tests, 0 errors, 0 failures, 0 skipped (backend unchanged) |
| curl regression `--upto M08` | pass | `loops/verify/outputs/evidence/M08/round-1/curl.log` (7 suites M01-M07, 0 failed; no M08 suite, spec 002 is frontend-only) |
| E2E-M08-01..07 (real stack) | pass | `loops/verify/outputs/evidence/M08/round-1/e2e-result.md` |
| Smoke E2E-M01-01, M02-01, M03-01, M04-01, M05-01, M06-01 | pass | same file; screenshots `run-artifacts/playwright/verify-M08-r1-*.png` |
| Console / network | pass | no console errors, no 5xx, every request on localhost |

M08 has no `[real-stack-only]` scenarios. Empty states on the fresh db were still checked for contrast (Tasks, light and dark).

## Review notes
- T101 font: `@fontsource-variable/inter` 5.3.0, one `@font-face` Latin variable + size-matched `Inter Fallback`; package default CSS not imported.
- T102 theme: `mat.theme` cyan/violet with `theme-type: color-scheme`; azure-blue prebuilt theme removed from both `styles` arrays; `styles.scss` `@use` order theme, palette, tokens, typography, base, material.
- T103 tokens: all `--qf-*` tokens and `.qf-*` classes present. **Defect found**: `--qf-shadow-1/-2` were `light-dark(<shadow>, <shadow>)`; `light-dark()` only accepts colours, so any `box-shadow: var(--qf-shadow-*)` would be invalid (computed `none`). Not used yet (first use in M10), fixed as F1.
- T104 base/material: `color-scheme` rules per `data-theme`, focus rings via `--qf-focus-ring`, Material overrides only through `--mat-*` variables.
- T105/T106 literals replaced by `--qf-*` tokens in the listed files; colour grep clean.
- T107 icons: 26 SVGs, `IconService.registerAll()` from the app initializer, `src/assets` in angular.json. Deviation accepted: `settings.svg` 785 B and `light_mode.svg` 932 B exceed the 0.5 kB guideline (no functional impact; total 6.8 kB).
- T108 `ThemeService` (mode/effective signals, `qf.theme` in try/catch, `data-theme`), index.html color-scheme meta, neutral background and pre-paint script before the app bundle.
- T109 `ThemeToggle` with `theme-toggle`, `theme-option-light/dark/system`, `aria-pressed`, `aria-label`s, in the toolbar; no existing id touched.
- T110 contrast script present and usable; T111 scenario file `loops/frontend-dev/outputs/e2e/M08.md` present.
- Constitution: frontend-only change, no contract change, no backend change, no external dependency at runtime (font and icons self-hosted). OK.

## Fixes applied
| Id | File | What | Why | Lines |
|---|---|---|---|---|
| F1 | `frontend/src/styles/_tokens.scss` | `--qf-shadow-1/-2` rewritten as fixed shadow layers whose colours use `light-dark()` (light: soft black shadow, dark: transparent shadow + 1px `outline-variant` ring via new `--qf-shadow-ring`) | `light-dark()` accepts only `<color>`; the old values were invalid CSS when used | 5 |

Re-run after F1: `ng build` (pass, no warnings), colour grep (pass), browser: `var(--qf-shadow-1)` in dark computes to `rgba(0,0,0,0) 0 1px 2px 0, rgb(63,73,72) 0 0 0 1px`; contrast runs were done after the fix.

## FIX tasks
None.

## Contract issues
None for M08 (spec 002 does not change the API).

## Notes (non-blocking, for M09-M11)
- E2E-M08-04 dark-OS branch is checked by code review only (no media emulation in Playwright MCP 0.0.40).
- `index.html` sets `html[data-theme=...] { background: #fcfcfd / #121214 }`, which out-specifies `_base.scss` `html { background: var(--qf-bg) }`, so the `html` element keeps the pre-paint neutral colour (body covers it; visible only on overscroll). Could scope the inline style to the pre-bootstrap phase in M11 (theme change / first paint work).
- Light theme: the active nav item uses `--qf-accent-soft` = `primary-container`, a very saturated cyan. Contrast passes; M09 restyles the nav.
- `mat-toolbar color="primary"` has no effect under Material 3 (toolbar is surface-coloured); M09 owns the shell.
- The display name on the shared :8090 backend changed to "Ahmed AbdelDaiem" without this session saving Settings, probably external use of the stable stack; no impact.
