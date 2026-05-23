output "ecr_repository_url" { value = aws_ecr_repository.ingestion.repository_url }
output "service_name"       { value = var.ingestion_image != "" ? aws_ecs_service.ingestion[0].name : "" }
