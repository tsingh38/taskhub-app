# TaskHub App

## Overview
TaskHub is a Spring Boot backend for managing tasks (CRUD). The list endpoint is paginated to behave well as data grows.

This repository focuses on the **application layer**:
- Spring Boot REST API + validation + consistent error handling
- Tests (Gradle)
- Docker image build (used by CI / release build)
- Observability via Actuator + Micrometer (`/actuator/prometheus`)

> Kubernetes platform provisioning and deployments are handled by Jenkins jobs on the server.

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
- Version: http://localhost:8080/version
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

---

## CI/CD (Jenkins)

### 1) DEV pipeline — `taskhub-app-ci`
**Trigger:** push to `develop`

**What it does:**
1. Checkout
2. Resolve app version from Gradle
3. Run tests: `./gradlew clean test`
4. Build & push Docker image to Docker Hub:
   - `tsingh38/taskhub:<version>-<buildNumber>`
5. Trivy scan gate (fails on **HIGH/CRITICAL**)
6. Triggers DEV deploy job with the computed image tag

**Artifacts:**
- JUnit test reports
- `trivy-report.json`

### 2) Release build — `taskhub-release-build`
**Trigger:** manual (intended for PROD release flow)

**Input:**
- `RELEASE_TAG` (example: `0.1.2`)

**What it does:**
1. Checkout the Git tag `refs/tags/<RELEASE_TAG>`
2. Build & test
3. Build & push Docker image:
   - `tsingh38/taskhub:<RELEASE_TAG>`
4. Trivy scan gate (fails on **HIGH/CRITICAL**)

> After the release image exists in Docker Hub, the PROD deploy job (in the infra repo) deploys it to the `prod` namespace.

---

## Running environments (server)

| Component | DEV | PROD |
|---|---|---|
| Swagger UI | http://51.158.200.80:30080/swagger-ui/index.html#/ | http://51.158.200.80:30081/swagger-ui/index.html#/ |
| Version endpoint | http://51.158.200.80:30080/version | http://51.158.200.80:30081/version |
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
- `GET /version` → returns the running app version (from Spring Boot build info)
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
