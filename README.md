# Harbor

Harbor is a self-hosted Platform-as-a-Service (PaaS) prototype with a Spring Boot backend and a Next.js web console. It lets a user register, create projects, attach a Git repository, and deploy an application to a Linux server. Harbor clones the repository, builds a Docker image, starts a container, and routes traffic to it through Traefik.

> **Note:** this repository contains the backend service, its deployment infrastructure, and the frontend console.

## Features

- Next.js web console for projects, deploys, deployment status, and live logs.
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
                    Traefik ──────────────┐
                       |                  |
            Host(harbor.<domain>)   per-deployment networks (net-<id>)
                       |                  |
              Harbor frontend      deployed containers
                       |
             private harbor-network
                       |
              Harbor backend ────────► PostgreSQL
                       |
                       └──► Git repository (HTTPS clone)
                              Docker API (socket) ──► deployed containers
```

Only the frontend is a public Harbor service. The browser talks to the frontend, and the frontend's Next.js server proxies `/api/*` to the backend over `harbor-network`; the backend is not published on a host port and has no Traefik route. The backend is a Spring Boot service. PostgreSQL stores users, projects, and deployment records. Docker is accessed through the host Docker socket, and Traefik discovers deployed containers through Docker labels.

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
| Frontend | Next.js 16 (App Router) + React 19 + TypeScript + Tailwind CSS |
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
frontend/
  app/            Landing page, authenticated console, and /api gateway
  components/     UI, auth provider, dialogs, status pills
  lib/            Typed API client, SSE parser, deployment URL helper
  Dockerfile      Multi-stage frontend image (production `next start`)
docker-compose.yml  PostgreSQL, backend, frontend, Traefik, Cloudflare Tunnel
dev_logs/           Development notes and design documents
```

## Prerequisites

- Docker Engine with the Docker socket available.
- Docker Compose v2.
- For local (non-container) backend development: JDK 25 and the Maven wrapper.
- For local (non-container) frontend development: Node.js 22 and npm.
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
| `NEXT_PUBLIC_SITE_URL` | Public URL of the frontend, used for metadata. Defaults to `http://localhost:3000`. |
| `NEXT_PUBLIC_DEPLOY_DOMAIN` | Base domain for deployed-app links. Defaults to `DOMAIN`. |
| `FRONTEND_PORT` | Host port that publishes the frontend container. Defaults to `3000`. |
| `FRONTEND_HOST` | Traefik `Host(...)` for the frontend. Defaults to `harbor.<DOMAIN>`. |

The frontend container reaches the backend at `INTERNAL_API_URL`, which Compose sets to `http://spring:8080` on `harbor-network`. It is not user-configurable and never exposed outside the stack. In local development (no Compose), the same proxy defaults to `http://localhost:8080`.

`NEXT_PUBLIC_*` values are inlined by Next.js at build time. After changing them, rebuild the frontend image:

```bash
docker compose build frontend
```

Example `.env` (replace the values):

```dotenv
DB_USER=harbor
DB_PWD=change-me
DB_NAME=harbor
JWT_SECRET_KEY=replace-with-a-base64url-secret
DOMAIN=example.com
CLOUDFLARE_TOKEN=replace-with-your-tunnel-token
HARBOR_WORKSPACE=./tmp
NEXT_PUBLIC_SITE_URL=http://localhost:3000
NEXT_PUBLIC_DEPLOY_DOMAIN=
FRONTEND_PORT=3000
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

4. The frontend is available at `http://localhost:3000`. That is the only Harbor port published by Compose; the backend is reachable only by the frontend container.
5. PostgreSQL is reachable on the host at `127.0.0.1:5433` (mapped to `5432` inside the network).
6. Traefik listens on port `80` inside the `harbor-proxy` network and is reached by Cloudflare Tunnel; it is not published directly on a host port by this Compose file.

The frontend image runs a production build (`npm run build`, then `next start`), not the dev server.

### How requests reach the backend

The browser only ever talks to the frontend origin. Requests to `/api/*` are handled by a Next.js route handler (`frontend/app/api/[...path]/route.ts`) that forwards them to `INTERNAL_API_URL`, which Compose sets to `http://spring:8080` on `harbor-network`. This has a few consequences:

- The backend has no host port, no Traefik labels, and no public hostname. Nothing outside the stack can call it directly.
- Authentication, logs (including SSE streaming), uploads, and error responses pass through unchanged; the bearer token is forwarded as-is.
- CORS is irrelevant in the Compose setup because every request is same-origin.

`FRONTEND_HOST` only changes which `Host` header Traefik accepts for the frontend, so it does nothing when you browse to the published port. It matters for Cloudflare Tunnel, which reaches the frontend through Traefik.

### Public access through Cloudflare

Point the tunnel at `harbor.<domain>` for the frontend and add a wildcard route for deployed apps (`*.<domain>`), since Traefik routes those by hostname. Keep `NEXT_PUBLIC_SITE_URL` and `FRONTEND_HOST` on the public hostname, and rebuild because `NEXT_PUBLIC_*` values are compiled into the browser bundle:

```dotenv
NEXT_PUBLIC_SITE_URL=https://harbor.example.com
FRONTEND_HOST=harbor.example.com
```

```bash
docker compose build frontend
docker compose up -d frontend
```

Services started by Compose:

- `db` — PostgreSQL 18 with a persistent `harbor_db_data` volume.
- `spring` — the Harbor backend image, built from `backend/Dockerfile`, with no published host port.
- `frontend` — the Harbor console image, built from `frontend/Dockerfile`, published on `${FRONTEND_PORT:-3000}`; the only public Harbor service.
- `traefik` — Docker provider with `exposedByDefault=false`, using the `harbor-proxy` network.
- `cloudflared` — Cloudflare Tunnel client.

Networks declared by Compose: `harbor-network` (frontend ↔ backend ↔ database) and `harbor-proxy` (Traefik ↔ frontend and the networks Traefik joins). Per-deployment `net-<id>` networks are created dynamically at deployment time.

## Local backend development

From `backend/`:

```bash
./mvnw clean package      # build
./mvnw spring-boot:run    # run (requires the environment variables and a database)
```

On Windows, use `mvnw.cmd`. Provide at least `JWT_SECRET_KEY`, `CONFIG_PATH`, `DOMAIN_NAME`, and the datasource variables before starting. The deployment engine also expects a working Docker socket.

## Local frontend development

From `frontend/`:

```bash
npm install
npm run dev       # http://localhost:3000
npm run build
npm run start     # production server, used by the Docker image
npm run typecheck
```

The site calls its own `/api` routes; the Next.js server (dev or production) forwards them to `INTERNAL_API_URL`, which defaults to `http://localhost:8080`. Keep the backend running locally, or start it from `backend/` with `./mvnw spring-boot:run`. Set `INTERNAL_API_URL` if the backend lives elsewhere. `NEXT_PUBLIC_API_URL` exists only as an escape hatch to bypass the proxy and call a different origin directly.

When you run the stack with Compose, the `frontend` service builds this app for production and serves it with `next start`; `npm run dev` is only for local development.

Set `NEXT_PUBLIC_DEPLOY_DOMAIN` to the backend's `DOMAIN_NAME` so deployment pages can link to live apps at `https://harbor-<first 8 chars of deploymentId>.<domain>`. HTTPS is assumed; prefix the value with `http://` for a local Traefik that only serves plain HTTP. Without it, the console still works but hides the public link.

Signed-in routes:

- `/dashboard` — list and create projects.
- `/projects/[projectId]` — set the repository URL, deploy, and review deployment history.
- `/deployments/[deploymentId]` — status, public URL, container logs, delete.

Authentication is stored in `localStorage` and sent as a bearer token. Signed-out visitors to a console route are sent to the landing page with the login dialog and returned to their original route after signing in.

## API

All routes except the public ones listed below require an `Authorization: Bearer <token>` header. Project and deployment operations are scoped to the authenticated user.

In the Compose stack, the backend is not reachable from outside. The frontend exposes the same paths under `/api` (for example `POST /api/auth/login`), and its server forwards them over the internal network. Paths below are relative to the backend root.

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

The backend is not published or routed publicly: it sits on `harbor-network` and is reachable only from the frontend container, which forwards `/api/*`. Keep it that way; the frontend's proxy route is the single public entrypoint for the control plane. The backend's permissive CORS configuration no longer matters for the Compose stack, but it should still be tightened if the API is ever exposed directly.

## License

MIT — see [LICENSE](LICENSE).
