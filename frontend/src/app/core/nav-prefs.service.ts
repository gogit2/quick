import { BreakpointObserver } from '@angular/cdk/layout';
import { Injectable, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

const COLLAPSED_KEY = 'qf.nav.collapsed';

// Navigation layout state: icon rail or full labels, and the drawer on narrow screens.
@Injectable({ providedIn: 'root' })
export class NavPrefsService {
  private readonly breakpoints = inject(BreakpointObserver);

  /** Below 960 px the navigation is a drawer. */
  readonly compact = this.matches('(max-width: 959.98px)');
  /** Below 600 px the user chip hides the name. */
  readonly phone = this.matches('(max-width: 599.98px)');

  /** Icon rail chosen by the user, kept in localStorage. Ignored while compact. */
  readonly collapsed = signal(this.readCollapsed());
  /** Icon rail as shown on screen. */
  readonly rail = computed(() => this.collapsed() && !this.compact());

  /** Drawer state on narrow screens. */
  readonly drawerOpen = signal(false);

  toggleCollapsed(): void {
    this.collapsed.update((value) => !value);
    try {
      localStorage.setItem(COLLAPSED_KEY, String(this.collapsed()));
    } catch {
      // Storage is unavailable (for example private mode): the choice lasts for this page only.
    }
  }

  openDrawer(): void {
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  private matches(query: string) {
    return toSignal(this.breakpoints.observe(query).pipe(map((state) => state.matches)), { initialValue: false });
  }

  private readCollapsed(): boolean {
    try {
      return localStorage.getItem(COLLAPSED_KEY) === 'true';
    } catch {
      return false;
    }
  }
}
