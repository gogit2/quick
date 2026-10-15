import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, of, switchMap, tap } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { LearningApi } from '../../core/api/learning.api';
import { LearningCard } from '../../core/api/models';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { UiCardComponent } from '../../shared/ui-card.component';
import { UiChipComponent } from '../../shared/ui-chip.component';
import { UiPageHeaderComponent } from '../../shared/ui-page-header.component';
import { UiSkeletonComponent } from '../../shared/ui-skeleton.component';
import { LearningCardDetailsComponent } from './learning-card-details.component';
import { LearningCardDialogComponent, LearningCardDialogData } from './learning-card-dialog.component';

@Component({
  selector: 'qf-learning-page',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    EmptyStateComponent,
    LearningCardDetailsComponent,
    UiCardComponent,
    UiChipComponent,
    UiPageHeaderComponent,
    UiSkeletonComponent,
  ],
  template: `
    <ui-page-header title="Learning" titleTestId="learning-title">
      <button
        actions
        mat-raised-button
        color="primary"
        type="button"
        data-testid="learning-add-button"
        (click)="openCreate()"
      >
        Add learning resource
      </button>
    </ui-page-header>

    @if (loading()) {
      <div class="loading" data-testid="learning-loading">
        <ui-skeleton variant="card" [count]="3" />
      </div>
    }

    @if (loadFailed()) {
      <div class="error-state" role="alert" data-testid="learning-load-error">
        <span>Learning resources could not be loaded.</span>
        <button mat-stroked-button type="button" data-testid="learning-error-retry" (click)="load()">Retry</button>
      </div>
    } @else if (!loading() && cards().length === 0) {
      <qf-empty-state
        testId="learning"
        illustration="learning"
        icon="✎"
        title="No learning resources yet"
        message="Add a course, book or video to track milestones and notes."
        actionLabel="Add learning resource"
        (action)="openCreate()"
      />
    }

    <ul class="card-grid">
      @for (card of cards(); track card.id) {
        <li>
          <ui-card class="learning-card" [attr.data-testid]="'learning-card-' + card.id">
            <div class="card-head">
              <h2 class="card-title qf-title">{{ card.title }}</h2>
              @if (card.description) {
                <p class="card-description qf-body">{{ card.description }}</p>
              }
            </div>

            <div class="meta">
              <ui-chip kind="learning-status" [value]="card.status" [testId]="'learning-status-' + card.id" />
              <div class="progress">
                <mat-progress-bar
                  mode="determinate"
                  class="bar"
                  [value]="progressPercent(card)"
                  aria-hidden="true"
                />
                <span class="qf-caption" [attr.data-testid]="'learning-progress-' + card.id">
                  {{ progressLabel(card) }}
                </span>
              </div>
            </div>

            <div class="card-actions">
              <button
                mat-button
                type="button"
                [attr.data-testid]="'learning-expand-' + card.id"
                [attr.aria-expanded]="isExpanded(card.id)"
                (click)="toggleExpanded(card.id)"
              >
                <mat-icon [svgIcon]="isExpanded(card.id) ? 'expand_less' : 'expand_more'" aria-hidden="true" />
                {{ isExpanded(card.id) ? 'Hide details' : 'Show details' }}
              </button>
              <span class="spacer"></span>
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
            </div>

            <!-- Details render on first open and stay in the DOM, so collapsing animates. -->
            @if (rendered(card.id)) {
              <div class="collapse" [class.open]="isExpanded(card.id)">
                <div class="collapse-inner">
                  <qf-learning-card-details [card]="card" (changed)="load()" />
                </div>
              </div>
            }
          </ui-card>
        </li>
      }
    </ul>
  `,
  styles: `
    .loading {
      margin: var(--qf-space-4) 0;
    }

    .card-grid {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: var(--qf-space-4);

      > li {
        display: flex;
      }
    }

    .learning-card {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-3);
      flex: 1 1 auto;
      min-width: 0;
    }

    .card-head {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-1);
    }

    .card-title {
      margin: 0;
      color: var(--qf-text);
      overflow-wrap: anywhere;
    }

    .card-description {
      margin: 0;
      color: var(--qf-text-muted);
      overflow-wrap: anywhere;
    }

    .meta {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-2);
      align-items: flex-start;
    }

    .progress {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-1);
      width: 100%;
      color: var(--qf-text-muted);
    }

    .bar {
      --mdc-linear-progress-active-indicator-color: var(--qf-accent);
      --mdc-linear-progress-track-color: var(--qf-outline);
    }

    .card-actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--qf-space-1);
      margin-top: auto;
    }

    .spacer {
      flex: 1 1 auto;
    }

    // Expand and collapse animate the grid row height from 0fr to 1fr.
    .collapse {
      display: grid;
      grid-template-rows: 0fr;
      visibility: hidden;
      transition:
        grid-template-rows var(--qf-motion-base) var(--qf-ease),
        visibility 0s linear var(--qf-motion-base);

      &.open {
        grid-template-rows: 1fr;
        visibility: visible;
        transition:
          grid-template-rows var(--qf-motion-base) var(--qf-ease),
          visibility 0s;
      }
    }

    .collapse-inner {
      min-height: 0;
      overflow: hidden;
    }

    @media (prefers-reduced-motion: reduce) {
      .collapse,
      .collapse.open {
        transition: none;
      }
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
  // Ids of the cards whose details have been opened at least once; their details stay mounted.
  readonly renderedIds = signal<ReadonlySet<number>>(new Set());

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

  rendered(id: number): boolean {
    return this.renderedIds().has(id);
  }

  toggleExpanded(id: number): void {
    const next = new Set(this.expandedIds());
    if (next.has(id)) {
      next.delete(id);
    } else if (!this.rendered(id)) {
      // First open: render the details closed, then open them a frame later so the height animates.
      this.renderedIds.set(new Set(this.renderedIds()).add(id));
      requestAnimationFrame(() => requestAnimationFrame(() => this.expandedIds.set(new Set(this.expandedIds()).add(id))));
      return;
    } else {
      next.add(id);
    }
    this.expandedIds.set(next);
  }

  progressPercent(card: LearningCard): number {
    if (card.milestones.length === 0) {
      return 0;
    }
    return (card.milestones.filter((milestone) => milestone.done).length / card.milestones.length) * 100;
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
