import { Component, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { Plan, PlanItem, PlanItemSourceType } from '../../core/api/models';
import { PlansApi } from '../../core/api/plans.api';
import { ClockService } from '../../core/clock.service';
import { RestTimePipe } from '../../shared/rest-time.pipe';
import { UiCardComponent } from '../../shared/ui-card.component';
import { UiChipComponent } from '../../shared/ui-chip.component';
import { UiListRowComponent } from '../../shared/ui-list-row.component';
import { PlanProgressComponent } from './plan-progress.component';
import { restSecondsAt } from './plan-clock';

// One plan in the Plans page: window, status, progress, live rest time and its items.
// Toggling an item replaces this plan with the Plan the server returns (progress is server-computed).
@Component({
  selector: 'qf-plan-card',
  standalone: true,
  imports: [
    DatePipe,
    RestTimePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    PlanProgressComponent,
    UiCardComponent,
    UiChipComponent,
    UiListRowComponent,
  ],
  template: `
    <div
      class="plan-frame"
      [class.plan-highlight]="highlighted()"
      [attr.data-testid]="highlighted() ? 'plans-highlight-' + plan().id : null"
    >
      <ui-card class="plan-card" [attr.data-testid]="'plans-card-' + plan().id">
        <div class="card-head">
          <h3 class="card-title qf-title">{{ plan().title }}</h3>
          <p class="window qf-caption">
            {{ plan().startDateTime | date: 'MMM d, HH:mm' }} – {{ plan().endDateTime | date: 'MMM d, HH:mm' }}
          </p>
        </div>

        <div class="meta">
          <ui-chip kind="plan-status" [value]="plan().status" [testId]="'plans-status-' + plan().id" />
          <span class="tag qf-label" [attr.data-testid]="'plans-priority-' + plan().id">
            Priority {{ plan().priorityOrder }}
          </span>
        </div>

        <div class="hero">
          @if (plan().status === 'IN_PROGRESS') {
            <span class="rest qf-display" [attr.data-testid]="'plans-rest-' + plan().id">
              Rest {{ rest() | restTime }}
            </span>
          }
          <qf-plan-progress class="progress" [plan]="plan()" />
        </div>

        @if (plan().durationExceedsWindow) {
          <p class="warning qf-label" role="note" [attr.data-testid]="'plans-duration-warning-' + plan().id">
            <mat-icon svgIcon="warning" aria-hidden="true" />
            <span>The estimated duration is longer than the plan window.</span>
          </p>
        }

        <ul class="items">
          @for (item of plan().items; track item.id) {
            <li>
              <ui-list-row [attr.data-testid]="'plans-item-' + item.id">
                <mat-checkbox
                  leading
                  [attr.data-testid]="'plans-item-toggle-' + item.id"
                  [checked]="item.done"
                  [disabled]="pendingIds().has(item.id)"
                  [attr.aria-label]="'Mark ' + item.title + ' as ' + (item.done ? 'not done' : 'done')"
                  (change)="toggle(item, $event.checked)"
                />
                <span class="item-title" [class.item-done]="item.done">
                  <span class="type-icon" aria-hidden="true">{{ typeIcon(item.sourceType) }}</span>
                  {{ item.title }}
                </span>
              </ui-list-row>
            </li>
          }
        </ul>

        <div class="card-actions">
          <button
            mat-button
            color="warn"
            type="button"
            [attr.data-testid]="'plans-delete-' + plan().id"
            (click)="deleteRequested.emit()"
          >
            Delete
          </button>
        </div>
      </ui-card>
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .plan-frame {
      border-radius: var(--qf-radius-md);
      height: 100%;
    }

    // Accent outline marks the plan that is starting now.
    .plan-highlight {
      outline: 2px solid var(--qf-accent);
      outline-offset: 2px;
    }

    .plan-card {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-3);
      height: 100%;
      box-sizing: border-box;
    }

    .card-head {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-1);
    }

    .card-title {
      margin: 0;
      color: var(--qf-text);
      overflow-wrap: anywhere;
    }

    .window {
      margin: 0;
      color: var(--qf-text-muted);
    }

    .meta {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--qf-space-2);
    }

    .tag {
      padding: var(--qf-space-1) var(--qf-space-2);
      border: 1px solid var(--qf-outline);
      border-radius: var(--qf-radius-pill);
      color: var(--qf-text-muted);
    }

    // Rest time is the largest figure on the card; digits keep their width while it counts down.
    .hero {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      justify-content: space-between;
      gap: var(--qf-space-3);
    }

    .rest {
      margin: 0;
      color: var(--qf-text);
      font-variant-numeric: tabular-nums;
    }

    .progress {
      flex: 1 1 180px;
      min-width: 0;
    }

    .warning {
      display: flex;
      align-items: center;
      gap: var(--qf-space-2);
      margin: 0;
      padding: var(--qf-space-2) var(--qf-space-3);
      border-radius: var(--qf-radius-sm);
      background: var(--qf-warning-bg);
      color: var(--qf-warning);

      mat-icon {
        width: 18px;
        height: 18px;
        font-size: 18px;
        flex-shrink: 0;
      }
    }

    .items {
      list-style: none;
      margin: 0;
      padding: 0;
      border: 1px solid var(--qf-outline);
      border-radius: var(--qf-radius-sm);
      overflow: hidden;
    }

    .type-icon {
      display: inline-block;
      width: 1.5em;
      text-align: center;
    }

    .item-title {
      overflow-wrap: anywhere;
    }

    .item-done {
      text-decoration: line-through;
      color: var(--qf-text-muted);
    }

    .card-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: auto;
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
