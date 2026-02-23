# TaskHub App

## Overview
TaskHub is a Spring Boot backend for managing tasks (CRUD). The list endpoint is paginated to behave well as data grows.

This repository focuses on the **application**:
- Spring Boot REST API + validation + error handling
- Tests (Gradle)
- Docker image build (used by CI)
- Metrics via Actuator/Micrometer (`/actuator/prometheus`)

> Kubernetes deployment and platform provisioning are handled by Jenkins jobs configured on the server.

---

## Quick start (local)

### Prerequisites
- Git
- Java 17+
- Docker + Docker Compose

### Run
From the repo root:

```bash
docker compose up --build
```

Then open:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

---

## CI (Jenkins)

### Job: `taskhub-app-ci`
Trigger:
- Push to `develop`

Pipeline:
1. Checkout
2. Resolve version
3. `./gradlew clean test`
4. Build & push Docker image (`tsingh38/taskhub:<version>-<buildNumber>`)
5. Trivy scan gate (HIGH/CRITICAL)
6. Triggers the DEV deploy job with the computed image tag

Artifacts:
- JUnit test results
- `trivy-report.json`

---

## Running environments (server)

| Component | DEV | PROD |
|---|---|---|
| Swagger UI | http://51.158.200.80:30080/swagger-ui/index.html#/ | http://51.158.200.80:30081/swagger-ui/index.html#/ |
| Grafana | http://51.158.200.80:30030/ (env dropdown dev/prod) | same |
| Jenkins | http://51.158.200.80:8080/ | same |

---

## API summary

### Task fields
- `id` (string)
- `title` (string)
- `dueDate` (ISO-8601 timestamp)
- `status` (enum, default `OPEN`)

### Endpoints
- `POST /tasks` → `201 Created`
- `GET /tasks` → `200 OK` (paginated via Spring `Pageable`)
- `GET /tasks/{id}` → `200 OK` / `404`
- `PUT /tasks/{id}` → `200 OK` / `404`
- `DELETE /tasks/{id}` → `204 No Content` / `404`

---

## Testing

```bash
./gradlew clean test
```

---

## Observability (app-side)

- Metrics endpoint: `/actuator/prometheus`
- API timings via Micrometer
- Error counter: `api.errors.total` with `type=validation|not_found|server_error`

---

## Roadmap (post submission)
- OAuth2
- Async/event-driven processing
- Additional microservice
- DR hardening
