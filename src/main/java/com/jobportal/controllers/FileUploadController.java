package com.jobportal.controllers;

import com.jobportal.services.FileCategory;
import com.jobportal.services.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/resume")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileStorageService.store(file, FileCategory.RESUME);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }

    @PostMapping("/logo")
    public ResponseEntity<?> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileStorageService.store(file, FileCategory.LOGO);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload logo"));
        }
    }
}
