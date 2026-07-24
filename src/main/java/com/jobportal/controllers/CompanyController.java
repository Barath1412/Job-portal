package com.jobportal.controllers;

import com.jobportal.models.Company;
import com.jobportal.models.Role;
import com.jobportal.models.User;
import com.jobportal.repositories.CompanyRepository;
import com.jobportal.repositories.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    public record CreateCompanyRequest(
            String name,
            String description,
            String website,
            String industry,
            String logoUrl
    ) {}

    @PostMapping
    public ResponseEntity<?> createCompany(@RequestBody CreateCompanyRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        if (currentUser.getRole() != Role.RECRUITER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only recruiters or admins can create companies"));
        }

        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Company name is required"));
        }

        Company company = new Company(
                request.name().trim(),
                request.description(),
                request.website(),
                request.industry(),
                request.logoUrl(),
                currentUser
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(companyRepository.save(company));
    }

    @GetMapping
    public ResponseEntity<?> getAllCompanies(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int validPage = Math.max(0, page);
        int validSize = (size <= 0) ? 10 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.ASC, "name"));

        Page<Company> companyPage;
        if (name != null && !name.isBlank()) {
            companyPage = companyRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
        } else {
            companyPage = companyRepository.findAll(pageable);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", companyPage.getContent());
        response.put("totalElements", companyPage.getTotalElements());
        response.put("totalPages", companyPage.getTotalPages());
        response.put("currentPage", companyPage.getNumber());
        response.put("pageSize", companyPage.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyCompanies() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        List<Company> companies;
        if (currentUser.getRole() == Role.ADMIN) {
            companies = companyRepository.findAll();
        } else {
            companies = companyRepository.findByOwnerId(currentUser.getId());
        }

        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Company not found"));
        }
        return ResponseEntity.ok(company);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @RequestBody CreateCompanyRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Company not found"));
        }

        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to edit this company"));
        }

        if (request.name() != null && !request.name().isBlank()) {
            company.setName(request.name().trim());
        }
        company.setDescription(request.description());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        if (request.logoUrl() != null) {
            company.setLogoUrl(request.logoUrl());
        }

        return ResponseEntity.ok(companyRepository.save(company));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Company not found"));
        }

        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to delete this company"));
        }

        // Policy: Block deletion with 409 Conflict if jobs reference this company
        if (jobRepository.existsByCompanyId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Cannot delete company because jobs are still associated with it"));
        }

        companyRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Company deleted successfully"));
    }
}
