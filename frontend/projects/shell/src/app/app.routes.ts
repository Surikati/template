import { Routes } from '@angular/router';
import { authGuard } from '@tmpmgmt/core';
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
      { path: '', pathMatch: 'full', redirectTo: 'templates' },
    ],
  },
];
