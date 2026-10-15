# M05 evidence - tasks T069-T073 (iteration 2, Playwright MCP on the Prism mock)

Setup: `scripts/mock.sh start` (4010), `scripts/frontend.sh start --api http://localhost:4010` (4300).
`npx ng build` (frontend/): passes, 500 kB budget warning only.
Both servers stopped by script ("stopped").

## Checks
- T069 types/API: contract PlanItemSourceType is TASK, HABIT, LEARNING_RESOURCE; models.ts and plans.api.ts match. Build green.
- T070 rest time: at app clock 2026-10-15T09:30:00Z the active plan shows "Rest 1h 29m 57s" and the value drops by about 1 s per tick (1h 29m 50s, 1h 29m 41s, ...). Progress shows "1 of 3 items (33%)".
- T071 page: GET /api/plans?group=ACTIVE_UPCOMING and GET /api/plans?group=COMPLETED both sent. The stateless mock returns the same example plan for both groups, so it renders in both sections (mock artifact). Status chip, priority, window and progress render. No console errors.
- T072 builder: empty submit shows title, items and validation messages with no POST. End before start shows `plans-form-window-error`. Duration 90 over a 30-minute window shows `plans-form-duration-warning`, which clears after the window is widened. Picking 1 task, 1 habit and 1 learning card sends POST /api/plans with 3 items (title, duration, UTC start/end, priority all correct); the dialog closes and both groups reload.
- T073 items and delete: toggling an item sends PUT /api/plans/1/items/1. Delete: Cancel sends nothing; OK sends DELETE /api/plans/1.

Not run here: screenshots (copy into this folder is subject to the permission layer; the Playwright run folder was not copied), and the formal E2E-M05-01..09 scenarios (T075, next iteration).
