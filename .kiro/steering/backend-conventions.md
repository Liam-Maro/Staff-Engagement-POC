# Backend Conventions

## Project Structure
```
backend/src/main/java/com/staffengagement/
├── employee/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── staff/
├── interaction/
├── task/
├── portfolio/
├── skills/
└── shared/          # cross-cutting concerns (config, exceptions, etc.)
```

## Coding Rules
- Java 21 — use records for DTOs, sealed interfaces where appropriate
- Each module exposes a public service interface; internals are package-private
- Controllers only handle HTTP concerns; delegate logic to services
- Use constructor injection (no field injection)
- Validate inputs at the controller layer using Jakarta Bean Validation annotations

## Database
- PostgreSQL with Spring Data JPA / Hibernate
- Entity classes annotated with JPA annotations in the `model` package
- Use Flyway or Liquibase for schema migrations (scripts in `src/main/resources/db/migration`)
- Each module prefixes its tables (e.g. `emp_`, `stf_`, `int_`, `tsk_`, `prt_`, `skl_`)

## REST API
- Base path: `/api`
- Module endpoints: `/api/employees`, `/api/staff`, `/api/interactions`, `/api/tasks`, `/api/portfolios`, `/api/skills`
- Use DTOs for request/response — never expose JPA entities directly
- Return appropriate HTTP status codes (201 for creation, 204 for deletion, etc.)

## Testing (HIGH PRIORITY)
- Unit tests: JUnit 5 + Mockito for services and logic
- Integration tests: @SpringBootTest with Testcontainers (PostgreSQL)
- API tests: MockMvc or WebTestClient for controller layer
- Aim for test coverage on all service-layer logic
- Test classes mirror the main source structure under `src/test/java`

## Error Handling
- Global exception handler via `@RestControllerAdvice`
- Consistent error response format across all modules
