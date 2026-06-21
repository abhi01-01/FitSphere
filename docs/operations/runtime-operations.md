# Runtime Operations

This document summarizes the runtime behaviors that matter once FitSphere is deployed.

## Health endpoints

Spring services expose:

- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`

These are used by the Kubernetes manifests in [deploy/k8s](/Users/abhising/DevTanishq/IntellijAll/FitSphere/deploy/k8s).

## Timeouts and retries

The following paths now use explicit timeout and retry controls:

- gateway -> userservice validation and registration calls
- activityservice -> userservice validation calls
- aiservice -> Gemini API calls

Gemini timeouts are intentionally longer than the internal service timeouts because free-tier or cold external API responses can be slow. When Gemini times out or returns a retryable provider error, `aiservice` stores a default recommendation and logs a concise fallback warning instead of failing the activity pipeline.

## Request tracing and audit logs

- the gateway creates or forwards `X-Correlation-ID`
- the gateway forwards correlation ids to downstream synchronous service calls
- `userservice`, `activityservice`, and `aiservice` place the correlation id into the logging MDC
- audit logs are emitted for:
  - Keycloak user sync create and update events
  - user profile reads
  - activity creation and reads
  - recommendation reads

These are configured through the Config Server service config files.

## Queue safety

RabbitMQ now has:

- a primary activity queue
- a dead-letter exchange
- a dead-letter queue
- bounded retry attempts on the AI consumer

## Idempotency

`aiservice` now prevents duplicate recommendation creation by:

- checking whether a recommendation already exists for `activityId`
- enforcing a unique MongoDB index on `Recommendation.activityId`
- treating duplicate writes as already-processed events

## Recommended alerts

At minimum, alert on:

- service scrape failures on `/actuator/prometheus`
- gateway readiness failures
- repeated `503` errors from user-service validation paths
- growth in the activity dead-letter queue
- AI service recommendation fallback spikes
- MongoDB, PostgreSQL, or RabbitMQ connectivity failures

Example `ServiceMonitor` and `PrometheusRule` manifests are included in [deploy/k8s/base](/Users/abhising/DevTanishq/IntellijAll/FitSphere/deploy/k8s/base).
