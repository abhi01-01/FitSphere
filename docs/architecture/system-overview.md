# FitSphere System Architecture

This document is the source of truth for the platform-level runtime topology and cross-service data model.

## Runtime Topology

```mermaid
flowchart TB
    user["Browser User"]

    subgraph client[Client]
        frontend["React Frontend<br/>Vite SPA"]
        keycloak["Keycloak<br/>realm: fitSphere-auth"]
    end

    subgraph edge[Edge and Platform]
        gateway["API Gateway<br/>Spring Cloud Gateway"]
        config["Config Server<br/>classpath native config"]
        eureka["Eureka<br/>service registry"]
    end

    subgraph domain[Domain Services]
        usersvc["User Service"]
        activitysvc["Activity Service"]
        aisvc["AI Service"]
    end

    subgraph state[Stateful Dependencies]
        userdb["PostgreSQL<br/>users"]
        activitydb["MongoDB<br/>activities"]
        rabbit["RabbitMQ<br/>activity.queue"]
        recdb["MongoDB<br/>recommendations"]
        gemini["Gemini API"]
    end

    user --> frontend
    frontend -->|OIDC PKCE login and logout| keycloak
    frontend -->|"Bearer JWT /api/*"| gateway

    gateway -. JWK set and JWT trust .-> keycloak
    gateway -. startup config .-> config
    usersvc -. startup config .-> config
    activitysvc -. startup config .-> config
    aisvc -. startup config .-> config

    gateway -. register and lookup .-> eureka
    usersvc -. register .-> eureka
    activitysvc -. register .-> eureka
    aisvc -. register .-> eureka

    gateway -->|"JWT -> X-User-ID, X-User-Roles,<br/>X-Correlation-ID, X-Internal-Token"| usersvc
    gateway -->|"JWT -> X-User-ID, X-User-Roles,<br/>X-Correlation-ID, X-Internal-Token"| activitysvc
    gateway -->|"JWT -> X-User-ID, X-User-Roles,<br/>X-Correlation-ID, X-Internal-Token"| aisvc

    activitysvc -->|validate X-User-ID with X-Internal-Token| usersvc
    usersvc --> userdb
    activitysvc --> activitydb
    activitysvc -->|publish activity event| rabbit
    aisvc -->|consume activity event| rabbit
    aisvc --> gemini
    aisvc --> recdb
```

## Detailed Request and Event Path

```mermaid
flowchart LR
    frontend["Frontend"]
    gateway["Gateway"]
    syncfilter["KeycloakUserSyncFilter"]
    usersvc["User Service"]
    activitysvc["Activity Service"]
    activitydb["Activities MongoDB"]
    rabbit["RabbitMQ"]
    listener["ActivityMessageListener"]
    aiworker["ActivityAIService"]
    aisvc["Recommendation Controller and Service"]
    gemini["Gemini API"]
    recdb["Recommendations MongoDB"]

    frontend -->|"Bearer JWT + activity payload"| gateway
    gateway --> syncfilter
    syncfilter -->|"extract sub, roles, email,<br/>given_name, family_name"| usersvc
    usersvc -->|"validate existing user by keycloakId<br/>register if absent by email"| syncfilter
    syncfilter -->|"forward X-User-ID, X-User-Roles,<br/>X-Correlation-ID, X-Internal-Token"| activitysvc
    activitysvc -->|"GET /api/users/:sub/validate<br/>with X-Internal-Token"| usersvc
    activitysvc -->|"save activity document"| activitydb
    activitysvc -->|"publish activity event"| rabbit
    rabbit --> listener
    listener --> aiworker
    aiworker -->|"build prompt from activity"| gemini
    aiworker -->|"parse response or fallback"| recdb
    frontend -->|"GET /api/recommendations/activity/:activityId"| gateway
    gateway -->|"route query"| aisvc
    aisvc --> recdb
```

## Whole-System ER Diagram

This is a logical cross-store ER view. For MongoDB documents and list fields, child entities below represent embedded or repeated structures rather than separate physical tables.

```mermaid
erDiagram
    KEYCLOAK_SUBJECT ||--o| APP_USER : mapped_to
    APP_USER ||--o{ ACTIVITY : logs
    ACTIVITY ||--o| ACTIVITY_EVENT : published_as
    ACTIVITY ||--o| RECOMMENDATION : analyzed_into
    ACTIVITY ||--o{ ACTIVITY_METRIC : carries
    RECOMMENDATION ||--o{ RECOMMENDATION_IMPROVEMENT : contains
    RECOMMENDATION ||--o{ WORKOUT_SUGGESTION : contains
    RECOMMENDATION ||--o{ SAFETY_GUIDELINE : contains

    KEYCLOAK_SUBJECT {
        string sub PK
        string email
        string given_name
        string family_name
    }

    APP_USER {
        string id PK
        string email UK
        string keycloakId UK
        string firstName
        string lastName
        string role
        datetime createdAt
        datetime updatedAt
    }

    ACTIVITY {
        string id PK
        string userId FK
        string type
        int duration
        int caloriesBurned
        datetime startTime
        datetime createdAt
        datetime updatedAt
    }

    ACTIVITY_METRIC {
        string activityId FK
        string metricKey
        string metricValue
    }

    ACTIVITY_EVENT {
        string activityId FK
        string routingKey
        datetime publishedAt
    }

    RECOMMENDATION {
        string id PK
        string activityId FK
        string userId FK
        string activityType
        string recommendation
        datetime createdAt
    }

    RECOMMENDATION_IMPROVEMENT {
        string recommendationId FK
        string area
        string recommendationText
    }

    WORKOUT_SUGGESTION {
        string recommendationId FK
        string workout
        string description
    }

    SAFETY_GUIDELINE {
        string recommendationId FK
        string text
    }
```
