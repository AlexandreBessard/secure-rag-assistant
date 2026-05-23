output "ecr_repository_url"       { value = aws_ecr_repository.backend.repository_url }
output "ecr_tools_repository_url" { value = aws_ecr_repository.tools.repository_url }
output "ecs_cluster_id"           { value = aws_ecs_cluster.main.id }
output "ecs_cluster_name"         { value = aws_ecs_cluster.main.name }
output "service_name"             { value = var.backend_image != "" ? aws_ecs_service.backend[0].name : "" }
