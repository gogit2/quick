import { Component } from '@angular/core';

// Row for lists: optional [leading] slot, default content, optional [actions] slot.
// Actions drop under the content on phones.
@Component({
  selector: 'ui-list-row',
  standalone: true,
  template: `
    <div class="row">
      <div class="leading">
        <ng-content select="[leading]" />
      </div>
      <div class="content">
        <ng-content />
      </div>
      <div class="actions">
        <ng-content select="[actions]" />
      </div>
    </div>
  `,
  styles: `
    :host {
      display: block;
      border-bottom: 1px solid var(--qf-outline);
    }

    :host:last-child {
      border-bottom: 0;
    }

    .row {
      display: flex;
      align-items: center;
      gap: var(--qf-space-3);
      padding: var(--qf-space-3) var(--qf-space-4);
      transition: background-color var(--qf-motion-fast) var(--qf-ease);
    }

    .row:hover {
      background: var(--qf-surface-raised);
    }

    .row:focus-within {
      outline: 2px solid var(--qf-focus-ring);
      outline-offset: -2px;
    }

    .leading {
      display: flex;
      flex-shrink: 0;
      align-items: center;
    }

    .leading:empty,
    .actions:empty {
      display: none;
    }

    .content {
      flex: 1 1 auto;
      min-width: 0;
    }

    .actions {
      display: flex;
      flex-shrink: 0;
      align-items: center;
      gap: var(--qf-space-1);
    }

    @media (max-width: 599.98px) {
      .row {
        flex-wrap: wrap;
      }

      .actions {
        width: 100%;
        justify-content: flex-end;
      }
    }
  `,
})
export class UiListRowComponent {}
