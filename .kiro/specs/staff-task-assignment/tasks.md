# Implementation Plan: Staff Task Assignment

## Overview

This plan extends the existing `task` module to support staff-to-staff task assignment. The implementation adds `creatorId` and `assigneeId` fields to the Task entity, introduces new exception classes, updates the service layer with assignment validation logic, modifies the REST controller for filtering/sorting, and updates the frontend models and services. Tasks are ordered to build incrementally with early validation of core functionality.

## Tasks

- [x] 1. Database migration and entity updates
  - [x] 1.1 Create Liquibase changeset for assignment columns
    - Create `backend/src/main/resources/db/changelog/changes/008-add-assignment-fields-to-task.xml`
    - Add `creator_id` (UUID, nullable) and `assignee_id` (UUID, nullable) columns to `tsk_tasks`
    - Add foreign key constraints referencing `stf_staff(id)`
    - Add indexes `idx_tsk_tasks_assignee_id` and `idx_tsk_tasks_creator_id`
    - Register the changeset in `db.changelog-master.xml`
    - _Requirements: 1.2, 5.1_

  - [x] 1.2 Create TaskStatus enum and update Task entity
    - Create `TaskStatus` enum in `com.staffengagement.task.model` with values OPEN, IN_PROGRESS, COMPLETED
    - Add `creatorId` (UUID, nullable) and `assigneeId` (UUID, nullable) fields to `Task` entity
    - Change `status` field from `String` to `@Enumerated(EnumType.STRING) TaskStatus status`
    - Update constructor to accept creatorId and assigneeId parameters
    - Add getters and setter for new fields
    - _Requirements: 1.2, 5.1, 6.1_

  - [x] 1.3 Update TaskRepository with assignment query methods
    - Add `List<Task> findByAssigneeId(UUID assigneeId, Sort sort)` method
    - Add `List<Task> findByCreatorId(UUID creatorId, Sort sort)` method
    - _Requirements: 2.1, 2.3, 3.1, 3.2_

- [x] 2. Exception classes and error handling
  - [x] 2.1 Create new exception classes
    - Create `InactiveStaffException` in `com.staffengagement.shared.exception` — extends RuntimeException
    - Create `TaskAssignmentForbiddenException` in `com.staffengagement.shared.exception` — extends RuntimeException
    - Create `InvalidParameterException` in `com.staffengagement.shared.exception` — extends RuntimeException
    - _Requirements: 1.5, 4.3, 4.5, 6.4, 2.4, 3.4, 7.5_

  - [x] 2.2 Register new exceptions in GlobalExceptionHandler
    - Add handler for `InactiveStaffException` → HTTP 400
    - Add handler for `TaskAssignmentForbiddenException` → HTTP 403
    - Add handler for `InvalidParameterException` → HTTP 400
    - All handlers use existing `ErrorResponse` record format
    - _Requirements: 1.5, 4.3, 4.5, 6.4, 2.4, 3.4, 7.5_

- [x] 3. DTO updates
  - [x] 3.1 Update CreateTaskRequest DTO
    - Add `@NotNull UUID assigneeId` field to the record
    - Add `@Size(max = 255) String title` constraint (replace @NotBlank with both @NotBlank and @Size)
    - Add `@Size(max = 2000) String description` constraint
    - _Requirements: 1.3, 1.6, 4.1_

  - [x] 3.2 Update TaskResponse DTO
    - Add `UUID creatorId` and `UUID assigneeId` fields to the record
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 3.3 Create UpdateStatusRequest DTO
    - Create `UpdateStatusRequest` record in `com.staffengagement.task.dto` with `@NotBlank String status`
    - _Requirements: 6.1, 6.2_

- [x] 4. Service layer implementation
  - [x] 4.1 Update TaskService interface
    - Add `List<TaskResponse> findByAssigneeId(UUID assigneeId, String sortOrder)` method
    - Add `List<TaskResponse> findByCreatorId(UUID creatorId, String sortOrder)` method
    - Modify `create` signature to `TaskResponse create(CreateTaskRequest request, UUID creatorId)`
    - Modify `updateStatus` signature to `TaskResponse updateStatus(UUID taskId, String status, UUID requesterId)`
    - _Requirements: 2.1, 3.1, 1.1, 1.2, 6.1, 6.4_

  - [x] 4.2 Implement task creation with assignment validation in TaskServiceImpl
    - Inject `StaffService` dependency via constructor
    - Validate creator exists and is active (throw `TaskAssignmentForbiddenException` if not)
    - Validate assignee exists (throw `EntityNotFoundException` if not found)
    - Validate assignee is active (throw `InactiveStaffException` if inactive)
    - Validate due date is not in the past (throw `InvalidParameterException` if past)
    - Create task with creatorId and assigneeId, set status to OPEN
    - Return `TaskResponse` with all fields including creatorId and assigneeId
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 4.2, 4.3, 4.4, 4.5_

  - [x] 4.3 Implement assignee/creator query methods in TaskServiceImpl
    - Implement `findByAssigneeId` with sort order support (default desc)
    - Implement `findByCreatorId` with sort order support (default desc)
    - Apply secondary sort by task ID for deterministic ordering
    - Return empty list when no results found
    - _Requirements: 2.1, 2.2, 2.5, 3.1, 3.3, 7.2, 7.3, 7.4, 7.6_

  - [x] 4.4 Implement status update with assignee authorization in TaskServiceImpl
    - Validate task exists (throw `EntityNotFoundException` if not)
    - Validate status value is a valid `TaskStatus` enum value (throw `InvalidParameterException` if not)
    - Validate requester is the task's assignee (throw `TaskAssignmentForbiddenException` if not)
    - Update and persist the task status
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 4.5 Write unit tests for TaskServiceImpl
    - Test creation with valid inputs returns expected TaskResponse
    - Test creation with inactive creator throws TaskAssignmentForbiddenException
    - Test creation with non-existent assignee throws EntityNotFoundException
    - Test creation with inactive assignee throws InactiveStaffException
    - Test creation with past due date throws InvalidParameterException
    - Test findByAssigneeId returns matching tasks sorted correctly
    - Test findByCreatorId returns matching tasks sorted correctly
    - Test findByAssigneeId returns empty list for no matches
    - Test updateStatus with valid assignee succeeds
    - Test updateStatus with non-assignee throws TaskAssignmentForbiddenException
    - Test updateStatus with invalid status throws InvalidParameterException
    - Test backward compatibility: tasks without creator/assignee return null fields
    - _Requirements: 1.1–1.5, 2.1, 2.2, 3.1, 3.3, 4.2–4.5, 5.4, 6.1–6.4_

- [x] 5. Checkpoint - Core backend logic
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Controller layer updates
  - [x] 6.1 Update TaskController with filter and sort query parameters
    - Add `@RequestParam(required = false) String assigneeId` query parameter to GET endpoint
    - Add `@RequestParam(required = false) String creatorId` query parameter to GET endpoint
    - Add `@RequestParam(required = false) String sortOrder` query parameter to GET endpoint
    - Validate UUID format for assigneeId/creatorId (throw `InvalidParameterException` if invalid)
    - Validate sortOrder is "asc" or "desc" case-insensitively (throw `InvalidParameterException` if invalid)
    - Route to appropriate service method based on which filter params are present
    - _Requirements: 2.3, 2.4, 3.2, 3.4, 7.1, 7.5_

  - [x] 6.2 Update TaskController create and status update endpoints
    - Modify POST handler to extract authenticated staff member's UUID as creatorId and pass to service
    - Replace `@RequestBody Map<String, String>` in PATCH with `@Valid @RequestBody UpdateStatusRequest`
    - Extract requester ID from authenticated principal for status update authorization
    - _Requirements: 1.1, 1.2, 6.1, 6.4_

  - [x] 6.3 Add creator validation in filter endpoint
    - For `creatorId` query parameter: after UUID format validation, validate creator exists as a staff member
    - Throw `InvalidParameterException` with HTTP 400 if creator UUID not found
    - _Requirements: 3.4, 3.5_

  - [x] 6.4 Write MockMvc API tests for TaskController
    - Test POST /api/tasks with valid payload returns 201 with TaskResponse
    - Test POST /api/tasks with missing title returns 400 with validation errors
    - Test POST /api/tasks with null assigneeId returns 400
    - Test POST /api/tasks with multiple invalid fields returns all errors in single 400
    - Test GET /api/tasks?assigneeId={valid-uuid} returns filtered results
    - Test GET /api/tasks?assigneeId=not-a-uuid returns 400
    - Test GET /api/tasks?creatorId={valid-uuid} returns filtered results
    - Test GET /api/tasks?creatorId=not-a-uuid returns 400
    - Test GET /api/tasks?sortOrder=invalid returns 400 with valid values listed
    - Test PATCH /api/tasks/{id}/status with valid assignee returns 200
    - Test PATCH /api/tasks/{id}/status with non-assignee returns 403
    - Test PATCH /api/tasks/{id}/status with invalid status returns 400
    - Test PATCH /api/tasks/{non-existent-id}/status returns 404
    - _Requirements: 1.3, 1.4, 1.5, 1.6, 1.7, 2.3, 2.4, 3.2, 3.4, 6.1–6.4, 7.1, 7.5_

- [x] 7. Checkpoint - Backend API complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Property-based testing setup and implementation
  - [x] 8.1 Add jqwik dependency to pom.xml
    - Add `net.jqwik:jqwik:1.9.1` test dependency to `backend/pom.xml`
    - Verify jqwik integrates with existing JUnit 5 test infrastructure
    - _Requirements: Testing strategy prerequisite_

  - [x] 8.2 Write property test for task creation round-trip
    - **Property 1: Task creation round-trip preserves all input data**
    - Generate random valid CreateTaskRequests with active staff members
    - Assert response contains matching employeeId, assigneeId, title, description, dueDate, status=OPEN, non-null id and createdAt
    - **Validates: Requirements 1.1, 1.2, 5.1, 5.2**

  - [x] 8.3 Write property tests for input validation rejection
    - **Property 2: Invalid field values are always rejected**
    - Generate random strings violating title/description constraints; assert 400 and unchanged task list
    - **Property 3: Multi-field validation returns all errors simultaneously**
    - Generate requests with N>=2 invalid fields; assert response contains at least N errors
    - **Property 13: Past due dates are rejected**
    - Generate random dates before today; assert 400 rejection
    - **Validates: Requirements 1.3, 1.6, 1.7, 4.1, 4.2**

  - [x] 8.4 Write property tests for assignment validation
    - **Property 4: Non-existent assignee is rejected with 404**
    - Generate random UUIDs not in staff table; assert 404
    - **Property 5: Inactive staff assignment is rejected**
    - Use inactive staff members; assert 400 for assignee and 403 for creator
    - **Validates: Requirements 1.4, 1.5, 4.3, 4.4, 4.5**

  - [x] 8.5 Write property tests for query filtering
    - **Property 6: Filter by assignee returns exactly the matching tasks**
    - Generate task sets with known assignee distribution; verify exact match
    - **Property 7: Filter by creator returns exactly the matching tasks**
    - Generate task sets with known creator distribution; verify exact match
    - **Property 8: Invalid UUID query parameters are rejected**
    - Generate random non-UUID strings; assert 400
    - **Validates: Requirements 2.1, 2.4, 3.1, 3.4**

  - [x] 8.6 Write property tests for sort ordering and status updates
    - **Property 9: Sort ordering invariant**
    - Generate task sets; verify consecutive pair ordering for desc/asc with secondary ID sort
    - **Property 10: Invalid sort order is rejected**
    - Generate random strings ≠ asc/desc; assert 400
    - **Property 11: Invalid status value is rejected**
    - Generate random strings ∉ {OPEN, IN_PROGRESS, COMPLETED}; assert 400
    - **Property 12: Non-assignee cannot update task status**
    - Generate mismatched assignee/requester pairs; assert 403
    - **Validates: Requirements 6.2, 6.4, 7.2, 7.3, 7.5, 7.6**

- [x] 9. Frontend implementation
  - [x] 9.1 Create frontend task module structure and models
    - Create `frontend/src/app/tasks/models/task.model.ts` with `TaskResponse` and `CreateTaskRequest` interfaces
    - Include `creatorId` and `assigneeId` fields in both interfaces
    - Define `TaskStatus` type union: `'OPEN' | 'IN_PROGRESS' | 'COMPLETED'`
    - _Requirements: 5.1, 5.2_

  - [x] 9.2 Implement TaskService in frontend
    - Create `frontend/src/app/tasks/services/task.service.ts`
    - Implement `createTask(request: CreateTaskRequest): Observable<TaskResponse>`
    - Implement `getTasksByAssignee(assigneeId: string, sortOrder?: string): Observable<TaskResponse[]>`
    - Implement `getTasksByCreator(creatorId: string, sortOrder?: string): Observable<TaskResponse[]>`
    - Implement `updateTaskStatus(taskId: string, status: string): Observable<TaskResponse>`
    - Use HttpClient with base URL from environment config
    - _Requirements: 2.1, 3.1, 6.1, 7.1_

  - [x] 9.3 Write unit tests for frontend TaskService
    - Test createTask sends correct POST payload and returns TaskResponse
    - Test getTasksByAssignee sends assigneeId and sortOrder query params
    - Test getTasksByCreator sends creatorId and sortOrder query params
    - Test updateTaskStatus sends PATCH with status body
    - Use HttpClientTestingModule to mock HTTP calls
    - _Requirements: 2.1, 3.1, 6.1, 7.1_

- [x] 10. Integration testing
  - [x] 10.1 Write integration tests with Testcontainers
    - Test full task creation lifecycle: create → retrieve → filter by assignee → filter by creator → update status
    - Test backward compatibility: pre-existing tasks without creator/assignee return null fields
    - Test database constraint enforcement (FK constraints on creator_id and assignee_id)
    - Test sort behavior with real PostgreSQL ordering semantics
    - Test empty result sets for non-matching filters
    - Use `@SpringBootTest` with Testcontainers PostgreSQL
    - _Requirements: 1.1, 2.1, 2.2, 2.5, 3.1, 3.3, 5.4, 6.1, 7.2, 7.3, 7.6_

- [x] 11. Final checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The backend uses Java 21 with Spring Boot 3.4.x; jqwik is used for property-based testing
- The frontend uses Angular 21 standalone components with RxJS/HttpClient
- Existing `StaffService.findById()` is used for cross-module assignee/creator validation

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "3.1", "3.2", "3.3"] },
    { "id": 1, "tasks": ["1.2", "2.2"] },
    { "id": 2, "tasks": ["1.3", "4.1"] },
    { "id": 3, "tasks": ["4.2", "4.3", "4.4"] },
    { "id": 4, "tasks": ["4.5", "6.1", "6.2"] },
    { "id": 5, "tasks": ["6.3", "6.4"] },
    { "id": 6, "tasks": ["8.1", "9.1"] },
    { "id": 7, "tasks": ["8.2", "8.3", "8.4", "9.2"] },
    { "id": 8, "tasks": ["8.5", "8.6", "9.3"] },
    { "id": 9, "tasks": ["10.1"] }
  ]
}
```
