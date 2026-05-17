package com.lexoft.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Set;
import java.util.UUID;

@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);
    private static final Set<String> ALLOWED_ROLES = Set.of("executive", "hr", "manager", "employee");

    private final S3Client s3Client;
    private final String bucket;

    public DocumentUploadService(S3Client s3Client, @Value("${app.s3.bucket-name}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public String upload(String targetRole, String filename, byte[] bytes) {
        if (!ALLOWED_ROLES.contains(targetRole)) {
            throw new IllegalArgumentException("Unknown role: " + targetRole);
        }
        String key = targetRole + "/" + UUID.randomUUID() + "/" + filename;
        s3Client.putObject(
                r -> r.bucket(bucket).key(key).contentLength((long) bytes.length),
                RequestBody.fromBytes(bytes)
        );
        log.info("Uploaded s3://{}/{}", bucket, key);
        return key;
    }
}
