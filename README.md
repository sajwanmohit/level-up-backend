# Level Up Backend

Level Up is a REST backend for daily strength-workout tracking and training analytics. V1 includes reusable exercises, workouts and sets, exercise-level personal records, daily analysis, progression history and basic dashboard metrics. Nutrition is intentionally out of scope. The workout/PR code is isolated from future domains so Nutrition can be added independently.

## Architecture and stack

Java 21, Spring Boot 3, Spring Web, Spring Security, Spring Data JPA, PostgreSQL, Flyway and Maven. This is a layered modular monolith: controllers handle HTTP, services own business rules, repositories handle persistence, and DTO records define API contracts. The `analytics` and `pr` packages own analysis and records separately from workout CRUD. Stateless bearer-token authentication is used for API requests, with per-user resource ownership enforced in the services.

All measurement values use `BigDecimal`. The e1RM calculator is an injectable component using Epley: `weight * (1 + reps / 30)`, rounded to two decimal places. PR history records a new row only when a working set exceeds the prior best of that type for the same user and exercise. Maximum reps is the highest reps in a single working set; volume PR is the highest single-set `weight × reps`. Warmups do not count toward PRs or working-set volume. Workout totals include all exercises and working sets; average RPE/RIR are null if none were recorded.

## Local setup

Requirements: Docker with Compose and Java 21/Maven if running outside containers. Configure the local environment file and start PostgreSQL and the backend:

```sh
cp .env.example .env
# Change DB_PASSWORD and JWT_SIGNING_KEY to local-only values.
docker compose up --build
```

Alternatively start only the local database with `docker compose up -d postgres`, then run `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`. Local database credentials and the JWT signing key are read from `.env`; copy `.env.example` to `.env` and replace its placeholders. Docker Compose loads `.env` automatically. When running Maven directly, export the variables from `.env` in your shell or configure them in your IDE run configuration. Generate the local signing key with `openssl rand -base64 32`. The application is available at `http://localhost:8080`; Swagger UI is at `/swagger-ui.html` and OpenAPI JSON at `/v3/api-docs`.

## Environment and database

`application.yml` contains common safe defaults and selects `local` as the default profile. `application-local.yml` connects to local Postgres. `application-prod.yml` requires production database host, port, database, user and password variables. Never commit `.env` or production credentials. Render's PostgreSQL internal connection values should be provided through its environment settings.

| Variable | Local example / purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` or `prod` |
| `DB_HOST` | `localhost` for host-run application; Compose overrides to `postgres` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `levelup` |
| `DB_USERNAME` | local database user |
| `DB_PASSWORD` | local-only password; set securely in Render for production |
| `JWT_SIGNING_KEY` | Required Base64-encoded random key of at least 32 bytes; generate with `openssl rand -base64 32`. Keep it in local `.env` or deployed server secrets; rotating it invalidates issued tokens. |
| `AUTH_TOKEN_EXPIRATION_SECONDS` | Access-token lifetime; defaults to `900` seconds (15 minutes). |
| `SERVER_PORT` | `8080` |
| `FRONTEND_URL` | Local `http://localhost:5173`; production defaults to `https://level-up-frontend-rho.vercel.app` and can be overridden with the deployed frontend origin |

Flyway migrations under `src/main/resources/db/migration` are the schema source of truth. Hibernate runs with `ddl-auto=validate`; it does not create or update production tables. A named Docker volume retains local PostgreSQL data.

Existing users created before password authentication retain their fitness data, but their new `password_hash` is empty and they cannot log in until provisioned through a separately verified account-recovery process. Do not assign a password to a legacy account based only on an unverified email address.

## REST API

All training endpoints are under `/api/v1`. UUIDs are used for identifiers. JSON date fields use ISO-8601 (`YYYY-MM-DD` for workout dates and UTC instants for timestamps). Validation failures use the common API error response.

### Authentication

| Method and path | Request / response |
|---|---|
| `POST /api/v1/auth/signup` | `{ "name": "...", "email": "...", "password": "..." }`. Name/email follow the existing validation; password must be 8–72 characters and no more than 72 UTF-8 bytes for BCrypt. Email is trimmed and lower-cased. Returns `201` with no body. |
| `POST /api/v1/auth/login` | `{ "email": "...", "password": "..." }`. Returns exactly `{ "user": { "id": "uuid", "name": "...", "email": "..." }, "accessToken": "..." }`. Unknown email and incorrect password return the same `401` credential error. |

Passwords are stored as BCrypt hashes. Login issues an HS256 JWT with the user's UUID as `sub`, issuer `level-up-backend`, and an expiry set by `AUTH_TOKEN_EXPIRATION_SECONDS` (default 900 seconds). The signing key comes from the required `JWT_SIGNING_KEY` server environment variable; it must be a Base64-encoded random key of at least 32 bytes and is never stored in the repository. Send it as `Authorization: Bearer <accessToken>`.

Every `/api/v1/**` endpoint requires a valid, unexpired token except `POST /api/v1/auth/signup` and `POST /api/v1/auth/login`. User IDs supplied in query parameters or workout request bodies must match the token subject; a mismatch is rejected with `403`. Access to workouts, their exercise sessions/sets, daily analytics, and personal records is limited to the owning user. Missing, invalid, and expired tokens return `401`. The two authentication POST routes and API documentation routes are public. The former passwordless `POST /api/v1/users` route is no longer available.

### Request logging

The backend logs each HTTP request with its method, path (without query parameters), response status, duration, and a generated `X-Request-Id` returned in the response. Client-supplied request IDs, request/response bodies, authorization headers, passwords, tokens, and query parameter values are not logged. HTTP 4xx requests log at `WARN`, 5xx at `ERROR`, and other responses at `INFO`.

| Method and path | Purpose / request |
|---|---|
| `GET /exercises` | List exercises alphabetically. |
| `POST /exercises` | Create exercise: `name` required; optional `description`, `muscleGroup`, `equipment`, `movementType`; `compound` boolean. |
| `GET /workouts?userId={uuid}[&from=YYYY-MM-DD&to=YYYY-MM-DD]` | List the authenticated user's workouts, optionally within an inclusive date range. `userId` must match the token subject. Supply both range bounds or neither. |
| `GET /workouts/{id}` | Get workout and nested exercise sessions and sets. |
| `POST /workouts` | Create workout with required `userId`, `workoutDate`, optional `startedAt`, `completedAt`, `name`, `notes`. Completion cannot precede start. |
| `PUT /workouts/{id}` | Replace workout metadata with the same body. The owner cannot change; completed workouts are immutable. |
| `DELETE /workouts/{id}` | Delete workout and its dependent exercise sessions, sets and PR rows. |
| `POST /workouts/{id}/exercises` | Add `{ "exerciseId": "uuid", "exerciseOrder": 1, "notes": "..." }`; order is unique within the workout. |
| `DELETE /workouts/{id}/exercises/{exerciseSessionId}` | Remove a session and its sets and PR rows. |
| `POST /exercise-sessions/{id}/sets` | Create set: `setNumber`, `weight`, `reps` required; optional `rpe` (0–10), `rir` (non-negative), `warmup`, `failure`. Returns any PRs achieved by this set. |
| `PUT /sets/{id}` | Replace set fields with the same request shape; recalculates the set's PRs. |
| `DELETE /sets/{id}` | Remove set and its PR rows. |
| `GET /prs?userId={uuid}` | List a user's PR history. |
| `GET /prs/exercises/{exerciseId}?userId={uuid}` | List PR history for an exercise and user. Types: `MAX_WEIGHT`, `MAX_REPS`, `ESTIMATED_ONE_REP_MAX`, `VOLUME`. |
| `GET /analytics/daily?workoutId={uuid}` | Analyze a completed workout: exercise counts/breakdowns, working/warmup sets, reps, volume, average RPE/RIR, PRs and best set e1RMs. |
| `GET /analytics/exercises/{exerciseId}?userId={uuid}` | Exercise progression summary and chart history grouped by session date. Includes sessions, working sets, volume, best weight/reps/e1RM, latest e1RM and PR count. |
| `GET /analytics/dashboard?userId={uuid}` | User's total workouts, PR rows, working-set volume and distinct performed exercises. |

Example workout set request:

```json
{
  "setNumber": 1,
  "weight": 85.00,
  "reps": 8,
  "rpe": 8.5,
  "rir": 1.5,
  "warmup": false,
  "failure": false
}
```

Errors have the shape `{ "timestamp": "...", "status": 400, "error": "VALIDATION_ERROR", "message": "...", "path": "/api/v1/..." }`. Unexpected server errors return a generic message without stack traces.

## Tests and build

```sh
mvn test
mvn verify
docker build -t level-up-backend .
```

Tests cover the e1RM formula and edge cases, warmup exclusion and independent PR detection (a reps record at an unchanged weight need not produce a weight PR).

## GitHub setup

Create an empty GitHub repository, then from this directory configure its URL and push the initial branch:

```sh
git remote add origin https://github.com/<owner>/<repository>.git
git add .
git commit -m "Initial Level Up backend"
git push -u origin main
```

## Render deployment

Deploy the Dockerfile as a Render Web Service and provision a separate Render PostgreSQL database. Set `SPRING_PROFILES_ACTIVE=prod`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and a securely generated `JWT_SIGNING_KEY` in Render's environment secrets. Optionally set `AUTH_TOKEN_EXPIRATION_SECONDS`. `FRONTEND_URL` defaults to `https://level-up-frontend-rho.vercel.app`; set it explicitly if you use a different production origin. Set credentials in Render's secret environment variables only, use the database's private/internal host where available, and do not add PostgreSQL to the application container. The server listens on Render's `PORT` when provided (otherwise `SERVER_PORT`, defaulting to `8080`). Flyway applies pending migrations when the application starts. Configure a health-check path appropriate to your deployment (for example `/v3/api-docs`).
