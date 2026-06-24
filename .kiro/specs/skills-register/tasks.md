# Implementation Plan: Skills Register

## Overview

The Skills Register feature is partially implemented. Core CRUD operations, search, entity/repository, controller, frontend component, and basic unit tests are in place. This plan addresses the remaining gaps: input validation hardening (query length, proficiency enum, employee existence, result limits, ordering), frontend error handling, and comprehensive test coverage (property-based, integration, and API tests).

## Tasks

- [x] 1. Harden backend validation and business logic
  - [x] 1.1 Add query length validation and result limit to search endpoint
    - In `SkillController.search()`, add `@Size(max = 100)` on the `query` parameter with a descriptive validation message
    - Add `@RequestParam @NotBlank` validation to reject empty/whitespace queries at controller level
    - In `SkillServiceImpl.search()`, add early return for whitespace-only queries (`.trim().isEmpty()` check)
    - In `SkillServiceImpl.search()`, limit the returned list to a maximum of 50 results using `.limit(50)` in the stream
    - _Requirements: 1.5, 1.6, 1.8_

  - [x] 1.2 Add employee existence validation on skill creation
    - In `SkillServiceImpl.create()`, call `employeeRepository.existsById(request.employeeId())` before persisting
    - If employee does not exist, throw `EntityNotFoundException` with message "Employee not found: {id}"
    - _Requirements: 3.8_

  - [x] 1.3 Add proficiency enum validation to CreateSkillRequest and UpdateSkillRequest
    - Create a `Proficiency` enum in `com.staffengagement.skills.model` with values: `Beginner`, `Intermediate`, `Advanced`, `Expert`
    - Create a custom `@ValidProficiency` Jakarta constraint annotation with a corresponding `ProficiencyValidator` that checks the string value is one of the allowed enum values (case-sensitive match)
    - Apply `@ValidProficiency` to the `proficiency` field in both `CreateSkillRequest` and `UpdateSkillRequest` (replacing `@NotBlank`)
    - Provide a validation error message: "Proficiency must be one of: Beginner, Intermediate, Advanced, Expert"
    - _Requirements: 3.7, 4.1_

  - [x] 1.4 Add max value constraints to CreateSkillRequest and UpdateSkillRequest
    - Add `@Max(50)` to `yearsExperience` field in both DTOs with message "Years of experience must be between 0 and 50"
    - Add `@Max(500)` to `projectCount` field in both DTOs with message "Project count must be between 0 and 500"
    - Add `@Size(max = 100)` to `name` field in both DTOs with message "Skill name must not exceed 100 characters"
    - _Requirements: 3.1, 3.3, 3.5, 3.6, 4.1, 4.4, 4.5_

  - [x] 1.5 Add ordering by name ascending to findByEmployeeId
    - In `SkillRepository`, change `findByEmployeeId(UUID)` to `findByEmployeeIdOrderByNameAsc(UUID)` 
    - Update `SkillServiceImpl.findByEmployeeId()` to use the new method
    - _Requirements: 2.1_

- [x] 2. Checkpoint - Ensure backend compiles and existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Add frontend error handling for search
  - [x] 3.1 Display error message on search failure in SkillSearchComponent
    - Add an `errorMessage` signal (`signal<string | null>(null)`) to `SkillSearchComponent`
    - In the `onSearch()` error callback, set `errorMessage` to "Search could not be completed. Please try again."
    - Reset `errorMessage` to `null` at the start of each new search
    - In the template, add an error message block after the loading state: display the error text when `errorMessage()` is non-null
    - Style the error message with a red/danger colour class
    - _Requirements: 7.5_

- [x] 4. Backend unit tests for new validation logic
  - [x] 4.1 Add unit tests for search validation and limits in SkillServiceTest
    - Test that whitespace-only query returns empty list without calling repository
    - Test that results are capped at 50 when more matching skills exist
    - _Requirements: 1.5, 1.8_

  - [x] 4.2 Add unit tests for employee existence validation in SkillServiceTest
    - Test that `create()` throws `EntityNotFoundException` when employee does not exist
    - Test that `create()` succeeds when employee exists
    - _Requirements: 3.8_

  - [x] 4.3 Add unit tests for findByEmployeeId ordering
    - Test that results are returned in alphabetical order by skill name
    - _Requirements: 2.1_

- [x] 5. Checkpoint - Ensure all unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. API tests with MockMvc
  - [x] 6.1 Create SkillControllerTest with MockMvc
    - Create `src/test/java/com/staffengagement/skills/controller/SkillControllerTest.java`
    - Use `@WebMvcTest(SkillController.class)` with `@MockBean SkillService`
    - Disable security for these tests with `@AutoConfigureMockMvc(addFilters = false)`
    - Test `GET /api/skills/search?query=Angular` returns 200 with JSON array
    - Test `POST /api/skills` with valid body returns 201
    - Test `POST /api/skills` with blank name returns 400 with field-level validation errors
    - Test `POST /api/skills` with invalid proficiency returns 400
    - Test `POST /api/skills` with yearsExperience > 50 returns 400
    - Test `POST /api/skills` with projectCount > 500 returns 400
    - Test `PUT /api/skills/{id}` with valid body returns 200
    - Test `PUT /api/skills/{id}` for non-existent ID returns 404
    - Test `DELETE /api/skills/{id}` returns 204
    - Test `GET /api/skills/search?query=` (empty query) returns 400
    - Test `GET /api/skills/search` with query > 100 chars returns 400
    - _Requirements: 1.6, 3.1, 3.2, 3.5, 3.6, 3.7, 4.2, 5.1, 5.2_

- [x] 7. Integration tests with Testcontainers
  - [x] 7.1 Create SkillIntegrationTest with full Spring context and PostgreSQL
    - Create `src/test/java/com/staffengagement/skills/SkillIntegrationTest.java`
    - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `@Testcontainers`
    - Configure PostgreSQL Testcontainer with `@ServiceConnection`
    - Use `TestRestTemplate` for HTTP calls
    - Disable security or use a test authentication utility
    - Test full CRUD lifecycle: create → read → update → read → delete → verify gone
    - Test search returns ranked results with real database data
    - Test search with partial match returns correct results
    - Test create with non-existent employee returns error
    - Test findByEmployeeId returns results ordered by name ascending
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 3.1, 3.8, 4.1, 5.1_

- [x] 8. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Property-based tests (jqwik)
  - [ ]* 9.1 Add jqwik dependency and create SkillServicePropertyTest
    - Add `net.jqwik:jqwik:1.9.1` to pom.xml test dependencies
    - Create `src/test/java/com/staffengagement/skills/service/SkillServicePropertyTest.java`
    - Use `@ExtendWith(MockitoExtension.class)` with jqwik `@Property(tries = 100)`
    - **Property 1: Search returns correct partial matches with complete data**
    - Generate random skill names and query substrings; verify all returned results contain the query (case-insensitive) and have complete employee data
    - **Validates: Requirements 1.1, 1.2**

  - [ ]* 9.2 Write property test for search ranking correctness
    - **Property 2: Search results are ranked correctly**
    - Generate random lists of skills with varying years/projects; verify returned list is sorted by years desc then projects desc
    - **Validates: Requirements 1.3**

  - [ ]* 9.3 Write property test for whitespace-only query handling
    - **Property 3: Whitespace-only queries produce no results**
    - Generate random whitespace strings (spaces, tabs, newlines); verify search returns empty and repository is never called
    - **Validates: Requirements 1.5**

  - [ ]* 9.4 Write property test for search results bounded at 50
    - **Property 4: Search results bounded at 50**
    - Generate lists of 51+ matching skills; verify returned results never exceed 50
    - **Validates: Requirements 1.8**

  - [ ]* 9.5 Write property test for employee skill retrieval ordering
    - **Property 5: Employee skill retrieval returns complete sorted set**
    - Generate N skills with random names for one employee; verify findByEmployeeId returns all N sorted by name ascending
    - **Validates: Requirements 2.1**

  - [ ]* 9.6 Write property test for create round-trip
    - **Property 6: Create round-trip preserves data**
    - Generate valid CreateSkillRequest with arbitrary valid values; verify response has matching fields, non-null id and createdAt
    - **Validates: Requirements 3.1**

  - [ ]* 9.7 Write property test for update round-trip
    - **Property 7: Update round-trip reflects new values**
    - Generate existing skill and valid UpdateSkillRequest; verify updated response matches new values while preserving id and employeeId
    - **Validates: Requirements 4.1**

  - [ ]* 9.8 Write property test for delete removes record
    - **Property 8: Delete removes record permanently**
    - Verify that after delete, existsById returns false and findByEmployeeId excludes the deleted skill
    - **Validates: Requirements 5.1**

  - [ ]* 9.9 Write property test for exact name match case-sensitivity
    - **Property 9: Exact name match is case-sensitive**
    - Generate skill names with mixed casing; verify findByName returns only byte-for-byte matches
    - **Validates: Requirements 6.1**

- [ ] 10. Frontend unit tests
  - [ ]* 10.1 Create SkillSearchComponent unit tests
    - Create `frontend/src/app/skills/components/skill-search/skill-search.component.spec.ts`
    - Use Angular TestBed with mocked `SkillService`
    - Test that `onSearch()` sets `isLoading` to true and `hasSearched` to true
    - Test that successful search populates `results` signal and resets `isLoading`
    - Test that failed search sets `errorMessage` and resets `isLoading`
    - Test that whitespace-only query does not trigger HTTP call
    - Test that template renders table rows with rank, name, email, years, projects, proficiency badge
    - Test that empty results shows "No employees found" message
    - Test that loading state shows "Searching..." text
    - **Property 10: UI renders complete result rows**
    - **Validates: Requirements 7.1, 7.3, 7.4, 7.5**

  - [ ]* 10.2 Create SkillService (frontend) unit tests
    - Create `frontend/src/app/skills/services/skill.service.spec.ts`
    - Use `HttpClientTestingModule` to mock HTTP responses
    - Test `search()` sends GET to `/api/skills/search` with query param
    - Test `findByEmployee()` sends GET to `/api/skills` with employeeId param
    - Test `create()` sends POST to `/api/skills` with request body
    - Test `update()` sends PUT to `/api/skills/{id}` with request body
    - Test `delete()` sends DELETE to `/api/skills/{id}`
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1_

- [x] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The jqwik dependency needs to be added before property tests can run
- Frontend tests use Angular TestBed with `provideHttpClientTesting()` (Angular 21 standalone approach)
- Integration tests require Docker for Testcontainers PostgreSQL

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.3", "1.4", "1.5"] },
    { "id": 1, "tasks": ["1.2", "3.1"] },
    { "id": 2, "tasks": ["4.1", "4.2", "4.3"] },
    { "id": 3, "tasks": ["6.1"] },
    { "id": 4, "tasks": ["7.1"] },
    { "id": 5, "tasks": ["9.1", "9.2", "9.3", "9.4", "9.5"] },
    { "id": 6, "tasks": ["9.6", "9.7", "9.8", "9.9"] },
    { "id": 7, "tasks": ["10.1", "10.2"] }
  ]
}
```
