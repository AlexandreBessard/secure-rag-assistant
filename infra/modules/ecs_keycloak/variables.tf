variable "project"           { type = string }
variable "environment"       { type = string }
variable "cluster_id"        { type = string }
variable "subnet_ids"        { type = list(string) }
variable "security_group_id" { type = string }
variable "target_group_arn"  { type = string }
variable "alb_dns_name"      { type = string }
variable "admin_password" {
  type      = string
  sensitive = true
}

variable "keycloak_image" {
  description = "Keycloak image URI. Defaults to the upstream image (no realm baked in). Build keycloak/Dockerfile and push to ECR, then override this."
  type        = string
  default     = "quay.io/keycloak/keycloak:26.1.5"
}

variable "desired_count" {
  type    = number
  default = 1
}
