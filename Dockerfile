# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    bash curl netcat-openbsd rlwrap && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar

# CLI command to connect to shell server
RUN printf '#!/bin/bash\nrlwrap nc localhost 6380\n' > /usr/local/bin/redis-cli && chmod +x /usr/local/bin/redis-cli

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]