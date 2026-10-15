import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, of, switchMap, tap } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { LearningApi } from '../../core/api/learning.api';
import { LearningCard, LearningStatus } from '../../core/api/models';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { LearningCardDetailsComponent } from './learning-card-details.component';
import { LearningCardDialogComponent, LearningCardDialogData } from './learning-card-dialog.component';

@Component({
  selector: 'qf-learning-page',
  standalone: true,
  imports: [MatButtonModule, MatCardModule, MatProgressBarModule, EmptyStateComponent, LearningCardDetailsComponent],
  template: `
    <header class="page-header">
      <h1 data-testid="learning-title">Learning</h1>
      <button mat-raised-button color="primary" type="button" data-testid="learning-add-button" (click)="openCreate()">
        Add learning resource
      </button>
    </header>

    @if (loading()) {
      <mat-progress-bar mode="indeterminate" data-testid="learning-loading" />
    }

    @if (loadFailed()) {
      <div class="error-state" role="alert" data-testid="learning-load-error">
        <span>Learning resources could not be loaded.</span>
        <button mat-stroked-button type="button" data-testid="learning-error-retry" (click)="load()">Retry</button>
      </div>
    } @else if (!loading() && cards().length === 0) {
      <qf-empty-state
        testId="learning"
        icon="✎"
        title="No learning resources yet"
        message="Add a course, book or video to track milestones and notes."
        actionLabel="Add learning resource"
        (action)="openCreate()"
      />
    }

    <div class="card-grid">
      @for (card of cards(); track card.id) {
        <mat-card class="learning-card" [attr.data-testid]="'learning-card-' + card.id">
          <mat-card-header>
            <mat-card-title class="card-title">{{ card.title }}</mat-card-title>
            @if (card.description) {
              <mat-card-subtitle class="card-description">{{ card.description }}</mat-card-subtitle>
            }
          </mat-card-header>

          <mat-card-content>
            <div class="meta">
              <span class="chip" [attr.data-testid]="'learning-status-' + card.id">{{ statusLabel(card.status) }}</span>
              <span [attr.data-testid]="'learning-progress-' + card.id">{{ progressLabel(card) }}</span>
            </div>
          </mat-card-content>

          <mat-card-actions align="end">
            <button
              mat-button
              type="button"
              [attr.data-testid]="'learning-expand-' + card.id"
              [attr.aria-expanded]="isExpanded(card.id)"
              (click)="toggleExpanded(card.id)"
            >
              {{ isExpanded(card.id) ? 'Hide details' : 'Show details' }}
            </button>
            <button mat-button type="button" [attr.data-testid]="'learning-edit-' + card.id" (click)="openEdit(card)">
              Edit
            </button>
            <button
              mat-button
              color="warn"
              type="button"
              [attr.data-testid]="'learning-delete-' + card.id"
              (click)="confirmDelete(card)"
            >
              Delete
            </button>
          </mat-card-actions>

          @if (isExpanded(card.id)) {
            <qf-learning-card-details [card]="card" (changed)="load()" />
          }
        </mat-card>
      }
    </div>
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

    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
      margin-top: 16px;
    }

    .learning-card {
      display: flex;
      flex-direction: column;
    }

    .card-title,
    .card-description {
      overflow-wrap: anywhere;
    }

    .meta {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      font-size: 13px;
    }

    .chip {
      padding: 2px 8px;
      border-radius: 12px;
      background: rgba(0, 0, 0, 0.08);
    }
  `,
})
export class LearningPageComponent implements OnInit {
  private readonly api = inject(LearningApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly cards = signal<LearningCard[]>([]);
  readonly loading = signal(false);
  // Set when the last list request failed, so the page does not claim "no resources".
  readonly loadFailed = signal(false);
  // Ids of the cards whose details (milestones, notes) are open. Survives reloads.
  readonly expandedIds = signal<ReadonlySet<number>>(new Set());

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
          this.api.listCards().pipe(
            catchError(() => {
              this.loadFailed.set(true);
              return of<LearningCard[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((cards) => {
        this.cards.set(cards);
        this.loading.set(false);
      });
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.reload.next();
  }

  openCreate(): void {
    this.dialog
      .open<LearningCardDialogComponent, LearningCardDialogData, LearningCard | undefined>(LearningCardDialogComponent, {
        data: {},
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  openEdit(card: LearningCard): void {
    this.dialog
      .open<LearningCardDialogComponent, LearningCardDialogData, LearningCard | undefined>(LearningCardDialogComponent, {
        data: { card },
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  confirmDelete(card: LearningCard): void {
    const data: ConfirmDialogData = {
      title: 'Delete learning resource?',
      message: `"${card.title}" with its milestones and notes is deleted for good.`,
      confirmLabel: 'Delete',
    };
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, { data })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.api.deleteCard(card.id).subscribe({
          next: () => this.load(),
          error: this.onActionError('The learning resource could not be deleted.'),
        });
      });
  }

  isExpanded(id: number): boolean {
    return this.expandedIds().has(id);
  }

  toggleExpanded(id: number): void {
    const next = new Set(this.expandedIds());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.expandedIds.set(next);
  }

  statusLabel(status: LearningStatus): string {
    switch (status) {
      case 'NOT_STARTED':
        return 'Not started';
      case 'IN_PROGRESS':
        return 'In progress';
      case 'COMPLETED':
        return 'Completed';
    }
  }

  progressLabel(card: LearningCard): string {
    if (card.milestones.length === 0) {
      return 'No milestones yet';
    }
    const done = card.milestones.filter((milestone) => milestone.done).length;
    return `${done}/${card.milestones.length} milestones`;
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
