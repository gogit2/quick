import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
}

// Closes with true when confirmed and false when cancelled. The confirm action is destructive, so it uses the danger token.
@Component({
  selector: 'qf-confirm-dialog',
  standalone: true,
  imports: [MatDialogTitle, MatDialogContent, MatDialogActions, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="qf-title title">
      <mat-icon svgIcon="warning" aria-hidden="true" class="icon"></mat-icon>
      {{ data.title }}
    </h2>
    <mat-dialog-content>
      <p class="message">{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" data-testid="confirm-cancel" (click)="ref.close(false)">Cancel</button>
      <button mat-raised-button type="button" class="danger" data-testid="confirm-ok" (click)="ref.close(true)">
        {{ data.confirmLabel ?? 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .title {
      display: flex;
      align-items: center;
      gap: var(--qf-space-2);
    }

    .icon {
      color: var(--qf-danger);
    }

    .message {
      margin: 0;
      color: var(--qf-text-muted);
    }

    .danger {
      --mdc-protected-button-container-color: var(--qf-danger);
      --mdc-protected-button-label-text-color: var(--mat-sys-on-error, #fff);
    }
  `,
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
  readonly ref = inject<MatDialogRef<ConfirmDialogComponent, boolean>>(MatDialogRef);
}
