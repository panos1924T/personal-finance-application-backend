# Personal Finance Application — Backend

Spring Boot REST API for a personal finance application.

The application manages accounts, categories and financial transactions, while keeping account balances synchronized with transaction creation, updates and deletion.

The repository also contains the Docker Compose configuration used to run the complete application:

- PostgreSQL
- Spring Boot backend
- Angular frontend with nginx

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security
- JWT authentication
- PostgreSQL
- Flyway
- Gradle
- JUnit 5
- Mockito
- JaCoCo
- Springdoc OpenAPI / Swagger
- Docker
- Docker Compose

## Main Features

- JWT authentication
- User-scoped financial data
- Accounts
- Categories with parent/child hierarchy
- Income transactions
- Expense transactions
- Transfers between accounts
- Automatic account balance updates
- Transaction update with balance reversal/recalculation
- Transaction deletion with balance reversal
- Soft deletion
- Transaction filtering
- Pagination
- Flyway database migrations

## Project Structure

Main backend layers:

```text
api/          REST controllers
service/      Business logic
repository/   Database access
dto/          Request / response DTOs
mapper/       Entity / DTO mapping
validator/    Business validation
security/     JWT and Spring Security
db/migration/ Flyway migrations
```

---

# Quick Start with Docker

This is the easiest way to run the complete application.

Docker Compose starts:

```text
Browser
   |
   v
Angular + nginx :8081
   |
   | /api
   v
Spring Boot :8080
   |
   v
PostgreSQL
```

## 1. Required folder structure

The frontend and backend repositories must be next to each other:

```text
projects/
├── personal-finance-application-backend/
└── personal-finance-application-frontend/
```

Clone both repositories:

```bash
git clone git@github.com:panos1924T/personal-finance-application-backend.git
git clone git@github.com:panos1924T/personal-finance-application-frontend.git
```

Enter the backend repository:

```bash
cd personal-finance-application-backend
```

## 2. Create Docker environment file

Copy:

```bash
cp env.example .env.docker
```

On Windows PowerShell:

```powershell
Copy-Item env.example .env.docker
```

Then edit `.env.docker`.

Example:

```env
POSTGRES_DB=expense_tracker_db

DB_USERNAME=your_database_user
DB_PASSWORD=your_database_password

SPRING_PROFILES_ACTIVE=prod

JWT_SECRET_KEY=your_long_random_secret
JWT_EXPIRATION=3600000

ADMIN_EMAIL=
ADMIN_PASSWORD=

APP_PORT=8081

ALLOWED_ORIGINS=http://localhost:8081
```

`JWT_EXPIRATION=3600000` means one hour.

Never commit `.env` or `.env.docker`.

## 3. Start the complete application

```bash
docker compose --env-file .env.docker up -d --build
```

Or on Linux / Git Bash:

```bash
chmod +x startup.sh
./startup.sh
```

Open:

```text
http://localhost:8081
```

No separate Gradle or Angular build is required.

Docker builds both applications automatically.

---

# Docker Commands

## Check containers

```bash
docker compose --env-file .env.docker ps
```

## View all logs

```bash
docker compose --env-file .env.docker logs -f
```

## Backend logs only

```bash
docker compose --env-file .env.docker logs -f backend
```

## Database logs only

```bash
docker compose --env-file .env.docker logs -f db
```

## Frontend logs only

```bash
docker compose --env-file .env.docker logs -f frontend
```

## Stop the application

```bash
docker compose --env-file .env.docker down
```

## Start existing containers again

```bash
docker compose --env-file .env.docker up -d
```

## Rebuild after pulling new code

```bash
git pull

docker compose --env-file .env.docker up -d --build
```

## Completely delete containers and database data

Warning: this deletes the PostgreSQL Docker volume.

```bash
docker compose --env-file .env.docker down -v
```

Normal `docker compose down` does **not** delete the database.

---

# Database Persistence

PostgreSQL data is stored in a named Docker volume:

```text
personal_finance_postgres_data
```

This means application data survives:

- container restarts
- `docker compose down`
- application rebuilds
- server reboots

Do not use:

```bash
docker compose down -v
```

unless the database should intentionally be deleted.

---

# Local Development without Docker

Docker is recommended for running the complete application, but the backend can also run directly.

## Requirements

- Java 21
- PostgreSQL
- `.env`

The local `.env` and `.env.docker` are intentionally separate.

```text
.env          local development
.env.docker   Docker deployment
```

## Build

Linux / macOS / Git Bash:

```bash
./gradlew clean build
```

Windows:

```powershell
.\gradlew.bat clean build
```

Generated jar:

```text
build/libs/et.jar
```

## Run

Linux / macOS / Git Bash:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

Default backend URL:

```text
http://localhost:8080
```

API:

```text
http://localhost:8080/api/v1
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# Tests

Run all backend tests:

Linux / macOS / Git Bash:

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

Generate JaCoCo report:

```bash
./gradlew jacocoTestReport
```

Test configuration uses an H2 in-memory database and does not require the development PostgreSQL database.

---

# Authentication

Login endpoint:

```text
POST /api/v1/auth/authenticate
```

Authenticated requests use:

```text
Authorization: Bearer <JWT>
```

JWT lifetime is configured through:

```env
JWT_EXPIRATION=3600000
```

The JWT secret must remain private:

```env
JWT_SECRET_KEY=...
```

---

# API Areas

Main endpoints:

```text
/api/v1/auth
/api/v1/users
/api/v1/accounts
/api/v1/categories
/api/v1/transactions
```

Swagger provides the complete request and response contracts.

---

# Transaction Rules

## Income

- requires a positive amount
- requires an income category
- credits the destination/default account

## Expense

- requires a positive amount
- requires an expense category
- debits the selected source account

## Transfer

- requires source and target accounts
- source and target must be different
- debits the source account
- credits the target account

## Update

When a transaction is edited:

1. the previous financial effect is reversed
2. the transaction is updated
3. the new financial effect is applied

## Delete

Transaction deletion reverses its financial effect before soft deletion.

---

# Home Server / Tailscale

The application is designed to run on a private home server.

Install Docker and Tailscale on the server, clone both repositories as sibling directories, create `.env.docker`, then run:

```bash
./startup.sh
```

or:

```bash
docker compose --env-file .env.docker up -d --build
```

If the server Tailscale IP is for example:

```text
100.x.x.x
```

the application is available to devices on the same Tailnet at:

```text
http://100.x.x.x:8081
```

For the server environment, add the address to `.env.docker` if required:

```env
ALLOWED_ORIGINS=http://localhost:8081,http://100.x.x.x:8081
```

No router port forwarding is required.

---

# Updating the Home Server

From the backend repository:

```bash
git pull
```

From the frontend repository:

```bash
git pull
```

Then return to the backend repository:

```bash
docker compose --env-file .env.docker up -d --build
```

The PostgreSQL volume remains unchanged.

---

# Security Notes

Never commit:

```text
.env
.env.docker
```

These files may contain:

- PostgreSQL credentials
- JWT secret
- admin credentials

The repository contains only `env.example` as a configuration template.