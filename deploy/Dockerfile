# =========================
# 1. Build Stage
# =========================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper & 설정 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성 캐시 (빌드 속도 개선)
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon --stacktrace --info

# 소스 코드 복사
COPY src src

# JAR 빌드
RUN ./gradlew clean bootJar --no-daemon


# =========================
# 2. Runtime Stage
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 JAR만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring 기본 포트
EXPOSE 8080

# JVM 옵션 (필요하면 조정)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
