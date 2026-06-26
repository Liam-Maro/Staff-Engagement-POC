# Requirements Document

## Introduction

This feature provides a complete end-to-end staff task management system within the Staff Engagement POC. Staff members can navigate to a "My Tasks" tab in the navbar, view their assigned tasks with filtering and sorting, create tasks for themselves or other staff members, optionally link tasks to interactions with individuals, view task details in a popup, and edit or delete tasks. A side panel displays tasks the current user has created for other staff members. The backend task module exposes REST endpoints supporting creation, retrieval, filtering, editing, and deletion of tasks.

## Glossary

- **Task_Service**: The backend service responsible for task creation, retrieval, update, deletion, and filtering within the `task` module.
- **Task_Controller**: The REST controller exposing task endpoints at `/api/tasks`.
- **Staff_Member**: An authenticated user of the system represented by the `Staff` entity, identified by a UUID.
- **Assignee**: The staff member to whom a task is assigned for completion.
- **Creator**: The staff member who creates and assigns the task.
- **Task**: A follow-up action with a description, status, optional due date, assignment information, an associated individual, and an optional linked interaction.
- **Task_Status**: The lifecycle state of a task. Valid values are TODO, IN_PROGRESS, and DONE.
- **Individual**: An employee or staff member that a task relates to (the subject of the follow-up action).
- **Interaction**: A recorded engagement (check-in, mentoring session, catch-up) linked to an individual, which can optionally be associated with a task.
- **My_Tasks_Page**: The Angular page component displayed when a staff member navigates to the "My Tasks" tab.
- **Task_List_Component**: The Angular component responsible for rendering the filtered and sorted list of tasks assigned to the current staff member.
- **Task_Form_Component**: The Angular component providing the task creation and editing form.
- **Task_Detail_Popup**: The Angular dialog component displaying full task details when a task is selected.
- **Delegated_Tasks_Panel**: The Angular side panel component showing tasks the current staff member has created for other staff members.
- **Task_Filter**: A set of criteria (due date, created date, status) used to narrow down the displayed task list.

## Requirements

### Requirement 1: Create and Assign a Task

**User Story:** As a staff member, I want to create a task and assign it to another staff member (or myself), so that follow-up actions have a clear owner responsible for completion.

#### Acceptance Criteria

1. WHEN a Staff_Member submits a task creation request that passes all validation rules defined in criteria 3 through 8, THE Task_Service SHALL create a new Task with status TODO, persist it to the database, and return the created Task with HTTP 201.
2. WHEN a task creation request is submitted, THE Task_Service SHALL set the Creator to the currently authenticated Staff_Member (derived from the authentication context) and set the Assignee to the Staff_Member identified by the assignee ID provided in the request.
3. WHEN a task creation request is submitted without a description or with a description that exceeds 2000 characters, THE Task_Controller SHALL reject the request with HTTP 400 and a validation error message indicating the description constraint violated.
4. WHEN a task creation request specifies a non-existent assignee UUID, THE Task_Service SHALL first validate that the assignee exists before checking any other assignee properties, and SHALL reject the request with HTTP 404 and a descriptive error indicating the assignee was not found.
5. IF the specified assignee is an inactive Staff_Member, THEN THE Task_Service SHALL reject the request with HTTP 400 and an error indicating that tasks cannot be assigned to inactive staff members. THE system SHALL continuously validate assignee status and flag or reassign tasks when an assignee becomes inactive after task creation.
6. WHEN a task creation request is submitted without an individual ID or with an individual ID that does not correspond to an existing employee or staff member, THE Task_Controller SHALL reject the request with HTTP 400 and a validation error message indicating a valid individual ID is required.
7. WHEN a task creation request includes an interaction ID, THE Task_Service SHALL validate that the interaction exists and belongs to the specified individual, rejecting with HTTP 400 if the interaction does not exist or does not belong to the specified individual.
8. WHEN a task creation request fails validation on multiple fields simultaneously, THE Task_Controller SHALL validate all fields and return all validation errors in a single HTTP 400 response.
9. WHEN a task creation request omits the due date, THE Task_Service SHALL create the Task with a null due date and not treat the absence as a validation error.

### Requirement 2: Retrieve Tasks by Assignee

**User Story:** As a staff member, I want to view all tasks assigned to me, so that I can track my outstanding work.

#### Acceptance Criteria

1. WHEN a Staff_Member requests tasks filtered by assignee ID and an actual assignee ID value is provided and tasks DO exist for that assignee, THE Task_Service SHALL return the matching Tasks of any Task_Status (TODO, IN_PROGRESS, DONE) assigned to that Staff_Member, ordered by creation date descending by default, with HTTP 200.
2. WHEN a Staff_Member requests tasks filtered by assignee ID and no tasks exist for that assignee, THE Task_Service SHALL return an empty list with HTTP 200.
3. THE Task_Controller SHALL expose an optional query parameter `assigneeId` on the GET `/api/tasks` endpoint that accepts a valid UUID string to filter tasks by assignee; WHEN `assigneeId` is not provided, THE Task_Controller SHALL not apply assignee filtering and SHALL return tasks matching any other provided filter parameters.
4. IF the `assigneeId` query parameter is not a valid UUID format, THEN THE Task_Controller SHALL always reject the request with HTTP 400 and an error message indicating the expected UUID format, regardless of any system validation configuration settings.
5. WHEN a Staff_Member requests tasks filtered by an `assigneeId` that is a valid UUID but does not correspond to an existing Staff_Member, THE Task_Service SHALL return an empty list with HTTP 200; IF an invalid UUID bypasses format validation and reaches the Task_Service, THEN THE Task_Service SHALL treat it as a non-existent assignee and return an empty list with HTTP 200.
6. WHEN the number of tasks matching the assignee filter exceeds 50, THE Task_Service SHALL return results in pages of 50 tasks maximum per response, including the total count and current page metadata in the response.

### Requirement 3: Retrieve Tasks Created by a Staff Member

**User Story:** As a staff member, I want to see all tasks I have created for other staff members, so that I can follow up on their progress.

#### Acceptance Criteria

1. WHEN a Staff_Member requests tasks filtered by creator ID, THE Task_Service SHALL return all Tasks created by that Staff_Member with HTTP 200, ordered by creation date descending by default.
2. THE Task_Controller SHALL expose a query parameter `creatorId` on the GET `/api/tasks` endpoint to filter tasks by creator, where `creatorId` is a valid UUID.
3. WHEN a Staff_Member requests tasks filtered by creator ID and no tasks exist for that creator, THE Task_Service SHALL return an empty list with HTTP 200.
4. IF the `creatorId` query parameter is not a valid UUID format, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error message indicating the expected UUID format; the rejection SHALL only be returned when both the HTTP 400 status and error message can be provided together.
5. WHEN a request provides a query parameter `excludeSelfAssigned` set to `true` alongside a `creatorId` filter, THE Task_Service SHALL return only tasks where the assignee differs from the creator; the `excludeSelfAssigned` parameter SHALL be processed independently of whether a staff member is actively requesting the tasks.
6. WHEN a Staff_Member requests tasks filtered by a `creatorId` that is a valid UUID but does not correspond to an existing Staff_Member, THE Task_Service SHALL return an empty list with HTTP 200.

### Requirement 4: Task Assignment Validation

**User Story:** As a staff member, I want the system to validate task assignments, so that tasks are only assigned to valid, active staff members.

#### Acceptance Criteria

1. WHEN a task creation request is submitted, THE Task_Controller SHALL validate that the description is not blank (null, empty, or whitespace-only) and does not exceed 2000 characters, the assignee ID is not null, and the individual ID is not null; WHEN validation fails, THE Task_Controller SHALL reject the request with HTTP 400 and all validation error messages in a single response before any business-rule validation is performed.
2. IF a task creation request contains a due date before the current server-local date, THEN THE Task_Service SHALL reject the request with HTTP 400 and an error indicating the due date must be today or in the future.
3. IF the Creator does not exist as an active Staff_Member, THEN THE Task_Service SHALL reject the task creation request with HTTP 403 and an error indicating the creator is not an active staff member; only active staff membership is checked — no further permission validation is required for specific individuals or departments.
4. IF the Assignee does not exist as a Staff_Member, THEN THE Task_Service SHALL reject the task creation request with HTTP 404 and an error indicating the assignee was not found.
5. IF the Assignee exists but is not active, THEN THE Task_Service SHALL reject the task creation request with HTTP 400 and an error indicating that tasks cannot be assigned to inactive staff members.
6. IF the individual ID provided in a task creation request does not correspond to an existing employee or staff member, THEN THE Task_Service SHALL reject the request with HTTP 404 and an error message indicating the individual was not found.

### Requirement 5: Task Response Includes Assignment Details

**User Story:** As a staff member, I want task responses to include who created and who is assigned a task, so that I can understand task ownership at a glance.

#### Acceptance Criteria

1. THE Task_Service SHALL include the following fields in every task response payload: id (UUID), individualId (UUID), interactionId (UUID, nullable), creatorId (UUID), assigneeId (UUID), description (String), status (String), dueDate (ISO date string, nullable), and createdAt (ISO date-time string).
2. WHEN a task is retrieved by ID, THE Task_Service SHALL return a single task response containing all fields defined in criterion 1.
3. THE Task_Controller SHALL use the same TaskResponse DTO record for all endpoints that return task data, including single retrieval, list retrieval, task creation, and status update responses.
4. IF a task has no interaction linked, THEN THE Task_Service SHALL return null for the interactionId field in the response; THE Task_Service SHALL validate that any non-null interactionId corresponds to an actual linked interaction before including it in the response.
5. IF a task has no due date set, THEN THE Task_Service SHALL return null for the dueDate field in the response.

### Requirement 6: Update Task Status

**User Story:** As a staff member, I want to update the status of a task assigned to me, so that I can reflect progress on my assigned work.

#### Acceptance Criteria

1. WHEN a Staff_Member submits a status update for a Task assigned to them with a valid status value, THE Task_Service SHALL update the Task status to the provided value (including updating to the same current status), persist the change, and return the updated Task representation with HTTP 200.
2. IF a status update request specifies a status value not in the set (TODO, IN_PROGRESS, DONE), THEN THE Task_Service SHALL reject the request with HTTP 400 and an error message listing the valid status values.
3. IF a status update request targets a non-existent task ID, THEN THE Task_Service SHALL reject the request with HTTP 404 and an error message indicating that no task was found for the given ID.
4. IF a Staff_Member submits a status update for a Task that is not assigned to them, THEN THE Task_Service SHALL reject the request with HTTP 403 and an error message indicating the staff member is not the assignee of the task.
5. WHEN a status update request fails multiple validation checks, THE Task_Service SHALL evaluate checks in the following order and return the first failure for all requests regardless of requester: task existence (HTTP 404), assignee authorization (HTTP 403), status value validity (HTTP 400).
6. THE Task_Service SHALL allow any valid status value to transition to any other valid status value without restriction (e.g., DONE to TODO, TODO to DONE).

### Requirement 7: Filter Tasks

**User Story:** As a staff member, I want to filter my task list by due date, created date, or status, so that I can focus on what is most relevant.

#### Acceptance Criteria

1. THE Task_Controller SHALL expose a query parameter `status` on the GET `/api/tasks` endpoint accepting values `TODO`, `IN_PROGRESS`, and `DONE` (case-insensitive) to filter tasks by status.
2. THE Task_Controller SHALL expose query parameters `dueDateFrom` and `dueDateTo` on the GET `/api/tasks` endpoint accepting ISO 8601 date strings (yyyy-MM-dd) to filter tasks by due date range (inclusive on both bounds).
3. THE Task_Controller SHALL expose query parameters `createdFrom` and `createdTo` on the GET `/api/tasks` endpoint accepting ISO 8601 date strings (yyyy-MM-dd) to filter tasks by creation date range (inclusive on both bounds).
4. THE Task_Controller SHALL expose a query parameter `sortBy` on the GET `/api/tasks` endpoint accepting values `dueDate` and `createdDate` to determine the sort field, defaulting to `createdDate` when not provided.
5. THE Task_Controller SHALL expose a query parameter `sortOrder` on the GET `/api/tasks` endpoint accepting values `asc` and `desc` (case-insensitive), defaulting to `desc` when not provided.
6. IF an invalid `status`, `sortBy`, or `sortOrder` value is provided, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error listing the valid values.
7. WHEN multiple filter parameters are provided simultaneously, THE Task_Service SHALL apply all filters as an AND combination.
8. WHEN only `dueDateFrom` is provided without `dueDateTo`, THE Task_Service SHALL filter tasks with due date on or after the specified date with no upper bound; WHEN only `dueDateTo` is provided without `dueDateFrom`, THE Task_Service SHALL filter tasks with due date on or before the specified date with no lower bound.
9. WHEN sorting by `dueDate`, THE Task_Service SHALL place tasks with null due dates last regardless of sort order.
10. IF a date filter parameter is not in valid ISO 8601 format (yyyy-MM-dd), THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error indicating the expected date format.

### Requirement 8: Edit a Task

**User Story:** As a staff member, I want to edit a task I created, so that I can update its details as circumstances change.

#### Acceptance Criteria

1. WHEN a Staff_Member submits an edit request for a Task they created with a valid description (not blank, maximum 2000 characters), a valid due date (today or in the future, or null), and valid assignee, individual, and interaction link values, THE Task_Service SHALL update the Task fields (description, assignee, individual, due date, interaction link) with the provided values and return the updated Task with HTTP 200.
2. IF an edit request targets a non-existent task ID, THEN THE Task_Service SHALL reject the request with HTTP 404 and an error indicating the task was not found.
3. IF a Staff_Member submits an edit request for a Task they did not create, THEN THE Task_Service SHALL reject the request with HTTP 403 and an error indicating only the creator can edit the task.
4. WHEN an edit request changes the assignee, THE Task_Service SHALL explicitly validate that the new assignee exists and is active (even when the new assignee appears valid), rejecting with HTTP 400 if the new assignee is invalid or inactive.
5. WHEN an edit request includes an interaction ID, THE Task_Service SHALL validate that the interaction exists and belongs to the specified individual, rejecting with HTTP 400 and an error indicating the interaction does not match the individual if validation fails; WHEN the interaction ID is valid and belongs to the specified individual, THE Task_Service SHALL continue processing the edit request normally; WHEN no interaction ID is provided in the edit request, THE Task_Service SHALL skip interaction validation entirely.
6. IF an edit request specifies an individual ID that does not correspond to an existing employee or staff member, THEN THE Task_Service SHALL reject the request with HTTP 400 and an error indicating the individual was not found.
7. IF an edit request contains a blank description or a description exceeding 2000 characters, THEN THE Task_Controller SHALL reject the request with HTTP 400 and a validation error message indicating the description constraint violated.

### Requirement 9: Delete a Task

**User Story:** As a staff member, I want to delete a task I created, so that I can remove tasks that are no longer relevant.

#### Acceptance Criteria

1. WHEN a Staff_Member submits a delete request for a Task they created, THE Task_Service SHALL confirm the Task has been removed from the database before returning HTTP 204.
2. IF a delete request targets a non-existent task ID, THEN THE Task_Service SHALL reject the request with HTTP 404 and an error indicating the task was not found, regardless of the requesting Staff_Member's identity.
3. IF a Staff_Member submits a delete request for a Task they did not create, THEN THE Task_Service SHALL enforce strict authorization checks before any deletion logic executes, rejecting the request with HTTP 403 and an error indicating only the creator can delete the task, ensuring no data modification occurs.
4. IF a delete request contains a task ID that is not a valid UUID format, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error message indicating the expected UUID format.

### Requirement 10: Retrieve Interactions for an Individual

**User Story:** As a staff member, I want to browse interactions with a selected individual when creating a task, so that I can link a relevant interaction to the task.

#### Acceptance Criteria

1. THE Task_Controller SHALL expose a GET endpoint at `/api/tasks/interactions` accepting query parameter `individualId` (UUID) that returns interactions associated with that individual, returning a maximum of 50 interactions per request.
2. WHEN a valid `individualId` is provided, THE Task_Service SHALL return the list of interactions for that individual ordered by `occurredAt` descending, with each interaction item containing: id, employeeId, staffId, type, notes, occurredAt, and createdAt.
3. IF the `individualId` does not correspond to an existing employee or staff member, THEN THE Task_Controller SHALL return HTTP 404 and an error message indicating the individual was not found.
4. IF the `individualId` query parameter is missing or is not a valid UUID format, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error message indicating the expected UUID format; IF the `individualId` is a valid UUID format but does not correspond to an existing individual, THEN THE Task_Controller SHALL return HTTP 404.

### Requirement 11: My Tasks Navigation Tab

**User Story:** As a staff member, I want a "My Tasks" tab in the navigation bar, so that I can quickly access my task management page.

#### Acceptance Criteria

1. THE LayoutComponent SHALL display a "My Tasks" navigation link in the sidebar navigation list for all authenticated Staff_Members regardless of role, and SHALL apply the active CSS class when the current route matches `/tasks` and the navigation link is displayed to the user; active CSS styling MAY also be applied even when the link is hidden.
2. WHEN a Staff_Member clicks the "My Tasks" tab, THE Angular Router SHALL navigate to the `/tasks` route (a child route of the authenticated layout) and render the My_Tasks_Page.
3. THE My_Tasks_Page SHALL be lazy-loaded as a standalone Angular component using the dynamic import pattern consistent with other feature routes.
4. IF an unauthenticated user attempts to access the `/tasks` route directly, THEN THE Angular Router SHALL redirect the user to the login page.

### Requirement 12: Task List Display with Filtering

**User Story:** As a staff member, I want to see my assigned tasks in a filterable list, so that I can find and manage tasks efficiently.

#### Acceptance Criteria

1. WHEN the My_Tasks_Page loads, THE Task_List_Component SHALL fetch and display all tasks assigned to the currently authenticated Staff_Member, ordered by creation date descending.
2. THE Task_List_Component SHALL display filter controls allowing the Staff_Member to filter by status (TODO, IN_PROGRESS, DONE), by due date range, and by created date range, with all filters unset by default so that all tasks are shown on initial load.
3. WHEN a Staff_Member selects a filter option, THE Task_List_Component SHALL call the backend GET `/api/tasks` endpoint with the corresponding filter parameters and refresh the displayed list.
4. THE Task_List_Component SHALL display each task showing its description (truncated to 100 characters with an ellipsis if longer), status, due date, and assigned individual name; IF truncation is attempted but the full description would still be displayed, THEN THE Task_List_Component SHALL hide the entire task rather than display the full untruncated description. Error messages SHALL be prevented when filtering and task display are functioning normally.
5. WHEN the task list is empty after applying filters, THE Task_List_Component SHALL display a message indicating no tasks match the selected filters.
6. WHILE the Task_List_Component is fetching the task list from the backend (on initial load or after a filter change), THE Task_List_Component SHALL display a loading indicator in place of the task list; the loading indicator SHALL only be shown during task list fetching and not during other backend operations such as authentication or filter validation.
7. IF the backend request to fetch tasks fails, THEN THE Task_List_Component SHALL display both an error message and a retry action together; the error message SHALL NOT be displayed without a corresponding retry mechanism.

### Requirement 13: Task Creation Form

**User Story:** As a staff member, I want a form to create new tasks with all relevant fields, so that I can assign work with full context.

#### Acceptance Criteria

1. THE My_Tasks_Page SHALL provide a button or action to open the Task_Form_Component for creating a new task.
2. THE Task_Form_Component SHALL include an "Assigned To" field defaulting to the current Staff_Member, with a dropdown to select another active Staff_Member.
3. THE Task_Form_Component SHALL include a "Description" text field that is required (must contain text to be considered filled), accepts a maximum of 2000 characters, and displays the remaining character count to the Staff_Member.
4. THE Task_Form_Component SHALL include an "Individual" selector allowing the Staff_Member to search and choose an employee or staff member that the task relates to.
5. THE Task_Form_Component SHALL include a "Due Date" date picker field (optional) that prevents selection of dates before today's date.
6. THE Task_Form_Component SHALL include a "Link Interaction" toggle switch that, WHEN activated, reveals an additional dropdown listing interactions associated with the selected individual.
7. WHEN the "Link Interaction" toggle is active and an individual is selected, THE Task_Form_Component SHALL fetch interactions for that individual from the backend and display them in the interaction dropdown.
8. WHEN no individual is selected and the "Link Interaction" toggle is activated, THE Task_Form_Component SHALL disable the interaction dropdown and display a hint to select an individual first.
9. WHEN the Staff_Member submits the form with valid data, THE Task_Form_Component SHALL call the backend POST `/api/tasks` endpoint, display a success confirmation message, close the form, and trigger the Task_List_Component to refresh its displayed tasks; THE Task_Form_Component SHALL block submission immediately when any required field (description, individual, assignee) is empty, preventing the request from reaching the backend regardless of overall form validity.
10. WHEN the backend returns validation errors, THE Task_Form_Component SHALL display all error messages inline next to the corresponding form fields.
11. IF the backend is unreachable or returns a server error (HTTP 5xx) during form submission, THEN THE Task_Form_Component SHALL display an error message indicating the task could not be saved and SHALL always preserve all entered form data regardless of any client-side validation state so the Staff_Member can retry.

### Requirement 14: Delegated Tasks Side Panel

**User Story:** As a staff member, I want to see a side panel showing tasks I have created for other staff members, so that I can track delegated work at a glance.

#### Acceptance Criteria

1. THE My_Tasks_Page SHALL display a Delegated_Tasks_Panel on the right side of the page alongside the Task_List_Component.
2. WHEN the My_Tasks_Page loads, THE Delegated_Tasks_Panel SHALL fetch up to 50 most recently created tasks where the current Staff_Member is the creator AND the assignee is a different Staff_Member (both constraints must be satisfied), ordered by creation date descending.
3. THE Delegated_Tasks_Panel SHALL display each delegated task showing its description (truncated to 100 characters with an ellipsis if longer), the assignee's name, and the current status (TODO, IN_PROGRESS, or DONE).
4. WHEN a Staff_Member creates, edits, or deletes a task via the Task_Form_Component, THE Delegated_Tasks_Panel SHALL refresh its data from the backend to reflect the current state of delegated tasks.
5. WHEN the Delegated_Tasks_Panel has no delegated tasks to display, THE Delegated_Tasks_Panel SHALL display a message indicating the Staff_Member has no tasks delegated to other staff members.
6. IF the Delegated_Tasks_Panel fails to fetch data from the backend, THEN THE Delegated_Tasks_Panel SHALL display both an error message and a retry action together; the error message SHALL NOT be displayed without a corresponding retry mechanism.

### Requirement 15: Task Detail Popup

**User Story:** As a staff member, I want to click on a task to view its full details in a popup, so that I can see all information without leaving the task list.

#### Acceptance Criteria

1. WHEN a Staff_Member clicks on a task in the Task_List_Component, THE Task_Detail_Popup SHALL open and display the task's description, status, assignee name, creator name, individual name, due date (or a "No due date" indicator if not set), and linked interaction summary (or a "No linked interaction" indicator if not present).
2. WHEN a linked interaction is displayed in the Task_Detail_Popup, THE Task_Detail_Popup SHALL render it as a clickable link showing the interaction date and type.
3. WHEN a Staff_Member clicks the linked interaction link in the Task_Detail_Popup, THE Angular Router SHALL navigate to the interaction detail view for that interaction.
4. IF the current Staff_Member is the creator of the displayed task, THEN THE Task_Detail_Popup SHALL display an enabled "Edit" action that opens the Task_Form_Component pre-populated with the task's current data.
5. IF the current Staff_Member is the creator of the displayed task, THEN THE Task_Detail_Popup SHALL display an enabled "Delete" action that, when clicked, presents a confirmation dialog with "Confirm" and "Cancel" options before calling the backend DELETE endpoint.
6. IF the current Staff_Member is not the creator of the displayed task, THEN THE Task_Detail_Popup SHALL hide the "Edit" and "Delete" actions.
7. WHEN deletion is confirmed and the backend returns HTTP 204, THE Task_Detail_Popup SHALL close and the Task_List_Component SHALL refresh to remove the deleted task.
8. THE Task_Detail_Popup SHALL provide a close control (e.g., close button or backdrop click) that dismisses the popup without performing any action and returns focus to the Task_List_Component.

### Requirement 16: Edit Task from UI

**User Story:** As a staff member, I want to edit a task through the UI, so that I can update task details when requirements change.

#### Acceptance Criteria

1. WHEN a Staff_Member opens the edit form from the Task_Detail_Popup, THE Task_Form_Component SHALL pre-populate the description, assignee, individual, due date, and linked interaction fields with the existing task data.
2. WHEN the Staff_Member submits the edited form with valid data, THE Task_Form_Component SHALL call the backend PUT `/api/tasks/{id}` endpoint with the updated fields.
3. WHEN the backend returns a successful response, THE Task_Form_Component SHALL close and the Task_List_Component SHALL refresh to display the updated task.
4. WHEN the backend returns a 403 error (not the creator), THE Task_Form_Component SHALL display an error message indicating the user does not have permission to edit the task; the system SHALL show this error only after backend rejection, allowing users to attempt edits regardless of predicted permission status.
5. WHEN the backend returns validation errors with HTTP 400, THE Task_Form_Component SHALL display all error messages inline next to the corresponding form fields.
6. IF the backend returns a 404 error during edit submission, THEN THE Task_Form_Component SHALL close, display an error message indicating the task no longer exists, and the Task_List_Component SHALL refresh to remove the stale task.

### Requirement 17: Delete Task from UI

**User Story:** As a staff member, I want to delete a task through the UI, so that I can remove tasks that are no longer needed.

#### Acceptance Criteria

1. WHEN a Staff_Member triggers the delete action from the Task_Detail_Popup, THE Task_Detail_Popup SHALL display a confirmation dialog with a confirm button and a cancel button asking the Staff_Member to confirm deletion.
2. WHEN the Staff_Member clicks the cancel button in the confirmation dialog, THE Task_Detail_Popup SHALL dismiss the confirmation dialog without calling the backend and leave the Task_Detail_Popup open with the task details unchanged.
3. WHEN the Staff_Member confirms deletion, THE Task_Detail_Popup SHALL disable the confirm button, call the backend DELETE `/api/tasks/{id}` endpoint, and remain in a non-interactive state until a response is received.
4. WHEN the backend returns HTTP 204, THE Task_List_Component SHALL remove the deleted task from the displayed list, THE Delegated_Tasks_Panel SHALL refresh its data, and THE Task_Detail_Popup SHALL close.
5. WHEN the backend returns a 403 error (not the creator), THE Task_Detail_Popup SHALL display an error message indicating the user does not have permission to delete the task and re-enable the dialog actions.
6. WHEN the backend returns a 404 error (task not found), THE Task_Detail_Popup SHALL close, display an error message indicating the task no longer exists, and THE Task_List_Component SHALL refresh to remove the stale entry.
7. IF the backend returns an unexpected error (network failure or server error), THEN THE Task_Detail_Popup SHALL display an error message indicating the deletion failed and re-enable the dialog actions so the Staff_Member can retry or cancel.
