// Production config — typically overridden by fileReplacements in angular.json,
// or replaced by a runtime config fetched from /assets/config.json.
export const environment = {
  production: true,
  apiBase: '/api/v1',
  keycloak: {
    issuer: '__KEYCLOAK_ISSUER__',
    clientId: 'tmpmgmt-frontend',
    redirectUri: window.location.origin,
    scope: 'openid profile email',
  },
};
