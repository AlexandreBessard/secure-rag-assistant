variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "eu-west-1"
}

variable "bedrock_region" {
  description = "AWS region for Bedrock (must support the selected models)"
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

variable "tools_image" {
  description = "Docker image URI for the MCP tools sidecar (ECR)"
  type        = string
}

variable "ingestion_image" {
  description = "Docker image URI for the ingestion service (ECR)"
  type        = string
}

variable "frontend_image" {
  description = "Docker image URI for the Angular/nginx frontend (ECR)"
  type        = string
}

variable "keycloak_image" {
  description = "Keycloak image URI. Default uses the upstream image (no realm baked in). After the first apply, build keycloak/Dockerfile, push to ecr_keycloak_url, and update this variable."
  type        = string
  default     = "quay.io/keycloak/keycloak:26.1.5"
}
