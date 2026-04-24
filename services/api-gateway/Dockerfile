# Stage 1 — build
# Build context must be the repo root so the parent pom.xml is accessible.
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and parent pom first for dependency caching
COPY services/api-gateway/.mvn/ .mvn/
COPY services/api-gateway/mvnw ./mvnw
RUN chmod +x mvnw

COPY pom.xml ./pom.xml
COPY services/api-gateway/pom.xml ./services/api-gateway/pom.xml
RUN ./mvnw -f services/api-gateway/pom.xml dependency:go-offline -q

# Copy source and build executable jar
COPY services/api-gateway/src ./services/api-gateway/src
RUN ./mvnw -f services/api-gateway/pom.xml package spring-boot:repackage -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=builder /build/services/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
