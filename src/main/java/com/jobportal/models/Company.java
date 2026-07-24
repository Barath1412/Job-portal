package com.jobportal.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String website;
    private String industry;
    private String logoUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "hibernateLazyInitializer", "handler"})
    private User owner;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Company() {}

    public Company(String name, String description, String website, String industry, String logoUrl, User owner) {
        this.name = name;
        this.description = description;
        this.website = website;
        this.industry = industry;
        this.logoUrl = logoUrl;
        this.owner = owner;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
