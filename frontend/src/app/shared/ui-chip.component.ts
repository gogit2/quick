import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type ChipKind = 'task-status' | 'priority' | 'plan-status' | 'learning-status' | 'overdue';
type ChipTone = 'info' | 'warning' | 'danger' | 'success';

interface ChipView {
  tone: ChipTone;
  icon: string | null;
  label: string;
}

// One meaning, one colour, one icon, always with text (data-model.md, chips table).
const STATUS_TONE: Record<string, ChipView> = {
  NOT_STARTED: { tone: 'info', icon: 'schedule', label: 'Not started' },
  IN_PROGRESS: { tone: 'warning', icon: 'schedule', label: 'In progress' },
  COMPLETED: { tone: 'success', icon: 'check', label: 'Completed' },
};

const TASK_STATUS: Record<string, ChipView> = {
  TODO: { tone: 'info', icon: 'schedule', label: 'Todo' },
  IN_PROGRESS: { tone: 'warning', icon: 'schedule', label: 'In progress' },
  DONE: { tone: 'success', icon: 'check', label: 'Done' },
};

const PRIORITY: Record<string, ChipView> = {
  LOW: { tone: 'info', icon: null, label: 'Low' },
  MEDIUM: { tone: 'warning', icon: null, label: 'Medium' },
  HIGH: { tone: 'danger', icon: null, label: 'High' },
};

const OVERDUE: ChipView = { tone: 'danger', icon: 'warning', label: 'Overdue' };

export function describeChip(kind: ChipKind, value: string): ChipView {
  switch (kind) {
    case 'task-status':
      return TASK_STATUS[value] ?? { tone: 'info', icon: null, label: value };
    case 'priority':
      return PRIORITY[value] ?? { tone: 'info', icon: null, label: value };
    case 'plan-status':
    case 'learning-status':
      return STATUS_TONE[value] ?? { tone: 'info', icon: null, label: value };
    case 'overdue':
      return OVERDUE;
  }
}

// Status, priority and overdue badge. `testId` is set as data-testid on the chip element.
@Component({
  selector: 'ui-chip',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <span class="chip qf-label" [class]="'chip-' + view().tone">
      @if (view().icon) {
        <mat-icon class="icon" [svgIcon]="view().icon!" aria-hidden="true" />
      }
      <span class="text">{{ view().label }}</span>
    </span>
  `,
  host: {
    '[attr.data-testid]': 'testId() || null',
  },
  styles: `
    :host {
      display: inline-flex;
      vertical-align: middle;
    }

    .chip {
      display: inline-flex;
      align-items: center;
      gap: var(--qf-space-1);
      padding: var(--qf-space-1) var(--qf-space-3);
      border-radius: var(--qf-radius-pill);
      white-space: nowrap;
    }

    .icon {
      width: 16px;
      height: 16px;
      font-size: 16px;
    }

    .chip-info {
      background: var(--qf-info-bg);
      color: var(--qf-info);
    }

    .chip-warning {
      background: var(--qf-warning-bg);
      color: var(--qf-warning);
    }

    .chip-danger {
      background: var(--qf-danger-bg);
      color: var(--qf-danger);
    }

    .chip-success {
      background: var(--qf-success-bg);
      color: var(--qf-success);
    }
  `,
})
export class UiChipComponent {
  readonly kind = input.required<ChipKind>();
  readonly value = input('');
  readonly testId = input('');
  readonly view = computed(() => describeChip(this.kind(), this.value()));
}
