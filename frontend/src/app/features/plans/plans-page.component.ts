import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, fromEvent, interval, merge, Subject, catchError, of, switchMap, tap } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { Plan, PlanGroup } from '../../core/api/models';
import { PlansApi } from '../../core/api/plans.api';
import { NotificationService } from '../../core/notification.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { PlanBuilderDialogComponent } from './plan-builder-dialog.component';
import { PlanCardComponent } from './plan-card.component';

// Plans are reloaded this often and on window focus, so the server status wins over the local view.
const RELOAD_INTERVAL_MS = 30_000;

@Component({
  selector: 'qf-plans-page',
  standalone: true,
  imports: [MatButtonModule, MatProgressBarModule, EmptyStateComponent, PlanCardComponent],
  template: `
    <header class="page-header">
      <h1 data-testid="plans-title">Plans</h1>
      <button mat-raised-button color="primary" type="button" data-testid="plans-create-button" (click)="openCreate()">
        Create plan
      </button>
    </header>

    @if (loading()) {
      <mat-progress-bar mode="indeterminate" data-testid="plans-loading" />
    }

    @if (loadFailed()) {
      <p class="notice" data-testid="plans-load-error">Plans could not be loaded.</p>
    } @else if (!loading() && active().length === 0 && completed().length === 0) {
      <qf-empty-state
        testId="plans"
        icon="▤"
        title="No plans yet"
        message="Create a plan to group tasks, habits and learning into a timed block."
        actionLabel="Create plan"
        (action)="openCreate()"
      />
    }

    @if (active().length > 0) {
      <section data-testid="plans-group-active">
        <h2>Active and upcoming</h2>
        <div class="card-grid">
          @for (plan of active(); track plan.id) {
            <qf-plan-card
              [plan]="plan"
              [highlighted]="notifications.highlightedIds().has(plan.id)"
              (updated)="onUpdated($event)"
              (deleteRequested)="confirmDelete(plan)"
            />
          }
        </div>
      </section>
    }

    @if (completed().length > 0) {
      <section data-testid="plans-group-completed">
        <h2>History</h2>
        <div class="card-grid">
          @for (plan of completed(); track plan.id) {
            <qf-plan-card [plan]="plan" (updated)="onUpdated($event)" (deleteRequested)="confirmDelete(plan)" />
          }
        </div>
      </section>
    }
  `,
  styles: `
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }

    .notice {
      color: #b00020;
      margin: 16px 0;
    }

    section h2 {
      margin-top: 24px;
      font-size: 18px;
    }

    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
      margin-top: 8px;
    }
  `,
})
export class PlansPageComponent implements OnInit {
  private readonly api = inject(PlansApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  readonly notifications = inject(NotificationService);

  readonly active = signal<Plan[]>([]);
  readonly completed = signal<Plan[]>([]);
  readonly loading = signal(false);
  // Set when the last list request failed, so the page does not claim "no plans".
  readonly loadFailed = signal(false);

  private hasLoaded = false;

  // Every list request goes through here; switchMap drops responses to superseded requests.
  private readonly reload = new Subject<void>();

  constructor() {
    this.reload
      .pipe(
        tap(() => {
          // Show the spinner only before the first answer, so periodic refreshes do not flicker.
          if (!this.hasLoaded) {
            this.loading.set(true);
          }
        }),
        switchMap(() =>
          forkJoin([this.api.listPlans('ACTIVE_UPCOMING'), this.api.listPlans('COMPLETED')]).pipe(
            catchError(() => of(null)),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        this.loading.set(false);
        if (result === null) {
          this.loadFailed.set(true);
          return;
        }
        this.hasLoaded = true;
        this.loadFailed.set(false);
        this.active.set(result[0]);
        this.completed.set(result[1]);
      });

    // Server status wins: refresh on a timer and when the user comes back to the window.
    merge(interval(RELOAD_INTERVAL_MS), fromEvent(window, 'focus'))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.reload.next();
  }

  openCreate(): void {
    this.dialog
      .open<PlanBuilderDialogComponent, void, Plan | undefined>(PlanBuilderDialogComponent)
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  // Replaces the plan in place right away; a reload then moves it to the right group if its status changed.
  onUpdated(plan: Plan): void {
    this.active.update((plans) => this.replace(plans, plan));
    this.completed.update((plans) => this.replace(plans, plan));
    this.load();
  }

  confirmDelete(plan: Plan): void {
    const data: ConfirmDialogData = {
      title: 'Delete plan?',
      message: `"${plan.title}" is removed. Its tasks, habits and learning resources are kept.`,
      confirmLabel: 'Delete',
    };
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, { data })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.api.deletePlan(plan.id).subscribe({
          next: () => this.load(),
          error: (err: unknown) => {
            // 5xx errors already show a snackbar in the HTTP interceptor.
            if (err instanceof ApiRequestError && err.status >= 500) {
              return;
            }
            this.snackBar.open('The plan could not be deleted.', 'Dismiss', { duration: 6000 });
          },
        });
      });
  }

  private replace(plans: Plan[], plan: Plan): Plan[] {
    return plans.map((existing) => (existing.id === plan.id ? plan : existing));
  }
}
