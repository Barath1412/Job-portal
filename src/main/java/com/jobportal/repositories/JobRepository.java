package com.jobportal.repositories;

import com.jobportal.models.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByTitleContaining(String title);
    List<Job> findByLocationContaining(String location);
    List<Job> findByTitleContainingAndLocationContaining(String title, String location);
}
