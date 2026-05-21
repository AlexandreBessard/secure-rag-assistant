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
  description = "SQS queue consumed by the ingestion service"
  value       = module.sqs.queue_name
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint (private)"
  value       = module.rds.db_host
  sensitive   = true
}

# ── ECR push targets (use these after the first apply) ────────────────────────

output "ecr_backend_url" {
  description = "ECR URL — push the backend image here"
  value       = module.ecs_backend.ecr_repository_url
}

output "ecr_tools_url" {
  description = "ECR URL — push the tools image here"
  value       = module.ecs_backend.ecr_tools_repository_url
}

output "ecr_ingestion_url" {
  description = "ECR URL — push the ingestion image here"
  value       = module.ecs_ingestion.ecr_repository_url
}

output "ecr_keycloak_url" {
  description = "ECR URL — push the realm-baked Keycloak image here, then update keycloak_image in tfvars"
  value       = module.ecs_keycloak.ecr_repository_url
}
