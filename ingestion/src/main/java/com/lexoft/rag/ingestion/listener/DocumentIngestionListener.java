package com.lexoft.rag.ingestion.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexoft.rag.ingestion.exception.DocumentParseException;
import com.lexoft.rag.ingestion.model.S3EventNotification;
import com.lexoft.rag.ingestion.service.IngestionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Profile("aws")
@Component
public class DocumentIngestionListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionListener.class);

    private final S3Client s3Client;
    private final IngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public DocumentIngestionListener(S3Client s3Client,
                                     IngestionService ingestionService,
                                     ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    @SqsListener("${app.sqs.queue-name}")
    public void onS3Event(String messageBody) throws IOException {
        S3EventNotification event = objectMapper.readValue(messageBody, S3EventNotification.class);

        if (event.records() == null || event.records().isEmpty()) {
            log.info("Ignoring S3 notification with no records");
            return;
        }

        for (S3EventNotification.Record record : event.records()) {
            String bucket = record.s3().bucket().name();
            // S3 URL-encodes the key in the notification (spaces → '+' or '%20')
            String key = URLDecoder.decode(record.s3().object().key(), StandardCharsets.UTF_8);
            log.info("Received S3 event: s3://{}/{}", bucket, key);

            try {
                process(bucket, key);
            } catch (DocumentParseException e) {
                // Permanent failure — retrying the same file produces the same result.
                // Log and ACK the message so it is not re-queued indefinitely.
                log.error("Permanent parse failure for s3://{}/{} — message consumed without retry: {}",
                        bucket, key, e.getMessage(), e);
            } catch (Exception e) {
                // Transient failure (S3, DB, network) — re-throw so SQS retries
                // and eventually routes to the DLQ after max attempts.
                log.error("Transient failure processing s3://{}/{} — will retry", bucket, key, e);
                throw e;
            }
        }
    }

    private void process(String bucket, String key) {
        HeadObjectResponse head = s3Client.headObject(r -> r.bucket(bucket).key(key));
        Map<String, String> metadata = head.metadata();

        String filename = key.substring(key.lastIndexOf('/') + 1);
        String documentId = metadata.getOrDefault("document-id",
                filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename);
        String requiredRole = metadata.getOrDefault("required-role", "employee");

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(
                r -> r.bucket(bucket).key(key));

        ingestionService.ingest(documentId, requiredRole, filename, objectBytes.asByteArray());
    }
}
