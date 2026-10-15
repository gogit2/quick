# Quickflow

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 19.2.10.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

## Design system

The styles live in `src/styles/` and are loaded from `src/styles.scss`.

### Token and style files

- `_palette.scss`: the raw colour values. Each status colour (success, warning, danger, info) is a light/dark pair.
- `_tokens.scss`: the `--qf-*` design tokens on `:root`. It holds the surface and text roles (aliases of the Material `--mat-sys-*` variables), accent and focus, status colours and their soft backgrounds (generated from `_palette.scss` with `light-dark()`), spacing (4 px base), radius, shadows, the type scale (`.qf-display`, `.qf-title`, `.qf-body`, `.qf-label`, `.qf-caption`), motion durations and easing, and the layout sizes (sidenav, rail, toolbar).
- `_theme.scss`: the Material 3 theme (cyan primary, violet tertiary, Inter typography), set with `mat.theme`.
- `_material.scss`: overrides of Material shapes and the snackbar, only through `--mat-*` variables on `html`.
- `_typography.scss`: the self-hosted Inter variable font (Latin subset) and a metric-matched fallback, so text does not shift when Inter loads.
- `_motion.scss`: the transitions (only opacity, transform, background-color, border-color, box-shadow and grid-template-rows, all under 300 ms) and the reduced-motion rules.
- `_base.scss`: element base styles, `color-scheme` per theme, and the focus ring (2 px outline, both themes).

### How the theme is chosen and stored

- The modes are light, dark and system. The Theme group in the toolbar and the Appearance radios on Settings both set the mode.
- `ThemeService` keeps the choice in `localStorage` under `qf.theme`. A missing or unknown value means `system`.
- `<html data-theme="light|dark|system">` carries the mode. `_base.scss` maps it to `color-scheme`, and the `light-dark()` tokens pick the colours. The inline script in `index.html` sets the attribute before the first paint.

### Adding a colour or a component

- A new colour role: add a `--qf-*` variable in `_tokens.scss` that aliases a `--mat-sys-*` variable, with `light-dark()` when the two themes differ.
- A new status colour: add its light/dark pair in `_palette.scss`. `_tokens.scss` generates `--qf-<name>` and `--qf-<name>-bg` from it.
- A new component: use Angular Material where it exists, style it with tokens and the `.qf-*` type classes, and give every element a test hook named `<page>-<element>[-<qualifier>]`.

### Colour rule

No hex, `rgb()` or `hsl()` literal may appear outside `_palette.scss` and `_tokens.scss`. Check with:

```bash
grep -rnE '#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(' src/app src/styles
```

The only lines printed must come from those two files. Exception: `src/index.html` sets the pre-paint background with two literals, because the stylesheet has not loaded yet.

### Contrast and focus

Text needs 4.5:1 contrast (3:1 for large text). Icon-only buttons and the focus ring need 3:1. The browser check script is `loops/frontend-dev/outputs/e2e/contrast-check.js`.

### Test flags

- `?clockOffsetMs=<ms>` moves the app clock by that many milliseconds (`ClockService`).
- `?reduceMotion=1` turns motion off for that page (sets `html[data-reduce-motion='true']`), the same rules as the OS setting `prefers-reduced-motion`.
