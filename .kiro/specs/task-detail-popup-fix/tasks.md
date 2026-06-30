# Implementation Plan

## Overview

Fix the "My Tasks" page so clicking a task item renders `TaskDetailPopupComponent` with correct bindings. Root cause: `TaskListComponent` lacks `@Output` emitter and `MyTasksPageComponent` never includes the popup in its template.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Task Click Does Not Render Detail Popup
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Scope property to concrete failing cases — click/Enter on any task item in a loaded task list asserts `<app-task-detail-popup>` renders in DOM
  - Bug Condition from design: `isBugCondition(input)` where `(input.type == 'click' OR input.type == 'keydown.enter') AND input.target IS TaskListItem AND input.targetTask EXISTS in tasks() AND TaskDetailPopupComponent NOT rendered in DOM`
  - Test: In `MyTasksPageComponent` test harness, render component with mocked task list, simulate click on task item, assert `fixture.nativeElement.querySelector('app-task-detail-popup')` exists
  - Test: Simulate Enter keydown on focused task item, assert popup renders
  - Test: Assert popup receives correct `[task]`, `[staffMembers]`, `[currentStaffId]` input bindings
  - Generate random `TaskResponse` objects (varying description, status, assignee, dueDate presence, interactionId presence) and verify popup renders for each
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS — no `<app-task-detail-popup>` element found in DOM after click (confirms bug exists: no @Output emitter, no popup in parent template)
  - Document counterexamples: "Click on task item → DOM contains zero `app-task-detail-popup` elements, `TaskListComponent` has no `taskSelected` @Output, `MyTasksPageComponent` template has no popup reference"
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 2.1, 2.2_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Filter/Sort, Create Task, Delegated Panel, and Loading States Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - **Step 1 — Observe on UNFIXED code:**
  - Observe: Applying status filter "TODO" calls `GET /api/tasks?status=TODO` with correct params on unfixed code
  - Observe: Applying due date range filter calls backend with `dueDateFrom` and `dueDateTo` params on unfixed code
  - Observe: Applying sort control calls backend with correct `sortBy` and `sortDirection` params on unfixed code
  - Observe: Clicking "Create Task" button opens `TaskFormComponent` with default field state (assignee = current user, description empty, no individual, no due date) on unfixed code
  - Observe: `DelegatedTasksPanelComponent` fetches and displays delegated tasks independently on unfixed code
  - Observe: During loading state, no `<app-task-detail-popup>` DOM element present on unfixed code
  - Observe: During error state with retry action, no `<app-task-detail-popup>` DOM element present on unfixed code
  - **Step 2 — Write property-based tests capturing observed behavior:**
  - Write property: for all valid filter combinations (status ∈ {TODO, IN_PROGRESS, DONE, null} × dueDateRange × createdDateRange × sort), API call params match filter state
  - Write property: for all states of DelegatedTasksPanel, its content/loading state is independent of any popup-related signal changes
  - Write property: for all loading/error states, no popup DOM element exists
  - Write test: Create Task button opens form with correct defaults regardless of popup state
  - Verify all tests pass on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. Fix for task detail popup not rendering on task click

  - [x] 3.1 Add @Output emitter to TaskListComponent
    - Add `@Output() taskSelected = new EventEmitter<TaskResponse>()` to `TaskListComponent`
    - Update `onTaskClick(task)` method to call `this.taskSelected.emit(task)` after setting internal signal
    - Ensure Enter key handler also calls `this.taskSelected.emit(task)`
    - File: `frontend/src/app/tasks/components/task-list/task-list.component.ts`
    - _Bug_Condition: isBugCondition(input) where (input.type == 'click' OR input.type == 'keydown.enter') AND input.target IS TaskListItem_
    - _Requirements: 1.1, 1.2, 2.1, 2.2_

  - [x] 3.2 Add state management to MyTasksPageComponent
    - Add `selectedTask = signal<TaskResponse | null>(null)` signal
    - Add `staffMembers = signal<StaffMember[]>([])` signal, loaded on init via `StaffService`
    - Add `currentStaffId` property, resolved from `AuthService` + `StaffService` on init
    - Add `onTaskSelected(task: TaskResponse)` method that sets `selectedTask` signal
    - Add `onPopupClosed()` method that sets `selectedTask` to null
    - Add `onTaskDeleted(taskId: number)` method that sets `selectedTask` to null and triggers list refresh
    - File: `frontend/src/app/tasks/components/my-tasks-page/my-tasks-page.component.ts`
    - _Bug_Condition: isBugCondition(input) — parent has no selectedTask signal, no staffMembers, no currentStaffId_
    - _Expected_Behavior: Parent manages selectedTask signal, provides staffMembers and currentStaffId to popup_
    - _Preservation: Existing filter/sort, Create Task, DelegatedTasksPanel behavior must remain unchanged_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3_

  - [x] 3.3 Wire TaskDetailPopupComponent into MyTasksPageComponent template
    - Import `TaskDetailPopupComponent` in standalone `imports` array
    - Bind `(taskSelected)="onTaskSelected($event)"` on `<app-task-list>` element
    - Add `@if (selectedTask())` block rendering `<app-task-detail-popup>` with bindings:
      - `[task]="selectedTask()!"`
      - `[staffMembers]="staffMembers()"`
      - `[currentStaffId]="currentStaffId"`
    - Bind `(closed)="onPopupClosed()"` on popup
    - Bind `(taskDeleted)="onTaskDeleted($event)"` on popup
    - File: `frontend/src/app/tasks/components/my-tasks-page/my-tasks-page.component.html`
    - _Expected_Behavior: TaskDetailPopupComponent renders with correct task data, staffMembers, and currentStaffId when selectedTask is non-null_
    - _Preservation: No changes to existing template elements (task-list filters, create-task button, delegated-tasks-panel)_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 3.4 Add Escape key handling to close popup
    - Add `@HostListener('document:keydown.escape')` or `(keydown.escape)` binding on popup container
    - Handler calls `onPopupClosed()` to set `selectedTask` to null
    - Ensure Escape only fires when popup is open (guard with `if (this.selectedTask())`)
    - File: `frontend/src/app/tasks/components/my-tasks-page/my-tasks-page.component.ts`
    - _Expected_Behavior: Escape key closes popup, sets selectedTask to null, returns focus to task list_
    - _Requirements: 2.3_

  - [x] 3.5 Handle task deletion flow
    - On `(taskDeleted)` event from popup: set `selectedTask` to null, trigger task list refresh (e.g., `refresh$.next()` or re-fetch tasks)
    - Deleted task removed from displayed list without full page reload
    - Error display handled internally by `TaskDetailPopupComponent` via its `deleteError` signal — no parent change needed for error case
    - _Expected_Behavior: Popup closes on successful delete, task removed from list; popup stays open with error on failed delete_
    - _Preservation: DelegatedTasksPanel unaffected by delete events_
    - _Requirements: 2.4, 2.5, 3.3_

  - [x] 3.6 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Task Click Opens Detail Popup
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior (popup renders with correct bindings on click/Enter)
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed — popup renders on click/Enter with correct data)
    - _Requirements: 2.1, 2.2_

  - [x] 3.7 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Click Interactions Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions — filters, Create Task, DelegatedTasksPanel, loading/error states all unchanged)
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 4. Checkpoint - Ensure all tests pass
  - Run full Angular test suite (`ng test --watch=false`)
  - Ensure bug condition exploration test passes (popup renders on click/Enter)
  - Ensure preservation tests pass (filters, Create Task, DelegatedTasksPanel, loading/error states unchanged)
  - Ensure no other existing tests broken by changes
  - Ask the user if questions arise


## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1", "2"] },
    { "id": 1, "tasks": ["3.1"] },
    { "id": 2, "tasks": ["3.2"] },
    { "id": 3, "tasks": ["3.3", "3.4"] },
    { "id": 4, "tasks": ["3.5"] },
    { "id": 5, "tasks": ["3.6"] },
    { "id": 6, "tasks": ["3.7"] },
    { "id": 7, "tasks": ["4"] }
  ]
}
```

## Notes

- Tasks 1 and 2 are standalone and must run BEFORE any implementation (tasks 3.x)
- Task 1 is expected to FAIL on unfixed code — this confirms the bug exists
- Task 2 is expected to PASS on unfixed code — this captures baseline behavior
- After implementation (3.1–3.5), task 3.6 re-runs the same test from task 1 (should now PASS)
- Task 3.7 re-runs the same tests from task 2 (should still PASS, confirming no regressions)
- `TaskDetailPopupComponent` is already fully implemented — this fix only wires it into the page
- Error display on failed delete is handled internally by `TaskDetailPopupComponent` — no parent logic needed
