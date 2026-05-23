output "ecr_repository_url" { value = aws_ecr_repository.frontend.repository_url }
output "service_name"       { value = var.frontend_image != "" ? aws_ecs_service.frontend[0].name : "" }
