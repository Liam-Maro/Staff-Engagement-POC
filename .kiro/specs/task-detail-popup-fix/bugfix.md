# Bugfix Requirements Document

## Introduction

Clicking a task item on the "My Tasks" page does nothing. The `TaskListComponent` sets a `selectedTask` signal internally on click, but neither renders `TaskDetailPopupComponent` itself nor emits the selection to the parent `MyTasksPageComponent`. The fully-implemented `TaskDetailPopupComponent` is never wired into any template, so users cannot view task details from the task list.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a user clicks a task item in the task list on the "My Tasks" page THEN no popup, dialog, or overlay appears on screen; the DOM contains no rendered `<app-task-detail-popup>` element and no visual change occurs beyond the default hover/focus style on the clicked item

1.2 WHEN a user presses Enter on a focused task item in the task list THEN the same non-response occurs — no popup or detail view renders

### Expected Behavior (Correct)

2.1 WHEN a user clicks a task item in the task list on the "My Tasks" page THEN the system SHALL display the TaskDetailPopupComponent bound with that task's data, the current staffMembers list, and the current user's staffId, showing: description, status, assignee name (resolved from staffMembers), creator name (resolved from staffMembers), individual ID, due date (or "No due date" if absent), linked interaction link (or "No linked interaction" if absent), and edit/delete action buttons visible only when the current user is the task creator

2.2 WHEN a user presses Enter on a focused task item in the task list THEN the system SHALL display the TaskDetailPopupComponent with identical content and behavior as described in criterion 2.1

2.3 WHEN the TaskDetailPopupComponent is open and the user clicks the overlay backdrop, clicks the close button, or presses the Escape key THEN the system SHALL close the popup, set the selectedTask signal to null, and return focus to the task list

2.4 WHEN a task is successfully deleted via the popup THEN the system SHALL close the popup, set the selectedTask signal to null, and remove the deleted task from the displayed task list without requiring a full page reload

2.5 IF a task deletion request fails THEN the system SHALL keep the popup open and display an error message within the popup indicating the failure reason (permission denied, task not found, or server error)

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a Staff Member applies any combination of status filter (TODO, IN_PROGRESS, DONE), due date range filter, created date range filter, or sort controls in the TaskListComponent THEN the system SHALL CONTINUE TO call the backend GET `/api/tasks` endpoint with the corresponding filter and sort parameters and display the returned tasks matching the applied criteria, with no change in filter behavior from the state prior to the bugfix

3.2 WHEN a Staff Member clicks the "Create Task" button on the My Tasks page THEN the system SHALL CONTINUE TO open the TaskFormComponent with all fields in their default state (assignee defaulting to the current Staff Member, description empty, no individual selected, no due date)

3.3 WHILE the TaskDetailPopup is open, closed, or transitioning between states THE DelegatedTasksPanel SHALL CONTINUE TO fetch and display delegated tasks from the backend independently and SHALL NOT alter its displayed content or loading state in response to popup open/close events

3.4 WHILE the TaskListComponent is in a loading state or an error state THE system SHALL CONTINUE TO display the corresponding loading indicator or error message with retry action, and SHALL NOT render any TaskDetailPopup DOM element or overlay until the task list has successfully loaded and a task is explicitly clicked
