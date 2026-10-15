# M08 round 1 - browser results (Playwright MCP, real stack)

Stack: backend :8090 (fresh `stable` db at start), frontend :4300 -> :8090. Browser 1440x900, local time +03:00, 2026-10-15 ~12:31-12:34. OS colour scheme of the test browser: light.
Screenshots are in `run-artifacts/playwright/` (prefix `verify-M08-r1-`); copying into this folder is not permitted in the headless session.
The contrast script `loops/frontend-dev/outputs/e2e/contrast-check.js` was installed once as `window.__qfContrast` and run after SPA navigation (nav clicks); themes were switched by clicking `theme-option-*`.

## E2E-M08 scenarios
| Id | Result | Observed |
|---|---|---|
| E2E-M08-01 [smoke] | PASS | body font `"Inter Variable", "Inter Fallback", system-ui, ...`; `document.fonts.check('16px "Inter Variable"')` true; `--mat-sys-primary` = `light-dark(#006a6a, #00dddd)`; primary (raised) buttons `tasks-add-button` / `settings-save-button` have teal label `rgb(0,106,106)` light, `rgb(0,221,221)` dark, pill radius 9999px |
| E2E-M08-02 | PASS | Light: `data-theme=light`, `color-scheme: light`, body `rgb(247,250,249)`, only `theme-option-light` aria-pressed=true, storage `light`. Dark: `dark`/`dark`, body `rgb(16,20,20)`, only Dark pressed. System: `system`/`light dark`, only System pressed |
| E2E-M08-03 | PASS | No saved value (fresh profile, key removed) -> System pressed. `dark` stored + reload of /settings -> first evaluate: `data-theme=dark`, `color-scheme: dark`, html bg `rgb(18,18,20)`; the inline pre-paint script is script #1 in the document, the app bundle #3. Corrupted `x` + reload of /dashboard -> `data-theme=system`, System pressed (storage keeps `x` until the user chooses) |
| E2E-M08-04 | PASS (comparison) | `matchMedia('(prefers-color-scheme: dark)').matches` = false; effective theme from body background = light -> match. The dark-OS branch cannot be exercised (Playwright MCP 0.0.40 has no media emulation); code review: `ThemeService` listens to the `change` event and the CSS for `system` is `color-scheme: light dark`, so the browser resolves it |
| E2E-M08-05 | PASS | contrast-check on all six pages (with real data) in light and dark, 0 failures, 0 unknown: Dashboard 67, Tasks 49, Habits 24, Learning 21, Plans 28, Settings 21 elements per theme; focus ring 6.42 (light) / 10.94 (dark). Also Tasks empty state light/dark: 27 elements, 0 failures |
| E2E-M08-06 | PASS | `browser_network_requests`: every URL on `http://localhost:4300` (app, vite deps, `/media/inter-latin-wght-normal.woff2`, `/assets/icons/*.svg`, `/api/*`); no external host, no 5xx. All 26 `/assets/icons/<name>.svg` return 200 with an `<svg>` body |
| E2E-M08-07 | PASS | `nav-dashboard..nav-settings`: each route loads, `<page>-title` present, exactly one `aria-current="page"` = the clicked link |

## Smoke scenarios of earlier milestones (dark theme, real backend)
| Scenario | Result | Evidence |
|---|---|---|
| E2E-M01-01 | PASS | six nav links, see E2E-M08-07 |
| E2E-M02-01 | PASS | Save disabled when empty, enabled with "Pay rent"; dialog closes; `tasks-row-1` "Pay rent · To do · Medium" |
| E2E-M03-01 | PASS | "Read 20 pages" Daily -> `habits-card-1` "0 days streak · Not done today · Mark done" |
| E2E-M04-01 | PASS | "Kubernetes basics" -> `learning-card-1` "Not started · No milestones yet" |
| E2E-M05-01 | PASS | Plan "Morning block" from task 1 + habit 1 + card 1 (Save disabled before picks), default start 12:33, end 13:33, 60 min; banner "Plan Morning block has started" at 12:33:03, `plans-highlight-1`; card "In progress · Rest 59m 43s" at 12:33:16. Screenshot `verify-M08-r1-smoke-M05-banner-dark.png` |
| E2E-M06-01 | PASS | after seeding a due-today and an overdue task (POST 201): metrics Due today 1, Overdue 1, 0% (0 of 3), Habits 0 of 1, Active plans 1; `dashboard-task-2`, `dashboard-overdue-3`, `dashboard-habit-1`, `dashboard-plan-1` with rest time. Screenshot `verify-M08-r1-dashboard-light.png` |

## Other
- Screenshot `verify-M08-r1-tasks-empty-dark.png`: empty state on the fresh db in dark.
- Console: only `[DEBUG] [vite]` and `[LOG] Angular is running in development mode`; no errors or warnings.
- Fix F1 re-check: `box-shadow: var(--qf-shadow-1)` in dark computes to `rgba(0,0,0,0) 0 1px 2px 0, rgb(63,73,72) 0 0 0 1px` (before the fix the value was invalid).
- The display name on the shared :8090 backend changed to "Ahmed AbdelDaiem" between two page loads without this session saving Settings; probably someone else used the stable stack. No effect on the checks.
