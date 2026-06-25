package com.staffengagement.portfolio.service;

import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.portfolio.dto.CreateEducationRequest;
import com.staffengagement.portfolio.dto.CreateLinkRequest;
import com.staffengagement.portfolio.dto.CreateProjectRequest;
import com.staffengagement.portfolio.dto.EducationResponse;
import com.staffengagement.portfolio.dto.FullPortfolioResponse;
import com.staffengagement.portfolio.dto.LinkResponse;
import com.staffengagement.portfolio.dto.PortfolioSkillResponse;
import com.staffengagement.portfolio.dto.ProjectResponse;
import com.staffengagement.portfolio.dto.UpdateEducationRequest;
import com.staffengagement.portfolio.dto.UpdateLinkRequest;
import com.staffengagement.portfolio.dto.UpdateProjectRequest;
import com.staffengagement.portfolio.model.PortfolioEducation;
import com.staffengagement.portfolio.model.PortfolioLink;
import com.staffengagement.portfolio.model.PortfolioProject;
import com.staffengagement.portfolio.repository.PortfolioEducationRepository;
import com.staffengagement.portfolio.repository.PortfolioLinkRepository;
import com.staffengagement.portfolio.repository.PortfolioProjectRepository;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.service.SkillService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioEducationRepository educationRepository;
    private final PortfolioProjectRepository projectRepository;
    private final PortfolioLinkRepository linkRepository;
    private final EmployeeRepository employeeRepository;
    private final SkillService skillService;

    PortfolioServiceImpl(PortfolioEducationRepository educationRepository,
                         PortfolioProjectRepository projectRepository,
                         PortfolioLinkRepository linkRepository,
                         EmployeeRepository employeeRepository,
                         SkillService skillService) {
        this.educationRepository = educationRepository;
        this.projectRepository = projectRepository;
        this.linkRepository = linkRepository;
        this.employeeRepository = employeeRepository;
        this.skillService = skillService;
    }

    // --- Skills (read-only, sourced from Skills Register) ---

    @Override
    public List<PortfolioSkillResponse> getSkillsByEmployee(UUID employeeId) {
        return skillService.findByEmployeeId(employeeId).stream()
                .map(this::toPortfolioSkillResponse)
                .toList();
    }

    // --- Education ---

    @Override
    public EducationResponse createEducation(UUID employeeId, CreateEducationRequest request) {
        validateEmployeeExists(employeeId);

        PortfolioEducation education = new PortfolioEducation();
        education.setEmployeeId(employeeId);
        education.setInstitution(request.institution());
        education.setDegree(request.degree());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setGraduationDate(request.graduationDate());

        PortfolioEducation saved = educationRepository.save(education);
        return toEducationResponse(saved);
    }

    @Override
    public List<EducationResponse> getEducationByEmployee(UUID employeeId) {
        return educationRepository.findByEmployeeIdOrderByGraduationDateDesc(employeeId).stream()
                .map(this::toEducationResponse)
                .toList();
    }

    @Override
    public EducationResponse updateEducation(UUID educationId, UpdateEducationRequest request) {
        PortfolioEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new EntityNotFoundException("Education not found: " + educationId));

        education.setInstitution(request.institution());
        education.setDegree(request.degree());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setGraduationDate(request.graduationDate());

        PortfolioEducation saved = educationRepository.save(education);
        return toEducationResponse(saved);
    }

    @Override
    public void deleteEducation(UUID educationId) {
        PortfolioEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new EntityNotFoundException("Education not found: " + educationId));
        educationRepository.delete(education);
    }

    // --- Projects ---

    @Override
    public ProjectResponse createProject(UUID employeeId, CreateProjectRequest request) {
        validateEmployeeExists(employeeId);

        PortfolioProject project = new PortfolioProject();
        project.setEmployeeId(employeeId);
        project.setProjectName(request.projectName());
        project.setDescription(request.description());
        project.setRole(request.role());
        project.setTechnologies(request.technologies());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        PortfolioProject saved = projectRepository.save(project);
        return toProjectResponse(saved);
    }

    @Override
    public List<ProjectResponse> getProjectsByEmployee(UUID employeeId) {
        return projectRepository.findByEmployeeIdOrderByStartDateDesc(employeeId).stream()
                .map(this::toProjectResponse)
                .toList();
    }

    @Override
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        PortfolioProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        project.setProjectName(request.projectName());
        project.setDescription(request.description());
        project.setRole(request.role());
        project.setTechnologies(request.technologies());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        PortfolioProject saved = projectRepository.save(project);
        return toProjectResponse(saved);
    }

    @Override
    public void deleteProject(UUID projectId) {
        PortfolioProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
        projectRepository.delete(project);
    }

    // --- Links ---

    @Override
    public LinkResponse createLink(UUID employeeId, CreateLinkRequest request) {
        validateEmployeeExists(employeeId);

        PortfolioLink link = new PortfolioLink();
        link.setEmployeeId(employeeId);
        link.setUrl(request.url());
        link.setLabel(request.label());

        PortfolioLink saved = linkRepository.save(link);
        return toLinkResponse(saved);
    }

    @Override
    public List<LinkResponse> getLinksByEmployee(UUID employeeId) {
        return linkRepository.findByEmployeeId(employeeId).stream()
                .map(this::toLinkResponse)
                .toList();
    }

    @Override
    public LinkResponse updateLink(UUID linkId, UpdateLinkRequest request) {
        PortfolioLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Link not found: " + linkId));

        link.setUrl(request.url());
        link.setLabel(request.label());

        PortfolioLink saved = linkRepository.save(link);
        return toLinkResponse(saved);
    }

    @Override
    public void deleteLink(UUID linkId) {
        PortfolioLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Link not found: " + linkId));
        linkRepository.delete(link);
    }

    // --- Full Portfolio ---

    @Override
    public FullPortfolioResponse getFullPortfolio(UUID employeeId) {
        validateEmployeeExists(employeeId);

        List<PortfolioSkillResponse> skills = getSkillsByEmployee(employeeId);
        List<EducationResponse> education = getEducationByEmployee(employeeId);
        List<ProjectResponse> projects = getProjectsByEmployee(employeeId);
        List<LinkResponse> links = getLinksByEmployee(employeeId);

        return new FullPortfolioResponse(skills, education, projects, links);
    }

    // --- Private helpers ---

    private void validateEmployeeExists(UUID employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EntityNotFoundException("Employee not found: " + employeeId);
        }
    }

    private PortfolioSkillResponse toPortfolioSkillResponse(SkillResponse skill) {
        return new PortfolioSkillResponse(
                skill.id(),
                skill.employeeId(),
                skill.name(),
                skill.yearsExperience(),
                skill.projectCount(),
                skill.proficiency(),
                skill.createdAt()
        );
    }

    private EducationResponse toEducationResponse(PortfolioEducation education) {
        return new EducationResponse(
                education.getId(),
                education.getEmployeeId(),
                education.getInstitution(),
                education.getDegree(),
                education.getFieldOfStudy(),
                education.getGraduationDate(),
                education.getCreatedAt()
        );
    }

    private ProjectResponse toProjectResponse(PortfolioProject project) {
        return new ProjectResponse(
                project.getId(),
                project.getEmployeeId(),
                project.getProjectName(),
                project.getDescription(),
                project.getRole(),
                project.getTechnologies(),
                project.getStartDate(),
                project.getEndDate(),
                project.getCreatedAt()
        );
    }

    private LinkResponse toLinkResponse(PortfolioLink link) {
        return new LinkResponse(
                link.getId(),
                link.getEmployeeId(),
                link.getUrl(),
                link.getLabel(),
                link.getCreatedAt()
        );
    }
}
