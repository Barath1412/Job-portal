package com.jobportal.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String companyNameLegacy;
    private String location;
    private String description;
    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    @JsonIgnoreProperties({"owner", "hibernateLazyInitializer", "handler"})
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "posted_by_id")
    @JsonIgnoreProperties({"passwordHash", "hibernateLazyInitializer", "handler"})
    private User postedBy;

    // Constructors
    public Job() {}

    public Job(String title, Company company, String location, String description, LocalDate deadline) {
        this.title = title;
        this.company = company;
        if (company != null) {
            this.companyNameLegacy = company.getName();
        }
        this.location = location;
        this.description = description;
        this.deadline = deadline;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
        if (company != null) {
            this.companyNameLegacy = company.getName();
        }
    }

    public String getCompanyNameLegacy() {
        return companyNameLegacy;
    }

    public void setCompanyNameLegacy(String companyNameLegacy) {
        this.companyNameLegacy = companyNameLegacy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public User getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(User postedBy) {
        this.postedBy = postedBy;
    }
}