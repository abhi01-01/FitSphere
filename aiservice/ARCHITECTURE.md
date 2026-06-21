# AI Service Architecture

The AI service consumes activity events, calls Gemini, normalizes the response, and exposes recommendation lookup endpoints.

## Runtime Flow

```mermaid
flowchart TB
    caller["Gateway or Internal Caller"]
    getbyuser["GET /api/recommendations/user/:userId"]
    getbyactivity["GET /api/recommendations/activity/:activityId"]
    internalauth["InternalRequestInterceptor"]
    controller["RecommendationController"]
    readservice["RecommendationService"]
    ownership["Allow self or FIT_ADMIN"]
    listener["ActivityMessageListener"]
    queue["RabbitMQ activity.queue"]
    aiworker["ActivityAIService"]
    prompt["createPromptForActivity"]
    gemini["GeminiService.getAnswer"]
    parse["processAiResponse"]
    fallback["createDefaultRecommendation"]
    repo["RecommendationRepository"]
    mongo["MongoDB recommendations"]
    error["GlobalExceptionHandler"]

    queue --> listener
    listener --> aiworker
    aiworker --> prompt
    prompt --> gemini
    gemini -->|success| parse
    gemini -->|API failure or missing key| fallback
    parse -->|parse failure| fallback
    parse --> repo
    fallback --> repo
    repo --> mongo

    caller --> internalauth
    internalauth --> getbyuser
    internalauth --> getbyactivity
    getbyuser --> controller
    getbyactivity --> controller
    controller --> readservice
    readservice --> repo
    repo --> ownership
    repo -->|activity missing| error
```

## ER Diagram

The recommendation document stores lists for `improvements`, `suggestions`, and `safety`. The child entities below are logical views of those embedded arrays.

```mermaid
erDiagram
    ACTIVITY_REFERENCE ||--o{ RECOMMENDATION : drives
    USER_REFERENCE ||--o{ RECOMMENDATION : receives
    RECOMMENDATION ||--o{ RECOMMENDATION_IMPROVEMENT : embeds
    RECOMMENDATION ||--o{ WORKOUT_SUGGESTION : embeds
    RECOMMENDATION ||--o{ SAFETY_GUIDELINE : embeds

    ACTIVITY_REFERENCE {
        string activityId PK
        string activityType
    }

    USER_REFERENCE {
        string userId PK
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
