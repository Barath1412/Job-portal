package com.jobportal.controllers;

import com.jobportal.models.Application;
import com.jobportal.repositories.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @GetMapping
    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    @PostMapping
    public Application submitApplication(@RequestBody Application application) {
        return applicationRepository.save(application);
    }

    @GetMapping("/{id}")
    public Application getApplicationById(@PathVariable String id) {
        return applicationRepository.findById(id).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void deleteApplication(@PathVariable String id) {
        applicationRepository.deleteById(id);
    }

    @GetMapping("/job/{jobId}")
    public List<Application> getApplicationsByJobId(@PathVariable String jobId) {
        return applicationRepository.findByJobId(jobId);
    }
}