# Harbor

Harbor is a self-hosted Platform-as-a-Service (PaaS) prototype with a Spring Boot backend. It lets a user register, create projects, attach a Git repository, and deploy an application to a Linux server. Harbor clones the repository, builds a Docker image, starts a container, and routes traffic to it through Traefik.

> **Note:** this repository currently contains the backend service and its deployment infrastructure.

## Features

- Username/email registration and login with BCrypt-hashed passwords.
- Stateless JWT bearer authentication.
- User-owned projects with per-user unique names.
- Per-project HTTPS Git repository configuration.
- Persistent deployment records with lifecycle status tracking.
- Git clone and latest-commit identification using JGit.
- Docker image builds from the repository's `Dockerfile`.
- Isolated per-deployment Docker networks.
- Traefik routing driven by Docker labels.
- Running-container logs streamed over Server-Sent Events (SSE).
- Startup recovery that reconnects Traefik to `READY` deployment networks.
- Resource limits and failure cleanup for deployed containers.

## Architecture

```text
                    Internet
                       |
              Cloudflare Tunnel
                       |
                    Traefik
                       |
        per-deployment network (net-<id>)
                       |
              deployed container
                       ^
                       | Docker API (socket)
                       |
   Client ───────► Harbor backend ───────► PostgreSQL
                       |
                       └──► Git repository (HTTPS clone)
```

The backend is a Spring Boot service. PostgreSQL stores users, projects, and deployment records. Docker is accessed through the host Docker socket, and Traefik discovers deployed containers through Docker labels.

### Deployment flow

`DeploymentStatus` moves through `QUEUED` → `CLONING` → `BUILDING` → `STARTING` → `READY`, or `FAILED` on error.

Execution currently happens synchronously inside the `POST /deployments/{deploymentId}/execute` request:

1. Verify the project belongs to the authenticated user and the deployment is `QUEUED`.
2. Clone the project's HTTPS repository into `${CONFIG_PATH}/{userId}/{projectId}/{deploymentId}`.
3. Build a Docker image tagged `{projectId}:{latestCommitHash}` from the checkout's `Dockerfile`.
4. Read the first `EXPOSE`d image port, create a container named `harbor-{deploymentId[0..8]}`, attach it to the per-deployment bridge network `net-{deploymentId[0..8]}`, and add Traefik labels routing `Host("harbor-{deploymentId[0..8]}.{DOMAIN_NAME}")` on the `web` entrypoint.
5. Connect Traefik to that network and start the container.
6. Persist `containerId`, `commitHash`, `imageName`, and status changes as the work progresses.

Containers are limited to 512 MiB memory, 0.5 CPU, 100 PIDs, and a `noRestart` policy. Failure cleanup removes the container, disconnects/removes the network, removes the image when no other deployment references it, deletes the workspace directory, and marks the deployment `FAILED`. Deleting a deployment follows the same cleanup path.

## Tech stack

| Area | Technology |
|---|---|
| Language / runtime | Java 25 |
| Framework | Spring Boot 4.1.0 (Web MVC, Security, Data JPA, Validation) |
| Database | PostgreSQL 18 (Compose image) |
| Authentication | Spring Security + JJWT 0.12.6 |
| Git | Eclipse JGit |
| Docker | docker-java 3.7.1 |
| Proxy | Traefik v3.6 |
| Ingress | Cloudflare Tunnel |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers |
| Build | Maven wrapper (`backend/mvnw`) |

## Repository layout

```text
backend/
  src/main/java/com/waffles/harbor/
    auth/         Registration, login, JWT responses, roles
    user/         User entity, repository, service
    project/      Project API, ownership, persistence
    deployment/   Deployment records, lifecycle, recovery, API
    git/          Clone, URL validation, commit metadata
    docker/       Docker client config, images, containers, networks, labels
    security/     JWT filter/service and Spring Security config
    sse/          Deployment log streaming and the demo SSE endpoint
    exception/    Domain exceptions and global error handling
    config/       Application configuration (SSE executor)
  src/test/       Unit, repository, controller, and integration tests
  Dockerfile      Multi-stage backend image
docker-compose.yml  PostgreSQL, backend, Traefik, Cloudflare Tunnel
dev_logs/           Development notes and design documents
```

## Prerequisites

- Docker Engine with the Docker socket available.
- Docker Compose v2.
- For local (non-container) backend development: JDK 25 and the Maven wrapper.
- A domain and a Cloudflare Tunnel token for public ingress (optional for local work).
- Docker available if you run the Testcontainers-based tests.

## Configuration

The backend reads these properties (see `backend/src/main/resources/application.properties`):

| Variable | Purpose |
|---|---|
| `JWT_SECRET_KEY` | Base64URL-encoded HMAC secret used to sign JWTs. |
| `CONFIG_PATH` | Root directory for cloned repositories/workspaces. Compose sets this to `/harbor-workspace`. |
| `DOMAIN_NAME` | Base domain used in Traefik `Host(...)` rules. |
| `SPRING_DATASOURCE_URL` | JDBC URL (provided by Compose). |
| `SPRING_DATASOURCE_USERNAME` | Database user (provided by Compose). |
| `SPRING_DATASOURCE_PASSWORD` | Database password (provided by Compose). |

Docker Compose reads these variables from `.env` at the repository root:

| Variable | Purpose |
|---|---|
| `DB_USER` | PostgreSQL user. |
| `DB_PWD` | PostgreSQL password. |
| `DB_NAME` | PostgreSQL database name. |
| `JWT_SECRET_KEY` | JWT signing secret (Base64URL). |
| `DOMAIN` | Base domain passed to the backend as `DOMAIN_NAME`. |
| `CLOUDFLARE_TOKEN` | Cloudflare Tunnel token. |
| `HARBOR_WORKSPACE` | Host directory mounted into the backend at `/harbor-workspace`. Defaults to `./tmp`. |

Example `.env` (replace the values):

```dotenv
DB_USER=harbor
DB_PWD=change-me
DB_NAME=harbor
JWT_SECRET_KEY=replace-with-a-base64url-secret
DOMAIN=example.com
CLOUDFLARE_TOKEN=replace-with-your-tunnel-token
HARBOR_WORKSPACE=./tmp
```

Generate a suitable JWT secret with:

```bash
openssl rand -base64 64 | tr '+/' '-_' | tr -d '='
```

> `.env` is ignored by Git. Never commit credentials or deployment secrets.

## Quick start with Docker Compose

1. Create `.env` in the repository root using the variables listed above.
2. Set `HARBOR_WORKSPACE` in `.env` to the host directory Harbor should use for cloned repositories (it defaults to `./tmp`). This directory is mounted into the backend at `/harbor-workspace`.
3. Build and start the stack:

   ```bash
   docker compose up --build
   ```

4. The backend is available at `http://localhost:8080`.
5. PostgreSQL is reachable on the host at `127.0.0.1:5433` (mapped to `5432` inside the network).
6. Traefik listens on port `80` inside the `harbor-proxy` network and is reached by Cloudflare Tunnel; it is not published directly on a host port by this Compose file.

Services started by Compose:

- `db` — PostgreSQL 18 with a persistent `harbor_db_data` volume.
- `spring` — the Harbor backend image, built from `backend/Dockerfile`.
- `traefik` — Docker provider with `exposedByDefault=false`, using the `harbor-proxy` network.
- `cloudflared` — Cloudflare Tunnel client.

Networks declared by Compose: `harbor-network` (backend ↔ database) and `harbor-proxy` (Traefik ↔ public edge and the networks Traefik joins). Per-deployment `net-<id>` networks are created dynamically at deployment time.

## Local backend development

From `backend/`:

```bash
./mvnw clean package      # build
./mvnw spring-boot:run    # run (requires the environment variables and a database)
```

On Windows, use `mvnw.cmd`. Provide at least `JWT_SECRET_KEY`, `CONFIG_PATH`, `DOMAIN_NAME`, and the datasource variables before starting. The deployment engine also expects a working Docker socket.

## API

All routes except the public ones listed below require an `Authorization: Bearer <token>` header. Project and deployment operations are scoped to the authenticated user.

### Authentication

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/register` | Register a user and return a JWT. |
| `POST` | `/auth/login` | Authenticate and return a JWT. |

### Projects

| Method | Path | Description |
|---|---|---|
| `GET` | `/projects` | List the current user's projects. |
| `POST` | `/projects` | Create a project. Names are unique per user. |
| `GET` | `/projects/{projectId}` | Get one owned project. |
| `PUT` | `/projects/{projectId}` | Set the project's HTTPS repository URL. |
| `DELETE` | `/projects/{projectId}/deployments` | Delete all deployments and related resources for a project. |

### Deployments

| Method | Path | Description |
|---|---|---|
| `POST` | `/deployments/{projectId}` | Create a `QUEUED` deployment record. |
| `POST` | `/deployments/{deploymentId}/execute` | Run the Git/Docker deployment work. |
| `GET` | `/deployments/{deploymentId}` | Get one owned deployment. |
| `GET` | `/deployments/project/{projectId}` | List a project's deployment history. |
| `DELETE` | `/deployments/{deploymentId}` | Delete a deployment and its Docker/workspace resources. |
| `GET` | `/deployments/{deploymentId}/logs` | Stream the running container's logs as SSE. |

### Demo SSE

| Method | Path | Description |
|---|---|---|
| `GET` | `/sse` | Minimal demo endpoint that emits `hi` and completes. |

### Public routes

The security configuration permits `/auth/**`, `/users/**`, `/health`, and `/error`; all other routes require authentication.

## Testing

From `backend/`:

```bash
./mvnw test
```

The suite includes unit tests (JUnit 5 + Mockito), MVC tests (`@WebMvcTest` + MockMvc), repository tests, and integration tests. Testcontainers-based tests require a working Docker daemon.

## Security notes

Harbor treats repository URLs and checked-out code as untrusted input. The current implementation enforces HTTPS-only repository URLs, rejects credentials embedded in URLs, and rejects URLs that resolve to private, loopback, link-local, or multicast addresses. Workspaces are isolated per user/project/deployment.

Docker access is powerful: the backend mounts the host Docker socket, so the service should be treated as trusted infrastructure. Do not expose the Docker socket through application APIs, and avoid adding arbitrary shell execution.

CORS currently allows any origin pattern with credentials. Restrict allowed origins before exposing Harbor to untrusted clients.

## License

MIT — see [LICENSE](LICENSE).
