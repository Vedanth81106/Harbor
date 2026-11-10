# Harbor contributor guide

## Project overview

Harbor is a self-hosted, backend-led PaaS prototype. The Spring Boot service manages users and projects, then deploys a project's Git repository by cloning it, building a Docker image, and starting a container. PostgreSQL stores users, projects, and deployment records. Traefik routes traffic to deployed containers using Docker labels, and deployment container logs are streamed over Server-Sent Events (SSE).

The repository currently contains the backend and its deployment infrastructure; there is no frontend yet. Roadmap notes live in `dev_logs/`.

## Agent rules

- **Never create Git commits.** Do not run `git commit`, `git commit --amend`, `git rebase`, `git merge`, `git tag`, or any other command that creates a commit or rewrites history. Leave every change in the working tree, uncommitted, so the repository owner can review the diff.
- **Report changes instead of committing them.** At the end of every task, list the files changed, summarize what changed and why, and call out anything the owner should verify. The repository owner commits manually.
- Do not push, publish, or create releases. The repository owner is the only intended GitHub collaborator, so no agent may add commits under its own identity.
- Do not change Git author/committer configuration or bypass hooks.
- Read-only Git commands such as `git status`, `git diff`, `git log`, and `git show` are fine.

## Repository layout

- `backend/src/main/java/com/waffles/harbor/auth/`: registration, login, JWT authentication, and roles.
- `backend/src/main/java/com/waffles/harbor/user/`: user entity, persistence, and service.
- `backend/src/main/java/com/waffles/harbor/project/`: project API, ownership, persistence, and repository URL configuration.
- `backend/src/main/java/com/waffles/harbor/deployment/`: deployment records, lifecycle orchestration, recovery, and API.
- `backend/src/main/java/com/waffles/harbor/git/`: Git clone, URL validation, and commit metadata operations.
- `backend/src/main/java/com/waffles/harbor/docker/`: Docker client config, image/container operations, isolated networks, and Traefik labels.
- `backend/src/main/java/com/waffles/harbor/security/`: JWT filter/service and Spring Security configuration.
- `backend/src/main/java/com/waffles/harbor/sse/`: streaming support for deployment logs (plus a small demo endpoint).
- `backend/src/main/java/com/waffles/harbor/exception/`: domain exceptions and the global error handler.
- `backend/src/main/java/com/waffles/harbor/config/`: application-level configuration such as the SSE executor.
- `backend/src/test/`: unit, repository, controller, and integration tests.
- `backend/Dockerfile`: multi-stage build for the backend image.
- `docker-compose.yml`: PostgreSQL, Harbor backend, Traefik, and Cloudflare Tunnel services/networks.
- `dev_logs/`: phase plans and design decisions.

## Stack and local development

- Java 25 and Spring Boot 4.1.0; the Maven wrapper is in `backend/`.
- PostgreSQL is the application database (`spring.jpa.hibernate.ddl-auto=update`). Docker and the Docker socket are used by the deployment engine.
- The backend reads `JWT_SECRET_KEY`, `CONFIG_PATH`, and `DOMAIN_NAME` from the environment (see `backend/src/main/resources/application.properties`).
- Compose expects `DB_USER`, `DB_PWD`, `DB_NAME`, `JWT_SECRET_KEY`, `DOMAIN`, `CLOUDFLARE_TOKEN`, and `HARBOR_WORKSPACE`. Keep `.env.example` in sync with the variables Compose references. Do not commit `.env`, credentials, or deployment secrets.
- Compose mounts the host Docker socket and mounts the host directory from `HARBOR_WORKSPACE` (default `./tmp`) at `/harbor-workspace` inside the backend container. The backend's `CONFIG_PATH` is set to `/harbor-workspace`; do not hard-code machine-specific host paths in tracked files.
- Start the stack from the repository root with `docker compose up --build` after configuring `.env`.
- Build the backend directly from `backend/` with `./mvnw clean package`; run it with `./mvnw spring-boot:run` only after providing the required environment variables.
- Run backend tests from `backend/` with `./mvnw test` (Windows: `mvnw.cmd test`). Tests that use Testcontainers need Docker available.

## API areas

- `/auth/register` and `/auth/login` provide registration and JWT responses. Passwords are BCrypt-hashed; JWTs are signed with the Base64URL-decoded `JWT_SECRET_KEY` and currently expire after 240 hours.
- `/projects` provides authenticated project listing/creation, project lookup, repository URL configuration (`PUT /projects/{projectId}`), and deployment cleanup (`DELETE /projects/{projectId}/deployments`). Project names are unique per user.
- `POST /deployments/{projectId}` creates a queued deployment record; `POST /deployments/{deploymentId}/execute` runs its Git/Docker work. `GET /deployments/{deploymentId}` returns one deployment, `GET /deployments/project/{projectId}` returns project history, `DELETE /deployments/{deploymentId}` removes a deployment and its resources, and `GET /deployments/{deploymentId}/logs` streams container logs as SSE.
- `/sse` is a small standalone demo SSE endpoint currently present in addition to deployment log streaming.
- Security is stateless JWT bearer authentication. `/auth/**`, `/users/**`, `/health`, and `/error` are public; everything else requires authentication. CORS currently allows any origin pattern with credentials, which is acceptable only for the prototype.

## Deployment lifecycle

`DeploymentStatus` is `QUEUED`, `CLONING`, `BUILDING`, `STARTING`, `READY`, `FAILED`, or `STOPPED` (`STOPPED` is defined but not yet driven by an API). Execution is synchronous inside the `executeDeployment` request:

1. Validate the project belongs to the authenticated user and the deployment is `QUEUED`.
2. Clone the project's HTTPS repository into `${CONFIG_PATH}/{userId}/{projectId}/{deploymentId}`.
3. Build a Docker image tagged `{projectId}:{latestCommitHash}` from the checkout's `Dockerfile`.
4. Read the first exposed image port, create a container named `harbor-{deploymentId[0..8]}`, attach it to an isolated bridge network `net-{deploymentId[0..8]}`, and add Traefik labels routing `Host("harbor-{deploymentId[0..8]}.{DOMAIN_NAME}")` on the `web` entrypoint.
5. Connect Traefik to that per-deployment network and start the container.
6. Persist `containerId`, `commitHash`, `imageName`, and status changes as the work progresses.

Failure cleanup removes the container, disconnects/removes the network, removes the image when no other deployment references it, deletes the workspace directory, and marks the deployment `FAILED`. Deleting deployments follows the same cleanup path. `DeploymentRecoveryService` reconnects Traefik to networks for `READY` deployments on application startup.

Containers are currently limited to 512 MiB memory, 0.5 CPU, 100 PIDs, and `noRestart`. Builds expect a usable `Dockerfile` and an `EXPOSE`d port; the deployment controller has a TODO for non-`EXPOSE` Dockerfiles.

## Change guidance

- Keep the existing package-by-feature structure and use the service/repository boundaries already present.
- Project and deployment operations must enforce ownership using the authenticated `User`; do not expose another user's resources by UUID.
- Treat repository URLs and checked-out code as untrusted input. Preserve HTTPS-only validation, rejection of user info in URLs and of resolved private/loopback/link-local/multicast addresses, workspace isolation, timeouts, resource limits, and cleanup behavior when changing deployment execution.
- Docker access is powerful: avoid adding arbitrary shell execution or exposing the Docker socket through application APIs. Changes to container networking, labels, and Compose networks must be kept consistent with Traefik's `harbor-proxy`/per-deployment network assumptions.
- Keep deployment status and persisted metadata in sync with Docker/Git side effects, including partial-provisioning and failure cleanup. Save the container ID before starting it so cleanup can find it.
- Add or update focused tests for behavior changes, especially ownership, validation, deployment lifecycle, and failure paths.
- Keep phase plans in `dev_logs/` aligned with implemented functionality; clearly distinguish planned work from completed behavior.
- Do not reformat or revert unrelated user changes while working in the repository.

## Current implementation boundaries

Harbor has authentication, user-owned project management, repository URL configuration, persisted deployment records, Git cloning/commit identification, Docker image builds and container lifecycle operations, per-deployment network isolation, Traefik routing labels, startup routing recovery, and deployment container log streaming.

The roadmap describes additional work such as production routing/TLS, public access, CI/CD, rollback, monitoring, persistent logs, and a dashboard; treat those as planned unless implemented in source. Also note that deployment execution is request-driven rather than backed by a durable worker/queue, build logs are not streamed (only running container logs are), the `/sse` endpoint is a demo, and Compose contains machine-specific absolute paths.

Review the current Compose configuration and environment before changing filesystem paths or network names, since the backend, deployment containers, and Traefik share those assumptions.
