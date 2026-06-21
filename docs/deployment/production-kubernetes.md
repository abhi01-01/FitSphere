# Production Deployment Target

FitSphere now has a concrete production target:

- Kubernetes for application workloads
- GHCR for container images
- managed PostgreSQL for `userservice`
- managed MongoDB for `activityservice` and `aiservice`
- managed RabbitMQ for asynchronous activity processing
- managed Keycloak or an equivalent externally hosted identity deployment

## Included in the repository

- Kubernetes base manifests and a production overlay in [deploy/k8s](/Users/abhising/DevTanishq/IntellijAll/FitSphere/deploy/k8s)
- per-service Dockerfiles
- a GitHub Actions image release workflow
- a GitHub Actions production deployment workflow

## External prerequisites

You still need to provide:

- a Kubernetes cluster with an ingress controller
- TLS certificates for `app` and `api` hosts
- a reachable PostgreSQL endpoint
- reachable MongoDB endpoints
- a reachable RabbitMQ endpoint
- a reachable Keycloak realm and JWK endpoint
- a `GEMINI_API_KEY`

## Required GitHub secrets

The deploy workflow expects these repository or environment secrets:

- `KUBE_CONFIG`
- `USER_SERVICE_DATASOURCE_URL`
- `USER_SERVICE_DATASOURCE_USERNAME`
- `USER_SERVICE_DATASOURCE_PASSWORD`
- `ACTIVITY_MONGODB_URI`
- `AI_MONGODB_URI`
- `RABBITMQ_HOST`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `GEMINI_API_KEY`
- `KEYCLOAK_JWK_SET_URI`
- `INTERNAL_SERVICE_TOKEN`

## Required GitHub variables for frontend production builds

The frontend image release workflow can use:

- `FRONTEND_API_BASE_URL`
- `FRONTEND_KEYCLOAK_BASE_URL`
- `FRONTEND_KEYCLOAK_REALM`
- `FRONTEND_KEYCLOAK_CLIENT_ID`
- `FRONTEND_APP_URL`

## Operational behavior now encoded in the app

- liveness and readiness probes are exposed through Spring Boot actuator endpoints
- Prometheus scrape endpoints are exposed through `/actuator/prometheus`
- gateway and activity service user-service calls now use explicit timeouts and bounded retries
- Gemini calls now use explicit timeouts and bounded retries
- RabbitMQ queues now include dead-letter routing
- AI consumption is idempotent at the recommendation level through duplicate detection and a unique activity recommendation key
- gateway-to-service and service-to-service trust requires a shared internal token
- the Kubernetes ingress only routes `/api` to the gateway and forces TLS redirects
