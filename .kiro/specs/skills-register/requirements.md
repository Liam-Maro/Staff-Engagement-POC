# Requirements Document

## Introduction

The Skills Register is the centrepiece of the Staff Engagement POC. It quantifies experience per skill (years of experience and project count) for technologies such as Angular, Java, Python, etc. The feature answers questions like "Who's strong on Angular?" by returning ranked lists of employees with their skill metrics. It provides a searchable, organisation-wide view of technical competencies and supports full CRUD management of skill records per employee.

## Glossary

- **Skills_Register**: The system module responsible for storing, retrieving, searching, and managing skill records across the organisation.
- **Skill**: A record representing an employee's experience with a named technology or competency, quantified by years of experience, project count, and proficiency level.
- **Employee**: A person record in the system to whom skills are attributed.
- **Proficiency**: A categorical rating of skill level. Valid values are: expert, advanced, intermediate, beginner.
- **Search_Query**: A case-insensitive partial text input used to find skills by name across all employees.
- **Skill_Ranking**: The ordering algorithm applied to search results, sorting by years of experience descending then project count descending.

## Requirements

### Requirement 1: Search Skills Across Organisation

**User Story:** As an authenticated user, I want to search for a skill by name, so that I can find out who in the organisation has experience with that technology.

#### Acceptance Criteria

1. WHEN a user submits a Search_Query, THE Skills_Register SHALL return all Skill records where the Skill name contains the Search_Query (case-insensitive partial match).
2. WHEN search results are returned, THE Skills_Register SHALL include the Employee first name, last name, email, Skill name, years of experience, project count, and Proficiency for each matching record.
3. WHEN search results are returned, THE Skills_Register SHALL apply Skill_Ranking by sorting results by years of experience descending, then by project count descending.
4. WHEN a Search_Query matches no Skill records, THE Skills_Register SHALL return an empty result set.
5. WHEN a Search_Query is submitted with an empty or whitespace-only string, THE Skills_Register SHALL not execute the search and SHALL return no results.
6. IF a user submits a Search_Query that exceeds 100 characters, THEN THE Skills_Register SHALL reject the request with a validation error indicating the query is too long.
7. IF an unauthenticated user attempts to submit a Search_Query, THEN THE Skills_Register SHALL reject the request and return an authentication error.
8. WHEN search results are returned, THE Skills_Register SHALL return a maximum of 50 results per request.

### Requirement 2: Retrieve Skills by Employee

**User Story:** As an authenticated user, I want to view all skills for a specific employee, so that I can see their complete skill profile.

#### Acceptance Criteria

1. WHEN a valid Employee identifier (UUID) is provided, THE Skills_Register SHALL return all Skill records associated with that Employee, ordered by name ascending.
2. WHEN a valid Employee identifier is provided that has no associated skills, THE Skills_Register SHALL return an empty result set.
3. THE Skills_Register SHALL include the Skill id, Employee identifier, name, years of experience, project count, Proficiency, and creation timestamp in each Skill response.
4. IF an Employee identifier is provided that does not exist in the system, THEN THE Skills_Register SHALL return an empty result set.
5. IF an Employee identifier is provided in an invalid UUID format, THEN THE Skills_Register SHALL return a validation error indicating the identifier format is invalid.

### Requirement 3: Create a Skill Record

**User Story:** As an authenticated user, I want to add a new skill record for an employee, so that their technical competencies are captured in the register.

#### Acceptance Criteria

1. WHEN a valid create request is submitted with an Employee identifier (UUID), Skill name (1 to 100 characters), years of experience (integer, 0 to 50), project count (integer, 0 to 500), and Proficiency (one of: Beginner, Intermediate, Advanced, Expert), THE Skills_Register SHALL create a new Skill record and return it with a system-generated UUID identifier and a creation timestamp in ISO-8601 format.
2. WHEN a create request is submitted with a missing or blank Skill name, THE Skills_Register SHALL reject the request with a validation error indicating that Skill name is required.
3. WHEN a create request is submitted with a Skill name exceeding 100 characters, THE Skills_Register SHALL reject the request with a validation error indicating that Skill name exceeds the maximum length.
4. WHEN a create request is submitted with a missing Employee identifier, THE Skills_Register SHALL reject the request with a validation error indicating that Employee identifier is required.
5. WHEN a create request is submitted with a negative years of experience value or a value exceeding 50, THE Skills_Register SHALL reject the request with a validation error indicating the allowed range of 0 to 50.
6. WHEN a create request is submitted with a negative project count value or a value exceeding 500, THE Skills_Register SHALL reject the request with a validation error indicating the allowed range of 0 to 500.
7. WHEN a create request is submitted with a missing, blank, or unrecognized Proficiency value, THE Skills_Register SHALL reject the request with a validation error indicating the accepted values (Beginner, Intermediate, Advanced, Expert).
8. IF a create request references an Employee identifier that does not exist in the system, THEN THE Skills_Register SHALL reject the request with a validation error indicating that the specified employee was not found.
9. WHEN a Skill record is successfully created, THE Skills_Register SHALL return HTTP status 201 (Created).

### Requirement 4: Update a Skill Record

**User Story:** As an authenticated user, I want to update an existing skill record, so that I can correct or refresh an employee's skill information.

#### Acceptance Criteria

1. WHEN a valid update request is submitted with a Skill identifier, name (1 to 100 characters), years of experience (integer, 0 to 50), project count (integer, 0 to 500), and Proficiency (one of: Beginner, Intermediate, Advanced, Expert), THE Skills_Register SHALL update the existing Skill record and return the updated response.
2. WHEN an update request references a Skill identifier that does not exist, THE Skills_Register SHALL return HTTP status 404 with an error indicating the Skill was not found.
3. WHEN an update request is submitted with a missing or blank Skill name, THE Skills_Register SHALL reject the request with a validation error.
4. WHEN an update request is submitted with a negative years of experience value or a value exceeding 50, THE Skills_Register SHALL reject the request with a validation error.
5. WHEN an update request is submitted with a negative project count value or a value exceeding 500, THE Skills_Register SHALL reject the request with a validation error.
6. IF an update request is submitted with a Skill identifier that is not a valid UUID format, THEN THE Skills_Register SHALL return a validation error indicating the identifier format is invalid.

### Requirement 5: Delete a Skill Record

**User Story:** As an authenticated user, I want to delete a skill record, so that outdated or incorrect skills can be removed from an employee's profile.

#### Acceptance Criteria

1. WHEN a delete request is submitted with a Skill identifier that corresponds to an existing Skill record, THE Skills_Register SHALL remove the Skill record, return HTTP status 204 (No Content) with an empty response body, and ensure the record is no longer retrievable by subsequent requests.
2. IF a delete request references a Skill identifier that does not exist, THEN THE Skills_Register SHALL return HTTP status 404 with an error response indicating the Skill was not found.
3. IF a delete request is submitted with a Skill identifier that is not a valid UUID format, THEN THE Skills_Register SHALL return a validation error indicating the identifier format is invalid.

### Requirement 6: Retrieve Skills by Exact Name

**User Story:** As an authenticated user, I want to retrieve all skill records with an exact name match, so that I can see every employee who has registered a specific skill.

#### Acceptance Criteria

1. WHEN an exact Skill name is provided, THE Skills_Register SHALL return all Skill records where the name matches the provided value using case-sensitive comparison, including the Skill id, Employee identifier, name, years of experience, project count, Proficiency, and creation timestamp for each record.
2. WHEN an exact Skill name is provided that matches no records, THE Skills_Register SHALL return an empty result set.
3. IF an empty or whitespace-only Skill name is provided, THEN THE Skills_Register SHALL return an empty result set without executing a lookup.

### Requirement 7: Display Search Results in UI

**User Story:** As an authenticated user, I want to see search results displayed in a ranked table with proficiency badges, so that I can quickly compare employees by skill level.

#### Acceptance Criteria

1. WHEN search results are displayed, THE Skills_Register SHALL show each result in a table row containing a sequential rank number (starting at 1, assigned by Skill_Ranking order), Employee name, Employee email, years of experience, project count, and Proficiency badge.
2. THE Skills_Register SHALL display Proficiency values as colour-coded badges where each Proficiency level (expert, advanced, intermediate, beginner) is assigned a visually distinct, consistent colour that remains the same across all search results.
3. WHILE a search is in progress, THE Skills_Register SHALL display a loading indicator and SHALL hide the loading indicator when results are returned or an error response is received.
4. WHEN no results match the search, THE Skills_Register SHALL display a message indicating no employees were found with that skill.
5. IF the search request fails due to a network or server error, THEN THE Skills_Register SHALL hide the loading indicator and display an error message indicating that the search could not be completed.
6. WHEN search results are displayed and the result set exceeds 50 records, THE Skills_Register SHALL display only the first 50 results.

### Requirement 8: Navigation Access

**User Story:** As an authenticated user, I want to access the Skills Register from the main navigation, so that I can find it easily within the application.

#### Acceptance Criteria

1. THE Skills_Register SHALL provide a "Skills Register" link in the application sidebar navigation visible to all authenticated users regardless of role.
2. WHEN the "Skills Register" navigation link is clicked, THE Skills_Register SHALL navigate to the skills search page at the `/skills` route.
3. WHILE the user is on the skills search page, THE Skills_Register SHALL visually indicate the "Skills Register" link as the active navigation item in the sidebar.
4. IF a non-authenticated user attempts to access the `/skills` route directly, THEN THE Skills_Register SHALL redirect the user to the login page.
