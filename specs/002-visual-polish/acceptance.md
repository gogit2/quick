# Acceptance: QuickFlow Visual Polish (spec 002)

Spec 002 is frontend-only, so there are **no curl cases**: the backend and its curl suites are unchanged and the verifier keeps running them as a guard. All checks are Playwright scenarios (`E2E-Mxx-nn`), written by frontend-dev in `loops/frontend-dev/outputs/e2e/Mxx.md`, executed first against the Prism mock and again by the verifier on the real stack.

Tags: **[smoke]** = re-run by the verifier in every later milestone. **[real-stack-only]** = written by frontend-dev, executed only by the verifier (empty and error states need the real backend). Every case names the story (`US-VIS-NN`) or requirement (`FR-Vnn`, `SC-Vnn`) it proves. All earlier scenarios of spec 001 stay valid and are re-run as listed.

**Matrix for visual checks**: pages = Dashboard, Tasks, Habits, Learning, Plans, Settings; themes = light, dark (and system); widths = 1440x900, 1024x768, 390x844 (`browser_resize`). Theme is forced with `localStorage.setItem('qf.theme', '<mode>')` + reload. Contrast is measured with `loops/frontend-dev/outputs/e2e/contrast-check.js` through `browser_evaluate` (0 failures required). Horizontal scroll is checked with `document.documentElement.scrollWidth <= window.innerWidth`.

**Test ids**: every `data-testid` of spec 001 stays unchanged (FR-V25). New ids follow the same naming rule: `theme-toggle`, `theme-option-light`, `theme-option-dark`, `theme-option-system`, `menu-button`, `nav-collapse-toggle`, `user-chip`, `settings-theme-light`, `settings-theme-dark`, `settings-theme-system`.

---

## M08 Design foundation

| Id | Scenario | Proves |
|---|---|---|
| E2E-M08-01 **[smoke]** | The app loads with the new theme: primary buttons are teal, body `font-family` starts with `Inter Variable`, `document.fonts.check('16px "Inter Variable"')` is true | US-VIS-01, FR-V01, FR-V02 |
| E2E-M08-02 | The toggle offers Light, Dark, System; each sets `document.documentElement.dataset.theme` and the computed `color-scheme`; the active option has `aria-pressed="true"` | US-VIS-02, FR-V05 |
| E2E-M08-03 | After choosing Dark and reloading, `localStorage['qf.theme']` is `dark` and the page is dark on first paint; a corrupted value falls back to System; with no saved value System is selected | US-VIS-02, FR-V06, SC-V03 |
| E2E-M08-04 | System follows the OS: `matchMedia('(prefers-color-scheme: dark)').matches` equals the app's effective theme (the MCP cannot flip the OS setting, so this is checked by comparison) | FR-V05 |
| E2E-M08-05 | Contrast on Settings, Tasks and Dashboard in light and dark: 0 failures | US-VIS-14, FR-V24, SC-V01 |
| E2E-M08-06 | Self-contained: `browser_network_requests` shows only localhost URLs (font, icons, styles) | US-VIS-15, FR-V26, SC-V05 |
| E2E-M08-07 | All six nav pages still open (regression of E2E-M01-01) | FR-V25 |

---

## M09 Shell and navigation

| Id | Scenario | Proves |
|---|---|---|
| E2E-M09-01 **[smoke]** | At 1440 px each nav item shows an icon and a label and the active one has `aria-current="page"` and the accent highlight | US-VIS-03, FR-V07 |
| E2E-M09-02 | The collapse button turns the nav into a 72 px icon rail with tooltips; the choice survives a reload (`localStorage['qf.nav.collapsed']`); expanding restores labels | US-VIS-03, FR-V08 |
| E2E-M09-03 | At 390 px the nav is a drawer opened by `menu-button` and closed after choosing a page; `scrollWidth <= innerWidth` on all six pages | US-VIS-04, FR-V11, FR-V12, SC-V02 |
| E2E-M09-04 | At 1024 px the layout is side navigation without overlap or clipped controls | US-VIS-04, SC-V02 |
| E2E-M09-05 | The user chip shows the initials and name from Settings (mock name Alex), updates after saving Settings (real stack), and opens /settings | US-VIS-03, FR-V09 |
| E2E-M09-06 | The plan start banner and snackbars use the new styling and are readable in light and dark | US-VIS-03, FR-V10 |
| E2E-M09-07 | Contrast on the shell (toolbar + navigation) in light and dark: 0 failures | FR-V24, SC-V01 |
| E2E-M09-08 | Regression: E2E-M01-01 and E2E-M08-02 still pass | FR-V25 |

---

## M10 Pages

| Id | Scenario | Proves |
|---|---|---|
| E2E-M10-01 **[smoke]** | Each page renders its header, cards and rows with the shared look in light and dark (screenshots at 1440 px); cards, dialogs and panels share one radius, surface colour and shadow (compare computed `border-radius` and `box-shadow`); Tasks and Habits show every element listed in their restyle tasks | US-VIS-05, US-VIS-07..12, FR-V03, FR-V13, FR-V17, FR-V18 |
| E2E-M10-02 | The same status, priority and overdue state has the same colour, icon and text on Tasks, Dashboard and Plans; every chip contains text, so colour is never the only signal | US-VIS-05, FR-V14 |
| E2E-M10-03 | Loading skeletons: with a delayed XHR (`browser_evaluate` wrapping `XMLHttpRequest.prototype.send` with a 1500 ms delay), navigating to Tasks shows skeleton blocks inside `tasks-loading` and a 'Loading' status text, then the rows | US-VIS-06, FR-V16 |
| E2E-M10-04 | Learning: expand and collapse is smooth (`grid-template-rows` transition), milestones and notes keep their ids, the progress bar matches the milestone count | US-VIS-09, FR-V19 |
| E2E-M10-05 | Plans: rest time is the largest text on an in-progress card with progress beside it; the start highlight is visible (app clock set to 2026-10-15T09:30:00Z as in E2E-M05-06) | US-VIS-10, FR-V20 |
| E2E-M10-06 | Dashboard: four metric cards in one row at 1440 px, stacked at 390 px; sections in a balanced grid | US-VIS-11, FR-V21 |
| E2E-M10-07 | Settings: three sections (Profile, Preferences, Appearance); the Appearance radios change the theme and stay in sync with the toolbar toggle | US-VIS-12, US-VIS-02, FR-V22 |
| E2E-M10-08 **[real-stack-only]** | Fresh database: Tasks, Habits, Learning and Plans show their illustration, message and add action in light and dark | US-VIS-06, FR-V15 |
| E2E-M10-09 | Contrast on all six pages in light and dark: 0 failures | US-VIS-14, FR-V24, SC-V01 |
| E2E-M10-10 | Regression: re-run E2E-M02-01, E2E-M03-01, E2E-M04-01, E2E-M05-01, E2E-M06-01 (the [smoke] set of spec 001) | FR-V25, SC-V06 |

---

## M11 Polish and regression

| Id | Scenario | Proves |
|---|---|---|
| E2E-M11-01 **[smoke]** | Reduced motion: with `?reduceMotion=1` the computed `transition-duration` of a card and of the nav is `0s` and `document.getAnimations().length` is 0 after load; without the flag cards have a transition under 300 ms | US-VIS-13, FR-V23, SC-V08 |
| E2E-M11-02 | The theme change animates in under 300 ms and does not animate on first paint (no `qf-ready` class on load, present after the first frame) | US-VIS-13, FR-V23, SC-V03 |
| E2E-M11-03 | Contrast: 0 failures on all six pages in both themes (`contrast-after.json`) | US-VIS-14, FR-V24, SC-V01 |
| E2E-M11-04 | Keyboard: tabbing through Tasks and Settings, every control shows a focus ring (computed outline width >= 2 px) in both themes; the theme toggle exposes `aria-pressed`; every icon-only button has an `aria-label` | US-VIS-14, FR-V24 |
| E2E-M11-05 | Bundle and network: `npx ng build` prints no warnings and the initial transfer size is under 500 kB; every network request is on localhost; the only Inter file loaded is the Latin variable file | US-VIS-15, FR-V26, SC-V04, SC-V05 |
| E2E-M11-06 | Responsive: no horizontal scroll and no clipped control on the six pages at 390, 1024 and 1440 px, in both themes | US-VIS-04, FR-V11, SC-V02 |
| E2E-M11-07 | Full regression: `regression.json` shows every executable scenario of specs 001 and 002 passed with no console errors (the verifier additionally runs E2E-M07-01 and all [real-stack-only] scenarios on the real stack) | FR-V25, SC-V06 |

---

## Static checks run by the verifier

| Check | Command | Expected | Proves |
|---|---|---|---|
| Colour hygiene | `grep -rnE '#[0-9a-fA-F]{3,8}\b\|rgba?\(\|hsla?\(' frontend/src/app frontend/src/styles` | only lines from `frontend/src/styles/_palette.scss` and `_tokens.scss` | FR-V04, SC-V07 |
| Lost test ids | `git diff <previous tag> -- frontend/src/app` searched for removed `data-testid` / `testId` lines | none removed (renamed ones count as removed) | FR-V25 |
| Build | `cd frontend && npx ng build` | success, no warnings, initial transfer size under 500 kB | FR-V26, SC-V04 |
| Backend guard | `scripts/curl-regression.sh --upto M11` and `./mvnw verify` | unchanged results | D-19 |

---

## Traceability (every story is proved)

| Story | Scenarios |
|---|---|
| US-VIS-01 design tokens and identity | E2E-M08-01, M10-01 |
| US-VIS-02 theme switching | E2E-M08-02, M08-03, M08-04, M10-07, M11-02 |
| US-VIS-03 navigation shell | E2E-M09-01, M09-02, M09-05, M09-06 |
| US-VIS-04 small screens | E2E-M09-03, M09-04, M11-06 |
| US-VIS-05 shared building blocks | E2E-M10-01, M10-02 |
| US-VIS-06 empty and loading states | E2E-M10-03, M10-08 |
| US-VIS-07 Tasks | E2E-M10-01, M10-02, M10-10 |
| US-VIS-08 Habits | E2E-M10-01, M10-10 |
| US-VIS-09 Learning | E2E-M10-01, M10-04 |
| US-VIS-10 Plans | E2E-M10-01, M10-05 |
| US-VIS-11 Dashboard | E2E-M10-01, M10-06 |
| US-VIS-12 Settings | E2E-M10-07 |
| US-VIS-13 motion | E2E-M11-01, M11-02 |
| US-VIS-14 accessibility and contrast | E2E-M08-05, M09-07, M10-09, M11-03, M11-04 |
| US-VIS-15 performance and self-contained | E2E-M08-06, M11-05 |
| No regression (FR-V25) | E2E-M08-07, M09-08, M10-10, M11-07 |
