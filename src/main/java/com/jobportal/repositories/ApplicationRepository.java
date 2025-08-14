package com.jobportal.repositories;

import com.jobportal.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    // Custom query methods can be added here
    // Example: List<Application> findByJobId(Long jobId);
}
