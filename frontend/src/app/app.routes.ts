import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { SettingsStore } from './core/settings.store';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  // "/" opens the start page chosen in Settings (D-10).
  { path: '', pathMatch: 'full', redirectTo: () => inject(SettingsStore).startPagePath() },
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard-page.component').then((m) => m.DashboardPageComponent),
      },
      {
        path: 'tasks',
        loadComponent: () => import('./features/tasks/tasks-page.component').then((m) => m.TasksPageComponent),
      },
      {
        path: 'habits',
        loadComponent: () => import('./features/habits/habits-page.component').then((m) => m.HabitsPageComponent),
      },
      {
        path: 'learning',
        loadComponent: () => import('./features/learning/learning-page.component').then((m) => m.LearningPageComponent),
      },
      {
        path: 'plans',
        loadComponent: () => import('./features/plans/plans-page.component').then((m) => m.PlansPageComponent),
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings-page.component').then((m) => m.SettingsPageComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
