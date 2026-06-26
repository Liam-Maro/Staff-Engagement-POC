# Implementation Plan: Interaction Module

## Overview

This plan extends the existing partial interaction module (basic entity, repository, controller with create/findAll/findById) into a fully-featured module with complete validation, update/delete operations, pagination with multi-criteria filtering, follow-up task spawning, proper exception handling, audit timestamps, and a full Angular frontend with list, detail, and form views.

## Tasks

- [x] 1. Enhance backend data model and migration
  - [x] 1.1 Create InteractionType enum and update Interaction entity
    - Create `InteractionType` enum in `interaction.model` package with values: CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL
    - Update `Interaction` entity: change `type` field from String to `@Enumerated(EnumType.STRING) InteractionType`, add `updatedAt` field with `@PrePersist` and `@PreUpdate` lifecycle callbacks, add `@Column(length = 5000)` to notes, ensure `createdAt` has `updatable = false`
    - _Requirements: 1.1, 1.5, 7.1, 7.2, 10.3, 10.5_

  - [x] 1.2 Create Liquibase migration for int_interactions table
    - Add Liquibase changeset in `src/main/resources/db/changelog/` that creates (or updates) the `int_interactions` table with: UUID PK, employee_id FK (ON DELETE RESTRICT), staff_id FK (ON DELETE RESTRICT), type VARCHAR(20) with CHECK constraint, notes TEXT with length check, occurred_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL
    - Add indexes on employee_id, staff_id, occurred_at DESC, and type
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 2. Implement backend DTOs and validation
  - [x] 2.1 Update CreateInteractionRequest with full validation
    - Convert to use `@NotNull InteractionType type` instead of `@NotBlank String type`
    - Add `@Size(max = 5000)` to notes field
    - Add `@PastOrPresent` to occurredAt field
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [x] 2.2 Create UpdateInteractionRequest DTO
    - Create record with `@NotNull InteractionType type`, `@Size(max = 5000) String notes`, `@NotNull @PastOrPresent LocalDateTime occurredAt`
    - _Requirements: 3.1, 3.3, 3.4, 3.6, 3.7_

  - [x] 2.3 Create CreateFollowUpTaskRequest DTO
    - Create record with `@NotBlank @Size(max = 255) String title`, `@Size(max = 2000) String description`, `@FutureOrPresent LocalDate dueDate`
    - _Requirements: 5.1, 5.3, 5.4, 5.5, 5.6_

  - [x] 2.4 Update InteractionResponse DTO to include updatedAt
    - Add `LocalDateTime updatedAt` field and update `InteractionType type` from String to enum
    - _Requirements: 7.3, 7.4_

- [x] 3. Implement backend exception handling
  - [x] 3.1 Create custom exception classes
    - Create `InteractionNotFoundException`, `InvalidDateRangeException`, `TaskCreationFailedException` in `interaction.exception` package
    - _Requirements: 2.4, 3.2, 4.2, 5.2, 6.7_

  - [x] 3.2 Create or update exception handler for interaction module
    - Add handler methods in the global `@RestControllerAdvice` (or create one if none exists) mapping: `InteractionNotFoundException` → 404, `InvalidDateRangeException` → 400, `TaskCreationFailedException` → 500, `MethodArgumentTypeMismatchException` → 400
    - _Requirements: 2.4, 2.7, 4.4, 6.5, 6.7_

- [x] 4. Implement backend repository with specifications
  - [x] 4.1 Update InteractionRepository to support pagination and specifications
    - Extend `JpaSpecificationExecutor<Interaction>` in addition to `JpaRepository`
    - Remove or keep legacy `findByEmployeeId` method as needed
    - _Requirements: 2.2, 6.1, 6.4, 6.6, 6.8_

  - [x] 4.2 Create InteractionSpecifications class for dynamic filtering
    - Create `InteractionSpecifications` in `interaction.repository` package with static methods: `hasEmployeeId(UUID)`, `hasType(InteractionType)`, `occurredAfter(LocalDateTime)`, `occurredBefore(LocalDateTime)`
    - Compose predicates with AND logic when multiple filters are provided
    - _Requirements: 6.4, 6.6, 6.8_

- [x] 5. Implement backend service layer (full logic)
  - [x] 5.1 Update InteractionService interface with complete API
    - Add methods: `update(UUID id, UpdateInteractionRequest)`, `delete(UUID id)`, `findAll(UUID employeeId, InteractionType type, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable)` returning `Page<InteractionResponse>`, `createFollowUpTask(UUID interactionId, CreateFollowUpTaskRequest)`
    - _Requirements: 2.2, 3.1, 4.1, 5.1, 6.1_

  - [x] 5.2 Implement InteractionServiceImpl create logic with employee verification
    - On create: validate employeeId exists via `EmployeeService.findById()`, validate staffId exists via StaffService, map DTO to entity, persist and return response
    - _Requirements: 1.1, 1.8, 1.9, 10.8_

  - [x] 5.3 Implement InteractionServiceImpl update logic
    - Find interaction by ID (throw InteractionNotFoundException if missing), update only type/notes/occurredAt (ignore employeeId/staffId), persist and return updated response
    - _Requirements: 3.1, 3.2, 3.5_

  - [x] 5.4 Implement InteractionServiceImpl delete logic
    - Find interaction by ID (throw InteractionNotFoundException if missing), delete from repository
    - _Requirements: 4.1, 4.2_

  - [x] 5.5 Implement InteractionServiceImpl paginated findAll with filters
    - Accept optional employeeId, type, fromDate, toDate parameters; validate fromDate ≤ toDate (throw InvalidDateRangeException); compose specifications; query with Pageable sorted by occurredAt DESC; cap page size at 100; default page=0 size=20; map results to InteractionResponse page
    - _Requirements: 2.2, 2.3, 6.1, 6.2, 6.3, 6.4, 6.6, 6.7, 6.8_

  - [x] 5.6 Implement InteractionServiceImpl follow-up task creation
    - Find interaction by ID (throw InteractionNotFoundException if missing), build CreateTaskRequest with interaction's employeeId and interactionId, delegate to `TaskService.createTask(...)`, catch task module failures and throw TaskCreationFailedException
    - _Requirements: 5.1, 5.2, 5.7, 5.8, 5.9_

- [x] 6. Update backend controller with full endpoint set
  - [x] 6.1 Update InteractionController with pagination, update, delete, and task endpoints
    - `GET /api/interactions` — accept employeeId, type, fromDate, toDate, page, size query params; delegate to paginated service method; return `Page<InteractionResponse>` with HTTP 200
    - `PUT /api/interactions/{id}` — accept `@Valid UpdateInteractionRequest`; return updated response with HTTP 200
    - `DELETE /api/interactions/{id}` — call service.delete; return HTTP 204
    - `POST /api/interactions/{id}/tasks` — accept `@Valid CreateFollowUpTaskRequest`; return TaskResponse with HTTP 201
    - _Requirements: 2.5, 2.6, 3.7, 4.3, 5.8, 6.1, 6.2, 6.3_

- [x] 7. Checkpoint - Backend compilation and basic validation
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Backend unit tests
  - [x] 8.1 Write unit tests for InteractionServiceImpl
    - Test create with valid data, update with valid data, update preserving employeeId/staffId, delete, findById not found, paginated findAll, invalid date range, follow-up task creation, task delegation failure
    - Mock InteractionRepository, EmployeeService, TaskService with Mockito
    - _Requirements: 1.1, 3.1, 3.5, 4.1, 5.1, 5.7, 6.1, 6.7_

  - [x] 8.2 Write property test: Create-retrieve round-trip (Property 1)
    - **Property 1: Create-retrieve round-trip**
    - Use jqwik to generate valid CreateInteractionRequest instances; verify create then findById returns matching field values
    - **Validates: Requirements 1.1, 2.1**

  - [x] 8.3 Write property test: Invalid interaction type rejection (Property 2)
    - **Property 2: Invalid interaction type rejection**
    - Use jqwik to generate arbitrary strings not in the valid type set; verify HTTP 400 rejection
    - **Validates: Requirements 1.5, 3.3**

  - [x] 8.4 Write property test: Future occurredAt rejection (Property 3)
    - **Property 3: Future occurredAt rejection**
    - Use jqwik to generate LocalDateTime values strictly in the future; verify HTTP 400 rejection
    - **Validates: Requirements 1.7, 3.4, 10.4**

  - [x] 8.5 Write property test: employeeId and staffId immutability on update (Property 4)
    - **Property 4: employeeId and staffId immutability on update**
    - Use jqwik to generate update requests with varying employeeId/staffId; verify response retains original values
    - **Validates: Requirements 3.5**

  - [x] 8.6 Write property test: createdAt immutability and updatedAt advancement (Property 5)
    - **Property 5: createdAt immutability and updatedAt advancement**
    - Use jqwik to apply N updates; verify createdAt unchanged and updatedAt ≥ previous value
    - **Validates: Requirements 7.1, 7.2**

  - [x] 8.7 Write property test: Field length limit enforcement (Property 7)
    - **Property 7: Field length limit enforcement**
    - Use jqwik to generate strings exceeding 5000 chars (notes), 255 chars (title), 2000 chars (description); verify HTTP 400
    - **Validates: Requirements 3.6, 5.4, 5.5, 10.6**

  - [x] 8.8 Write property test: Pagination metadata consistency (Property 8)
    - **Property 8: Pagination metadata consistency**
    - Use jqwik to generate varying record counts and page sizes; verify totalElements, totalPages, content.length, and page number
    - **Validates: Requirements 6.1**

  - [x] 8.9 Write property test: Page size capped at 100 (Property 9)
    - **Property 9: Page size capped at 100**
    - Use jqwik to generate size values > 100; verify response content never exceeds 100 elements
    - **Validates: Requirements 6.3**

  - [x] 8.10 Write property test: Combined filter AND semantics (Property 10)
    - **Property 10: Combined filter AND semantics**
    - Use jqwik to generate filter combinations; verify every result satisfies ALL provided predicates
    - **Validates: Requirements 6.4, 6.6, 6.8, 2.2**

  - [x] 8.11 Write property test: List ordering by occurredAt descending (Property 11)
    - **Property 11: List ordering by occurredAt descending**
    - Use jqwik to generate interaction sets; verify results are in non-ascending occurredAt order
    - **Validates: Requirements 2.2, 2.3, 6.1**

  - [x] 8.12 Write property test: Notes preview truncation (Property 14)
    - **Property 14: Notes preview truncation**
    - Use jqwik to generate notes strings of varying lengths; verify truncation at 100 chars with "..." appended for longer strings
    - **Validates: Requirements 8.2**

  - [x] 8.13 Write property test: createdAt equals updatedAt on fresh creation (Property 15)
    - **Property 15: createdAt equals updatedAt on fresh creation**
    - Use jqwik to generate valid create requests; verify response has createdAt == updatedAt
    - **Validates: Requirements 7.4**

- [x] 9. Backend integration tests
  - [x] 9.1 Write integration tests for InteractionController
    - Use `@SpringBootTest` with Testcontainers PostgreSQL (extend `BaseIntegrationTest`)
    - Test full request lifecycle: create (201), findById (200), findAll with pagination (200), update (200), delete (204), follow-up task creation (201)
    - Test error scenarios: not found (404), validation failures (400), invalid UUID (400), invalid date range (400)
    - Test FK RESTRICT constraint prevents employee/staff deletion when interactions exist
    - _Requirements: 1.10, 2.5, 2.6, 3.7, 4.3, 5.8, 10.1, 10.2, 10.7_

- [x] 10. Checkpoint - All backend tests green
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Implement Angular frontend models and service
  - [x] 11.1 Create TypeScript interfaces and models
    - Create `frontend/src/app/interactions/models/interaction.model.ts` with `InteractionType`, `InteractionResponse`, `CreateInteractionRequest`, `UpdateInteractionRequest`, `CreateFollowUpTaskRequest`, `PageResponse<T>` interfaces
    - _Requirements: 8.2, 9.4_

  - [x] 11.2 Create InteractionService HTTP client
    - Create `frontend/src/app/interactions/services/interaction.service.ts` with methods: `getAll(params)`, `getById(id)`, `create(request)`, `update(id, request)`, `delete(id)`, `createFollowUpTask(id, request)`
    - Use HttpClient, strongly-typed return observables, parameterised query string construction for filters/pagination
    - _Requirements: 2.5, 2.6, 3.7, 4.3, 5.8, 6.1_

- [x] 12. Implement Angular InteractionListComponent
  - [x] 12.1 Create InteractionListComponent with pagination and filtering
    - Create standalone component at `frontend/src/app/interactions/components/interaction-list/`
    - Display interactions in a table/card layout: type, employee name, occurred date, notes preview (truncated to 100 chars with "...")
    - Implement pagination controls (page number, total pages, next/previous buttons, default size 20)
    - Implement filter controls: employee searchable dropdown, interaction type multi-select
    - Handle loading state (spinner), error state (error message + retry button), empty state (no results message)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

- [x] 13. Implement Angular InteractionDetailComponent
  - [x] 13.1 Create InteractionDetailComponent
    - Create standalone component at `frontend/src/app/interactions/components/interaction-detail/`
    - Fetch interaction by ID from route param, display all fields: type, employee name, staff member name, occurred date, full notes, createdAt, updatedAt
    - Provide "Create Follow-up Task" button that navigates to task form pre-populated with interaction context
    - Handle 404 (not found message with link back to list), 5xx/network errors (error message with retry)
    - _Requirements: 9.1, 9.2, 9.3, 9.10, 9.11_

- [x] 14. Implement Angular InteractionFormComponent
  - [x] 14.1 Create InteractionFormComponent with reactive form
    - Create standalone component at `frontend/src/app/interactions/components/interaction-form/`
    - Angular reactive form with fields: employee (searchable dropdown, required), type (select from enum values, required), occurredAt (date-time picker, required, not in future), notes (textarea, max 5000 chars)
    - Support both create (POST) and edit (PUT) modes — disable employeeId/staffId on edit
    - Client-side validation with inline error messages, form submit disabled until valid
    - On success: navigate to `/interactions`
    - On HTTP 400: display server validation messages inline
    - On HTTP 5xx: display general error with retry option
    - _Requirements: 9.4, 9.5, 9.6, 9.7, 9.8, 9.9_

- [x] 15. Wire Angular routing and navigation
  - [x] 15.1 Add interaction routes and lazy loading
    - Add routes to `app.routes.ts`: `/interactions` → InteractionListComponent, `/interactions/create` → InteractionFormComponent, `/interactions/:id` → InteractionDetailComponent, `/interactions/:id/edit` → InteractionFormComponent (edit mode)
    - All routes lazy-loaded, guarded by `authGuard`
    - Add navigation link to shared layout/sidebar
    - _Requirements: 8.1, 9.1_

- [x] 16. Checkpoint - Frontend compilation and manual smoke check
  - Ensure all tests pass, ask the user if questions arise.

- [x] 17. Frontend unit tests
  - [x] 17.1 Write unit tests for InteractionService
    - Test all HTTP methods (GET, POST, PUT, DELETE) with correct URL construction, query parameters, and request bodies
    - Use HttpClientTestingModule to mock HTTP calls
    - _Requirements: 2.5, 2.6, 3.7, 4.3, 5.8_

  - [x] 17.2 Write unit tests for InteractionListComponent
    - Test initial data load, pagination navigation, filter application, loading/error/empty states rendering
    - Test notes truncation display logic (> 100 chars → truncated with "...")
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - [x] 17.3 Write unit tests for InteractionDetailComponent
    - Test data display, error states (404, 5xx), retry behavior, follow-up task button navigation
    - _Requirements: 9.1, 9.2, 9.3, 9.10, 9.11_

  - [x] 17.4 Write unit tests for InteractionFormComponent
    - Test reactive form validation (required fields, max lengths, date constraints), create vs edit mode, submission flow, error handling (400, 5xx)
    - _Requirements: 9.4, 9.5, 9.6, 9.7, 9.8, 9.9_

- [x] 18. Final checkpoint - All tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- The existing basic implementation (entity, repository, controller, service) will be refactored in place — no greenfield rewrites
- Property tests use jqwik (already configured in the project as evidenced by `.jqwik-database`)
- Integration tests extend `BaseIntegrationTest` which provides Testcontainers PostgreSQL
- Frontend follows existing patterns: standalone components, lazy-loaded routes, Angular reactive forms
- The Task module is assumed to expose a `TaskService` interface for follow-up task delegation

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1", "2.2", "2.3", "2.4", "3.1"] },
    { "id": 1, "tasks": ["3.2", "4.1", "4.2"] },
    { "id": 2, "tasks": ["5.1"] },
    { "id": 3, "tasks": ["5.2", "5.3", "5.4", "5.5", "5.6"] },
    { "id": 4, "tasks": ["6.1"] },
    { "id": 5, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6", "8.7", "8.8", "8.9", "8.10", "8.11", "8.12", "8.13"] },
    { "id": 6, "tasks": ["9.1"] },
    { "id": 7, "tasks": ["11.1"] },
    { "id": 8, "tasks": ["11.2"] },
    { "id": 9, "tasks": ["12.1", "13.1", "14.1"] },
    { "id": 10, "tasks": ["15.1"] },
    { "id": 11, "tasks": ["17.1", "17.2", "17.3", "17.4"] }
  ]
}
```
