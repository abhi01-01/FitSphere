# FitSphere

FitSphere is a microservice-based fitness activity tracking platform with AI-generated workout recommendations. It combines a React frontend, a Spring Cloud API gateway, independently deployable Spring Boot services, asynchronous event processing with RabbitMQ, and separate datastores for user and recommendation domains.

The project is designed around one core workflow: a signed-in user logs an activity, that activity is validated and persisted immediately, and recommendation generation happens asynchronously through the AI service.

## Table of Contents

1. [What is the project?](#what-is-the-project)
2. [What does it do?](#what-does-it-do)
3. [Project Structure](#project-structure)
4. [Architecture Docs](#architecture-docs)
5. [Endpoints and Services](#endpoints-and-services)
6. [Error Handling](#error-handling)
7. [CI Pipelines](#ci-pipelines)
8. [Local Development Setup](#local-development-setup)
9. [Deployment Docs](#deployment-docs)
10. [Security Docs](#security-docs)

## What is the project?

FitSphere is a distributed application for recording workouts and attaching AI-generated coaching guidance to them.

It uses:

- `fitness-app-frontend` for the browser experience
- `gateway` as the authenticated entry point for all backend traffic
- `userservice` for user persistence and validation
- `activityservice` for workout tracking and event publishing
- `aiservice` for recommendation generation and retrieval
- `configserver` for centralized runtime configuration
- `eureka` for service discovery
- `Keycloak`, `PostgreSQL`, `MongoDB`, `RabbitMQ`, and `Gemini` as supporting infrastructure

## What does it do?

At a product level, FitSphere does four things:

1. Authenticates users through Keycloak using OAuth2 PKCE.
2. Lets authenticated users log fitness activities such as running, walking, and cycling.
3. Persists activities immediately and publishes them for asynchronous AI processing.
4. Generates and serves training recommendations, suggestions, and safety guidance per activity.

The user-visible behavior is:

- sign in from the browser
- create an activity record
- browse previously logged activities
- open a recommendation detail page for a specific activity
- see `404` for recommendations that have not been generated yet

The system behavior is:

- the gateway validates the JWT and syncs the Keycloak subject into `userservice`
- `activityservice` validates the user, stores the activity, and emits an event
- `aiservice` consumes the event, calls Gemini, and stores the recommendation
- the frontend later fetches the stored recommendation through the gateway

## Project Structure

### Top-level layout

| Path | Purpose |
| --- | --- |
| `fitness-app-frontend/` | React 19 + Vite 6 client |
| `gateway/` | Spring Cloud Gateway with OAuth2 JWT validation and user sync |
| `userservice/` | User registration, user profile lookup, Keycloak subject validation |
| `activityservice/` | Activity tracking, MongoDB persistence, RabbitMQ publishing |
| `aiservice/` | Recommendation generation, Gemini integration, recommendation retrieval |
| `configserver/` | Spring Cloud Config Server with native classpath-backed config |
| `eureka/` | Eureka service registry |
| `infra/keycloak/` | Development realm import for Keycloak |
| `docker-compose.yml` | Full local stack orchestration |
| `Makefile` | Build, test, and compose helper commands |
| `docs/architecture/` | Mermaid-first system architecture and whole-system ER docs |
| `docs/diagrams/` | Legacy rendered image assets kept for reference |

### Backend service responsibilities

| Service | Port | Responsibility | Storage / Dependency |
| --- | ---: | --- | --- |
| `eureka` | `8761` | Service discovery for runtime services | none |
| `configserver` | `8888` | Centralized runtime config | classpath config files |
| `gateway` | `8080` | JWT validation, CORS, routing, Keycloak user sync | Keycloak JWK endpoint |
| `userservice` | `8081` | User persistence and Keycloak subject validation | PostgreSQL |
| `activityservice` | `8082` | Activity persistence and event publishing | MongoDB, RabbitMQ |
| `aiservice` | `8083` | Recommendation generation and retrieval | MongoDB, RabbitMQ, Gemini API |
| `fitness-app-frontend` | `5173` dev / `3000` Docker | Browser UI | Keycloak, gateway |

### Configuration layout

Runtime configuration is served from `configserver/src/main/resources/config`.

| File | Used by | Notes |
| --- | --- | --- |
| `api-gateway.yml` | gateway | routes, JWT JWK URI, CORS, Eureka |
| `user-service.yml` | userservice | JDBC, JPA, Eureka |
| `activity-service.yml` | activityservice | MongoDB, RabbitMQ, Eureka |
| `ai-service.yml` | aiservice | MongoDB, RabbitMQ, Gemini, Eureka |

## Architecture Docs

The architecture source of truth is now Mermaid-based documentation instead of embedded PNG or SVG diagrams in the root README.

### Root system docs

- [System overview, detailed request/event path, and whole-system ER](docs/architecture/system-overview.md)

### Service architecture docs

- [Gateway architecture](gateway/ARCHITECTURE.md)
- [User service architecture](userservice/ARCHITECTURE.md)
- [Activity service architecture](activityservice/ARCHITECTURE.md)
- [AI service architecture](aiservice/ARCHITECTURE.md)
- [Config server architecture](configserver/ARCHITECTURE.md)
- [Eureka architecture](eureka/ARCHITECTURE.md)

### Legacy rendered assets

These files are still in the repository, but the Mermaid docs above are the maintained source of truth:

- `docs/diagrams/architecture-overview.png`
- `docs/diagrams/activity-recommendation-flow.png`
- `docs/diagrams/auth-and-user-sync-flow.png`
- `docs/diagrams/deployment-topology.svg`
- `docs/diagrams/ci-pipelines.svg`
- `docs/diagrams/gateway-route-map.svg`

## Endpoints and Services

All browser traffic should go through the gateway at `http://localhost:8080`.

### Public runtime entrypoints

| URL | Purpose |
| --- | --- |
| `http://localhost:3000` | Frontend in Docker |
| `http://localhost:5173` | Frontend in local Vite dev mode |
| `http://localhost:8080` | API gateway |
| `http://localhost:8761` | Eureka dashboard |
| `http://localhost:8888` | Config Server |
| `http://localhost:15672` | RabbitMQ management UI |
| `http://localhost:8181` | Keycloak |

### Gateway API surface

| Method | Path | Downstream service | Description |
| --- | --- | --- | --- |
| `POST` | `/api/users/register` | `userservice` | Register or sync a user record |
| `GET` | `/api/users/{userId}` | `userservice` | Get a user profile by internal application id |
| `GET` | `/api/users/{userId}/validate` | `userservice` | Validate that a Keycloak subject exists in the application |
| `POST` | `/api/activities` | `activityservice` | Track an activity for the authenticated user |
| `GET` | `/api/activities` | `activityservice` | List activities for the authenticated user |
| `GET` | `/api/activities/{activityId}` | `activityservice` | Fetch one activity |
| `GET` | `/api/recommendations/user/{userId}` | `aiservice` | List all recommendations for a user |
| `GET` | `/api/recommendations/activity/{activityId}` | `aiservice` | Fetch the recommendation for one activity |

### Service-by-service notes

#### `gateway`

- validates every request as an OAuth2 resource server
- fetches JWK metadata from Keycloak
- derives the effective user identity from the JWT subject
- syncs that subject through `userservice` before forwarding business requests
- forwards `X-User-ID` downstream after validation

#### `userservice`

- stores users in PostgreSQL
- validates existence by `keycloakId`
- updates an existing user when the same email appears with a new Keycloak subject

#### `activityservice`

- validates the user through `userservice`
- persists workout data in MongoDB
- publishes activity events to RabbitMQ for AI processing

#### `aiservice`

- consumes activity events from RabbitMQ
- calls Gemini to generate recommendation content
- stores recommendations in MongoDB
- returns `404` when a recommendation does not exist yet

## Error Handling

The repository now uses a more explicit error contract across the main application services.

### JSON error shape

`userservice`, `activityservice`, `aiservice`, and the `gateway` return structured JSON errors in this shape:

```json
{
  "timestamp": "2026-06-21T07:38:22.413Z",
  "status": 404,
  "error": "Not Found",
  "message": "Activity not found with id: abc123",
  "path": "/api/activities/abc123"
}
```

### Current status mapping

| Service | Condition | Status |
| --- | --- | ---: |
| `userservice` | validation failure on register payload | `400` |
| `userservice` | user profile not found | `404` |
| `activityservice` | invalid activity payload | `400` |
| `activityservice` | invalid user id for activity creation | `400` |
| `activityservice` | activity not found | `404` |
| `activityservice` | upstream user validation unavailable | `503` |
| `aiservice` | recommendation not found | `404` |
| `aiservice` | Gemini configuration unavailable | `503` |
| `gateway` | user sync / validation upstream unavailable | `503` |

### Important behavior notes

- `activityservice` now validates `type`, `duration`, and `caloriesBurned` explicitly.
- `aiservice` still falls back to default recommendations during async processing if Gemini calls or parsing fail.
- gateway identity failures now return a predictable JSON response instead of an opaque WebFlux default body.

## CI Pipelines

The repository now includes independent GitHub Actions workflows for each deployable part of the system.

### Workflow files

| Workflow | Trigger scope | Main commands |
| --- | --- | --- |
| `.github/workflows/frontend.yml` | `fitness-app-frontend/**` | `npm ci`, `npm run lint`, `npm run build` |
| `.github/workflows/eureka.yml` | `eureka/**` | `./mvnw test` |
| `.github/workflows/configserver.yml` | `configserver/**` | `./mvnw test` |
| `.github/workflows/userservice.yml` | `userservice/**` | `./mvnw test` |
| `.github/workflows/release-images.yml` | `main` branch changes or manual dispatch | build, push, and Trivy-scan GHCR images |
| `.github/workflows/deploy-k8s-prod.yml` | manual dispatch | create/update Kubernetes secret, apply overlay, wait for rollouts |
| `.github/workflows/activityservice.yml` | `activityservice/**` | `./mvnw test` |
| `.github/workflows/aiservice.yml` | `aiservice/**` | `./mvnw test` |
| `.github/workflows/gateway.yml` | `gateway/**` | `./mvnw test` |

### CI design choices

- each workflow uses path filters so unrelated services do not run unnecessarily
- frontend and backend failures are isolated to the changed component
- Java workflows standardize on Temurin `21`
- Maven caching and npm caching are enabled through the setup actions
- release images are published to GHCR with immutable `sha-<commit>` tags
- production deployment is driven through the Kubernetes overlay in `deploy/k8s/overlays/prod`

## Local Development Setup

### Prerequisites

You need the following installed locally:

- Java `21`
- Node.js `20+` and `npm`
- Docker Desktop with Compose support

For non-Docker development, you also need local instances of:

- PostgreSQL
- MongoDB
- RabbitMQ
- Keycloak

### Recommended mode: Docker Compose

This is the simplest way to run the full stack.

1. Create local environment overrides if needed:

```bash
cp .env.example .env
```

2. Start the stack:

```bash
make compose-up
```

This runs:

```bash
docker compose up --build
```

3. Stop the stack:

```bash
make compose-down
```

### Which YAML files matter locally

If you just want to orchestrate the project on your machine, only these matter at first:

- `docker-compose.yml`: starts the full local stack
- `.env` or `.env.example`: local environment values consumed by Compose
- `configserver/src/main/resources/config/*.yml`: runtime configuration served to the Spring services
- `*/src/main/resources/application.yml`: bootstrap config for each service

These do not affect your local run unless you explicitly use them:

- `.github/workflows/*.yml`: GitHub Actions CI/CD pipelines
- `deploy/k8s/**/*.yaml`: Kubernetes manifests for a production-style deployment target

So the local-first path is:

1. Use `docker-compose.yml` and `.env`
2. Ignore `.github/workflows` and `deploy/k8s`
3. Bring the full stack up with `make compose-up`

### Docker services and default local credentials

| System | Username | Password |
| --- | --- | --- |
| PostgreSQL | `postgres` | `postgres` |
| RabbitMQ | `guest` | `guest` |
| Keycloak admin | `admin` | `admin` |

### Docker environment variables

| Variable | Default | Used by |
| --- | --- | --- |
| `POSTGRES_DB` | `fitSphere` | PostgreSQL, userservice |
| `POSTGRES_USER` | `postgres` | PostgreSQL, userservice |
| `POSTGRES_PASSWORD` | `postgres` | PostgreSQL, userservice |
| `RABBITMQ_DEFAULT_USER` | `guest` | RabbitMQ, activityservice, aiservice |
| `RABBITMQ_DEFAULT_PASS` | `guest` | RabbitMQ, activityservice, aiservice |
| `KEYCLOAK_ADMIN` | `admin` | Keycloak bootstrap |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Keycloak bootstrap |
| `KEYCLOAK_REALM` | `fitSphere-auth` | gateway, frontend, Keycloak realm selection |
| `KEYCLOAK_CLIENT_ID` | `oauth2-pkce-client` | frontend client id |
| `GEMINI_API_KEY` | empty | aiservice |
| `GEMINI_MODEL` | `gemini-2.5-flash` | aiservice |
| `GEMINI_RESPONSE_TIMEOUT` | `30s` | aiservice |
| `GEMINI_READ_TIMEOUT` | `30s` | aiservice |
| `GEMINI_REQUEST_TIMEOUT` | `35s` | aiservice |
| `GEMINI_RETRIES` | `1` | aiservice |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | gateway |
| `INTERNAL_SERVICE_TOKEN` | `fitsphere-internal-dev-token` | gateway, userservice, activityservice, aiservice |
| `GATEWAY_REQUIRE_HTTPS` | `false` | gateway |

### Local schema reset note

The `userservice` no longer stores any password column for Keycloak-synchronized users. If you already have an older local PostgreSQL volume, your `users` table may still contain the old `password` column with a `NOT NULL` constraint.

For local Docker Compose, the cleanest fix is to recreate the Postgres volume once:

```bash
docker compose down -v
make compose-up
```

That resets local database state and lets Hibernate create the current schema cleanly.

### Local development without Docker

Start infrastructure first:

1. PostgreSQL on `localhost:5432` with database `fitSphere`
2. MongoDB on `localhost:27017`
3. RabbitMQ on `localhost:5672`
4. Keycloak on `localhost:8181`

The imported development realm should be:

- realm: `fitSphere-auth`
- client: `oauth2-pkce-client`
- default realm role: `FIT_USER`
- optional elevated realm role: `FIT_ADMIN`

Start backend services in this order:

```bash
cd eureka && ./mvnw spring-boot:run
cd configserver && ./mvnw spring-boot:run
cd userservice && ./mvnw spring-boot:run
cd activityservice && ./mvnw spring-boot:run
cd aiservice && GEMINI_API_KEY=your-key ./mvnw spring-boot:run
cd gateway && ./mvnw spring-boot:run
```

Start the frontend:

```bash
cd fitness-app-frontend
npm install
npm run dev
```

Frontend runtime variables:

| Variable | Default |
| --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` |
| `VITE_KEYCLOAK_BASE_URL` | `http://localhost:8181` |
| `VITE_KEYCLOAK_REALM` | `fitSphere-auth` |
| `VITE_KEYCLOAK_CLIENT_ID` | `oauth2-pkce-client` |
| `VITE_APP_URL` | browser origin |

### Build and verification

Build backend services:

```bash
make build-backend
```

Run backend tests:

```bash
make test-backend
```

Build frontend:

```bash
make build-frontend
```

Lint frontend:

```bash
make lint-frontend
```

Run a single service test suite manually:

```bash
cd activityservice && ./mvnw test
cd gateway && ./mvnw test
cd userservice && ./mvnw test
cd aiservice && ./mvnw test
```

### Local troubleshooting

#### Port conflicts

If `docker compose up --build` fails with `bind: address already in use`, stop the local process using that port first. Typical collisions:

- `5432` PostgreSQL
- `27017` MongoDB
- `5672` / `15672` RabbitMQ
- `8181` Keycloak
- `8761` Eureka
- `8888` Config Server
- `8080` / `8081` / `8082` / `8083` backend services
- `3000` Docker frontend
- `5173` local Vite frontend

#### MongoDB healthcheck logs

Repeated `Connection ended` and `Connection not authenticating` lines from MongoDB are expected when Docker healthchecks run:

```yaml
mongosh --quiet --eval "db.adminCommand('ping').ok"
```

Those lines are noise, not application failures.

#### Recommendation timing

Recommendations are generated asynchronously. A newly created activity may return `404` from:

```text
GET /api/recommendations/activity/{activityId}
```

until the AI pipeline finishes.

## Deployment Docs

### Repository deployment targets

The repository now ships with:

- Dockerfiles for all runtime services
- a working `docker-compose.yml` for local development
- a Kubernetes baseline and production overlay under [deploy/k8s](/Users/abhising/DevTanishq/IntellijAll/FitSphere/deploy/k8s)
- a GHCR image-release workflow
- a Kubernetes production deploy workflow

### Production target

The intended production shape is:

1. `frontend` and `gateway` deployed in Kubernetes behind ingress and TLS
2. `userservice`, `activityservice`, `aiservice`, `eureka`, and `configserver` as internal Kubernetes workloads
3. managed PostgreSQL, MongoDB, RabbitMQ, and Keycloak outside the cluster or in separately governed infrastructure
4. Kubernetes secrets created at deploy time from GitHub environment secrets
5. immutable container image tags from GitHub Actions

### Production docs

- [Kubernetes deployment README](deploy/k8s/README.md)
- [Production deployment target](docs/deployment/production-kubernetes.md)
- [Runtime operations guide](docs/operations/runtime-operations.md)

### Deployment order

For a fresh production environment:

1. provision PostgreSQL, MongoDB, RabbitMQ, ingress, TLS, and Keycloak
2. publish application images to GHCR
3. create Kubernetes secrets from production credentials
4. apply `deploy/k8s/overlays/prod`
5. verify rollout status and ingress reachability

### Production checklist

- set real database and broker credentials
- set a real `GEMINI_API_KEY`
- set a long random `INTERNAL_SERVICE_TOKEN`
- replace ingress hostnames and TLS secret names
- tighten `CORS_ALLOWED_ORIGINS`
- verify Keycloak JWK URL and realm settings
- ensure managed database backup and restore policy exists
- wire logs, metrics, and alerts into your platform observability stack

## Security Docs

### Current security model

The current security model is:

- the frontend authenticates through Keycloak using OAuth2 Authorization Code Flow with PKCE
- the gateway validates JWTs as a resource server and derives both identity and realm roles from the token
- the gateway syncs that identity into `userservice`
- the gateway forwards `X-User-ID`, `X-User-Roles`, `X-Correlation-ID`, and `X-Internal-Token` to downstream services
- downstream services reject direct API calls without the shared internal token
- user-owned reads are enforced in the service layer, with `FIT_ADMIN` allowed as an override role

### Secret handling

Do not commit real values for:

- `POSTGRES_PASSWORD`
- `KEYCLOAK_ADMIN_PASSWORD`
- `GEMINI_API_KEY`
- any future client secrets or database credentials

Use `.env.example` as the template and keep real secrets out of Git.

For Kubernetes production deploys, use GitHub environment secrets plus the secret creation step in `.github/workflows/deploy-k8s-prod.yml`.

### Current security posture

What is in place:

- JWT validation in the gateway
- role-based authorization in the gateway using Keycloak realm roles
- strict CORS configuration in the gateway
- Keycloak-based login for the frontend
- service-to-service routing through the gateway
- internal service authentication with a shared `X-Internal-Token`
- ownership checks for user, activity, and recommendation reads
- HTTPS enforcement support in the gateway and TLS redirect in the Kubernetes ingress
- correlation-id propagation for synchronous service calls
- structured audit logs on identity sync and user-owned resource access
- Prometheus scrape endpoints and example Kubernetes alerting manifests
- user API responses no longer expose password fields
- bounded retries and request timeouts exist for critical upstream calls
- RabbitMQ dead-letter routing is configured for the activity pipeline
- recommendation writes are idempotent on `activityId`


### Data boundaries

- `userservice` owns user profile data in PostgreSQL
- `activityservice` owns activity history in MongoDB
- `aiservice` owns recommendation history in MongoDB
- RabbitMQ carries activity events between activity tracking and AI processing

Each bounded context should continue to own its storage independently. Avoid direct cross-service database access.

## Limitations

These are the main limitations visible in the current implementation:

| Area                    | Current state                                                                                                               |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Recommendation pipeline | asynchronous; recommendations may not exist immediately after an activity is saved                                          |
| Observability           | correlation ids, audit logs, Prometheus endpoints, and example alert rules are included; no full telemetry stack is bundled |
| Deployment              | local orchestration is Docker Compose-based; Kubernetes manifests also exist under `deploy/k8s`                             |
| Rate Limiting           | rate limiting and abuse controls are not implemented                                                                        |