# Verify M10 - Pages, round 1

- Milestone: M10 (spec `specs/002-visual-polish`), trial 0, model claude-opus-5-5
- Verdict: **PASS** (after 2 small fixes)
- Time: 2026-10-15T13:35:00+03:00
- Stack: backend :8090 (`run-artifacts/backend-latest.jar`, fresh `stable` db), frontend :4300 -> :8090
- Screenshots: `run-artifacts/playwright/verify-M10-r1-*.png` (copying into the evidence folder is denied in headless mode, as in earlier rounds)

## Checks

| Check | Result | Evidence |
|---|---|---|
| Review (`git diff HEAD` + untracked) | pass | T119-T130 all exist in code. Changes are presentational only (imports, templates, styles; no API or logic change apart from fix F1). No data-testid lost (`evidence/M10/round-1/testid-diff.js`: only the `<page>-title` literals moved into `titleTestId` inputs of `ui-page-header`). Illustrations 1.0-1.2 kB each, `aria-hidden`, `currentColor`/CSS vars only. No backend change, no contract change. |
| Swagger | pass | `evidence/M10/round-1/swagger-diff.md` (Result: PASS) |
| Unit (`./mvnw -q verify`) | pass | exit 0, 188 tests, 0 errors, 0 failures, 0 skipped (`run-artifacts/verify-M10-r1-mvn.log`) |
| curl (`--upto M10`, fresh db) | pass | `evidence/M10/round-1/curl.log`: 7 suites (M01-M07), 0 failed |
| `ng build` | pass | builds, budget WARNING only (616.77 kB > 600 kB, known since M09, M11 T134 owns it) |
| E2E (M10 scenarios, real stack) | pass | see below |
| Smoke (earlier milestones) | pass | E2E-M01-01, M02-01, M03-01, M04-01, M05-01, M06-01, M08-01, M09-01 |

## Browser detail (Playwright MCP, 1440 px unless noted)

- **E2E-M10-08 [real-stack-only]** (fresh db, first): Tasks, Habits, Learning and Plans show `<page>-empty-state` with a 160x120 SVG illustration (aria-hidden), title, message and `<page>-empty-add-button`, in light and dark. Contrast 0 failures on all 8 views. Screenshots `verify-M10-r1-08-tasks-empty-light.png`, `verify-M10-r1-08-plans-empty-light.png` (stored under `run-artifacts/playwright/run-artifacts/playwright/` by a path slip), `verify-M10-r1-08-plans-empty-dark.png`.
- **Error state** (backend stopped): `tasks-error-retry` and `habits-error-retry` inside the restyled `.error-state` (danger-bg, radius 12 px), contrast 0 failures. `verify-M10-r1-error-tasks-light.png`. The two 500s in the console came from the dev proxy while the backend was down on purpose. Backend restarted afterwards.
- **E2E-M01-01**: 6 nav links open their `<page>-title` with `aria-current="page"`.
- **E2E-M02-01**: Tasks dialog -> `tasks-row-1` "Pay rent", Todo, Medium; complete/edit/archive/delete icon buttons with aria-labels.
- **E2E-M03-01**: habit "Read 20 pages" -> `habits-card-2`: Daily, "Not done today", "0 days streak"; toggled -> "Done today", "1 day streak", accent border. Weekly habit shows "Weekly".
- **E2E-M04-01**: learning card "Kubernetes basics" -> `learning-card-2`, "Not started".
- **E2E-M10-04**: `learning-expand-1` collapse 617 -> 165 -> 48 -> 6 -> 0 px, re-expand 0 -> 251 -> 504 -> 617 px (`grid-template-rows 0.2s`), collapsed content stays in the DOM (hidden). The **first** open of a card jumped with no animation; fixed (F1), after the fix the first open goes 0 -> 251 -> 504 -> 617 px. Milestone/note ids present (13). Tick milestone 1 -> "1/2 milestones", status "In progress" (D-7), bar `aria-valuenow` 50. `verify-M10-r1-04-learning-expanded-light.png`.
- **E2E-M05-01**: builder (sectioned pickers, form inside the dialog: form right 938 < dialog right 962) -> plan with 3 items saved, `plans-card-1`. `verify-M10-r1-05-plan-builder-light.png`.
- **E2E-M10-05** (real clock instead of the mock override): plan "Deep work" created via API to start at the next minute; after the start + reload: `plans-start-banner` "Plan Deep work has started", `plans-highlight-2` with a 2 px accent outline, `plans-duration-warning-2` warning colour + icon, rest time is the largest text in the card (28 px, `tabular-nums`) and counts down, with progress beside it. `verify-M10-r1-05-plans-light.png`. Seen there: at 0 % the progress bars looked **completely filled** (track = `--qf-accent-soft` = saturated `#00fbfb` in light). Fixed (F2), `verify-M10-r1-05-plans-light-fixed.png` (neutral track, teal 33 %).
- **E2E-M10-02**: same tone, icon and text on all three pages: In progress = warning + schedule (Tasks `tasks-status-2`, Plans `plans-status-1`); Overdue = danger + warning icon (Tasks `tasks-overdue-2`, Dashboard `dashboard-overdue-2` + `dashboard-overdue-marker-2`); Todo/Low info, High danger, Medium warning. No chip without text.
- **E2E-M06-01**: dashboard shows greeting, 4 metric cards, today's/overdue tasks, habit checklist, active plans with rest and progress, learning snapshot. Ticking a plan item synced the task to Done (FR-08).
- **E2E-M10-06**: 1440 px: 4 metric cards on one row (top 166, 276 px each); 390 px: stacked (tops 285/503/641/779), no horizontal scroll.
- **E2E-M10-01**: every `ui-card` on the six pages has radius 12 px, the same `--qf-shadow-1` and surface `rgb(241,244,243)` (done habit card uses a raised tint plus accent border). Screenshots `verify-M10-r1-01-dashboard-light.png`, `-dashboard-dark.png`, `-tasks-dark.png`, `-habits-light.png`.
- **E2E-M10-03**: with XHR delayed 1500 ms, `tasks-loading` shows 5 skeleton blocks (aria-hidden) and a "Loading" status, then 3 rows.
- **E2E-M10-07**: headings Profile, Preferences, Appearance; `settings-theme-dark` -> html dark + toolbar "Dark theme" pressed; toolbar Light -> radio light; System -> `color-scheme: light dark`. `verify-M10-r1-07-settings-light.png`.
- **E2E-M10-09**: contrast check on all six pages, light and dark, with real data: 12 runs, 0 failures, 0 unknown (checked 70/50/33/31/39/30 elements).
- **E2E-M08-01**: body font "Inter Variable", `document.fonts.check` true, primary buttons teal. **E2E-M09-01**: each nav item has an icon and a label, active one has `aria-current`.
- Console: no errors after the last reload (Vite/Angular dev logs only). Network: no 5xx.

## Fixes applied

| Id | File | What | Why | Lines |
|---|---|---|---|---|
| F1 | `frontend/src/app/features/learning/learning-page.component.ts` | `toggleExpanded`: on the first open, render the details closed and add `open` two animation frames later | the collapse block was created with `.open` already set, so the first expand jumped (E2E-M10-04 "expand is smooth") | +5 / -1 |
| F2 | `frontend/src/app/features/plans/plan-progress.component.ts`, `frontend/src/app/features/dashboard/dashboard-page.component.scss`, `frontend/src/app/features/learning/learning-page.component.ts` | progress track `--qf-accent-soft` -> `--qf-outline` | in light the track was saturated cyan, so a 0 % bar looked full (misleading progress on Plans, Dashboard and Learning) | 3 x 1 |

Total: 3 files, about 9 changed lines, no design or contract change. Re-run after the fixes: E2E-M10-04 first-open/collapse/re-open, Plans bars light and dark (track `rgb(190,201,200)` / `rgb(63,73,72)`, indicator teal, 33 %), contrast on all six pages light and dark, `ng build`.

A third fix (empty-state add button `mat-flat-button`, T122 says "add button filled") was applied and then **reverted** to stay within the 3-file limit. Acceptance E2E-M10-08 does not ask for a filled button, so it goes to the backlog.

## FIX tasks

None.

## Contract issues

None open for M10.

## Notes (backlog, not blocking)

- T122 wording: empty-state add button should be filled (`mat-flat-button`); it is still `mat-raised-button` (elevated, teal text). One-line change in `frontend/src/app/shared/empty-state.component.ts`.
- `--qf-accent-soft` (`#00fbfb` in light) is still the active nav background (already in the backlog since M09). It is now used nowhere else.
- Spec 001 backlog still applies: a new plan item starts unticked even if the source is done (seen: habit done today, plan item unticked); "Show inactive" on Habits starts switched on.
- Bundle budget warning 616.77 kB stays for M11 T134.
