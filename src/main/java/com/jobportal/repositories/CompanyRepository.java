package com.jobportal.repositories;

import com.jobportal.models.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {
    List<Company> findByOwnerId(Long ownerId);
    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
