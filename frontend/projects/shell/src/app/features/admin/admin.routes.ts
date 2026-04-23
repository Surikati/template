import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
  {
    path: 'users',
    loadComponent: () => import('./user-list.component').then((m) => m.UserListComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'users' },
];
