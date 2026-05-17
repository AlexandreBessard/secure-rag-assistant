variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-1"
}

variable "project" {
  description = "Project name — used as prefix for every resource"
  type        = string
  default     = "rag-assistant"
}

variable "environment" {
  description = "Environment (poc, staging, prod)"
  type        = string
  default     = "poc"
}

variable "db_password" {
  description = "Master password for the RDS PostgreSQL instance"
  type        = string
  sensitive   = true
}

variable "keycloak_admin_password" {
  description = "Keycloak bootstrap admin password"
  type        = string
  sensitive   = true
}

variable "backend_image" {
  description = "Docker image URI for the Spring Boot backend (ECR)"
  type        = string
}
