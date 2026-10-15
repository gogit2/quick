import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ThemeMode, ThemeService } from '../core/theme.service';

interface ThemeOption {
  value: ThemeMode;
  label: string;
  icon: string;
  testId: string;
}

// Three buttons in the toolbar; the active one carries aria-pressed="true".
@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="theme-toggle" role="group" aria-label="Theme" data-testid="theme-toggle">
      @for (option of options; track option.value) {
        <button
          mat-icon-button
          type="button"
          class="option"
          [class.active]="theme.mode() === option.value"
          [attr.aria-pressed]="theme.mode() === option.value"
          [attr.aria-label]="option.label"
          [attr.data-testid]="option.testId"
          (click)="theme.setMode(option.value)"
        >
          <mat-icon [svgIcon]="option.icon" />
        </button>
      }
    </div>
  `,
  styles: `
    :host {
      margin-left: auto;
    }

    .theme-toggle {
      display: flex;
      gap: var(--qf-space-1);
    }

    .option.active {
      background: var(--qf-on-accent);
      color: var(--qf-accent);
    }
  `,
})
export class ThemeToggleComponent {
  readonly theme = inject(ThemeService);

  readonly options: ThemeOption[] = [
    { value: 'light', label: 'Light theme', icon: 'light_mode', testId: 'theme-option-light' },
    { value: 'dark', label: 'Dark theme', icon: 'dark_mode', testId: 'theme-option-dark' },
    { value: 'system', label: 'System theme', icon: 'brightness_auto', testId: 'theme-option-system' },
  ];
}
