import { Routes } from '@angular/router';

export const templatesRoutes: Routes = [
  { path: '', loadComponent: () => import('./template-list.component').then((m) => m.TemplateListComponent) },
  {
    path: ':id/edit',
    loadComponent: () => import('./template-editor-page.component').then((m) => m.TemplateEditorPageComponent),
  },
  {
    path: ':id/diff',
    loadComponent: () =>
      import('./template-version-diff-page.component').then((m) => m.TemplateVersionDiffPageComponent),
  },
];
