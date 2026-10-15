import { Component } from '@angular/core';

import { EmptyStateComponent } from '../../shared/empty-state.component';

// Placeholder for M01: always empty; the add action is inert until the task feature lands.
@Component({
  selector: 'qf-tasks-page',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <h1 data-testid="tasks-title">Tasks</h1>
    <qf-empty-state
      testId="tasks"
      icon="☑"
      title="No tasks yet"
      message="Add your first task to start tracking what you need to do."
      actionLabel="Add task"
    />
  `,
})
export class TasksPageComponent {}
