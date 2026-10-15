import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NavPrefsService } from '../core/nav-prefs.service';
import { SettingsStore } from '../core/settings.store';

// Avatar with the initials and the display name from Settings. Links to the Settings page.
@Component({
  selector: 'app-user-chip',
  standalone: true,
  imports: [RouterLink],
  template: `
    <a class="chip" routerLink="/settings" data-testid="user-chip" [attr.aria-label]="'Profile: ' + name()">
      <span class="avatar" aria-hidden="true">{{ initials() }}</span>
      @if (!prefs.phone()) {
        <span class="name">{{ name() }}</span>
      }
    </a>
  `,
  styles: `
    :host {
      display: inline-flex;
      min-width: 0;
      margin-inline-start: var(--qf-space-2);
    }

    .chip {
      display: flex;
      align-items: center;
      gap: var(--qf-space-2);
      min-width: 0;
      padding: var(--qf-space-1);
      padding-inline-end: var(--qf-space-3);
      border-radius: var(--qf-radius-pill);
      color: var(--qf-text);
      text-decoration: none;
    }

    .chip:hover {
      background: var(--qf-surface-raised);
    }

    .avatar {
      display: inline-grid;
      flex: none;
      place-items: center;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--qf-accent);
      color: var(--qf-on-accent);
      font: var(--qf-text-label);
    }

    .name {
      max-width: 160px;
      overflow: hidden;
      font: var(--qf-text-label);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  `,
})
export class UserChipComponent {
  readonly prefs = inject(NavPrefsService);
  private readonly store = inject(SettingsStore);

  readonly name = computed(() => this.store.settings()?.displayName?.trim() ?? '');

  // First letters of up to two words, uppercase.
  readonly initials = computed(() => {
    const words = this.name().split(/\s+/).filter(Boolean).slice(0, 2);
    return words.map((word) => word[0].toUpperCase()).join('') || '?';
  });
}
