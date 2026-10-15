import { Injectable, inject } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

// Local copies of Material Design icons (Apache-2.0) in src/assets/icons.
// The app never loads icons from a CDN, so it works offline and in tests.
export const ICON_NAMES = [
  'dashboard', 'task_alt', 'repeat', 'school', 'event_available', 'settings', 'menu',
  'chevron_left', 'chevron_right', 'add', 'edit', 'delete', 'archive', 'unarchive',
  'check', 'expand_more', 'expand_less', 'error', 'warning', 'schedule', 'light_mode',
  'dark_mode', 'brightness_auto', 'person', 'search', 'close',
] as const;

@Injectable({ providedIn: 'root' })
export class IconService {
  private readonly registry = inject(MatIconRegistry);
  private readonly sanitizer = inject(DomSanitizer);

  // Registers every bundled icon as <mat-icon svgIcon="<name>">.
  registerAll(): void {
    for (const name of ICON_NAMES) {
      const url = this.sanitizer.bypassSecurityTrustResourceUrl(`assets/icons/${name}.svg`);
      this.registry.addSvgIcon(name, url);
    }
  }
}
