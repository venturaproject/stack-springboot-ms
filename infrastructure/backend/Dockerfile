FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Use the committed Gradle wrapper (pinned version) — not a hand-downloaded Gradle.
COPY backend/gradlew ./
COPY backend/gradle ./gradle
RUN chmod +x ./gradlew

# Warm the dependency cache before copying sources so edits don't re-download the world.
COPY backend/build.gradle.kts backend/settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY backend/ .
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache postgresql-client curl && \
    addgroup -S app && adduser -S -G app app

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
COPY infrastructure/backend/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && mkdir -p /app/media && chown -R app:app /app

USER app

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["/entrypoint.sh"]
