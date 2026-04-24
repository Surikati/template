import { Routes } from '@angular/router';
import { hasRole } from '@tmpmgmt/core';

export const questionnairesRoutes: Routes = [
  {
    path: 'new',
    canActivate: [hasRole('TEMPLATE_EDITOR', 'ADMIN')],
    loadComponent: () =>
      import('./questionnaire-editor-page.component').then(
        (m) => m.QuestionnaireEditorPageComponent,
      ),
  },
  {
    path: ':id/edit',
    canActivate: [hasRole('TEMPLATE_EDITOR', 'ADMIN')],
    loadComponent: () =>
      import('./questionnaire-editor-page.component').then(
        (m) => m.QuestionnaireEditorPageComponent,
      ),
  },
  {
    path: ':id/run',
    loadComponent: () =>
      import('./questionnaire-runner-page.component').then(
        (m) => m.QuestionnaireRunnerPageComponent,
      ),
  },
];
