import { Component, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { Plan, PlanItem, PlanItemSourceType, PlanStatus } from '../../core/api/models';
import { PlansApi } from '../../core/api/plans.api';
import { ClockService } from '../../core/clock.service';
import { RestTimePipe } from '../../shared/rest-time.pipe';
import { PlanProgressComponent } from './plan-progress.component';
import { restSecondsAt } from './plan-clock';

// One plan in the Plans page: window, status, progress, live rest time and its items.
// Toggling an item replaces this plan with the Plan the server returns (progress is server-computed).
@Component({
  selector: 'qf-plan-card',
  standalone: true,
  imports: [DatePipe, RestTimePipe, MatButtonModule, MatCardModule, MatCheckboxModule, PlanProgressComponent],
  template: `
    <div
      class="plan-frame"
      [class.plan-highlight]="highlighted()"
      [attr.data-testid]="highlighted() ? 'plans-highlight-' + plan().id : null"
    >
    <mat-card class="plan-card" [attr.data-testid]="'plans-card-' + plan().id">
      <mat-card-header>
        <mat-card-title class="card-title">{{ plan().title }}</mat-card-title>
        <mat-card-subtitle>
          {{ plan().startDateTime | date: 'MMM d, HH:mm' }} – {{ plan().endDateTime | date: 'MMM d, HH:mm' }}
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <div class="meta">
          <span class="chip" [attr.data-testid]="'plans-status-' + plan().id">{{ statusLabel(plan().status) }}</span>
          <span [attr.data-testid]="'plans-priority-' + plan().id">Priority {{ plan().priorityOrder }}</span>
          @if (plan().status === 'IN_PROGRESS') {
            <span [attr.data-testid]="'plans-rest-' + plan().id">Rest {{ rest() | restTime }}</span>
          }
        </div>

        @if (plan().durationExceedsWindow) {
          <p class="warning" [attr.data-testid]="'plans-duration-warning-' + plan().id">
            The estimated duration is longer than the plan window.
          </p>
        }

        <qf-plan-progress [plan]="plan()" />

        <ul class="items">
          @for (item of plan().items; track item.id) {
            <li class="item" [attr.data-testid]="'plans-item-' + item.id">
              <mat-checkbox
                [attr.data-testid]="'plans-item-toggle-' + item.id"
                [checked]="item.done"
                [disabled]="pendingIds().has(item.id)"
                [attr.aria-label]="'Mark ' + item.title + ' as ' + (item.done ? 'not done' : 'done')"
                (change)="toggle(item, $event.checked)"
              />
              <span class="type-icon" aria-hidden="true">{{ typeIcon(item.sourceType) }}</span>
              <span class="item-title" [class.item-done]="item.done">{{ item.title }}</span>
            </li>
          }
        </ul>
      </mat-card-content>

      <mat-card-actions align="end">
        <button
          mat-button
          color="warn"
          type="button"
          [attr.data-testid]="'plans-delete-' + plan().id"
          (click)="deleteRequested.emit()"
        >
          Delete
        </button>
      </mat-card-actions>
    </mat-card>
    </div>
  `,
  styles: `
    .card-title {
      overflow-wrap: anywhere;
    }

    .meta {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      font-size: 13px;
    }

    .chip {
      padding: 2px 8px;
      border-radius: 12px;
      background: var(--qf-surface-raised);
    }

    .warning {
      color: var(--qf-warning);
      font-size: 13px;
    }

    .items {
      list-style: none;
      margin: 12px 0 0;
      padding: 0;
    }

    .item {
      display: flex;
      align-items: center;
      gap: 8px;
      min-height: 40px;
    }

    .type-icon {
      width: 1.5em;
      text-align: center;
    }

    .item-title {
      overflow-wrap: anywhere;
    }

    .item-done {
      text-decoration: line-through;
      opacity: 0.6;
    }

    .plan-highlight {
      outline: 2px solid var(--qf-focus-ring);
      border-radius: 4px;
    }
  `,
})
export class PlanCardComponent {
  private readonly api = inject(PlansApi);
  private readonly clock = inject(ClockService);
  private readonly snackBar = inject(MatSnackBar);

  readonly plan = input.required<Plan>();
  readonly highlighted = input(false);
  // Emits the Plan returned by the server after an item toggle.
  readonly updated = output<Plan>();
  readonly deleteRequested = output<void>();

  // Item ids with a PUT in flight; their checkbox is disabled until the answer arrives.
  readonly pendingIds = signal<ReadonlySet<number>>(new Set());

  // Read inside the template, so it re-evaluates on every clock tick.
  rest(): number | null {
    return restSecondsAt(this.plan(), this.clock.now());
  }

  statusLabel(status: PlanStatus): string {
    switch (status) {
      case 'NOT_STARTED':
        return 'Not started';
      case 'IN_PROGRESS':
        return 'In progress';
      case 'COMPLETED':
        return 'Completed';
    }
  }

  typeIcon(sourceType: PlanItemSourceType): string {
    switch (sourceType) {
      case 'TASK':
        return '☑';
      case 'HABIT':
        return '↻';
      case 'LEARNING_RESOURCE':
        return '✎';
    }
  }

  toggle(item: PlanItem, done: boolean): void {
    this.pendingIds.update((ids) => new Set(ids).add(item.id));
    this.api.setItemDone(this.plan().id, item.id, done).subscribe({
      next: (plan) => {
        this.clearPending(item.id);
        this.updated.emit(plan);
      },
      error: (err: unknown) => {
        this.clearPending(item.id);
        // 5xx errors already show a snackbar in the HTTP interceptor.
        if (err instanceof ApiRequestError && err.status >= 500) {
          return;
        }
        this.snackBar.open('The plan item could not be updated.', 'Dismiss', { duration: 6000 });
      },
    });
  }

  private clearPending(itemId: number): void {
    this.pendingIds.update((ids) => {
      const next = new Set(ids);
      next.delete(itemId);
      return next;
    });
  }
}
