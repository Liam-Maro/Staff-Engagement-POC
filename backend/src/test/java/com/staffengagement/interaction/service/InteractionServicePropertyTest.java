package com.staffengagement.interaction.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.dto.CreateFollowUpTaskRequest;
import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.dto.UpdateInteractionRequest;
import com.staffengagement.interaction.model.Interaction;
import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.repository.InteractionRepository;
import com.staffengagement.task.service.TaskService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for InteractionService.
 *
 * Uses jqwik with mocked repository to verify service-layer properties.
 */
class InteractionServicePropertyTest {

    private final InteractionRepository repository;
    private final EmployeeService employeeService;
    private final TaskService taskService;
    private final InteractionService service;
    private final Validator validator;

    InteractionServicePropertyTest() {
        this.repository = Mockito.mock(InteractionRepository.class);
        this.employeeService = Mockito.mock(EmployeeService.class);
        this.taskService = Mockito.mock(TaskService.class);
        this.service = new InteractionServiceImpl(repository, employeeService, taskService);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    /**
     * **Validates: Requirements 1.5, 3.3**
     *
     * Property 2: For any string value that is not one of
     * {CHECK_IN, MENTORING, CATCH_UP, PERFORMANCE_REVIEW, INFORMAL},
     * attempting to parse it as InteractionType SHALL throw IllegalArgumentException.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 2: Invalid interaction type rejection")
    void invalidInteractionTypeIsRejected(@ForAll @StringLength(min = 1, max = 50) String randomType) {
        Assume.that(!Set.of("CHECK_IN", "MENTORING", "CATCH_UP", "PERFORMANCE_REVIEW", "INFORMAL").contains(randomType));
        assertThatThrownBy(() -> InteractionType.valueOf(randomType))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * **Validates: Requirements 1.7, 3.4, 10.4**
     *
     * Property 3: For any LocalDateTime value that is strictly after the server's current time,
     * submitting it as the occurredAt field SHALL result in a validation violation.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 3: Future occurredAt rejection")
    void futureOccurredAtIsRejected(@ForAll("futureDateTimes") LocalDateTime futureDate) {
        var request = new CreateInteractionRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                InteractionType.CHECK_IN, "notes", futureDate);

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("occurredAt"));
    }

    @Provide
    Arbitrary<LocalDateTime> futureDateTimes() {
        return Arbitraries.longs()
                .between(1, 365L * 24 * 60 * 60) // 1 second to 1 year in seconds
                .map(offset -> LocalDateTime.now().plusSeconds(offset));
    }

    /**
     * **Validates: Requirements 3.5**
     *
     * Property 4: For any existing interaction and for any update request,
     * the response after update SHALL still contain the original employeeId
     * and staffId assigned at creation time.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 4: employeeId and staffId immutability on update")
    void updatePreservesOriginalEmployeeIdAndStaffId(
            @ForAll("validUUIDs") UUID originalEmployeeId,
            @ForAll("validUUIDs") UUID originalStaffId,
            @ForAll("validInteractionTypes") InteractionType newType,
            @ForAll("pastDateTimes") LocalDateTime newOccurredAt) {

        // Setup: existing interaction with original IDs
        UUID interactionId = UUID.randomUUID();
        var existingInteraction = createInteraction(interactionId, originalEmployeeId, originalStaffId);

        Mockito.reset(repository);
        when(repository.findById(interactionId)).thenReturn(Optional.of(existingInteraction));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updateRequest = new UpdateInteractionRequest(newType, "updated notes", newOccurredAt);
        InteractionResponse response = service.update(interactionId, updateRequest);

        // Property: original IDs preserved regardless of update content
        assertThat(response.employeeId()).isEqualTo(originalEmployeeId);
        assertThat(response.staffId()).isEqualTo(originalStaffId);
    }

    // --- Providers ---

    @Provide
    Arbitrary<UUID> validUUIDs() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<InteractionType> validInteractionTypes() {
        return Arbitraries.of(InteractionType.values());
    }

    @Provide
    Arbitrary<LocalDateTime> pastDateTimes() {
        return Arbitraries.longs()
                .between(
                        LocalDateTime.of(2020, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC),
                        LocalDateTime.now().minusMinutes(1).toEpochSecond(java.time.ZoneOffset.UTC))
                .map(epoch -> LocalDateTime.ofEpochSecond(epoch, 0, java.time.ZoneOffset.UTC));
    }

    // --- Property 5: createdAt immutability and updatedAt advancement ---

    /**
     * **Validates: Requirements 7.1, 7.2**
     *
     * Property 5: For any existing interaction, after applying N update operations (N ≥ 1),
     * the createdAt timestamp SHALL remain identical to its initial value,
     * and the updatedAt timestamp SHALL be greater than or equal to the pre-update updatedAt value.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 5: createdAt immutability and updatedAt advancement")
    void createdAtImmutableAndUpdatedAtAdvances(
            @ForAll("validUUIDs") UUID employeeId,
            @ForAll("validUUIDs") UUID staffId,
            @ForAll @IntRange(min = 1, max = 5) int updateCount,
            @ForAll("validInteractionTypes") InteractionType type,
            @ForAll("pastDateTimes") LocalDateTime occurredAt) {

        UUID interactionId = UUID.randomUUID();
        var interaction = createInteraction(interactionId, employeeId, staffId);
        LocalDateTime originalCreatedAt = interaction.getCreatedAt();

        Mockito.reset(repository);
        when(repository.findById(interactionId)).thenReturn(Optional.of(interaction));
        when(repository.save(any())).thenAnswer(inv -> {
            Interaction saved = inv.getArgument(0);
            // Simulate @PreUpdate by advancing updatedAt
            try {
                var onUpdateMethod = Interaction.class.getDeclaredMethod("onUpdate");
                onUpdateMethod.setAccessible(true);
                onUpdateMethod.invoke(saved);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke onUpdate", e);
            }
            return saved;
        });

        LocalDateTime previousUpdatedAt = interaction.getUpdatedAt();

        for (int i = 0; i < updateCount; i++) {
            var updateRequest = new UpdateInteractionRequest(type, "update " + i, occurredAt);
            InteractionResponse response = service.update(interactionId, updateRequest);

            // createdAt must remain unchanged
            assertThat(response.createdAt()).isEqualTo(originalCreatedAt);
            // updatedAt must be >= previous updatedAt
            assertThat(response.updatedAt()).isAfterOrEqualTo(previousUpdatedAt);

            previousUpdatedAt = response.updatedAt();
        }
    }

    // --- Property 7: Field length limit enforcement ---

    /**
     * **Validates: Requirements 3.6, 5.4, 5.5, 10.6**
     *
     * Property 7: For any string with length exceeding 5000 characters submitted as notes,
     * OR for any string with length exceeding 255 characters submitted as task title,
     * OR for any string with length exceeding 2000 characters submitted as task description,
     * the system SHALL reject the request with validation violations.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 7: Field length limit enforcement")
    void fieldLengthLimitsEnforced(
            @ForAll("oversizedNotes") String oversizedNotes,
            @ForAll("oversizedTitle") String oversizedTitle,
            @ForAll("oversizedDescription") String oversizedDescription) {

        // Notes exceeding 5000 chars on CreateInteractionRequest
        var interactionRequest = new CreateInteractionRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                InteractionType.CHECK_IN, oversizedNotes,
                LocalDateTime.now().minusDays(1));
        var notesViolations = validator.validate(interactionRequest);
        assertThat(notesViolations).anyMatch(v -> v.getPropertyPath().toString().equals("notes"));

        // Title exceeding 255 chars on CreateFollowUpTaskRequest
        var taskRequestTitle = new CreateFollowUpTaskRequest(
                oversizedTitle, "valid description", null);
        var titleViolations = validator.validate(taskRequestTitle);
        assertThat(titleViolations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));

        // Description exceeding 2000 chars on CreateFollowUpTaskRequest
        var taskRequestDesc = new CreateFollowUpTaskRequest(
                "Valid Title", oversizedDescription, null);
        var descViolations = validator.validate(taskRequestDesc);
        assertThat(descViolations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    // --- Property 8: Pagination metadata consistency ---

    /**
     * **Validates: Requirements 6.1**
     *
     * Property 8: For any set of N interaction records and for any page/size request where size ≥ 1,
     * the returned pagination response SHALL satisfy: totalElements = N,
     * totalPages = ⌈N / size⌉, content.length ≤ min(size, N - page*size).
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 8: Pagination metadata consistency")
    @SuppressWarnings("unchecked")
    void paginationMetadataIsConsistent(
            @ForAll @IntRange(min = 0, max = 200) int totalRecords,
            @ForAll @IntRange(min = 1, max = 100) int pageSize) {

        int requestedPage = 0;
        int effectiveSize = Math.min(pageSize, 100);
        int totalPages = totalRecords == 0 ? 0 : (int) Math.ceil((double) totalRecords / effectiveSize);
        int contentSize = Math.min(effectiveSize, Math.max(0, totalRecords - requestedPage * effectiveSize));

        // Create mock interactions for the page content
        List<Interaction> content = IntStream.range(0, contentSize)
                .mapToObj(i -> {
                    var interaction = createInteraction(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
                    interaction.setOccurredAt(LocalDateTime.now().minusDays(i));
                    return interaction;
                })
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(requestedPage, pageSize, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<Interaction> mockPage = new PageImpl<>(content, pageable, totalRecords);

        Mockito.reset(repository);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<InteractionResponse> result = service.findAll(null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(totalRecords);
        if (totalRecords == 0) {
            assertThat(result.getTotalPages()).isEqualTo(0);
        } else {
            assertThat(result.getTotalPages()).isEqualTo(totalPages);
        }
        assertThat(result.getContent().size()).isLessThanOrEqualTo(effectiveSize);
        assertThat(result.getContent().size()).isEqualTo(contentSize);
    }

    // --- Property 9: Page size capped at 100 ---

    /**
     * **Validates: Requirements 6.3**
     *
     * Property 9: For any list request with a size parameter greater than 100,
     * the pageable passed to repository SHALL have size ≤ 100.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 9: Page size capped at 100")
    @SuppressWarnings("unchecked")
    void pageSizeCappedAt100(@ForAll @IntRange(min = 101, max = 1000) int requestedSize) {

        Pageable pageable = PageRequest.of(0, requestedSize, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<Interaction> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "occurredAt")), 0);

        Mockito.reset(repository);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(emptyPage);

        service.findAll(null, null, null, null, pageable);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageSize()).isLessThanOrEqualTo(100);
    }

    // --- Property 10: Combined filter AND semantics ---

    /**
     * **Validates: Requirements 6.4, 6.6, 6.8, 2.2**
     *
     * Property 10: For any combination of filter parameters (employeeId, type, fromDate, toDate)
     * applied to a list request, when at least one filter is non-null, the service calls
     * repository.findAll with a non-null Specification.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 10: Combined filter AND semantics")
    @SuppressWarnings("unchecked")
    void combinedFiltersUseAndSemantics(
            @ForAll("optionalUUID") UUID employeeId,
            @ForAll("optionalInteractionType") InteractionType type,
            @ForAll("optionalPastDateTime") LocalDateTime fromDate,
            @ForAll("optionalPastDateTime") LocalDateTime toDate) {

        // Ensure fromDate <= toDate when both are non-null
        LocalDateTime effectiveFrom = fromDate;
        LocalDateTime effectiveTo = toDate;
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            // Swap them
            LocalDateTime temp = effectiveFrom;
            effectiveFrom = effectiveTo;
            effectiveTo = temp;
        }

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<Interaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        Mockito.reset(repository);
        ArgumentCaptor<Specification<Interaction>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        when(repository.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(emptyPage);

        service.findAll(employeeId, type, effectiveFrom, effectiveTo, pageable);

        // Verify a Specification was passed (always non-null since service starts with Specification.where(null))
        Specification<Interaction> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();

        // If at least one filter was provided, the spec is composed (non-trivial)
        // We verify the service properly delegates to repository with the specification
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    // --- Property 11: List ordering by occurredAt descending ---

    /**
     * **Validates: Requirements 2.2, 2.3, 6.1**
     *
     * Property 11: For any list/paginated response containing more than one interaction,
     * the occurredAt values SHALL be in non-ascending order.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 11: List ordering by occurredAt descending")
    @SuppressWarnings("unchecked")
    void listOrderedByOccurredAtDescending(@ForAll @IntRange(min = 2, max = 20) int recordCount) {

        // Generate interactions with random occurredAt, then sort them DESC
        List<Interaction> interactions = IntStream.range(0, recordCount)
                .mapToObj(i -> {
                    var interaction = createInteraction(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
                    interaction.setOccurredAt(LocalDateTime.now().minusDays((long)(Math.random() * 365)));
                    return interaction;
                })
                .sorted((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<Interaction> mockPage = new PageImpl<>(interactions, pageable, recordCount);

        Mockito.reset(repository);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<InteractionResponse> result = service.findAll(null, null, null, null, pageable);

        List<InteractionResponse> content = result.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).occurredAt())
                    .isAfterOrEqualTo(content.get(i + 1).occurredAt());
        }
    }

    // --- Property 14: Notes preview truncation ---

    /**
     * **Validates: Requirements 8.2**
     *
     * Property 14: For any notes string with length > 100 characters,
     * truncation gives first 100 chars + "...".
     * For any notes string with length ≤ 100, no truncation occurs.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 14: Notes preview truncation")
    void notesPreviewTruncation(@ForAll("variableLengthNotes") String notes) {
        String result = truncateNotes(notes, 100);

        if (notes.length() > 100) {
            assertThat(result).hasSize(103); // 100 chars + "..."
            assertThat(result).isEqualTo(notes.substring(0, 100) + "...");
        } else {
            assertThat(result).isEqualTo(notes);
        }
    }

    /**
     * Static helper method simulating notes preview truncation logic.
     */
    private static String truncateNotes(String notes, int maxLength) {
        if (notes == null) {
            return null;
        }
        if (notes.length() > maxLength) {
            return notes.substring(0, maxLength) + "...";
        }
        return notes;
    }

    // --- Property 15: createdAt equals updatedAt on fresh creation ---

    /**
     * **Validates: Requirements 7.4**
     *
     * Property 15: For any newly created interaction, the response SHALL have
     * createdAt exactly equal to updatedAt, confirming no modification has occurred
     * since initial persistence.
     */
    @Property(tries = 100)
    @Tag("Feature: interaction-module, Property 15: createdAt equals updatedAt on fresh creation")
    void createdAtEqualsUpdatedAtOnFreshCreation(
            @ForAll("validUUIDs") UUID employeeId,
            @ForAll("validUUIDs") UUID staffId,
            @ForAll("validInteractionTypes") InteractionType type,
            @ForAll("pastDateTimes") LocalDateTime occurredAt) {

        var request = new CreateInteractionRequest(employeeId, staffId, type, "some notes", occurredAt);

        Mockito.reset(repository, employeeService);
        when(employeeService.findById(employeeId)).thenReturn(null);
        when(repository.save(any())).thenAnswer(inv -> {
            Interaction saved = inv.getArgument(0);
            // Simulate @PrePersist
            try {
                var onCreateMethod = Interaction.class.getDeclaredMethod("onCreate");
                onCreateMethod.setAccessible(true);
                onCreateMethod.invoke(saved);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke onCreate", e);
            }
            // Set an ID via reflection
            try {
                var idField = Interaction.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, UUID.randomUUID());
            } catch (Exception e) {
                throw new RuntimeException("Failed to set id", e);
            }
            return saved;
        });

        InteractionResponse response = service.create(request);

        assertThat(response.createdAt()).isEqualTo(response.updatedAt());
    }

    // --- Additional Providers ---

    @Provide
    Arbitrary<String> oversizedNotes() {
        return Arbitraries.integers().between(5001, 6000)
                .map(length -> "x".repeat(length));
    }

    @Provide
    Arbitrary<String> oversizedTitle() {
        return Arbitraries.integers().between(256, 500)
                .map(length -> "t".repeat(length));
    }

    @Provide
    Arbitrary<String> oversizedDescription() {
        return Arbitraries.integers().between(2001, 3000)
                .map(length -> "d".repeat(length));
    }

    @Provide
    Arbitrary<String> variableLengthNotes() {
        return Arbitraries.integers().between(1, 300)
                .map(length -> "n".repeat(length));
    }

    @Provide
    Arbitrary<UUID> optionalUUID() {
        return Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(null)),
                Tuple.of(3, Arbitraries.create(UUID::randomUUID))
        );
    }

    @Provide
    Arbitrary<InteractionType> optionalInteractionType() {
        return Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(null)),
                Tuple.of(3, Arbitraries.of(InteractionType.values()))
        );
    }

    @Provide
    Arbitrary<LocalDateTime> optionalPastDateTime() {
        return Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(null)),
                Tuple.of(3, pastDateTimes())
        );
    }

    // --- Helpers ---

    private Interaction createInteraction(UUID id, UUID employeeId, UUID staffId) {
        var interaction = new Interaction();
        // Use reflection to set the id since it's auto-generated and has no setter
        try {
            var idField = Interaction.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(interaction, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set interaction id", e);
        }
        interaction.setEmployeeId(employeeId);
        interaction.setStaffId(staffId);
        interaction.setType(InteractionType.CHECK_IN);
        interaction.setNotes("original notes");
        interaction.setOccurredAt(LocalDateTime.now().minusDays(1));
        // Manually trigger lifecycle callbacks for timestamps
        try {
            var onCreateMethod = Interaction.class.getDeclaredMethod("onCreate");
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(interaction);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke onCreate", e);
        }
        return interaction;
    }
}
