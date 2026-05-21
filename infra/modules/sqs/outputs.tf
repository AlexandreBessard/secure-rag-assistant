output "queue_url"  { value = aws_sqs_queue.documents.url }
output "queue_arn"  { value = aws_sqs_queue.documents.arn }
output "queue_name" { value = aws_sqs_queue.documents.name }

output "dlq_url"  { value = aws_sqs_queue.documents_dlq.url }
output "dlq_arn"  { value = aws_sqs_queue.documents_dlq.arn }
output "dlq_name" { value = aws_sqs_queue.documents_dlq.name }
