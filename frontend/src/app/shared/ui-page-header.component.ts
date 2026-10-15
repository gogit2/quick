import { Component, input } from '@angular/core';

// Page title with an optional subtitle and a primary action in the [actions] slot.
// `titleTestId` goes on the title element, e.g. "tasks-title".
@Component({
  selector: 'ui-page-header',
  standalone: true,
  template: `
    <header class="header">
      <div class="text">
        <h1 class="qf-display title" [attr.data-testid]="titleTestId() || null">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="qf-body subtitle">{{ subtitle() }}</p>
        }
      </div>
      <div class="actions">
        <ng-content select="[actions]" />
      </div>
    </header>
  `,
  styles: `
    :host {
      display: block;
    }

    .header {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      justify-content: space-between;
      gap: var(--qf-space-3) var(--qf-space-4);
      margin-bottom: var(--qf-space-5);
    }

    .title {
      margin: 0;
      color: var(--qf-text);
    }

    .subtitle {
      margin: var(--qf-space-1) 0 0;
      color: var(--qf-text-muted);
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--qf-space-2);
    }

    @media (max-width: 599.98px) {
      .header {
        flex-direction: column;
        align-items: stretch;
      }
    }
  `,
})
export class UiPageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input('');
  readonly titleTestId = input('');
}
