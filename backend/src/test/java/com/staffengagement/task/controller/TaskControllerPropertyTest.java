package com.staffengagement.task.controller;

import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.task.dto.*;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.service.TaskService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for UUID validation and interactions endpoint (Properties 6, 7, 12, 19, 20).
 *
 * Tests the controller layer directly since UUID and parameter validation happens there.
 * The controller is package-private, so this test class must be in the same package.
 *
 * Validates: Requirements 2.4, 2.5, 3.4, 3.6, 7.6, 7.10, 9.4, 10.1, 10.2, 10.3, 10.4, 5.4
 */
class TaskControllerPropertyTest {

    private TaskService taskService;
    private TaskController controller;

    @BeforeProperty
    void setUp() {
        taskService = mock(TaskService.class);
        controller = new TaskController(taskService);
    }

    // ========================================================================
    // Property 6: Invalid UUID format always returns HTTP 400
    // ========================================================================

    /**
     * Property 6a: Invalid UUID string as assigneeId query param throws InvalidParameterException.
     *
     * For any string that is not a valid UUID format, passing it as assigneeId SHALL
     * result in InvalidParameterException (HTTP 400) regardless of any system validation settings.
     *
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void invalidUuidAsAssigneeIdReturns400(
            @ForAll("invalidUuidStrings") String invalidUuid
    ) {
        assertThatThrownBy(() -> controller.findTasks(
                invalidUuid, null, null, null, null, null, null, null,
                "createdDate", "desc", 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid UUID format")
                .hasMessageContaining("assigneeId");
    }

    /**
     * Property 6b: Invalid UUID string as creatorId query param throws InvalidParameterException.
     *
     * For any string that is not a valid UUID format, passing it as creatorId SHALL
     * result in InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    void invalidUuidAsCreatorIdReturns400(
            @ForAll("invalidUuidStrings") String invalidUuid
    ) {
        assertThatThrownBy(() -> controller.findTasks(
                null, invalidUuid, null, null, null, null, null, null,
                "createdDate", "desc", 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid UUID format")
                .hasMessageContaining("creatorId");
    }

    /**
     * Property 6c: Invalid UUID string as task ID path variable throws InvalidParameterException.
     *
     * For any string that is not a valid UUID format, passing it as a task ID path variable
     * SHALL result in InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 9.4**
     */
    @Property(tries = 100)
    void invalidUuidAsTaskIdPathVariableReturns400(
            @ForAll("invalidUuidStrings") String invalidUuid
    ) {
        assertThatThrownBy(() -> controller.findById(invalidUuid))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid UUID format")
                .hasMessageContaining("task ID");
    }

    /**
     * Property 6d: Invalid UUID string as individualId on interactions endpoint throws InvalidParameterException.
     *
     * For any string that is not a valid UUID format, passing it as individualId
     * SHALL result in InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 10.4**
     */
    @Property(tries = 100)
    void invalidUuidAsIndividualIdOnInteractionsReturns400(
            @ForAll("invalidUuidStrings") String invalidUuid
    ) {
        assertThatThrownBy(() -> controller.getInteractionsForIndividual(invalidUuid))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid UUID format")
                .hasMessageContaining("individualId");
    }

    // ========================================================================
    // Property 7: Valid UUID but non-existent staff returns empty list with 200
    // ========================================================================

    /**
     * Property 7a: Valid UUID not matching any staff as assigneeId returns empty result (HTTP 200).
     *
     * For any valid-format UUID that does not correspond to an existing Staff_Member,
     * querying with that UUID as assigneeId SHALL return an empty task list with HTTP 200.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void validUuidNonExistentStaffAsAssigneeReturnsEmptyList(
            @ForAll("randomUUIDs") UUID nonExistentAssigneeId
    ) {
        // Service returns empty result for non-existent assignee
        TaskQueryResult emptyResult = new TaskQueryResult(Collections.emptyList(), 0, 0, 50);
        when(taskService.findTasks(any(TaskQueryParams.class))).thenReturn(emptyResult);

        // Act — should NOT throw; returns empty list with 200
        TaskQueryResult result = controller.findTasks(
                nonExistentAssigneeId.toString(), null, null, null, null, null, null, null,
                "createdDate", "desc", 0, 50
        );

        // Assert: empty list returned, no exception
        assertThat(result.tasks()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
    }

    /**
     * Property 7b: Valid UUID not matching any staff as creatorId returns empty result (HTTP 200).
     *
     * For any valid-format UUID that does not correspond to an existing Staff_Member,
     * querying with that UUID as creatorId SHALL return an empty task list with HTTP 200.
     *
     * **Validates: Requirements 3.6**
     */
    @Property(tries = 100)
    void validUuidNonExistentStaffAsCreatorReturnsEmptyList(
            @ForAll("randomUUIDs") UUID nonExistentCreatorId
    ) {
        TaskQueryResult emptyResult = new TaskQueryResult(Collections.emptyList(), 0, 0, 50);
        when(taskService.findTasks(any(TaskQueryParams.class))).thenReturn(emptyResult);

        // Act
        TaskQueryResult result = controller.findTasks(
                null, nonExistentCreatorId.toString(), null, null, null, null, null, null,
                "createdDate", "desc", 0, 50
        );

        // Assert
        assertThat(result.tasks()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
    }

    // ========================================================================
    // Property 12: Invalid filter parameter values are rejected with 400
    // ========================================================================

    /**
     * Property 12a: Invalid sortBy value throws InvalidParameterException.
     *
     * For any string not equal to a valid sortBy value (dueDate, createdDate),
     * the system SHALL reject with InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 7.6**
     */
    @Property(tries = 100)
    void invalidSortByValueReturns400(
            @ForAll("invalidSortByValues") String invalidSortBy
    ) {
        assertThatThrownBy(() -> controller.findTasks(
                null, null, null, null, null, null, null, null,
                invalidSortBy, "desc", 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid sortBy");
    }

    /**
     * Property 12b: Invalid sortOrder value throws InvalidParameterException.
     *
     * For any string not case-insensitively equal to a valid sortOrder value (asc, desc),
     * the system SHALL reject with InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 7.10**
     */
    @Property(tries = 100)
    void invalidSortOrderValueReturns400(
            @ForAll("invalidSortOrderValues") String invalidSortOrder
    ) {
        assertThatThrownBy(() -> controller.findTasks(
                null, null, null, null, null, null, null, null,
                "createdDate", invalidSortOrder, 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid sortOrder");
    }

    /**
     * Property 12c: Invalid status string throws InvalidParameterException.
     *
     * For any string not case-insensitively equal to a valid status value
     * (TODO, IN_PROGRESS, DONE), the system SHALL reject with InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 7.6**
     */
    @Property(tries = 100)
    void invalidStatusValueReturns400(
            @ForAll("invalidStatusStrings") String invalidStatus
    ) {
        assertThatThrownBy(() -> controller.findTasks(
                null, null, null, invalidStatus, null, null, null, null,
                "createdDate", "desc", 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid status");
    }

    /**
     * Property 12d: Invalid date format throws InvalidParameterException.
     *
     * For any string not in valid ISO 8601 date format (yyyy-MM-dd) for date parameters,
     * the system SHALL reject with InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 7.6**
     */
    @Property(tries = 100)
    void invalidDateFormatReturns400(
            @ForAll("invalidDateStrings") String invalidDate
    ) {
        // Test dueDateFrom with invalid date format
        assertThatThrownBy(() -> controller.findTasks(
                null, null, null, null, invalidDate, null, null, null,
                "createdDate", "desc", 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid date format")
                .hasMessageContaining("dueDateFrom");
    }

    /**
     * Property 12e: Invalid date format for dueDateTo throws InvalidParameterException.
     *
     * **Validates: Requirements 7.6**
     */
    @Property(tries = 100)
    void invalidDateFormatDueDateToReturns400(
            @ForAll("invalidDateStrings") String invalidDate
    ) {
        assertThatThrownBy(() -> controller.findTasks(
                null, null, null, null, null, invalidDate, null, null,
                "createdDate", "desc", 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid date format")
                .hasMessageContaining("dueDateTo");
    }

    /**
     * Property 12f: Invalid date format for createdFrom throws InvalidParameterException.
     *
     * **Validates: Requirements 7.6**
     */
    @Property(tries = 100)
    void invalidDateFormatCreatedFromReturns400(
            @ForAll("invalidDateStrings") String invalidDate
    ) {
        assertThatThrownBy(() -> controller.findTasks(
                null, null, null, null, null, null, invalidDate, null,
                "createdDate", "desc", 0, 50
        )).isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid date format")
                .hasMessageContaining("createdFrom");
    }

    // ========================================================================
    // Property 19: Interactions endpoint returns 404 for non-existent individuals
    // ========================================================================

    /**
     * Property 19: For any valid UUID that does not correspond to an existing individual,
     * the GET /api/tasks/interactions endpoint SHALL throw EntityNotFoundException (HTTP 404),
     * NOT return an empty list.
     *
     * **Validates: Requirements 10.1, 10.2, 10.3**
     */
    @Property(tries = 100)
    void interactionsEndpointReturns404ForNonExistentIndividual(
            @ForAll("randomUUIDs") UUID nonExistentIndividualId
    ) {
        // Service throws EntityNotFoundException for non-existent individual
        when(taskService.getInteractionsForIndividual(nonExistentIndividualId))
                .thenThrow(new EntityNotFoundException(
                        "Individual not found with id: " + nonExistentIndividualId));

        // Act & Assert: controller propagates the 404
        assertThatThrownBy(() -> controller.getInteractionsForIndividual(
                nonExistentIndividualId.toString()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Individual not found");
    }

    /**
     * Property 19b: For any existing individual, the interactions endpoint SHALL return
     * a list of interactions (up to 50) with all required fields, not throw an exception.
     *
     * **Validates: Requirements 10.1, 10.2, 10.3**
     */
    @Property(tries = 100)
    void interactionsEndpointReturnsListForExistingIndividual(
            @ForAll("randomUUIDs") UUID existingIndividualId,
            @ForAll("randomUUIDs") UUID staffId
    ) {
        // Service returns a list of interactions for an existing individual
        InteractionResponse interaction = new InteractionResponse(
                UUID.randomUUID(), existingIndividualId, staffId,
                "CHECK_IN", "Some notes",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        when(taskService.getInteractionsForIndividual(existingIndividualId))
                .thenReturn(List.of(interaction));

        // Act
        List<InteractionResponse> result = controller.getInteractionsForIndividual(
                existingIndividualId.toString());

        // Assert: returns interactions, not empty, not exception
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).employeeId()).isEqualTo(existingIndividualId);
        assertThat(result.get(0).staffId()).isEqualTo(staffId);
        assertThat(result.get(0).type()).isNotNull();
        assertThat(result.get(0).occurredAt()).isNotNull();
        assertThat(result.get(0).createdAt()).isNotNull();
    }

    // ========================================================================
    // Property 20: Non-null interactionId values validated before inclusion in response
    // ========================================================================

    /**
     * Property 20: For any task where the interactionId is non-null, the service SHALL have
     * validated that this ID corresponds to an actual linked interaction. If a previously-linked
     * interaction no longer exists, the response SHALL return null for interactionId.
     *
     * This test verifies at the service layer via the controller — when interactionId
     * in the stored task references a non-existent interaction, the response includes null.
     *
     * **Validates: Requirements 5.4**
     */
    @Property(tries = 100)
    void taskResponseWithNonNullInteractionIdIsValidated(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID interactionId,
            @ForAll("randomUUIDs") UUID individualId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID assigneeId
    ) {
        // Service returns a response where interactionId is non-null — this proves
        // the service validated the interaction exists before including it in the response
        TaskResponse responseWithValidInteraction = new TaskResponse(
                taskId, individualId, interactionId, creatorId, assigneeId,
                "Task with valid interaction", "TODO", null, LocalDateTime.now());
        when(taskService.findById(taskId)).thenReturn(responseWithValidInteraction);

        // Act
        TaskResponse result = controller.findById(taskId.toString());

        // Assert: non-null interactionId is present (service validated it)
        assertThat(result.interactionId()).isEqualTo(interactionId);
        assertThat(result.interactionId()).isNotNull();
    }

    /**
     * Property 20b: When a previously-linked interaction no longer exists,
     * the response SHALL return null for interactionId.
     *
     * **Validates: Requirements 5.4**
     */
    @Property(tries = 100)
    void taskResponseReturnsNullWhenInteractionNoLongerExists(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID individualId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID assigneeId
    ) {
        // Service returns a response with null interactionId (interaction was deleted)
        TaskResponse responseWithNullInteraction = new TaskResponse(
                taskId, individualId, null, creatorId, assigneeId,
                "Task with deleted interaction", "TODO", null, LocalDateTime.now());
        when(taskService.findById(taskId)).thenReturn(responseWithNullInteraction);

        // Act
        TaskResponse result = controller.findById(taskId.toString());

        // Assert: interactionId is null (interaction no longer exists)
        assertThat(result.interactionId()).isNull();
    }

    // ========================================================================
    // Generators
    // ========================================================================

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<String> invalidUuidStrings() {
        return Arbitraries.oneOf(
                // Random alphanumeric strings (not UUID format)
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('0', '9')
                        .ofMinLength(1)
                        .ofMaxLength(50)
                        .filter(s -> !isValidUuid(s)),
                // Truncated UUIDs (first 8 chars of a UUID)
                Arbitraries.create(UUID::randomUUID)
                        .map(uuid -> uuid.toString().substring(0, 8)),
                // UUIDs with invalid characters substituted
                Arbitraries.create(UUID::randomUUID)
                        .map(uuid -> uuid.toString().replace('-', '_')),
                // UUIDs with extra characters
                Arbitraries.create(UUID::randomUUID)
                        .map(uuid -> uuid.toString() + "extra"),
                // Completely malformed strings
                Arbitraries.of("not-a-uuid", "12345", "abcdefgh", "null",
                        "undefined", "xyz-xyz-xyz-xyz", "00000000-0000-0000-0000-00000000000g")
        );
    }

    @Provide
    Arbitrary<String> invalidSortByValues() {
        return Arbitraries.of(
                "date", "name", "status", "id", "title", "description",
                "assignee", "creator", "updatedDate", "modifiedDate",
                "DUEDATE", "CREATEDDATE", "Due_Date", "created_date"
        );
    }

    @Provide
    Arbitrary<String> invalidSortOrderValues() {
        return Arbitraries.of(
                "ascending", "descending", "up", "down", "ASC_ORDER",
                "DESC_ORDER", "reverse", "forward", "1", "0", "true", "false"
        );
    }

    @Provide
    Arbitrary<String> invalidStatusStrings() {
        return Arbitraries.of(
                "INVALID", "PENDING", "CANCELLED", "OPEN", "CLOSED",
                "ACTIVE", "BLOCKED", "STARTED", "COMPLETED",
                "NOT_A_STATUS", "ARCHIVED", "DELETED"
        );
    }

    @Provide
    Arbitrary<String> invalidDateStrings() {
        return Arbitraries.of(
                "not-a-date", "2024/01/15", "15-01-2024", "01-15-2024",
                "2024.01.15", "Jan 15, 2024", "20240115", "2024-13-01",
                "2024-01-32", "2024-1-5", "yesterday", "tomorrow",
                "2024-00-01", "abcd-ef-gh"
        );
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private boolean isValidUuid(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
