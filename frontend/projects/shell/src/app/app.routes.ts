import { Routes } from '@angular/router';
import { authGuard, hasRole } from '@tmpmgmt/core';
import { AppShellComponent } from './layout/app-shell.component';

export const appRoutes: Routes = [
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'templates',
        loadChildren: () =>
          import('./features/templates/templates.routes').then((m) => m.templatesRoutes),
      },
      {
        path: 'clauses',
        loadChildren: () =>
          import('./features/clauses/clauses.routes').then((m) => m.clausesRoutes),
      },
      {
        path: 'admin',
        canActivate: [hasRole('ADMIN')],
        loadChildren: () =>
          import('./features/admin/admin.routes').then((m) => m.adminRoutes),
      },
      {
        path: 'questionnaires',
        loadChildren: () =>
          import('./features/questionnaires/questionnaires.routes').then(
            (m) => m.questionnairesRoutes,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'templates' },
    ],
  },
];
