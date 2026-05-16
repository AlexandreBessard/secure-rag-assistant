package com.lexoft.rag.ingestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Minimal representation of the S3 event notification JSON delivered to SQS.
 * "Records" is capitalised in the S3 payload — @JsonProperty handles the mapping.
 */
public record S3EventNotification(
        @JsonProperty("Records") List<Record> records
) {
    public record Record(S3Entity s3) {}

    public record S3Entity(BucketEntity bucket, ObjectEntity object) {}

    public record BucketEntity(String name) {}

    public record ObjectEntity(String key) {}
}
