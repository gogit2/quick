import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, of, Subject, switchMap, catchError, tap } from 'rxjs';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { Task, TaskDueFilter, TaskPriority, TaskSort, TaskStatus } from '../../core/api/models';
import { TaskListParams, TasksApi } from '../../core/api/tasks.api';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { TaskFormDialogComponent, TaskFormDialogData } from './task-form-dialog.component';

// Empty value of a select means "all".
type Filter<T extends string> = T | '';

interface TaskFilters {
  status: FormControl<Filter<TaskStatus>>;
  priority: FormControl<Filter<TaskPriority>>;
  due: FormControl<Filter<TaskDueFilter>>;
  sort: FormControl<TaskSort>;
  archived: FormControl<boolean>;
}

const DEFAULT_SORT: TaskSort = 'CREATED_DESC';

@Component({
  selector: 'qf-tasks-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSlideToggleModule,
    EmptyStateComponent,
  ],
  templateUrl: './tasks-page.component.html',
  styleUrl: './tasks-page.component.scss',
})
export class TasksPageComponent implements OnInit {
  private readonly api = inject(TasksApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly tasks = signal<Task[]>([]);
  readonly loading = signal(false);
  // Set when the last list request failed, so the page does not claim "no tasks".
  readonly loadFailed = signal(false);
  readonly filtersActive = signal(false);

  readonly statusOptions: { value: Filter<TaskStatus>; label: string }[] = [
    { value: '', label: 'All statuses' },
    { value: 'TODO', label: 'To do' },
    { value: 'IN_PROGRESS', label: 'In progress' },
    { value: 'DONE', label: 'Done' },
  ];

  readonly priorityOptions: { value: Filter<TaskPriority>; label: string }[] = [
    { value: '', label: 'All priorities' },
    { value: 'LOW', label: 'Low' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HIGH', label: 'High' },
  ];

  readonly dueOptions: { value: Filter<TaskDueFilter>; label: string }[] = [
    { value: '', label: 'Any due date' },
    { value: 'TODAY', label: 'Today' },
    { value: 'OVERDUE', label: 'Overdue' },
    { value: 'THIS_WEEK', label: 'This week' },
    { value: 'NO_DUE_DATE', label: 'No due date' },
  ];

  readonly sortOptions: { value: TaskSort; label: string }[] = [
    { value: 'CREATED_DESC', label: 'Newest' },
    { value: 'CREATED_ASC', label: 'Oldest' },
    { value: 'DUE_DATE_ASC', label: 'Due date (earliest first)' },
    { value: 'DUE_DATE_DESC', label: 'Due date (latest first)' },
  ];

  readonly search = new FormControl('', { nonNullable: true });

  readonly filters = new FormGroup<TaskFilters>({
    status: new FormControl<Filter<TaskStatus>>('', { nonNullable: true }),
    priority: new FormControl<Filter<TaskPriority>>('', { nonNullable: true }),
    due: new FormControl<Filter<TaskDueFilter>>('', { nonNullable: true }),
    sort: new FormControl<TaskSort>(DEFAULT_SORT, { nonNullable: true }),
    archived: new FormControl(false, { nonNullable: true }),
  });

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
          this.api.listTasks(this.params()).pipe(
            catchError(() => {
              this.loadFailed.set(true);
              return of<Task[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((tasks) => {
        this.tasks.set(tasks);
        this.loading.set(false);
      });

    // Typing in the search box waits 300 ms before it reloads.
    this.search.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());

    this.filters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.filtersActive.set(this.hasActiveFilters());
    this.reload.next();
  }

  clearFilters(): void {
    this.search.setValue('', { emitEvent: false });
    this.filters.reset({ status: '', priority: '', due: '', sort: DEFAULT_SORT, archived: false }, { emitEvent: false });
    this.load();
  }

  openCreate(): void {
    this.dialog
      .open<TaskFormDialogComponent, TaskFormDialogData, Task | undefined>(TaskFormDialogComponent, { data: {} })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  openEdit(task: Task): void {
    this.dialog
      .open<TaskFormDialogComponent, TaskFormDialogData, Task | undefined>(TaskFormDialogComponent, { data: { task } })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  complete(task: Task): void {
    this.api.completeTask(task.id).subscribe({
      next: () => this.load(),
      error: this.onActionError('The task could not be completed.'),
    });
  }

  toggleArchive(task: Task): void {
    const request = task.archived ? this.api.restoreTask(task.id) : this.api.archiveTask(task.id);
    request.subscribe({
      next: () => this.load(),
      error: this.onActionError(task.archived ? 'The task could not be restored.' : 'The task could not be archived.'),
    });
  }

  confirmDelete(task: Task): void {
    const data: ConfirmDialogData = {
      title: 'Delete task?',
      message: `"${task.title}" is deleted for good and removed from every plan.`,
      confirmLabel: 'Delete',
    };
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, { data })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.api.deleteTask(task.id).subscribe({
          next: () => this.load(),
          error: this.onActionError('The task could not be deleted.'),
        });
      });
  }

  statusLabel(status: TaskStatus): string {
    return this.statusOptions.find((option) => option.value === status)?.label ?? status;
  }

  priorityLabel(priority: TaskPriority): string {
    return this.priorityOptions.find((option) => option.value === priority)?.label ?? priority;
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

  private hasActiveFilters(): boolean {
    const f = this.filters.getRawValue();
    return (
      this.search.value.trim() !== '' || f.status !== '' || f.priority !== '' || f.due !== '' || f.archived
    );
  }

  private params(): TaskListParams {
    const f = this.filters.getRawValue();
    const q = this.search.value.trim();
    return {
      q: q === '' ? undefined : q,
      status: f.status === '' ? undefined : f.status,
      priority: f.priority === '' ? undefined : f.priority,
      due: f.due === '' ? undefined : f.due,
      sort: f.sort,
      archived: f.archived,
    };
  }
}
