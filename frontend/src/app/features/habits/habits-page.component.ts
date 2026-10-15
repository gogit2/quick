import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, of, switchMap, tap } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { HabitsApi } from '../../core/api/habits.api';
import { Habit } from '../../core/api/models';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { UiCardComponent } from '../../shared/ui-card.component';
import { UiPageHeaderComponent } from '../../shared/ui-page-header.component';
import { UiSkeletonComponent } from '../../shared/ui-skeleton.component';
import { HabitCompletionService } from './habit-completion.service';
import { HabitFormDialogComponent, HabitFormDialogData } from './habit-form-dialog.component';

@Component({
  selector: 'qf-habits-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    EmptyStateComponent,
    UiCardComponent,
    UiPageHeaderComponent,
    UiSkeletonComponent,
  ],
  templateUrl: './habits-page.component.html',
  styleUrl: './habits-page.component.scss',
})
export class HabitsPageComponent implements OnInit {
  private readonly api = inject(HabitsApi);
  private readonly completion = inject(HabitCompletionService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly habits = signal<Habit[]>([]);
  readonly loading = signal(false);
  // Set when the last list request failed, so the page does not claim "no habits".
  readonly loadFailed = signal(false);

  // On by default: all habits are shown. Off shows only the active ones.
  readonly showInactive = new FormControl(true, { nonNullable: true });

  // Every list request goes through here; switchMap drops responses to superseded requests.
  private readonly reload = new Subject<void>();

  constructor() {
    this.reload
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.loadFailed.set(false);
        }),
        switchMap(() =>
          this.api.listHabits(this.showInactive.value ? undefined : true).pipe(
            catchError(() => {
              this.loadFailed.set(true);
              return of<Habit[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((habits) => {
        this.habits.set(habits);
        this.loading.set(false);
      });

    this.showInactive.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.reload.next();
  }

  openCreate(): void {
    this.dialog
      .open<HabitFormDialogComponent, HabitFormDialogData, Habit | undefined>(HabitFormDialogComponent, { data: {} })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  openEdit(habit: Habit): void {
    this.dialog
      .open<HabitFormDialogComponent, HabitFormDialogData, Habit | undefined>(HabitFormDialogComponent, {
        data: { habit },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  toggleCompletion(habit: Habit): void {
    if (!habit.active) {
      return;
    }
    this.completion.toggle(habit).subscribe({
      next: () => this.load(),
      error: this.onActionError('The habit could not be updated.'),
    });
  }

  toggleActive(habit: Habit): void {
    this.api
      .updateHabit(habit.id, {
        name: habit.name,
        description: habit.description ?? null,
        frequency: habit.frequency,
        active: !habit.active,
      })
      .subscribe({
        next: () => this.load(),
        error: this.onActionError('The habit could not be updated.'),
      });
  }

  confirmDelete(habit: Habit): void {
    const data: ConfirmDialogData = {
      title: 'Delete habit?',
      message: `"${habit.name}" and all its completions are deleted for good and removed from every plan.`,
      confirmLabel: 'Delete',
    };
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, { data })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.api.deleteHabit(habit.id).subscribe({
          next: () => this.load(),
          error: this.onActionError('The habit could not be deleted.'),
        });
      });
  }

  frequencyLabel(habit: Habit): string {
    return habit.frequency === 'DAILY' ? 'Daily' : 'Weekly';
  }

  streakLabel(habit: Habit): string {
    const unit = habit.frequency === 'DAILY' ? 'day' : 'week';
    return `${habit.currentStreak} ${unit}${habit.currentStreak === 1 ? '' : 's'} streak`;
  }

  progressLabel(habit: Habit): string {
    const period = habit.frequency === 'DAILY' ? 'today' : 'this week';
    return habit.doneForCurrentPeriod ? `Done ${period}` : `Not done ${period}`;
  }

  // 5xx errors already show a snackbar in the HTTP interceptor, so only other failures are shown here.
  private onActionError(message: string) {
    return (err: unknown) => {
      if (err instanceof ApiRequestError && err.status >= 500) {
        return;
      }
      this.snackBar.open(message, 'Dismiss', { duration: 6000 });
    };
  }
}
