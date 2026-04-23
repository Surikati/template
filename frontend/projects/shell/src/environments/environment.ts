export const environment = {
  production: false,
  apiBase: 'http://localhost:8080/api/v1',
  keycloak: {
    issuer: 'http://localhost:8180/realms/tmpmgmt',
    clientId: 'tmpmgmt-frontend',
    redirectUri: window.location.origin,
    scope: 'openid profile email',
  },
};
