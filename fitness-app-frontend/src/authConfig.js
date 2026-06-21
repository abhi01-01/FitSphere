const keycloakBaseUrl = import.meta.env.VITE_KEYCLOAK_BASE_URL || 'http://localhost:8181';
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM || 'fitSphere-auth';
const appUrl = import.meta.env.VITE_APP_URL || window.location.origin;
const realmBaseUrl = `${keycloakBaseUrl}/realms/${keycloakRealm}/protocol/openid-connect`;

export const authConfig = {
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'oauth2-pkce-client',
  authorizationEndpoint: `${realmBaseUrl}/auth`,
  tokenEndpoint: `${realmBaseUrl}/token`,
  logoutEndpoint: `${realmBaseUrl}/logout`,
  redirectUri: appUrl,
  logoutRedirect: appUrl,
  scope: 'openid profile email offline_access',
  loginMethod: 'redirect',
  autoLogin: false,
  extraAuthParameters: {
    prompt: 'login',
  },
  onRefreshTokenExpire: (event) => event.logIn(),
}
