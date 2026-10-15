import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';

import { NotificationService } from '../core/notification.service';

interface NavItem {
  label: string;
  path: string;
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
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  // Created with the shell, so the start notice runs from app start.
  readonly notifications = inject(NotificationService);

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', testId: 'nav-dashboard' },
    { label: 'Tasks', path: '/tasks', testId: 'nav-tasks' },
    { label: 'Habits', path: '/habits', testId: 'nav-habits' },
    { label: 'Learning', path: '/learning', testId: 'nav-learning' },
    { label: 'Plans', path: '/plans', testId: 'nav-plans' },
    { label: 'Settings', path: '/settings', testId: 'nav-settings' },
  ];
}
