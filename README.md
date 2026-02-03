# Finvibe Investment

투자 도메인(자산, 시장, 거래, 지갑)을 다루는 Spring Boot 기반 서비스입니다.

## 주요 기술
- Java 21, Spring Boot 4.0.1
- Spring Data JPA / MongoDB / Redis
- Kafka, QueryDSL, Lombok
- Gradle (Wrapper 포함)

## 모듈
- asset: 자산 관련 도메인
- market: 시장/시세 관련 도메인
- trade: 거래 관련 도메인
- wallet: 지갑/보유 자산 관련 도메인

## 디렉터리 구조
- `src/main/java/depth/finvibe/investment`: 애플리케이션 코드
  - `boot`: 부트스트랩/설정
  - `modules`: 도메인 모듈
  - `shared`: 공통 구성요소
- `src/main/resources`: 설정 파일 (`application-*.yml`)
- `src/test/java/depth/finvibe/investment`: 테스트 코드
- `src/test/resources`: 테스트 리소스

## 실행/빌드/테스트
```bash
./gradlew bootRun
./gradlew test
./gradlew build
./gradlew clean
./gradlew bootJar
```

## 프로필과 설정
- 기본 프로필: `kafka` (`src/main/resources/application.yml`)
- 로컬 실행 예시:
```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```
- 프로필별 설정 파일: `application-local.yml`, `application-kafka.yml`, `application-prod.yml`

## 로컬 개발 환경
로컬 프로필 기준으로 MariaDB, MongoDB, Redis, Kafka가 필요할 수 있습니다.
각 연결 정보는 `src/main/resources/application-*.yml`에서 관리합니다.

## 인프라 (Docker Compose)
`infra/docker-compose.yml`에 로컬 개발용 인프라가 정의되어 있습니다.
- MariaDB (3306), Redis (6379), Kafka (9092), Zookeeper (2181)

```bash
docker compose -f infra/docker-compose.yml up -d
```

중지/정리:
```bash
docker compose -f infra/docker-compose.yml down
```

## 코드/테스트 가이드
- 4-space 들여쓰기, 표준 Java 네이밍
- Lombok 사용 (Getter/Builder 등)
- 테스트는 JUnit 5, 이름은 `*Test` 접미사

.
