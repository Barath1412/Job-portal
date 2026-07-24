package com.jobportal.controllers;

import com.jobportal.models.Company;
import com.jobportal.models.Job;
import com.jobportal.models.Role;
import com.jobportal.models.User;
import com.jobportal.repositories.CompanyRepository;
import com.jobportal.repositories.JobRepository;
import com.jobportal.specifications.JobSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    public record JobRequest(
            Long companyId,
            String title,
            String location,
            String description,
            LocalDate deadline
    ) {}

    @GetMapping
    public ResponseEntity<?> getAllJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "deadline") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        int validPage = Math.max(0, page);
        int validSize = (size <= 0) ? 10 : Math.min(size, 50);

        String sortProperty = switch (sortBy != null ? sortBy.trim().toLowerCase() : "") {
            case "createdat", "id" -> "id";
            case "title" -> "title";
            case "location" -> "location";
            case "deadline" -> "deadline";
            default -> "deadline";
        };

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(direction, sortProperty));

        Specification<Job> spec = Specification.where(null);
        if (title != null && !title.isBlank()) {
            spec = spec.and(JobSpecification.hasTitle(title));
        }
        if (location != null && !location.isBlank()) {
            spec = spec.and(JobSpecification.hasLocation(location));
        }
        if (company != null && !company.isBlank()) {
            spec = spec.and(JobSpecification.hasCompany(company));
        }
        if (companyId != null) {
            spec = spec.and(JobSpecification.hasCompanyId(companyId));
        }

        Page<Job> jobPage = jobRepository.findAll(spec, pageable);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", jobPage.getContent());
        response.put("totalElements", jobPage.getTotalElements());
        response.put("totalPages", jobPage.getTotalPages());
        response.put("currentPage", jobPage.getNumber());
        response.put("pageSize", jobPage.getSize());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody JobRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        if (currentUser.getRole() != Role.RECRUITER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only recruiters or admins can post jobs"));
        }

        if (request.companyId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "companyId is required"));
        }

        Company company = companyRepository.findById(request.companyId()).orElse(null);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Company not found"));
        }

        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not own this company"));
        }

        Job job = new Job();
        job.setTitle(request.title());
        job.setLocation(request.location());
        job.setDescription(request.description());
        job.setDeadline(request.deadline());
        job.setCompany(company);
        job.setPostedBy(currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(jobRepository.save(job));
    }

    @GetMapping("/{id}")
    public Job getJobById(@PathVariable Long id) {
        return jobRepository.findById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody JobRequest request) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        boolean isOwner = job.getPostedBy() != null && job.getPostedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to edit this job"));
        }

        if (request.companyId() != null) {
            Company company = companyRepository.findById(request.companyId()).orElse(null);
            if (company == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Company not found"));
            }
            boolean ownsCompany = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());
            if (!ownsCompany && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not own this company"));
            }
            job.setCompany(company);
        }

        if (request.title() != null) job.setTitle(request.title());
        if (request.location() != null) job.setLocation(request.location());
        if (request.description() != null) job.setDescription(request.description());
        if (request.deadline() != null) job.setDeadline(request.deadline());

        return ResponseEntity.ok(jobRepository.save(job));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        boolean isOwner = job.getPostedBy() != null && job.getPostedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to delete this job"));
        }

        jobRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));
    }
}
