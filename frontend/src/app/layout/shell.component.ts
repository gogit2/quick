import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';

interface NavItem {
  label: string;
  path: string;
  testId: string;
}

// Persistent layout: toolbar, sidenav with the six pages, and the routed page.
@Component({
  selector: 'qf-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatListModule, MatSidenavModule, MatToolbarModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  readonly navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', testId: 'nav-dashboard' },
    { label: 'Tasks', path: '/tasks', testId: 'nav-tasks' },
    { label: 'Habits', path: '/habits', testId: 'nav-habits' },
    { label: 'Learning', path: '/learning', testId: 'nav-learning' },
    { label: 'Plans', path: '/plans', testId: 'nav-plans' },
    { label: 'Settings', path: '/settings', testId: 'nav-settings' },
  ];
}
