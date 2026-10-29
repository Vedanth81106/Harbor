# Harbor Phase 3 — Production Deployment Infrastructure

## 3.1 Reverse Proxy & Application Routing

### Learn

- Reverse proxies
- Nginx vs. Caddy
- HTTP-to-upstream routing
- Ports and hostnames
- TLS termination
- Domain and subdomain routing
- Why every container port should not be exposed directly

### Build

```text
Internet
   ↓
Nginx / Caddy
   ↓
Harbor-managed container
```

Harbor should be able to route a request to the appropriate application:

```text
myproject.example.com
        ↓
container:32781
```

### Goal

Applications deployed by Harbor become accessible through a proper HTTP endpoint instead of requiring users to access randomly assigned Docker ports.

---

## 3.2 Deployment Networking

Deployment networking is a separate subphase rather than a concern hidden inside reverse-proxy configuration.

### Learn

- Docker networks
- Container-to-container communication
- Bridge networks
- Network isolation
- Service discovery
- Host networking vs. container networking

### Build

```text
             harbor-network
                    │
          ┌─────────┴─────────┐
          ↓                   ↓
    Harbor backend       App container
                                ↑
                                │
                           Nginx / Caddy
```

Instead of relying entirely on this pattern:

```text
host:randomPort → container:port
```

Harbor should manage Docker networking in a deliberate and controlled way.

### Goal

Harbor-managed applications can communicate through controlled Docker networks, with appropriate isolation between services.

---

## 3.3 Public Access with Cloudflare Tunnel

This subphase makes locally deployed applications reachable from the public internet.

### Learn

- Tunnels
- NAT and firewalls
- Cloudflare Tunnel
- DNS
- HTTPS
- Why directly exposing ports is undesirable

### Build

```text
Internet
   ↓
Cloudflare
   ↓
Cloudflare Tunnel
   ↓
Nginx / Caddy
   ↓
Harbor-managed app container
```

### Goal

A deployed application can be accessed publicly without opening inbound ports on the host machine or server.

This stage also brings Harbor's earlier Docker security decisions into the public-access architecture.

---

## 3.4 Automated Builds & Deployments with GitHub Actions

This subphase moves Harbor closer to functioning as a complete Platform as a Service (PaaS), rather than only as a deployment API.

### Learn

- CI/CD
- GitHub Actions
- Webhooks
- Build pipelines
- Deployment triggers
- Secrets management
- Commit-based deployments
- Build, test, and deploy stages

### Build — Initial Workflow

```text
git push
   ↓
GitHub Actions
   ↓
Build / Test
   ↓
Trigger Harbor
   ↓
Harbor clones the selected commit
   ↓
Docker build
   ↓
Container
```

### Build — Later Workflow

```text
git push
   ↓
GitHub
   ↓
Webhook / GitHub Actions
   ↓
Harbor
   ↓
Deployment
```

### Goal

A developer can push code to a repository and have Harbor automatically build and deploy it.

---

## 3.5 Deployment Versioning & Rollbacks

Deployment versioning and rollback support build on Harbor's existing deployment records and commit hashes.

### Learn

- Immutable deployments
- Deployment versions
- Rollbacks
- Blue-green deployment concepts
- Tracking previous deployments

### Build

```text
v1 → Deployment 1
v2 → Deployment 2
v3 → Deployment 3

If v3 fails:
rollback
   ↓
v2
```

Harbor already persists deployment information such as:

```text
Deployment
├── commitHash
├── imageName
├── containerId
└── status
```

This provides a natural foundation for tracking deployment history and restoring a previous working version.

### Goal

Harbor can deploy a specific version of an application and roll back to an earlier deployment when necessary.

---

## 3.6 Monitoring & Health Checks

Harbor should be able to determine whether an application is functioning properly, not merely whether its container started.

### Learn

- Liveness vs. readiness
- Docker health checks
- CPU and memory metrics
- Container states
- Uptime
- Application health endpoints
- Basic metrics

### Build

```text
Deployment
    ↓
Container
    ↓
Health check
    ↓
┌──────────────┐
│ HEALTHY      │
│ UNHEALTHY    │
│ STOPPED      │
└──────────────┘
```

The deployment dashboard could eventually show:

```text
CPU:       32%
Memory:    184 MB
Status:    Healthy
Uptime:    2h 41m
Restarts:  0
```

### Goal

Harbor can report whether an application is healthy and provide useful operational information about its running container.

---

## 3.7 Centralized Logging

Harbor already supports live container logs through Server-Sent Events (SSE) in Phase 2. This subphase expands that capability into persistent deployment and application logging.

### Learn

- Structured logs
- Log persistence
- Log rotation
- `stdout` and `stderr`
- Log retention
- Querying and filtering logs

### Build

```text
Container
   ↓
Docker logs
   ↓
Harbor
   ↓
Log storage
   ↓
Dashboard
```

A historical logs endpoint could look like:

```http
GET /deployments/{id}/logs
```

This endpoint would allow developers to inspect stored logs after a container has stopped, rather than relying only on a live stream.

### Goal

Developers can inspect deployment and application logs, including historical logs from stopped containers.

---

## 3.8 Production Deployment Dashboard

Dashboard work is grouped into this subphase so that UI improvements do not need to be mixed into every backend subphase.

### Build

A production-style deployment view could include:

```text
┌──────────────────────────────────────┐
│ my-project                           │
│                                      │
│ Status: ● Healthy                    │
│ Version: a81f29c                     │
│ URL: https://my-project.example.com  │
│                                      │
│ CPU       23%                        │
│ Memory   184 MB                      │
│                                      │
│ Deployments                          │
│ ───────────────────────────────────  │
│ a81f29c    Healthy    5 min ago      │
│ 72bc812    Stopped    2 hours ago    │
│ 61ad920    Failed     yesterday      │
└──────────────────────────────────────┘
```

### Goal

The dashboard presents the infrastructure and deployment capabilities built throughout Phase 3 as a cohesive PaaS experience.

---

## Phase 3 Roadmap

1. **3.1 — Reverse Proxy & Routing**
2. **3.2 — Docker Deployment Networking**
3. **3.3 — Cloudflare Tunnel / Public Access**
4. **3.4 — GitHub Actions / CI/CD**
5. **3.5 — Deployment Versioning & Rollbacks**
6. **3.6 — Monitoring & Health Checks**
7. **3.7 — Centralized Logging**
8. **3.8 — Production Deployment Dashboard**

## Overall Progression

### Phase 2

```text
Git
 ↓
Build
 ↓
Docker
 ↓
Container
 ↓
Logs
```

### Phase 3

```text
Container
 ↓
Network
 ↓
Reverse Proxy
 ↓
Public URL
 ↓
CI/CD
 ↓
Versions / Rollbacks
 ↓
Monitoring
 ↓
Logs
 ↓
Dashboard
```

Phase 3 takes Harbor from creating and running containers toward managing the networking, accessibility, deployment lifecycle, observability, and user experience expected of a production-style PaaS.
