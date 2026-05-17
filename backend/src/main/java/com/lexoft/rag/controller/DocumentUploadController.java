package com.lexoft.rag.controller;

import com.lexoft.rag.service.DocumentUploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
public class DocumentUploadController {

    private final DocumentUploadService uploadService;

    public DocumentUploadController(DocumentUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> upload(
            @RequestParam String targetRole,
            @RequestParam MultipartFile file) throws IOException {
        String key = uploadService.upload(targetRole, file.getOriginalFilename(), file.getBytes());
        return Map.of("key", key);
    }
}
