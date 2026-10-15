import { Component } from '@angular/core';

import { EmptyStateComponent } from '../../shared/empty-state.component';

// Placeholder for M01: always empty; the add action is inert until the habit feature lands.
@Component({
  selector: 'qf-habits-page',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <h1 data-testid="habits-title">Habits</h1>
    <qf-empty-state
      testId="habits"
      icon="↻"
      title="No habits yet"
      message="Add a habit to track what you do every day or every week."
      actionLabel="Add habit"
    />
  `,
})
export class HabitsPageComponent {}
