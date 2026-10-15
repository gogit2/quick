import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { forkJoin } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { HabitsApi } from '../../core/api/habits.api';
import { LearningApi } from '../../core/api/learning.api';
import { LearningCard, Habit, Plan, PlanCreate, PlanItemCreate, PlanItemSourceType, Task } from '../../core/api/models';
import { PlansApi } from '../../core/api/plans.api';
import { TasksApi } from '../../core/api/tasks.api';
import { ClockService } from '../../core/clock.service';

const DEFAULT_DURATION_MINUTES = 60;
const MINUTE_MS = 60_000;

interface PlanBuilderForm {
  title: FormControl<string>;
  estimatedDurationMinutes: FormControl<number | null>;
  startDateTime: FormControl<string>;
  endDateTime: FormControl<string>;
  priorityOrder: FormControl<number | null>;
}

// Picker key of a plan item source, e.g. "TASK:4".
function pickKey(sourceType: PlanItemSourceType, sourceId: number): string {
  return `${sourceType}:${sourceId}`;
}

// Value for <input type="datetime-local"> in the browser's local time.
function toLocalInput(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// The window must end after it starts (BR11). Reported as a group error.
function windowOrder(group: AbstractControl): ValidationErrors | null {
  const start = Date.parse(group.value.startDateTime);
  const end = Date.parse(group.value.endDateTime);
  if (Number.isNaN(start) || Number.isNaN(end)) {
    return null;
  }
  return end > start ? null : { windowOrder: true };
}

// Builds a time-boxed plan from existing tasks, habits and learning cards. Closes with the saved Plan.
@Component({
  selector: 'qf-plan-builder-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogActions,
    MatDialogContent,
    MatDialogTitle,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Create plan</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form" (ngSubmit)="save()">
        <mat-form-field appearance="outline">
          <mat-label>Title</mat-label>
          <input matInput data-testid="plans-form-title" formControlName="title" maxlength="200" />
          @if (form.controls.title.touched && form.controls.title.hasError('required')) {
            <mat-error>Title is required.</mat-error>
          }
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Duration (minutes)</mat-label>
            <input matInput type="number" min="1" step="1" data-testid="plans-form-duration" formControlName="estimatedDurationMinutes" />
            @if (form.controls.estimatedDurationMinutes.touched && form.controls.estimatedDurationMinutes.invalid) {
              <mat-error>Enter a whole number of minutes, 1 or more.</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Priority order</mat-label>
            <input matInput type="number" min="1" step="1" data-testid="plans-form-priority" formControlName="priorityOrder" />
            @if (form.controls.priorityOrder.touched && form.controls.priorityOrder.invalid) {
              <mat-error>Enter a whole number, 1 or more.</mat-error>
            }
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Start</mat-label>
            <input matInput type="datetime-local" data-testid="plans-form-start" formControlName="startDateTime" />
            @if (form.controls.startDateTime.touched && form.controls.startDateTime.hasError('required')) {
              <mat-error>Start is required.</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>End</mat-label>
            <input matInput type="datetime-local" data-testid="plans-form-end" formControlName="endDateTime" />
            @if (form.controls.endDateTime.touched && form.controls.endDateTime.hasError('required')) {
              <mat-error>End is required.</mat-error>
            }
          </mat-form-field>
        </div>

        @if (form.hasError('windowOrder') && form.touched) {
          <p class="error" data-testid="plans-form-window-error">End must be after start.</p>
        }

        @if (durationExceedsWindow()) {
          <p class="warning" data-testid="plans-form-duration-warning">
            The estimated duration is longer than the window. The plan will still be saved.
          </p>
        }

        <section data-testid="plans-pick-tasks">
          <h3>Tasks</h3>
          @for (task of tasks(); track task.id) {
            <mat-checkbox
              [attr.data-testid]="'plans-pick-task-' + task.id"
              [checked]="isPicked('TASK', task.id)"
              (change)="setPicked('TASK', task.id, $event.checked)"
            >
              {{ task.title }}
            </mat-checkbox>
          } @empty {
            <p class="muted">No tasks yet.</p>
          }
        </section>

        <section data-testid="plans-pick-habits">
          <h3>Habits</h3>
          @for (habit of habits(); track habit.id) {
            <mat-checkbox
              [attr.data-testid]="'plans-pick-habit-' + habit.id"
              [checked]="isPicked('HABIT', habit.id)"
              (change)="setPicked('HABIT', habit.id, $event.checked)"
            >
              {{ habit.name }}
            </mat-checkbox>
          } @empty {
            <p class="muted">No active habits yet.</p>
          }
        </section>

        <section data-testid="plans-pick-learning">
          <h3>Learning resources</h3>
          @for (card of cards(); track card.id) {
            <mat-checkbox
              [attr.data-testid]="'plans-pick-learning-' + card.id"
              [checked]="isPicked('LEARNING_RESOURCE', card.id)"
              (change)="setPicked('LEARNING_RESOURCE', card.id, $event.checked)"
            >
              {{ card.title }}
            </mat-checkbox>
          } @empty {
            <p class="muted">No learning resources yet.</p>
          }
        </section>

        @if (itemsError()) {
          <p class="error" data-testid="plans-form-items-error">Pick at least one task, habit or learning resource.</p>
        }

        @for (message of serverErrors(); track $index) {
          <p class="error" data-testid="plans-form-server-error">{{ message }}</p>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" data-testid="plans-form-cancel" (click)="ref.close()">Cancel</button>
      <button mat-raised-button color="primary" type="button" data-testid="plans-form-save" [disabled]="saving()" (click)="save()">
        Save
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: min(560px, 90vw);
    }

    .row {
      display: flex;
      gap: 12px;
    }

    .row mat-form-field {
      flex: 1;
    }

    h3 {
      margin: 16px 0 4px;
      font-size: 15px;
    }

    section mat-checkbox {
      display: block;
    }

    .muted {
      color: rgba(0, 0, 0, 0.6);
      margin: 4px 0;
    }

    .error {
      color: #b00020;
      margin: 4px 0;
    }

    .warning {
      color: #8a5a00;
      margin: 4px 0;
    }
  `,
})
export class PlanBuilderDialogComponent implements OnInit {
  private readonly plansApi = inject(PlansApi);
  private readonly tasksApi = inject(TasksApi);
  private readonly habitsApi = inject(HabitsApi);
  private readonly learningApi = inject(LearningApi);
  private readonly clock = inject(ClockService);
  readonly ref = inject<MatDialogRef<PlanBuilderDialogComponent, Plan | undefined>>(MatDialogRef);

  readonly tasks = signal<Task[]>([]);
  readonly habits = signal<Habit[]>([]);
  readonly cards = signal<LearningCard[]>([]);

  // Keys like "TASK:4" of the picked items.
  readonly picked = signal<ReadonlySet<string>>(new Set());
  readonly itemsError = signal(false);
  readonly serverErrors = signal<string[]>([]);
  readonly saving = signal(false);

  readonly form = new FormGroup<PlanBuilderForm>(
    {
      title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
      estimatedDurationMinutes: new FormControl<number | null>(DEFAULT_DURATION_MINUTES, [
        Validators.required,
        Validators.min(1),
        Validators.pattern(/^\d+$/),
      ]),
      startDateTime: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      endDateTime: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      priorityOrder: new FormControl<number | null>(1, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]),
    },
    { validators: windowOrder },
  );

  ngOnInit(): void {
    // Default window: the next whole minute, lasting the default duration.
    const start = new Date(Math.ceil(this.clock.now().getTime() / MINUTE_MS) * MINUTE_MS);
    this.form.patchValue({
      startDateTime: toLocalInput(start),
      endDateTime: toLocalInput(new Date(start.getTime() + DEFAULT_DURATION_MINUTES * MINUTE_MS)),
    });

    forkJoin({
      tasks: this.tasksApi.listTasks({ archived: false }),
      habits: this.habitsApi.listHabits(true),
      cards: this.learningApi.listCards(),
    }).subscribe(({ tasks, habits, cards }) => {
      // Open tasks first; DONE tasks follow. The order inside each group is kept.
      this.tasks.set([...tasks.filter((t) => t.status !== 'DONE'), ...tasks.filter((t) => t.status === 'DONE')]);
      this.habits.set(habits);
      this.cards.set(cards);
    });
  }

  durationExceedsWindow(): boolean {
    const minutes = Number(this.form.controls.estimatedDurationMinutes.value);
    const start = Date.parse(this.form.controls.startDateTime.value);
    const end = Date.parse(this.form.controls.endDateTime.value);
    if (!Number.isFinite(minutes) || minutes <= 0 || Number.isNaN(start) || Number.isNaN(end) || end <= start) {
      return false;
    }
    return minutes * MINUTE_MS > end - start;
  }

  isPicked(sourceType: PlanItemSourceType, sourceId: number): boolean {
    return this.picked().has(pickKey(sourceType, sourceId));
  }

  setPicked(sourceType: PlanItemSourceType, sourceId: number, checked: boolean): void {
    const next = new Set(this.picked());
    if (checked) {
      next.add(pickKey(sourceType, sourceId));
    } else {
      next.delete(pickKey(sourceType, sourceId));
    }
    this.picked.set(next);
    if (next.size > 0) {
      this.itemsError.set(false);
    }
  }

  save(): void {
    this.serverErrors.set([]);
    this.form.markAllAsTouched();
    const items = this.pickedItems();
    this.itemsError.set(items.length === 0);
    if (this.form.invalid || items.length === 0 || this.saving()) {
      return;
    }

    const raw = this.form.getRawValue();
    const body: PlanCreate = {
      title: raw.title.trim(),
      estimatedDurationMinutes: Number(raw.estimatedDurationMinutes),
      startDateTime: new Date(raw.startDateTime).toISOString(),
      endDateTime: new Date(raw.endDateTime).toISOString(),
      priorityOrder: Number(raw.priorityOrder),
      items,
    };

    this.saving.set(true);
    this.plansApi.createPlan(body).subscribe({
      next: (plan) => this.ref.close(plan),
      error: (err: unknown) => {
        this.saving.set(false);
        if (err instanceof ApiRequestError && err.status === 400 && err.body) {
          this.serverErrors.set(err.body.fieldErrors.map((fieldError) => fieldError.message));
          return;
        }
        this.serverErrors.set([err instanceof ApiRequestError ? err.message : 'The plan could not be saved.']);
      },
    });
  }

  private pickedItems(): PlanItemCreate[] {
    return [...this.picked()].map((key) => {
      const [sourceType, sourceId] = key.split(':');
      return { sourceType: sourceType as PlanItemSourceType, sourceId: Number(sourceId) };
    });
  }
}
