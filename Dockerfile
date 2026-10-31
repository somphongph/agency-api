# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Agency Dashboard API — Spring Boot 4 / Java 21
#
# Build context: this directory (agency-api/)
#   docker build -t agency-api .
#   docker compose -f ../agency-docs/docker/docker-compose.yml up -d --build
# ---------------------------------------------------------------------------

# ---- Build stage ----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Dependencies first, so a source-only change does not re-download them.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src src
# Tests run outside the image build (`./mvnw test`, Bruno collection), not here.
RUN ./mvnw -B -ntp package -DskipTests

# ---- Runtime stage --------------------------------------------------------
FROM eclipse-temurin:21-jre

# The app only needs to read its own jar, so run unprivileged.
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --create-home app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/*.jar app.jar

USER app
EXPOSE 8080

# MaxRAMPercentage keeps the heap proportional to the container memory limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# No curl/wget in the JRE image, so probe the real endpoint over bash's /dev/tcp.
# This also proves the Spring context is up, not just that the port is bound.
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
    CMD bash -c "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /api/v1/health HTTP/1.0\r\n\r\n' >&3 && head -1 <&3 | grep -q '200'"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
