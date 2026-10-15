import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { Task, TaskPriority, TaskStatus } from '../../core/api/models';
import { TasksApi } from '../../core/api/tasks.api';

// Passing a task edits it; passing nothing creates a new one.
export interface TaskFormDialogData {
  task?: Task;
}

type TaskField = 'title' | 'description' | 'status' | 'priority' | 'dueDate';

interface TaskForm {
  title: FormControl<string>;
  description: FormControl<string>;
  status: FormControl<TaskStatus>;
  priority: FormControl<TaskPriority>;
  dueDate: FormControl<string>;
}

// Closes with the saved Task, or undefined when cancelled.
@Component({
  selector: 'qf-task-form-dialog',
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
  ],
  template: `
    <h2 mat-dialog-title>{{ data.task ? 'Edit task' : 'New task' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content class="form">
        <mat-form-field appearance="outline">
          <mat-label>Title</mat-label>
          <input matInput formControlName="title" data-testid="tasks-form-title" />
          <mat-error>{{ errorMessage('title') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput rows="3" formControlName="description" data-testid="tasks-form-description"></textarea>
          <mat-error>{{ errorMessage('description') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status" data-testid="tasks-form-status">
            @for (option of statuses; track option.value) {
              <mat-option [value]="option.value">{{ option.label }}</mat-option>
            }
          </mat-select>
          <mat-error>{{ errorMessage('status') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Priority</mat-label>
          <mat-select formControlName="priority" data-testid="tasks-form-priority">
            @for (option of priorities; track option.value) {
              <mat-option [value]="option.value">{{ option.label }}</mat-option>
            }
          </mat-select>
          <mat-error>{{ errorMessage('priority') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Due date</mat-label>
          <input matInput type="date" formControlName="dueDate" data-testid="tasks-form-due-date" />
          <mat-error>{{ errorMessage('dueDate') }}</mat-error>
        </mat-form-field>

        @if (generalError()) {
          <p class="error" data-testid="tasks-form-error">{{ generalError() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" data-testid="tasks-form-cancel" (click)="ref.close()">Cancel</button>
        <button
          mat-raised-button
          color="primary"
          type="submit"
          data-testid="tasks-form-save"
          [disabled]="saving() || form.invalid"
        >
          Save
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-3);
      min-width: 320px;
    }

    .error {
      color: var(--qf-danger);
    }
  `,
})
export class TaskFormDialogComponent {
  readonly data = inject<TaskFormDialogData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<TaskFormDialogComponent, Task | undefined>>(MatDialogRef);
  private readonly api = inject(TasksApi);

  readonly statuses: { value: TaskStatus; label: string }[] = [
    { value: 'TODO', label: 'To do' },
    { value: 'IN_PROGRESS', label: 'In progress' },
    { value: 'DONE', label: 'Done' },
  ];

  readonly priorities: { value: TaskPriority; label: string }[] = [
    { value: 'LOW', label: 'Low' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HIGH', label: 'High' },
  ];

  readonly saving = signal(false);
  readonly serverErrors = signal<Partial<Record<TaskField, string>>>({});
  readonly generalError = signal<string | null>(null);

  readonly form = new FormGroup<TaskForm>({
    // The pattern rejects whitespace-only titles (BR1).
    title: new FormControl(this.data.task?.title ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)],
    }),
    description: new FormControl(this.data.task?.description ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(2000)],
    }),
    status: new FormControl<TaskStatus>(this.data.task?.status ?? 'TODO', { nonNullable: true }),
    priority: new FormControl<TaskPriority>(this.data.task?.priority ?? 'MEDIUM', { nonNullable: true }),
    dueDate: new FormControl(this.data.task?.dueDate ?? '', { nonNullable: true }),
  });

  errorMessage(field: TaskField): string {
    const server = this.serverErrors()[field];
    if (server) {
      return server;
    }
    const control = this.form.controls[field];
    if (control.hasError('required') || control.hasError('pattern')) {
      return field === 'title' ? 'Title is required.' : 'This field is required.';
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
    const body = {
      title: value.title.trim(),
      description: value.description.trim() === '' ? null : value.description,
      status: value.status,
      priority: value.priority,
      dueDate: value.dueDate === '' ? null : value.dueDate,
    };
    const request = this.data.task
      ? this.api.updateTask(this.data.task.id, body)
      : this.api.createTask(body);

    this.saving.set(true);
    request.subscribe({
      next: (task) => this.ref.close(task),
      error: (err: unknown) => {
        this.saving.set(false);
        if (err instanceof ApiRequestError && err.status === 400 && err.body) {
          const fields: Partial<Record<TaskField, string>> = {};
          for (const fieldError of err.body.fieldErrors) {
            fields[fieldError.field as TaskField] = fieldError.message;
          }
          this.serverErrors.set(fields);
          // mat-error only renders for invalid controls, so flag each field the server rejected.
          for (const field of Object.keys(fields) as TaskField[]) {
            this.form.controls[field]?.setErrors({ server: true });
          }
        }
        this.generalError.set(err instanceof ApiRequestError ? err.message : 'The task could not be saved.');
      },
    });
  }
}
