# Personal Finance API (ET)

Headless REST API for personal finance management (accounts, categories, transactions), with JWT authentication, role/capability authorization, and soft-delete behavior.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA (Hibernate)
- Spring Security (JWT + method security)
- Flyway migrations
- PostgreSQL
- Gradle
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 + Spring Boot Test + Spring Security Test + JaCoCo
- Docker + Docker Compose

## Architecture

- `api/`: REST controllers
- `service/`: business rules and transactional flows
- `repository/`: data access via Spring Data JPA
- `dto/`: request/response DTOs
- `mapper/`: entity <-> DTO mapping
- `validator/`: custom validation layer
- `security/`: JWT filter, auth config, CORS, handlers
- `db/migration`: versioned schema and seed migrations

## Build

```bash
./gradlew clean build
```

Generated artifact: `build/libs/et.jar`

Useful commands:

```bash
./gradlew test
./gradlew jacocoTestReport
```

## Run (Local JVM)

### 1) Prerequisites

- Java 21
- PostgreSQL
- `.env` file in project root

### 2) Configure `.env`

Create `.env` from `env.example` and fill:

```env
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET_KEY=...

PG_HOST=
PG_PORT=
PG_DB=
SPRING_PROFILES_ACTIVE=

# optional admin seeding
ADMIN_EMAIL=...
ADMIN_PASSWORD=...
```

Notes:
- Environment variables are loaded from `.env` by `EnvConfig` (dotenv).
- Default active profile in `application.properties` is `dev`; for local PostgreSQL on `5432`, `staging` is the practical default.

### 3) Start the application

```bash
./gradlew bootRun --args='--spring.profiles.active=staging'
```

API base URL: `http://localhost:8080/api/v1`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Run (Docker Compose)

`docker-compose.yml` starts:
- `db` (PostgreSQL 17, host port `5434`)
- `app` (Spring Boot API on `8080`)

Commands:

```bash
./gradlew clean build -x test
docker compose up -d --build
```

or use:

```bash
./startup.sh
```

## Authentication and Authorization

Public endpoints:
- `POST /api/v1/users` (register)
- `POST /api/v1/auth/authenticate` (login)

All other endpoints require:
- `Authorization: Bearer <jwt>`

Authorization model:
- Roles: `ADMIN`, `CITIZEN`
- Capabilities seeded by Flyway migrations (`V2`, `V3`)
- Method-level authorization with `@PreAuthorize` for user operations

## Business Logic

### 1) User lifecycle

- On registration:
  - user role is set to `CITIZEN`
  - default categories are seeded (expense and income roots)
  - default `Cash` account is created with zero balance
- User email must be unique.
- User delete is soft delete.

### 2) Admin seeding at startup

Implemented in `runner/AdminSeeder`:
- If `ADMIN_EMAIL` and `ADMIN_PASSWORD` are provided and the user does not already exist:
  - create a `superadmin` user
  - hash password with BCrypt
  - attach role `ADMIN` (must exist from DB migrations)
- If admin env vars are missing, seeding is skipped.

### 3) Account rules

- Accounts are always user-scoped.
- Default account cannot be updated or deleted.
- Account deletion is allowed only when:
  - account is not already deleted
  - account balance is exactly zero
- Account deletion is soft delete.

### 4) Category rules

- Category hierarchy supports 2 levels: root and child.
- Child category rules:
  - parent must be a root category (not another child)
  - child type must match parent transaction type
- Name uniqueness is enforced per user/type/parent scope.
- Parent category with active children cannot be deleted.
- Category deletion is soft delete.

### 5) Transaction and balance rules

Creation rules:
- `amount > 0` is mandatory
- `INCOME`: requires `INCOME` category, credits default account
- `EXPENSE`: requires `EXPENSE` category and source account, debits source account
- `TRANSFER`: requires source and target account, they must be different; debit source and credit target

Update/Delete behavior:
- Existing transaction financial effect is reverted first.
- On update, new effect is then applied.
- Delete is soft delete with balance reversal.

### 6) Soft-delete model

- Core entities include `deleted` and `deletedAt`.
- Active-only queries explicitly filter `deleted=false`.
- Several GET endpoints support `includeDeleted`.

## API Endpoints (Summary)

- Auth: `/api/v1/auth/authenticate`
- Users: `/api/v1/users`
- Accounts: `/api/v1/accounts`
- Categories: `/api/v1/categories`
- Transactions: `/api/v1/transactions`

For full request/response contracts, use Swagger UI.
