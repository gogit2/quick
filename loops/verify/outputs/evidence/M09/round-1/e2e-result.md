# M09 round 1 - browser results (Playwright MCP, real stack :4300 -> :8090)

Screenshots: `run-artifacts/playwright/verify-M09-r1-*.png` (cp into evidence is denied in headless mode).

| Scenario | Result | Detail |
|---|---|---|
| Empty states (fresh db) | pass | Dashboard zeros + empty lists, Tasks "No tasks yet", Habits "No habits yet", Learning "No learning resources yet", Plans "No plans yet" |
| E2E-M09-01 [smoke] | pass | 1440 px: 6 links with svg icon + label; only active link has `aria-current="page"` + class `active`, bg rgb(0,251,251) (light), icon rgb(0,106,106) accent; toolbar 64 px, surface bg rgb(241,244,243); no menu-button. Shot 01 |
| E2E-M09-02 | pass | collapse -> sidenav 72 px, `qf.nav.collapsed`='true', no labels, aria-label kept, hover tooltip "Tasks", content left 72; reload -> 72 px + "Expand navigation"; expand -> 240 px, 'false', labels back, content left 240. Shot 02 |
| E2E-M09-03 | pass | 390 px: menu-button shown, collapse toggle hidden, drawer left -240 -> 0 on open, choosing Tasks -> /tasks + tasks-title, drawer closed; Escape closes drawer; scrollWidth 390 = innerWidth on all 6 pages, no off-screen elements; chip shows avatar only. Shots 06, 07 |
| E2E-M09-04 | pass | 1024 px: side mode, sidenav 0..240, content/main left 240, main right 1024, chip right 1008, collapse toggle inside sidenav; scrollWidth 1024 on all 6 pages. Shot 05 |
| E2E-M09-05 | pass | real stack: chip "QU / QuickFlow User" -> saved display name "Verify Tester" -> chip "VT / Verify Tester", aria-label "Profile: Verify Tester" immediately; chip links to /settings |
| E2E-M09-06 | pass after F1 | banner: light bg rgba(122,79,0,.12) text rgb(25,28,28), dark bg rgba(245,195,91,.16) text rgb(224,227,226), radius 12 px, `schedule` icon in --qf-warning, Dismiss kept. Snackbar: max-width 560 px, inverse surface (light rgb(45,49,49)/text rgb(239,241,240); dark rgb(224,227,226)/rgb(45,49,49)); radius was 4 px -> 12 px after F1. Shots 03, 04 |
| E2E-M09-07 | pass | contrast-check with banner + snackbar open: /plans 1440 dark 60 checked 0 failures (focus ring 10.12), light 60 checked 0 failures (6.42); /settings 390 dark 19 checked 0 failures |
| E2E-M09-08 | pass | E2E-M01-01 (6 links, titles, single aria-current) and E2E-M08-02 (light/dark/system set data-theme + color-scheme light/dark/"light dark", aria-pressed exclusive) |
| Smoke spec 001 | pass | E2E-M02-01 task row (To do, Medium), E2E-M03-01 habit card (Daily, streak), E2E-M04-01 card (Not started), E2E-M05-01 plan with 3 items, Not started -> In progress with rest time, E2E-M06-01 dashboard (greeting, 4 metric cards, lists, 3 active plans with rest) |
| Smoke E2E-M08-01 | pass | body font "Inter Variable", `document.fonts.check` true |
| Console / network | pass | no console errors (only vite/Angular dev logs); no 5xx; all requests on localhost |

Note: the :8090 stack was shared with another client during the run. It acknowledged plans 1-3 before this tab's 30 s sync (my tab sent no acknowledge-start for them). For plan 4 the tab reloaded at the start minute, POSTed `/api/plans/4/acknowledge-start` (200) and showed the banner. This is the documented once-per-plan behaviour, not a defect.
