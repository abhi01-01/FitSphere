# Gateway Architecture

The gateway is the authenticated edge for all backend traffic. It validates JWTs, synchronizes the effective user identity, and routes requests to downstream services.

## Runtime Flow

```mermaid
flowchart TB
    client["Frontend Request"]
    security["SecurityWebFilterChain"]
    jwt["OAuth2 Resource Server JWT Validation"]
    sync["KeycloakUserSyncFilter"]
    claims["Extract JWT Claims and Realm Roles"]
    validate["UserService.validateUser(sub)"]
    register["UserService.registerUser(request)"]
    mutate["Add X-User-ID, X-User-Roles,<br/>X-Correlation-ID, X-Internal-Token"]
    route["Route by Path Predicate"]
    usersvc["User Service"]
    activitysvc["Activity Service"]
    aisvc["AI Service"]
    error["GatewayErrorWebExceptionHandler"]

    client --> security
    security --> jwt
    jwt --> sync
    sync --> claims
    claims --> validate
    validate -->|false| register
    validate -->|true| mutate
    register --> mutate
    mutate --> route
    route -->|"users routes"| usersvc
    route -->|"activity routes"| activitysvc
    route -->|"recommendation routes"| aisvc

    jwt -->|auth failure| error
    validate -->|upstream failure| error
    register -->|upstream failure| error
```

## Logical ER Diagram

The gateway has no persistent database. This ER diagram shows the request and identity objects it constructs and forwards.

```mermaid
erDiagram
    AUTHENTICATED_REQUEST ||--|| JWT_CLAIMS : contains
    JWT_CLAIMS ||--|| USER_SYNC_REQUEST : builds
    USER_SYNC_REQUEST ||--o| USER_VALIDATION_RESULT : checks
    AUTHENTICATED_REQUEST ||--o{ ROUTED_REQUEST : becomes
    ROUTED_REQUEST ||--o| API_ERROR : may_return

    AUTHENTICATED_REQUEST {
        string method
        string path
        string authorizationHeader
    }

    JWT_CLAIMS {
        string sub PK
        string email
        string given_name
        string family_name
    }

    USER_SYNC_REQUEST {
        string email
        string keycloakId
        string firstName
        string lastName
    }

    USER_VALIDATION_RESULT {
        string keycloakId PK
        boolean exists
    }

    ROUTED_REQUEST {
        string targetService
        string path
        string xUserId
        string xUserRoles
        string xInternalToken
    }

    API_ERROR {
        int status
        string error
        string message
        string path
    }
```
