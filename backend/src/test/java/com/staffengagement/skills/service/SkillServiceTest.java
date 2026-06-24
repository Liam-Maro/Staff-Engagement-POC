package com.staffengagement.skills.service;

import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillSearchResult;
import com.staffengagement.skills.dto.UpdateSkillRequest;
import com.staffengagement.skills.model.Skill;
import com.staffengagement.skills.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks
    private SkillServiceImpl service;

    private UUID employeeId;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        employee = new Employee();
        employee.setId(employeeId);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john@example.com");
    }

    @Test
    void search_shouldReturnResultsRankedByYearsThenProjects() {
        Skill junior = buildSkill(employeeId, "Angular", 2, 3);
        UUID seniorId = UUID.randomUUID();
        Employee senior = new Employee();
        senior.setId(seniorId);
        senior.setFirstName("Jane");
        senior.setLastName("Smith");
        senior.setEmail("jane@example.com");
        Skill seniorSkill = buildSkill(seniorId, "Angular", 7, 10);

        when(skillRepository.findByNameContainingIgnoreCase("angular")).thenReturn(List.of(junior, seniorSkill));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(seniorId)).thenReturn(Optional.of(senior));

        List<SkillSearchResult> results = service.search("angular");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).employeeFirstName()).isEqualTo("Jane"); // 7 years first
        assertThat(results.get(1).employeeFirstName()).isEqualTo("John"); // 2 years second
    }

    @Test
    void search_shouldReturnEmpty_whenNoMatchFound() {
        when(skillRepository.findByNameContainingIgnoreCase("cobol")).thenReturn(List.of());
        List<SkillSearchResult> results = service.search("cobol");
        assertThat(results).isEmpty();
    }

    @Test
    void search_shouldSkipSkills_whenEmployeeNotFound() {
        Skill orphan = buildSkill(UUID.randomUUID(), "React", 5, 3);
        when(skillRepository.findByNameContainingIgnoreCase("react")).thenReturn(List.of(orphan));
        when(employeeRepository.findById(any())).thenReturn(Optional.empty());

        List<SkillSearchResult> results = service.search("react");
        assertThat(results).isEmpty();
    }

    @Test
    void create_shouldSaveAndReturnSkill() {
        var request = new CreateSkillRequest(employeeId, "Java", 5, 8, "Advanced");
        Skill saved = buildSkill(employeeId, "Java", 5, 8);
        when(skillRepository.save(any())).thenReturn(saved);

        var result = service.create(request);
        assertThat(result.name()).isEqualTo("Java");
        assertThat(result.yearsExperience()).isEqualTo(5);
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    void update_shouldModifyExistingSkill() {
        UUID skillId = UUID.randomUUID();
        Skill existing = buildSkill(employeeId, "Java", 3, 4);
        ReflectionTestUtils.setField(existing, "id", skillId);

        when(skillRepository.findById(skillId)).thenReturn(Optional.of(existing));
        when(skillRepository.save(existing)).thenReturn(existing);

        var request = new UpdateSkillRequest("Java", 5, 8, "Expert");
        var result = service.update(skillId, request);

        assertThat(result.yearsExperience()).isEqualTo(5);
        assertThat(result.projectCount()).isEqualTo(8);
    }

    @Test
    void update_shouldThrow_whenSkillNotFound() {
        UUID id = UUID.randomUUID();
        when(skillRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id, new UpdateSkillRequest("X", 1, 1, "Basic")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldThrow_whenSkillNotFound() {
        UUID id = UUID.randomUUID();
        when(skillRepository.existsById(id)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Skill buildSkill(UUID empId, String name, int years, int projects) {
        Skill skill = new Skill();
        skill.setEmployeeId(empId);
        skill.setName(name);
        skill.setYearsExperience(years);
        skill.setProjectCount(projects);
        skill.setProficiency(years >= 5 ? "Advanced" : "Intermediate");
        return skill;
    }
}
