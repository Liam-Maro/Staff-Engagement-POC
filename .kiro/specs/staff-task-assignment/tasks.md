# Implementation Plan: Staff Task Assignment

## Overview

This plan implements the staff-to-staff task assignment feature across backend (Java 21 / Spring Boot), database (Liquibase migration), and frontend (Angular 21). The implementation evolves the existing `task` module by adding creator/assignee fields, replacing the title-centric model with a description-centric model, updating status enums, and building a full "My Tasks" UI with filtering, delegated tasks panel, task detail popup, and create/edit form.

## Tasks

- [x] 1. Database migration and entity updates
  - [x] 1.1 Create Liquibase changeset to evolve `tsk_tasks` table
    - Rename `employee_id` → `individual_id`
    - Add `creator_id` UUID column and `assignee_id` UUID column
    - Drop `title` column
    - Backfill null descriptions and add NOT NULL constraint to `description`
    - Update status values: `OPEN` → `TODO`, `COMPLETED` → `DONE`
    - Add FK constraints (`fk_tsk_creator`, `fk_tsk_assignee`) referencing `stf_staff(id)`
    - Create indexes on `assignee_id`, `creator_id`, `individual_id`, `status`, `due_date`, `created_at`
    - _Requirements: 1.1, 1.2, 5.1_

  - [x] 1.2 Update Task entity and TaskStatus enum
    - Modify `Task.java` entity: add `creatorId`, `assigneeId` fields; rename `employeeId` to `individualId`; remove `title` field; update column mappings
    - Update `TaskStatus` enum values from `OPEN/IN_PROGRESS/COMPLETED` to `TODO/IN_PROGRESS/DONE`
    - _Requirements: 5.1, 6.1, 6.6_

- [x] 2. Backend DTOs and exception infrastructure
  - [x] 2.1 Create new DTO records for task operations
    - Create `CreateTaskRequest` record with Jakarta validation annotations (`@NotNull`, `@NotBlank`, `@Size(max=2000)`)
    - Create `UpdateTaskRequest` record with same validation annotations
    - Create `UpdateStatusRequest` record with `@NotBlank` status field
    - Modify `TaskResponse` record to include: `id`, `individualId`, `interactionId`, `creatorId`, `assigneeId`, `description`, `status`, `dueDate`, `createdAt`
    - Create `TaskQueryParams` record for filter/sort/pagination parameters
    - Create `TaskQueryResult` record with `tasks`, `totalCount`, `currentPage`, `pageSize`
    - Create `InteractionResponse` record for the interactions endpoint (id, employeeId, staffId, type, notes, occurredAt, createdAt)
    - _Requirements: 1.3, 1.8, 4.1, 5.1, 5.2, 5.3, 5.4, 5.5, 7.4, 7.5_

  - [x] 2.2 Add or verify shared exception classes
    - Ensure `EntityNotFoundException`, `InactiveStaffException`, `TaskAssignmentForbiddenException`, `InvalidParameterException` exist in the shared module
    - Verify `GlobalExceptionHandler` maps each exception to correct HTTP status (404, 400, 403, 400 respectively)
    - Ensure `MethodArgumentNotValidException` handler returns all validation errors in a single response
    - _Requirements: 1.4, 1.5, 1.8, 4.1, 4.3, 4.4, 4.5, 9.2, 9.3_

  - [x] 2.3 Write unit tests for DTO validation annotations
    - Test `CreateTaskRequest` rejects blank/null description, null assigneeId, null individualId
    - Test `@Size(max=2000)` triggers for oversized descriptions
    - Test multiple simultaneous validation failures produce multiple error messages
    - _Requirements: 1.3, 1.8, 4.1, 8.7_

- [x] 3. Backend service layer — task creation and validation
  - [x] 3.1 Update `TaskService` interface with new method signatures
    - Add `create(CreateTaskRequest, UUID creatorId)`, `update(UUID, UpdateTaskRequest, UUID requesterId)`, `updateStatus(UUID, UpdateStatusRequest, UUID requesterId)`, `delete(UUID, UUID requesterId)`, `getInteractionsForIndividual(UUID)`, `findTasks(TaskQueryParams)`, `findById(UUID)`
    - _Requirements: 1.1, 2.1, 3.1, 6.1, 8.1, 9.1, 10.1_

  - [x] 3.2 Implement task creation logic in `TaskServiceImpl`
    - Validate creator exists and is active (reject with 403 via `TaskAssignmentForbiddenException`)
    - Validate assignee existence first (404 via `EntityNotFoundException`), then active status (400 via `InactiveStaffException`)
    - Validate individual exists via `EmployeeService.existsById()` (404 if not found)
    - Validate interaction (when provided) exists and belongs to individual (400 via `InvalidParameterException`)
    - Validate dueDate is not in the past (400 via `InvalidParameterException`)
    - Persist task with status `TODO`, set `creatorId` from auth context
    - Return `TaskResponse` with all fields
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 1.6, 1.7, 1.9, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1_

  - [x] 3.3 Write property tests for task creation (Properties 1–5)
    - **Property 1: Task creation round-trip preserves all input data**
    - **Property 2: Description validation rejects blank and oversized inputs**
    - **Property 3: Multi-field validation returns all errors simultaneously**
    - **Property 4: Non-existent assignee rejected with 404; inactive with 400**
    - **Property 5: Inactive creator rejected with 403**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 1.9, 4.1, 4.3, 4.4, 4.5, 5.1, 5.2, 5.5**

  - [x] 3.4 Write unit tests for task creation edge cases
    - Test null dueDate creates task successfully
    - Test interaction validation skipped when interactionId is null
    - Test exact boundary: 2000-char description succeeds, 2001-char fails
    - Test validation order: creator check before assignee check
    - _Requirements: 1.7, 1.9, 4.2, 4.3_

- [x] 4. Backend service layer — query, filter, and sort
  - [x] 4.1 Implement `TaskRepository` with `JpaSpecificationExecutor` and dynamic specifications
    - Build `Specification<Task>` implementations for each filter: assigneeId, creatorId, excludeSelfAssigned, status, dueDateFrom/To, createdFrom/To
    - Implement sort logic with secondary sort by task ID for deterministic ordering
    - Handle null due dates last when sorting by dueDate (regardless of sort direction)
    - Implement pagination with page size default of 50
    - _Requirements: 2.1, 2.6, 3.1, 3.5, 7.1, 7.2, 7.3, 7.4, 7.5, 7.7, 7.8, 7.9_

  - [x] 4.2 Implement `findTasks(TaskQueryParams)` in `TaskServiceImpl`
    - Combine all filter specifications with AND logic
    - Apply sort order with null-dueDate-last handling
    - Return `TaskQueryResult` with pagination metadata
    - Handle `excludeSelfAssigned=true` to return only tasks where assignee ≠ creator
    - Return empty list with HTTP 200 for valid UUID not matching any staff member
    - _Requirements: 2.1, 2.2, 2.5, 3.1, 3.3, 3.5, 3.6, 7.7, 7.9_

  - [x] 4.3 Write property tests for query and filtering (Properties 8–11, 21)
    - **Property 8: Filter by assignee returns exactly matching tasks**
    - **Property 9: Filter by creator with excludeSelfAssigned enforces both constraints**
    - **Property 10: Sort ordering invariant**
    - **Property 11: Filter combination applies all constraints as AND**
    - **Property 21: Pagination enforced at 50 tasks per page**
    - **Validates: Requirements 2.1, 2.2, 2.6, 3.1, 3.5, 7.1, 7.2, 7.3, 7.4, 7.5, 7.7, 7.8, 7.9, 14.2**

  - [x] 4.4 Write unit tests for sort and filter edge cases
    - Test partial date ranges (only dueDateFrom, only dueDateTo)
    - Test empty result set returns empty list not null
    - Test default sort is createdDate desc
    - Test case-insensitive status matching
    - _Requirements: 7.4, 7.5, 7.8_

- [x] 5. Backend service layer — status update, edit, and delete
  - [x] 5.1 Implement `updateStatus()` in `TaskServiceImpl`
    - Validate task exists (404 if not)
    - Validate requester is the assignee (403 if not)
    - Validate status value is valid enum (400 if not, list valid values in error message)
    - Allow any valid status → any valid status transition (including same-to-same)
    - Return updated `TaskResponse`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [x] 5.2 Implement `update()` (edit task) in `TaskServiceImpl`
    - Validate task exists (404)
    - Validate requester is creator (403)
    - Explicitly validate new assignee exists and is active (even when appears valid)
    - Validate individual exists (400 if not found)
    - Validate interaction only when interactionId is provided; skip entirely when omitted
    - Validate dueDate not in past (400)
    - Update fields: description, assigneeId, individualId, dueDate, interactionId
    - Return updated `TaskResponse`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

  - [x] 5.3 Implement `delete()` in `TaskServiceImpl`
    - Validate UUID format at controller level (400)
    - Validate task exists (404 regardless of requester identity)
    - Enforce strict authorization: check requester is creator BEFORE any deletion logic (403, no data modification)
    - Delete task and confirm removal before returning
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 5.4 Write property tests for status update, edit, and delete (Properties 13–18)
    - **Property 13: Past due dates are rejected**
    - **Property 14: Status update validation order and authorization**
    - **Property 15: Any valid status can transition to any other valid status**
    - **Property 16: Edit authorization and round-trip**
    - **Property 17: Interaction validation conditional on field presence**
    - **Property 18: Delete authorization is strict — no data modification on unauthorized attempts**
    - **Validates: Requirements 4.2, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 8.1, 8.2, 8.3, 8.5, 9.1, 9.2, 9.3**

  - [x] 5.5 Write unit tests for status update, edit, and delete edge cases
    - Test status update with same current status succeeds
    - Test edit with interaction validation skipped when interactionId omitted
    - Test delete: task remains unchanged after 403 rejection
    - Test validation order for status update matches spec (existence → authorization → value validity)
    - _Requirements: 6.1, 6.5, 8.5, 9.3_

- [x] 6. Backend controller layer
  - [x] 6.1 Implement `TaskController` with all endpoints
    - `GET /api/tasks` — parse/validate all query params (UUID format always returns 400), delegate to service
    - `GET /api/tasks/{id}` — validate UUID path variable, return single task
    - `POST /api/tasks` — `@Valid` request body, extract creatorId from auth context, delegate to service, return 201
    - `PUT /api/tasks/{id}` — `@Valid` request body, extract requesterId from auth context, delegate to service
    - `PATCH /api/tasks/{id}/status` — `@Valid` request body, extract requesterId from auth context, delegate to service
    - `DELETE /api/tasks/{id}` — validate UUID format (400), extract requesterId from auth context, delegate to service, return 204
    - `GET /api/tasks/interactions?individualId={uuid}` — validate UUID format, delegate to service
    - _Requirements: 2.3, 2.4, 3.2, 3.4, 5.2, 5.3, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.10, 9.4, 10.1, 10.4_

  - [x] 6.2 Implement `getInteractionsForIndividual()` in `TaskServiceImpl`
    - Validate individualId format (400 if invalid UUID)
    - Validate individual exists (404 if not — NOT empty list)
    - Fetch interactions via `InteractionService.findByEmployeeId()`
    - Return up to 50 interactions ordered by `occurredAt` desc
    - Map to `InteractionResponse` with all required fields
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 6.3 Write property tests for UUID validation and interactions endpoint (Properties 6, 7, 12, 19, 20)
    - **Property 6: Invalid UUID format always returns HTTP 400**
    - **Property 7: Valid UUID but non-existent staff returns empty list with 200**
    - **Property 12: Invalid filter parameter values are rejected with 400**
    - **Property 19: Interactions endpoint returns 404 for non-existent individuals**
    - **Property 20: Non-null interactionId values validated before inclusion in response**
    - **Validates: Requirements 2.4, 2.5, 3.4, 3.6, 7.6, 7.10, 9.4, 10.1, 10.2, 10.3, 10.4, 5.4**

  - [x] 6.4 Write integration tests for full API lifecycle
    - Test full lifecycle: create → retrieve → filter → update status → edit → delete
    - Test database constraint verification (FK constraints, NOT NULL)
    - Test pagination with >50 tasks
    - Test sort with null dueDate positioning
    - Test `/api/tasks/interactions` returns 404 for non-existent individual
    - Test delete: task persists unchanged after 403
    - _Requirements: 1.1, 2.1, 2.6, 6.1, 7.9, 8.1, 9.1, 9.3, 10.3_

- [x] 7. Checkpoint — Backend complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Frontend task service and models
  - [x] 8.1 Create TypeScript interfaces and models
    - Create `TaskResponse`, `CreateTaskRequest`, `UpdateTaskRequest`, `TaskQueryParams`, `TaskQueryResult`, `InteractionSummary` interfaces in `frontend/src/app/tasks/models/`
    - Define `TaskStatus` type union: `'TODO' | 'IN_PROGRESS' | 'DONE'`
    - _Requirements: 5.1, 5.3_

  - [x] 8.2 Implement Angular `TaskService`
    - `createTask(request)` → POST `/api/tasks`
    - `updateTask(id, request)` → PUT `/api/tasks/{id}`
    - `deleteTask(id)` → DELETE `/api/tasks/{id}`
    - `getTasks(params)` → GET `/api/tasks` with query params
    - `getTaskById(id)` → GET `/api/tasks/{id}`
    - `updateTaskStatus(taskId, status)` → PATCH `/api/tasks/{id}/status`
    - `getInteractionsForIndividual(individualId)` → GET `/api/tasks/interactions?individualId=`
    - _Requirements: 2.1, 3.1, 6.1, 8.1, 9.1, 10.1, 12.3, 13.9_

  - [x] 8.3 Write unit tests for Angular `TaskService`
    - Test HTTP method and URL for each service method
    - Test query parameter construction for `getTasks()`
    - Test error handling propagation
    - _Requirements: 2.3, 3.2, 7.1_

- [x] 9. Frontend routing and navigation
  - [x] 9.1 Add "My Tasks" route and update `LayoutComponent`
    - Add `/tasks` route as lazy-loaded child of authenticated layout
    - Add "My Tasks" navigation link in `LayoutComponent` sidebar
    - Apply active CSS class when route matches `/tasks`
    - Ensure route is protected by auth guard (redirect unauthenticated users to login)
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [x] 9.2 Write unit tests for routing and navigation
    - Test navigation link renders for authenticated users
    - Test active class applied on `/tasks` route
    - Test lazy-loading of `MyTasksPage` component
    - Test auth guard redirects unauthenticated users
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [x] 10. Frontend — MyTasksPage and TaskListComponent
  - [x] 10.1 Create `MyTasksPage` component
    - Layout with `TaskListComponent` on left, `DelegatedTasksPanel` on right
    - Provide button/action to open `TaskFormComponent` for task creation
    - _Requirements: 12.1, 13.1, 14.1_

  - [x] 10.2 Create `TaskListComponent`
    - Fetch tasks assigned to current user on load (createdDate desc)
    - Display filter controls: status dropdown (TODO/IN_PROGRESS/DONE), due date range pickers, created date range pickers
    - All filters unset by default (show all tasks on initial load)
    - On filter change: call backend with filter params, refresh list
    - Display each task: description (truncated to 100 chars + ellipsis), status, due date, individual name
    - Show loading indicator ONLY during task list fetching
    - On empty results: show "no tasks match selected filters" message
    - On error: show error message AND retry action together (never error without retry)
    - Click task → open `TaskDetailPopup`
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_

  - [x] 10.3 Write unit tests for `TaskListComponent`
    - Test loading indicator shown only during task fetch
    - Test filter controls call backend with correct params
    - Test empty state message displayed
    - Test error + retry displayed together
    - Test task description truncation at 100 chars
    - _Requirements: 12.1, 12.4, 12.5, 12.6, 12.7_

- [x] 11. Frontend — TaskFormComponent
  - [x] 11.1 Create `TaskFormComponent` for create and edit
    - "Assigned To" dropdown defaulting to current user, listing active staff members
    - "Description" text field: required, max 2000 chars, show remaining character count
    - "Individual" search/selector for employees or staff
    - "Due Date" date picker preventing past date selection
    - "Link Interaction" toggle: when active + individual selected, fetch and show interactions dropdown
    - When toggle active but no individual selected: disable dropdown, show hint
    - Pre-populate fields when editing an existing task
    - Block submission immediately when required fields (description, individual, assignee) are empty — prevent request reaching backend
    - Description must contain text (whitespace-only is not valid)
    - On success: show confirmation, close form, trigger task list and delegated panel refresh
    - On backend 400: display inline errors next to fields
    - On server error (5xx): show error message, preserve all entered form data for retry
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7, 13.8, 13.9, 13.10, 13.11, 16.1, 16.2, 16.3, 16.4, 16.5, 16.6_

  - [x] 11.2 Write unit tests for `TaskFormComponent`
    - **Property 22: Form blocks submission when required fields are empty**
    - Test whitespace-only description blocked
    - Test character count display
    - Test interaction dropdown disabled without individual
    - Test pre-population on edit mode
    - Test form data preserved on server error
    - **Validates: Requirements 13.3, 13.5, 13.6, 13.8, 13.9, 13.11, 16.1**

- [x] 12. Frontend — TaskDetailPopup and DelegatedTasksPanel
  - [x] 12.1 Create `TaskDetailPopup` component
    - Display: description, status, assignee name, creator name, individual name, due date (or "No due date"), linked interaction (or "No linked interaction")
    - Linked interaction as clickable link (date + type) navigating to interaction detail
    - If current user is creator: show Edit and Delete actions
    - If current user is NOT creator: hide Edit and Delete actions
    - Edit action opens `TaskFormComponent` pre-populated
    - Delete action: confirmation dialog → disable confirm button → call DELETE → refresh on 204
    - Handle 403 on delete: show permission error, re-enable dialog
    - Handle 404 on delete: close popup, show error, refresh list
    - Handle server error: show error, re-enable dialog for retry
    - Close control (button or backdrop click) dismisses popup, returns focus to task list
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7, 15.8, 17.1, 17.2, 17.3, 17.4, 17.5, 17.6, 17.7_

  - [x] 12.2 Create `DelegatedTasksPanel` component
    - Fetch tasks where creator = current user AND assignee ≠ current user (use `creatorId` + `excludeSelfAssigned=true`)
    - Show up to 50 most recent, ordered by createdDate desc
    - Display: description (truncated 100 chars), assignee name, status
    - Refresh on task creation, edit, or deletion
    - On empty: show "no delegated tasks" message
    - On error: show error AND retry action together
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6_

  - [x] 12.3 Write unit tests for `TaskDetailPopup` and `DelegatedTasksPanel`
    - Test Edit/Delete hidden for non-creators
    - Test delete confirmation flow and button disabling
    - Test delegated panel uses correct query params (creatorId + excludeSelfAssigned)
    - Test error + retry displayed together on panel error
    - Test empty state messages
    - _Requirements: 15.4, 15.5, 15.6, 15.7, 14.2, 14.5, 14.6_

- [x] 13. Final checkpoint — All tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Backend uses jqwik for property-based testing; frontend uses Jasmine + Angular TestBed
- The design specifies Java 21 and Angular 21 — no language selection needed

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1", "2.2"] },
    { "id": 2, "tasks": ["2.3", "3.1", "8.1"] },
    { "id": 3, "tasks": ["3.2", "4.1", "8.2"] },
    { "id": 4, "tasks": ["3.3", "3.4", "4.2", "8.3"] },
    { "id": 5, "tasks": ["4.3", "4.4", "5.1", "5.2", "5.3", "9.1"] },
    { "id": 6, "tasks": ["5.4", "5.5", "6.1", "6.2", "9.2"] },
    { "id": 7, "tasks": ["6.3", "6.4", "10.1"] },
    { "id": 8, "tasks": ["10.2", "10.3"] },
    { "id": 9, "tasks": ["11.1", "12.1", "12.2"] },
    { "id": 10, "tasks": ["11.2", "12.3"] }
  ]
}
```
