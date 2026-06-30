# Requirements Document

## Introduction

This feature enables users to create follow-up tasks directly from an interaction context. The interaction detail view already has a "Create Follow-up Task" button, and the task module already supports an optional `interactionId` field. This feature completes the integration by: (1) making the task creation form accept and use pre-populated context (individual, interaction link) when launched from an interaction, (2) adding a follow-up task action on the interaction list view, and (3) providing clear visual feedback that the task is linked to a specific interaction.

## Glossary

- **Interaction_View**: The Angular component displaying interaction details or the interaction list table
- **Task_Form**: The modal overlay component (`TaskFormComponent`) used to create or edit tasks
- **Follow_Up_Action**: A UI button or link that initiates task creation pre-populated with interaction context
- **Pre_Population**: The automatic filling of form fields (individual, interaction link) based on the source interaction
- **Interaction_Context**: The set of data passed from an interaction to the task form: `interactionId`, `employeeId` (individual)

## Requirements

### Requirement 1: Follow-up task action on interaction detail view

**User Story:** As a staff member viewing an interaction, I want to create a follow-up task directly from the detail view, so that I can quickly capture action items without manually linking the interaction.

#### Acceptance Criteria

1. WHEN the user clicks the "Create Follow-up Task" button on the interaction detail view, THE Interaction_View SHALL navigate to the task creation form passing the interaction's `id`, `employeeId`, and `staffId` as query parameters named `interactionId`, `employeeId`, and `staffId` respectively.
2. WHILE the interaction data is loading, THE Interaction_View SHALL NOT display the "Create Follow-up Task" button.
3. WHEN the interaction data has loaded successfully (HTTP 200 response received and interaction record is rendered), THE Interaction_View SHALL display the "Create Follow-up Task" button within the detail view's action area.
4. IF the interaction data fails to load (HTTP 4xx, HTTP 5xx, or network error), THEN THE Interaction_View SHALL NOT display the "Create Follow-up Task" button.

### Requirement 2: Follow-up task action on interaction list view

**User Story:** As a staff member browsing interactions, I want to create a follow-up task from the list view without opening the detail page, so that I can work faster when reviewing multiple interactions.

#### Acceptance Criteria

1. THE Interaction_View SHALL display a "Create Task" action for each interaction row in the Actions column of the list table
2. WHEN the user clicks the "Create Task" action on a list row, THE Interaction_View SHALL open the Task_Form as a modal overlay passing that interaction's `id` and `employeeId` as Interaction_Context via query parameters
3. THE Interaction_View SHALL render the "Create Task" action as a link styled consistently with the existing "View" action in the same Actions column

### Requirement 3: Task form pre-population from interaction context

**User Story:** As a staff member creating a follow-up task from an interaction, I want the form to be pre-filled with the relevant individual and interaction link, so that I do not have to manually select them.

#### Acceptance Criteria

1. WHEN the Task_Form opens with Interaction_Context provided, THE Task_Form SHALL pre-select the individual field with the employee from the interaction and then fetch the available interactions for that employee before applying criterion 3
2. WHEN the Task_Form opens with Interaction_Context provided, THE Task_Form SHALL enable the "Link Interaction" toggle automatically
3. WHEN the Task_Form opens with Interaction_Context provided and available interactions have loaded, THE Task_Form SHALL pre-select the interaction matching the provided interactionId in the interaction dropdown
4. WHEN the Task_Form opens with Interaction_Context provided, THE Task_Form SHALL allow the user to modify the individual field, the "Link Interaction" toggle, and the interaction selection before submission
5. WHEN the user changes the pre-populated individual field to a different employee, THE Task_Form SHALL clear the interaction dropdown selection, fetch interactions for the newly selected employee, and leave the interaction dropdown without a pre-selected value
6. WHEN the Task_Form opens without Interaction_Context, THE Task_Form SHALL display the individual field as empty, the "Link Interaction" toggle as disabled, and the interaction dropdown as hidden, matching the standard task creation flow

### Requirement 4: Pre-populated field editability constraints

**User Story:** As a staff member, I want to understand which fields were pre-filled from the interaction so I can decide whether to change them.

#### Acceptance Criteria

1. WHEN the Task_Form opens with Interaction_Context, THE Task_Form SHALL display a banner indicating the task is being created from a linked interaction, including the interaction type and date from the source interaction
2. THE Task_Form SHALL keep all pre-populated fields fully editable (individual, interaction link toggle, interaction selection) so that the user can change any value without restriction
3. WHEN the user disables the "Link Interaction" toggle after Pre_Population, THE Task_Form SHALL immediately clear the interaction selection field and reset it to its default empty state
4. WHEN the user changes the individual field after Pre_Population to a different individual, THE Task_Form SHALL clear the interaction selection, reload available interactions for the newly selected individual, and keep the "Link Interaction" toggle in its current state
5. WHEN the Task_Form is submitted after the user has disabled the "Link Interaction" toggle, THE Task_Form SHALL omit the `interactionId` from the submission payload

### Requirement 5: Interaction context passed via query parameters

**User Story:** As a developer, I want the interaction context to be passed via URL query parameters, so that the mechanism is stateless and supports page refresh.

#### Acceptance Criteria

1. WHEN navigating to task creation from an interaction, THE Interaction_View SHALL include `interactionId` and `employeeId` as URL query parameters with UUID-formatted values corresponding to the source interaction's ID and associated employee ID
2. WHEN the Task_Form initializes and both `interactionId` and `employeeId` query parameters are present with valid UUID format, THE Task_Form SHALL read both values and use them for Pre_Population as defined in Requirement 3
3. IF only `employeeId` is present as a valid UUID query parameter without `interactionId`, THEN THE Task_Form SHALL pre-select the individual field using the `employeeId` value and leave the "Link Interaction" toggle disabled with no interaction pre-selected
4. IF only `interactionId` is present as a valid UUID query parameter without `employeeId`, THEN THE Task_Form SHALL ignore the `interactionId` and behave as if no Interaction_Context was provided
5. IF either query parameter value is not a valid UUID format, THEN THE Task_Form SHALL ignore that malformed parameter (and any dependent parameter) and behave as if the ignored parameter was not provided, displaying no error to the user
6. IF the `employeeId` query parameter references a non-existent employee, THEN THE Task_Form SHALL display the individual field as empty with no error and allow manual selection
7. IF the `interactionId` query parameter references a non-existent interaction, THEN THE Task_Form SHALL enable the "Link Interaction" toggle but leave the interaction dropdown without a pre-selected value

### Requirement 6: Task form opens as modal overlay from interaction views

**User Story:** As a staff member, I want the task form to appear as an overlay when triggered from interaction views, so that I maintain context of the interaction I am acting on.

#### Acceptance Criteria

1. WHEN the Follow_Up_Action is triggered from the interaction detail view, THE Task_Form SHALL open as a modal overlay on top of the current view
2. WHEN the Follow_Up_Action is triggered from the interaction list view, THE Task_Form SHALL open as a modal overlay on top of the current view
3. WHEN the user closes the Task_Form modal (via close button, backdrop click, or pressing the Escape key), THE Interaction_View SHALL remain on the same URL with scroll position, any applied filters, and expanded or selected rows preserved as they were before the modal opened
4. WHEN task creation succeeds from the modal, THE Task_Form SHALL close and THE Interaction_View SHALL display a success confirmation message
5. IF the backend returns a validation error or server error during task creation from the modal, THEN THE Task_Form SHALL remain open, display the error indication inline, and preserve all entered form data so the user can correct and retry

### Requirement 7: Success feedback after follow-up task creation

**User Story:** As a staff member, I want confirmation that my follow-up task was created, so that I know the action succeeded.

#### Acceptance Criteria

1. WHEN the follow-up task creation API returns a successful response, THE Interaction_View SHALL display a success message indicating the task was created
2. WHEN 5 seconds have elapsed since the success message was displayed, THE Interaction_View SHALL automatically dismiss the success message
3. WHILE the success message is displayed, THE Interaction_View SHALL provide a visible dismiss control that, when activated by the user, immediately hides the success message and cancels the auto-dismiss timer
4. IF the follow-up task creation API returns an error response, THEN THE Interaction_View SHALL display an error message indicating that the task could not be created and SHALL NOT display the success message
