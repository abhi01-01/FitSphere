# Kubernetes Deployment

This directory contains a production-oriented Kubernetes baseline for FitSphere.

## Assumptions

- application services run in Kubernetes
- PostgreSQL, MongoDB, RabbitMQ, Keycloak, and DNS/TLS are managed externally
- container images are published to `ghcr.io`
- runtime secrets are injected at deploy time and are not committed to Git

## Layout

- `base/` contains reusable manifests for the application services
- `overlays/prod/` contains the production overlay
- `secrets.example.env` documents the required secret keys for the Kubernetes `fitsphere-secrets` secret
- `base/servicemonitor.yaml` and `base/prometheusrule.yaml` provide Prometheus Operator hooks for scraping and alerting

## Apply flow

1. Create a namespace:

```bash
kubectl create namespace fitsphere-prod
```

2. Create the shared secret from a local copy of `secrets.example.env`:

```bash
cp deploy/k8s/secrets.example.env deploy/k8s/secrets.env
kubectl -n fitsphere-prod create secret generic fitsphere-secrets \
  --from-env-file=deploy/k8s/secrets.env \
  --dry-run=client -o yaml | kubectl apply -f -
```

3. Apply the production overlay:

```bash
kubectl apply -k deploy/k8s/overlays/prod
```

4. Update ingress hosts and TLS secret names in `overlays/prod/ingress-patch.yaml` before a real deployment.

## Notes

- readiness and liveness probes use Spring Boot actuator health endpoints
- Prometheus Operator CRDs are assumed if you apply the bundled `ServiceMonitor` and `PrometheusRule` resources
- frontend configuration is build-time, so the frontend image must be built with production `VITE_*` values in CI
- RabbitMQ DLQ settings and client timeout settings are wired through the shared config map
