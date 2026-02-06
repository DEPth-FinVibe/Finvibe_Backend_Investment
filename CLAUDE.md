# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language

사용자에게 응답할 때 항상 한국어로 응답할 것.

## Build & Run Commands

```bash
./gradlew build                    # compile + test
./gradlew clean build              # clean build
./gradlew bootRun                  # run (default profile: local)
./gradlew test                     # all tests
./gradlew test --tests AssetServiceTest                    # single class
./gradlew test --tests AssetServiceTest.registerAsset_success  # single method
./gradlew test --tests "*.asset.*"                         # pattern match
./gradlew test --rerun-tasks                               # ignore cache
```

No dedicated lint/formatter task exists. `./gradlew build` runs all checks.

### Local Infrastructure (Docker)

```bash
docker compose -f infra/docker-compose.yml up -d    # MariaDB, Redis, MongoDB, Kafka
docker compose -f infra/docker-compose.yml down
```

## Architecture

**Spring Boot 4.0.1 / Java 21 / Gradle 9.2.1** — Modular Monolith with Hexagonal Architecture.

Root package: `depth.finvibe.investment`

### Module Layout

Four domain modules under `modules/`: **asset**, **market**, **trade**, **wallet** (plus `dev` for utilities).

Each module follows hexagonal layering:

```
modules/{module}/
  api/external/       # Public REST controllers
  api/internal/       # Inter-module APIs
  application/        # Services, use cases
    port/in/          # Input port interfaces
    port/out/         # Output port interfaces (repos, clients)
  domain/             # Entities, value objects, domain errors
  dto/                # Request/Response DTOs
  infra/              # JPA repos, Kafka producers/consumers, external clients, error mappers
```

`shared/` contains cross-cutting concerns (error handling, domain base classes, Redis/Kafka infra, distributed locking). `boot/` contains Spring config and JWT security.

### Dependency Rules

- Cross-module calls: `modules.{x}.application` → `modules.{y}.api` (never direct domain coupling)
- No cross-module JPA entity relationships
- Keep `shared/` minimal

### Key Integrations

- **KIS (Korea Investment & Securities)**: REST + WebSocket for real-time market data, configured via env vars
- **Kafka**: Event-driven communication between modules
- **Redis**: Caching + distributed locks (ShedLock/Redisson)
- **MariaDB**: Primary persistence (JPA/QueryDSL)
- **MongoDB**: Secondary data store

## Code Conventions

- **Indentation**: 2 spaces
- **Import order**: Java/Jakarta → Third-party → Project (`depth.finvibe.investment.*`) → Static
- **Types**: `BigDecimal` for money (never double/float), `Long` for entity IDs, `UUID` for user IDs
- **Lombok on entities**: `@Getter`, `@SuperBuilder`, `@NoArgsConstructor(access = PROTECTED)` — avoid `@Data`/`@Setter`
- **Lombok on services**: `@RequiredArgsConstructor`
- **Error handling**: Domain/application layers throw `DomainException` with module-specific `ErrorCode` enum. HTTP mapping lives in `modules/{module}/infra/error/` via `DomainErrorHttpMapper`. Never throw HTTP exceptions in domain/application.

## Testing Conventions

- **Structure**: Given/When/Then
- **`@DisplayName`**: Must be Korean
- **Unit tests**: `@ExtendWith(MockitoExtension.class)`, class suffix `*Test`, method names in snake_case
- **Assertions**: AssertJ; use `isEqualByComparingTo` for `BigDecimal`
- **Exception tests**: `assertThatThrownBy(...).isInstanceOf(DomainException.class)`

## Git Commit Convention

Format: `<type>(<module>): <한국어 설명>`

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`. Module: `asset`, `market`, `trade`, `wallet`, etc.

Multiple changes go as bullet points in the body:
```
feat(asset): 자산 등록 API 구현

- 자산 등록을 위한 REST API 엔드포인트 추가
- 자산 등록 서비스 로직 구현
```

Run `./gradlew test` before committing.
