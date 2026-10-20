# user-service

Spring Boot microservice for user authentication and profile management: registration, JWT-based login with refresh tokens, profile updates, and address management. Publishes a `user-events` Kafka event on registration.

## Stack

- Java 17, Spring Boot 3.3
- Spring Security + JWT (jjwt)
- Spring Data JPA, Flyway migrations
- PostgreSQL (prod/default), H2 (dev)
- Kafka (spring-kafka)
- springdoc OpenAPI / Swagger UI

## Running locally

### Option 1: Docker Compose (Postgres + Kafka + app)

```bash
docker compose up --build
```

The app starts on `http://localhost:8081`.

### Option 2: Dev profile (in-memory H2, no external services)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Kafka publishing failures are logged and swallowed, so this mode works without a running broker.

## Configuration

Environment variables (see `application.yml` / `application-prod.yml`):

| Variable | Description | Default (dev) |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | required in prod |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker | `localhost:29092` |
| `JWT_SECRET` | HMAC signing secret | required in prod |
| `JWT_EXPIRATION_MS` | Access token TTL | `86400000` (24h) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token TTL | `604800000` (7d) |

Activate the production profile with `SPRING_PROFILES_ACTIVE=prod`.

## API

Swagger UI: `http://localhost:8081/swagger-ui.html`
Health check: `http://localhost:8081/actuator/health`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create account, returns access + refresh token |
| POST | `/api/auth/login` | none | Login, returns access + refresh token |
| POST | `/api/auth/refresh` | none (refresh token in body) | Rotate access/refresh token pair |
| POST | `/api/auth/logout` | none (refresh token in body) | Revoke a refresh token |
| GET | `/api/users/me` | Bearer | Get current user's profile |
| PUT | `/api/users/me` | Bearer | Update current user's profile |
| POST | `/api/users/me/addresses` | Bearer | Add an address |
| GET | `/api/users/me/addresses` | Bearer | List addresses |
| DELETE | `/api/users/me/addresses/{id}` | Bearer | Delete an address |

## Tests

```bash
mvn test
```

Unit tests cover `AuthService` and `UserService`; `AuthFlowIntegrationTest` exercises the full register → login → refresh → logout → protected-endpoint flow via MockMvc against an H2 database, with Kafka mocked out.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` on every push/PR to `main`.
