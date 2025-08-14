package com.jobportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class JobApplicationPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobApplicationPortalApplication.class, args);
    }
}