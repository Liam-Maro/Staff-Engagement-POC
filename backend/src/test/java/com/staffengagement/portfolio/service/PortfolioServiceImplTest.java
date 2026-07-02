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

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private PortfolioEducationRepository educationRepository;

    @Mock
    private PortfolioProjectRepository projectRepository;

    @Mock
    private PortfolioLinkRepository linkRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SkillService skillService;

    @InjectMocks
    private PortfolioServiceImpl service;

    // --- Helper methods ---

    private PortfolioEducation createEducationEntity(UUID id, UUID employeeId) {
        var education = new PortfolioEducation();
        education.setId(id);
        education.setEmployeeId(employeeId);
        education.setInstitution("MIT");
        education.setDegree("BSc");
        education.setFieldOfStudy("Computer Science");
        education.setGraduationDate(LocalDate.of(2020, 6, 15));
        education.setCreatedAt(LocalDateTime.now());
        return education;
    }

    private PortfolioProject createProjectEntity(UUID id, UUID employeeId) {
        var project = new PortfolioProject();
        project.setId(id);
        project.setEmployeeId(employeeId);
        project.setProjectName("Staff Engagement");
        project.setDescription("A POC project");
        project.setRole("Developer");
        project.setTechnologies(List.of("Java", "Angular"));
        project.setStartDate(LocalDate.of(2023, 1, 1));
        project.setEndDate(LocalDate.of(2023, 12, 31));
        project.setCreatedAt(LocalDateTime.now());
        return project;
    }

    private PortfolioLink createLinkEntity(UUID id, UUID employeeId) {
        var link = new PortfolioLink();
        link.setId(id);
        link.setEmployeeId(employeeId);
        link.setUrl("https://github.com/johndoe");
        link.setLabel("GitHub");
        link.setCreatedAt(LocalDateTime.now());
        return link;
    }

    // ==================== Skills ====================

    @Test
    void getSkillsByEmployee_mapsSkillResponsesToPortfolioSkillResponses() {
        UUID employeeId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        var skillResponse = new SkillResponse(skillId, employeeId, "Java", 5, 3, "Advanced", createdAt);
        when(skillService.findByEmployeeId(employeeId)).thenReturn(List.of(skillResponse));

        List<PortfolioSkillResponse> result = service.getSkillsByEmployee(employeeId);

        assertThat(result).hasSize(1);
        PortfolioSkillResponse mapped = result.get(0);
        assertThat(mapped.id()).isEqualTo(skillId);
        assertThat(mapped.employeeId()).isEqualTo(employeeId);
        assertThat(mapped.name()).isEqualTo("Java");
        assertThat(mapped.yearsExperience()).isEqualTo(5);
        assertThat(mapped.projectCount()).isEqualTo(3);
        assertThat(mapped.proficiency()).isEqualTo("Advanced");
        assertThat(mapped.createdAt()).isEqualTo(createdAt);
        verify(skillService).findByEmployeeId(employeeId);
    }

    @Test
    void getSkillsByEmployee_returnsEmptyListWhenNoSkills() {
        UUID employeeId = UUID.randomUUID();
        when(skillService.findByEmployeeId(employeeId)).thenReturn(List.of());

        List<PortfolioSkillResponse> result = service.getSkillsByEmployee(employeeId);

        assertThat(result).isEmpty();
    }

    // ==================== Education ====================

    @Test
    void createEducation_happyPath_savesAndReturnsResponse() {
        UUID employeeId = UUID.randomUUID();
        UUID educationId = UUID.randomUUID();
        var request = new CreateEducationRequest("MIT", "BSc", "Computer Science", LocalDate.of(2020, 6, 15));

        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        var savedEntity = createEducationEntity(educationId, employeeId);
        when(educationRepository.save(any(PortfolioEducation.class))).thenReturn(savedEntity);

        EducationResponse response = service.createEducation(employeeId, request);

        assertThat(response.id()).isEqualTo(educationId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.institution()).isEqualTo("MIT");
        assertThat(response.degree()).isEqualTo("BSc");
        assertThat(response.fieldOfStudy()).isEqualTo("Computer Science");
        assertThat(response.graduationDate()).isEqualTo(LocalDate.of(2020, 6, 15));
        verify(educationRepository).save(any(PortfolioEducation.class));
    }

    @Test
    void createEducation_shouldSetAllFieldsOnEntity() {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateEducationRequest("Harvard", "PhD", "Physics", LocalDate.of(2022, 5, 20));

        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        var savedEntity = createEducationEntity(UUID.randomUUID(), employeeId);
        when(educationRepository.save(any(PortfolioEducation.class))).thenReturn(savedEntity);

        service.createEducation(employeeId, request);

        ArgumentCaptor<PortfolioEducation> captor = ArgumentCaptor.forClass(PortfolioEducation.class);
        verify(educationRepository).save(captor.capture());

        PortfolioEducation captured = captor.getValue();
        assertThat(captured.getEmployeeId()).isEqualTo(employeeId);
        assertThat(captured.getInstitution()).isEqualTo("Harvard");
        assertThat(captured.getDegree()).isEqualTo("PhD");
        assertThat(captured.getFieldOfStudy()).isEqualTo("Physics");
        assertThat(captured.getGraduationDate()).isEqualTo(LocalDate.of(2022, 5, 20));
    }

    @Test
    void createEducation_employeeNotFound_throwsEntityNotFoundException() {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateEducationRequest("MIT", "BSc", "CS", LocalDate.of(2020, 6, 15));

        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        assertThatThrownBy(() -> service.createEducation(employeeId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(educationRepository, never()).save(any());
    }

    @Test
    void getEducationByEmployee_returnsOrderedList() {
        UUID employeeId = UUID.randomUUID();
        var ed1 = createEducationEntity(UUID.randomUUID(), employeeId);
        var ed2 = createEducationEntity(UUID.randomUUID(), employeeId);
        ed2.setInstitution("Stanford");

        when(educationRepository.findByEmployeeIdOrderByGraduationDateDesc(employeeId))
                .thenReturn(List.of(ed1, ed2));

        List<EducationResponse> result = service.getEducationByEmployee(employeeId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).institution()).isEqualTo("MIT");
        assertThat(result.get(1).institution()).isEqualTo("Stanford");
        verify(educationRepository).findByEmployeeIdOrderByGraduationDateDesc(employeeId);
    }

    @Test
    void updateEducation_happyPath_updatesAndReturnsResponse() {
        UUID educationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var existing = createEducationEntity(educationId, employeeId);
        var request = new UpdateEducationRequest("Harvard", "MSc", "AI", LocalDate.of(2022, 5, 20));

        when(educationRepository.findById(educationId)).thenReturn(Optional.of(existing));
        when(educationRepository.save(any(PortfolioEducation.class))).thenAnswer(inv -> inv.getArgument(0));

        EducationResponse response = service.updateEducation(educationId, request);

        assertThat(response.institution()).isEqualTo("Harvard");
        assertThat(response.degree()).isEqualTo("MSc");
        assertThat(response.fieldOfStudy()).isEqualTo("AI");
        assertThat(response.graduationDate()).isEqualTo(LocalDate.of(2022, 5, 20));
    }

    @Test
    void updateEducation_notFound_throwsEntityNotFoundException() {
        UUID educationId = UUID.randomUUID();
        var request = new UpdateEducationRequest("Harvard", "MSc", "AI", LocalDate.of(2022, 5, 20));

        when(educationRepository.findById(educationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEducation(educationId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Education not found");
    }

    @Test
    void deleteEducation_happyPath_deletesEntity() {
        UUID educationId = UUID.randomUUID();
        var existing = createEducationEntity(educationId, UUID.randomUUID());

        when(educationRepository.findById(educationId)).thenReturn(Optional.of(existing));

        service.deleteEducation(educationId);

        verify(educationRepository).delete(existing);
    }

    @Test
    void deleteEducation_notFound_throwsEntityNotFoundException() {
        UUID educationId = UUID.randomUUID();

        when(educationRepository.findById(educationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEducation(educationId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Education not found");
    }

    // ==================== Projects ====================

    @Test
    void createProject_happyPath_savesAndReturnsResponse() {
        UUID employeeId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        var request = new CreateProjectRequest("Staff Engagement", "A POC", "Developer",
                List.of("Java", "Angular"), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        var savedEntity = createProjectEntity(projectId, employeeId);
        when(projectRepository.save(any(PortfolioProject.class))).thenReturn(savedEntity);

        ProjectResponse response = service.createProject(employeeId, request);

        assertThat(response.id()).isEqualTo(projectId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.projectName()).isEqualTo("Staff Engagement");
        assertThat(response.role()).isEqualTo("Developer");
        assertThat(response.technologies()).containsExactly("Java", "Angular");
        verify(projectRepository).save(any(PortfolioProject.class));
    }

    @Test
    void createProject_shouldSetAllFieldsOnEntity() {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateProjectRequest("My Project", "Desc here", "Architect",
                List.of("Kotlin", "React"), LocalDate.of(2024, 3, 1), LocalDate.of(2024, 9, 30));

        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        var savedEntity = createProjectEntity(UUID.randomUUID(), employeeId);
        when(projectRepository.save(any(PortfolioProject.class))).thenReturn(savedEntity);

        service.createProject(employeeId, request);

        ArgumentCaptor<PortfolioProject> captor = ArgumentCaptor.forClass(PortfolioProject.class);
        verify(projectRepository).save(captor.capture());

        PortfolioProject captured = captor.getValue();
        assertThat(captured.getEmployeeId()).isEqualTo(employeeId);
        assertThat(captured.getProjectName()).isEqualTo("My Project");
        assertThat(captured.getDescription()).isEqualTo("Desc here");
        assertThat(captured.getRole()).isEqualTo("Architect");
        assertThat(captured.getTechnologies()).containsExactly("Kotlin", "React");
        assertThat(captured.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(captured.getEndDate()).isEqualTo(LocalDate.of(2024, 9, 30));
    }

    @Test
    void createProject_employeeNotFound_throwsEntityNotFoundException() {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateProjectRequest("Project", "Desc", "Dev",
                List.of("Java"), LocalDate.of(2023, 1, 1), null);

        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        assertThatThrownBy(() -> service.createProject(employeeId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(projectRepository, never()).save(any());
    }

    @Test
    void getProjectsByEmployee_returnsOrderedList() {
        UUID employeeId = UUID.randomUUID();
        var p1 = createProjectEntity(UUID.randomUUID(), employeeId);
        var p2 = createProjectEntity(UUID.randomUUID(), employeeId);
        p2.setProjectName("Another Project");

        when(projectRepository.findByEmployeeIdOrderByStartDateDesc(employeeId))
                .thenReturn(List.of(p1, p2));

        List<ProjectResponse> result = service.getProjectsByEmployee(employeeId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).projectName()).isEqualTo("Staff Engagement");
        assertThat(result.get(1).projectName()).isEqualTo("Another Project");
        verify(projectRepository).findByEmployeeIdOrderByStartDateDesc(employeeId);
    }

    @Test
    void updateProject_happyPath_updatesAndReturnsResponse() {
        UUID projectId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var existing = createProjectEntity(projectId, employeeId);
        var request = new UpdateProjectRequest("Updated Project", "New desc", "Lead",
                List.of("Python"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(PortfolioProject.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse response = service.updateProject(projectId, request);

        assertThat(response.projectName()).isEqualTo("Updated Project");
        assertThat(response.description()).isEqualTo("New desc");
        assertThat(response.role()).isEqualTo("Lead");
        assertThat(response.technologies()).containsExactly("Python");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2024, 6, 30));
    }

    @Test
    void updateProject_notFound_throwsEntityNotFoundException() {
        UUID projectId = UUID.randomUUID();
        var request = new UpdateProjectRequest("Project", "Desc", "Dev",
                List.of("Java"), LocalDate.of(2023, 1, 1), null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProject(projectId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void deleteProject_happyPath_deletesEntity() {
        UUID projectId = UUID.randomUUID();
        var existing = createProjectEntity(projectId, UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existing));

        service.deleteProject(projectId);

        verify(projectRepository).delete(existing);
    }

    @Test
    void deleteProject_notFound_throwsEntityNotFoundException() {
        UUID projectId = UUID.randomUUID();

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteProject(projectId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    // ==================== Links ====================

    @Test
    void createLink_happyPath_savesAndReturnsResponse() {
        UUID employeeId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        var request = new CreateLinkRequest("https://github.com/johndoe", "GitHub");

        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        var savedEntity = createLinkEntity(linkId, employeeId);
        when(linkRepository.save(any(PortfolioLink.class))).thenReturn(savedEntity);

        LinkResponse response = service.createLink(employeeId, request);

        assertThat(response.id()).isEqualTo(linkId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.url()).isEqualTo("https://github.com/johndoe");
        assertThat(response.label()).isEqualTo("GitHub");
        verify(linkRepository).save(any(PortfolioLink.class));
    }

    @Test
    void createLink_shouldSetAllFieldsOnEntity() {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateLinkRequest("https://linkedin.com/in/test", "LinkedIn");

        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        var savedEntity = createLinkEntity(UUID.randomUUID(), employeeId);
        when(linkRepository.save(any(PortfolioLink.class))).thenReturn(savedEntity);

        service.createLink(employeeId, request);

        ArgumentCaptor<PortfolioLink> captor = ArgumentCaptor.forClass(PortfolioLink.class);
        verify(linkRepository).save(captor.capture());

        PortfolioLink captured = captor.getValue();
        assertThat(captured.getEmployeeId()).isEqualTo(employeeId);
        assertThat(captured.getUrl()).isEqualTo("https://linkedin.com/in/test");
        assertThat(captured.getLabel()).isEqualTo("LinkedIn");
    }

    @Test
    void createLink_employeeNotFound_throwsEntityNotFoundException() {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateLinkRequest("https://example.com", "My Site");

        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        assertThatThrownBy(() -> service.createLink(employeeId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(linkRepository, never()).save(any());
    }

    @Test
    void getLinksByEmployee_returnsList() {
        UUID employeeId = UUID.randomUUID();
        var link1 = createLinkEntity(UUID.randomUUID(), employeeId);
        var link2 = createLinkEntity(UUID.randomUUID(), employeeId);
        link2.setLabel("LinkedIn");
        link2.setUrl("https://linkedin.com/in/johndoe");

        when(linkRepository.findByEmployeeId(employeeId)).thenReturn(List.of(link1, link2));

        List<LinkResponse> result = service.getLinksByEmployee(employeeId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).label()).isEqualTo("GitHub");
        assertThat(result.get(1).label()).isEqualTo("LinkedIn");
        verify(linkRepository).findByEmployeeId(employeeId);
    }

    @Test
    void updateLink_happyPath_updatesAndReturnsResponse() {
        UUID linkId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var existing = createLinkEntity(linkId, employeeId);
        var request = new UpdateLinkRequest("https://linkedin.com/in/janedoe", "LinkedIn");

        when(linkRepository.findById(linkId)).thenReturn(Optional.of(existing));
        when(linkRepository.save(any(PortfolioLink.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkResponse response = service.updateLink(linkId, request);

        assertThat(response.url()).isEqualTo("https://linkedin.com/in/janedoe");
        assertThat(response.label()).isEqualTo("LinkedIn");
    }

    @Test
    void updateLink_notFound_throwsEntityNotFoundException() {
        UUID linkId = UUID.randomUUID();
        var request = new UpdateLinkRequest("https://example.com", "Site");

        when(linkRepository.findById(linkId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLink(linkId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Link not found");
    }

    @Test
    void deleteLink_happyPath_deletesEntity() {
        UUID linkId = UUID.randomUUID();
        var existing = createLinkEntity(linkId, UUID.randomUUID());

        when(linkRepository.findById(linkId)).thenReturn(Optional.of(existing));

        service.deleteLink(linkId);

        verify(linkRepository).delete(existing);
    }

    @Test
    void deleteLink_notFound_throwsEntityNotFoundException() {
        UUID linkId = UUID.randomUUID();

        when(linkRepository.findById(linkId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteLink(linkId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Link not found");
    }

    // ==================== Full Portfolio ====================

    @Test
    void getFullPortfolio_aggregatesAllSections() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        var skillResponse = new SkillResponse(UUID.randomUUID(), employeeId, "Java", 5, 3, "Advanced", now);
        when(skillService.findByEmployeeId(employeeId)).thenReturn(List.of(skillResponse));

        var education = createEducationEntity(UUID.randomUUID(), employeeId);
        when(educationRepository.findByEmployeeIdOrderByGraduationDateDesc(employeeId))
                .thenReturn(List.of(education));

        var project = createProjectEntity(UUID.randomUUID(), employeeId);
        when(projectRepository.findByEmployeeIdOrderByStartDateDesc(employeeId))
                .thenReturn(List.of(project));

        var link = createLinkEntity(UUID.randomUUID(), employeeId);
        when(linkRepository.findByEmployeeId(employeeId)).thenReturn(List.of(link));

        FullPortfolioResponse response = service.getFullPortfolio(employeeId);

        assertThat(response.skills()).hasSize(1);
        assertThat(response.skills().get(0).name()).isEqualTo("Java");
        assertThat(response.education()).hasSize(1);
        assertThat(response.education().get(0).institution()).isEqualTo("MIT");
        assertThat(response.projects()).hasSize(1);
        assertThat(response.projects().get(0).projectName()).isEqualTo("Staff Engagement");
        assertThat(response.links()).hasSize(1);
        assertThat(response.links().get(0).label()).isEqualTo("GitHub");
    }

    @Test
    void getFullPortfolio_employeeNotFound_throwsEntityNotFoundException() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        assertThatThrownBy(() -> service.getFullPortfolio(employeeId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }
}
