import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, catchError, map, of, switchMap, throwError } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { HabitsApi } from '../../core/api/habits.api';
import { Habit } from '../../core/api/models';

// The one place that marks a habit done or undoes it. The habit page and the dashboard toggle (M06) both use it.
@Injectable({ providedIn: 'root' })
export class HabitCompletionService {
  private readonly api = inject(HabitsApi);
  private readonly snackBar = inject(MatSnackBar);

  // Not done yet: complete for today (the server picks the date). Done: remove the newest completion in the
  // current period. Emits once the change is final; the caller reloads the habit. A 409 shows "Already completed"
  // and still emits, so the caller can refresh. Other errors are passed on to the caller.
  toggle(habit: Habit): Observable<void> {
    if (!habit.doneForCurrentPeriod) {
      return this.api.completeHabit(habit.id).pipe(
        map(() => undefined),
        catchError((err: unknown) => this.alreadyCompleted(err)),
      );
    }
    return this.api.listCompletions(habit.id, { from: habit.periodStart, to: habit.periodEnd }).pipe(
      // The API returns completions newest first.
      switchMap((completions) =>
        completions.length > 0
          ? this.api.deleteCompletion(habit.id, completions[0].completionDate)
          : of(undefined),
      ),
      map(() => undefined),
    );
  }

  private alreadyCompleted(err: unknown): Observable<void> {
    if (err instanceof ApiRequestError && err.status === 409) {
      this.snackBar.open('Already completed', 'Dismiss', { duration: 6000 });
      return of(undefined);
    }
    return throwError(() => err);
  }
}
