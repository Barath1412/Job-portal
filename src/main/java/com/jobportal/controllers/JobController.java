package com.jobportal.controllers;

import com.jobportal.models.Job;
import com.jobportal.repositories.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @GetMapping("/{id}")
    public Job getJobById(@PathVariable String id) {
        return jobRepository.findById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public Job updateJob(@PathVariable String id, @RequestBody Job jobDetails) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job != null) {
            job.setTitle(jobDetails.getTitle());
            job.setCompany(jobDetails.getCompany());
            job.setLocation(jobDetails.getLocation());
            job.setDescription(jobDetails.getDescription());
            job.setDeadline(jobDetails.getDeadline());
            return jobRepository.save(job);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteJob(@PathVariable String id) {
        jobRepository.deleteById(id);
    }

    @GetMapping("/search")
    public List<Job> searchJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location) {
        if (title != null && location != null) {
            return jobRepository.findByTitleContainingAndLocationContaining(title, location);
        } else if (title != null) {
            return jobRepository.findByTitleContaining(title);
        } else if (location != null) {
            return jobRepository.findByLocationContaining(location);
        }
        return jobRepository.findAll();
    }
}
