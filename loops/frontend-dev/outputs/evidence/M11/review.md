# M11 responsive review (T135) - done

Date: 2026-10-15. Dev server :4300 on the Prism mock :4010, Playwright MCP. Screenshots are saved by the MCP under `run-artifacts/playwright/` (copying them into this folder is denied by the permission layer, so the paths are listed here instead).

## Overflow check (`document.documentElement.scrollWidth` vs `window.innerWidth`)
All 6 pages x light/dark x 3 widths = 36 combinations. Result: `scrollWidth == innerWidth` in all 36, so no horizontal scroll.

## Screenshots (36 of 36), `run-artifacts/playwright/<page>-<theme>-<width>.png`
- dashboard-light-1440.png, dashboard-light-1024.png, dashboard-light-390.png
- dashboard-dark-1440.png, dashboard-dark-1024.png, dashboard-dark-390.png
- tasks-light-1440.png, tasks-light-1024.png, tasks-light-390.png
- tasks-dark-1440.png, tasks-dark-1024.png, tasks-dark-390.png
- habits-light-1440.png, habits-light-1024.png, habits-light-390.png
- habits-dark-1440.png, habits-dark-1024.png, habits-dark-390.png
- learning-light-1440.png, learning-light-1024.png, learning-light-390.png
- learning-dark-1440.png, learning-dark-1024.png, learning-dark-390.png
- plans-light-1440.png, plans-light-1024.png, plans-light-390.png
- plans-dark-1440.png, plans-dark-1024.png, plans-dark-390.png
- settings-light-1440.png, settings-light-1024.png, settings-light-390.png
- settings-dark-1440.png, settings-dark-1024.png, settings-dark-390.png

## Findings
1. Dashboard (light, 1440): "Completed 0%(0 of 2)", "1 of 1" and "1 of 2" ran together because Angular dropped the whitespace between `</strong>` and `<span>`. FIXED in `features/dashboard/dashboard-page.component.html` with `&ngsp;` (iteration 1). Verified by screenshot.
2. Dashboard (1024, light and dark): the Learning card wraps onto its own row and leaves an empty area to its right. This is normal grid wrapping, not a defect. Not changed.
3. Plans (all themes and widths): each plan item shows both a checkbox and a status glyph (☑, ↻, ✎) next to the title. This comes from the M06 plan item markup, not from the restyle. Not changed here; flag for the user.
4. Settings (390, both themes): the Start page and Time zone select arrows are small but legible. Not changed.
5. No clipped controls, no overlapping panels, no truncated titles, and no console errors seen on the visited pages.
