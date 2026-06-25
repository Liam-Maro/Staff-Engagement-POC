# Requirements Document

## Introduction

This feature enables staff members to create tasks and assign them to other staff members within the Staff Engagement POC system. Currently, tasks are linked to employees (the subjects of engagement) and optionally to interactions. This feature extends the task module so that a staff member (the creator) can assign a task to another staff member (the assignee), establishing clear ownership and accountability for follow-up actions.

## Glossary

- **Task_Service**: The backend service responsible for task creation, retrieval, and status management within the `task` module.
- **Task_Controller**: The REST controller exposing task endpoints at `/api/tasks`.
- **Staff_Member**: An authenticated user of the system represented by the `Staff` entity, identified by a UUID.
- **Assignee**: The staff member to whom a task is assigned for completion.
- **Creator**: The staff member who creates and assigns the task.
- **Task**: A follow-up action with a title, description, status, optional due date, and assignment information.
- **Task_Status**: The lifecycle state of a task. Valid values are OPEN, IN_PROGRESS, and COMPLETED.

## Requirements

### Requirement 1: Create and Assign a Task

**User Story:** As a staff member, I want to create a task and assign it to another staff member, so that follow-up actions have a clear owner responsible for completion.

#### Acceptance Criteria

1. WHEN a Staff_Member submits a task creation request that passes all validation rules, THE Task_Service SHALL create a new Task with status OPEN, persist it to the database, and return the created Task with HTTP 201.
2. WHEN a task creation request is submitted, THE Task_Service SHALL record the Creator (the authenticated Staff_Member) and the Assignee (the target Staff_Member) on the Task.
3. WHEN a task creation request is submitted without a title or with a title that exceeds 255 characters, THE Task_Controller SHALL reject the request with HTTP 400 and a validation error message indicating the title constraint violated.
4. WHEN a task creation request specifies a non-existent assignee UUID, THE Task_Service SHALL reject the request with HTTP 404 and a descriptive error indicating the assignee was not found.
5. IF the specified assignee is an inactive Staff_Member, THEN THE Task_Service SHALL reject the request with HTTP 400 and an error indicating that tasks cannot be assigned to inactive staff members.
6. WHEN a task creation request is submitted with a description exceeding 2000 characters, THE Task_Controller SHALL reject the request with HTTP 400 and a validation error message indicating the description length limit.
7. WHEN a task creation request fails validation on multiple fields simultaneously, THE Task_Controller SHALL validate all fields and return all validation errors in a single HTTP 400 response.

### Requirement 2: Retrieve Tasks by Assignee

**User Story:** As a staff member, I want to view all tasks assigned to me, so that I can track my outstanding work.

#### Acceptance Criteria

1. WHEN a Staff_Member requests tasks filtered by assignee ID and tasks DO exist for that assignee, THE Task_Service SHALL return the actual matching Tasks of any Task_Status (OPEN, IN_PROGRESS, COMPLETED) assigned to that Staff_Member, ordered by creation date descending by default.
2. WHEN a Staff_Member requests tasks filtered by assignee ID and no tasks exist for that assignee, THE Task_Service SHALL return an empty list with HTTP 200.
3. THE Task_Controller SHALL expose a query parameter `assigneeId` on the GET `/api/tasks` endpoint that accepts a valid UUID string to filter tasks by assignee.
4. IF the `assigneeId` query parameter is not a valid UUID format, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error message indicating the expected UUID format.
5. WHEN a Staff_Member requests tasks filtered by an `assigneeId` that is a valid UUID but does not correspond to an existing Staff_Member, THE Task_Service SHALL return an empty list with HTTP 200.

### Requirement 3: Retrieve Tasks Created by a Staff Member

**User Story:** As a staff member, I want to see all tasks I have created, so that I can follow up on their progress.

#### Acceptance Criteria

1. WHEN a Staff_Member requests tasks filtered by creator ID, THE Task_Service SHALL return all Tasks created by that Staff_Member.
2. THE Task_Controller SHALL expose a query parameter `creatorId` on the GET `/api/tasks` endpoint to filter tasks by creator, where `creatorId` is a valid UUID.
3. WHEN a Staff_Member requests tasks filtered by creator ID and no tasks exist for that creator, THE Task_Service SHALL return an empty list with HTTP 200.
4. IF the `creatorId` query parameter is not a valid UUID format OR does not correspond to an existing Staff_Member, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error message indicating the creatorId is invalid (format validation takes priority over existence checks).
5. IF the `creatorId` query parameter is a valid UUID format but does not match any existing Staff_Member, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error message indicating the creator was not found.

### Requirement 4: Task Assignment Validation

**User Story:** As a staff member, I want the system to validate task assignments, so that tasks are only assigned to valid, active staff members.

#### Acceptance Criteria

1. WHEN a task creation request is submitted, THE Task_Controller SHALL validate that the title is not blank, does not exceed 255 characters, and the assignee ID is not null, rejecting the request with HTTP 400 and a validation error message if any condition is violated.
2. IF a task creation request contains a due date before today's date, THEN THE Task_Service SHALL reject the request with HTTP 400 and an error indicating the due date must be today or in the future.
3. IF the Creator does not exist as an active Staff_Member, THEN THE Task_Service SHALL reject the task creation request with HTTP 403 and an error indicating the creator is not an active staff member.
4. IF the Assignee does not exist as a Staff_Member, THEN THE Task_Service SHALL reject the task creation request with HTTP 404 and an error indicating the assignee was not found.
5. IF the Assignee exists but is not active, THEN THE Task_Service SHALL reject the task creation request with HTTP 400 and an error indicating that tasks cannot be assigned to inactive staff members.

### Requirement 5: Task Response Includes Assignment Details

**User Story:** As a staff member, I want task responses to include who created and who is assigned a task, so that I can understand task ownership at a glance.

#### Acceptance Criteria

1. THE Task_Service SHALL include the following fields in every task response payload: id, employeeId, interactionId, creatorId, assigneeId, title, description, status, dueDate, and createdAt.
2. WHEN a task is retrieved by ID, THE Task_Service SHALL return a single task response containing all fields defined in criterion 1.
3. THE Task_Controller SHALL use the same TaskResponse DTO record for all endpoints that return task data, including single retrieval, list retrieval, task creation, and status update responses.
4. IF a task has no assignee or no creator stored (e.g., tasks created before assignment feature), THEN THE Task_Service SHALL return null for the creatorId and assigneeId fields in the response.

### Requirement 6: Update Task Status

**User Story:** As a staff member, I want to update the status of a task assigned to me, so that I can reflect progress on my assigned work.

#### Acceptance Criteria

1. WHEN a Staff_Member submits a status update for a Task assigned to them, THE Task_Service SHALL update the Task status to the provided value, persist the change, and return the updated Task representation with HTTP 200.
2. IF a status update request specifies a status value not in the set (OPEN, IN_PROGRESS, COMPLETED), THEN THE Task_Service SHALL first verify the task exists, THEN reject the request with HTTP 400 and an error message listing the valid status values without returning any task data.
3. IF a status update request targets a non-existent task ID, THEN THE Task_Service SHALL reject the request with HTTP 404 and an error message indicating that no task was found for the given ID (task existence is validated before status value validation).
4. IF a Staff_Member submits a status update for a Task that is not assigned to them, THEN THE Task_Service SHALL reject the request with HTTP 403 and an error message indicating the staff member is not the assignee of the task.

### Requirement 7: Sort Task Results by Creation Date

**User Story:** As a staff member, I want to sort task lists by creation date in ascending or descending order, so that I can view tasks in the order most useful to me.

#### Acceptance Criteria

1. THE Task_Controller SHALL expose a query parameter `sortOrder` on the GET `/api/tasks` endpoint accepting values `asc` and `desc` (case-insensitive).
2. WHEN a Staff_Member provides `sortOrder=desc`, THE Task_Service SHALL return tasks ordered by creation date from newest to oldest.
3. WHEN a Staff_Member provides `sortOrder=asc`, THE Task_Service SHALL return tasks ordered by creation date from oldest to newest.
4. WHEN no `sortOrder` parameter is provided, THE Task_Service SHALL default to descending order (newest first).
5. IF an invalid `sortOrder` value is provided, THEN THE Task_Controller SHALL reject the request with HTTP 400 and an error listing valid values (asc, desc) without returning any task data in the response.
6. WHEN multiple tasks have identical creation timestamps, THE Task_Service SHALL use task ID as a secondary sort key to guarantee deterministic ordering.
