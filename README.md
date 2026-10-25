# Real Estate Portal API (rsp-api-server-zio)

Hexagonal Scala 3 API implementing `real-estate-portal-api.yaml` (50 operations / 10 resources) on top of the
`real-estate-portal-db-ddl.sql` PostgreSQL schema.

| Item | Version |
|---|---|
| Scala | 3.9.0 |
| sbt | 2.0.9 |
| organization | `com.atacamasoft.rsp` |
| logback-classic | 1.6.5 |
| zio-http | 3.11.6 |
| Tapir (SoftwareMill) | 1.13.33 |
| Persistence | PostgreSQL (+ PostGIS) via JDBC/HikariCP |

## Run

```bash
docker compose up -d            # PostGIS + schema rsp_db_schema_dev + DDL (db/)
sbt run                         # http://localhost:8080
```

* `GET /health` – liveness (process is up; never touches PostgreSQL)
* `GET /health/ready` – readiness (200 if PostgreSQL answers `select 1`, else 503)
* `OPTIONS /**` – Preflight Handler (`PreflightHandler`): CORS preflight -> 204 + `Access-Control-*`; plain OPTIONS -> 204 + `Allow`
* `GET /docs` – Swagger UI generated from the Tapir endpoints; API under `/api/v1`

Configuration (environment variables, defaults in parentheses): `HTTP_HOST` (0.0.0.0), `HTTP_PORT` (8080),
`DB_URL` (jdbc:postgresql://localhost:5432/rsp_db_dev), `DB_USER` (rsp_db_role_dev),
`DB_PASSWORD` or `POSTGRES_PASSWORD` (rsp_dev_password), `DB_SCHEMA` (rsp_db_schema_dev), `DB_POOL_SIZE` (10),
`CORS_ALLOWED_ORIGINS` (`*`, comma separated).

## Layout

```
domain/                      entities, commands, filters, Patch (Keep|Clear|Assign), DomainError
application/port/in          use-case ports (CrudUseCase + one trait per aggregate)
application/port/out         repository ports, HealthProbe, TokenVerifier
application/service          services + business rules (address hash / duplicate detection, lead<->listing agency, date ranges...)
infrastructure/adapter/in/http   Tapir endpoints, DTOs/mappers (api/), health, preflight, request-id/CORS middleware
infrastructure/adapter/out/persistence   JDBC (HikariCP): table definitions + generic CRUD repository
infrastructure/Application.scala         composition root
```

## Behaviour notes

* JSON uses camelCase without the table prefix; `id/createdAt/updatedAt` are server generated.
* PATCH: absent property = unchanged, `null` = clear (nullable columns only, otherwise 400), at least one property required.
* Errors are `Error` objects: 400 validation, 404 not found, 409 unique violation / delete of referenced rows,
  422 FK violation, check violation or business rule, 500 unexpected.
* `X-Request-Id` / `X-Correlation-Id` are reused or generated and echoed on every response.
* Properties: `addressHash` is computed server side (SHA-256 of the normalized address); creating/updating a property
  to an address that already exists answers 409.
* Not implemented: bearer token validation. `PermitAllTokenVerifier` accepts everything; plug a Keycloak/JWT
  `TokenVerifier` adapter in `Application.scala`.
* The DDL needs PostGIS (`geography`) and the `rsp_db_schema_dev` schema (`db/00-init.sql`).
