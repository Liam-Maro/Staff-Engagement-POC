package com.staffengagement.staff.service;

import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.UpdateStaffRequest;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock private StaffRepository repository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StaffServiceImpl service;

    private Staff staff;
    private UUID staffId;

    @BeforeEach
    void setUp() {
        staffId = UUID.randomUUID();
        staff = new Staff();
        ReflectionTestUtils.setField(staff, "id", staffId);
        staff.setEmployeeId(UUID.randomUUID());
        staff.setEmail("john@example.com");
        staff.setPassword("encoded");
        staff.setRole(StaffRole.STAFF);
        staff.setActive(true);
    }

    @Test
    void findAll_shouldReturnAllStaff() {
        when(repository.findAll()).thenReturn(List.of(staff));
        var result = service.findAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("john@example.com");
    }

    @Test
    void findById_shouldReturnStaff_whenExists() {
        when(repository.findById(staffId)).thenReturn(Optional.of(staff));
        var result = service.findById(staffId);
        assertThat(result.id()).isEqualTo(staffId);
        assertThat(result.role()).isEqualTo(StaffRole.STAFF);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(repository.findById(staffId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(staffId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_shouldSaveStaff_withEncodedPassword() {
        when(repository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("encoded");
        when(repository.save(any())).thenReturn(staff);

        var request = new CreateStaffRequest(UUID.randomUUID(), "john@example.com", "plaintext", StaffRole.STAFF);
        var result = service.create(request);

        assertThat(result.email()).isEqualTo("john@example.com");
        verify(passwordEncoder).encode("plaintext");
        verify(repository).save(any(Staff.class));
    }

    @Test
    void create_shouldThrow_whenEmailAlreadyExists() {
        when(repository.existsByEmail("john@example.com")).thenReturn(true);
        var request = new CreateStaffRequest(UUID.randomUUID(), "john@example.com", "password", StaffRole.STAFF);
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void update_shouldChangeRoleAndActiveStatus() {
        when(repository.findById(staffId)).thenReturn(Optional.of(staff));
        when(repository.save(staff)).thenReturn(staff);

        var request = new UpdateStaffRequest(StaffRole.ADMIN, false);
        var result = service.update(staffId, request);

        assertThat(result.role()).isEqualTo(StaffRole.ADMIN);
        assertThat(result.active()).isFalse();
    }

    @Test
    void update_shouldThrow_whenStaffNotFound() {
        when(repository.findById(staffId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(staffId, new UpdateStaffRequest(StaffRole.ADMIN, true)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deactivate_shouldSetActiveFalse() {
        when(repository.findById(staffId)).thenReturn(Optional.of(staff));
        service.deactivate(staffId);
        verify(repository).save(staff);
        assertThat(staff.isActive()).isFalse();
    }

    @Test
    void delete_shouldCallRepository_whenStaffExists() {
        when(repository.existsById(staffId)).thenReturn(true);
        service.delete(staffId);
        verify(repository).deleteById(staffId);
    }

    @Test
    void delete_shouldThrow_whenStaffNotFound() {
        when(repository.existsById(staffId)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(staffId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
