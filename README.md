# Language Learning Platform

Full-stack web application for interactive foreign-language learning with AI-assisted feedback and a conversational learning module.

This project was developed as part of an engineering thesis at the University of Rzeszów.

The written thesis document is not included in this repository. This repository contains only the source code of the application.

## Overview

The system consists of two main modules:

- `frontend` - Angular single-page application
- `backend` - Spring Boot REST API

The application supports user registration, login, course browsing, lesson navigation, exercise solving, progress tracking, spaced repetition, AI-assisted feedback, chat-based language practice and an administrative panel for content management.

## Main features

### User features

- User registration and login
- JWT-based authentication
- Refresh token flow with `HttpOnly` cookies
- Public course catalog
- Course enrollment
- Lesson and exercise views
- Exercise solving and attempt history
- AI-assisted hints and feedback
- Conversational AI learning module
- Progress dashboard
- Learning statistics
- Spaced repetition system
- User profile management
- PDF report export

### Admin features

- Administrative panel
- Course management
- Lesson management
- Exercise management
- Language and proficiency level management
- Achievement management
- User management
- Chat session and chat message overview
- AI-generated exercise support
- LLM logs and usage statistics
- Admin statistics export

## Tech stack

### Frontend

- Angular 18
- TypeScript
- HTML
- SCSS
- RxJS
- Angular Router
- Angular HttpClient
- Playwright
- npm
- Node.js 20.x LTS or newer

### Backend

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Docker / Docker Compose
- Maven Wrapper
- Claude API integration
- Apache PDFBox
- Testcontainers
- JUnit 5

### Infrastructure

- Docker Compose
- PostgreSQL container
- Nginx container for serving the Angular production build
- Spring Boot container for the backend API

## Repository structure

```text
language-learning-platform/
├── backend/
│   ├── docker/
│   │   └── initdb/
│   ├── photos/
│   ├── src/
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── secrets.properties.example
├── frontend/
│   ├── e2e/
│   ├── public/
│   ├── src/
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── nginx.conf
│   ├── angular.json
│   ├── package.json
│   ├── package-lock.json
│   ├── playwright.config.ts
│   └── proxy.conf.json
├── compose.yaml
├── .gitignore
└── README.md
```

## Quick start with Docker

The recommended way to run the whole application is Docker Compose.

From the root directory:

```bash
docker compose up --build
```

After startup, open:

```text
http://localhost:4200
```

The backend is also available directly at:

```text
http://localhost:8080
```

If Swagger UI is enabled, it may be available at:

```text
http://localhost:8080/swagger-ui/index.html
```

or:

```text
http://localhost:8080/swagger-ui.html
```

## Docker services

The Docker Compose setup starts:

- PostgreSQL database
- Spring Boot backend
- Angular frontend served by Nginx

The frontend container exposes the application on:

```text
http://localhost:4200
```

The backend container exposes the REST API on:

```text
http://localhost:8080
```

The frontend communicates with the backend through relative `/api` URLs. Nginx proxies `/api/*` requests to the backend container.

## Demo configuration

The Docker Compose setup is intended for local portfolio testing and development preview.

It uses public demo values for:

- `SECURITY_JWT_SECRET`
- `SECURITY_FIELD_ENCRYPTION_KEY`
- PostgreSQL username and password

These values are safe only for local demo usage. Replace them before any production deployment.

AI-assisted features require a real Anthropic Claude API key. By default, the Docker Compose setup runs with:

```text
ANTHROPIC_API_KEY=""
```

This means that the application can run without exposing a real API key, but AI-related functionality may be disabled or unavailable depending on the backend configuration.

## Demo credentials

The local development seed may create an administrator account.

Default local credentials:

```text
login: admin
password: password
```

These credentials are intended only for local testing.

## Stopping the application

Stop containers:

```bash
docker compose down
```

Stop containers and remove the database volume:

```bash
docker compose down -v
```

Use `docker compose down -v` when you want to reset the local database completely.

## Manual local development

Docker Compose is the easiest way to run the full application, but both modules can also be started manually.

### Backend requirements

- Java 21 JDK
- Maven Wrapper
- PostgreSQL
- Docker, if using Testcontainers or the provided database setup

### Frontend requirements

- Node.js 20.x LTS or newer
- npm
- Angular CLI, optional

### Backend setup

From the `backend` directory:

```bash
cp secrets.properties.example secrets.properties
```

Fill in local configuration values if needed.

Start the backend:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows PowerShell:

```powershell
.\mvnw.cmd --% spring-boot:run -Dspring-boot.run.profiles=dev
```

By default, the backend runs on:

```text
http://localhost:8080
```

### Frontend setup

From the `frontend` directory:

```bash
npm install
npm start
```

By default, the frontend runs on:

```text
http://localhost:4200
```

For local development, `proxy.conf.json` forwards API requests to the backend.

## Authentication flow

1. The user logs in through the frontend login form.
2. The backend validates credentials.
3. The backend returns a JWT access token in the response body.
4. The backend sets a refresh token in an `HttpOnly` cookie.
5. The frontend sends the access token in the `Authorization: Bearer ...` header.
6. When the access token expires, the frontend calls `/api/auth/refresh`.
7. The backend validates the refresh token and issues a new access token.
8. Frontend interceptors handle `401` responses and retry the original request when possible.

## Key API areas

The backend exposes REST API endpoints under `/api`.

Selected API areas:

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/courses`
- `POST /api/enrollments`
- `GET /api/lessons/{lessonId}/exercises`
- `POST /api/exercise-attempts`
- `POST /api/exercise-attempts/{attemptId}/evaluations`
- `POST /api/ai/hints`
- `POST /api/chat-sessions/{id}/ai-messages`
- `POST /api/lessons/{lessonId}/exercises`
- `GET /api/users/me/export`
- `GET /api/llm-logs`

## Frontend areas

Main UI areas:

- Home page
- Login and registration
- Course catalog
- Course details
- Lesson view
- Exercise solving
- Attempt history
- Chat-based learning module
- Dashboard
- Statistics
- Spaced repetition
- User profile
- Admin panel

## Backend architecture

The backend follows a layered architecture:

- controllers expose REST endpoints
- DTOs define API request and response models
- services contain business logic
- repositories handle database access
- entities represent the persistence model
- security components handle authentication and authorization
- AI services handle communication with the Claude API
- export services generate PDF reports

The backend uses PostgreSQL as the main database and Spring Data JPA for persistence.

## AI integration

AI-related features are handled exclusively by the backend.

The frontend does not communicate with the AI provider directly.

AI-supported functionality includes:

- hints for exercises
- feedback for user answers
- chat-based language practice
- AI-generated exercises
- logging of LLM interactions and usage statistics

A real Anthropic API key is required to use AI features.

## Testing

### Backend tests

From the `backend` directory:

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

The backend includes tests for:

- service layer logic
- REST endpoint behavior
- authentication and authorization
- database integration with Testcontainers
- selected AI-related flows with controlled responses

### Frontend tests

From the `frontend` directory:

```bash
npm run test
```

End-to-end tests are implemented with Playwright:

```bash
npm run e2e
```

The exact scripts depend on the current `package.json`.

## Security notes

- Do not commit real secrets.
- Do not commit `.env`.
- Do not commit `secrets.properties`.
- Do not commit real API keys.
- Do not use demo JWT or encryption keys in production.
- The Docker Compose setup is for local demo usage only.
- Refresh tokens are stored in `HttpOnly` cookies.
- Administrative endpoints are protected by role checks.
- AI calls are performed only from the backend.

## Generated files and ignored files

The following files and directories should not be committed:

```text
frontend/node_modules/
frontend/dist/
frontend/.angular/
frontend/playwright-report/
frontend/test-results/
backend/target/
backend/.env
backend/secrets.properties
backend/logs/
backend/uploads/
backend/photos/avatars/
```

## Project context

This project was created as an engineering thesis project.

Thesis topic:

```text
Interactive foreign language learning platform with a conversational module based on a large language model
```

This repository contains the source code only.

The written thesis document, university submission files, reviews and administrative documents are not included.

## License

This repository is published for portfolio and recruitment purposes only.

All rights reserved.
