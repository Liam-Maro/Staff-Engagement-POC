package com.staffengagement.skills;

import com.staffengagement.BaseIntegrationTest;
import com.staffengagement.config.TestSecurityConfig;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.dto.SkillSearchResult;
import com.staffengagement.skills.dto.UpdateSkillRequest;
import com.staffengagement.skills.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestSecurityConfig.class)
class SkillIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SkillRepository skillRepository;

    private UUID employeeId;

    @BeforeEach
    void setUp() {
        skillRepository.deleteAll();
        employeeRepository.deleteAll();

        Employee employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe+" + UUID.randomUUID() + "@example.com");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Developer");
        employee.setActive(true);
        employee = employeeRepository.save(employee);
        employeeId = employee.getId();
    }

    @Test
    void fullCrudLifecycle_createReadUpdateReadDeleteVerifyGone() {
        // CREATE
        CreateSkillRequest createRequest = new CreateSkillRequest(
                employeeId, "Angular", 5, 12, "Advanced"
        );
        ResponseEntity<SkillResponse> createResponse = restTemplate.postForEntity(
                "/api/skills", createRequest, SkillResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SkillResponse created = createResponse.getBody();
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.employeeId()).isEqualTo(employeeId);
        assertThat(created.name()).isEqualTo("Angular");
        assertThat(created.yearsExperience()).isEqualTo(5);
        assertThat(created.projectCount()).isEqualTo(12);
        assertThat(created.proficiency()).isEqualTo("Advanced");
        assertThat(created.createdAt()).isNotNull();

        UUID skillId = created.id();

        // READ (by employee)
        ResponseEntity<List<SkillResponse>> readResponse = restTemplate.exchange(
                "/api/skills?employeeId=" + employeeId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readResponse.getBody()).hasSize(1);
        assertThat(readResponse.getBody().get(0).id()).isEqualTo(skillId);

        // UPDATE
        UpdateSkillRequest updateRequest = new UpdateSkillRequest("React", 3, 8, "Intermediate");
        ResponseEntity<SkillResponse> updateResponse = restTemplate.exchange(
                "/api/skills/" + skillId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                SkillResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        SkillResponse updated = updateResponse.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.id()).isEqualTo(skillId);
        assertThat(updated.employeeId()).isEqualTo(employeeId);
        assertThat(updated.name()).isEqualTo("React");
        assertThat(updated.yearsExperience()).isEqualTo(3);
        assertThat(updated.projectCount()).isEqualTo(8);
        assertThat(updated.proficiency()).isEqualTo("Intermediate");

        // READ after update
        ResponseEntity<List<SkillResponse>> readAfterUpdate = restTemplate.exchange(
                "/api/skills?employeeId=" + employeeId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(readAfterUpdate.getBody()).hasSize(1);
        assertThat(readAfterUpdate.getBody().get(0).name()).isEqualTo("React");

        // DELETE
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/skills/" + skillId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // VERIFY GONE
        ResponseEntity<List<SkillResponse>> readAfterDelete = restTemplate.exchange(
                "/api/skills?employeeId=" + employeeId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(readAfterDelete.getBody()).isEmpty();
    }

    @Test
    void search_returnsRankedResults() {
        // Create multiple skills with different experience levels
        createSkill(employeeId, "Java", 10, 20, "Expert");
        createSkill(employeeId, "JavaScript", 3, 5, "Intermediate");
        createSkill(employeeId, "Java Spring", 7, 15, "Advanced");

        ResponseEntity<List<SkillSearchResult>> response = restTemplate.exchange(
                "/api/skills/search?query=Java",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<SkillSearchResult> results = response.getBody();
        assertThat(results).isNotNull();
        assertThat(results).hasSize(3);

        // Verify ranking: sorted by years desc, then projects desc
        assertThat(results.get(0).skillName()).isEqualTo("Java");
        assertThat(results.get(0).yearsExperience()).isEqualTo(10);
        assertThat(results.get(1).skillName()).isEqualTo("Java Spring");
        assertThat(results.get(1).yearsExperience()).isEqualTo(7);
        assertThat(results.get(2).skillName()).isEqualTo("JavaScript");
        assertThat(results.get(2).yearsExperience()).isEqualTo(3);

        // Verify employee data is included
        assertThat(results.get(0).employeeFirstName()).isEqualTo("John");
        assertThat(results.get(0).employeeLastName()).isEqualTo("Doe");
        assertThat(results.get(0).employeeEmail()).contains("john.doe");
    }

    @Test
    void search_withPartialMatch_returnsCorrectResults() {
        createSkill(employeeId, "Angular", 5, 10, "Advanced");
        createSkill(employeeId, "React", 3, 8, "Intermediate");
        createSkill(employeeId, "AngularJS", 2, 4, "Beginner");

        ResponseEntity<List<SkillSearchResult>> response = restTemplate.exchange(
                "/api/skills/search?query=angular",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<SkillSearchResult> results = response.getBody();
        assertThat(results).hasSize(2);

        // All results should contain "angular" case-insensitive
        assertThat(results).allSatisfy(result ->
                assertThat(result.skillName().toLowerCase()).contains("angular")
        );

        // "React" should NOT be in the results
        assertThat(results).noneMatch(r -> r.skillName().equals("React"));
    }

    @Test
    void create_withNonExistentEmployee_returnsError() {
        UUID nonExistentId = UUID.randomUUID();
        CreateSkillRequest request = new CreateSkillRequest(
                nonExistentId, "Java", 5, 10, "Advanced"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/skills", request, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Employee not found");
    }

    @Test
    void findByEmployeeId_returnsResultsOrderedByNameAscending() {
        createSkill(employeeId, "Rust", 2, 3, "Beginner");
        createSkill(employeeId, "Angular", 5, 10, "Advanced");
        createSkill(employeeId, "Java", 8, 15, "Expert");
        createSkill(employeeId, "Python", 4, 7, "Intermediate");

        ResponseEntity<List<SkillResponse>> response = restTemplate.exchange(
                "/api/skills?employeeId=" + employeeId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<SkillResponse> results = response.getBody();
        assertThat(results).hasSize(4);

        // Verify alphabetical ordering by name
        assertThat(results.get(0).name()).isEqualTo("Angular");
        assertThat(results.get(1).name()).isEqualTo("Java");
        assertThat(results.get(2).name()).isEqualTo("Python");
        assertThat(results.get(3).name()).isEqualTo("Rust");
    }

    private void createSkill(UUID empId, String name, int years, int projects, String proficiency) {
        CreateSkillRequest request = new CreateSkillRequest(empId, name, years, projects, proficiency);
        ResponseEntity<SkillResponse> response = restTemplate.postForEntity(
                "/api/skills", request, SkillResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
