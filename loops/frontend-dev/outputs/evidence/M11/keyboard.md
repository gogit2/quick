# M11 keyboard and focus check (T133)

Measured 2026-10-15 on http://localhost:4300 with the Prism mock on :4010, Playwright MCP.

## Focus ring
- Method: every visible focusable control on each page (button, link, input, select, combobox, switch, radio, tabindex 0) is focused and its computed `outline-style` and `outline-width` are read. Pass = solid and 2 px or more.
- Before the fix: text inputs (`settings-display-name`, `settings-email`) and the mat-selects (`settings-time-zone`, `settings-start-page`) had `outline: none` on focus (Material removes it).
- Fix: `frontend/src/styles/_base.scss` adds `html :is(input, textarea, select, [role='combobox'], [role='switch'], [role='radio'], [role='checkbox']):focus-visible { outline: 2px solid var(--qf-focus-ring); outline-offset: 2px }`. The selector is `html`-prefixed so it beats Material's `:focus` reset.
- After the fix: 0 controls without a ring on dashboard, tasks, habits, learning, plans and settings, in light and dark (totals 22, 28, 22, 16, 21, 19 per theme).

## ARIA
- Theme toggle: each option is a button with `aria-label` and `aria-pressed` (true on the active one). Checked on every page.
- Nav: each link has `ariaCurrentWhenActive="page"` (shell.component.html). The active page gets `aria-current="page"`.
- Icon-only buttons: every `mat-icon-button` in shell, tasks and habits templates has an `aria-label` (checked by grep of templates).
- `<html lang="en">` is set in `frontend/src/index.html`.

## Dialog focus and Escape (New task dialog on Tasks)
- Keyboard flow: focus `tasks-add-button`, press Enter. Dialog opens and focus moves into it (first field, `tasks-form-title`).
- Press Escape. Dialog closes and focus returns to `tasks-add-button`.
- A first attempt opened the dialog with a script `.click()`, so nothing had focus. Focus then went to BODY. That is expected for a non-focused opener and is not a defect.

## Tab order (DOM order of tabbable controls, visible only)
Computed from the DOM, not by pressing Tab. The Playwright keyboard was used only for the dialog check.

Tasks:
theme-option-light, theme-option-dark, theme-option-system, user-chip, plans-start-banner-dismiss (when shown), nav-dashboard, nav-tasks, nav-habits, nav-learning, nav-plans, nav-settings, nav-collapse-toggle, tasks-add-button, tasks-search-input, tasks-filter-status, tasks-filter-priority, tasks-filter-due, tasks-sort, Show archived (switch), tasks-clear-filters, tasks-complete-1, tasks-edit-1, tasks-archive-1, tasks-delete-1, tasks-complete-2, tasks-edit-2, tasks-archive-2, tasks-delete-2

Settings:
theme-option-light, theme-option-dark, theme-option-system, user-chip, plans-start-banner-dismiss (when shown), nav-dashboard, nav-tasks, nav-habits, nav-learning, nav-plans, nav-settings, nav-collapse-toggle, settings-display-name, settings-email, settings-time-zone, settings-start-page, Notifications (switch), the checked theme radio (named by its label, which the DOM-text helper did not read), settings-save-button

Note: the three theme radios are a roving group, so only the checked one is in the tab order.
