# Feature Specification: QuickFlow Visual Polish

**Feature Branch**: `002-visual-polish` (spec folder only, no git branch)

**Created**: 2026-10-15

**Status**: Draft

**Input**: User description: "QuickFlow visual polish: restyle the existing app without changing any behaviour, API or test hook. Calm and modern look, light + dark + follow-system theme with a toolbar toggle, self-hosted Inter font, frontend-only."

**Traceability**: stories are `US-VIS-<NN>`; requirements `FR-V<NN>`; success criteria `SC-V<NN>`. Decisions D-19..D-24 in `requirements/DECISIONS.md` are settled and override anything here. This spec **reuses spec 001 unchanged**: same contract (`specs/001-quickflow/contracts/openapi.yaml`), same behaviour, same `data-testid`s. Milestones continue the numbering: M08 Design foundation, M09 Shell and navigation, M10 Pages, M11 Polish and regression.

## Clarifications

### Session 2026-10-15

- Q: What overall look? → A: Calm and modern: soft neutral surfaces, one teal/indigo accent, generous spacing, rounded cards with light shadows (D-20).
- Q: Dark theme? → A: Yes: light, dark and follow-system, a toolbar toggle, choice saved in the browser, default = system (D-21).
- Q: Which typeface? → A: Inter, self-hosted, Latin subset, no external requests (D-22).
- Q: Backend changes? → A: None: frontend-only against the unchanged contract (D-19).
- Q: What is out of scope? → A: Plan editing, drag-and-drop plan priority, charts and calendar views, tags (D-23).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A consistent visual identity (Priority: P1) US-VIS-01

Every screen uses one coherent look: the same colours, type sizes, spacing, corner radius and shadows, so the app feels like one product.

**Why this priority**: all other stories build on it, and it is what the user sees first.

**Independent Test**: open every page in light mode and check that text, buttons, cards, chips and form fields share one palette, one typeface and one spacing scale; no page uses a leftover default or hard-coded colour.

**Acceptance Scenarios**:

1. **Given** any page, **When** it loads, **Then** body text uses the Inter typeface, headings follow one type scale, and the accent colour is the same teal/indigo everywhere. (FR-V01, FR-V02)
2. **Given** any card, dialog or panel, **When** shown, **Then** it has the same corner radius, soft surface colour and light shadow. (FR-V03)
3. **Given** the whole app, **When** all styles are searched, **Then** no screen defines its own one-off colour: every colour comes from the shared palette. (FR-V04)

---

### User Story 2 - Light, dark and system theme (Priority: P1) US-VIS-02

A user can pick light, dark or "follow my system", and the app remembers the choice.

**Why this priority**: it is the biggest visible feature of this enhancement and the riskiest for consistency.

**Independent Test**: switch the toggle through its three states on several pages; reload; change the system setting.

**Acceptance Scenarios**:

1. **Given** the toolbar, **When** the user chooses Light, Dark or System, **Then** the whole app changes immediately without a reload. (FR-V05)
2. **Given** a chosen option, **When** the app is reloaded or reopened, **Then** the same option is applied before the first screen is visible, with no flash of the wrong theme. (FR-V06)
3. **Given** the option System, **When** the operating system switches between light and dark, **Then** the app follows. (FR-V05)
4. **Given** a first visit with no saved choice, **When** the app opens, **Then** System is selected. (FR-V06)

---

### User Story 3 - Navigation shell (Priority: P1) US-VIS-03

The toolbar and side navigation are redesigned: clear icons, a visible active item, a collapsible side panel, and a small user chip showing the name from Settings.

**Why this priority**: the shell is on screen at all times.

**Independent Test**: use every navigation item, collapse and expand the panel, change the display name in Settings and see the chip update.

**Acceptance Scenarios**:

1. **Given** the side navigation, **When** a page is open, **Then** its item is clearly highlighted and every item has an icon and a label. (FR-V07)
2. **Given** the side panel, **When** the user collapses it, **Then** only icons remain with tooltips, and the choice is remembered. (FR-V08)
3. **Given** a saved display name, **When** any page is open, **Then** the toolbar shows the name (or its initials) in a user chip. (FR-V09)
4. **Given** the plan start banner or a snackbar, **When** shown, **Then** it uses the new styling in both themes and remains readable. (FR-V10)

---

### User Story 4 - Works on small screens (Priority: P2) US-VIS-04

The app is comfortable to use on a phone-width screen.

**Why this priority**: not a PRD requirement, but a polished product must not break on narrow screens.

**Independent Test**: view every page at 390 px wide and at 1024 px and 1440 px.

**Acceptance Scenarios**:

1. **Given** a viewport of 390 px, **When** any page is shown, **Then** nothing scrolls horizontally and no control is cut off. (FR-V11)
2. **Given** a narrow viewport, **When** the page loads, **Then** the side navigation becomes a drawer opened from a menu button in the toolbar. (FR-V12)
3. **Given** lists and forms on a narrow viewport, **When** shown, **Then** rows stack, buttons stay reachable, and dialogs fit the screen. (FR-V11)

---

### User Story 5 - Shared building blocks (Priority: P2) US-VIS-05

Cards, status chips, list rows and page headers look and behave the same on every page.

**Why this priority**: it removes inconsistencies and makes later changes cheap.

**Independent Test**: compare the same kind of element across pages: a status chip on Tasks and on Plans, a page header everywhere.

**Acceptance Scenarios**:

1. **Given** any page, **When** shown, **Then** its title area (title, optional subtitle, primary action) follows one layout. (FR-V13)
2. **Given** any status or priority, **When** shown as a chip, **Then** the same status has the same colour and wording everywhere, in both themes, and the meaning is never given by colour alone. (FR-V14)
3. **Given** a list row or a card, **When** hovered or focused, **Then** it gives the same visible feedback on every page. (FR-V13)

---

### User Story 6 - Friendly empty and loading states (Priority: P2) US-VIS-06

Empty pages show a small illustration and a clear call to action; pages that are loading show placeholder shapes instead of a bare spinner.

**Why this priority**: first impressions and perceived speed.

**Independent Test**: open each list page with no data, and with a slowed connection.

**Acceptance Scenarios**:

1. **Given** a list page with no items, **When** shown, **Then** it displays an illustration, a short message and the add action, in both themes. (FR-V15)
2. **Given** a page that is loading, **When** data has not arrived, **Then** it shows placeholder shapes with the layout of the final content. (FR-V16)
3. **Given** a load failure, **When** shown, **Then** the error state uses the shared styling and keeps its retry action. (FR-V15)

---

### User Story 7 - Tasks, Habits, Learning pages (Priority: P1) US-VIS-07 US-VIS-08 US-VIS-09

The Tasks, Habits and Learning pages are restyled with the shared building blocks while keeping every behaviour: filters, dialogs, actions.

- **US-VIS-07 Tasks**: clearer rows, priority and status chips, a visible overdue treatment, a tidy filter toolbar.
- **US-VIS-08 Habits**: cards with a clear done state, streak indicator and frequency label.
- **US-VIS-09 Learning**: a card grid with progress bars, a smoother expand/collapse for milestones and notes.

**Why this priority**: these are the pages used most.

**Independent Test**: run every earlier scenario for these pages (E2E-M02-xx, M03-xx, M04-xx); they still pass unchanged, and the pages look consistent in both themes.

**Acceptance Scenarios**:

1. **Given** the Tasks page, **When** shown, **Then** each row shows title, status, priority and due date with clear hierarchy, and overdue tasks stand out without relying on colour alone. (FR-V17)
2. **Given** the Habits page, **When** a habit is done for the period, **Then** the card shows a clear done state and the streak. (FR-V18)
3. **Given** the Learning page, **When** a card is expanded, **Then** milestones and notes appear with a smooth transition and a progress bar reflects completed milestones. (FR-V19)
4. **Given** any of these pages, **When** the earlier scenarios are run, **Then** they all pass without changing a single `data-testid`. (FR-V25)

---

### User Story 8 - Plans, Dashboard, Settings pages (Priority: P1) US-VIS-10 US-VIS-11 US-VIS-12

- **US-VIS-10 Plans**: plan cards with a clear status, progress bar, a prominent live rest-time display and a visible start highlight.
- **US-VIS-11 Dashboard**: the greeting, metric cards, task lists, habit checklist, active plans and learning snapshot arranged in a balanced grid.
- **US-VIS-12 Settings**: a tidy form with grouped sections (profile, preferences) that also hosts the theme choice.

**Why this priority**: these show the product's main value (plans and the overview).

**Independent Test**: run every earlier scenario for these pages (E2E-M05-xx, M06-xx, M01-03/04); they still pass, and the rest-time display is readable at a glance.

**Acceptance Scenarios**:

1. **Given** an in-progress plan, **When** shown, **Then** rest time is the most prominent figure on its card, with progress next to it. (FR-V20)
2. **Given** the Dashboard, **When** shown at 1440 px, **Then** metric cards sit in one row above the lists in a balanced grid; at 390 px they stack. (FR-V21)
3. **Given** the Settings page, **When** shown, **Then** fields are grouped under clear headings and the theme choice is available there as well as in the toolbar. (FR-V22)
4. **Given** these pages, **When** the earlier scenarios are run, **Then** they all pass. (FR-V25)

---

### User Story 9 - Motion that respects the user (Priority: P3) US-VIS-13

Small transitions (hover, expand/collapse, dialogs opening, theme change) make the app feel smooth, and are switched off for users who prefer reduced motion.

**Why this priority**: nice to have; must never get in the way.

**Independent Test**: use the app with the system "reduce motion" setting on and off.

**Acceptance Scenarios**:

1. **Given** normal settings, **When** the user hovers, expands, opens a dialog or switches theme, **Then** the change animates in under 300 ms. (FR-V23)
2. **Given** the "reduce motion" system setting, **When** the same actions happen, **Then** no animation plays. (FR-V23)

---

### User Story 10 - Readable and accessible in both themes (Priority: P1) US-VIS-14

Everything is readable and operable by keyboard and assistive technology in both themes.

**Why this priority**: a polished look must not cost usability.

**Independent Test**: measure contrast of text, icons and chips in both themes; tab through every page.

**Acceptance Scenarios**:

1. **Given** either theme, **When** any text, icon or control is measured against its background, **Then** the contrast meets WCAG AA (4.5:1 normal text, 3:1 large text and UI components). (FR-V24)
2. **Given** keyboard use, **When** the user tabs through a page, **Then** every control shows a clearly visible focus ring in both themes. (FR-V24)
3. **Given** status chips, priority markers and overdue marks, **When** viewed, **Then** each also carries text or an icon, so colour alone never carries meaning. (FR-V14)

---

### User Story 11 - Fast and self-contained (Priority: P2) US-VIS-15

The polished app loads as quickly as before, makes no requests to outside servers for fonts or styles, and stays within the size budget.

**Why this priority**: the project rule is "self-contained"; the font must not break it.

**Independent Test**: build the app and check its size; load it with the network log open.

**Acceptance Scenarios**:

1. **Given** a production build, **When** measured, **Then** the initial download is under 500 kB (gzipped transfer or the build's initial budget, whichever the build reports) with no budget warning. (FR-V26)
2. **Given** a page load, **When** the network log is inspected, **Then** every font and style file comes from the app itself. (FR-V26)

---

### Edge Cases

- A very long task title, habit name or plan title: it wraps or truncates with a tooltip, never breaks the layout.
- A very long list: rows keep their spacing and the page scrolls normally.
- The saved theme value in the browser is missing or invalid: the app falls back to System.
- Browser storage is blocked: the theme still works for the session, with System as the default.
- Right-to-left or non-Latin text: not supported in this spec; Latin text only (the font subset is Latin).
- A phone in landscape (about 844 px wide): the layout switches smoothly, with no overlapping panels.
- Printing is not styled.
- The start-notification banner appears while the drawer is open on a small screen: both remain usable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-V01**: the app MUST use the Inter typeface for all text, delivered from the app itself (D-22), with a one-scale type hierarchy (page title, section title, body, caption).
- **FR-V02**: the app MUST use one accent colour (teal/indigo) for primary actions, links, selection and focus, with matching light and dark variants (D-20).
- **FR-V03**: every card, dialog and panel MUST share one corner radius, one surface colour per theme and one soft shadow style.
- **FR-V04**: all colours, spacing, radii and shadows MUST come from a single shared set of design values; no component may define its own one-off values.
- **FR-V05**: the user MUST be able to choose Light, Dark or System from the toolbar; the app MUST change immediately and follow operating-system changes while System is selected (D-21).
- **FR-V06**: the chosen theme MUST be remembered in the browser and applied before the first screen is visible; with no saved choice the default is System.
- **FR-V07**: the side navigation MUST show an icon and label per page and a clear active state.
- **FR-V08**: the side navigation MUST be collapsible to icons with tooltips, and the choice MUST be remembered in the browser.
- **FR-V09**: the toolbar MUST show a user chip with the display name from Settings (initials when narrow).
- **FR-V10**: the plan start banner, snackbars and dialogs MUST be restyled for both themes.
- **FR-V11**: every page MUST be usable at 390 px width with no horizontal scrolling.
- **FR-V12**: below a tablet-width breakpoint the side navigation MUST become a drawer opened from the toolbar.
- **FR-V13**: the app MUST provide shared card, list-row and page-header building blocks that every page uses.
- **FR-V14**: status, priority and overdue indications MUST use the same colour, wording and icon everywhere and MUST NOT rely on colour alone.
- **FR-V15**: every list page MUST have a styled empty state (illustration, message, add action) and a styled load-error state with retry.
- **FR-V16**: every page that loads data MUST show placeholder shapes shaped like the final content while loading.
- **FR-V17**: the Tasks page MUST be restyled with the shared blocks; all behaviour from spec 001 unchanged.
- **FR-V18**: the Habits page MUST be restyled with the shared blocks; all behaviour unchanged.
- **FR-V19**: the Learning page MUST be restyled with the shared blocks and a smooth expand/collapse; all behaviour unchanged.
- **FR-V20**: the Plans page MUST present rest time prominently, with progress, status and the start highlight; all behaviour unchanged.
- **FR-V21**: the Dashboard MUST arrange its sections in a balanced responsive grid; all behaviour unchanged.
- **FR-V22**: the Settings page MUST group its fields under headings and offer the theme choice; all existing settings behaviour unchanged.
- **FR-V23**: transitions MUST be shorter than 300 ms and MUST be disabled when the user prefers reduced motion.
- **FR-V24**: all text, icons and controls MUST meet WCAG AA contrast in both themes, and every interactive control MUST show a visible focus indicator in both themes.
- **FR-V25 (no regression)**: no behaviour, API call, route or `data-testid` of spec 001 may change; every scenario `E2E-M01-xx` to `E2E-M07-xx` MUST still pass.
- **FR-V26**: the production build MUST stay within its size budget with no warnings, and the app MUST make no requests to external servers for fonts, icons or styles.

### Key Entities

- **Theme preference**: a per-browser choice of Light, Dark or System; not sent to the server and not part of Settings data.
- **Navigation collapse preference**: a per-browser choice of expanded or collapsed side navigation.
- **Design values**: the shared set of colours, type sizes, spacing steps, radii and shadows for each theme.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-V01**: in both themes, 100% of checked text and icon pairs meet WCAG AA (4.5:1 normal text, 3:1 large text and controls).
- **SC-V02**: at 390 px, 1024 px and 1440 px, none of the six pages scrolls horizontally or clips a control.
- **SC-V03**: switching the theme takes effect in under 300 ms and survives a reload with no visible flash of the other theme.
- **SC-V04**: the production build has no size-budget warning and its initial download stays under 500 kB.
- **SC-V05**: a network log of a full page load shows 0 requests to external hosts.
- **SC-V06**: all earlier scenarios (E2E-M01-01 to E2E-M07-04 as defined in spec 001) pass unchanged on the polished app, with no console errors.
- **SC-V07**: a search of all style code finds no colour defined outside the shared design values.
- **SC-V08**: with "reduce motion" on, 0 animations play; with it off, hover, expand and dialog transitions are under 300 ms.

## Assumptions

- The backend, API contract and data are untouched; this is a frontend-only change (D-19).
- Existing `data-testid` names stay exactly as they are; new elements may add new ids following the same naming rule.
- Latin text only; the font is the Latin subset (D-22).
- Browser support is the current Chromium used for testing; other browsers are expected to work but are not part of the acceptance tests.
- The theme and navigation-collapse preferences live in browser storage only and are not synchronised between devices.
- Out of scope (D-23): editing a plan after creation, drag-and-drop plan priority, charts and calendar views, tags.
- The user-chip name comes from the existing Settings data; no new server fields.
