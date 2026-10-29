# Harbor contributor guide

## Project overview

Harbor is a self-hosted, backend-led PaaS prototype. The Spring Boot service manages users and projects, then deploys a project's Git repository by cloning it, building a Docker image, and starting a container. PostgreSQL stores users, projects, and deployment records. Docker labels are intended to let Traefik route to deployed containers, and deployment logs are streamed over Server-Sent Events (SSE).

The repository currently contains the backend and its deployment infrastructure; there is no frontend yet. Roadmap notes live in `dev_logs/`.

## Repository layout

- `backend/src/main/java/com/waffles/harbor/auth/`: registration, login, JWT authentication, and roles.
- `backend/src/main/java/com/waffles/harbor/user/`: user entity, persistence, and service.
- `backend/src/main/java/com/waffles/harbor/project/`: project API, ownership, persistence, and repository URL configuration.
- `backend/src/main/java/com/waffles/harbor/deployment/`: deployment records, lifecycle orchestration, and API.
- `backend/src/main/java/com/waffles/harbor/git/`: Git clone and commit metadata operations.
- `backend/src/main/java/com/waffles/harbor/docker/`: Docker image/container operations and Traefik labels.
- `backend/src/main/java/com/waffles/harbor/sse/`: streaming support for deployment logs.
- `backend/src/test/`: unit, repository, controller, and integration tests.
- `docker-compose.yml`: PostgreSQL, Harbor backend, and Traefik services/networks.
- `dev_logs/`: phase plans and design decisions.

## Stack and local development

- Java 25 and Spring Boot 4.1.0; Maven wrapper is in `backend/`.
- PostgreSQL is the application database. Docker and the Docker socket are used by the deployment engine.
- Copy `.env.example` to `.env` and provide the database credentials and JWT secret expected by Compose. Do not commit `.env`, credentials, or deployment secrets.
- Start the stack from the repository root with `docker compose up --build` after configuring `.env`.
- Run backend tests from `backend/` with `./mvnw test` (Windows: `mvnw.cmd test`). Tests that use Testcontainers need Docker available.

## API areas

- `/auth/register` and `/auth/login` provide authentication and JWT responses.
- `/projects` provides authenticated project listing/creation, project lookup, repository URL configuration, and deployment cleanup.
- `POST /deployments/{projectId}` creates a queued deployment record; `POST /deployments/{deploymentId}/execute` runs its Git/Docker work. `GET /deployments/{deploymentId}` returns one deployment, while `GET /deployments/project/{projectId}` returns project history.
- `/sse` is a small standalone SSE endpoint currently present in addition to deployment log streaming.

## Change guidance

- Keep the existing package-by-feature structure and use the service/repository boundaries already present.
- Project and deployment operations must enforce ownership using the authenticated `User`; do not expose another user's resources by UUID.
- Treat repository URLs and checked-out code as untrusted input. Preserve URL validation, workspace isolation, timeouts, resource limits, and cleanup behavior when changing deployment execution.
- Docker access is powerful: avoid adding arbitrary shell execution or exposing the Docker socket through application APIs. Changes to container networking, labels, and Compose networks must be kept consistent.
- Keep deployment status and persisted metadata in sync with Docker/Git side effects, including failure cleanup.
- Add or update focused tests for behavior changes, especially ownership, validation, deployment lifecycle, and failure paths.
- Keep phase plans in `dev_logs/` aligned with implemented functionality; clearly distinguish planned work from completed behavior.
- Do not reformat or revert unrelated user changes while working in the repository.

## Current implementation boundaries

Harbor has authentication, user-owned project management, repository URL configuration, persisted deployment records, Git cloning/commit identification, Docker image builds and container lifecycle operations, and SSE log streaming. The roadmap describes additional work such as production routing/TLS, public access, CI/CD, rollback, monitoring, persistent logs, and a dashboard; treat those as planned unless implemented in source.

Deployment currently expects a usable Dockerfile and an exposed image port. The deployment controller itself notes that non-`EXPOSE` Dockerfiles are not handled. Review the current Compose configuration and environment before changing filesystem paths or network names, since the backend, deployment containers, and Traefik share those assumptions.
