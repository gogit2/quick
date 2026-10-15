import { Component, input } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Plan } from '../../core/api/models';

// Progress bar and "x of y items (n%)" label for one plan. Counts come from the server.
@Component({
  selector: 'qf-plan-progress',
  standalone: true,
  imports: [MatProgressBarModule],
  template: `
    <div class="progress" [attr.data-testid]="'plans-progress-' + plan().id">
      <mat-progress-bar mode="determinate" class="bar" [value]="plan().completionPercent" aria-hidden="true" />
      <span class="qf-caption label">{{ plan().doneItems }} of {{ plan().totalItems }} items ({{ plan().completionPercent }}%)</span>
    </div>
  `,
  styles: `
    .progress {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-1);
    }

    .bar {
      --mdc-linear-progress-active-indicator-color: var(--qf-accent);
      --mdc-linear-progress-track-color: var(--qf-outline);
    }

    .label {
      color: var(--qf-text-muted);
    }
  `,
})
export class PlanProgressComponent {
  readonly plan = input.required<Plan>();
}
