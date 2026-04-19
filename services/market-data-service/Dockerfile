# Stage 1 — build
# Build context must be the repo root so the parent pom.xml is accessible.
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and parent pom first for dependency caching
COPY services/market-data-service/.mvn/ .mvn/
COPY services/market-data-service/mvnw ./mvnw
RUN chmod +x mvnw

COPY pom.xml ./pom.xml
COPY services/market-data-service/pom.xml ./services/market-data-service/pom.xml
RUN ./mvnw -f services/market-data-service/pom.xml dependency:go-offline -q

# Copy source and build executable jar
COPY services/market-data-service/src ./services/market-data-service/src
RUN ./mvnw -f services/market-data-service/pom.xml package spring-boot:repackage -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=builder /build/services/market-data-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
