# Implementation Plan: Interaction Follow-Up Task

## Overview

This implementation integrates the interaction module with the task module by enhancing the `TaskFormComponent` to accept pre-populated interaction context, converting the existing navigation-based "Create Follow-up Task" flow into a modal overlay, adding a "Create Task" action to the interaction list view, and providing toast notification feedback. All changes are frontend-only — the backend already supports `interactionId` on task creation.

## Tasks

- [x] 1. Create shared components and interfaces
  - [x] 1.1 Add `InteractionContext` interface to task models
    - Add `InteractionContext` interface to `frontend/src/app/tasks/models/task.model.ts`
    - Interface includes `interactionId`, `employeeId`, optional `interactionType`, optional `interactionDate`
    - _Requirements: 5.1, 5.2_

  - [x] 1.2 Create `ToastNotificationComponent` in shared module
    - Create `frontend/src/app/shared/components/toast-notification/toast-notification.component.ts`
    - Implement `@Input() message`, `@Input() type` (success/error), `@Input() autoDismissMs` (default 5000)
    - Implement `@Output() dismissed` event emitter
    - Add auto-dismiss timer via `setTimeout`, cancel on manual dismiss or `ngOnDestroy`
    - Add visible dismiss button (×) that emits `dismissed` and cancels timer
    - Style: green for success, red for error, fixed position top-right
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 1.3 Write unit tests for `ToastNotificationComponent`
    - Test auto-dismiss fires after configured timeout
    - Test manual dismiss cancels auto-dismiss timer
    - Test dismiss button emits `dismissed` event
    - Test timer cancelled on component destroy
    - Test success/error styling applied correctly
    - _Requirements: 7.2, 7.3_

- [x] 2. Enhance `TaskFormComponent` with interaction context pre-population
  - [x] 2.1 Add `interactionContext` input and UUID validation to `TaskFormComponent`
    - Add `@Input() interactionContext: InteractionContext | null = null` to the component
    - Add private `UUID_REGEX` constant and `isValidUuid()` helper method
    - Modify `ngOnInit` to check `interactionContext` and apply validation rules:
      - Both valid UUIDs → full pre-population
      - Only `employeeId` valid → partial (individual only, toggle off)
      - Only `interactionId` valid (no `employeeId`) → ignore entirely
      - Malformed UUID → silently ignore that parameter
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

  - [x] 2.2 Implement full pre-population logic in `TaskFormComponent`
    - When both `interactionId` and `employeeId` are valid UUIDs:
      - Set `individualId` form control to `employeeId`
      - Set `linkInteraction` toggle to `true`
      - Call `loadInteractions(employeeId)`
      - After interactions load, set `interactionId` form control to matching value
    - Handle non-existent employee: leave individual field empty, allow manual selection
    - Handle non-existent interaction: enable toggle, leave dropdown unselected
    - _Requirements: 3.1, 3.2, 3.3, 5.6, 5.7_

  - [x] 2.3 Add context banner display to `TaskFormComponent`
    - When `interactionContext` is provided with valid full context, render a banner above the form
    - Banner shows: "Creating task from [type] interaction on [date]"
    - Use `interactionType` and `interactionDate` from context for banner text
    - Keep all pre-populated fields fully editable
    - _Requirements: 4.1, 4.2_

  - [x] 2.4 Implement individual change and toggle off behavior
    - When user changes `individualId` after pre-population: clear `interactionId`, reload interactions for new employee, keep toggle in current state
    - When user disables `linkInteraction` toggle after pre-population: clear `interactionId` field immediately, reset to empty state
    - Ensure submission with toggle off omits `interactionId` from payload (already handled by existing submit logic)
    - _Requirements: 3.5, 4.3, 4.4, 4.5_

  - [x] 2.5 Write property tests for `TaskFormComponent` pre-population (fast-check)
    - **Property 3: Full pre-population from valid interaction context**
    - **Property 7: Partial context (employeeId only) pre-populates individual only**
    - **Property 8: InteractionId without employeeId is ignored**
    - **Property 9: Malformed UUID parameters are silently ignored**
    - **Validates: Requirements 3.1, 3.2, 3.3, 5.2, 5.3, 5.4, 5.5**

  - [x] 2.6 Write unit tests for `TaskFormComponent` pre-population behavior
    - Test full pre-population sets individual, toggle, and interaction
    - Test partial pre-population (employeeId only) sets individual only
    - Test interactionId-only context is ignored
    - Test malformed UUID is silently ignored
    - Test banner renders with correct type and date
    - Test individual change clears interaction and reloads
    - Test toggle off clears interaction selection
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 4.1, 4.3, 4.4_

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Enhance interaction detail view with modal overlay
  - [x] 4.1 Convert `InteractionDetailComponent` to open task form as modal overlay
    - Add `showTaskFormModal` signal (boolean)
    - Add `taskFormContext` signal (`InteractionContext | null`)
    - Modify `createFollowUpTask()`: instead of `router.navigate`, set context signal with `interactionId`, `employeeId`, `interactionType`, `interactionDate` and show modal
    - Add `<app-task-form>` in template conditionally rendered when `showTaskFormModal()` is true, with `[interactionContext]="taskFormContext()"` binding
    - Listen to `(closed)` event to hide modal
    - Listen to `(taskCreated)` event to hide modal and show success toast
    - _Requirements: 1.1, 6.1_

  - [x] 4.2 Implement button visibility rules on interaction detail view
    - Hide "Create Follow-up Task" button while `isLoading()` is true
    - Show button only when interaction data has loaded successfully (`interaction()` is not null and `isLoading()` is false)
    - Do not show button if error occurred (`errorMessage()` is not null or `isNotFound()` is true)
    - _Requirements: 1.2, 1.3, 1.4_

  - [x] 4.3 Add success/error toast to interaction detail view
    - Add `successMessage` and `errorMessage` signals
    - On `taskCreated` event: set success message "Follow-up task created successfully"
    - On task creation error: set error message
    - Include `<app-toast-notification>` in template bound to message signals
    - Listen to `(dismissed)` event to clear message signal
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 4.4 Write unit tests for interaction detail view modal behavior
    - Test button hidden during loading state
    - Test button visible after successful load
    - Test button hidden on error
    - Test clicking button opens modal with correct context
    - Test modal close preserves view state
    - Test success toast shown after task creation
    - _Requirements: 1.2, 1.3, 1.4, 6.1, 6.3, 7.1_

- [x] 5. Enhance interaction list view with "Create Task" action and modal
  - [x] 5.1 Add "Create Task" action to interaction list table rows
    - Add "Create Task" link in the Actions column for each interaction row
    - Style consistently with existing "View" action link in the same column
    - _Requirements: 2.1, 2.3_

  - [x] 5.2 Implement modal overlay from interaction list view
    - Add `showTaskFormModal` signal (boolean)
    - Add `taskFormContext` signal (`InteractionContext | null`)
    - On "Create Task" click: build `InteractionContext` from that row's data and show modal
    - Add `<app-task-form>` with `[interactionContext]` binding
    - Listen to `(closed)` event: hide modal, ensure filters/pagination/scroll preserved
    - Listen to `(taskCreated)` event: hide modal, show success toast
    - _Requirements: 2.2, 6.2, 6.3_

  - [x] 5.3 Add success/error toast to interaction list view
    - Add `successMessage` signal
    - On `taskCreated`: set success message
    - On error: show error toast
    - Include `<app-toast-notification>` component in template
    - Ensure list state (filters, page, scroll) preserved after modal close
    - _Requirements: 6.3, 6.4, 7.1, 7.4_

  - [x] 5.4 Write unit tests for interaction list view "Create Task" flow
    - Test "Create Task" action rendered for each row
    - Test clicking action opens modal with correct context for that row
    - Test modal close preserves filters and pagination
    - Test success toast shown after creation
    - _Requirements: 2.1, 2.2, 6.2, 6.3, 7.1_

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Modal close behavior and Escape key handling
  - [x] 7.1 Implement modal close via backdrop click, close button, and Escape key
    - Ensure clicking overlay backdrop calls `onClose()` (already handled by existing template)
    - Add `@HostListener('document:keydown.escape')` or keyboard event handler to close modal on Escape
    - Verify close button (×) emits `closed` event
    - On close: parent view URL, scroll position, filters, and selected rows remain unchanged
    - _Requirements: 6.3_

  - [x] 7.2 Implement error handling for task creation in modal context
    - On 400 validation error: show inline field errors in modal, keep modal open, preserve form data
    - On 500 server error: show server error banner in modal, keep open for retry
    - On network error: show generic error message, keep modal open
    - Do NOT show success toast on error
    - _Requirements: 6.4, 6.5, 7.4_

  - [x] 7.3 Write integration tests for full modal flow
    - Test: detail view → open modal → submit → modal closes → success toast appears
    - Test: list view → open modal → submit → modal closes → success toast appears → filters preserved
    - Test: modal → submit with error → modal stays open → inline errors shown
    - Test: modal → Escape key → modal closes → view state preserved
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1_

- [x] 8. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- All changes are frontend-only — the backend already supports `interactionId` on task creation
- The existing `TaskFormComponent` already handles overlay rendering, form validation, and submission; enhancements add the `interactionContext` input and pre-population logic
- The existing `InteractionDetailComponent.createFollowUpTask()` currently uses `router.navigate` — this will be replaced with inline modal rendering

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3"] },
    { "id": 3, "tasks": ["2.4"] },
    { "id": 4, "tasks": ["2.5", "2.6"] },
    { "id": 5, "tasks": ["4.1", "5.1"] },
    { "id": 6, "tasks": ["4.2", "4.3", "5.2"] },
    { "id": 7, "tasks": ["4.4", "5.3"] },
    { "id": 8, "tasks": ["5.4", "7.1"] },
    { "id": 9, "tasks": ["7.2"] },
    { "id": 10, "tasks": ["7.3"] }
  ]
}
```
