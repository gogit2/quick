import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, tap } from 'rxjs';

import { SettingsApi } from './api/settings.api';
import { Settings, SettingsUpdate, StartPage } from './api/models';

// D-10: the app opens on the start page chosen in Settings.
export const START_PAGE_PATHS: Record<StartPage, string> = {
  DASHBOARD: '/dashboard',
  TASKS: '/tasks',
  HABITS: '/habits',
  LEARNING: '/learning',
  PLANS: '/plans',
};

@Injectable({ providedIn: 'root' })
export class SettingsStore {
  private readonly api = inject(SettingsApi);

  readonly settings = signal<Settings | null>(null);

  /** Route of the start page; Dashboard until the settings are loaded. */
  readonly startPagePath = computed(() => START_PAGE_PATHS[this.settings()?.defaultStartPage ?? 'DASHBOARD']);

  /** Runs before routing (APP_INITIALIZER). A failed load keeps the defaults so the app still opens. */
  async initialize(): Promise<void> {
    try {
      const current = await firstValueFrom(this.refresh());
      if (!current.configured) {
        // First run: store the browser time zone silently, keeping the other values.
        const browserZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
        await firstValueFrom(
          this.save({
            displayName: current.displayName,
            email: current.email ?? null,
            timeZone: browserZone,
            notificationsEnabled: current.notificationsEnabled,
            defaultStartPage: current.defaultStartPage,
          }),
        );
      }
    } catch {
      // Keep the defaults; the error interceptor has already reported 5xx responses.
    }
  }

  refresh(): Observable<Settings> {
    return this.api.getSettings().pipe(tap((settings) => this.settings.set(settings)));
  }

  save(body: SettingsUpdate): Observable<Settings> {
    return this.api.updateSettings(body).pipe(tap((settings) => this.settings.set(settings)));
  }
}
