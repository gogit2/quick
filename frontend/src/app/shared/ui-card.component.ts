import { Component, input } from '@angular/core';

// Surface block used for dashboard metrics, plan and habit cards and settings sections.
// `interactive` adds the hover lift; the focus ring shows when a child has focus.
@Component({
  selector: 'ui-card',
  standalone: true,
  template: `<ng-content />`,
  host: {
    '[class.interactive]': 'interactive()',
  },
  styles: `
    :host {
      display: block;
      background: var(--qf-surface);
      border: 1px solid var(--qf-outline);
      border-radius: var(--qf-radius-md);
      box-shadow: var(--qf-shadow-1);
      padding: var(--qf-space-4);
      transition:
        background-color var(--qf-motion-base) var(--qf-ease),
        box-shadow var(--qf-motion-base) var(--qf-ease);
    }

    :host(.interactive:hover) {
      background: var(--qf-surface-raised);
    }

    :host(:focus-within) {
      outline: 2px solid var(--qf-focus-ring);
      outline-offset: 2px;
    }
  `,
})
export class UiCardComponent {
  readonly interactive = input(false);
}
