import { AuthConfig } from 'angular-oauth2-oidc';
import { InjectionToken } from '@angular/core';

export interface KeycloakConfig {
  issuer: string;
  clientId: string;
  redirectUri: string;
  scope: string;
}

export const KEYCLOAK_CONFIG = new InjectionToken<KeycloakConfig>('KEYCLOAK_CONFIG');

export function buildAuthConfig(cfg: KeycloakConfig): AuthConfig {
  return {
    issuer: cfg.issuer,
    clientId: cfg.clientId,
    redirectUri: cfg.redirectUri,
    responseType: 'code',
    scope: cfg.scope,
    requireHttps: false,               // dev; enforce at prod ingress
    useSilentRefresh: true,
    silentRefreshTimeout: 20000,
    timeoutFactor: 0.75,
    sessionChecksEnabled: false,
    showDebugInformation: false,
  };
}
