output "service_name"       { value = aws_ecs_service.keycloak.name }
output "keycloak_url"       { value = "http://${var.alb_dns_name}:8180" }
output "ecr_repository_url" { value = aws_ecr_repository.keycloak.repository_url }
