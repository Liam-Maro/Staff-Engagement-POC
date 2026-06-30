# Task Detail Popup Fix — Bugfix Design

## Overview

The "My Tasks" page has a fully-implemented `TaskDetailPopupComponent` that is never rendered because: (1) `TaskListComponent` sets an internal `selectedTask` signal on click but does not emit the selection to its parent, and (2) `MyTasksPageComponent` never includes `<app-task-detail-popup>` in its template. The fix wires the existing popup component into the page by adding an `@Output` emitter to `TaskListComponent` and conditionally rendering `TaskDetailPopupComponent` in `MyTasksPageComponent` with the correct bindings.

## Glossary

- **Bug_Condition (C)**: A user clicks (or presses Enter on) a task item in the task list — the action that should open the detail popup but currently does nothing
- **Property (P)**: The desired behavior when a task is clicked — `TaskDetailPopupComponent` renders with correct task data, staff members, and current user's staffId
- **Preservation**: Existing filter/sort behavior, "Create Task" flow, DelegatedTasksPanel independence, and loading/error states must remain unchanged
- **TaskListComponent**: Component in `tasks/components/task-list/` that fetches and renders the user's tasks with filters
- **TaskDetailPopupComponent**: Component in `tasks/components/task-detail-popup/` with full popup UI including task details, edit/delete actions, and close handling
- **MyTasksPageComponent**: Parent page component in `tasks/components/my-tasks-page/` that orchestrates task list, delegated panel, and task form
- **selectedTask**: Signal in TaskListComponent (currently internal-only) holding the clicked task or null

## Bug Details

### Bug Condition

The bug manifests when a user clicks or presses Enter on a task item in the task list. The `TaskListComponent.onTaskClick()` method sets an internal signal but does not emit the event to the parent. The parent `MyTasksPageComponent` has no reference to `TaskDetailPopupComponent` in its template and no binding to receive the selected task.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type UserInteraction
  OUTPUT: boolean
  
  RETURN (input.type == 'click' OR input.type == 'keydown.enter')
         AND input.target IS TaskListItem
         AND input.targetTask EXISTS in tasks()
         AND TaskDetailPopupComponent NOT rendered in DOM
END FUNCTION
```

### Examples

- User clicks first task in list → `selectedTask` signal updates to that task internally, but no `<app-task-detail-popup>` element appears in DOM. Expected: popup renders with that task's details.
- User focuses third task item and presses Enter → same non-response. Expected: popup renders identically to click behavior.
- User clicks task when 5 staff members exist → popup should show assignee/creator names resolved from staff list. Actual: nothing renders.
- User clicks task where `interactionId` is null → popup should show "No linked interaction". Actual: nothing renders.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Applying status filter (TODO, IN_PROGRESS, DONE), due date range, created date range, or sort controls must continue calling `GET /api/tasks` with correct parameters and displaying results
- Clicking "Create Task" button must continue opening `TaskFormComponent` with default field state
- `DelegatedTasksPanelComponent` must continue fetching and displaying delegated tasks independently, unaffected by popup open/close
- Loading indicator and error state with retry action must continue rendering correctly; no popup DOM should appear until task list is loaded and a task is explicitly clicked

**Scope:**
All inputs that do NOT involve clicking/pressing Enter on a task list item should be completely unaffected by this fix. This includes:
- Filter and sort interactions
- Create Task button click
- DelegatedTasksPanel interactions
- Loading and error state displays
- Mouse hover/focus styling on task items (visual-only, no popup)

## Hypothesized Root Cause

Based on code analysis, the root causes are confirmed (not hypothesized):

1. **Missing Output Emitter on TaskListComponent**: `onTaskClick()` sets `this.selectedTask.set(task)` but there is no `@Output() taskSelected` EventEmitter to notify the parent. The signal is internal-only.

2. **Missing TaskDetailPopupComponent in MyTasksPageComponent Template**: The parent component imports `TaskListComponent`, `DelegatedTasksPanelComponent`, and `TaskFormComponent` but does NOT import or render `TaskDetailPopupComponent`. There is no `@if` block for the popup.

3. **Missing State Management in Parent**: `MyTasksPageComponent` has no `selectedTask` signal, no `staffMembers` data, and no `currentStaffId` to pass as inputs to the popup.

4. **Missing Escape Key Handling**: No `@HostListener` or `(keydown.escape)` binding exists to close the popup on Escape key press.

## Correctness Properties

Property 1: Bug Condition - Task Click Opens Detail Popup

_For any_ user interaction where a task item is clicked or Enter is pressed on a focused task item in a successfully-loaded task list, the fixed system SHALL render `TaskDetailPopupComponent` bound with that task's data (`task`), the current `staffMembers` list, and the current user's `staffId`, displaying all required detail fields and conditional action buttons.

**Validates: Requirements 2.1, 2.2**

Property 2: Preservation - Non-Click Interactions Unchanged

_For any_ interaction that is NOT a click/Enter on a task list item (filter changes, sort changes, Create Task button, DelegatedTasksPanel usage, loading/error states), the fixed code SHALL produce exactly the same behavior as the original code, preserving all existing filter/sort, task creation, delegated panel, and state display functionality.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

**File**: `frontend/src/app/tasks/components/task-list/task-list.component.ts`

**Specific Changes**:
1. **Add Output Emitter**: Add `@Output() taskSelected = new EventEmitter<TaskResponse>()` to emit the clicked task to the parent
2. **Emit on Click**: Update `onTaskClick()` to call `this.taskSelected.emit(task)` in addition to setting the signal

---

**File**: `frontend/src/app/tasks/components/my-tasks-page/my-tasks-page.component.ts`

**Specific Changes**:
1. **Import TaskDetailPopupComponent**: Add to standalone `imports` array
2. **Add selectedTask Signal**: `selectedTask = signal<TaskResponse | null>(null)` to track which task is selected
3. **Add staffMembers Signal**: `staffMembers = signal<StaffMember[]>([])` loaded on init
4. **Add currentStaffId**: Resolved from `AuthService` + `StaffService` on init
5. **Handle taskSelected Event**: Bind `(taskSelected)="onTaskSelected($event)"` on `<app-task-list>`
6. **Render Popup Conditionally**: Add `@if (selectedTask())` block rendering `<app-task-detail-popup>` with bindings: `[task]="selectedTask()!"`, `[staffMembers]="staffMembers()"`, `[currentStaffId]="currentStaffId"`
7. **Handle Popup Close**: `(closed)` event sets `selectedTask` to null
8. **Handle Task Deleted**: `(taskDeleted)` event sets `selectedTask` to null, removes task from list via `refresh$.next()`
9. **Handle Escape Key**: Add `(keydown.escape)` or `@HostListener` to close popup when Escape pressed
10. **Handle Delete Error Display**: The popup component already handles error display internally via `deleteError` signal — no parent change needed

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm root cause analysis.

**Test Plan**: Write component tests that simulate click/Enter on task items and assert DOM contains `<app-task-detail-popup>`. Run on UNFIXED code to observe failures.

**Test Cases**:
1. **Click Task Test**: Simulate click on task item in `MyTasksPageComponent` — assert popup renders (will fail on unfixed code)
2. **Enter Key Test**: Simulate Enter keydown on focused task item — assert popup renders (will fail on unfixed code)
3. **Popup Data Binding Test**: After click, assert popup receives correct `task`, `staffMembers`, `currentStaffId` inputs (will fail on unfixed code)
4. **Close Popup Test**: After opening, click overlay and assert popup removed from DOM (will fail on unfixed code)

**Expected Counterexamples**:
- No `<app-task-detail-popup>` element found in DOM after click
- Root cause confirmed: no `@Output` emitter in TaskListComponent, no popup in parent template

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed system produces the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := simulateTaskClick(input.task)
  ASSERT document.querySelector('app-task-detail-popup') EXISTS
  ASSERT popup.task == input.task
  ASSERT popup.staffMembers == currentStaffMembers
  ASSERT popup.currentStaffId == loggedInUser.staffId
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed code produces the same result as the original.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT originalBehavior(input) == fixedBehavior(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many combinations of filter/sort parameters to verify API calls remain identical
- It catches edge cases in state transitions (loading → loaded → popup open/close)
- It provides strong guarantees that DelegatedTasksPanel independence is maintained

**Test Plan**: Observe behavior on UNFIXED code for filter/sort, create task, and delegated panel flows, then write tests verifying identical behavior after fix.

**Test Cases**:
1. **Filter Preservation**: Verify all filter combinations continue calling correct API params after fix
2. **Create Task Flow Preservation**: Verify Create Task button opens form with defaults unchanged
3. **Delegated Panel Independence**: Verify DelegatedTasksPanel content/loading state unaffected by popup open/close
4. **Loading/Error State Preservation**: Verify no popup DOM appears during loading or error states

### Unit Tests

- Test `TaskListComponent` emits `taskSelected` event with correct task on click
- Test `TaskListComponent` emits `taskSelected` event on Enter keydown
- Test `MyTasksPageComponent` renders popup when `selectedTask` is non-null
- Test `MyTasksPageComponent` does NOT render popup when `selectedTask` is null
- Test popup receives correct `[task]`, `[staffMembers]`, `[currentStaffId]` inputs
- Test `(closed)` event sets `selectedTask` to null and removes popup from DOM
- Test `(taskDeleted)` event sets `selectedTask` to null and triggers list refresh
- Test Escape key closes popup

### Property-Based Tests

- Generate random `TaskResponse` objects and verify popup renders correct field values for each
- Generate random `staffMembers` lists and verify name resolution displays correctly
- Generate random filter parameter combinations and verify API call parameters unchanged
- Generate task lists of varying lengths and verify click on any index opens correct popup

### Integration Tests

- Test full flow: load page → click task → verify popup → close popup → verify task list still visible
- Test full flow: click task → delete task → verify popup closes and task removed from list
- Test full flow: click task → delete fails → verify popup stays open with error message
- Test Create Task and popup don't interfere: open popup → close → create task → verify form works
