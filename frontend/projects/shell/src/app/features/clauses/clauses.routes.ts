import { Routes } from '@angular/router';

export const clausesRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./clause-list.component').then((m) => m.ClauseListComponent),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./clause-editor-page.component').then((m) => m.ClauseEditorPageComponent),
  },
];
