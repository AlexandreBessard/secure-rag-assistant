output "backend_url" {
  description = "Spring Boot backend base URL"
  value       = "http://${module.networking.alb_dns_name}"
}

output "keycloak_url" {
  description = "Keycloak admin console URL"
  value       = "http://${module.networking.alb_dns_name}:8180"
}

output "document_bucket_name" {
  description = "S3 bucket — upload documents here under raw/<role>/filename"
  value       = module.s3.bucket_id
}

output "sqs_queue_name" {
  description = "SQS queue consumed by the ingestion service (set as SQS_QUEUE_NAME env var)"
  value       = module.sqs.queue_name
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint (private)"
  value       = module.rds.db_host
  sensitive   = true
}

output "ecr_repository_url" {
  description = "ECR repository URL — push the backend Docker image here"
  value       = module.ecs_backend.ecr_repository_url
}
