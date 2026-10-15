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
      <mat-progress-bar mode="determinate" [value]="plan().completionPercent" aria-hidden="true" />
      <span class="label">{{ plan().doneItems }} of {{ plan().totalItems }} items ({{ plan().completionPercent }}%)</span>
    </div>
  `,
  styles: `
    .progress {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .label {
      font-size: 13px;
    }
  `,
})
export class PlanProgressComponent {
  readonly plan = input.required<Plan>();
}
