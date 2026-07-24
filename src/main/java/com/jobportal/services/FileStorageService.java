package com.jobportal.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, FileCategory category);
    
    default String store(MultipartFile file) {
        return store(file, FileCategory.RESUME);
    }
}
