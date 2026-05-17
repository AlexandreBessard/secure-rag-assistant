output "service_name"   { value = aws_ecs_service.keycloak.name }
output "keycloak_url"   { value = "http://${var.alb_dns_name}:8180" }
