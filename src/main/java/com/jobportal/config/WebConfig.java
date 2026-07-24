package com.jobportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path resumesDir = Paths.get("uploads/resumes").toAbsolutePath().normalize();
        String resumesPath = resumesDir.toUri().toString();
        if (!resumesPath.endsWith("/")) resumesPath += "/";

        Path logosDir = Paths.get("uploads/logos").toAbsolutePath().normalize();
        String logosPath = logosDir.toUri().toString();
        if (!logosPath.endsWith("/")) logosPath += "/";

        registry.addResourceHandler("/uploads/resumes/**")
                .addResourceLocations(resumesPath);

        registry.addResourceHandler("/uploads/logos/**")
                .addResourceLocations(logosPath);
    }
}
