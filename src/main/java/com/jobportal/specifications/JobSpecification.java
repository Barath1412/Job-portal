package com.jobportal.specifications;

import com.jobportal.models.Job;
import org.springframework.data.jpa.domain.Specification;

public class JobSpecification {

    public static Specification<Job> hasTitle(String title) {
        return (root, query, builder) -> {
            if (title == null || title.isBlank()) return null;
            return builder.like(builder.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Job> hasLocation(String location) {
        return (root, query, builder) -> {
            if (location == null || location.isBlank()) return null;
            return builder.like(builder.lower(root.get("location")), "%" + location.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Job> hasCompany(String company) {
        return (root, query, builder) -> {
            if (company == null || company.isBlank()) return null;
            String pattern = "%" + company.toLowerCase().trim() + "%";
            return builder.or(
                builder.like(builder.lower(root.get("company").get("name")), pattern),
                builder.like(builder.lower(root.get("companyNameLegacy")), pattern)
            );
        };
    }

    public static Specification<Job> hasCompanyId(Long companyId) {
        return (root, query, builder) -> {
            if (companyId == null) return null;
            return builder.equal(root.get("company").get("id"), companyId);
        };
    }
}
