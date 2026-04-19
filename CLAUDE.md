# StockPulse — Claude Code Instructions

## Project overview
StockPulse is a real-time stock portfolio tracker built as a microservices
architecture for learning and portfolio purposes.
Target: Java/Finance missions — demonstrates Kafka streaming, microservices,
Kubernetes, ArgoCD GitOps, and Angular 21.

## Stack
- Java 21 (virtual threads enabled), Spring Boot 3.5.x
- Angular 21, NgRx, Chart.js
- Apache Kafka 3.8.x, PostgreSQL 16, TimescaleDB
- Docker, Kubernetes (Helm), ArgoCD
- GitHub Actions CI/CD, AWS SES / S3

## Monorepo structure
- `services/market-data-service` — polls Alpha Vantage, publishes to Kafka topic `stock.prices`
- `services/portfolio-service`   — manages portfolios, consumes Kafka, calculates P&L
- `services/alert-service`       — evaluates price alerts, sends emails via AWS SES
- `services/api-gateway`         — Spring Cloud Gateway, JWT auth, routing
- `frontend/`                    — Angular 21 SPA
- `infra/docker/`                — docker-compose files
- `infra/k8s/`                   — Helm charts
- `infra/argocd/`                — ArgoCD application manifests
- `.github/workflows/`           — GitHub Actions pipelines

## Service communication
- All external traffic enters through api-gateway (port 8080)
- Gateway handles JWT validation and routing — no Eureka, service discovery
  is handled by Kubernetes DNS in production and static URLs in local dev
- Inter-service communication is event-driven via Kafka only
- Services never call each other via HTTP — Kafka events only
- WebSocket connections are proxied through the gateway to portfolio-service

## Local routing (docker-compose)
- api-gateway → market-data-service: http://localhost:8081
- api-gateway → portfolio-service: http://localhost:8082
- api-gateway → alert-service: http://localhost:8083

## Code conventions (Java)
- Java 21 records for DTOs, value objects, @ConfigurationProperties
- JPA entities use Lombok: @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
- No manual getters/setters/constructors outside of entities
- Virtual threads: `spring.threads.virtual.enabled=true` in all services
- Package structure: `com.stockpulse.{service}.{domain|application|infrastructure|api}`
- Flyway for all DB migrations — never modify an existing migration file
- DLQ pattern on all Kafka consumers (dead letter queue)
- Unit tests: JUnit 5 + Mockito
- Integration tests: @SpringBootTest + Testcontainers
- Kafka consumer tests: EmbeddedKafka

## Code conventions (Angular)
- Angular 21 — standalone components only, no NgModules
- Signals for local/component state
- NgRx only for global shared state (portfolio, auth)
- Typed reactive forms exclusively
- HTTP calls only in service layer — never in components

## Rules
- Do not modify files in `infra/` unless explicitly asked
- Do not change Flyway migration files already committed
- Do not add Maven/npm dependencies without explicit approval

## Git conventions
- Use conventional commits: feat|fix|chore|refactor|test|docs(scope): message
- Scope = service name: market-data-service, portfolio-service, alert-service, api-gateway, frontend, infra
- Examples:
    - feat(market-data-service): add Kafka producer for stock prices
    - fix(portfolio-service): handle null price in P&L calculation
    - chore(infra): update docker-compose Kafka listeners
- Always commit per feature/layer — never one big commit for everything
- NEVER commit or push automatically
- NEVER run git commit or git push without explicit user instruction
- When work is complete, summarize what was done and wait for user to commit manually
- Run tests before committing when possible
