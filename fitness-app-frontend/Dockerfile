FROM node:22-alpine AS build
WORKDIR /app

ARG VITE_API_BASE_URL=http://localhost:8080/api
ARG VITE_KEYCLOAK_BASE_URL=http://localhost:8181
ARG VITE_KEYCLOAK_REALM=fitSphere-auth
ARG VITE_KEYCLOAK_CLIENT_ID=oauth2-pkce-client
ARG VITE_APP_URL=http://localhost:3000

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_KEYCLOAK_BASE_URL=$VITE_KEYCLOAK_BASE_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID
ENV VITE_APP_URL=$VITE_APP_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
