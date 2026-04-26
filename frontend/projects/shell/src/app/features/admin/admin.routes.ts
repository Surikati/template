import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./admin-shell.component').then((m) => m.AdminShellComponent),
    children: [
      {
        path: 'users',
        loadComponent: () => import('./user-list.component').then((m) => m.UserListComponent),
      },
      {
        path: 'audit',
        loadComponent: () => import('./audit-viewer.component').then((m) => m.AuditViewerComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./app-settings.component').then((m) => m.AppSettingsComponent),
      },
      { path: '', pathMatch: 'full', redirectTo: 'users' },
    ],
  },
];
