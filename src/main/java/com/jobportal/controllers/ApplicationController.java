package com.jobportal.controllers;

import com.jobportal.models.Application;
import com.jobportal.models.ApplicationStatus;
import com.jobportal.models.Job;
import com.jobportal.models.Role;
import com.jobportal.models.User;
import com.jobportal.repositories.ApplicationRepository;
import com.jobportal.repositories.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    public record CreateApplicationRequest(
            Long jobId,
            String applicantName,
            String email,
            String resumeUrl,
            String coverLetter
    ) {}

    public record StatusUpdateRequest(String status) {}

    @GetMapping("/applications")
    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    @PostMapping("/applications")
    public ResponseEntity<?> submitApplication(@RequestBody CreateApplicationRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        if (request.jobId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Job ID is required"));
        }

        Job job = jobRepository.findById(request.jobId()).orElse(null);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Job not found"));
        }

        if (applicationRepository.existsByJobIdAndApplicantId(job.getId(), currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "You have already applied for this job"));
        }

        String name = request.applicantName() != null ? request.applicantName() : currentUser.getName();
        String email = request.email() != null ? request.email() : currentUser.getEmail();

        Application application = new Application(
                job,
                currentUser,
                name,
                email,
                request.resumeUrl(),
                request.coverLetter()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(applicationRepository.save(application));
    }

    @GetMapping("/applications/me")
    public ResponseEntity<?> getMyApplications() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(applicationRepository.findByApplicantId(currentUser.getId()));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<?> getApplicationsByJobId(@PathVariable Long jobId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Job not found"));
        }

        boolean isOwner = job.getPostedBy() != null && job.getPostedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to view applications for this job"));
        }

        return ResponseEntity.ok(applicationRepository.findByJobId(jobId));
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Application application = applicationRepository.findById(id).orElse(null);
        if (application == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found"));
        }

        Job job = application.getJob();
        boolean isOwner = job != null && job.getPostedBy() != null && job.getPostedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to update status for this application"));
        }

        if (request.status() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value: " + request.status()));
        }

        application.setStatus(newStatus);
        return ResponseEntity.ok(applicationRepository.save(application));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<?> getApplicationById(@PathVariable Long id) {
        Application application = applicationRepository.findById(id).orElse(null);
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(application);
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<?> deleteApplication(@PathVariable Long id) {
        if (!applicationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        applicationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Application deleted successfully"));
    }
}