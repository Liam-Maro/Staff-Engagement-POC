package com.staffengagement.portfolio.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prt_portfolio_items")
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String url;

    private LocalDate dateObtained;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PortfolioItem() {}

    public PortfolioItem(UUID employeeId, String type, String title, String description, String url, LocalDate dateObtained) {
        this.employeeId = employeeId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.url = url;
        this.dateObtained = dateObtained;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public LocalDate getDateObtained() { return dateObtained; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
