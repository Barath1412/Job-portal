package com.jobportal.repositories;

import com.jobportal.models.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByTitleContaining(String title);
    List<Job> findByLocationContaining(String location);
    List<Job> findByTitleContainingAndLocationContaining(String title, String location);
    boolean existsByCompanyId(Long companyId);
}
