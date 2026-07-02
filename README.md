# Language Learning Platform

Full-stack web application for interactive foreign-language learning with AI-assisted feedback and a conversational learning module.

This project was developed as part of an engineering thesis at the University of Rzeszów.

The written thesis document is not included in this repository. This repository contains only the source code of the application.

## Overview

The system consists of two main modules:

- `frontend` - Angular single-page application
- `backend` - Spring Boot REST API

The application supports user registration, login, course browsing, lesson navigation, exercise solving, progress tracking, spaced repetition, AI-assisted feedback, chat-based language practice and an administrative panel for content management.

The application has also been deployed and tested on Amazon EC2 using Docker Compose as a portfolio-oriented AWS deployment.

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
- Nginx for serving the production build

### Backend

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Maven Wrapper
- Claude API integration
- Apache PDFBox
- Testcontainers
- JUnit 5

### Infrastructure

- Docker
- Docker Compose
- PostgreSQL container
- Nginx container for serving the Angular production build
- Spring Boot container for the backend API
- Amazon EC2
- IAM role for EC2
- AWS security groups
- CloudWatch billing alerts
- AWS Budget for cost monitoring

## Repository structure

```text
language-learning-platform/
├── backend/
│   ├── docker/
│   │   └── initdb/
│   ├── photos/
│   │   └── .gitkeep
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
├── docs/
│   └── aws-deployment.md
├── compose.yaml
├── compose.ec2.yaml
├── .env.ec2.example
├── .gitignore
└── README.md
```

## Local quick start with Docker

The recommended way to run the whole application locally is Docker Compose.

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

## Local Docker services

The local Docker Compose setup starts:

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

## AWS deployment

The application has been deployed and tested using a simple AWS setup intended for portfolio and recruitment purposes.

Deployment setup:

- Amazon EC2
- Docker Compose
- Angular production build served by Nginx
- Spring Boot backend API
- PostgreSQL running as a Docker container
- IAM role assigned to the EC2 instance
- Security group exposing only HTTP `80` publicly and SSH `22` from a restricted IP
- CloudWatch billing alarm and AWS Budget for cost monitoring

The AWS deployment uses:

```text
compose.ec2.yaml
.env.ec2.example
docs/aws-deployment.md
```

The real `.env.ec2` file is created only on the EC2 instance and must not be committed.

The deployed application is available through the EC2 public IPv4 address while the instance is running. The public IP may change after stopping and starting the instance unless an Elastic IP is configured.

Detailed deployment notes are available in:

```text
docs/aws-deployment.md
```

## AWS deployment architecture

```text
User browser
    |
    | HTTP :80
    v
Amazon EC2
    |
    | Docker Compose
    |
    |-- frontend container
    |   Angular production build served by Nginx
    |   Public port: 80
    |
    |-- backend container
    |   Spring Boot REST API
    |   Internal port: 8080
    |
    |-- database container
        PostgreSQL
        Internal port: 5432
```

Only the frontend container is exposed publicly.

The backend and database are not exposed directly to the internet. The Nginx frontend container proxies `/api` requests to the backend container inside the Docker network.

## AWS deployment quick start

Create an environment file on the EC2 instance:

```bash
cp .env.ec2.example .env.ec2
```

Generate secrets:

```bash
openssl rand -base64 32
openssl rand -base64 32
openssl rand -hex 16
```

Fill `.env.ec2`:

```env
POSTGRES_PASSWORD=replace-with-strong-postgres-password
SECURITY_JWT_SECRET=replace-with-generated-base64-secret
SECURITY_FIELD_ENCRYPTION_KEY=replace-with-generated-base64-secret
ANTHROPIC_API_KEY=
```

Start the AWS deployment:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml up -d --build
```

Check containers:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml ps
```

Expected result:

```text
language-learning-database    Up / healthy
language-learning-backend     Up
language-learning-frontend    Up
```

After deployment, the application is available at:

```text
http://EC2_PUBLIC_IP
```

Do not open backend or database ports publicly.

These ports should remain internal:

```text
8080
5432
```

## Demo configuration

The local Docker Compose setup is intended for local portfolio testing and development preview.

It uses public demo values for:

- `SECURITY_JWT_SECRET`
- `SECURITY_FIELD_ENCRYPTION_KEY`
- PostgreSQL username and password

These values are safe only for local demo usage. Replace them before any public deployment.

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

These credentials are intended only for local testing and portfolio demo usage.

## Stopping the application

### Local Docker Compose

Stop containers:

```bash
docker compose down
```

Stop containers and remove the database volume:

```bash
docker compose down -v
```

Use `docker compose down -v` when you want to reset the local database completely.

### AWS Docker Compose

Stop containers on EC2:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml down
```

Stop containers and remove Docker volumes on EC2:

```bash
docker compose --env-file .env.ec2 -f compose.ec2.yaml down -v
```

Removing volumes deletes the PostgreSQL data stored in Docker.

To reduce AWS costs, stop or terminate the EC2 instance when the deployment is no longer needed.

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
- Do not commit `.env.ec2`.
- Do not commit `.env.aws`.
- Do not commit `.pem` SSH keys.
- Do not commit `secrets.properties`.
- Do not commit real API keys.
- Do not commit AWS access keys.
- Do not use demo JWT or encryption keys in production.
- Do not expose PostgreSQL publicly.
- Do not expose backend port `8080` publicly.
- Restrict SSH access to a trusted IP address.
- Use IAM roles instead of static AWS credentials.
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
.env
.env.local
.env.production
.env.ec2
.env.aws
*.pem
```

## Project context

This project was created as an engineering thesis project.

Thesis topic:

```text
Interactive foreign language learning platform with a conversational module based on a large language model
```

This repository contains the source code only.

The written thesis document, university submission files, reviews and administrative documents are not included.

## Portfolio summary

This project demonstrates practical usage of:

- Java 21
- Spring Boot
- REST API development
- Spring Security and JWT authentication
- PostgreSQL
- Angular
- TypeScript
- Docker
- Docker Compose
- Nginx reverse proxy
- Amazon EC2
- IAM role configuration
- AWS security groups
- CloudWatch billing alerts
- environment-based deployment configuration
- full-stack deployment of Angular and Spring Boot

Possible future improvements include:

- Amazon RDS PostgreSQL
- Amazon S3 file storage
- CloudWatch Agent log collection
- HTTPS and domain configuration
- CI/CD pipeline
- separate production Spring profile
- database migration tool such as Flyway or Liquibase

## License

This repository is published for portfolio and recruitment purposes only.

All rights reserved.
