# Requirements Document

## Introduction

This feature enables importing programming language skills from a GitHub profile into the Staff Engagement system. A user inputs a GitHub profile URL on an employee's portfolio page, the system fetches public repository data via the GitHub REST API, detects programming languages used across all repositories, and auto-creates or updates skill entries in the skills module linked to that employee. The feature spans the portfolio module (endpoint, GitHub integration, URL storage), the skills module (skill creation/update), and the frontend (input UI, loading states, result display).

## Glossary

- **GitHub_Import_Service**: The backend service in the portfolio module responsible for orchestrating the GitHub profile import workflow — URL parsing, GitHub API calls, language aggregation, and skill creation/update.
- **Portfolio_Controller**: The REST controller exposing portfolio endpoints at `/api/portfolios`, extended with the GitHub import endpoint.
- **Skill_Service**: The existing backend service in the skills module responsible for creating and managing skill entries for employees.
- **GitHub_API_Client**: The HTTP client component that communicates with the GitHub REST API to fetch repository and language data.
- **Language_Skill_Mapping**: A configurable mapping that translates GitHub language names (e.g., "JavaScript", "TypeScript") to skill entry names in the skills module.
- **Employee**: The person whose portfolio is being updated with GitHub-imported skills, identified by a UUID.
- **GitHub_Profile_URL**: A URL in the format `https://github.com/{username}` identifying a public GitHub user profile.
- **Language_Byte_Count**: The number of bytes of code written in a specific programming language within a GitHub repository, as reported by the GitHub Languages API.
- **Import_Result**: The response payload containing the list of skills that were created or updated during the import operation.
- **GitHub_PAT**: A GitHub Personal Access Token used for authenticated API requests to achieve higher rate limits (5000 requests/hour).
- **Import_Skills_Component**: The Angular component on the portfolio page providing the GitHub URL input field, import button, loading state, and result display.

## Requirements

### Requirement 1: GitHub Profile URL Parsing

**User Story:** As a system component, I want to parse and validate a GitHub profile URL, so that only well-formed URLs are accepted before making external API calls.

#### Acceptance Criteria

1. WHEN a GitHub_Profile_URL is submitted, THE GitHub_Import_Service SHALL validate that the URL matches the pattern `https://github.com/{username}` (case-insensitive scheme and host) where `{username}` contains only alphanumeric characters and hyphens, does not start or end with a hyphen, does not contain consecutive hyphens, and is between 1 and 39 characters long.
2. WHEN a valid GitHub_Profile_URL is submitted, THE GitHub_Import_Service SHALL extract the username from the URL path.
3. IF the submitted URL does not match the expected GitHub profile URL pattern (including malformed syntax, wrong domain, or any unrecognized URL format), THEN THE GitHub_Import_Service SHALL reject the request with HTTP 400 and an error message indicating the URL format is invalid.
4. IF the submitted URL is null, blank, or present but invalid (malformed syntax, non-GitHub domain, or structurally incorrect), THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a validation error indicating the GitHub profile URL is required or invalid.
5. WHEN a GitHub_Profile_URL contains a trailing slash (e.g., `https://github.com/username/`), THE GitHub_Import_Service SHALL treat it as valid and extract the username correctly.
6. IF the submitted URL contains additional path segments beyond the username (e.g., `https://github.com/username/repo`) or contains query parameters or fragment identifiers (e.g., `https://github.com/username?tab=repos`), THEN THE GitHub_Import_Service SHALL reject the request with HTTP 400 and an error indicating only profile URLs are accepted.
7. WHEN a GitHub_Profile_URL contains leading or trailing whitespace, THE GitHub_Import_Service SHALL trim the whitespace before performing validation.

### Requirement 2: Fetch Repositories from GitHub

**User Story:** As the system, I want to fetch all public repositories for a GitHub user, so that I can analyse their language usage across projects.

#### Acceptance Criteria

1. WHEN a valid username is extracted, THE GitHub_API_Client SHALL call `GET /users/{username}/repos` on the configured GitHub API base URL with the query parameters `type=public` and `per_page=100`, including the configured GitHub_PAT in the Authorization header.
2. WHEN the GitHub API returns a paginated response, THE GitHub_API_Client SHALL follow pagination links to retrieve all public repositories for the user, up to a maximum of 10 pages (1000 repositories).
3. IF the GitHub API returns HTTP 404 for the username, THEN THE GitHub_Import_Service SHALL return HTTP 404 with an error message indicating the GitHub user was not found.
4. IF the GitHub API returns HTTP 403 with a rate-limit header indicating the limit is exceeded, THEN THE GitHub_Import_Service SHALL return HTTP 429 with an error message indicating the GitHub API rate limit has been reached and include the reset time from the response headers.
5. IF the GitHub API returns any other error status (5xx or unexpected 4xx), THEN THE GitHub_Import_Service SHALL return HTTP 502 with an error message indicating the GitHub API is unavailable.
6. THE GitHub_API_Client SHALL set a request timeout of 30 seconds for each GitHub API call; IF the timeout is exceeded, THEN THE GitHub_Import_Service SHALL return HTTP 504 with an error indicating the GitHub API request timed out.
7. WHEN the GitHub API returns a successful response containing zero public repositories for the user, THE GitHub_Import_Service SHALL return an empty repository list with HTTP 200.

### Requirement 3: Fetch Language Data per Repository

**User Story:** As the system, I want to retrieve language byte counts for each repository, so that I can determine which programming languages the user works with.

#### Acceptance Criteria

1. WHEN the list of public repositories is retrieved, THE GitHub_API_Client SHALL call `GET /repos/{owner}/{repo}/languages` for each repository using the configured GitHub_PAT in the Authorization header, applying the same 30-second request timeout defined for all GitHub API calls.
2. WHEN the GitHub API returns language data for a repository, THE GitHub_Import_Service SHALL receive a map of language names to byte counts (e.g., `{"Java": 150000, "Python": 32000}`) where each byte count is a non-negative integer.
3. IF the GitHub API returns a non-rate-limit error (HTTP 5xx or non-403/429 4xx) for a specific repository's language endpoint, THEN THE GitHub_Import_Service SHALL skip that repository, continue processing remaining repositories, and include the skipped repository name in the response metadata.
4. IF the GitHub API returns HTTP 403 with a rate-limit-exceeded header during language fetching for any repository, THEN THE GitHub_Import_Service SHALL stop processing remaining repositories and return HTTP 429 with the rate limit reset time; IF language data was successfully collected from previously processed repositories, THEN the response SHALL include that collected data, otherwise the collected data field SHALL be omitted.
5. IF a repository has no detected languages (empty language map), THEN THE GitHub_Import_Service SHALL skip that repository without error and not include it in skipped repository metadata.
6. IF the request timeout is exceeded for a specific repository's language endpoint, THEN THE GitHub_Import_Service SHALL treat the timed-out repository as a skipped repository, include it in the response metadata, and continue processing remaining repositories.

### Requirement 4: Aggregate Language Usage

**User Story:** As the system, I want to aggregate language byte counts across all repositories, so that I can determine overall language proficiency.

#### Acceptance Criteria

1. WHEN language data is collected from all repositories, THE GitHub_Import_Service SHALL sum the byte counts for each language across all repositories to produce a total byte count per language, where each byte count is represented as a 64-bit integer.
2. THE GitHub_Import_Service SHALL sort the aggregated languages by total byte count in descending order in the Import_Result response, and WHERE two or more languages have equal byte counts, THE GitHub_Import_Service SHALL sort those languages alphabetically by language name in ascending order.
3. IF no languages are detected across all repositories (all repositories are empty or contain no code), THEN THE GitHub_Import_Service SHALL return HTTP 200 with an empty skill list and a message indicating no languages were detected.
4. THE GitHub_Import_Service SHALL produce identical aggregation results regardless of the order in which repository language maps are processed (associativity and commutativity property).
5. IF language data retrieval fails for one or more repositories during aggregation, THEN THE GitHub_Import_Service SHALL exclude the failed repositories from the aggregation, include the successfully retrieved language data in the result, and indicate in the response which repositories were excluded due to retrieval failure; WHEN no retrieval failures occur, THE response SHALL NOT include exclusion indicators or failure metadata.

### Requirement 5: Map Languages to Skills

**User Story:** As an administrator, I want a configurable mapping between GitHub language names and skill entries, so that language detection produces meaningful skill records.

#### Acceptance Criteria

1. WHEN a GitHub language has an entry in the Language_Skill_Mapping, THE GitHub_Import_Service SHALL use the mapped skill name as the skill entry name; the mapping lookup SHALL be case-insensitive against the GitHub language name (e.g., "javascript" matches a mapping key "JavaScript").
2. WHEN a GitHub language does not have an explicit mapping entry, THE GitHub_Import_Service SHALL use the GitHub language name exactly as returned by the GitHub API (preserving original casing) for the skill entry name.
3. WHEN multiple GitHub languages map to the same skill name via many-to-one mapping, THE GitHub_Import_Service SHALL combine their byte counts and repository counts into a single skill entry for that mapped name before determining proficiency ranking.
4. THE Language_Skill_Mapping SHALL support many-to-one mappings (e.g., both "Jupyter Notebook" and "Python" can map to "Python") with no restriction on the number of source languages that can map to a single target skill name.
5. THE Language_Skill_Mapping SHALL be loaded from application configuration (e.g., YAML/properties file) and not hardcoded in source code.
6. THE Language_Skill_Mapping SHALL be extensible at deployment time without code changes, by modifying the configuration file or environment variables.
7. IF the Language_Skill_Mapping configuration is absent or contains no entries, THEN THE GitHub_Import_Service SHALL proceed with the import using all GitHub language names as-is (no translations applied), without raising an error.

### Requirement 6: Create or Update Skill Entries

**User Story:** As the system, I want to create new skill entries or update existing ones from a previous import, so that repeated imports do not produce duplicate skills.

#### Acceptance Criteria

1. WHEN a language is detected and mapped to a skill name, THE GitHub_Import_Service SHALL check if a Skill entry already exists for that employee with the same skill name and `source = "GITHUB"`.
2. IF no matching Skill entry exists, THEN THE GitHub_Import_Service SHALL create a new Skill entry with the mapped skill name, `source = "GITHUB"`, the `projectCount` set to the number of distinct repositories where that language (or any language mapped to the same skill name) was detected, and proficiency derived from the language's rank in the aggregated results.
3. IF a matching Skill entry exists (same employee, same skill name, `source = "GITHUB"`), THEN THE GitHub_Import_Service SHALL update the existing entry's `projectCount` to the new repository count for that language and update the proficiency to reflect the language's current rank in the aggregated results, rather than creating a duplicate.
4. THE GitHub_Import_Service SHALL set proficiency based on the language's rank in the aggregated results sorted by total byte count descending: the top-ranked language receives "EXPERT", the second and third-ranked languages receive "ADVANCED", and all remaining languages receive "INTERMEDIATE"; IF two or more languages share the same total byte count at a rank boundary, THE GitHub_Import_Service SHALL assign the higher proficiency level to all tied languages.
5. WHEN the import completes, THE GitHub_Import_Service SHALL return the full list of created and updated Skill entries in the Import_Result response with HTTP 200.
6. IF the employee ID in the request path does not correspond to an existing Employee, THEN THE Portfolio_Controller SHALL reject the request with HTTP 404 and an error indicating the employee was not found.
8. IF the employee exists but is in an error state that prevents skill processing (e.g., employee record is archived or deactivated), THEN THE GitHub_Import_Service SHALL reject the request with HTTP 409 and an error indicating the employee cannot receive skill imports in their current state.
7. WHEN the import completes and Skill entries with `source = "GITHUB"` exist for the employee that correspond to languages no longer detected in the current import, THE GitHub_Import_Service SHALL remove those stale Skill entries so that only currently detected languages are represented.

### Requirement 7: Store GitHub Profile URL on Portfolio

**User Story:** As a user, I want the GitHub profile URL saved on the employee's portfolio record, so that future re-imports can use the same URL.

#### Acceptance Criteria

1. WHEN a successful import completes (HTTP 200 with skills created/updated), THE GitHub_Import_Service SHALL store the GitHub_Profile_URL on the employee's portfolio record as a PortfolioLink with the label "GitHub" and the URL field set to the submitted GitHub_Profile_URL.
2. IF a PortfolioLink with the exact label "GitHub" (case-sensitive match) already exists for that employee, THEN THE GitHub_Import_Service SHALL update the existing link's URL to the newly submitted GitHub_Profile_URL rather than creating a duplicate entry.
3. WHEN the import completes successfully, THE GitHub_Import_Service SHALL include the stored GitHub_Profile_URL as a field in the Import_Result response payload.
4. IF storing or updating the PortfolioLink fails due to a persistence error, THEN THE GitHub_Import_Service SHALL return HTTP 500 with an error message indicating the profile URL could not be saved, even if skill creation succeeded.

### Requirement 8: GitHub API Configuration

**User Story:** As a developer, I want GitHub API credentials and base URL to be configurable, so that I can run tests with mock servers and manage tokens securely.

#### Acceptance Criteria

1. THE application SHALL read the GitHub_PAT from the environment variable `GITHUB_PAT` and not from hardcoded values in source code.
2. THE application SHALL read the GitHub API base URL from the application property `github.api.base-url`; both the `GITHUB_PAT` environment variable and the `github.api.base-url` property SHALL be explicitly configured for the integration to be considered operational.
3. IF the `GITHUB_PAT` environment variable is not set, is empty, or contains only whitespace characters, OR IF the `github.api.base-url` property is not explicitly configured, THEN THE GitHub_Import_Service SHALL completely block and reject the import request before any processing begins, returning HTTP 503 and an error indicating the GitHub integration is not configured.
4. THE application SHALL support overriding the GitHub API base URL via the application property `github.api.base-url` for testing with mock servers; the configured base URL SHALL be used as the prefix for all GitHub REST API calls made by the GitHub_API_Client.
5. IF the `github.api.base-url` property is set to a value that is not a valid HTTP or HTTPS URL, THEN THE application SHALL fail to start and log an error indicating the GitHub API base URL is malformed.

### Requirement 9: Import Endpoint

**User Story:** As a user, I want a REST endpoint to trigger the GitHub profile import for a specific employee, so that skills can be imported on demand.

#### Acceptance Criteria

1. THE Portfolio_Controller SHALL expose `POST /api/portfolios/{employeeId}/github-import` accepting a JSON request body with a `githubProfileUrl` field.
2. WHEN the endpoint receives a valid request, THE Portfolio_Controller SHALL delegate to the GitHub_Import_Service and return the Import_Result with HTTP 200.
3. IF the `employeeId` path variable is not a valid UUID, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and an error indicating the expected UUID format.
4. THE Import_Result response SHALL contain: the list of created/updated skills (each with id, name, projectCount, proficiency as one of Beginner/Intermediate/Advanced/Expert, and source set to "GITHUB"), the stored GitHub profile URL, and the count of repositories analysed.
5. IF the request body is missing or does not contain the `githubProfileUrl` field, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a validation error.
6. IF the `githubProfileUrl` value is not a valid URL matching the pattern `https://github.com/{username}`, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and an error indicating the expected GitHub profile URL format.
7. IF no employee exists for the given `employeeId`, THEN THE Portfolio_Controller SHALL return HTTP 404 and an error indicating that the employee was not found.
8. IF the GitHub_Import_Service fails to reach the GitHub API or the profile does not exist, THEN THE Portfolio_Controller SHALL return HTTP 502 and an error indicating that the GitHub import could not be completed.

### Requirement 10: Frontend Import UI

**User Story:** As a user, I want an input field and button on the portfolio page to trigger a GitHub import, so that I can import skills with minimal effort.

#### Acceptance Criteria

1. THE Import_Skills_Component SHALL display a text input field for the GitHub profile URL (maximum 2048 characters, with placeholder text "https://github.com/username") and an "Import Skills" button on the employee's portfolio page.
2. WHEN the "Import Skills" button is clicked and the input field contains at least one non-whitespace character, THE Import_Skills_Component SHALL call the backend `POST /api/portfolios/{employeeId}/github-import` endpoint with the provided URL.
3. IF the input field is empty or contains only whitespace when the "Import Skills" button is clicked, THEN THE Import_Skills_Component SHALL display a validation message indicating the URL is required and SHALL NOT call the backend.
4. WHILE the import request is in progress, THE Import_Skills_Component SHALL disable the "Import Skills" button, disable the input field, display a loading spinner adjacent to the button, and show a text message indicating the import is in progress.
5. WHEN the backend returns a successful Import_Result, THE Import_Skills_Component SHALL hide the loading spinner, re-enable the input field and the "Import Skills" button, and display the list of imported skills (name, proficiency, project count) and the total number of repositories analysed, replacing any previously displayed import results or error messages.
6. IF the backend returns an error response, THEN THE Import_Skills_Component SHALL hide the loading spinner, re-enable the input field and the "Import Skills" button, display the error message from the response body, and preserve the current input field value so the user can correct and retry.
7. WHEN the employee's portfolio contains a PortfolioLink with label "GitHub", THE Import_Skills_Component SHALL pre-populate the input field with that link's URL on page load.
