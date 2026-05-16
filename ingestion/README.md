# rag-ingestion

Standalone Spring Boot service that listens for S3 upload events via SQS, extracts text from documents, chunks and embeds them using Amazon Titan Embeddings V2, and stores the result in pgvector.

## How it works

```
S3 upload (raw/<file>)
  → S3 event notification → SQS queue
  → DocumentIngestionListener
      → HeadObject  (reads document-id, required-role from S3 metadata)
      → GetObject   (downloads file bytes)
      → TikaDocumentReader   (extracts text — PDF, Word, plain text, …)
      → TokenTextSplitter    (chunks)
      → VectorStore.accept() (Titan embeds → pgvector stores)
```

## Profiles

| Profile | Entry point | Use case |
|---------|-------------|----------|
| `aws`   | `DocumentIngestionListener` — polls SQS, downloads from S3 | Deployed on ECS |
| `local` | `LocalIngestController` — `POST /ingest` multipart endpoint on port 8081 | Local development and testing |

The `IngestionService` (chunk → embed → store) is shared by both profiles.

## S3 upload convention

```bash
aws s3 cp company-policy.pdf s3://<bucket>/raw/company-policy.pdf \
  --metadata "document-id=company-policy,required-role=hr"
```

| Metadata key    | Values                                    | Default       |
|-----------------|-------------------------------------------|---------------|
| `document-id`   | stable identifier (e.g. `company-policy`) | filename stem |
| `required-role` | `employee`, `manager`, `hr`, `executive`  | `employee`    |

## Configuration

| Env var          | Default              | Description                        |
|------------------|----------------------|------------------------------------|
| `AWS_REGION`     | `eu-west-1`          | AWS region for S3, SQS and Bedrock |
| `SQS_QUEUE_NAME` | `rag-document-queue` | SQS queue receiving S3 events (`aws` profile only) |
| `DB_HOST`        | `localhost`          | pgvector host                      |
| `DB_PORT`        | `5433`               | pgvector port                      |
| `DB_USER`        | `raguser`            | pgvector user                      |
| `DB_PASSWORD`    | `ragpass`            | pgvector password                  |

AWS credentials are resolved from the standard chain: `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` env vars, `~/.aws/credentials`, or IAM role.

## Run

### Local profile

Requires Docker (pgvector) and AWS credentials with access to Bedrock (Titan embeddings).

```bash
# Start pgvector
docker compose up -d pgvector

# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Upload a document via the REST endpoint:

```bash
curl -X POST http://localhost:8081/ingest \
  -F "documentId=company-policy" \
  -F "requiredRole=hr" \
  -F "file=@/path/to/company-policy.pdf"
```

Response:

```json
{ "documentId": "company-policy", "chunks": 12 }
```

### AWS profile

Requires AWS credentials with access to SQS, S3, and Bedrock, and a `SQS_QUEUE_NAME` that receives S3 ObjectCreated notifications.

```bash
SQS_QUEUE_NAME=rag-document-queue ./mvnw spring-boot:run -Dspring-boot.run.profiles=aws
```
