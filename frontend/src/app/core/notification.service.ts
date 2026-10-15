import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { forkJoin } from 'rxjs';

import { Plan } from './api/models';
import { PlansApi } from './api/plans.api';
import { ClockService } from './clock.service';
import { SettingsStore } from './settings.store';

// The plan list is reloaded from the server at most this often while the app is open.
const SYNC_INTERVAL_MS = 30_000;

export interface StartNotice {
  planId: number;
  title: string;
}

// D-8: in-app start notice only. When a plan has started (server status is not NOT_STARTED and the
// app clock is at or after its start), one banner is shown per plan per session and the plan card is
// highlighted. The server is then told with acknowledge-start, so a plan that started while the app was
// closed is announced once on the next start. With Settings notifications off, the plan is only acknowledged.
// Runs on app start (the first tick syncs the list) and on every ClockService tick.
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly plansApi = inject(PlansApi);
  private readonly clock = inject(ClockService);
  private readonly settings = inject(SettingsStore);

  private plans: Plan[] = [];
  private lastSyncMs = 0;
  private syncing = false;
  // Plans already handled this session (announced or acknowledged, or the acknowledge is in flight).
  private readonly handled = new Set<number>();
  // Notices waiting on screen; the first one is the banner, and its card stays highlighted until dismissed.
  private readonly queue = signal<StartNotice[]>([]);

  readonly banner = computed(() => this.queue()[0] ?? null);
  readonly highlightedIds = computed(() => new Set(this.queue().map((notice) => notice.planId)));

  constructor() {
    effect(() => {
      const now = this.clock.now();
      untracked(() => this.check(now));
    });
  }

  dismiss(): void {
    this.queue.update((notices) => notices.slice(1));
  }

  private check(now: Date): void {
    if (this.syncing) {
      return;
    }
    if (Date.now() - this.lastSyncMs >= SYNC_INTERVAL_MS) {
      this.sync();
      return;
    }
    this.announceStarted(now);
  }

  private sync(): void {
    this.syncing = true;
    this.lastSyncMs = Date.now();
    forkJoin([this.plansApi.listPlans('ACTIVE_UPCOMING'), this.plansApi.listPlans('COMPLETED')]).subscribe({
      next: ([active, completed]) => {
        this.plans = [...active, ...completed];
        this.syncing = false;
        this.announceStarted(this.clock.now());
      },
      // 5xx errors are reported by the HTTP interceptor; the next try is after SYNC_INTERVAL_MS.
      error: () => {
        this.syncing = false;
      },
    });
  }

  private announceStarted(now: Date): void {
    const time = now.getTime();
    for (const plan of this.plans) {
      const started = plan.status !== 'NOT_STARTED' && time >= Date.parse(plan.startDateTime);
      if (!started || plan.startNotified || this.handled.has(plan.id)) {
        continue;
      }
      this.handled.add(plan.id);
      if (this.settings.settings()?.notificationsEnabled !== false) {
        this.queue.update((notices) => [...notices, { planId: plan.id, title: plan.title }]);
      }
      // A failed acknowledge is not retried in this session, so the banner is not shown twice.
      // 5xx errors are reported by the HTTP interceptor.
      this.plansApi.acknowledgeStart(plan.id).subscribe({ error: () => undefined });
    }
  }
}
