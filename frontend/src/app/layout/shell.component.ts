import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { NavPrefsService } from '../core/nav-prefs.service';
import { NotificationService } from '../core/notification.service';
import { ThemeToggleComponent } from './theme-toggle.component';
import { UserChipComponent } from './user-chip.component';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  testId: string;
}

// Persistent layout: toolbar, sidenav with the six pages, and the routed page.
@Component({
  selector: 'qf-shell',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
    MatTooltipModule,
    ThemeToggleComponent,
    UserChipComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  // Created with the shell, so the start notice runs from app start.
  readonly notifications = inject(NotificationService);
  readonly prefs = inject(NavPrefsService);

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard', testId: 'nav-dashboard' },
    { label: 'Tasks', path: '/tasks', icon: 'task_alt', testId: 'nav-tasks' },
    { label: 'Habits', path: '/habits', icon: 'repeat', testId: 'nav-habits' },
    { label: 'Learning', path: '/learning', icon: 'school', testId: 'nav-learning' },
    { label: 'Plans', path: '/plans', icon: 'event_available', testId: 'nav-plans' },
    { label: 'Settings', path: '/settings', icon: 'settings', testId: 'nav-settings' },
  ];
}
