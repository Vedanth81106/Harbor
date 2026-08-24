# 2.1 — Docker Fundamentals & Docker Engine Integration

### Learn

* Docker images/*
* Containers
* Dockerfiles
* Docker Engine
* Docker CLI
* Container lifecycle
* Volumes
* Networks
* Docker Java/API integration

### Build

Harbor can communicate with Docker:

* Start a container
* Stop a container
* Remove a container
* Inspect a running container

### Goal

> Harbor can control Docker programmatically.

---

# 2.2 — Git Integration

### Learn

* Git repositories
* `git clone`
* Branches and commits
* Git process execution
* Temporary workspaces
* Handling external processes safely

### Build

```text
Repository URL
      ↓
    Harbor
      ↓
 Clone repository
      ↓
Local deployment workspace
```

### Goal

> Harbor can obtain application source code from a Git repository.

---

# 2.3 — Application Builds

### Learn

* Dockerfiles
* Docker build context
* Docker image tags
* Build arguments
* Build failures
* Build logs

### Build

```text
Git repository
      ↓
  Dockerfile
      ↓
 docker build
      ↓
 Harbor image
```

### Goal

> Harbor can turn a Git repository into a Docker image.

---

# 2.4 — Deployment Lifecycle

Now combine the previous pieces.

```text
Git
 ↓
Clone
 ↓
Build
 ↓
Image
 ↓
Container
```

### Learn

* Deployment states
* State transitions
* Failure handling
* Cleanup
* Idempotency

### Build

```text
QUEUED
   ↓
CLONING
   ↓
BUILDING
   ↓
STARTING
   ↓
RUNNING
```

With failure paths such as:

```text
BUILDING
   ↓
FAILED
```

### Goal

> Harbor can perform and track an actual deployment.

---

# 2.5 — Deployment Persistence

This is where the earlier `Deployment` entity becomes useful.

### Build

```text
Project
│
├── Deployment 1
├── Deployment 2
└── Deployment 3
```

Store things like:

* Deployment ID
* Project
* Status
* Commit/version
* Timestamps
* Image information
* Container information where appropriate

### Goal

> Harbor remembers what happened instead of treating deployments as temporary operations.

---

# 2.6 — Deployment Logs

### Learn

* `stdout` / `stderr`
* Process output
* Docker logs
* Log streaming
* Buffering

### Build

```text
Docker / Git / Build
        │
        ▼
      Harbor
        │
        ▼
 Deployment logs
        │
        ▼
      Client
```

### Goal

> A user can see what is happening during their deployment.

---

# 2.7 — Reliability & Security

This is an important one, and I wouldn't skip it.

Think about:

* Command injection
* Malicious repositories
* Filesystem isolation
* Docker socket security
* Process timeouts
* Build timeouts
* Disk exhaustion
* Container cleanup
* Resource limits
* Concurrent deployments
* Failed deployments
* Orphaned containers

### Goal

> Harbor doesn't just deploy applications — it handles things going wrong safely.
