package com.staffengagement.portfolio.service;

import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.portfolio.dto.*;
import com.staffengagement.portfolio.model.PortfolioEducation;
import com.staffengagement.portfolio.model.PortfolioLink;
import com.staffengagement.portfolio.model.PortfolioProject;
import com.staffengagement.portfolio.repository.PortfolioEducationRepository;
import com.staffengagement.portfolio.repository.PortfolioLinkRepository;
import com.staffengagement.portfolio.repository.PortfolioProjectRepository;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock private PortfolioEducationRepository educationRepository;
    @Mock private PortfolioProjectRepository projectRepository;
    @Mock private PortfolioLinkRepository linkRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private SkillService skillService;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private final UUID employeeId = UUID.randomUUID();

    // ==================== Skills ====================

    @Test
    void getSkillsByEmployee_shouldReturnMappedSkills() {
        var skill = new SkillResponse(UUID.randomUUID(), employeeId, "Java", 5, 3, "ADVANCED", LocalDateTime.now());
        when(skillService.findByEmployeeId(employeeId)).thenReturn(List.of(skill));

        List<PortfolioSkillResponse> result = portfolioService.getSkillsByEmployee(employeeId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Java");
        assertThat(result.get(0).yearsExperience()).isEqualTo(5);
    }

    // ==================== Education ====================

    @Test
    void createEducation_shouldCreateAndReturn_whenEmployeeExists() {
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        var request = new CreateEducationRequest("MIT", "BSc", "CS", LocalDate.of(2020, 6, 15));

        PortfolioEducation saved = buildEducation(employeeId, "MIT", "BSc", "CS", LocalDate.of(2020, 6, 15));
        when(educationRepository.save(any(PortfolioEducation.class))).thenReturn(saved);

        EducationResponse result = portfolioService.createEducation(employeeId, request);

        assertThat(result.institution()).isEqualTo("MIT");
        assertThat(result.degree()).isEqualTo("BSc");
        verify(educationRepository).save(any(PortfolioEducation.class));
    }

    @Test
    void createEducation_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.existsById(employeeId)).thenReturn(false);
        var request = new CreateEducationRequest("MIT", "BSc", "CS", null);

        assertThatThrownBy(() -> portfolioService.createEducation(employeeId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getEducationByEmployee_shouldReturnList() {
        var edu = buildEducation(employeeId, "Harvard", "MBA", "Business", LocalDate.of(2021, 5, 1));
        when(educationRepository.findByEmployeeIdOrderByGraduationDateDesc(employeeId)).thenReturn(List.of(edu));

        List<EducationResponse> result = portfolioService.getEducationByEmployee(employeeId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).institution()).isEqualTo("Harvard");
    }

    @Test
    void updateEducation_shouldUpdate_whenExists() {
        UUID educationId = UUID.randomUUID();
        var existing = buildEducation(employeeId, "Old", "Old", null, null);
        existing.setId(educationId);
        when(educationRepository.findById(educationId)).thenReturn(Optional.of(existing));
        when(educationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new UpdateEducationRequest("New", "New", "Field", LocalDate.now());
        EducationResponse result = portfolioService.updateEducation(educationId, request);

        assertThat(result.institution()).isEqualTo("New");
        assertThat(result.degree()).isEqualTo("New");
    }

    @Test
    void updateEducation_shouldThrow_whenNotFound() {
        UUID educationId = UUID.randomUUID();
        when(educationRepository.findById(educationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.updateEducation(educationId,
                new UpdateEducationRequest("X", "Y", null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteEducation_shouldDelete_whenExists() {
        UUID educationId = UUID.randomUUID();
        var existing = buildEducation(employeeId, "MIT", "BSc", null, null);
        when(educationRepository.findById(educationId)).thenReturn(Optional.of(existing));

        portfolioService.deleteEducation(educationId);

        verify(educationRepository).delete(existing);
    }

    @Test
    void deleteEducation_shouldThrow_whenNotFound() {
        UUID educationId = UUID.randomUUID();
        when(educationRepository.findById(educationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.deleteEducation(educationId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== Projects ====================

    @Test
    void createProject_shouldCreateAndReturn_whenEmployeeExists() {
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        var request = new CreateProjectRequest("Proj", "Desc", "Dev", List.of("Java"), LocalDate.now(), null);

        var saved = buildProject(employeeId, "Proj", "Desc", "Dev", List.of("Java"), LocalDate.now(), null);
        when(projectRepository.save(any(PortfolioProject.class))).thenReturn(saved);

        ProjectResponse result = portfolioService.createProject(employeeId, request);

        assertThat(result.projectName()).isEqualTo("Proj");
        assertThat(result.technologies()).containsExactly("Java");
    }

    @Test
    void createProject_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.existsById(employeeId)).thenReturn(false);
        var request = new CreateProjectRequest("Proj", "Desc", "Dev", List.of("Java"), LocalDate.now(), null);

        assertThatThrownBy(() -> portfolioService.createProject(employeeId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getProjectsByEmployee_shouldReturnList() {
        var proj = buildProject(employeeId, "Proj", "Desc", "Dev", List.of("Java"), LocalDate.now(), null);
        when(projectRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)).thenReturn(List.of(proj));

        List<ProjectResponse> result = portfolioService.getProjectsByEmployee(employeeId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).projectName()).isEqualTo("Proj");
    }

    @Test
    void updateProject_shouldUpdate_whenExists() {
        UUID projectId = UUID.randomUUID();
        var existing = buildProject(employeeId, "Old", "Old", "Old", List.of(), LocalDate.now(), null);
        existing.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new UpdateProjectRequest("New", "NewDesc", "NewRole", List.of("Go"), LocalDate.now(), LocalDate.now().plusDays(30));
        ProjectResponse result = portfolioService.updateProject(projectId, request);

        assertThat(result.projectName()).isEqualTo("New");
        assertThat(result.technologies()).containsExactly("Go");
    }

    @Test
    void updateProject_shouldThrow_whenNotFound() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.updateProject(projectId,
                new UpdateProjectRequest("X", "Y", "Z", List.of(), LocalDate.now(), null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteProject_shouldDelete_whenExists() {
        UUID projectId = UUID.randomUUID();
        var existing = buildProject(employeeId, "Proj", null, "Dev", List.of(), LocalDate.now(), null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));

        portfolioService.deleteProject(projectId);

        verify(projectRepository).delete(existing);
    }

    @Test
    void deleteProject_shouldThrow_whenNotFound() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.deleteProject(projectId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== Links ====================

    @Test
    void createLink_shouldCreateAndReturn_whenEmployeeExists() {
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        var request = new CreateLinkRequest("https://github.com", "GitHub");

        var saved = buildLink(employeeId, "https://github.com", "GitHub");
        when(linkRepository.save(any(PortfolioLink.class))).thenReturn(saved);

        LinkResponse result = portfolioService.createLink(employeeId, request);

        assertThat(result.url()).isEqualTo("https://github.com");
        assertThat(result.label()).isEqualTo("GitHub");
    }

    @Test
    void createLink_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.existsById(employeeId)).thenReturn(false);
        var request = new CreateLinkRequest("https://github.com", "GitHub");

        assertThatThrownBy(() -> portfolioService.createLink(employeeId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getLinksByEmployee_shouldReturnList() {
        var link = buildLink(employeeId, "https://x.com", "X");
        when(linkRepository.findByEmployeeId(employeeId)).thenReturn(List.of(link));

        List<LinkResponse> result = portfolioService.getLinksByEmployee(employeeId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).url()).isEqualTo("https://x.com");
    }

    @Test
    void updateLink_shouldUpdate_whenExists() {
        UUID linkId = UUID.randomUUID();
        var existing = buildLink(employeeId, "https://old.com", "Old");
        existing.setId(linkId);
        when(linkRepository.findById(linkId)).thenReturn(Optional.of(existing));
        when(linkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new UpdateLinkRequest("https://new.com", "New");
        LinkResponse result = portfolioService.updateLink(linkId, request);

        assertThat(result.url()).isEqualTo("https://new.com");
        assertThat(result.label()).isEqualTo("New");
    }

    @Test
    void updateLink_shouldThrow_whenNotFound() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findById(linkId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.updateLink(linkId,
                new UpdateLinkRequest("https://x.com", "X")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteLink_shouldDelete_whenExists() {
        UUID linkId = UUID.randomUUID();
        var existing = buildLink(employeeId, "https://x.com", "X");
        when(linkRepository.findById(linkId)).thenReturn(Optional.of(existing));

        portfolioService.deleteLink(linkId);

        verify(linkRepository).delete(existing);
    }

    @Test
    void deleteLink_shouldThrow_whenNotFound() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findById(linkId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.deleteLink(linkId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== Full Portfolio ====================

    @Test
    void getFullPortfolio_shouldReturnAll_whenEmployeeExists() {
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(skillService.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(educationRepository.findByEmployeeIdOrderByGraduationDateDesc(employeeId)).thenReturn(List.of());
        when(projectRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)).thenReturn(List.of());
        when(linkRepository.findByEmployeeId(employeeId)).thenReturn(List.of());

        FullPortfolioResponse result = portfolioService.getFullPortfolio(employeeId);

        assertThat(result.skills()).isEmpty();
        assertThat(result.education()).isEmpty();
        assertThat(result.projects()).isEmpty();
        assertThat(result.links()).isEmpty();
    }

    @Test
    void getFullPortfolio_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        assertThatThrownBy(() -> portfolioService.getFullPortfolio(employeeId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== Helpers ====================

    private PortfolioEducation buildEducation(UUID empId, String inst, String degree, String field, LocalDate grad) {
        var edu = new PortfolioEducation();
        edu.setId(UUID.randomUUID());
        edu.setEmployeeId(empId);
        edu.setInstitution(inst);
        edu.setDegree(degree);
        edu.setFieldOfStudy(field);
        edu.setGraduationDate(grad);
        edu.setCreatedAt(LocalDateTime.now());
        return edu;
    }

    private PortfolioProject buildProject(UUID empId, String name, String desc, String role,
                                          List<String> tech, LocalDate start, LocalDate end) {
        var proj = new PortfolioProject();
        proj.setId(UUID.randomUUID());
        proj.setEmployeeId(empId);
        proj.setProjectName(name);
        proj.setDescription(desc);
        proj.setRole(role);
        proj.setTechnologies(tech);
        proj.setStartDate(start);
        proj.setEndDate(end);
        proj.setCreatedAt(LocalDateTime.now());
        return proj;
    }

    private PortfolioLink buildLink(UUID empId, String url, String label) {
        var link = new PortfolioLink();
        link.setId(UUID.randomUUID());
        link.setEmployeeId(empId);
        link.setUrl(url);
        link.setLabel(label);
        link.setCreatedAt(LocalDateTime.now());
        return link;
    }
}
