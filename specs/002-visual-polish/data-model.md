# Data model: QuickFlow Visual Polish

**No server data, no API change, no database change.** Everything here lives in CSS or in the browser. Server entities are those of spec 001 ([data-model](../001-quickflow/data-model.md)).

## Browser-stored preferences (`localStorage`, per browser)

| Key | Values | Default | Written by | Rules |
|---|---|---|---|---|
| `qf.theme` | `light`, `dark`, `system` | `system` | `ThemeService.setMode()` | read before first paint by the inline script in `index.html`; any other value or a storage error = `system`; never sent to the server |
| `qf.nav.collapsed` | `true`, `false` | `false` | `NavPrefs.setCollapsed()` | only used at >= 960 px; ignored in drawer mode |

Test-only query flags (documented, harmless in production): `?clockOffsetMs=` (spec 001), `?reduceMotion=1` (forces reduced motion for tests, R7).

## Design tokens

Material 3 system variables (`--mat-sys-*`) come from one `mat.theme(...)` call (research R1); the app adds `--qf-*` tokens in `frontend/src/styles/_tokens.scss`. **No colour, spacing, radius, shadow or duration may be written as a literal anywhere else** (SC-V07).

### Colour roles (light value / dark value come from `light-dark()` or the `--mat-sys-*` alias)
| Token | Meaning | Source |
|---|---|---|
| `--qf-bg` | page background | `--mat-sys-background` |
| `--qf-surface` | cards, dialogs, sidenav | `--mat-sys-surface-container-low` |
| `--qf-surface-raised` | hover, selected rows | `--mat-sys-surface-container` |
| `--qf-text` / `--qf-text-muted` | primary / secondary text | `--mat-sys-on-surface` / `--mat-sys-on-surface-variant` |
| `--qf-outline` | borders, dividers | `--mat-sys-outline-variant` |
| `--qf-accent` / `--qf-on-accent` | primary actions, selection, focus | `--mat-sys-primary` / `--mat-sys-on-primary` |
| `--qf-accent-soft` | active nav item, selected chip background | `--mat-sys-primary-container` |
| `--qf-focus-ring` | focus indicator colour | `--mat-sys-primary` (checked >= 3:1 against bg and surface) |
| `--qf-success`, `--qf-warning`, `--qf-danger`, `--qf-info` (+ `-bg` variants) | status chips and overdue (always paired with icon + text) | `light-dark(...)` values chosen to pass AA |

### Chips (FR-V14: one meaning, one colour, one icon, always with text)
| Value | Colour role | Icon | Text |
|---|---|---|---|
| Task TODO | `info` | `schedule` | Todo |
| Task IN_PROGRESS | `warning` | `schedule` | In progress |
| Task DONE | `success` | `check` | Done |
| Priority LOW / MEDIUM / HIGH | `info` / `warning` / `danger` | none | Low / Medium / High |
| Overdue | `danger` | `warning` | Overdue |
| Plan NOT_STARTED / IN_PROGRESS / COMPLETED | `info` / `warning` / `success` | `schedule` / `schedule` / `check` | Not started / In progress / Completed |
| Learning NOT_STARTED / IN_PROGRESS / COMPLETED | as plans | as plans | Not started / In progress / Completed |

### Space, shape, elevation, type, motion
| Group | Tokens |
|---|---|
| Spacing (4 px base) | `--qf-space-1` 4, `-2` 8, `-3` 12, `-4` 16, `-5` 24, `-6` 32, `-7` 48 (px) |
| Radius | `--qf-radius-sm` 8, `--qf-radius-md` 12, `--qf-radius-lg` 20, `--qf-radius-pill` 999 (px) |
| Shadow | `--qf-shadow-1` (cards), `--qf-shadow-2` (menus, dialogs) soft, low opacity; dark theme uses a border instead of a heavy shadow |
| Type scale (Inter Variable) | `--qf-text-display` 28/36 600, `--qf-text-title` 20/28 600, `--qf-text-body` 15/24 400, `--qf-text-label` 13/20 500, `--qf-text-caption` 12/16 400 (size/line-height px, weight) |
| Motion | `--qf-motion-fast` 120 ms, `--qf-motion-base` 200 ms, `--qf-motion-slow` 280 ms, `--qf-ease` `cubic-bezier(0.2, 0, 0, 1)`; all 0 ms under reduced motion |
| Layout | `--qf-sidenav-width` 240, `--qf-sidenav-rail` 72, `--qf-toolbar-height` 64 (px); breakpoints: phone < 600, tablet 600-959, desktop >= 960, wide >= 1280 |

## Component inventory (shared building blocks, FR-V13)
`UiCard`, `UiChip` (status/priority), `UiListRow`, `UiPageHeader`, `UiSkeleton` (line, card, row variants), `EmptyState` (existing, restyled, ids kept), `ThemeToggle`, `UserChip`, `IconService` + `<mat-icon svgIcon>`, illustrations (research R10).
