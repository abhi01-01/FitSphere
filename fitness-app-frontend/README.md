# FitSphere Frontend

Vite React client for FitSphere.

## Scripts

```bash
npm install
npm run dev
npm run build
npm run lint
```

## Environment

Copy `.env.example` to `.env.local` for local overrides.

| Variable | Default |
| --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` |
| `VITE_KEYCLOAK_BASE_URL` | `http://localhost:8181` |
| `VITE_KEYCLOAK_REALM` | `fitSphere-auth` |
| `VITE_KEYCLOAK_CLIENT_ID` | `oauth2-pkce-client` |
| `VITE_APP_URL` | `http://localhost:5173` |
