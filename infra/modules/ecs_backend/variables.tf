variable "project"             { type = string }
variable "environment"         { type = string }
variable "subnet_ids"          { type = list(string) }
variable "security_group_id"   { type = string }
variable "target_group_arn"    { type = string }
variable "backend_image" {
  type    = string
  default = ""
}
variable "tools_image" {
  type    = string
  default = ""
}
variable "db_host"             { type = string }
variable "db_name"             { type = string }
variable "aws_region"          { type = string }
variable "bedrock_region"      { type = string }
variable "s3_bucket_name"      { type = string }
variable "keycloak_issuer_uri" { type = string }
variable "alb_dns_name"       { type = string }

variable "db_password" {
  type      = string
  sensitive = true
}

variable "desired_count" {
  type    = number
  default = 1
}
