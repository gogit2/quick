import { Component } from '@angular/core';

import { EmptyStateComponent } from '../../shared/empty-state.component';

// Placeholder for M01: always empty; the add action is inert until the learning feature lands.
@Component({
  selector: 'qf-learning-page',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <h1 data-testid="learning-title">Learning</h1>
    <qf-empty-state
      testId="learning"
      icon="✎"
      title="No learning resources yet"
      message="Add a course, book or video to track milestones and notes."
      actionLabel="Add learning resource"
    />
  `,
})
export class LearningPageComponent {}
