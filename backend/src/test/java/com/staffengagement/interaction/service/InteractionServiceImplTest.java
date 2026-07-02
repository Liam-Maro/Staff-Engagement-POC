package com.staffengagement.interaction.service;

import com.staffengagement.employee.dto.EmployeeResponse;
import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.dto.CreateFollowUpTaskRequest;
import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.dto.UpdateInteractionRequest;
import com.staffengagement.interaction.exception.InteractionNotFoundException;
import com.staffengagement.interaction.exception.InvalidDateRangeException;
import com.staffengagement.interaction.exception.TaskCreationFailedException;
import com.staffengagement.interaction.model.Interaction;
import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.repository.InteractionRepository;
import com.staffengagement.interaction.repository.InteractionSpecifications;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceImplTest {

    @Mock
    private InteractionRepository repository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private InteractionServiceImpl service;

    // --- Helper methods ---

    private Interaction createInteractionEntity(UUID id, UUID employeeId, UUID staffId,
                                                 InteractionType type, String notes,
                                                 LocalDateTime occurredAt) {
        var interaction = new Interaction();
        interaction.setEmployeeId(employeeId);
        interaction.setStaffId(staffId);
        interaction.setType(type);
        interaction.setNotes(notes);
        interaction.setOccurredAt(occurredAt);
        ReflectionTestUtils.setField(interaction, "id", id);
        ReflectionTestUtils.setField(interaction, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(interaction, "updatedAt", LocalDateTime.now());
        return interaction;
    }

    private EmployeeResponse createEmployeeResponse(UUID id) {
        return new EmployeeResponse(id, "John", "Doe", "john@example.com",
                "Engineering", "Developer", LocalDate.of(2020, 1, 1), true);
    }

    // --- Test: create ---

    @Test
    void create_withValidData_persistsAndReturnsResponse() {
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID interactionId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now().minusDays(1);

        var request = new CreateInteractionRequest(employeeId, staffId,
                InteractionType.CHECK_IN, "Test notes", occurredAt);

        when(employeeService.findById(employeeId)).thenReturn(createEmployeeResponse(employeeId));

        var savedEntity = createInteractionEntity(interactionId, employeeId, staffId,
                InteractionType.CHECK_IN, "Test notes", occurredAt);
        when(repository.save(any(Interaction.class))).thenReturn(savedEntity);

        InteractionResponse response = service.create(request);

        verify(employeeService).findById(employeeId);
        verify(repository).save(any(Interaction.class));
        assertThat(response.id()).isEqualTo(interactionId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.staffId()).isEqualTo(staffId);
        assertThat(response.type()).isEqualTo(InteractionType.CHECK_IN);
        assertThat(response.notes()).isEqualTo("Test notes");
        assertThat(response.occurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void create_shouldSetAllFieldsOnEntity() {
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID interactionId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2024, 6, 15, 10, 30);

        var request = new CreateInteractionRequest(employeeId, staffId,
                InteractionType.MENTORING, "Mentoring session notes", occurredAt);

        when(employeeService.findById(employeeId)).thenReturn(createEmployeeResponse(employeeId));

        var savedEntity = createInteractionEntity(interactionId, employeeId, staffId,
                InteractionType.MENTORING, "Mentoring session notes", occurredAt);
        when(repository.save(any(Interaction.class))).thenReturn(savedEntity);

        service.create(request);

        ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
        verify(repository).save(captor.capture());

        Interaction captured = captor.getValue();
        assertThat(captured.getEmployeeId()).isEqualTo(employeeId);
        assertThat(captured.getStaffId()).isEqualTo(staffId);
        assertThat(captured.getType()).isEqualTo(InteractionType.MENTORING);
        assertThat(captured.getNotes()).isEqualTo("Mentoring session notes");
        assertThat(captured.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void create_withNonExistentEmployee_throwsException() {
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now().minusDays(1);

        var request = new CreateInteractionRequest(employeeId, staffId,
                InteractionType.MENTORING, "Notes", occurredAt);

        when(employeeService.findById(employeeId))
                .thenThrow(new RuntimeException("Employee not found"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employee not found");

        verify(repository, never()).save(any());
    }

    // --- Test: update ---

    @Test
    void update_withValidData_updatesFieldsAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDateTime originalOccurredAt = LocalDateTime.now().minusDays(5);
        LocalDateTime newOccurredAt = LocalDateTime.now().minusDays(1);

        var existingEntity = createInteractionEntity(id, employeeId, staffId,
                InteractionType.CHECK_IN, "Old notes", originalOccurredAt);

        var request = new UpdateInteractionRequest(InteractionType.MENTORING,
                "Updated notes", newOccurredAt);

        when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(repository.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InteractionResponse response = service.update(id, request);

        assertThat(response.type()).isEqualTo(InteractionType.MENTORING);
        assertThat(response.notes()).isEqualTo("Updated notes");
        assertThat(response.occurredAt()).isEqualTo(newOccurredAt);
    }

    @Test
    void update_preservesOriginalEmployeeIdAndStaffId() {
        UUID id = UUID.randomUUID();
        UUID originalEmployeeId = UUID.randomUUID();
        UUID originalStaffId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now().minusDays(1);

        var existingEntity = createInteractionEntity(id, originalEmployeeId, originalStaffId,
                InteractionType.INFORMAL, "Notes", occurredAt);

        var request = new UpdateInteractionRequest(InteractionType.CATCH_UP,
                "New notes", occurredAt);

        when(repository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(repository.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InteractionResponse response = service.update(id, request);

        assertThat(response.employeeId()).isEqualTo(originalEmployeeId);
        assertThat(response.staffId()).isEqualTo(originalStaffId);
    }

    @Test
    void update_withNonExistentId_throwsInteractionNotFoundException() {
        UUID id = UUID.randomUUID();
        var request = new UpdateInteractionRequest(InteractionType.CHECK_IN,
                "Notes", LocalDateTime.now().minusDays(1));

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(InteractionNotFoundException.class);
    }

    // --- Test: delete ---

    @Test
    void delete_withExistingId_deletesFromRepository() {
        UUID id = UUID.randomUUID();
        var entity = createInteractionEntity(id, UUID.randomUUID(), UUID.randomUUID(),
                InteractionType.CHECK_IN, null, LocalDateTime.now().minusDays(1));

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        service.delete(id);

        verify(repository).delete(entity);
    }

    @Test
    void delete_withNonExistentId_throwsInteractionNotFoundException() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(InteractionNotFoundException.class);
    }

    // --- Test: findById ---

    @Test
    void findById_withExistingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now().minusDays(2);

        var entity = createInteractionEntity(id, employeeId, staffId,
                InteractionType.PERFORMANCE_REVIEW, "Review notes", occurredAt);

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        InteractionResponse response = service.findById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.type()).isEqualTo(InteractionType.PERFORMANCE_REVIEW);
        assertThat(response.notes()).isEqualTo("Review notes");
    }

    @Test
    void findById_withNonExistentId_throwsInteractionNotFoundException() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(InteractionNotFoundException.class);
    }

    // --- Test: findAll ---

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withPagination_returnsPage() {
        UUID employeeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"));

        var entity = createInteractionEntity(UUID.randomUUID(), employeeId, UUID.randomUUID(),
                InteractionType.CHECK_IN, "Notes", LocalDateTime.now().minusDays(1));
        Page<Interaction> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<InteractionResponse> result = service.findAll(employeeId, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void findAll_withInvalidDateRange_throwsInvalidDateRangeException() {
        LocalDateTime fromDate = LocalDateTime.now();
        LocalDateTime toDate = LocalDateTime.now().minusDays(5);
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.findAll(null, null, fromDate, toDate, pageable))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_capsPageSizeAt100() {
        Pageable oversizedPageable = PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "occurredAt"));

        Page<Interaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100,
                Sort.by(Sort.Direction.DESC, "occurredAt")), 0);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        service.findAll(null, null, null, null, oversizedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    // --- Test: createFollowUpTask ---

    @Test
    void createFollowUpTask_withValidData_delegatesToTaskService() {
        UUID interactionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.now().plusDays(7);

        var entity = createInteractionEntity(interactionId, employeeId, staffId,
                InteractionType.MENTORING, "Notes", LocalDateTime.now().minusDays(1));

        var followUpRequest = new CreateFollowUpTaskRequest("Follow up task", "Description", dueDate);

        var taskResponse = new TaskResponse(UUID.randomUUID(), employeeId, interactionId,
                staffId, staffId, "Description", "To Do", dueDate, LocalDateTime.now());

        when(repository.findById(interactionId)).thenReturn(Optional.of(entity));
        when(taskService.create(any(CreateTaskRequest.class), any(UUID.class))).thenReturn(taskResponse);

        Object result = service.createFollowUpTask(interactionId, followUpRequest);

        assertThat(result).isEqualTo(taskResponse);

        ArgumentCaptor<CreateTaskRequest> taskCaptor = ArgumentCaptor.forClass(CreateTaskRequest.class);
        verify(taskService).create(taskCaptor.capture(), eq(staffId));
        CreateTaskRequest capturedTask = taskCaptor.getValue();
        assertThat(capturedTask.individualId()).isEqualTo(employeeId);
        assertThat(capturedTask.interactionId()).isEqualTo(interactionId);
        assertThat(capturedTask.assigneeId()).isEqualTo(staffId);
        assertThat(capturedTask.dueDate()).isEqualTo(dueDate);
    }

    @Test
    void createFollowUpTask_withNonExistentInteraction_throwsNotFoundException() {
        UUID interactionId = UUID.randomUUID();
        var followUpRequest = new CreateFollowUpTaskRequest("Task", null, null);

        when(repository.findById(interactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFollowUpTask(interactionId, followUpRequest))
                .isInstanceOf(InteractionNotFoundException.class);
    }

    @Test
    void createFollowUpTask_whenTaskServiceFails_throwsTaskCreationFailedException() {
        UUID interactionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        var entity = createInteractionEntity(interactionId, employeeId, staffId,
                InteractionType.CHECK_IN, null, LocalDateTime.now().minusDays(1));

        var followUpRequest = new CreateFollowUpTaskRequest("Task", "Desc", LocalDate.now().plusDays(3));

        when(repository.findById(interactionId)).thenReturn(Optional.of(entity));
        when(taskService.create(any(CreateTaskRequest.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThatThrownBy(() -> service.createFollowUpTask(interactionId, followUpRequest))
                .isInstanceOf(TaskCreationFailedException.class)
                .hasMessageContaining("Service unavailable");
    }

    // --- Test: findAll filter specifications ---

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withTypeFilter_callsHasTypeSpecification() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<Interaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        try (MockedStatic<InteractionSpecifications> specs = mockStatic(InteractionSpecifications.class)) {
            Specification<Interaction> mockSpec = mock(Specification.class);
            specs.when(() -> InteractionSpecifications.hasType(InteractionType.MENTORING)).thenReturn(mockSpec);

            service.findAll(null, InteractionType.MENTORING, null, null, pageable);

            specs.verify(() -> InteractionSpecifications.hasType(InteractionType.MENTORING));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withEmployeeIdFilter_callsHasEmployeeIdSpecification() {
        UUID employeeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<Interaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        try (MockedStatic<InteractionSpecifications> specs = mockStatic(InteractionSpecifications.class)) {
            Specification<Interaction> mockSpec = mock(Specification.class);
            specs.when(() -> InteractionSpecifications.hasEmployeeId(employeeId)).thenReturn(mockSpec);

            service.findAll(employeeId, null, null, null, pageable);

            specs.verify(() -> InteractionSpecifications.hasEmployeeId(employeeId));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withDateFromFilter_callsOccurredAfterSpecification() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"));
        LocalDateTime fromDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        Page<Interaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        try (MockedStatic<InteractionSpecifications> specs = mockStatic(InteractionSpecifications.class)) {
            Specification<Interaction> mockSpec = mock(Specification.class);
            specs.when(() -> InteractionSpecifications.occurredAfter(fromDate)).thenReturn(mockSpec);

            service.findAll(null, null, fromDate, null, pageable);

            specs.verify(() -> InteractionSpecifications.occurredAfter(fromDate));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withDateToFilter_callsOccurredBeforeSpecification() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"));
        LocalDateTime toDate = LocalDateTime.of(2024, 12, 31, 23, 59);
        Page<Interaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        try (MockedStatic<InteractionSpecifications> specs = mockStatic(InteractionSpecifications.class)) {
            Specification<Interaction> mockSpec = mock(Specification.class);
            specs.when(() -> InteractionSpecifications.occurredBefore(toDate)).thenReturn(mockSpec);

            service.findAll(null, null, null, toDate, pageable);

            specs.verify(() -> InteractionSpecifications.occurredBefore(toDate));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withNoFilters_doesNotCallAnySpecificationBuilder() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<Interaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        try (MockedStatic<InteractionSpecifications> specs = mockStatic(InteractionSpecifications.class)) {
            service.findAll(null, null, null, null, pageable);

            specs.verify(() -> InteractionSpecifications.hasEmployeeId(any()), never());
            specs.verify(() -> InteractionSpecifications.hasType(any()), never());
            specs.verify(() -> InteractionSpecifications.occurredAfter(any()), never());
            specs.verify(() -> InteractionSpecifications.occurredBefore(any()), never());
        }
    }

    // --- Test: createFollowUpTask description conditional (line 129) ---

    @Test
    void createFollowUpTask_withNullDescription_buildsTaskTitleWithoutDescription() {
        UUID interactionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.now().plusDays(7);

        var entity = createInteractionEntity(interactionId, employeeId, staffId,
                InteractionType.MENTORING, "Notes", LocalDateTime.now().minusDays(1));

        var followUpRequest = new CreateFollowUpTaskRequest("Follow up", null, dueDate);

        var taskResponse = new TaskResponse(UUID.randomUUID(), employeeId, interactionId,
                staffId, staffId, "Follow up", "To Do", dueDate, LocalDateTime.now());

        when(repository.findById(interactionId)).thenReturn(Optional.of(entity));
        when(taskService.create(any(CreateTaskRequest.class), any(UUID.class))).thenReturn(taskResponse);

        service.createFollowUpTask(interactionId, followUpRequest);

        ArgumentCaptor<CreateTaskRequest> taskCaptor = ArgumentCaptor.forClass(CreateTaskRequest.class);
        verify(taskService).create(taskCaptor.capture(), eq(staffId));
        // When description is null, task description should be just the title without " - "
        assertThat(taskCaptor.getValue().description()).isEqualTo("Follow up");
        assertThat(taskCaptor.getValue().description()).doesNotContain(" - ");
    }

    @Test
    void createFollowUpTask_withDescription_buildsTaskTitleWithDescription() {
        UUID interactionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.now().plusDays(7);

        var entity = createInteractionEntity(interactionId, employeeId, staffId,
                InteractionType.CHECK_IN, "Notes", LocalDateTime.now().minusDays(1));

        var followUpRequest = new CreateFollowUpTaskRequest("Follow up", "Some details", dueDate);

        var taskResponse = new TaskResponse(UUID.randomUUID(), employeeId, interactionId,
                staffId, staffId, "Follow up - Some details", "To Do", dueDate, LocalDateTime.now());

        when(repository.findById(interactionId)).thenReturn(Optional.of(entity));
        when(taskService.create(any(CreateTaskRequest.class), any(UUID.class))).thenReturn(taskResponse);

        service.createFollowUpTask(interactionId, followUpRequest);

        ArgumentCaptor<CreateTaskRequest> taskCaptor = ArgumentCaptor.forClass(CreateTaskRequest.class);
        verify(taskService).create(taskCaptor.capture(), eq(staffId));
        // When description is provided, task description should be "title - description"
        assertThat(taskCaptor.getValue().description()).isEqualTo("Follow up - Some details");
    }
}
