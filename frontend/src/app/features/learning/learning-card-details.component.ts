import { Component, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, FormGroup, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { LearningApi } from '../../core/api/learning.api';
import { LearningCard, LearningMilestone } from '../../core/api/models';

interface MilestoneForm {
  title: FormControl<string>;
  targetDate: FormControl<string>;
}

interface NoteForm {
  text: FormControl<string>;
}

// Milestones and notes of one expanded card. Every change emits `changed` so the page reloads the
// cards, which is where the status and the progress are derived.
@Component({
  selector: 'qf-learning-card-details',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="details" [attr.aria-label]="'Details of ' + card().title">
      <h3>Milestones</h3>
      <ul class="milestones">
        @for (milestone of card().milestones; track milestone.id) {
          <li class="row" [attr.data-testid]="'learning-milestone-' + milestone.id">
            <mat-checkbox
              [checked]="milestone.done"
              [attr.data-testid]="'learning-milestone-done-' + milestone.id"
              (change)="setDone(milestone, $event.checked)"
            >
              {{ milestone.title }}
            </mat-checkbox>
            <span class="muted">{{ targetLabel(milestone) }}</span>
            <button
              mat-button
              color="warn"
              type="button"
              [attr.data-testid]="'learning-milestone-delete-' + milestone.id"
              (click)="removeMilestone(milestone)"
            >
              Remove
            </button>
          </li>
        } @empty {
          <li class="muted">No milestones yet.</li>
        }
      </ul>

      <form class="add-row" [formGroup]="milestoneForm" #milestoneFormDir="ngForm" (ngSubmit)="addMilestone(milestoneFormDir)" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>Milestone</mat-label>
          <input matInput formControlName="title" [attr.data-testid]="'learning-milestone-title-' + card().id" />
          <mat-error>{{ milestoneTitleError() }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Target date</mat-label>
          <input
            matInput
            type="date"
            formControlName="targetDate"
            [attr.data-testid]="'learning-milestone-target-' + card().id"
          />
        </mat-form-field>
        <button
          mat-stroked-button
          type="submit"
          [attr.data-testid]="'learning-milestone-add-' + card().id"
          [disabled]="saving() || milestoneForm.invalid"
        >
          Add milestone
        </button>
      </form>

      <h3>Notes</h3>
      <ul class="notes">
        @for (note of card().notes; track note.id) {
          <li class="row" [attr.data-testid]="'learning-note-' + note.id">
            <div class="note-body">
              <p>{{ note.text }}</p>
              <span class="muted">{{ note.createdAt | date: 'medium' }}</span>
            </div>
            <button
              mat-button
              color="warn"
              type="button"
              [attr.data-testid]="'learning-note-delete-' + note.id"
              (click)="removeNote(note.id)"
            >
              Remove
            </button>
          </li>
        } @empty {
          <li class="muted">No notes yet.</li>
        }
      </ul>

      <form class="add-row" [formGroup]="noteForm" #noteFormDir="ngForm" (ngSubmit)="addNote(noteFormDir)" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>Note</mat-label>
          <textarea matInput rows="2" formControlName="text" [attr.data-testid]="'learning-note-text-' + card().id"></textarea>
          <mat-error>{{ noteTextError() }}</mat-error>
        </mat-form-field>
        <button
          mat-stroked-button
          type="submit"
          [attr.data-testid]="'learning-note-add-' + card().id"
          [disabled]="saving() || noteForm.invalid"
        >
          Add note
        </button>
      </form>
    </section>
  `,
  styles: `
    .details {
      padding: 0 16px 16px;
    }

    h3 {
      font-size: 16px;
      font-weight: 500;
      margin: 12px 0 4px;
    }

    ul {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 4px 0;
    }

    .note-body {
      display: flex;
      flex-direction: column;
      min-width: 0;

      p {
        margin: 0;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
      }
    }

    .add-row {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      gap: 8px;
    }

    .muted {
      color: rgba(0, 0, 0, 0.6);
      font-size: 13px;
    }
  `,
})
export class LearningCardDetailsComponent {
  readonly card = input.required<LearningCard>();
  readonly changed = output<void>();

  private readonly api = inject(LearningApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly saving = signal(false);

  readonly milestoneForm = new FormGroup<MilestoneForm>({
    // The pattern rejects whitespace-only titles.
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)],
    }),
    targetDate: new FormControl('', { nonNullable: true }),
  });

  readonly noteForm = new FormGroup<NoteForm>({
    text: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(4000)],
    }),
  });

  milestoneTitleError(): string {
    const control = this.milestoneForm.controls.title;
    if (control.hasError('required') || control.hasError('pattern')) {
      return 'Milestone title is required.';
    }
    if (control.hasError('maxlength')) {
      return `Must be at most ${control.getError('maxlength').requiredLength} characters.`;
    }
    return '';
  }

  noteTextError(): string {
    const control = this.noteForm.controls.text;
    if (control.hasError('required') || control.hasError('pattern')) {
      return 'Note text is required.';
    }
    if (control.hasError('maxlength')) {
      return `Must be at most ${control.getError('maxlength').requiredLength} characters.`;
    }
    return '';
  }

  targetLabel(milestone: LearningMilestone): string {
    return milestone.targetDate ? `Target ${milestone.targetDate}` : 'No target date';
  }

  // resetForm() (not reset()) also clears the submitted flag, so no "required" error shows after a successful add.
  addMilestone(formDir: FormGroupDirective): void {
    this.milestoneForm.markAllAsTouched();
    if (this.milestoneForm.invalid) {
      return;
    }
    const value = this.milestoneForm.getRawValue();
    this.saving.set(true);
    this.api
      .createMilestone(this.card().id, {
        title: value.title.trim(),
        targetDate: value.targetDate === '' ? null : value.targetDate,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          formDir.resetForm();
          this.changed.emit();
        },
        error: this.onActionError('The milestone could not be added.'),
      });
  }

  // PUT sends the whole milestone; only `done` changes here.
  setDone(milestone: LearningMilestone, done: boolean): void {
    this.api
      .updateMilestone(this.card().id, milestone.id, {
        title: milestone.title,
        done,
        targetDate: milestone.targetDate ?? null,
      })
      .subscribe({
        next: () => this.changed.emit(),
        error: this.onActionError('The milestone could not be updated.'),
      });
  }

  removeMilestone(milestone: LearningMilestone): void {
    this.api.deleteMilestone(this.card().id, milestone.id).subscribe({
      next: () => this.changed.emit(),
      error: this.onActionError('The milestone could not be removed.'),
    });
  }

  addNote(formDir: FormGroupDirective): void {
    this.noteForm.markAllAsTouched();
    if (this.noteForm.invalid) {
      return;
    }
    this.saving.set(true);
    this.api.createNote(this.card().id, { text: this.noteForm.getRawValue().text.trim() }).subscribe({
      next: () => {
        this.saving.set(false);
        formDir.resetForm();
        this.changed.emit();
      },
      error: this.onActionError('The note could not be added.'),
    });
  }

  removeNote(noteId: number): void {
    this.api.deleteNote(this.card().id, noteId).subscribe({
      next: () => this.changed.emit(),
      error: this.onActionError('The note could not be removed.'),
    });
  }

  // 5xx errors already show a snackbar in the HTTP interceptor, so only other failures are shown here.
  private onActionError(message: string) {
    return (err: unknown) => {
      this.saving.set(false);
      if (err instanceof ApiRequestError && err.status >= 500) {
        return;
      }
      this.snackBar.open(message, 'Dismiss', { duration: 6000 });
    };
  }
}
