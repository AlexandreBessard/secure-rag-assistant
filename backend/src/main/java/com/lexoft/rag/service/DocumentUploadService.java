package com.lexoft.rag.service;

import com.lexoft.rag.common.security.Role;
import com.lexoft.rag.common.security.RoleHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "xlsx", "pptx", "txt");

    // Magic bytes for format verification — cannot be spoofed via Content-Type header
    private static final byte[] PDF_MAGIC   = {0x25, 0x50, 0x44, 0x46};        // %PDF
    private static final byte[] OOXML_MAGIC = {0x50, 0x4B, 0x03, 0x04};        // PK zip (docx/xlsx/pptx)

    private final S3Client s3Client;
    private final String bucket;
    private final long maxFileSizeBytes;

    public DocumentUploadService(S3Client s3Client,
                                 @Value("${app.s3.bucket-name}") String bucket,
                                 @Value("${app.upload.max-file-size-bytes:20971520}") long maxFileSizeBytes) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String upload(String targetRole, String originalFilename, long fileSize, InputStream inputStream)
            throws IOException {
        validateRole(targetRole);
        validateSize(fileSize);
        String ext = validateExtension(originalFilename);

        // Read just the header bytes for magic check, then reconstruct the full stream
        byte[] header = inputStream.readNBytes(8);
        validateMagicBytes(ext, header);

        String safeFilename = sanitizeFilename(originalFilename);
        String documentId = safeFilename.contains(".")
                ? safeFilename.substring(0, safeFilename.lastIndexOf('.'))
                : safeFilename;
        String key = targetRole + "/" + UUID.randomUUID() + "/" + safeFilename;

        Map<String, String> metadata = Map.of(
                "required-role", targetRole,
                "document-id", documentId
        );

        InputStream fullStream = new SequenceInputStream(new ByteArrayInputStream(header), inputStream);
        s3Client.putObject(
                r -> r.bucket(bucket).key(key).contentLength(fileSize).metadata(metadata),
                RequestBody.fromInputStream(fullStream, fileSize)
        );

        log.info("Uploaded s3://{}/{} ({} bytes)", bucket, key, fileSize);
        return key;
    }

    private void validateRole(String targetRole) {
        boolean known = RoleHierarchy.ALL.stream().map(Role::value).anyMatch(targetRole::equals);
        if (!known) {
            throw new IllegalArgumentException("Unknown role: " + targetRole);
        }
    }

    private void validateSize(long fileSize) {
        if (fileSize > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "File exceeds the maximum allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }
    }

    private String validateExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File has no extension");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "File type not allowed: ." + ext + ". Allowed: " + ALLOWED_EXTENSIONS);
        }
        return ext;
    }

    private static void validateMagicBytes(String ext, byte[] header) {
        if (header.length < 4) {
            throw new IllegalArgumentException("File is too small to be a valid document");
        }
        byte[] expected = switch (ext) {
            case "pdf"  -> PDF_MAGIC;
            case "docx", "xlsx", "pptx" -> OOXML_MAGIC;
            default -> null; // txt — no universal magic bytes, extension check is sufficient
        };
        if (expected != null && !startsWith(header, expected)) {
            throw new IllegalArgumentException("File content does not match its declared type");
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }

    private static String sanitizeFilename(String filename) {
        // Strip any path components (e.g. ../../etc/passwd → passwd)
        String basename = Paths.get(filename).getFileName().toString();
        // Keep only safe characters
        return basename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
