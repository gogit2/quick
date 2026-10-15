# M07 self-check, iteration 1 (tasks T094-T098)

Run on the Prism mock (port 4010) and the dev server (port 4300) with the Playwright MCP. Both servers were stopped afterwards.
`npx ng build` passed twice (the last run after the final edit): only the 500 kB initial budget warning remains (T100).

## T094 error states and retry
- Mock stopped, `/habits` loaded: `habits-load-error` with `habits-error-retry` shown, no empty state, console shows 500 responses (expected for the outage).
- Mock restarted, `habits-error-retry` clicked: 2 habit cards loaded, error state gone.
- Other pages: `<page>-error-retry` added the same way (tasks, learning, plans, dashboard, settings). Not exercised one by one in this iteration (E2E-M07-03 is the verifier's real-stack run).

## T095 form validation
- Tasks dialog, empty title: `tasks-form-save` disabled, title field focused.
- Plan builder, whitespace title: "Title is required." shown, `plans-form-save` disabled, "Pick at least one task..." shown when nothing picked.

## T096 accessibility and layout
- `nav-tasks` has `aria-current="page"` on /tasks (the other links have none).
- Navigation is a single landmark, `Main navigation` (a nested `navigation` found by snapshot was removed).
- At 800 px width: `document.documentElement.scrollWidth` 800 == `clientWidth` 800 on /tasks, /plans and /dashboard.
- Resized back to 1440x900.
- Not checked in this iteration: /habits, /learning, /settings at 800 px; keyboard walk (E2E-M07-04, next iteration).

## T097 notification and clock
- Clock set to 2026-10-15T09:30:00Z on /dashboard: one `plans-start-banner`; after `plans-start-banner-dismiss` the banner count is 0.
- `dashboard-plan-rest-1` reads "Rest 1h 29m 52s" (positive, follows the clock).
- Notifications-off path: not re-run here; unchanged since E2E-M05-07 step 6.

## T098 journey scenario
- `outputs/e2e/M07-journey.md` written. Not executed (the verifier runs it on the real stack).

## Console
- Session console: only Vite and Angular dev-mode logs, plus the 500 responses from the deliberate mock outage.
