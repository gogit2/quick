import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { LearningApi } from '../../core/api/learning.api';
import { LearningCard, LearningStatus } from '../../core/api/models';

// Passing a card edits it; passing nothing creates a new one.
export interface LearningCardDialogData {
  card?: LearningCard;
}

type LearningCardField = 'title' | 'description' | 'status';

interface LearningCardForm {
  title: FormControl<string>;
  description: FormControl<string>;
  status: FormControl<LearningStatus>;
}

// Closes with the saved LearningCard, or undefined when cancelled.
@Component({
  selector: 'qf-learning-card-dialog',
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
    <h2 mat-dialog-title>{{ data.card ? 'Edit learning resource' : 'New learning resource' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content class="form">
        <mat-form-field appearance="outline">
          <mat-label>Title</mat-label>
          <input matInput formControlName="title" data-testid="learning-form-title" />
          <mat-error>{{ errorMessage('title') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Description or source</mat-label>
          <textarea matInput rows="3" formControlName="description" data-testid="learning-form-description"></textarea>
          <mat-error>{{ errorMessage('description') }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status" data-testid="learning-form-status">
            @for (option of statuses; track option.value) {
              <mat-option [value]="option.value">{{ option.label }}</mat-option>
            }
          </mat-select>
          @if (hasMilestones) {
            <mat-hint>Derived from the milestones. Remove them all to set the status by hand.</mat-hint>
          }
          <mat-error>{{ errorMessage('status') }}</mat-error>
        </mat-form-field>

        @if (generalError()) {
          <p class="error" data-testid="learning-form-error">{{ generalError() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" data-testid="learning-form-cancel" (click)="ref.close()">Cancel</button>
        <button
          mat-raised-button
          color="primary"
          type="submit"
          data-testid="learning-form-save"
          [disabled]="saving()"
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
      min-width: 320px;
    }

    .error {
      color: #b00020;
    }
  `,
})
export class LearningCardDialogComponent {
  readonly data = inject<LearningCardDialogData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<LearningCardDialogComponent, LearningCard | undefined>>(MatDialogRef);
  private readonly api = inject(LearningApi);

  readonly statuses: { value: LearningStatus; label: string }[] = [
    { value: 'NOT_STARTED', label: 'Not started' },
    { value: 'IN_PROGRESS', label: 'In progress' },
    { value: 'COMPLETED', label: 'Completed' },
  ];

  readonly hasMilestones = (this.data.card?.milestones.length ?? 0) > 0;

  readonly saving = signal(false);
  readonly serverErrors = signal<Partial<Record<LearningCardField, string>>>({});
  readonly generalError = signal<string | null>(null);

  readonly form = new FormGroup<LearningCardForm>({
    // The pattern rejects whitespace-only titles.
    title: new FormControl(this.data.card?.title ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)],
    }),
    description: new FormControl(this.data.card?.description ?? '', {
      nonNullable: true,
      validators: [Validators.maxLength(2000)],
    }),
    status: new FormControl<LearningStatus>(this.data.card?.status ?? 'NOT_STARTED', { nonNullable: true }),
  });

  constructor() {
    // The status is only accepted while the card has no milestones (D-7).
    if (this.hasMilestones) {
      this.form.controls.status.disable();
    }
  }

  errorMessage(field: LearningCardField): string {
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
    const title = value.title.trim();
    const description = value.description.trim() === '' ? null : value.description;
    // A disabled status is left out of the body, so the server keeps the derived status.
    const status = this.hasMilestones ? undefined : value.status;
    const request = this.data.card
      ? this.api.updateCard(this.data.card.id, { title, description, status })
      : this.api.createCard({ title, description, status });

    this.saving.set(true);
    request.subscribe({
      next: (card) => this.ref.close(card),
      error: (err: unknown) => {
        this.saving.set(false);
        if (err instanceof ApiRequestError && err.status === 400 && err.body) {
          const fields: Partial<Record<LearningCardField, string>> = {};
          for (const fieldError of err.body.fieldErrors) {
            fields[fieldError.field as LearningCardField] = fieldError.message;
          }
          this.serverErrors.set(fields);
          // mat-error only renders for invalid controls, so flag each field the server rejected.
          for (const field of Object.keys(fields) as LearningCardField[]) {
            this.form.controls[field]?.setErrors({ server: true });
          }
        }
        this.generalError.set(err instanceof ApiRequestError ? err.message : 'The learning resource could not be saved.');
      },
    });
  }
}
