# StockPulse — Claude Code Instructions

## Project overview
StockPulse is a real-time stock portfolio tracker built as a microservices
architecture for learning and portfolio purposes.
Target: Java/Finance missions — demonstrates Kafka streaming, microservices,
Kubernetes, ArgoCD GitOps, and Angular 21.

## Stack
- Java 21 (virtual threads enabled), Spring Boot 3.5.x
- Angular 21, Angular Material, NgRx, Chart.js
- Apache Kafka 3.8.x, PostgreSQL 16, TimescaleDB
- Docker, Kubernetes (Helm), ArgoCD
- GitHub Actions CI/CD, AWS SES / S3

## Monorepo structure
- `services/market-data-service`  — polls Alpha Vantage, publishes to Kafka topic `stock.prices`
- `services/portfolio-service`    — manages portfolios, consumes Kafka, calculates P&L
- `services/alert-service`        — evaluates price alerts, DLQ + retry, sends emails via AWS SES
- `services/analytics-service`    — Kafka Streams, tumbling windows 5min, spike detection +3%
- `services/api-gateway`          — Spring Cloud Gateway, JWT auth, routing
- `frontend/`                     — Angular 21 SPA with Angular Material
- `infra/docker/`                 — docker-compose files
- `infra/k8s/`                    — Helm charts
- `infra/argocd/`                 — ArgoCD application manifests
- `.github/workflows/`            — GitHub Actions pipelines

## Service ports
- api-gateway          : 8080
- market-data-service  : 8081
- portfolio-service    : 8082
- alert-service        : 8083
- analytics-service    : 8084
- frontend             : 4200

## Service communication
- All external traffic enters through api-gateway (port 8080)
- Gateway handles JWT validation and routing — no Eureka
- Service discovery: Kubernetes DNS in production, static URLs in local dev
- Inter-service communication is event-driven via Kafka only
- Services NEVER call each other via HTTP — Kafka events only
- WebSocket connections proxied through gateway to portfolio-service

## Kafka topics
| Topic | Producer | Consumers | Partitions |
|---|---|---|---|
| stock.prices | market-data-service | portfolio-service, alert-service, analytics-service | 5 |
| stock.prices.analytics | analytics-service | (frontend via websocket) | 1 |
| stock.prices.spikes | analytics-service | alert-service | 1 |
| alerts.triggered | alert-service | (email sink) | 1 |
| portfolio.updates | portfolio-service | (websocket sink) | 5 |
| portfolio.dlq | — | dead letter | 1 |
| stock.prices.dlt | — | dead letter (after 3 retries) | 1 |
| stock.prices-retry-1000 | Spring Retry | alert-service retry 1s | 1 |
| stock.prices-retry-2000 | Spring Retry | alert-service retry 2s | 1 |
| stock.prices-retry-4000 | Spring Retry | alert-service retry 4s | 1 |

## Kafka advanced patterns implemented
- Custom partitioner: StockPricePartitioner (Finance US=0, Tech US=1, Finance EU=2, overflow=3-4)
- Manual offset acknowledgment: AckMode.MANUAL in portfolio-service
- Kafka Streams: tumbling 5-min windows + spike detection in analytics-service
- Retry + DLQ: @RetryableTopic (1s→2s→4s backoff) + @DltHandler in alert-service

## Code conventions (Java)
- Java 21 records for DTOs, value objects, @ConfigurationProperties
- JPA entities use Lombok: @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @EqualsAndHashCode
- No manual getters/setters/constructors outside of entities
- Virtual threads: `spring.threads.virtual.enabled=true` in all services
- Package structure: `com.stockpulse.{service}.{domain|application|infrastructure|api}`
- Flyway for all DB migrations — NEVER modify an existing migration file
- Unit tests: JUnit 5 + Mockito
- Integration tests: @SpringBootTest + Testcontainers
- Kafka Streams tests: TopologyTestDriver (no real Kafka needed)
- Always add Javadoc on public service methods

## Code conventions (Angular)
- Angular 21 — standalone components only, no NgModules
- Angular Material for all UI components (mat-table, mat-card, mat-toolbar, mat-dialog etc.)
- Signals for local/component state
- NgRx only for global shared state (portfolio, auth)
- Typed reactive forms exclusively
- HTTP calls only in service layer — never in components
- WebSocket (STOMP) for real-time price updates

## Rules
- Do not modify files in `infra/` unless explicitly asked
- Do not change Flyway migration files already committed
- Do not add Maven/npm dependencies without explaining the reason
- Always add Javadoc on public service methods
- Every new feature needs at least one unit test
- ALWAYS run `git add -A` after creating or modifying any file
- Never leave untracked files — stage every new file immediately after creation

## Git conventions
- Use conventional commits: feat|fix|chore|refactor|test|docs(scope): message
- Scope = service name: market-data-service, portfolio-service, alert-service,
  analytics-service, api-gateway, frontend, infra
- Examples:
  - feat(analytics-service): add Kafka Streams spike detection
  - fix(portfolio-service): fix manual ack on consumer error
  - chore(infra): add analytics-service to docker-compose
- Always commit per feature/layer — never one big commit for everything
- NEVER commit or push automatically
- NEVER run git commit or git push without explicit user instruction
- When work is complete, summarize changes and wait for user to commit
- Run tests before committing when possible

## Local dev commands
\`\`\`bash
# Start all infra
docker compose up -d

# Build specific service
docker compose up -d --build market-data-service

# Run tests for a service
cd services/market-data-service && ./mvnw test

# Angular dev server
cd frontend && ng serve

# View logs
docker compose logs -f [service-name]
\`\`\`

## Database connections (local)
| Service | Host | Port | DB |
|---|---|---|---|
| market-data-service | localhost | 5433 | market_data |
| portfolio-service | localhost | 5434 | portfolio |
| alert-service | localhost | 5435 | alerts |