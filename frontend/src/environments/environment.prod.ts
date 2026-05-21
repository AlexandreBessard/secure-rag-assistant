export const environment = {
  production: true,
  apiBaseUrl: '',  // Empty = relative URLs; a reverse proxy routes /ask, /upload etc. to the backend
  keycloak: {
    url: '',  // Set via KEYCLOAK_URL at build time or injected by the deployment pipeline
    realm: 'rag-assistant',
    clientId: 'rag-frontend',
  },
};
