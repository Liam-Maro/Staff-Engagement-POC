# Frontend Conventions

## Project Structure
```
frontend/src/app/
├── employees/
│   ├── components/
│   ├── services/
│   └── models/
├── staff/
├── interactions/
├── tasks/
├── portfolio/
├── skills/
└── shared/          # shared components, pipes, guards, interceptors
```

## Coding Rules
- Angular 21 standalone components (no NgModules)
- Feature-based folder structure mirroring backend modules
- Use Angular Router with lazy-loaded routes per feature
- Services use HttpClient to call backend REST API
- Strongly type all API responses with TypeScript interfaces in `models/`

## State & Data
- Use Angular services with signals or RxJS for state as appropriate
- Keep components thin — business logic in services
- Use Angular reactive forms for form handling

## Testing (HIGH PRIORITY)
- Unit tests for services and components
- Use Angular TestBed for component tests
- Mock HTTP calls with HttpClientTestingModule
- Test files co-located with source (`*.spec.ts`)

## API Communication
- Base API URL configured via `environment.ts`
- All HTTP calls go through feature services (e.g. `EmployeeService`)
- Use interceptors for common concerns (error handling, auth headers when added)

## Styling
- CSS (default setup)
- Responsive design considered from the start
