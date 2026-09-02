# AEP6S — ODS 2 Food Donation CRUD

**Maven module:** `aep/aep/` (Spring Boot 4.1.0, Java 17, MongoDB `poc_doacoes`)

## Commands

```bash
cd aep/aep
./mvnw compile -DskipTests          # build (BUILD SUCCESS)
./mvnw spring-boot:run               # run (requires Mongo on localhost:27017)
docker compose up -d                 # starts mongo:7.0 on 27017
```

## Architecture

- **Domain:** `Usuario` (donor) + `Doacao` (food item, `String` item, `int` quantidade)
- **Packages:** `fz.exemple.aep` — controllers, services, repositories, models, dto, mapper, config, exception
- **Security:** `config/SecurityConfig.java` → `permitAll` (no auth)
- **Validation:** `@Valid` on DTOs, centralized `GlobalExceptionHandler` → `400` (validation), `404` (not found)
- **API:** `/api/usuarios`, `/api/doacoes` — CRUD + `GET /api/doacoes/resumo` (total doações, quantidade, itens distintos)
- **Status codes:** `POST → 201 + Location`, `PUT → 200/404`, `DELETE → 204` idempotent

## Key files

| Purpose | Path |
|---------|------|
| Build | `aep/aep/pom.xml` |
| Config | `aep/aep/src/main/resources/application.properties` |
| Mongo compose | `aep/aep/compose.yaml` |
| Entry | `aep/aep/src/main/java/fz/exemple/aep/AepApplication.java` |
| DTOs | `aep/aep/src/main/java/fz/exemple/aep/dto/*` |
| Mappers | `aep/aep/src/main/java/fz/exemple/aep/mapper/*` |
| Controllers | `aep/aep/src/main/java/fz/exemple/aep/controllers/*` |
| Services | `aep/aep/src/main/java/fz/exemple/aep/services/*` |
| Models | `aep/aep/src/main/java/fz/exemple/aep/models/*` |

## Gotchas

- `spring-boot-starter-parent 4.1.0` uses `spring-boot-starter-webmvc` (not `web`)
- No tests — test deps and `src/test` removed per request
- Mongo expects unauthenticated `localhost:27017` / `poc_doacoes` (see `application.properties:2-4`)
- `data_doacao` stored as `data_doacao` in Mongo via `@Field`; Java field is `dataDoacao`
- `Endereco` embedded in `Usuario`, no separate collection
- Lombok present but unused — prefer plain getters/setters