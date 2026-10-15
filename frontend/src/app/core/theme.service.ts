import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';
export type EffectiveTheme = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'qf.theme';
const MODES: readonly ThemeMode[] = ['light', 'dark', 'system'];

function isThemeMode(value: string | null): value is ThemeMode {
  return value !== null && (MODES as readonly string[]).includes(value);
}

// Test hook: ?reduceMotion=1 turns off motion for the page, like ?clockOffsetMs for time.
export function reduceMotionRequested(): boolean {
  return new URLSearchParams(window.location.search).get('reduceMotion') === '1';
}

// Light, dark or system theme. The choice is kept in localStorage and mirrored to
// <html data-theme>; the inline script in index.html sets the same attribute before paint.
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);
  private readonly systemQuery = this.doc.defaultView?.matchMedia('(prefers-color-scheme: dark)') ?? null;
  private readonly systemDark = signal(this.systemQuery?.matches ?? false);

  readonly mode = signal<ThemeMode>(this.readStoredMode());
  readonly effective = computed<EffectiveTheme>(() => {
    const mode = this.mode();
    if (mode === 'system') {
      return this.systemDark() ? 'dark' : 'light';
    }
    return mode;
  });

  constructor() {
    this.systemQuery?.addEventListener('change', (event) => this.systemDark.set(event.matches));
    this.apply(this.mode());
    if (reduceMotionRequested()) {
      this.doc.documentElement.dataset['reduceMotion'] = 'true';
    }
  }

  setMode(mode: ThemeMode): void {
    if (!isThemeMode(mode)) {
      return;
    }
    this.mode.set(mode);
    this.apply(mode);
    this.store(mode);
  }

  private readStoredMode(): ThemeMode {
    try {
      const stored = localStorage.getItem(THEME_STORAGE_KEY);
      return isThemeMode(stored) ? stored : 'system';
    } catch {
      // Storage can be blocked; fall back to the system theme.
      return 'system';
    }
  }

  private store(mode: ThemeMode): void {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, mode);
    } catch {
      // Storage can be blocked; the choice then lasts for this page only.
    }
  }

  private apply(mode: ThemeMode): void {
    this.doc.documentElement.dataset['theme'] = mode;
  }
}
