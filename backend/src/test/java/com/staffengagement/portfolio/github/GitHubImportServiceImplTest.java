package com.staffengagement.portfolio.github;

import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.portfolio.model.PortfolioLink;
import com.staffengagement.portfolio.repository.PortfolioLinkRepository;
import com.staffengagement.skills.model.Skill;
import com.staffengagement.skills.repository.SkillRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubImportServiceImplTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private PortfolioLinkRepository portfolioLinkRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private GitHubImportProperties validProperties() {
        return new GitHubImportProperties(
                new GitHubImportProperties.Api("https://api.github.com", "test-pat"),
                Map.of()
        );
    }

    private GitHubImportServiceImpl createService(GitHubImportProperties properties) {
        return new GitHubImportServiceImpl(
                gitHubApiClient, skillRepository, portfolioLinkRepository,
                properties, employeeRepository
        );
    }

    @Test
    void shouldThrowGitHubNotConfiguredException_whenPatIsBlank() {
        var props = new GitHubImportProperties(
                new GitHubImportProperties.Api("https://api.github.com", "  "),
                Map.of()
        );
        var service = createService(props);

        assertThatThrownBy(() -> service.importFromGitHub(UUID.randomUUID(), "https://github.com/user"))
                .isInstanceOf(GitHubNotConfiguredException.class);
    }

    @Test
    void shouldThrowGitHubNotConfiguredException_whenBaseUrlIsBlank() {
        var props = new GitHubImportProperties(
                new GitHubImportProperties.Api("", "test-pat"),
                Map.of()
        );
        var service = createService(props);

        assertThatThrownBy(() -> service.importFromGitHub(UUID.randomUUID(), "https://github.com/user"))
                .isInstanceOf(GitHubNotConfiguredException.class);
    }

    @Test
    void shouldThrowGitHubNotConfiguredException_whenApiIsNull() {
        var props = new GitHubImportProperties(null, Map.of());
        var service = createService(props);

        assertThatThrownBy(() -> service.importFromGitHub(UUID.randomUUID(), "https://github.com/user"))
                .isInstanceOf(GitHubNotConfiguredException.class);
    }

    @Test
    void shouldThrowEntityNotFoundException_whenEmployeeNotFound() {
        var service = createService(validProperties());
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importFromGitHub(employeeId, "https://github.com/user"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(employeeId.toString());
    }

    @Test
    void shouldThrowEmployeeNotActiveException_whenEmployeeIsInactive() {
        var service = createService(validProperties());
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(false);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.importFromGitHub(employeeId, "https://github.com/user"))
                .isInstanceOf(EmployeeNotActiveException.class);
    }

    @Test
    void shouldReturnEmptySkills_whenNoReposFound() {
        var service = createService(validProperties());
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(gitHubApiClient.fetchPublicRepos("user")).thenReturn(List.of());
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB")).thenReturn(List.of());
        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenReturn(Optional.empty());
        when(portfolioLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = service.importFromGitHub(employeeId, "https://github.com/user");

        assertThat(result.skills()).isEmpty();
        assertThat(result.repositoriesAnalysed()).isZero();
    }

    @Test
    void shouldSkipReposWithEmptyLanguages() {
        var service = createService(validProperties());
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        List<GitHubRepo> repos = List.of(
                new GitHubRepo("repo1", "user/repo1", new GitHubRepo.Owner("user")),
                new GitHubRepo("repo2", "user/repo2", new GitHubRepo.Owner("user"))
        );
        when(gitHubApiClient.fetchPublicRepos("user")).thenReturn(repos);
        when(gitHubApiClient.fetchLanguages("user", "repo1")).thenReturn(Map.of());
        when(gitHubApiClient.fetchLanguages("user", "repo2")).thenReturn(Map.of("Java", 5000L));

        when(skillRepository.findByEmployeeIdAndNameAndSource(eq(employeeId), eq("Java"), eq("GITHUB")))
                .thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            if (s.getId() == null) {
                try {
                    var f = Skill.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(s, UUID.randomUUID());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            return s;
        });
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB")).thenReturn(List.of());
        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenReturn(Optional.empty());
        when(portfolioLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = service.importFromGitHub(employeeId, "https://github.com/user");

        assertThat(result.repositoriesAnalysed()).isEqualTo(1);
        assertThat(result.skills()).hasSize(1);
        assertThat(result.skills().get(0).name()).isEqualTo("Java");
    }

    @Test
    void shouldUseLanguageMapping_whenConfigured() {
        var props = new GitHubImportProperties(
                new GitHubImportProperties.Api("https://api.github.com", "test-pat"),
                Map.of("Jupyter Notebook", "Python")
        );
        var service = createService(props);
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        List<GitHubRepo> repos = List.of(
                new GitHubRepo("repo1", "user/repo1", new GitHubRepo.Owner("user"))
        );
        when(gitHubApiClient.fetchPublicRepos("user")).thenReturn(repos);
        when(gitHubApiClient.fetchLanguages("user", "repo1"))
                .thenReturn(Map.of("Jupyter Notebook", 5000L, "Python", 10000L));

        when(skillRepository.findByEmployeeIdAndNameAndSource(eq(employeeId), eq("Python"), eq("GITHUB")))
                .thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            if (s.getId() == null) {
                try {
                    var f = Skill.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(s, UUID.randomUUID());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            return s;
        });
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB")).thenReturn(List.of());
        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenReturn(Optional.empty());
        when(portfolioLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = service.importFromGitHub(employeeId, "https://github.com/user");

        // Both "Jupyter Notebook" and "Python" map to "Python", so only 1 skill
        assertThat(result.skills()).hasSize(1);
        assertThat(result.skills().get(0).name()).isEqualTo("Python");
    }

    @Test
    void shouldHandleNullLanguageMapping() {
        var props = new GitHubImportProperties(
                new GitHubImportProperties.Api("https://api.github.com", "test-pat"),
                null
        );
        var service = createService(props);
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        List<GitHubRepo> repos = List.of(
                new GitHubRepo("repo1", "user/repo1", new GitHubRepo.Owner("user"))
        );
        when(gitHubApiClient.fetchPublicRepos("user")).thenReturn(repos);
        when(gitHubApiClient.fetchLanguages("user", "repo1"))
                .thenReturn(Map.of("Go", 8000L));

        when(skillRepository.findByEmployeeIdAndNameAndSource(eq(employeeId), eq("Go"), eq("GITHUB")))
                .thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            if (s.getId() == null) {
                try {
                    var f = Skill.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(s, UUID.randomUUID());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            return s;
        });
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB")).thenReturn(List.of());
        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenReturn(Optional.empty());
        when(portfolioLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportResult result = service.importFromGitHub(employeeId, "https://github.com/user");

        assertThat(result.skills()).hasSize(1);
        assertThat(result.skills().get(0).name()).isEqualTo("Go");
    }
}
