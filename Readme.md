## FitSphere

FitSphere is a modern microservices-based AI fitness web application designed to track workouts, analyze user activity, and offer personalized exercise recommendations. It demonstrates a scalable backend architecture using Java, Spring Boot, Spring Cloud, REST APIs, and frontend UI components.

## Description

FitSphere aims to help users track their fitness journey through a modular, production-style system. Rather than a monolithic app, it uses multiple services that communicate with each other, giving a real-world example of how to design microservices in Java. The backend logic handles user activity, request routing, data persistence, discovery, and configuration, while the frontend provides an interactive user interface.

## Tech Stack

>	Backend
>> * Java 21 (Spring Boot)
>> * Spring Cloud (Microservices tooling)
>> * Eureka (Service discovery)
>> * Config Server (Centralized configuration)
>>
>	Frontend
>> * JavaScript / React
>> * UI components & styling
>>
> API Architecture
>> * REST APIs
>> * API Gateway (routing + security)
>>
> Tools & Utilities
>> * Maven (build & dependency management)
>> * Docker (optional containerization)
>>
> Database
>> * Managed via backend service configs

## Project Structure

The repository contains multiple service modules under one workspace: 
├── activityservice
├── aiservice
├── configserver
├── eureka
├── fitness-app-frontend
├── gateway
├── userservice
└── Readme.md

Each folder represents a distinct microservice or module:
	•	activityservice: Tracks workout activities
	•	aiservice: AI/ML logic (if integrated)
	•	configserver: Centralized Spring config server
	•	eureka: Microservices discovery server
	•	gateway: API gateway for routing requests
	•	userservice: User management + authentication
	•	fitness-app-frontend: Web UI


## Setup & Installation

Follow these steps to get the project running locally.

Prerequisites

Make sure you have the following installed:
	•	Java JDK 21
	•	Maven
	•	Node.js & npm
	•	(Optional) Docker and Docker Compose


### Backend Setup (Java Services)

1. Clone the repo
  
```java
git clone https://github.com/abhi01-01/FitSphere.git
cd FitSphere
````

2. Build all services

Open a terminal and run:

```java
mvn clean install
```

If each module has its own pom.xml, run inside each service folder:

```java
cd activityservice
mvn clean package
```


### Running the Services

Start backend services in order:

1. Config Server
```java
cd configserver
mvn spring-boot:run
```
2. Eureka Discovery

```java
cd eureka
mvn spring-boot:run
```

3. Other Services
Repeat:
```java
cd activityservice
mvn spring-boot:run
```

```java
cd userservice
mvn spring-boot:run
```

And similarly for aiservice and gateway.

### Frontend Setup

1. Navigate to fitness-app-frontend:

```java
cd fitness-app-frontend
```

2. Install dependencies:

```java
npm install
```

3. Start the frontend:

```java
npm start
```

Open the browser at:

```java
http://localhost:3000
```

## Usage

Once everything is running:

1. Access the frontend UI at the port configured (http://localhost:3000 by default).

2. The frontend will call backend APIs routed through the gateway.

3. Backend services will discover each other via Eureka and load configs from the Config Server.

4. You can:

* Sign up and log in users

* Track workouts and activity

* View dashboards and recommendations

## Contributions

* Contributions are welcome! You can help by:

* Opening issues for bugs or enhancements

* Submitting pull requests

* Improving documentation

Please follow best practices for commits and use clear descriptions.


## Acknowledgments

Built and maintained by @abhi01-01
. Thanks for exploring FitSphere!






