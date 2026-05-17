output "ecr_repository_url" { value = aws_ecr_repository.backend.repository_url }
output "ecs_cluster_id"     { value = aws_ecs_cluster.main.id }
output "service_name"       { value = aws_ecs_service.backend.name }
