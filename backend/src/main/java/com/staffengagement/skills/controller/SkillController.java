package com.staffengagement.skills.controller;

import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.dto.SkillSearchResult;
import com.staffengagement.skills.dto.UpdateSkillRequest;
import com.staffengagement.skills.service.SkillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/skills")
@Validated
class SkillController {

    private final SkillService service;

    SkillController(SkillService service) {
        this.service = service;
    }

    @GetMapping(params = "employeeId")
    List<SkillResponse> byEmployee(@RequestParam UUID employeeId) {
        return service.findByEmployeeId(employeeId);
    }

    @GetMapping(params = "name")
    List<SkillResponse> byName(@RequestParam String name) {
        return service.findByName(name);
    }

    @GetMapping("/search")
    List<SkillSearchResult> search(
            @RequestParam @NotBlank(message = "Search query must not be blank")
            @Size(max = 100, message = "Search query must not exceed 100 characters") String query) {
        return service.search(query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SkillResponse create(@Valid @RequestBody CreateSkillRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    SkillResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSkillRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
