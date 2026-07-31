# Harbor — Phase 1 Design Decisions

## Project Structure

```text
harbor/
├── backend/              # Spring Boot application
├── frontend/             # Frontend application
├── docker-compose.yml
├── README.md
└── docs/
```

The repository root represents the entire Harbor platform, while each subdirectory is an independent component.

---

## Technology Stack

### Backend

* Java
* Spring Boot
* Spring Web
* Spring Security
* Spring Data JPA
* PostgreSQL
* Lombok
* Bean Validation

### Development Environment

Everything runs through **Docker Compose**.

Containers:

* Spring Boot backend
* PostgreSQL database

---

## Domain Model

### User

Represents a Harbor user.

Fields:

* `id`
* `username`
* `email`
* `passwordHash`

Relationship:

* One user owns many projects.

### Project

Represents an application owned by a user.

Fields:

* `id`
* `name`
* `owner` (`User`)

Relationship:

* Each project belongs to exactly one user.

---

## Relationship Rules

* Projects reference users by **User ID**.
* `username` and `email` may change over time.
* `id` is the permanent identifier used for relationships.

---

## Deletion Strategy

For Phase 1:

* Deleting a user also deletes all owned projects (cascade delete).

Soft deletes and account recovery may be considered in future phases.

---

## Authentication

Authentication is implemented with **Spring Security**.

Features:

* User registration
* User login
* Protected endpoints

Passwords are never stored in plaintext—only password hashes are persisted.

---

## Phase 1 Scope

### Authentication

* Register
* Login
* Protect endpoints

### User Management

* Create users
* Persist users in PostgreSQL

### Project Management

* Create projects
* View owned projects
* Update projects
* Delete projects

Each authenticated user may only access and manage their own projects.

---

## Persistence

* PostgreSQL
* Spring Data JPA

---

## Testing

* Unit tests
* Integration tests
* Meaningful coverage for authentication and project management

---

## Design Principles

* Build only what the current phase requires.
* Avoid creating future entities until they have a concrete use case.
* Keep the domain model simple and evolve it incrementally.
* Use stable primary keys (`id`) instead of mutable fields (`username`, `email`) for relationships.
* Load related data only when it is needed.
