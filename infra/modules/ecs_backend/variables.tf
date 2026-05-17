variable "project"              { type = string }
variable "environment"          { type = string }
variable "subnet_ids"           { type = list(string) }
variable "security_group_id"    { type = string }
variable "target_group_arn"     { type = string }
variable "backend_image"        { type = string }
variable "db_host"              { type = string }
variable "db_name"              { type = string }
variable "aws_region"           { type = string }
variable "keycloak_issuer_uri"  { type = string }

variable "db_password" {
  type      = string
  sensitive = true
}

variable "desired_count" {
  type    = number
  default = 1
}
