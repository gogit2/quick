import { Component } from '@angular/core';

import { EmptyStateComponent } from '../../shared/empty-state.component';

// Placeholder for M01: always empty; the add action is inert until the plan feature lands.
@Component({
  selector: 'qf-plans-page',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <h1 data-testid="plans-title">Plans</h1>
    <qf-empty-state
      testId="plans"
      icon="▤"
      title="No plans yet"
      message="Create a plan to group tasks, habits and learning into a timed block."
      actionLabel="Create plan"
    />
  `,
})
export class PlansPageComponent {}
