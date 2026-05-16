package com.lexoft.rag.ingestion.controller;

import com.lexoft.rag.ingestion.service.IngestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Profile("local")
@RestController
@RequestMapping("/ingest")
public class LocalIngestController {

    private final IngestionService ingestionService;

    public LocalIngestController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> ingest(
            @RequestParam(required = false) String documentId,
            @RequestParam(defaultValue = "employee") String requiredRole,
            @RequestParam MultipartFile file
    ) throws IOException {
        String filename = file.getOriginalFilename();
        String resolvedId = (documentId != null && !documentId.isBlank())
                ? documentId
                : filename.replaceAll("\\.[^.]+$", "");
        int chunks = ingestionService.ingest(resolvedId, requiredRole, filename, file.getBytes());
        return Map.of("documentId", resolvedId, "chunks", chunks);
    }
}
