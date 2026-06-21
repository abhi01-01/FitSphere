# Activity Service Architecture

The activity service validates the effective user, persists workout activity documents, and publishes activity events for asynchronous AI processing.

## Runtime Flow

```mermaid
flowchart TB
    gateway["Gateway"]
    post["POST /api/activities"]
    list["GET /api/activities"]
    one["GET /api/activities/:activityId"]
    internalauth["InternalRequestInterceptor"]
    controller["ActivityController"]
    validation["Bean Validation on ActivityRequest"]
    userheader["X-User-ID header injection"]
    userroles["Allow self or FIT_ADMIN"]
    usersvc["UserValidationService"]
    userapi["GET /api/users/:userId/validate"]
    service["ActivityService"]
    repo["ActivityRepository"]
    mongo["MongoDB activities"]
    rabbit["RabbitMQ exchange and queue"]
    error["GlobalExceptionHandler"]

    gateway --> internalauth
    internalauth --> post
    internalauth --> list
    internalauth --> one

    post --> controller
    controller --> validation
    validation --> userheader
    userheader --> service
    service --> usersvc
    usersvc --> userapi
    userapi -->|valid| repo
    userapi -->|invalid| error
    repo --> mongo
    service -->|convertAndSend| rabbit

    list --> controller
    controller --> service
    service --> repo

    one --> controller
    controller --> service
    service --> repo
    repo --> userroles
    repo -->|not found| error
```

## ER Diagram

`additionalMetrics` is stored as a `Map<String, Object>` inside the MongoDB activity document. The child entity below is a logical view of that embedded map.

```mermaid
erDiagram
    USER_REFERENCE ||--o{ ACTIVITY : owns
    ACTIVITY ||--o{ ACTIVITY_METRIC : embeds

    USER_REFERENCE {
        string keycloakId PK
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
```
