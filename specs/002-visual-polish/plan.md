# Implementation Plan: QuickFlow Visual Polish

**Branch**: `002-visual-polish` (spec folder only) | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-visual-polish/spec.md`; decisions `requirements/DECISIONS.md` D-19..D-24 (settled, no clarification round).

## Summary

Restyle the existing Angular 19 + Material app: one design-token layer on top of Angular Material 3 system tokens (`mat.theme` with a teal/indigo palette and `color-scheme` light/dark), self-hosted Inter, a `ThemeService` (light / dark / system, saved in the browser, applied before first paint), a redesigned responsive shell, shared building blocks (card, chip, list row, page header, skeleton, illustrations, SVG icons), and a restyle of the six pages. **Frontend only**: no backend, API contract or `data-testid` change (D-19). Four milestones, M08-M11, each with FE tasks only.

## Technical Context

**Language/Version**: TypeScript 5.x, Angular 19.2, SCSS (Sass modules)

**Primary Dependencies**: Angular Material and CDK 19.2.19 (installed), new: `@fontsource-variable/inter` 5.3.0 (font files only)

**Storage**: browser `localStorage` for two preferences (`qf.theme`, `qf.nav.collapsed`); no server data

**Testing**: Playwright MCP 0.0.40 scenarios (screenshots, contrast script, scroll-width check, network log), `ng build` size and warnings, two static scripts (`testid-check.sh`, `colour-check.sh`); backend curl and JUnit suites re-run unchanged as a guard

**Target Platform**: Chromium 1440x900 and 1024x768 and 390x844 viewports

**Project Type**: web application, frontend only

**Performance Goals**: initial transfer size under 500 kB (136.55 kB today), theme switch under 300 ms, no layout shift from the font (`font-display: swap` plus a size-matched fallback)

**Constraints**: Node 18.20; no external requests; no behaviour change; every `data-testid` kept

**Scale/Scope**: 6 pages, about 10 shared components, about 25 icons, 2 themes

## Constitution Check

*GATE: passed before research and again after design.*

| Principle | How the plan complies |
|---|---|
| I Contract-first | No contract change: spec 002 reuses `specs/001-quickflow/contracts/openapi.yaml` unchanged (see "Runner note"); loops still cannot edit it. |
| II Fixed stack | Angular 19 + Material, Node 18.20; one new npm dependency (font files), nothing outside the folder; the backend is untouched. |
| III Separation | Changes only in `frontend/`; presentational components only, no business rules move; the shared components take inputs and emit events, pages keep their logic. |
| IV Tests | Playwright scenario per milestone (screenshots, contrast, scroll width, network), full regression in M11; no BE tests added because no BE code changes; the BE suites still run in the verifier. |
| V Controllable time | `ClockService` and its overrides are untouched; `?reduceMotion=1` follows the same test-flag pattern. |
| VI Test hooks | Baseline of existing ids in `testids.baseline.txt`, enforced by `scripts/testid-check.sh`; new elements follow `<page>-<element>[-<qualifier>]`. |
| VII Autonomous loops | Decisions D-19..D-24 settled; remaining choices (exact hues, illustration style) are recorded as assumptions by the loops. |

No violations, so no Complexity Tracking section.

## Architecture

### Style layer (`frontend/src/styles/`)
```
styles.scss              entry: @use the files below in order (replaces the prebuilt azure-blue.css in angular.json)
styles/_palette.scss     the only place with colour literals (accent hues for status colours) and the palette choice
styles/_theme.scss       one `mat.theme(...)` call: primary $cyan-palette, tertiary $violet-palette, theme-type color-scheme, Inter, density 0
styles/_tokens.scss      --qf-* tokens: colour roles (light-dark()), spacing, radius, shadow, type scale, motion, layout, breakpoints
styles/_typography.scss  @font-face for 'Inter Variable' (Latin wght file only), font stacks, type utility classes
styles/_base.scss        html/body, scrollbars, focus ring (--qf-focus-ring), links, selection, color-scheme per data-theme
styles/_material.scss    overrides of Material component tokens through --mat-* variables only (buttons, chips, form fields, dialogs, snackbar, sidenav, list)
styles/_motion.scss      transitions utilities and the reduced-motion block
```
`index.html` gets the inline pre-paint script, `<meta name="color-scheme" content="light dark">` and an inline background per scheme (research R3).

### Services and components (`frontend/src/app/`)
```
core/theme.service.ts     signals mode (light|dark|system) and effective theme; setMode(); persists qf.theme (try/catch); sets data-theme; reads ?reduceMotion=1
core/nav-prefs.service.ts signal collapsed (qf.nav.collapsed); drawer open state; BreakpointObserver for < 960 px
core/icon.service.ts      registers assets/icons/*.svg with MatIconRegistry
layout/shell.*            redesigned toolbar (menu button < 960 px, title, ThemeToggle, UserChip) and sidenav (icons + labels, rail when collapsed, drawer when narrow)
layout/theme-toggle.component.ts   3-state toggle (light / dark / system) with icons and aria-labels, data-testid theme-toggle, theme-option-<mode>
layout/user-chip.component.ts      name or initials from SettingsStore, data-testid user-chip
shared/ui-card.component.ts        surface, radius, shadow, hover/focus feedback
shared/ui-chip.component.ts        status/priority chip with icon + text (table in data-model.md)
shared/ui-list-row.component.ts    row layout, hover/focus, actions slot
shared/ui-page-header.component.ts title, subtitle, primary action slot
shared/ui-skeleton.component.ts    line / card / row placeholders (shimmer off under reduced motion)
shared/empty-state.component.ts    (existing) restyled, takes an illustration name, keeps its testId inputs
shared/illustrations/*             inline SVG components, currentColor
assets/icons/*.svg                 ~25 icons (research R10)
```
Page components keep their TypeScript logic, their `data-testid` attributes and their API calls; only templates and SCSS change, and templates adopt the shared components **without removing or renaming any attribute that carries a `data-testid`**. Shared components take the test id as an input (`testId`) and set it on the same element kind as before.

### Responsive rules
Phone < 600: single column, toolbar with menu button, drawer navigation, dialogs full width, filter toolbar wraps. Tablet 600-959: drawer navigation, two-column grids where useful. Desktop >= 960: side navigation (expanded 240 px or collapsed 72 px rail), dashboard grid 4 metric cards in one row. Wide >= 1280: content max width 1200 px centred. No horizontal scroll at any width (SC-V02).

### Motion rules
Only `opacity`, `transform` and `grid-template-rows` transitions, durations from tokens (< 300 ms), one easing; no animation on first paint; skeleton shimmer under 1.5 s loop. Everything off under `prefers-reduced-motion: reduce` or `?reduceMotion=1` (research R7).

### Accessibility and contrast method
Contrast is measured, not assumed: `contrast-check.js` via `browser_evaluate` per page and theme (research R6), plus keyboard tab-through with visible focus ring and `aria-label` on every icon button, `aria-pressed`/`aria-checked` on the theme toggle, `aria-current="page"` on the active nav link, status chips with text and icon (never colour alone).

### Regression strategy
1. `scripts/testid-check.sh` fails on any lost `data-testid` (baseline generated before M08).
2. `scripts/colour-check.sh` fails on colour literals outside the token files.
3. Verifier re-runs every `[smoke]` scenario of spec 001 in M08-M10 and **all** E2E-M01..M07 scenarios in M11; BE curl regression and JUnit run unchanged.
4. Each milestone adds its own scenarios `E2E-M08-xx`.. with screenshots in both themes.

### Runner note (planning-time change)
Spec 002 has no `contracts/openapi.yaml`. A file `specs/002-visual-polish/contracts/contract-source.txt` names the contract to use, and `loopctl.py get-contract` resolves it; `mock.sh`, `export-swagger.sh` and the loops' `task.md` use it. The runner also needs `scripts/testid-check.sh`, `scripts/colour-check.sh`, `scripts/bundle-size.sh` (runner-owned, added in the tasks step).

## Milestones (FE-only; BE loop finds 0 tasks and finishes at once)

| Milestone | Scope | Spec stories | FE tasks (approx.) |
|---|---|---|---|
| **M08 Design foundation** | Dependency and font, remove prebuilt theme, `mat.theme`, tokens, typography, base styles, `ThemeService` + pre-paint script + toggle in the existing toolbar, icon service and assets, contrast-check script, colour hygiene across existing components (replace hard-coded colours) | US-VIS-01, 02, 15 (font part) | 9 |
| **M09 Shell and navigation** | Redesigned toolbar and sidenav (icons, collapsible rail, drawer < 960 px), `NavPrefs`, `UserChip`, restyled start banner and snackbars, responsive container | US-VIS-03, 04 | 7 |
| **M10 Pages** | Shared `UiCard`, `UiChip`, `UiListRow`, `UiPageHeader`, `UiSkeleton`, restyled `EmptyState` with illustrations; restyle Tasks, Habits, Learning, Plans, Dashboard, Settings (+ theme choice); loading skeletons | US-VIS-05, 06, 07-12 | 14 |
| **M11 Polish and regression** | Motion + reduced motion, contrast fixes found by the measurement, focus rings, bundle check (transfer size, warnings), 390/1024/1440 screenshot review in both themes, full E2E-M01..M07 regression, README section | US-VIS-13, 14, 15 | 7 |

Dependencies: M09 needs M08 tokens and icons; M10 needs M08 and M09; M11 needs all. The pipeline runs them in order.

## Project Structure

### Documentation
```text
specs/002-visual-polish/
├── spec.md  plan.md  research.md  data-model.md  quickstart.md
├── contracts/README.md  contracts/contract-source.txt     # points to spec 001's contract
├── testids.baseline.txt                                    # existing data-testid values (generated in the tasks step)
├── acceptance.md                                           # E2E-M08..M11 scenarios (after tasks)
└── tasks.md                                                # speckit-tasks
```

### Source code (frontend only)
```text
frontend/src/styles/{_palette,_theme,_tokens,_typography,_base,_material,_motion}.scss
frontend/src/styles.scss  frontend/src/index.html  frontend/angular.json  frontend/package.json
frontend/src/assets/icons/*.svg
frontend/src/app/{core,layout,shared,features}/...   (as listed above)
scripts/{testid-check,colour-check,bundle-size}.sh   # runner-owned
loops/frontend-dev/outputs/e2e/{M08..M11}.md  contrast-check.js
```

## Testing strategy (constitution IV)

| Level | What | Where |
|---|---|---|
| Static | `ng build` (no warnings, size lines), `testid-check.sh`, `colour-check.sh`, `bundle-size.sh` (transfer size under 500 kB) | scripts, runner gate and verifier |
| Playwright MCP | Per milestone: pages in light/dark/system at 1440/1024/390 px, screenshots, `contrast-check.js` 0 failures, scroll-width check, theme persistence after reload, network all `localhost`, keyboard tab-through | `loops/frontend-dev/outputs/e2e/M08..M11.md` |
| Regression | All 001 `[smoke]` scenarios every milestone; all E2E-M01..M07 in M11 | verifier |
| Backend guard | JUnit and curl regression unchanged | verifier |

## Risks
1. **Dark-mode contrast** is the likeliest cause of a REJECT; mitigated by measuring from M08 and a fallback palette (`$azure-palette`).
2. **Test id loss** while restructuring templates; mitigated by the baseline check as a runner gate.
3. **Bundle growth** from shared components; mitigated by tokens in global CSS, small SVGs and the transfer-size check.
4. **Haiku restyling many files** may drift from the tokens; mitigated by `colour-check.sh` and the shared components.

## Complexity Tracking

Not applicable (no constitution violations).
