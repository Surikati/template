import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import {
  AuthConfig,
  OAuthService,
  provideOAuthClient,
} from 'angular-oauth2-oidc';

import {
  APP_CONFIG,
  AuthService,
  KEYCLOAK_CONFIG,
  authInterceptor,
  buildAuthConfig,
  correlationIdInterceptor,
  errorInterceptor,
} from '@tmpmgmt/core';

import { environment } from '../environments/environment';
import { appRoutes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(appRoutes, withComponentInputBinding()),
    provideHttpClient(
      withInterceptors([correlationIdInterceptor, authInterceptor, errorInterceptor]),
    ),
    provideAnimations(),
    provideOAuthClient(),

    { provide: APP_CONFIG, useValue: { apiBase: environment.apiBase } },
    { provide: KEYCLOAK_CONFIG, useValue: environment.keycloak },

    MessageService,

    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [OAuthService, AuthService],
      useFactory: (oauth: OAuthService, auth: AuthService) => () => {
        const cfg: AuthConfig = buildAuthConfig(environment.keycloak);
        oauth.configure(cfg);
        return auth.init();
      },
    },
  ],
};
