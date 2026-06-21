# Eureka Architecture

Eureka is the service registry used by the gateway and domain services for discovery.

## Runtime Flow

```mermaid
flowchart TB
    gateway[API Gateway]
    usersvc[User Service]
    activitysvc[Activity Service]
    aisvc[AI Service]
    eureka[Eureka Server]
    routes[Gateway Route Resolution]

    gateway -->|register| eureka
    usersvc -->|register| eureka
    activitysvc -->|register| eureka
    aisvc -->|register| eureka

    gateway -->|lookup USER-SERVICE| routes
    gateway -->|lookup ACTIVITY-SERVICE| routes
    gateway -->|lookup AI-SERVICE| routes
    routes --> eureka
```

## Logical ER Diagram

The registry is in-memory service-discovery state rather than an application database.

```mermaid
erDiagram
    SERVICE_REGISTRY ||--o{ SERVICE_INSTANCE : contains
    SERVICE_INSTANCE ||--o{ LEASE : renews

    SERVICE_REGISTRY {
        string registryName PK
        boolean registerWithEureka
        boolean fetchRegistry
    }

    SERVICE_INSTANCE {
        string serviceName PK
        string instanceId
        string host
        int port
        string status
    }

    LEASE {
        string instanceId FK
        datetime lastRenewal
        string heartbeatState
    }
```
