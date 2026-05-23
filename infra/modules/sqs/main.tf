locals {
  name = "${var.project}-${var.environment}"
}

# ── Dead-letter queue — receives messages after max_receive_count failed attempts ─

resource "aws_sqs_queue" "documents_dlq" {
  name                      = "${local.name}-document-queue-dlq"
  message_retention_seconds = 1209600  # 14 days — enough time to inspect and replay

  tags = { Name = "${local.name}-document-queue-dlq" }
}

# ── Main queue ─────────────────────────────────────────────────────────────────

resource "aws_sqs_queue" "documents" {
  name                       = "${local.name}-document-queue"
  visibility_timeout_seconds = 300  # matches ingestion processing time
  message_retention_seconds  = 86400  # 1 day

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.documents_dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "${local.name}-document-queue" }
}

# ── Queue policy — allow the S3 bucket to publish ObjectCreated events ────────

resource "aws_sqs_queue_policy" "documents" {
  queue_url = aws_sqs_queue.documents.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowS3Publish"
      Effect    = "Allow"
      Principal = { Service = "s3.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.documents.arn
      Condition = {
        ArnLike = { "aws:SourceArn" = var.document_bucket_arn }
      }
    }]
  })
}

# ── S3 → SQS notification (raw/ prefix, all ObjectCreated events) ─────────────

resource "aws_s3_bucket_notification" "upload" {
  bucket = var.document_bucket_id

  queue {
    queue_arn = aws_sqs_queue.documents.arn
    events    = ["s3:ObjectCreated:*"]
  }

  depends_on = [aws_sqs_queue_policy.documents]
}
