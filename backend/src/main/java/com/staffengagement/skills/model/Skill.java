package com.staffengagement.skills.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "skl_skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private String name;

    private int yearsExperience;

    private int projectCount;

    @Column(nullable = false)
    private String proficiency;

    @Column(nullable = false, length = 20)
    private String source = "MANUAL";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public int getYearsExperience() { return yearsExperience; }
    public int getProjectCount() { return projectCount; }
    public String getProficiency() { return proficiency; }
    public String getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public void setName(String name) { this.name = name; }
    public void setYearsExperience(int yearsExperience) { this.yearsExperience = yearsExperience; }
    public void setProjectCount(int projectCount) { this.projectCount = projectCount; }
    public void setProficiency(String proficiency) { this.proficiency = proficiency; }
    public void setSource(String source) { this.source = source; }
}
