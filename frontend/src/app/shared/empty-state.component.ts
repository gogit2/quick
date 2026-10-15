import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { GenericIllustrationComponent } from './illustrations/generic-illustration.component';
import { HabitsIllustrationComponent } from './illustrations/habits-illustration.component';
import { LearningIllustrationComponent } from './illustrations/learning-illustration.component';
import { PlansIllustrationComponent } from './illustrations/plans-illustration.component';
import { TasksIllustrationComponent } from './illustrations/tasks-illustration.component';

export type EmptyIllustration = 'tasks' | 'habits' | 'learning' | 'plans' | 'generic';

// Empty state with an illustration and an add action. `testId` is the page prefix,
// e.g. "tasks" gives tasks-empty-state and tasks-empty-add-button.
// `icon` is kept for existing callers; the illustration replaces it on screen.
@Component({
  selector: 'qf-empty-state',
  standalone: true,
  imports: [
    MatButtonModule,
    TasksIllustrationComponent,
    HabitsIllustrationComponent,
    LearningIllustrationComponent,
    PlansIllustrationComponent,
    GenericIllustrationComponent,
  ],
  template: `
    <section class="empty" [attr.data-testid]="testId() + '-empty-state'">
      <div class="illustration">
        @switch (illustration()) {
          @case ('tasks') {
            <qf-tasks-illustration />
          }
          @case ('habits') {
            <qf-habits-illustration />
          }
          @case ('learning') {
            <qf-learning-illustration />
          }
          @case ('plans') {
            <qf-plans-illustration />
          }
          @default {
            <qf-generic-illustration />
          }
        }
      </div>
      <h2 class="qf-title">{{ title() }}</h2>
      <p class="qf-body message">{{ message() }}</p>
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
    :host {
      display: block;
    }

    .empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--qf-space-5);
      max-width: 480px;
      margin: 0 auto;
      padding: var(--qf-space-6) 0;
      text-align: center;
    }

    h2 {
      margin: 0;
      color: var(--qf-text);
    }

    .message {
      margin: 0;
      color: var(--qf-text-muted);
    }
  `,
})
export class EmptyStateComponent {
  readonly testId = input.required<string>();
  readonly illustration = input<EmptyIllustration>('generic');
  readonly icon = input<string>('');
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly actionLabel = input.required<string>();
  readonly action = output<void>();
}
