// ClockService: the only source of "now" for time-dependent UI (constitution V).
//
// Manual check (no test runner needed):
//   1. Open http://localhost:4300/settings?clockOffsetMs=3600000 -> now() is one hour ahead of real time.
//   2. In the browser console run: window.__qfClock.setNow('2026-10-15T09:00:00Z') -> now() reads that
//      instant and keeps advancing from it. window.__qfClock.clear() returns to the bootstrap offset.
//   3. Each second the `now` signal re-evaluates, so UI bound to it refreshes.
import { Injectable, Signal, computed, signal } from '@angular/core';

declare global {
  interface Window {
    __qfClock?: { setNow(iso: string): void; clear(): void };
  }
}

// Read once, when this module is loaded at bootstrap.
function readBaseOffset(): number {
  const raw = new URLSearchParams(window.location.search).get('clockOffsetMs');
  const value = raw === null ? NaN : Number(raw);
  return Number.isFinite(value) ? value : 0;
}

const BASE_OFFSET_MS = readBaseOffset();

@Injectable({ providedIn: 'root' })
export class ClockService {
  private readonly tick = signal(Date.now());

  /** Offset added to the real clock, in milliseconds. */
  readonly offsetMs = signal(BASE_OFFSET_MS);

  /** Current app time. Read it as `clock.now()`; it updates every second. */
  readonly now: Signal<Date> = computed(() => {
    void this.tick();
    return new Date(Date.now() + this.offsetMs());
  });

  constructor() {
    setInterval(() => this.tick.set(Date.now()), 1000);
    window.__qfClock = {
      setNow: (iso: string) => this.setNow(iso),
      clear: () => this.clear(),
    };
  }

  /** Makes the app clock read `iso` now, then keep running from there. */
  setNow(iso: string): void {
    const target = Date.parse(iso);
    if (Number.isNaN(target)) {
      throw new Error(`Invalid ISO date-time: ${iso}`);
    }
    this.offsetMs.set(target - Date.now());
  }

  /** Returns to the offset given in the URL at bootstrap (zero when there is none). */
  clear(): void {
    this.offsetMs.set(BASE_OFFSET_MS);
  }
}
