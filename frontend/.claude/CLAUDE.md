# StockPulse Frontend — Claude Code Instructions

## Stack
- Angular 21, standalone components, signals
- Angular Material 21 (Azure/Blue theme)
- NgRx (store, effects, store-devtools)
- Chart.js for price charts
- STOMP/SockJS for WebSocket real-time prices
- SCSS for styles

## Structure
src/app/
├── core/
│   ├── services/     ← API calls, WebSocket, Auth
│   ├── models/       ← TypeScript interfaces
│   └── store/        ← NgRx global state
├── shared/
│   ├── components/   ← reusable UI components
│   └── pipes/        ← custom pipes
└── features/
    ├── auth/         ← login page
    ├── dashboard/    ← main dashboard
    ├── portfolio/    ← portfolio management
    └── alerts/       ← alert rules

## API endpoints (via gateway port 8080)
- POST /api/v1/auth/login
- GET  /api/v1/portfolios/{userId}
- GET  /api/v1/portfolios/{portfolioId}/positions
- POST /api/v1/portfolios/{portfolioId}/positions
- GET  /api/v1/alerts/{userId}
- POST /api/v1/alerts
- DELETE /api/v1/alerts/{alertId}

## Code conventions
- Standalone components only — no NgModules
- Signals for local state, NgRx for global state (auth, portfolio)
- Typed reactive forms — no template-driven forms
- HTTP calls only in service layer (core/services)
- Angular Material components for all UI elements
- Always add proper TypeScript types — no `any`
- Component file naming: feature-name.component.ts

## Testing
- Unit tests: Jasmine + Karma (already configured by Angular CLI)
- Every service must have a .spec.ts file
- Every component must have a .spec.ts file
- Test file naming: feature-name.component.spec.ts / service-name.service.spec.ts
- Use TestBed for component tests
- Use HttpClientTestingModule for service tests
- Mock all external dependencies
- Test coverage: services 80%+, components 60%+
- Run tests: ng test
- Run tests headless (CI): ng test --watch=false --browsers=ChromeHeadless

## Git rules
- NEVER commit or push automatically
- ALWAYS run git add -A after creating files
- Wait for explicit commit instruction