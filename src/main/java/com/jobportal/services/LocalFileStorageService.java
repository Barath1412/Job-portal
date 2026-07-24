package com.jobportal.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path resumesLocation = Paths.get("uploads/resumes").toAbsolutePath().normalize();
    private final Path logosLocation = Paths.get("uploads/logos").toAbsolutePath().normalize();

    public LocalFileStorageService() {
        try {
            Files.createDirectories(resumesLocation);
            Files.createDirectories(logosLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload storage locations", e);
        }
    }

    @Override
    public String store(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) contentType = "";
        contentType = contentType.toLowerCase().trim();

        Path targetLocation;
        String urlPrefix;
        String defaultExt;

        if (category == FileCategory.LOGO) {
            List<String> allowedLogoTypes = List.of("image/png", "image/jpeg", "image/jpg", "image/webp");
            if (!allowedLogoTypes.contains(contentType)) {
                throw new IllegalArgumentException("Only image files (PNG, JPEG, WEBP) are allowed for company logos");
            }
            targetLocation = this.logosLocation;
            urlPrefix = "/uploads/logos/";
            defaultExt = getFileExtension(file.getOriginalFilename(), ".png");
        } else {
            // Default to RESUME
            if (!"application/pdf".equalsIgnoreCase(contentType)) {
                throw new IllegalArgumentException("Only PDF files are allowed (.pdf)");
            }
            targetLocation = this.resumesLocation;
            urlPrefix = "/uploads/resumes/";
            defaultExt = ".pdf";
        }

        try {
            String filename = UUID.randomUUID().toString() + defaultExt;
            Path destinationFile = targetLocation.resolve(filename).normalize();

            if (!destinationFile.getParent().equals(targetLocation)) {
                throw new IllegalArgumentException("Cannot store file outside current directory");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return urlPrefix + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private String getFileExtension(String originalFilename, String fallback) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ext.length() <= 5) return ext;
        }
        return fallback;
    }
}
