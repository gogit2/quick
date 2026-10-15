import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { HabitsApi } from '../../core/api/habits.api';
import { Habit, HabitFrequency } from '../../core/api/models';

// Passing a habit edits it (and shows the active switch); passing nothing creates a new one.
export interface HabitFormDialogData {
  habit?: Habit;
}

type HabitField = 'name' | 'description' | 'frequency' | 'active';

interface HabitForm {
  name: FormControl<string>;
  description: FormControl<string>;
  frequency: FormControl<HabitFrequency>;
  active: FormControl<boolean>;
}

// Closes with the saved Habit, or undefined when cancelled.
@Component({
  selector: 'qf-habit-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.habit ? 'Edit habit' : 'New habit' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content class="form">
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" data-testid="habits-form-name" />
          <mat-error>{{ errorMessage('name') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput rows="3" formControlName="description" data-testid="habits-form-description"></textarea>
          <mat-error>{{ errorMessage('description') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Frequency</mat-label>
          <mat-select formControlName="frequency" data-testid="habits-form-frequency">
            @for (option of frequencies; track option.value) {
              <mat-option [value]="option.value">{{ option.label }}</mat-option>
            }
          </mat-select>
          <mat-error>{{ errorMessage('frequency') }}</mat-error>
        </mat-form-field>

        @if (data.habit) {
          <mat-slide-toggle formControlName="active" data-testid="habits-form-active">Active</mat-slide-toggle>
        }

        @if (generalError()) {
          <p class="error" data-testid="habits-form-error">{{ generalError() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" data-testid="habits-form-cancel" (click)="ref.close()">Cancel</button>
        <button mat-raised-button color="primary" type="submit" data-testid="habits-form-save" [disabled]="saving()">
          Save
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      min-width: 320px;
    }

    .error {
      color: #b00020;
    }
  `,
})
export class HabitFormDialogComponent {
  readonly data = inject<HabitFormDialogData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<HabitFormDialogComponent, Habit | undefined>>(MatDialogRef);
  private readonly api = inject(HabitsApi);

  readonly frequencies: { value: HabitFrequency; label: string }[] = [
    { value: 'DAILY', label: 'Daily' },
    { value: 'WEEKLY', label: 'Weekly' },
  ];

  readonly saving = signal(false);
  readonly serverErrors = signal<Partial<Record<HabitField, string>>>({});
  readonly generalError = signal<string | null>(null);

  readonly form = new FormGroup<HabitForm>({
    // The pattern rejects whitespace-only names.
    name: new FormControl(this.data.habit?.name ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)],
    }),
    description: new FormControl(this.data.habit?.description ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(2000)],
    }),
    frequency: new FormControl<HabitFrequency>(this.data.habit?.frequency ?? 'DAILY', { nonNullable: true }),
    active: new FormControl(this.data.habit?.active ?? true, { nonNullable: true }),
  });

  errorMessage(field: HabitField): string {
    const server = this.serverErrors()[field];
    if (server) {
      return server;
    }
    const control = this.form.controls[field];
    if (control.hasError('required') || control.hasError('pattern')) {
      return field === 'name' ? 'Name is required.' : 'This field is required.';
    }
    if (control.hasError('maxlength')) {
      const max = control.getError('maxlength').requiredLength;
      return `Must be at most ${max} characters.`;
    }
    return '';
  }

  save(): void {
    this.serverErrors.set({});
    this.generalError.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    const common = {
      name: value.name.trim(),
      description: value.description.trim() === '' ? null : value.description,
      frequency: value.frequency,
    };
    const request = this.data.habit
      ? this.api.updateHabit(this.data.habit.id, { ...common, active: value.active })
      : this.api.createHabit(common);

    this.saving.set(true);
    request.subscribe({
      next: (habit) => this.ref.close(habit),
      error: (err: unknown) => {
        this.saving.set(false);
        if (err instanceof ApiRequestError && err.status === 400 && err.body) {
          const fields: Partial<Record<HabitField, string>> = {};
          for (const fieldError of err.body.fieldErrors) {
            fields[fieldError.field as HabitField] = fieldError.message;
          }
          this.serverErrors.set(fields);
          // mat-error only renders for invalid controls, so flag each field the server rejected.
          for (const field of Object.keys(fields) as HabitField[]) {
            this.form.controls[field]?.setErrors({ server: true });
          }
        }
        this.generalError.set(err instanceof ApiRequestError ? err.message : 'The habit could not be saved.');
      },
    });
  }
}
