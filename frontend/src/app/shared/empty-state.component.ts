import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

// Empty state with an add action. `testId` is the page prefix, e.g. "tasks" gives
// tasks-empty-state and tasks-empty-add-button.
@Component({
  selector: 'qf-empty-state',
  standalone: true,
  imports: [MatButtonModule],
  template: `
    <section class="empty" [attr.data-testid]="testId() + '-empty-state'">
      <div class="icon" aria-hidden="true">{{ icon() }}</div>
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
      <button
        mat-raised-button
        color="primary"
        type="button"
        [attr.data-testid]="testId() + '-empty-add-button'"
        (click)="action.emit()"
      >
        {{ actionLabel() }}
      </button>
    </section>
  `,
  styles: `
    .empty {
      max-width: 480px;
      padding: 32px 0;
    }

    .icon {
      font-size: 48px;
      line-height: 1;
      margin-bottom: 16px;
    }

    p {
      color: rgba(0, 0, 0, 0.6);
      margin: 8px 0 24px;
    }
  `,
})
export class EmptyStateComponent {
  readonly testId = input.required<string>();
  readonly icon = input<string>('');
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly actionLabel = input.required<string>();
  readonly action = output<void>();
}
