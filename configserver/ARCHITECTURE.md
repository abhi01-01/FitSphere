# Config Server Architecture

The config server serves classpath-backed configuration files to the runtime services during startup.

## Runtime Flow

```mermaid
flowchart TB
    svc[Gateway or Domain Service Startup]
    configserver[Config Server]
    native[Native Profile]
    classpath[classpath:/config]
    gatewaycfg[api-gateway.yml]
    usercfg[user-service.yml]
    activitycfg[activity-service.yml]
    aicfg[ai-service.yml]

    svc -->|spring.config.import=configserver| configserver
    configserver --> native
    native --> classpath
    classpath --> gatewaycfg
    classpath --> usercfg
    classpath --> activitycfg
    classpath --> aicfg
```

## Logical ER Diagram

The config server does not persist relational data. This ER diagram models the configuration documents it serves.

```mermaid
erDiagram
    CONFIG_APPLICATION ||--o{ CONFIG_FILE : resolves_to
    CONFIG_FILE ||--o{ PROPERTY_ENTRY : contains

    CONFIG_APPLICATION {
        string applicationName PK
        string profile
        string label
    }

    CONFIG_FILE {
        string filename PK
        string serviceName
        string sourceType
    }

    PROPERTY_ENTRY {
        string filename FK
        string key
        string value
    }
```
