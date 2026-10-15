import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxChange, MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, fromEvent, interval, merge, of, Subject, switchMap, tap } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { DashboardApi } from '../../core/api/dashboard.api';
import { Dashboard, Habit, LearningCard, Plan, PlanItem, Task } from '../../core/api/models';
import { PlansApi } from '../../core/api/plans.api';
import { TasksApi } from '../../core/api/tasks.api';
import { ClockService } from '../../core/clock.service';
import { HabitCompletionService } from '../habits/habit-completion.service';
import { HabitFormDialogComponent, HabitFormDialogData } from '../habits/habit-form-dialog.component';
import { LearningCardDialogComponent, LearningCardDialogData } from '../learning/learning-card-dialog.component';
import { restSecondsAt } from '../plans/plan-clock';
import { PlanBuilderDialogComponent } from '../plans/plan-builder-dialog.component';
import { TaskFormDialogComponent, TaskFormDialogData } from '../tasks/task-form-dialog.component';
import { RestTimePipe } from '../../shared/rest-time.pipe';
import { UiCardComponent } from '../../shared/ui-card.component';
import { UiChipComponent } from '../../shared/ui-chip.component';
import { UiListRowComponent } from '../../shared/ui-list-row.component';
import { UiSkeletonComponent } from '../../shared/ui-skeleton.component';

// The dashboard is reloaded this often and on window focus, so it matches the lists behind it.
const RELOAD_INTERVAL_MS = 30_000;

@Component({
  selector: 'qf-dashboard-page',
  standalone: true,
  imports: [
    DatePipe,
    RestTimePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressBarModule,
    UiCardComponent,
    UiChipComponent,
    UiListRowComponent,
    UiSkeletonComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent implements OnInit {
  private readonly api = inject(DashboardApi);
  private readonly tasksApi = inject(TasksApi);
  private readonly habitCompletion = inject(HabitCompletionService);
  private readonly plansApi = inject(PlansApi);
  private readonly clock = inject(ClockService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(false);
  // Set when the last request failed, so the page does not show stale or empty numbers as real.
  readonly loadFailed = signal(false);

  private hasLoaded = false;

  // Every request goes through here; switchMap drops responses to superseded requests.
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
        switchMap(() => this.api.getDashboard().pipe(catchError(() => of(null)))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dashboard) => {
        this.loading.set(false);
        if (dashboard === null) {
          this.loadFailed.set(true);
          return;
        }
        this.hasLoaded = true;
        this.loadFailed.set(false);
        this.data.set(dashboard);
      });

    // Server state wins: refresh on a timer and when the user comes back to the window.
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

  // Read inside the template, so it re-evaluates on every clock tick.
  greeting(): string {
    const hour = this.clock.now().getHours();
    const part = hour < 12 ? 'morning' : hour < 18 ? 'afternoon' : 'evening';
    const name = this.data()?.displayName;
    return name ? `Good ${part}, ${name}` : `Good ${part}`;
  }

  // Rest time of an IN_PROGRESS plan; re-evaluated on every clock tick.
  rest(plan: Plan): number | null {
    return restSecondsAt(plan, this.clock.now());
  }

  // Quick-add opens the same dialogs as the list pages and reloads the dashboard after a save.
  addTask(): void {
    this.dialog
      .open<TaskFormDialogComponent, TaskFormDialogData, Task | undefined>(TaskFormDialogComponent, { data: {} })
      .afterClosed()
      .subscribe((saved) => saved && this.load());
  }

  addHabit(): void {
    this.dialog
      .open<HabitFormDialogComponent, HabitFormDialogData, Habit | undefined>(HabitFormDialogComponent, { data: {} })
      .afterClosed()
      .subscribe((saved) => saved && this.load());
  }

  addLearning(): void {
    this.dialog
      .open<LearningCardDialogComponent, LearningCardDialogData, LearningCard | undefined>(LearningCardDialogComponent, {
        data: {},
      })
      .afterClosed()
      .subscribe((saved) => saved && this.load());
  }

  addPlan(): void {
    this.dialog
      .open<PlanBuilderDialogComponent, void, Plan | undefined>(PlanBuilderDialogComponent)
      .afterClosed()
      .subscribe((saved) => saved && this.load());
  }

  streakLabel(habit: Habit): string {
    return habit.frequency === 'WEEKLY'
      ? `Streak ${habit.currentStreak} ${habit.currentStreak === 1 ? 'week' : 'weeks'}`
      : `Streak ${habit.currentStreak} ${habit.currentStreak === 1 ? 'day' : 'days'}`;
  }

  completeTask(task: Task): void {
    this.tasksApi.completeTask(task.id).subscribe({
      next: () => this.load(),
      error: (err: unknown) => this.onActionError(err, 'The task could not be completed.'),
    });
  }

  // The shared helper decides complete or undo; the checkbox state follows the next reload.
  toggleHabit(habit: Habit, event: MatCheckboxChange): void {
    this.habitCompletion.toggle(habit).subscribe({
      next: () => this.load(),
      error: (err: unknown) => {
        // The bound value did not change, so the reload alone would leave the box toggled.
        event.source.checked = habit.doneForCurrentPeriod;
        this.onActionError(err, 'The habit could not be updated.');
        this.load();
      },
    });
  }

  togglePlanItem(plan: Plan, item: PlanItem, event: MatCheckboxChange): void {
    this.plansApi.setItemDone(plan.id, item.id, event.checked).subscribe({
      next: () => this.load(),
      error: (err: unknown) => {
        event.source.checked = item.done;
        this.onActionError(err, 'The plan item could not be updated.');
        this.load();
      },
    });
  }

  private onActionError(err: unknown, message: string): void {
    // 5xx errors already show a snackbar in the HTTP interceptor.
    if (err instanceof ApiRequestError && err.status >= 500) {
      return;
    }
    this.snackBar.open(message, 'Dismiss', { duration: 6000 });
  }
}
