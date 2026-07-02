# Implementation Plan: GitHub Profile Import

## Overview

Implement GitHub profile language import capability spanning the portfolio module (endpoint, GitHub API client, import orchestration, URL storage), the skills module (skill CRUD with `source = "GITHUB"`), and the frontend (Angular import UI component). Pure functions (URL parsing, aggregation, mapping, ranking) are implemented first with property-based tests, then the orchestrator and API client, then the frontend.

## Tasks

- [x] 1. Database migration and configuration setup
  - [x] 1.1 Add `source` column to `skl_skills` table
    - Create Flyway migration adding `source VARCHAR(20) DEFAULT 'MANUAL'` column to `skl_skills`
    - Add composite index on `(employee_id, name, source)`
    - Update `Skill` entity with `source` field
    - Update `SkillRepository` with `findByEmployeeIdAndSource` and `findByEmployeeIdAndNameAndSource` query methods
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 1.2 Add GitHub configuration properties
    - Create `GitHubImportProperties` record with `@ConfigurationProperties(prefix = "github")`
    - Define `Api` sub-record with `baseUrl` and `pat` fields
    - Define `languageMapping` as `Map<String, String>`
    - Add `github.api.base-url` and `GITHUB_PAT` env var binding in `application.yml`
    - Add sample `language-mapping` entries (e.g., "Jupyter Notebook" → "Python", "Shell" → "Bash")
    - _Requirements: 8.1, 8.2, 8.4, 5.5, 5.6_

- [x] 2. Implement pure function components
  - [x] 2.1 Implement `GitHubUrlParser`
    - Create `GitHubUrlParser` class in `com.staffengagement.portfolio.github`
    - Implement `ParseResult parse(String url)` static method
    - Handle: trim whitespace, strip trailing slash, validate scheme+host, validate username pattern (1–39 chars, alphanumeric+hyphens, no leading/trailing/consecutive hyphens)
    - Reject: null/blank, wrong domain, extra path segments, query params, fragments
    - Create `InvalidGitHubUrlException` extending appropriate exception
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [x] 2.2 Write property tests for `GitHubUrlParser` (Property 1: URL Parsing Round-Trip)
    - **Property 1: URL Parsing Round-Trip**
    - **Validates: Requirements 1.1, 1.2**
    - Create `GitHubUrlParserProperties` test class using jqwik
    - Generate valid usernames (1–39 alphanumeric+hyphens, no leading/trailing/consecutive hyphens)
    - Construct URL and parse — assert extracted username matches input

  - [x] 2.3 Write property tests for `GitHubUrlParser` (Property 2: Invalid URLs Are Rejected)
    - **Property 2: Invalid URLs Are Rejected**
    - **Validates: Requirements 1.3, 1.6**
    - Generate strings that don't match GitHub profile URL pattern (wrong domain, extra segments, invalid chars, query params, fragments)
    - Assert `InvalidGitHubUrlException` thrown for all

  - [x] 2.4 Write property tests for `GitHubUrlParser` (Property 3: URL Normalization Preserves Semantics)
    - **Property 3: URL Normalization Preserves Semantics**
    - **Validates: Requirements 1.5, 1.7**
    - Generate valid URLs with added whitespace and trailing slashes
    - Assert same username extracted as canonical form

  - [x] 2.5 Implement `LanguageAggregator`
    - Create `LanguageAggregator` class in `com.staffengagement.portfolio.github`
    - Implement `List<AggregatedLanguage> aggregate(List<Map<String, Long>> repoLanguageMaps)` static method
    - Sum byte counts per language, count repos per language
    - Sort: descending by totalBytes, alphabetical ascending by name for ties
    - Use 64-bit integers for byte counts
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 2.6 Write property tests for `LanguageAggregator` (Property 4: Commutativity)
    - **Property 4: Language Aggregation Is Commutative**
    - **Validates: Requirements 4.4**
    - Generate list of language maps, aggregate in different permutations, assert identical results

  - [x] 2.7 Write property tests for `LanguageAggregator` (Property 5: Sums Are Correct)
    - **Property 5: Aggregation Sums Are Correct**
    - **Validates: Requirements 4.1**
    - Generate language maps, verify total byte count = sum across all maps for each language, repo count = number of maps containing that language

  - [x] 2.8 Write property tests for `LanguageAggregator` (Property 6: Sort Order)
    - **Property 6: Aggregation Sort Order**
    - **Validates: Requirements 4.2**
    - Assert result ordered descending by bytes, alphabetical ascending for ties

  - [x] 2.9 Implement `LanguageSkillMapper`
    - Create `LanguageSkillMapper` class in `com.staffengagement.portfolio.github`
    - Implement `List<MappedSkill> map(List<AggregatedLanguage> languages, Map<String, String> mappingConfig)` static method
    - Case-insensitive lookup in mapping config
    - Many-to-one: combine byte counts and repo counts for languages mapping to same skill name
    - Unmapped languages pass through with original casing
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.7_

  - [x] 2.10 Write property tests for `LanguageSkillMapper` (Property 7: Case-Insensitive Mapping)
    - **Property 7: Case-Insensitive Mapping Lookup**
    - **Validates: Requirements 5.1**
    - Generate language names in various casings, assert same mapped skill name regardless of case

  - [x] 2.11 Write property tests for `LanguageSkillMapper` (Property 8: Unmapped Preserved)
    - **Property 8: Unmapped Languages Preserved Exactly**
    - **Validates: Requirements 5.2**
    - Generate language names absent from mapping, assert output name identical to input

  - [x] 2.12 Write property tests for `LanguageSkillMapper` (Property 9: Many-to-One Combines)
    - **Property 9: Many-to-One Mapping Combines Counts**
    - **Validates: Requirements 5.3**
    - Generate multiple languages mapping to same skill, assert combined bytes = sum, combined repos = sum

  - [x] 2.13 Implement `ProficiencyRanker`
    - Create `ProficiencyRanker` class in `com.staffengagement.portfolio.github`
    - Implement `Map<String, Proficiency> rank(List<MappedSkill> skills)` static method
    - Top skill (rank 1) → EXPERT, ranks 2–3 → ADVANCED, rank 4+ → INTERMEDIATE
    - Ties at boundary get higher proficiency
    - Create `Proficiency` enum if not existing (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)
    - _Requirements: 6.4_

  - [x] 2.14 Write property tests for `ProficiencyRanker` (Property 10: Ranking Invariant)
    - **Property 10: Proficiency Ranking Invariant**
    - **Validates: Requirements 6.4**
    - Generate non-empty skill lists sorted by bytes descending, assert top = EXPERT, 2nd–3rd = ADVANCED, rest = INTERMEDIATE, ties at boundaries get higher level

- [x] 3. Checkpoint - Ensure all pure function tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement GitHub API client
  - [x] 4.1 Implement `GitHubApiClient` interface and implementation
    - Create `GitHubApiClient` interface in `com.staffengagement.portfolio.github`
    - Create `GitHubApiClientImpl` using Spring `RestClient`
    - Implement `fetchPublicRepos(String username)` with pagination (follow `Link` header, max 10 pages, `type=public`, `per_page=100`)
    - Implement `fetchLanguages(String owner, String repo)` returning `Map<String, Long>`
    - Set 30s timeout per request
    - Add `Authorization: Bearer {PAT}` header
    - Create `GitHubRepo` record DTO for deserialization
    - _Requirements: 2.1, 2.2, 2.6, 3.1, 8.1, 8.4_

  - [x] 4.2 Implement error handling in `GitHubApiClient`
    - Handle 404 → throw `GitHubUserNotFoundException`
    - Handle 403 with rate-limit header → throw `GitHubRateLimitException` with reset time
    - Handle 5xx/unexpected 4xx → throw `GitHubApiUnavailableException`
    - Handle timeout → throw `GitHubTimeoutException`
    - For language endpoint errors (non-rate-limit): return empty/skip signal per repo
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 3.3, 3.4, 3.6_

  - [x] 4.3 Write property test for resilient fetching (Property 13)
    - **Property 13: Resilient Fetching Preserves Successful Data**
    - **Validates: Requirements 3.3, 4.5**
    - Use MockWebServer to simulate partial failures
    - Assert aggregation includes only successfully fetched repos; skipped list contains exactly failed repo names

- [x] 5. Implement import orchestration service
  - [x] 5.1 Create `ImportResult` DTO and supporting records
    - Create `ImportResult` record with fields: skills list, githubProfileUrl, repositoriesAnalysed, skippedRepositories
    - Create `ImportedSkill` nested record with id, name, projectCount, proficiency, source
    - Create `GitHubImportRequest` DTO with `githubProfileUrl` field and Jakarta validation
    - _Requirements: 9.4, 9.5_

  - [x] 5.2 Implement `GitHubImportService`
    - Create `GitHubImportService` interface and `GitHubImportServiceImpl`
    - Inject `GitHubApiClient`, `SkillService`, `PortfolioLinkRepository`, `GitHubImportProperties`, `EmployeeRepository`
    - Implement full workflow: validate config → validate employee → parse URL → fetch repos → fetch languages (resilient) → aggregate → map → rank → upsert skills → store/update PortfolioLink → return result
    - Handle configuration validation (PAT + base URL) → 503
    - Handle employee not found → 404
    - Handle employee archived/deactivated → 409
    - Implement skill upsert: create new, update existing (`source = "GITHUB"`), delete stale
    - Store PortfolioLink with label "GitHub" (upsert)
    - _Requirements: 6.1, 6.2, 6.3, 6.5, 6.6, 6.7, 6.8, 7.1, 7.2, 7.3, 7.4, 8.3_

  - [x] 5.3 Write property test for import idempotency (Property 11)
    - **Property 11: Import Idempotency (No Duplicates)**
    - **Validates: Requirements 6.3**
    - Mock dependencies, run import twice with same data, assert same set of skills (no duplicates)

  - [x] 5.4 Write property test for stale skill removal (Property 12)
    - **Property 12: Stale Skill Removal**
    - **Validates: Requirements 6.7**
    - Mock existing GITHUB skills for employee, import with subset of languages, assert removed skills match no-longer-detected languages

  - [x] 5.5 Write property test for GitHub link upsert idempotency (Property 14)
    - **Property 14: GitHub Link Upsert Idempotency**
    - **Validates: Requirements 7.1, 7.2**
    - Run import multiple times, assert exactly one PortfolioLink with label "GitHub" exists

- [x] 6. Implement REST controller endpoint
  - [x] 6.1 Create `POST /api/portfolios/{employeeId}/github-import` endpoint
    - Add endpoint method to `PortfolioController` (or create if not existing)
    - Accept `@PathVariable UUID employeeId` and `@RequestBody @Valid GitHubImportRequest`
    - Validate UUID format, validate request body presence and `githubProfileUrl` field
    - Delegate to `GitHubImportService.importFromGitHub()`
    - Return `ImportResult` with HTTP 200
    - _Requirements: 9.1, 9.2, 9.3, 9.5, 9.6, 9.7, 9.8_

  - [x] 6.2 Implement exception handling for GitHub import errors
    - Map `InvalidGitHubUrlException` → 400
    - Map `GitHubUserNotFoundException` → 404
    - Map `GitHubRateLimitException` → 429
    - Map `GitHubApiUnavailableException` → 502
    - Map `GitHubTimeoutException` → 504
    - Map configuration not available → 503
    - Map persistence error → 500
    - Follow existing `@RestControllerAdvice` pattern
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 7.4, 8.3_

  - [x] 6.3 Write unit tests for `PortfolioController` GitHub import endpoint
    - MockMvc tests for: valid request, missing body, invalid URL, invalid UUID, employee not found, GitHub errors
    - Verify correct HTTP status codes and error messages
    - _Requirements: 9.1–9.8_

- [x] 7. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement frontend import UI
  - [x] 8.1 Create `ImportSkillsComponent` Angular component
    - Create standalone component at `frontend/src/app/portfolio/components/import-skills/`
    - Add text input field (max 2048 chars, placeholder "https://github.com/username")
    - Add "Import Skills" button
    - Implement loading state: disable button + input, show spinner + "Import in progress" message
    - Implement success state: display skill list (name, proficiency, project count) + repos analysed count
    - Implement error state: display error message, preserve input value, re-enable controls
    - Use Angular reactive forms for URL input validation
    - Use signals for component state management
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [x] 8.2 Extend `PortfolioService` with GitHub import method
    - Add `importGitHubSkills(employeeId: string, githubProfileUrl: string): Observable<ImportResult>` method
    - Create `ImportResult` TypeScript interface in portfolio models
    - Call `POST /api/portfolios/${employeeId}/github-import`
    - _Requirements: 10.2_

  - [x] 8.3 Implement pre-population of GitHub URL from existing PortfolioLink
    - On component init, check if employee has PortfolioLink with label "GitHub"
    - Pre-populate input field with existing URL
    - _Requirements: 10.7_

  - [x] 8.4 Write unit tests for `ImportSkillsComponent`
    - Test DOM rendering: input field, button, placeholder
    - Test loading state: spinner shown, controls disabled
    - Test success state: skill list displayed correctly
    - Test error state: error message shown, input preserved
    - Test pre-population from existing link
    - _Requirements: 10.1–10.7_

  - [x] 8.5 Write unit tests for `PortfolioService` GitHub import method
    - Test HTTP call construction (URL, method, body)
    - Test error propagation
    - _Requirements: 10.2_

- [x] 9. Integration tests
  - [x] 9.1 Write integration test for full GitHub import workflow
    - Use Testcontainers (PostgreSQL) + MockWebServer (GitHub API)
    - Test full flow: submit URL → fetch repos → fetch languages → create skills → verify DB state
    - Test pagination handling with mock paginated responses
    - Test skill update on re-import
    - Test stale skill removal
    - Test PortfolioLink creation and update
    - _Requirements: 2.1, 2.2, 3.1, 4.1, 6.1–6.7, 7.1, 7.2_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Backend uses Java 21, Spring Boot 3.4.x, jqwik 1.9.2 for property tests
- Frontend uses Angular 21 standalone components with signals

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.5", "2.9", "2.13"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.6", "2.7", "2.8", "2.10", "2.11", "2.12", "2.14"] },
    { "id": 3, "tasks": ["4.1", "5.1"] },
    { "id": 4, "tasks": ["4.2", "4.3"] },
    { "id": 5, "tasks": ["5.2"] },
    { "id": 6, "tasks": ["5.3", "5.4", "5.5", "6.1"] },
    { "id": 7, "tasks": ["6.2", "6.3"] },
    { "id": 8, "tasks": ["8.1", "8.2"] },
    { "id": 9, "tasks": ["8.3", "8.4", "8.5"] },
    { "id": 10, "tasks": ["9.1"] }
  ]
}
```
