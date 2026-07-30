# 📋 Task Manager API

![CI](https://github.com/ssergionp/task-manager-api/actions/workflows/ci.yml/badge.svg)

A REST API built with **Java + Spring Boot** for task management, featuring a complete CRUD, JWT authentication with refresh tokens, Google OAuth2 login, role-based authorization, automated tests, and a production deployment.

🔗 **Live API:** [https://task-manager-api-vcu3.onrender.com](https://task-manager-api-vcu3.onrender.com)
📖 **Interactive documentation (Swagger):** [https://task-manager-api-vcu3.onrender.com/swagger-ui.html](https://task-manager-api-vcu3.onrender.com/swagger-ui.html)

> ⚠️ The service runs on Render's free tier, which spins down after 15 minutes of inactivity. The first request after that period may take 30-60 seconds to respond -- this is expected, not a bug.

🇧🇷 [Versao em portugues](./README.md)

---

## 🚀 Tech stack

- **Java 25**
- **Spring Boot 4.1** (Spring Web, Spring Data JPA, Validation, DevTools)
- **Spring Security** (authentication and authorization)
- **JWT (JJWT)** (stateless access tokens)
- **Refresh Token** (session renewal without re-login)
- **Spring Security OAuth2 Client** (social login with Google)
- **PostgreSQL** (relational database in production, via [Neon](https://neon.tech))
- **H2 Database** (in-memory database, used in automated tests)
- **Flyway** (versioned database migrations)
- **Hibernate / JPA** (object-relational mapping)
- **Lombok** (boilerplate reduction)
- **JUnit 5 + Mockito** (unit tests)
- **Spring Test / MockMvc** (integration tests)
- **Springdoc OpenAPI** (Swagger documentation, with Bearer Token support)
- **Spring Boot Actuator** (observability and health-check)
- **Docker & Docker Compose** (containerization of the app and database)
- **GitHub Actions** (continuous integration -- automated tests on every push)
- **Maven** (dependency management and build)
- **Render** (application hosting)

---

## 🏗️ Architecture

The project follows a layered architecture with clear separation of concerns:

```
com.ssergionp.taskmanagerapi
├── controller     → REST endpoints (HTTP entry layer)
├── service        → Business logic
├── repository     → Data access (Spring Data JPA)
├── model          → JPA entities (Task, User, RefreshToken, ...)
├── dto            → Data transfer objects (Request/Response)
├── security       → JWT, OAuth2, filters and auth services
├── config         → Security and application configuration
└── exception      → Global exception handling
```

**Why this separation matters:** the API never exposes JPA entities directly -- every piece of data going in or out passes through DTOs (`TaskRequestDTO` / `TaskResponseDTO`), which prevents coupling between the database model and the API contract.

---

## ✅ Features

- Full CRUD for tasks (create, list, get by id, update, delete)
- Filtering tasks by status (`TODO`, `IN_PROGRESS`, `DONE`), with pagination
- Input validation (required title, dates cannot be in the past, etc.)
- Global exception handling with standardized responses (`status`, `message`, `timestamp`)
- **Authentication via JWT** (registration and login)
- **Refresh token flow** (short-lived access token + long-lived, revocable refresh token)
- **Google OAuth2 login** (social sign-in reusing the same token system)
- **Ownership isolation** -- each user only sees and manages their own tasks
- **Role-based authorization** -- `ADMIN` users can access a special endpoint listing tasks from all users
- Passwords stored with **BCrypt** hashing, never in plain text
- Automated tests (unit and integration)
- Interactive Swagger/OpenAPI documentation
- Full containerization via Docker (multi-stage build)
- Production deployment with a managed PostgreSQL database
- Versioned database migrations (Flyway)
- Observability endpoints (Spring Boot Actuator)
- Continuous integration pipeline (GitHub Actions)

---

## 🔐 Authentication (JWT)

The API uses **JSON Web Token (JWT)** authentication via **Spring Security**. Most endpoints require a valid token in the `Authorization` header.

### Authentication flow

1. **Register a user** (once):
   ```
   POST /auth/register
   Content-Type: application/json

   {
     "username": "john",
     "password": "password123"
   }
   ```
   Returns `201 Created` with a ready-to-use token.

2. **Log in** (subsequent times):
   ```
   POST /auth/login
   Content-Type: application/json

   {
     "username": "john",
     "password": "password123"
   }
   ```
   Returns `200 OK` with:
   ```json
   {
     "token": "eyJhbGciOiJIUzUxMiJ9...",
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```

3. **Use the token** in the `Authorization` header for every protected request:
   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```

### Public routes (no token required)
- `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`
- `GET /oauth2/authorization/google`, `/login/**`
- `/swagger-ui/**`, `/v3/api-docs/**`
- `GET /actuator/health`

### Protected routes (token required)
- All `/tasks/**` endpoints
- `/actuator/info`, `/actuator/metrics` (ADMIN only)

### Testing via Swagger

Open `/swagger-ui.html`, click **"Authorize"** (top right), paste the token obtained from login (without the `Bearer` prefix), and all protected endpoints become testable directly from the UI.

### Technical details

- Passwords are hashed with **BCrypt**, never stored in plain text.
- Access tokens expire after **15 minutes** (configurable via `jwt.expiration`).
- The API is **stateless** -- no server-side session is kept; every request authenticates independently via its token.

---

## 🔄 Refresh Token

To avoid requiring the user to log in with username/password frequently, the API implements the **access token + refresh token** pattern:

- **Access token (JWT):** short-lived (**15 minutes**), used in the `Authorization` header of every request.
- **Refresh token:** long-lived (**7 days**), stored in the database, used exclusively to obtain a new access token.

### Full flow

1. **Login/registration** return both tokens (see above).
2. When the **access token expires**, instead of logging in again, the client exchanges the refresh token for a new access token:
   ```
   POST /auth/refresh
   Content-Type: application/json

   {
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```
   Returns a new `token`, keeping the same `refreshToken`.
3. **Logout** revokes the refresh token, preventing further use:
   ```
   POST /auth/logout
   Content-Type: application/json

   {
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```

### Why this approach

- **Short access token** → limits the blast radius if a leaked token is intercepted.
- **Revocable refresh token** → allows remotely ending a session (logout), something impossible with a bare JWT.
- **One active refresh token per user** → each new login discards the previous refresh token, avoiding forgotten lingering sessions.

---

## 🔗 Google Login (OAuth2)

In addition to traditional login, the API supports authentication via **Google (OAuth2 / OpenID Connect)**.

### Flow

1. The client redirects the user to:
   ```
   GET /oauth2/authorization/google
   ```
2. The user logs in and authorizes access on Google's consent screen.
3. Google redirects back to the application, which exchanges the authorization code for profile data (email).
4. The API then:
    - Finds an existing user with that email, **or** creates a new one automatically (flagged as `AuthProvider.GOOGLE`, with no local password).
    - Issues the **same tokens** (JWT access token + refresh token) used by the traditional login.
5. The final response is identical to a regular `/auth/login`:
   ```json
   {
     "token": "eyJhbGciOiJIUzUxMiJ9...",
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```

### Why this approach

- **A single token system** across the whole API -- it doesn't matter whether the user signed in with a password or with Google, everything downstream (authorization, refresh, expiration) behaves identically.
- **Accounts reused by email** -- if a user already existed locally and later signs in with Google using the same email, it's the same account, no duplicate registrations.
- **No password stored for social accounts** -- the `password` field is null for `AuthProvider.GOOGLE` users, since authentication is fully delegated to Google.

### A note on gov.br

The architecture was designed generically enough to, in principle, support any additional OpenID Connect provider -- including Brazil's gov.br Single Sign-On, which uses the same protocol. In practice, developer access to gov.br for private applications isn't self-service: it requires a commercial process through Serpro/Dataprev's marketplace (for companies) or an institutional request via the Secretaria de Governo Digital (for public agencies), which makes it impractical for a personal portfolio project. For that reason, Google was chosen as the demonstration provider, while keeping the same technical foundation (OAuth2/OIDC) that an eventual institutional gov.br integration would use.

---

## 🛡️ Advanced features

### Access control
- **Ownership isolation** -- the `Task` → `User` relationship guarantees full isolation between users.
- **Role-based authorization:** `ADMIN` users have access to a special endpoint (`GET /tasks/admin/all`) listing tasks from **all** users -- useful for an admin dashboard.
- **Semantically correct HTTP responses:**
    - `401 Unauthorized` → missing or invalid token
    - `403 Forbidden` → authenticated, but insufficient permissions
    - Both return a standardized JSON body (`status`, `message`, `timestamp`)

### Pagination
Listing endpoints (`GET /tasks` and `GET /tasks/status/{status}`) support pagination via query params:
```
GET /tasks?page=0&size=10
```
Response format:
```json
{
  "content": [ ... ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

### Versioned migrations (Flyway)
The database schema is controlled by versioned SQL migrations under `src/main/resources/db/migration`, instead of letting Hibernate auto-alter tables. This ensures:
- An auditable history of every schema change
- Consistency across environments (local, CI, production)
- Protection against accidental destructive changes

> In production, Flyway was configured with a baseline (`spring.flyway.baseline-on-migrate`), since the database already had tables created by Hibernate before Flyway was adopted.

### Observability (Spring Boot Actuator)
The API exposes monitoring endpoints:
- `GET /actuator/health` -- public, used by Render for automated health-checks
- `GET /actuator/info` -- application info (ADMIN only)
- `GET /actuator/metrics` -- technical metrics: JVM, HTTP requests, connection pool, etc. (ADMIN only)

### Continuous Integration (CI)
Every push to `main` (and every Pull Request) automatically triggers a **GitHub Actions** pipeline (`.github/workflows/ci.yml`) that:
1. Sets up the environment with JDK 25
2. Runs the full automated test suite (unit + integration)
3. Builds and packages the application, validating that the build is sound

Build status (✅ or ❌) is visible directly in the repository's commit history.

---

## 📍 Endpoints

| Method | Route | Description |
|---|---|---|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Log in (returns access + refresh token) |
| `POST` | `/auth/refresh` | Exchange a refresh token for a new access token |
| `POST` | `/auth/logout` | Revoke a refresh token |
| `GET` | `/oauth2/authorization/google` | Start Google OAuth2 login |
| `POST` | `/tasks` | Create a new task |
| `GET` | `/tasks` | List the authenticated user's tasks (paginated) |
| `GET` | `/tasks/{id}` | Get a task by ID |
| `PUT` | `/tasks/{id}` | Update an existing task |
| `DELETE` | `/tasks/{id}` | Delete a task |
| `GET` | `/tasks/status/{status}` | List tasks filtered by status (paginated) |
| `GET` | `/tasks/admin/all` | List tasks from all users (ADMIN only) |

Example payload for task creation (`POST /tasks`):
```json
{
  "title": "Study Spring Boot",
  "description": "Finish the task CRUD",
  "dueDate": "2026-08-15"
}
```

---

## ▶️ Running locally

### Prerequisites
- [JDK 21+](https://adoptium.net)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Maven (or use the bundled wrapper `./mvnw`)

### Option 1 -- Everything via Docker (recommended)

Runs the application **and** the PostgreSQL database together, with nothing installed besides Docker:

```bash
git clone https://github.com/ssergionp/task-manager-api.git
cd task-manager-api
docker compose up --build -d
```

The API will be available at `http://localhost:8080`, and Swagger at `http://localhost:8080/swagger-ui.html`.

### Option 2 -- App locally (outside Docker), database in Docker

Starts only the Postgres database via Docker, and runs the Java app directly on your machine (useful during development, to take advantage of your IDE's hot-reload):

```bash
docker compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

### Option 3 -- In-memory database (H2), no Docker

Faster for quick tests, without data persistence:

```bash
./mvnw spring-boot:run
```

---

## 🧪 Running the tests

```bash
./mvnw test
```

The project includes:
- **Unit tests** (`TaskServiceTest`, `OAuth2LoginSuccessHandlerTest`) -- test business logic in isolation, using Mockito to simulate repositories and external dependencies.
- **Integration tests** (`TaskControllerTest`, `AuthControllerTest`) -- test endpoints via simulated HTTP requests (`MockMvc`), with an in-memory H2 database.

---

## 🐳 Docker

The project uses a **multi-stage build** in its `Dockerfile`:
1. A stage with Maven + JDK compiles the project and generates the `.jar`.
2. The final image only includes the Java Runtime (JRE), copying just the compiled `.jar` -- resulting in a lighter final image.

The `docker-compose.yml` orchestrates two services:
- `postgres` -- the database, with a health-check before releasing the application.
- `app` -- the Spring Boot application, which only starts after the database is ready.

> **Note:** locally, Postgres is exposed on port `5433` (instead of the default `5432`), to avoid conflicts with native Postgres installations that may already exist on the machine.

---

## ☁️ Deployment

- **Application:** hosted on [Render](https://render.com), with automatic deployment on every push to `main` (Docker runtime, detected from the project's `Dockerfile`).
- **Database:** PostgreSQL managed by [Neon](https://neon.tech), with a permanent free tier (unlike free databases that expire after a few days).

Database credentials are passed via environment variables on the hosting platform, never committed to the repository.

---

## 📌 Next steps (ideas for evolution)

- [ ] Two-factor authentication (2FA)
- [ ] Email confirmation on registration
- [ ] Password recovery flow
- [ ] Caching with Redis
- [ ] Rate limiting
- [ ] Subtasks / tags for tasks
- [ ] Load testing

---

## 👤 Author

**Sergio do Nascimento Pereira**
[LinkedIn](https://www.linkedin.com/in/sergio-do-nascimento-pereira-7a174a112/) • [GitHub](https://github.com/ssergionp)
