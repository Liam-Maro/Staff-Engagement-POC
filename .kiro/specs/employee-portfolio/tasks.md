# Implementation Plan: Employee Portfolio

## Overview

Refactor the portfolio module from a single generic `PortfolioItem` entity into dedicated entities (education, projects, links) with full CRUD operations, a unified REST controller, Liquibase migrations, and an Angular frontend view. The Skills section is READ-ONLY and sourced from the Skills Register module (`skl_skills` table via `SkillService`). Skills CRUD (create, update, delete) is managed by the skills module at `/api/skills`. The implementation follows the existing modular monolith patterns (public service interface, package-private internals, record-based DTOs, Jakarta Bean Validation).

## Tasks

- [x] 1. Define entity models and repositories
  - [x] 1.1 Create the four JPA entity classes (`PortfolioSkill`, `PortfolioEducation`, `PortfolioProject`, `PortfolioLink`) in the `model` package with proper JPA annotations, UUID primary keys, `@PrePersist` for `createdAt`, and a `StringListConverter` for the technologies JSONB column on `PortfolioProject`
    - Remove or replace the existing `PortfolioItem.java` entity
    - Follow the entity mappings defined in the design document (table names: `prt_skills`, `prt_education`, `prt_projects`, `prt_links`)
    - Include `employeeId` as a UUID column (not a JPA relationship) on each entity
    - **(Note: `PortfolioSkill` entity is kept for backward compatibility but is no longer used by the portfolio service. Skills are sourced from the Skills Register module via `SkillService`.)**
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.6, 8.7_

  - [x] 1.2 Create the four Spring Data JPA repository interfaces (`PortfolioSkillRepository`, `PortfolioEducationRepository`, `PortfolioProjectRepository`, `PortfolioLinkRepository`) in the `repository` package
    - Each repository is package-private
    - Include custom query methods: `findByEmployeeIdOrderByNameAsc` for skills, `findByEmployeeIdOrderByGraduationDateDesc` for education, `findByEmployeeIdOrderByStartDateDesc` for projects, `findByEmployeeId` for links
    - Remove or replace the existing `PortfolioItemRepository.java`
    - **(Note: `PortfolioSkillRepository` is kept for backward compatibility but is no longer used by the portfolio service. Skills are read from the Skills Register module via `SkillService`.)**
    - _Requirements: 1.2, 2.2, 3.2, 4.2_

  - [x] 1.3 Create all request and response DTO records in the `dto` package
    - Request DTOs: `CreateEducationRequest`, `UpdateEducationRequest`, `CreateProjectRequest`, `UpdateProjectRequest`, `CreateLinkRequest`, `UpdateLinkRequest`
    - Response DTOs: `PortfolioSkillResponse` (now maps from SkillService response, includes `yearsExperience` and `projectCount`), `EducationResponse`, `ProjectResponse`, `LinkResponse`, `FullPortfolioResponse`
    - Add Jakarta Bean Validation annotations (`@NotBlank`, `@Size`, `@NotNull`, `@NotEmpty`) on request DTOs
    - Remove or replace the existing `CreatePortfolioItemRequest.java` and `PortfolioItemResponse.java`
    - **(Note: `CreatePortfolioSkillRequest` and `UpdatePortfolioSkillRequest` are no longer used — skills CRUD is handled by the Skills Register module at `/api/skills`.)**
    - _Requirements: 2.5, 2.6, 3.1, 3.5, 3.9, 3.10, 4.5, 4.6, 4.9_

  - [x] 1.4 Create custom validation annotations and validators
    - `@ValidProficiency` — validates proficiency is one of BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    - `@ValidUrl` — validates URL has http or https scheme and is well-formed
    - `@DateRangeValid` — class-level constraint on project request DTOs ensuring startDate ≤ endDate when endDate is not null
    - Place in a `validation` sub-package under `portfolio`
    - _Requirements: 1.7, 3.6, 4.7_

- [x] 2. Implement service layer
  - [x] 2.1 Redefine the `PortfolioService` public interface with CRUD methods for education, projects, and links, read-only `getSkillsByEmployee` (delegating to SkillService), plus `getFullPortfolio(UUID employeeId)`
    - Follow the interface definition from the design document exactly
    - Keep the interface public (module boundary contract)
    - **(Note: `createSkill`, `updateSkill`, `deleteSkill` methods removed — skills CRUD is managed by the Skills Register module)**
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 5.1_

  - [x] 2.2 Implement `PortfolioServiceImpl` with full CRUD logic for education, projects, and links, and read-only skills via SkillService
    - Package-private class annotated with `@Service`
    - Constructor-inject repositories (`PortfolioEducationRepository`, `PortfolioProjectRepository`, `PortfolioLinkRepository`), `EmployeeRepository` (from employee module), and `SkillService` (from skills module)
    - `getSkillsByEmployee` delegates to `SkillService` and maps responses to `PortfolioSkillResponse`
    - Validate employee existence before creation operations (throw `EntityNotFoundException` if not found)
    - Map entities to response DTOs and vice versa
    - Throw `EntityNotFoundException` for update/delete of non-existent items
    - Implement ordering: education by graduation date descending, projects by start date descending
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 2.7, 3.1, 3.2, 3.3, 3.4, 3.7, 3.8, 4.1, 4.2, 4.3, 4.4, 4.8, 5.1, 5.2, 5.3, 8.5_

- [x] 3. Implement controller layer
  - [x] 3.1 Rewrite `PortfolioController` with REST endpoints for skills (GET only), education, projects, links, and full portfolio retrieval
    - Package-private class annotated with `@RestController` and `@RequestMapping("/api/portfolios")`
    - Use `@Valid` on request bodies for Jakarta Bean Validation
    - Return correct HTTP status codes: 201 for creation, 200 for retrieval/update, 204 for deletion
    - Use path structure: `GET /{employeeId}/skills` (read-only), `POST/GET /{employeeId}/education`, `PUT/DELETE /education/{educationId}`, etc.
    - Skills POST/PUT/DELETE endpoints removed — handled by Skills Register module at `/api/skills`
    - Delegate all logic to `PortfolioService`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [x] 4. Checkpoint - Backend compiles and wires correctly
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Create Liquibase database migration
  - [x] 5.1 Create a new Liquibase changeset `008-refactor-portfolio-tables.xml` that drops the old `prt_portfolio_items` table and creates the four new tables (`prt_skills`, `prt_education`, `prt_projects`, `prt_links`) with proper columns, constraints, foreign keys to `emp_employees(id)`, and CHECK constraint on proficiency
    - Follow the schema definitions from the design document
    - Add the changeset include to `db.changelog-master.xml`
    - Use naming convention consistent with existing changesets (e.g. `008-*`)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.6, 8.7_

- [x] 6. Implement frontend portfolio feature
  - [x] 6.1 Create TypeScript model interfaces in `frontend/src/app/portfolio/models/`
    - Define `PortfolioSkill`, `Education`, `Project`, `PortfolioLink`, `FullPortfolio`, and `ProficiencyLevel` interfaces matching the design
    - _Requirements: 7.2, 7.3, 7.4, 7.5, 7.6_

  - [x] 6.2 Create `PortfolioService` in `frontend/src/app/portfolio/services/` using `HttpClient` to call all `/api/portfolios` endpoints
    - Method to fetch full portfolio: `getFullPortfolio(employeeId: string): Observable<FullPortfolio>`
    - Use signals or RxJS for state as per project conventions
    - _Requirements: 7.2_

  - [x] 6.3 Create `PortfolioViewComponent` as an Angular 21 standalone component in `frontend/src/app/portfolio/components/portfolio-view/`
    - Render four sections: Skills, Education, Projects, Links
    - Display loading indicator while fetching data
    - Display "Portfolio not found" for 404 responses
    - Display error message with retry option for network/server errors
    - Display "Ongoing" when project end date is null
    - Render links as clickable hyperlinks opening in a new tab (`target="_blank"`, `rel="noopener noreferrer"`)
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

  - [x] 6.4 Register the portfolio route in `app.routes.ts` with lazy loading under the authenticated layout
    - Path: `portfolio/:employeeId`
    - Protected by `authGuard`
    - _Requirements: 7.1_

- [x] 7. Checkpoint - Full stack compiles and frontend renders
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Write backend unit tests
  - [ ]* 8.1 Write JUnit 5 + Mockito unit tests for `PortfolioServiceImpl` covering CRUD operations for education, projects, and links, and SkillService integration for skills
    - Test successful creation, update, deletion for education, projects, and links
    - Test `getSkillsByEmployee` correctly delegates to SkillService and maps responses
    - Test employee-not-found scenario during creation
    - Test entity-not-found scenario during update/delete
    - Test ordering (education by grad date desc, projects by start date desc)
    - Test full portfolio aggregation (including skills from SkillService)
    - Place in `src/test/java/com/staffengagement/portfolio/service/`
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 2.7, 3.1, 3.2, 3.3, 3.4, 3.7, 4.1, 4.2, 4.3, 4.4, 4.8, 5.1, 5.2, 5.3, 8.5_

- [ ] 9. Write backend API tests
  - [ ]* 9.1 Write MockMvc API tests for `PortfolioController` covering endpoint routing, validation, and HTTP status codes
    - Test GET endpoint for skills (read-only from SkillService)
    - Test all CRUD endpoints for each sub-resource (education, projects, links)
    - Test full portfolio retrieval
    - Test validation error responses (blank fields, malformed URL, date constraint)
    - Test 404 responses for non-existent entities
    - Test 201/200/204 status codes for create/retrieve-update/delete
    - Verify that skills POST/PUT/DELETE endpoints do not exist on portfolio controller
    - Place in `src/test/java/com/staffengagement/portfolio/controller/`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 2.5, 2.6, 3.5, 3.6, 3.9, 3.10, 4.5, 4.6, 4.7, 4.9_

- [ ] 10. Write backend integration tests
  - [ ]* 10.1 Write integration tests using `@SpringBootTest` + Testcontainers (PostgreSQL) for full round-trip verification
    - Test full CRUD round-trip through controller → service → repository → PostgreSQL for education, projects, and links
    - Test skills retrieval via SkillService integration
    - Verify Liquibase migration creates schema correctly
    - Verify foreign key constraint enforcement (employeeId references real employee)
    - Verify JSONB column serialization/deserialization for technologies list
    - Verify ordering queries with real database
    - Place in `src/test/java/com/staffengagement/portfolio/`
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 4.1, 4.2, 5.1, 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 11. Write property-based tests
  - [ ]* 11.1 Write jqwik property test for creation round-trip (Property 1)
    - **Property 1: Portfolio item creation round-trip**
    - For education, projects, and links, creating an item and retrieving by employeeId yields a collection containing the submitted values with a non-null UUID
    - Use `@Property(tries = 100)` and custom generators for valid inputs
    - Place in `src/test/java/com/staffengagement/portfolio/`
    - **Validates: Requirements 2.1, 3.1, 4.1**

  - [ ]* 11.2 Write jqwik property test for deletion (Property 2)
    - **Property 2: Portfolio item deletion removes item**
    - For education, projects, and links, deleting an item and listing should not contain the deleted item
    - **Validates: Requirements 2.4, 3.4, 4.4**

  - [ ]* 11.3 Write jqwik property test for update round-trip (Property 3)
    - **Property 3: Portfolio item update round-trip**
    - For education, projects, and links, updating an item preserves the original ID and employeeId while reflecting new values
    - **Validates: Requirements 2.3, 3.3, 4.3**

  - [ ]* 11.4 Write jqwik property test for skills ordering (Property 4)
    - **Property 4: Skills listing is ordered by name ascending**
    - For any set of skills from the Skills Register, listing via portfolio returns them in lexicographic ascending order by name
    - **Note:** This validates the SkillService integration ordering, not direct CRUD
    - **Validates: Requirements 1.2**

  - [ ]* 11.5 Write jqwik property test for education ordering (Property 5)
    - **Property 5: Education listing is ordered by graduation date descending**
    - For any set of education records, listing returns them ordered by graduation date descending
    - **Validates: Requirements 2.2**

  - [ ]* 11.6 Write jqwik property test for projects ordering (Property 6)
    - **Property 6: Projects listing is ordered by start date descending**
    - For any set of project records, listing returns them ordered by start date descending
    - **Validates: Requirements 3.2**

  - [ ]* 11.7 Write jqwik property test for project date constraint (Property 7)
    - **Property 7: Project date constraint — start must not be after end**
    - For any date pair where start > end, submission should be rejected with a validation error
    - **Validates: Requirements 3.6**

  - [ ]* 11.8 Write jqwik property test for malformed URL rejection (Property 8)
    - **Property 8: Malformed URLs are rejected**
    - For any string without a valid http/https scheme, submission should be rejected
    - **Validates: Requirements 4.7**

  - [ ]* 11.9 Write jqwik property test for full portfolio aggregation (Property 9)
    - **Property 9: Full portfolio aggregation contains all items**
    - For any combination of items across all sections (skills from SkillService + education, projects, links), fetching the full portfolio contains exactly all items
    - **Validates: Requirements 5.1**

  - [ ]* 11.10 Write jqwik property test for non-existent employeeId (Property 10)
    - **Property 10: Non-existent employeeId rejects creation**
    - For any UUID not corresponding to an existing employee, creation of education, project, or link should result in a not-found error
    - **Validates: Requirements 8.4**

  **(Superseded: The following property tests are no longer needed in the portfolio module — validation is handled by the Skills Register module)**
  - ~~Property: Blank or whitespace-only skill names are rejected~~ (Superseded: skills now sourced from Skills Register module)
  - ~~Property: Invalid proficiency levels are rejected~~ (Superseded: skills now sourced from Skills Register module)

- [ ] 12. Write frontend tests
  - [ ]* 12.1 Write Angular unit tests for `PortfolioService` verifying HTTP calls to all endpoints
    - Mock HTTP calls with `HttpClientTestingModule`
    - Test that correct URLs, methods, and payloads are used
    - Place in `frontend/src/app/portfolio/services/portfolio.service.spec.ts`
    - _Requirements: 7.2_

  - [ ]* 12.2 Write Angular unit tests for `PortfolioViewComponent` verifying rendering of all four sections, loading state, error state, and retry behaviour
    - Use TestBed with mocked `PortfolioService`
    - Verify "Ongoing" displayed when endDate is null
    - Verify links open in new tab
    - Verify loading indicator shown while fetching
    - Verify error message with retry on failure
    - Place in `frontend/src/app/portfolio/components/portfolio-view/portfolio-view.component.spec.ts`
    - _Requirements: 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

- [x] 13. Final checkpoint - All tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests validate specific examples and edge cases
- The existing `PortfolioItem`, `PortfolioItemRepository`, `CreatePortfolioItemRequest`, and `PortfolioItemResponse` files should be removed/replaced during tasks 1.1–1.3
- The new Liquibase changeset (008) drops the old `prt_portfolio_items` table before creating the four new tables
- **Skills are sourced read-only from the Skills Register module (`SkillService`). The `prt_skills` table and `PortfolioSkillRepository` are kept for backward compatibility but are unused by the portfolio service.**
- **Skills CRUD (create, update, delete) is managed by the skills module at `/api/skills` — the portfolio module only reads skills**

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.4"] },
    { "id": 1, "tasks": ["1.2", "1.3"] },
    { "id": 2, "tasks": ["2.1"] },
    { "id": 3, "tasks": ["2.2"] },
    { "id": 4, "tasks": ["3.1", "5.1"] },
    { "id": 5, "tasks": ["6.1"] },
    { "id": 6, "tasks": ["6.2", "6.4"] },
    { "id": 7, "tasks": ["6.3"] },
    { "id": 8, "tasks": ["8.1", "9.1"] },
    { "id": 9, "tasks": ["10.1", "12.1", "12.2"] },
    { "id": 10, "tasks": ["11.1", "11.2", "11.3", "11.4", "11.5", "11.6", "11.7", "11.8", "11.9", "11.10"] }
  ]
}
```
