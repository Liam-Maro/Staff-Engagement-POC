# Requirements Document

## Introduction

The Interaction Module provides the ability to create, view, update, and manage interaction records within the Staff Engagement system. Interactions represent engagement events (check-ins, mentoring sessions, catch-ups) between a staff member and an employee. Each interaction captures who was involved, when it occurred, what type it was, and any notes recorded. The module also supports spawning follow-up tasks from interactions via integration with the Task module. The backend exposes REST endpoints under `/api/interactions`, with data stored in PostgreSQL tables prefixed `int_`. The frontend renders interaction views under `/interactions` routes.

## Glossary

- **Interaction_Service**: The backend service responsible for creating, retrieving, updating, and deleting interaction records.
- **Interaction_Controller**: The REST controller that exposes interaction endpoints under `/api/interactions`.
- **Interaction_Entity**: A JPA entity representing a single interaction record, stored in table `int_interactions`.
- **Task_Module**: The task module (`tsk_` tables, `TaskService`) that owns task CRUD operations. The interaction module delegates follow-up task creation to this module.
- **Staff_Member**: A user of the system (identified by staffId) who logs interactions and creates follow-up tasks.
- **Employee**: An existing entity (UUID id, firstName, lastName, email, department, jobTitle, active) managed by the employee module, representing the person an interaction is about.
- **Interaction_Type**: A classification for interactions. Valid types are: CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL.
- **Interaction_List_Component**: The Angular standalone component that displays a list of interactions, filterable by employee.
- **Interaction_Detail_Component**: The Angular standalone component that displays a single interaction record with full details.
- **Interaction_Form_Component**: The Angular standalone component that provides a form for creating or editing an interaction.

## Requirements

### Requirement 1: Create an Interaction Record

**User Story:** As a staff member, I want to create a new interaction record linked to an employee, so that I can document an engagement event (check-in, mentoring session, catch-up) and maintain a history of interactions.

#### Acceptance Criteria

1. WHEN a valid interaction creation request is submitted with employeeId (required), staffId (required), type (required, one of CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL), notes (optional, max 5000 characters), and occurredAt (required, a valid date-time not in the future relative to server time), THE Interaction_Service SHALL persist a new Interaction_Entity and return the created record with a generated UUID, createdAt timestamp, updatedAt timestamp, and all submitted fields.
2. IF an interaction creation request is submitted with a missing or null employeeId, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error identifying the employeeId field as required.
3. IF an interaction creation request is submitted with a missing or null staffId, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error identifying the staffId field as required.
4. IF an interaction creation request is submitted with a missing or blank type, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error identifying the type field as required.
5. IF an interaction creation request is submitted with a type value not in the allowed set (CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL), THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error listing the allowed type values.
6. IF an interaction creation request is submitted with a missing or null occurredAt, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error identifying the occurredAt field as required.
7. IF an interaction creation request is submitted with an occurredAt value in the future relative to the server's current time, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error indicating that interactions cannot be recorded for future dates.
8. IF an interaction creation request references an employeeId that does not exist in the employee module, THEN THE Interaction_Service SHALL signal a not-found condition resulting in HTTP 404.
9. IF an interaction creation request references a staffId that does not exist in the staff module, THEN THE Interaction_Service SHALL signal a not-found condition resulting in HTTP 404.
10. THE Interaction_Controller SHALL return HTTP 201 for a successful interaction creation.

### Requirement 2: Retrieve Interaction Records

**User Story:** As a staff member, I want to retrieve interaction records by ID or filtered by employee, so that I can review past engagement history for a specific person or across the organisation.

#### Acceptance Criteria

1. WHEN a request to retrieve an interaction by ID is received with a valid interaction UUID, THE Interaction_Service SHALL return the full Interaction_Entity including id, employeeId, staffId, type, notes, occurredAt, createdAt, and updatedAt.
2. WHEN a request to list interactions is received with a valid employeeId query parameter, THE Interaction_Service SHALL return all Interaction_Entity records associated with that employee ordered by occurredAt descending.
3. WHEN a request to list interactions is received without an employeeId query parameter, THE Interaction_Service SHALL return all Interaction_Entity records ordered by occurredAt descending.
4. IF a request to retrieve an interaction by ID references a non-existent interaction UUID, THEN THE Interaction_Service SHALL signal a not-found condition resulting in HTTP 404.
5. THE Interaction_Controller SHALL expose a retrieval endpoint at `GET /api/interactions/{id}` that returns a single interaction with HTTP 200 on success.
6. THE Interaction_Controller SHALL expose a list endpoint at `GET /api/interactions` that accepts an optional `employeeId` query parameter for filtering and returns HTTP 200 on success.
7. IF a request to retrieve an interaction by ID provides an `id` path parameter that is not a valid UUID format, THEN THE Interaction_Controller SHALL reject the request with HTTP 400.
8. IF a request to list interactions provides an `employeeId` query parameter that is not a valid UUID format, THEN THE Interaction_Controller SHALL reject the request with HTTP 400.

### Requirement 3: Update an Interaction Record

**User Story:** As a staff member, I want to update an existing interaction record, so that I can correct or add information after the initial recording.

#### Acceptance Criteria

1. WHEN a valid interaction update request is submitted with an existing interaction ID, THE Interaction_Service SHALL update the type, notes, and occurredAt fields and return the updated record with an updated updatedAt timestamp.
2. IF an interaction update request references a non-existent interaction ID, THEN THE Interaction_Service SHALL signal a not-found condition resulting in HTTP 404.
3. IF an interaction update request is submitted with a type value not in the allowed set (CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL), THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
4. IF an interaction update request is submitted with an occurredAt value in the future, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error indicating that interactions cannot be recorded for future dates.
5. IF an interaction update request includes employeeId or staffId fields in the request body, THEN THE Interaction_Service SHALL ignore those fields and preserve the original values assigned at creation.
6. IF an interaction update request is submitted with notes exceeding 5000 characters, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
7. THE Interaction_Controller SHALL expose an update endpoint at `PUT /api/interactions/{id}` that requires type (required), occurredAt (required), and notes (optional, max 5000 characters) in the request body, and return HTTP 200 for a successful update.

### Requirement 4: Delete an Interaction Record

**User Story:** As a staff member, I want to delete an interaction record, so that I can remove incorrect or duplicate entries from the system.

#### Acceptance Criteria

1. WHEN a valid deletion request is received with an existing interaction UUID, THE Interaction_Service SHALL permanently remove the Interaction_Entity from the database and any follow-up tasks previously spawned from that interaction SHALL retain their data but no longer reference the deleted interaction.
2. IF a deletion request references a non-existent interaction UUID, THEN THE Interaction_Service SHALL signal a not-found condition resulting in HTTP 404.
3. THE Interaction_Controller SHALL expose a deletion endpoint at `DELETE /api/interactions/{id}` and return HTTP 204 with no response body for a successful deletion.
4. IF a deletion request provides an interaction ID that is not a valid UUID format, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.

### Requirement 5: Spawn Follow-Up Task from Interaction

**User Story:** As a staff member, I want to create a follow-up task directly from an interaction record, so that action items identified during an engagement are tracked and assigned.

#### Acceptance Criteria

1. WHEN a valid follow-up task creation request is submitted with an existing interaction ID, task title (required, max 255 characters), task description (optional, max 2000 characters), and due date (optional, must be today or in the future if provided), THE Interaction_Service SHALL delegate task creation to the Task_Module with the interaction's employeeId and the originating interaction ID, and return the created task reference including the task's generated ID, status, and creation timestamp.
2. IF a follow-up task creation request references a non-existent interaction ID, THEN THE Interaction_Service SHALL signal a not-found condition resulting in HTTP 404.
3. IF a follow-up task creation request is submitted with a missing or blank task title, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
4. IF a follow-up task creation request is submitted with a task title exceeding 255 characters, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error indicating the title length limit.
5. IF a follow-up task creation request is submitted with a task description exceeding 2000 characters, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error indicating the description length limit.
6. IF a follow-up task creation request is submitted with a due date in the past, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error indicating that due date must be today or in the future.
7. IF the Task_Module returns an error during delegated task creation, THEN THE Interaction_Service SHALL propagate the error resulting in HTTP 500 with a message indicating that task creation failed.
8. THE Interaction_Controller SHALL expose a follow-up task endpoint at `POST /api/interactions/{id}/tasks` and return HTTP 201 for a successful task creation.
9. THE created task SHALL reference the originating interaction ID so the relationship is traceable.

### Requirement 6: Paginate and Filter Interaction Lists

**User Story:** As a staff member, I want to paginate and filter interaction lists, so that I can efficiently browse large numbers of interaction records.

#### Acceptance Criteria

1. WHEN a list interactions request includes page and size query parameters, THE Interaction_Service SHALL return a paginated response containing the requested page of results, total element count, total page count, and current page number, ordered by occurredAt descending.
2. IF pagination parameters are not provided in a list interactions request, THEN THE Interaction_Controller SHALL default to page 0 and size 20.
3. IF the size query parameter exceeds 100, THEN THE Interaction_Controller SHALL cap the page size to 100.
4. WHEN a list interactions request includes a type query parameter with a valid Interaction_Type value, THE Interaction_Service SHALL return only interactions matching the specified type.
5. IF a list interactions request includes a type query parameter with a value not in the allowed Interaction_Type set (CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL), THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a descriptive validation error.
6. WHEN a list interactions request includes fromDate and toDate query parameters where fromDate is on or before toDate, THE Interaction_Service SHALL return only interactions with occurredAt within the specified date range (inclusive on both bounds).
7. IF a list interactions request includes fromDate and toDate query parameters where fromDate is after toDate, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error indicating the date range is invalid.
8. THE Interaction_Controller SHALL support combining employeeId, type, fromDate, and toDate filters simultaneously, applying all provided filters with AND logic.

### Requirement 7: Track Interaction Update History

**User Story:** As a staff member, I want interaction records to track when they were last modified, so that I can see whether notes have been updated since the original recording.

#### Acceptance Criteria

1. THE Interaction_Entity SHALL store a createdAt timestamp that is set once on initial persistence and is never modified by any subsequent update operation.
2. THE Interaction_Entity SHALL store an updatedAt timestamp that is set equal to createdAt on initial persistence and updated to the current server time on every subsequent PUT request to that record, regardless of whether field values changed.
3. WHEN an interaction record is retrieved, THE Interaction_Service SHALL include both createdAt and updatedAt as ISO-8601 date-time values in the response.
4. WHEN an interaction record is created, THE Interaction_Service SHALL return the record with createdAt equal to updatedAt, confirming no modification has occurred since initial persistence.

### Requirement 8: Frontend Interaction List View

**User Story:** As a staff member, I want to view a list of interactions in the frontend, with filtering by employee and interaction type, so that I can quickly review engagement history.

#### Acceptance Criteria

1. THE Interaction_List_Component SHALL be accessible at route `/interactions` via Angular Router with lazy loading.
2. WHEN the Interaction_List_Component loads, THE Interaction_List_Component SHALL fetch the first page of interactions from `GET /api/interactions` (page 0, size 20) and display them in a table or card layout showing type, employee name, occurred date, and a notes preview truncated to 100 characters with an ellipsis appended when the full text exceeds that length.
3. WHEN the user applies a filter by employee (searchable dropdown) or interaction type (multi-select from the values CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL), THE Interaction_List_Component SHALL send the selected filter parameters to `GET /api/interactions`, reset pagination to the first page, and update the displayed results.
4. THE Interaction_List_Component SHALL display pagination controls showing the current page number, total pages, and next/previous navigation buttons, defaulting to page size 20 as defined by the backend, and SHALL fetch the corresponding page from the API when the user navigates.
5. WHEN the interaction data is loading, THE Interaction_List_Component SHALL display a loading indicator.
6. IF the interaction fetch fails with a network or server error (HTTP 5xx or no response), THEN THE Interaction_List_Component SHALL display an error message indicating the failure reason and a retry button that re-sends the last request.
7. IF no interaction records match the current filters and pagination, THEN THE Interaction_List_Component SHALL display an empty-state message indicating that no interactions were found and suggesting the user adjust filters.

### Requirement 9: Frontend Interaction Detail and Form Views

**User Story:** As a staff member, I want to view full details of an interaction and create or edit interactions through a form, so that I can manage engagement records directly in the UI.

#### Acceptance Criteria

1. THE Interaction_Detail_Component SHALL be accessible at route `/interactions/:id` via Angular Router with lazy loading.
2. WHEN the Interaction_Detail_Component loads, THE Interaction_Detail_Component SHALL fetch the interaction from `GET /api/interactions/{id}`, resolve employeeId and staffId to display employee name and staff member name, and display all fields: type, employee name, staff member name, occurred date, notes (full text), createdAt, and updatedAt.
3. THE Interaction_Detail_Component SHALL provide a button to spawn a follow-up task that navigates to the task creation form pre-populated with the interaction's employeeId, staffId, and interaction ID.
4. THE Interaction_Form_Component SHALL provide an Angular reactive form for creating a new interaction with fields: employee (searchable dropdown), type (select from allowed types: CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL), occurredAt (date-time picker constrained to dates not in the future), and notes (textarea with a maximum length of 5000 characters).
5. THE Interaction_Form_Component SHALL provide the same form for editing an existing interaction, pre-populated with current values, with employeeId and staffId fields disabled.
6. WHEN the form is submitted with valid data, THE Interaction_Form_Component SHALL call the appropriate API endpoint (POST `/api/interactions` for create, PUT `/api/interactions/{id}` for update) and navigate to the interaction list at `/interactions` on success.
7. THE Interaction_Form_Component SHALL enforce client-side validation requiring employee, type, and occurredAt before enabling form submission, and SHALL display inline validation messages for any field that fails validation.
8. IF form submission fails with a validation error (HTTP 400), THEN THE Interaction_Form_Component SHALL display the server validation messages inline next to the relevant fields.
9. IF form submission fails with a server error (HTTP 5xx), THEN THE Interaction_Form_Component SHALL display a general error message with a retry option that resubmits the form data.
10. IF the detail fetch fails with HTTP 404, THEN THE Interaction_Detail_Component SHALL display a message indicating the interaction was not found and provide a link to navigate back to the interaction list.
11. IF the detail fetch fails with a server error (HTTP 5xx) or network error, THEN THE Interaction_Detail_Component SHALL display an error message with a retry option that re-attempts the fetch.

### Requirement 10: Data Integrity and Validation Constraints

**User Story:** As a staff member, I want interaction data to be validated and referentially consistent, so that the database remains clean and reliable.

#### Acceptance Criteria

1. THE Interaction_Entity SHALL store a non-null employeeId referencing an existing Employee via a foreign key constraint that prevents deletion of an Employee while associated interaction records exist.
2. THE Interaction_Entity SHALL store a non-null staffId referencing an existing Staff_Member via a foreign key constraint that prevents deletion of a Staff_Member while associated interaction records exist.
3. THE Interaction_Entity SHALL store a non-null type that is constrained at the database level to one of the allowed Interaction_Type values (CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL).
4. THE Interaction_Entity SHALL store a non-null occurredAt timestamp that must not be later than the current time at the point of persistence.
5. THE Interaction_Entity SHALL enforce that notes do not exceed 5000 characters at the database column level.
6. IF an interaction creation or update request contains notes exceeding 5000 characters, THEN THE Interaction_Controller SHALL reject the request with HTTP 400 and a validation error indicating the maximum allowed length is 5000 characters.
7. IF a deletion request targets an Employee or Staff_Member that is referenced by one or more Interaction_Entity records, THEN THE system SHALL reject the deletion and return an error indicating the entity cannot be removed while associated interactions exist.
8. IF an interaction creation or update request references an employeeId or staffId that does not correspond to an existing record, THEN THE Interaction_Service SHALL reject the operation with an error indicating the referenced entity was not found.
