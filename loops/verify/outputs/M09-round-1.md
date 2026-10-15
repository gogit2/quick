# Verify report - M09 Shell and navigation, round 1

- **Milestone**: M09 (spec `specs/002-visual-polish`), trial 0
- **Round**: 1
- **Verdict**: **PASS** (after 1 small fix)
- **Time**: 2026-10-15T13:00:00+03:00
- **Stack**: backend :8090 (fresh `stable` db), frontend :4300 -> :8090

## Checks

| Check | Result | Evidence |
|---|---|---|
| Review (`git diff HEAD` + untracked) | pass | T112-T118 all exist: `core/nav-prefs.service.ts`, `layout/user-chip.component.ts`, `layout/shell.component.html/.scss/.ts`, `styles/_base.scss`, `styles/_material.scss`. All six `nav-*` test ids kept on the links. New ids: `menu-button`, `nav-collapse-toggle`, `user-chip`. No colour literals. No backend changes, no contract changes |
| Swagger | pass | `evidence/M09/round-1/swagger-diff.md` |
| `./mvnw -q verify` | pass | 188 tests, 0 errors, 0 failures, 0 skipped (`run-artifacts/verify-M09-r1-mvn.log`) |
| `npx ng build` | pass (with a warning) | builds; WARNING initial 615.45 kB > 600 kB budget (150.7 kB transfer). M08 was 587.8 kB with no warning. See Notes |
| curl regression M01..M07 (M09 has no BE suite) | pass | 7 suites, 0 failed: `evidence/M09/round-1/curl.log` |
| E2E-M09-01..08 (real stack) | pass | `evidence/M09/round-1/e2e-result.md`, screenshots `run-artifacts/playwright/verify-M09-r1-01..07-*.png` |
| Smoke (E2E-M01-01..M06-01, M08-01, M08-02) | pass | same file |
| Console / network | pass | no errors, no 5xx, localhost only |

## Fixes applied

- **F1** `frontend/src/styles/_material.scss` (+9/-4 lines): the snackbar surface stayed at 4 px radius instead of `--qf-radius-md`. Angular Material 19.2.19 reads `--mdc-snackbar-container-shape/-color/-supporting-text-color` for the surface, not `--mat-snack-bar-*`, and its component rule wins over the nested `.mdc-snackbar__surface` override. I set the `--mdc-snackbar-*` tokens on `html` and removed the override that had no effect. Re-checked: radius 12 px in light and dark, inverse colours unchanged, contrast 0 failures, `ng build` OK.

## FIX tasks

None.

## Contract issues

None for M09 (frontend-only milestone).

## Notes (non-blocking backlog)

- The bundle budget warning comes from M09 (icons, tooltip, layout and sidenav modules in the shell). `ng build` still passes, and T134 (M11) is the task that removes it (FR-V26/SC-V04: build without warnings, transfer < 500 kB; now 150.7 kB). M11 must clear it.
- Light theme: the active nav item uses the saturated `primary-container` cyan rgb(0,251,251). This carries over from the M08 backlog. It passes contrast but looks loud.
- The start banner sits above the sidenav container and spans the full width. This matches the design: the banner belongs to the shell, not the page.
- When the drawer opens below 960 px, focus moves to the first link (Material autofocus), which shows a grey focus background on "Dashboard". This is expected.
- The :8090 stack was shared with another client. It acknowledged plans 1-3 first, so the banner was proven with plan 4 (see e2e-result.md).
