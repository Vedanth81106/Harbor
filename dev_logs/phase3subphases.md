# Harbor Phase 3 Implement it in Harbor Reverse Proxy & Application Routing

## Objectives

Introduce a reverse proxy so deployed applications can be accessed through meaningful hostnames instead of dynamically exposed host ports.

### Concepts to Learn

- Reverse proxy
- Forward proxy
- Nginx vs Caddy
- HTTP upstreams
- Hostnames
- Ports
- TLS termination
- HTTP `Host` headers
- Routing requests to backend services
- Why applications should not individually expose public ports

### Target Architecture

```text
Browser
   https://myapp.example.com
  
  
Application Container
  
container:8080
```

### Implementation

1. Configure a reverse proxy manually.
2. Route a hostname to a Dockerized application.
3. Verify that the application is accessible through the hostname.
4. Integrate the routing configuration with Harbor.
5. Make Harbor responsible for creating or updating application routing.

### Expected Result

A deployed Harbor application is accessible through a hostname such as:

```text
https://myapp.example.com
```

instead of:

```text
http://server-ip:32781
```

---

# 3.2
container:8080
```

The application is exposed through a port on the host.

#### Docker Network

```text
Reverse Proxy
      HTTP
     
                              
                             :8080
```

Eventually:

```text
                harbor-network
                      
                      Cloudflare Tunnel / Public Access

## Objectives

Provide public access to Harbor deployments without requiring arbitrary inbound ports to be exposed on the host machine.

### Concepts to Learn

- NAT
- Inbound ports
- Firewalls
- DNS
- TLS
- Cloudflare Tunnel
- Outbound tunnels
- Public vs private networking
- How tunnels avoid direct inbound connectivity

### Target Architecture

```text
Internet
  
Cloudflare Tunnel
  
Docker Network
   GitHub Actions / CI/CD

## Objectives

Introduce automated deployment triggered by changes to the application's Git repository.

### Concepts to Learn

- Continuous Integration (CI)
- Continuous Delivery (CD)
- GitHub Actions
- Workflows
- Jobs
- Steps
- Secrets
- Webhooks
- Deployment triggers
- Commit SHA
- Automated deployment pipelines

### Target Architecture

```text
Developer
  
GitHub
  
Harbor Deployment API
  
Application Deployment
```

### Implementation

Start with a simple deployment-trigger workflow:

```text
git push
  
Call Harbor deployment endpoint
   Deployment Versioning & Rollbacks

## Objectives

Extend the Phase 2 deployment model to support deployment history, immutable artifacts, and reliable rollback.

### Target Deployment Model

```text
Project
Deployment A image A
commit def Deployment C image C
```

### Concepts to Learn

- Immutable artifacts
- Deployment versioning
- Deployment history
- Rollbacks
- Artifact identity
- Commit SHA
- Blue/green deployment concepts

### Deployment Flow

```text
v1

v3
```

Rollback:

```text
v3

v2
```

### Implementation

1. Associate every deployment with a unique version or commit SHA.
2. Store the corresponding container image or artifact reference.
3. Preserve deployment history.
4. Implement a rollback operation.
5. Roll back by redeploying the previously built artifact.
6. Avoid rebuilding an old Git commit unnecessarily during rollback.
7. Update deployment status appropriately.

### Expected Result

Harbor can reliably return an application to a previous deployment version.

---

# 3.6
Health Check

HEALTHY / UNHEALTHY / STOPPED
```

### Metrics

Harbor should eventually expose basic runtime information such as:

```text
CPU:       23%
Memory:    184 MB
Uptime:    2h 41m
Restarts:  0
```

### Implementation

1. Add application health checks.
2. Determine how Docker reports container health.
3. Integrate health information into Harbor.
4. Expose application status through the backend API.
5. Collect basic container resource metrics.
6. Track restart counts and uptime.
7. Display health state separately from deployment state.

### Expected Result

Harbor can determine whether a deployed application is healthy and provide basic runtime information.

---

# 3.7
Docker Logs

Persistent Storage

Dashboard
```

### API Example

```http
GET /deployments/{id}/logs
```

### Implementation

1. Capture container logs.
2. Determine the appropriate persistence mechanism.
3. Associate logs with deployments.
4. Implement historical log retrieval.
5. Add filtering or querying where appropriate.
6. Define a retention strategy.
7. Add log rotation to prevent unbounded storage growth.
8. Preserve the existing live SSE log functionality.

### Expected Result

Deployment logs remain accessible after the live SSE connection ends.

---

# 3.8 Current deployment
Health
CPU
Deployment history
Basic Deployment Information

```text
Project
Status
URL
Current version
```

#### Step 2 Health & Metrics

```text
Health
CPU
Memory
Uptime
Restarts
```

#### Step 4 Rollback

```text
Deployment history
       
Rollback
```

### Expected Result

A user can operate Harbor as a small PaaS without needing to manually inspect Docker containers or infrastructure.

---

# Overall Dependency Chain

```text
3.1 Reverse Proxy
       
3.3 Public Access
       
3.5 Versions / Rollbacks
       
3.7 Logs
        Make It Reachable

```text
3.1 3.3
```

Focus:

- Reverse proxy
- Internal Docker networking
- Public access
- Application URLs

---

## Milestone 2 Make It Reliable

```text
3.5 3.7
```

Focus:

- Deployment versions
- Rollbacks
- Health checks
- Runtime monitoring
- Persistent logs

---

## Milestone 4

GitHub / CI

Harbor
Build / Deploy
Versioning
Rollback
Health Checks
Monitoring
Logging

Reverse Proxy

Cloudflare Tunnel

Public Application
```

The resulting system should provide the core characteristics of a small PaaS:

- Automated deployments
- Public application URLs
- Container-based application isolation
- Deployment versioning
- Rollbacks
- Health monitoring
- Runtime metrics
- Persistent logs
- Centralized deployment management