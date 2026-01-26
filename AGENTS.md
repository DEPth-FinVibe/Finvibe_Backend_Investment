# AGENTS.md - Development Guide for Coding Agents

This guide documents how to work safely and consistently in `finvibe-investment`.

## Project Snapshot
- Spring Boot 4.0.1 투자 도메인 서비스 (자산, 시장, 거래, 지갑)
- Language: Java 21 (Temurin JDK)
- Build: Gradle 9.2.1 (use wrapper)
- Architecture: Modular Monolith + Hexagonal/Clean Architecture
- Root package: `depth.finvibe.investment`

## Build, Run, and Test Commands

### Build
```bash
./gradlew build                    # compile + test
./gradlew clean build              # clean build
./gradlew bootJar                  # executable JAR
./gradlew bootBuildImage           # Docker image
```

### Run
```bash
./gradlew bootRun
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### Tests
```bash
./gradlew test                                         # all tests
./gradlew test --tests AssetServiceTest                # single test class
./gradlew test --tests AssetServiceTest.registerAsset_success
./gradlew test --tests "*.asset.*"                     # pattern match
./gradlew test --rerun-tasks                           # ignore cache
```

### Lint/Format
No dedicated lint or formatter task is defined in the repo. Use `./gradlew build`
(or `./gradlew test`) to validate code and run checks.

### Local Infra (optional)
```bash
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml down
```

## Code Style and Conventions

### Formatting
- Indentation: 2 spaces, no tabs
- Braces: always use braces
- Line length: keep reasonable (around 120 chars)

### Import Order
1. Java/Jakarta (`java.*`, `javax.*`, `jakarta.*`)
2. Third-party (Spring, Lombok, etc.)
3. Project (`depth.finvibe.investment.*`)
4. Static imports (after non-static imports)

Example:
```java
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.shared.error.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
```

### Naming
- Classes/Interfaces: PascalCase, no `I` prefix
- Methods/Variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Packages: lowercase
- Test classes: `*Test`
- Test methods: snake_case (e.g., `registerAsset_success`)

### Type Usage
- Money: `BigDecimal` (never `double`/`float`)
- IDs: `Long` for entity IDs, `UUID` for user identifiers
- Collections: prefer interfaces (`List`, `Set`, `Map`)
- Optional: repository return types only, never parameters

### Lombok
- Entities: `@Getter`, `@SuperBuilder`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- Services: `@RequiredArgsConstructor`
- DTOs: `@Getter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`
- Avoid: `@Data` or public `@Setter` on entities

### Class Layout (recommended)
1. Annotations
2. Fields with annotations
3. Other fields
4. Public methods
5. Protected/private methods
6. Static factory methods

### Error Handling (Domain Purity)
- Domain/Application: throw `DomainException` with module-specific `DomainErrorCode`
- Create enum per module: `AssetErrorCode implements DomainErrorCode`
- HTTP mapping in `modules/{module}/infra/error/` via `DomainErrorHttpMapper`
- Never throw HTTP exceptions (e.g., `ResponseStatusException`) inside domain/application

## Architecture and Packages

### Module Structure
```
modules/
  asset/
  market/
  trade/
  wallet/
```

### Layering per Module
```
modules/{module}/
  api/external    # public REST controllers
  api/internal    # inter-module APIs
  application     # use cases and services
  application/port/in
  application/port/out
  domain          # entities, value objects, errors
  dto             # request/response DTOs
  infra           # persistence, messaging, clients, http mappers
```

### Dependency Rules
- Allowed: `modules.{x}.application` -> `modules.{y}.api`
- Forbidden: cross-module domain coupling or JPA entity relationships
- Shared kernel (`shared/`) must stay minimal

## Testing Guidelines
- Use Given/When/Then structure
- `@DisplayName` must be Korean
- Unit: `@ExtendWith(MockitoExtension.class)`
- Integration: `@SpringBootTest`
- Assertions: AssertJ; `BigDecimal` via `isEqualByComparingTo`
- Exception tests: `assertThatThrownBy(...).isInstanceOf(DomainException.class)`

## Git Conventions
- Commit messages must be in Korean
- Example: `"자산 등록 기능 추가"`
- Run `./gradlew test` before committing

## Cursor Rules to Follow
- `.cursor/rules/convention-package.mdc`
- `.cursor/rules/convention-errorhandling.mdc`

## Helpful References
- `docs/external-api-spec.md`
- `README.md`

## 추가 정보
- 사용자에게 응답할땐 항상 한국어로 응답할것