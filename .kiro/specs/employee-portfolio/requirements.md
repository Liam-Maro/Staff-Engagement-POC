# Requirements Document

## Introduction

The Employee Portfolio feature provides a structured, per-employee portfolio within the Staff Engagement system. Each portfolio comprises four distinct sections: skills (sourced read-only from the Skills Register module), education history (institutions, degrees, fields of study, graduation dates), projects worked on (name, description, role, technologies, duration), and public links (URL and label for GitHub, LinkedIn, or any showcase link). The feature replaces the current generic `PortfolioItem` model with dedicated entities per section, exposes CRUD REST endpoints under `/api/portfolios`, and renders a frontend view at `/portfolio/:employeeId`.

**Note:** The Skills section of the portfolio is read-only and sourced from the Skills Register module (`skl_skills` table via `SkillService`). Skills CRUD (create, update, delete) is managed by the `/api/skills` endpoints in the skills module. The portfolio module only reads and displays skills data.

## Glossary

- **Portfolio_Service**: The backend service responsible for managing portfolio data (education, projects, public links) and reading skills from the Skills Register module for a given employee.
- **Portfolio_Controller**: The REST controller that exposes portfolio endpoints under `/api/portfolios`.
- **Skills_Register**: The skills module (`skl_skills` table, `SkillService`) that owns skill CRUD operations. The portfolio module reads skills from this module in a read-only manner.
- **Portfolio_Education_Entity**: A JPA entity representing a single education record in an employee's portfolio, stored in table `prt_education`.
- **Portfolio_Project_Entity**: A JPA entity representing a single project entry in an employee's portfolio, stored in table `prt_projects`.
- **Portfolio_Link_Entity**: A JPA entity representing a single public link in an employee's portfolio, stored in table `prt_links`.
- **Portfolio_View_Component**: The Angular standalone component rendered at route `/portfolio/:employeeId` that displays all four portfolio sections.
- **Employee**: An existing entity (UUID id, firstName, lastName, email, department, jobTitle, active) managed by the employee module.
- **Proficiency_Level**: An enumeration representing skill mastery (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT), defined and validated by the Skills Register module.

## Requirements

### Requirement 1: Display Portfolio Skills (Read-Only from Skills Register)

**User Story:** As a staff member, I want to view an employee's skills in their portfolio, sourced from the Skills Register module, so that their competencies and proficiency levels are visible to the organisation.

**Note:** Skills CRUD (create, update, delete) is managed by the Skills Register module via `/api/skills` endpoints. The portfolio module only reads and displays skills data.

#### Acceptance Criteria

1. WHEN a request to list skills is received with a valid employeeId, THE Portfolio_Service SHALL delegate to the Skills Register module (SkillService) and return all skills associated with that employee, including skill name, proficiency level, years of experience, and project count.
2. THE skills returned SHALL be ordered by skill name ascending (as provided by the Skills Register module).
3. THE Portfolio_Controller SHALL expose a read-only endpoint at `GET /api/portfolios/{employeeId}/skills` that returns skills sourced from the Skills Register.
4. THE Portfolio_Controller SHALL NOT expose POST, PUT, or DELETE endpoints for skills — those operations are handled by the Skills Register module at `/api/skills`.

### Requirement 2: Manage Education History

**User Story:** As a staff member, I want to record an employee's education history including institutions, degrees, fields of study, and graduation dates, so that their qualifications are visible to colleagues and managers.

#### Acceptance Criteria

1. WHEN a valid education creation request is submitted with employeeId, institution, degree, field of study, and graduation date, THE Portfolio_Service SHALL persist a new Portfolio_Education_Entity and return the created record with a generated UUID.
2. WHEN a request to list education records is received with a valid employeeId, THE Portfolio_Service SHALL return all Portfolio_Education_Entity records associated with that employee ordered by graduation date descending.
3. WHEN a valid education update request is submitted with an existing education ID, THE Portfolio_Service SHALL update the institution, degree, field of study, and graduation date and return the updated record.
4. WHEN an education deletion request is received with an existing education ID, THE Portfolio_Service SHALL remove the Portfolio_Education_Entity and return no content.
5. IF an education creation request is submitted with a missing or blank institution name, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
6. IF an education creation request is submitted with a missing or blank degree, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
7. IF an education update or deletion request references a non-existent education ID, THEN THE Portfolio_Service SHALL signal a not-found condition resulting in HTTP 404.

### Requirement 3: Manage Projects Worked On

**User Story:** As a staff member, I want to document projects an employee has contributed to, including their role and technologies used, so that their practical experience is visible within the organisation.

#### Acceptance Criteria

1. WHEN a valid project creation request is submitted with employeeId, project name (required, max 255 characters), description (optional, max 2000 characters), role (required, max 255 characters), technologies used (required, max 20 entries each up to 100 characters), start date (required), and end date (optional), THE Portfolio_Service SHALL persist a new Portfolio_Project_Entity and return the created record with a generated UUID.
2. WHEN a request to list projects is received with a valid employeeId, THE Portfolio_Service SHALL return all Portfolio_Project_Entity records associated with that employee ordered by start date descending.
3. WHEN a valid project update request is submitted with an existing project ID, THE Portfolio_Service SHALL update the project name, description, role, technologies used, start date, and end date and return the updated record.
4. WHEN a project deletion request is received with an existing project ID, THE Portfolio_Service SHALL remove the Portfolio_Project_Entity and return no content.
5. IF a project creation or update request is submitted with a missing or blank project name, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a validation error indicating the missing field.
6. IF a project creation or update request is submitted with a start date after the end date, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a validation error indicating the date constraint violation.
7. IF a project update or deletion request references a non-existent project ID, THEN THE Portfolio_Service SHALL signal a not-found condition resulting in HTTP 404.
8. THE Portfolio_Project_Entity SHALL allow a null end date to represent an ongoing project.
9. IF a project creation or update request is submitted with a missing or blank role, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a validation error indicating the missing field.
10. IF a project creation or update request is submitted with an empty technologies used list, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a validation error indicating that at least one technology is required.

### Requirement 4: Manage Public Links

**User Story:** As a staff member, I want to add public links (GitHub, LinkedIn, personal site, or any showcase URL) to an employee's portfolio, so that colleagues can easily find their external profiles and work.

#### Acceptance Criteria

1. WHEN a valid link creation request is submitted with employeeId, URL (maximum 2048 characters), and label (maximum 100 characters), THE Portfolio_Service SHALL persist a new Portfolio_Link_Entity and return the created record with a generated UUID.
2. WHEN a request to list links is received with a valid employeeId, THE Portfolio_Service SHALL return all Portfolio_Link_Entity records associated with that employee.
3. WHEN a valid link update request is submitted with an existing link ID, THE Portfolio_Service SHALL update the URL and label and return the updated record.
4. WHEN a link deletion request is received with an existing link ID, THE Portfolio_Service SHALL remove the Portfolio_Link_Entity and return no content.
5. IF a link creation or update request is submitted with a missing or blank URL, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
6. IF a link creation or update request is submitted with a missing or blank label, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
7. IF a link creation or update request is submitted with a malformed URL (not a valid URI with http or https scheme), THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
8. IF a link update or deletion request references a non-existent link ID, THEN THE Portfolio_Service SHALL signal a not-found condition resulting in HTTP 404.
9. IF a link creation or update request is submitted with a URL exceeding 2048 characters or a label exceeding 100 characters, THEN THE Portfolio_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.

### Requirement 5: View Complete Portfolio

**User Story:** As a staff member, I want to view an employee's complete portfolio (all four sections in one response), so that I can quickly understand their skills, background, and contributions.

#### Acceptance Criteria

1. WHEN a request to retrieve a complete portfolio is received with a valid employeeId, THE Portfolio_Service SHALL return an aggregate response containing skills (sourced from the Skills Register module), education records, projects, and public links for that employee.
2. IF a complete portfolio request references an employeeId that does not exist in the employee module, THEN THE Portfolio_Service SHALL signal a not-found condition resulting in HTTP 404.
3. WHEN a complete portfolio is requested for an employee with no portfolio data in any section, THE Portfolio_Service SHALL return an aggregate response with empty collections for each section.

### Requirement 6: REST API Endpoint Structure

**User Story:** As a staff member, I want well-structured REST endpoints under `/api/portfolios`, so that the portfolio feature integrates consistently with the Angular application.

#### Acceptance Criteria

1. THE Portfolio_Controller SHALL expose a read-only skill endpoint at `GET /api/portfolios/{employeeId}/skills` (skills CRUD is handled by the Skills Register module at `/api/skills`).
2. THE Portfolio_Controller SHALL expose education endpoints at `POST /api/portfolios/{employeeId}/education`, `GET /api/portfolios/{employeeId}/education`, `PUT /api/portfolios/education/{educationId}`, and `DELETE /api/portfolios/education/{educationId}`.
3. THE Portfolio_Controller SHALL expose project endpoints at `POST /api/portfolios/{employeeId}/projects`, `GET /api/portfolios/{employeeId}/projects`, `PUT /api/portfolios/projects/{projectId}`, and `DELETE /api/portfolios/projects/{projectId}`.
4. THE Portfolio_Controller SHALL expose link endpoints at `POST /api/portfolios/{employeeId}/links`, `GET /api/portfolios/{employeeId}/links`, `PUT /api/portfolios/links/{linkId}`, and `DELETE /api/portfolios/links/{linkId}`.
5. THE Portfolio_Controller SHALL expose a complete portfolio endpoint at `GET /api/portfolios/{employeeId}`.
6. THE Portfolio_Controller SHALL return HTTP 201 for successful creation, HTTP 200 for successful retrieval and update, and HTTP 204 for successful deletion.
7. THE Portfolio_Controller SHALL require a valid JWT authentication token for all endpoints.

### Requirement 7: Frontend Portfolio View

**User Story:** As a staff member, I want to navigate to an employee's portfolio page and see all their skills, education, projects, and links organised in distinct sections, so that I can understand their professional profile at a glance.

#### Acceptance Criteria

1. THE Portfolio_View_Component SHALL be accessible at route `/portfolio/:employeeId` via Angular Router with lazy loading.
2. WHEN the Portfolio_View_Component loads, THE Portfolio_View_Component SHALL fetch the complete portfolio from `GET /api/portfolios/{employeeId}` and display four sections: Skills, Education, Projects, and Links.
3. THE Portfolio_View_Component SHALL display each skill entry with the skill name and proficiency level.
4. THE Portfolio_View_Component SHALL display each education entry with institution, degree, field of study, and graduation date.
5. THE Portfolio_View_Component SHALL display each project entry with project name, description, role, technologies used, start date, and end date (or "Ongoing" when end date is null).
6. THE Portfolio_View_Component SHALL display each link entry as a clickable hyperlink with the label as link text and the URL as the href target, opening in a new tab.
7. WHEN the portfolio data is loading, THE Portfolio_View_Component SHALL display a loading indicator.
8. IF the portfolio fetch fails with HTTP 404, THEN THE Portfolio_View_Component SHALL display a "Portfolio not found" message.
9. IF the portfolio fetch fails with a network or server error, THEN THE Portfolio_View_Component SHALL display an error message with a retry option.

### Requirement 8: Portfolio Data Validation and Integrity

**User Story:** As a staff member, I want portfolio data to be validated and referentially consistent, so that the database remains clean and reliable.

#### Acceptance Criteria

1. THE Portfolio_Education_Entity SHALL store a non-null employeeId referencing an existing Employee.
2. THE Portfolio_Project_Entity SHALL store a non-null employeeId referencing an existing Employee.
3. THE Portfolio_Link_Entity SHALL store a non-null employeeId referencing an existing Employee.
4. IF a portfolio entry creation request references an employeeId that does not exist, THEN THE Portfolio_Service SHALL signal a not-found condition resulting in HTTP 404.
5. THE Portfolio_Project_Entity SHALL enforce that start date is not null for any project entry.

**Note:** Skills validation (proficiency level, skill name constraints) is handled by the Skills Register module, not the portfolio module.
