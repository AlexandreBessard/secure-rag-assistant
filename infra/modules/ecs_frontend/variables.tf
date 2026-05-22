variable "project"           { type = string }
variable "environment"       { type = string }
variable "cluster_id"        { type = string }
variable "subnet_ids"        { type = list(string) }
variable "security_group_id" { type = string }
variable "target_group_arn"  { type = string }
variable "frontend_image"    { type = string }
variable "backend_url"       { type = string }
variable "aws_region"        { type = string }

variable "desired_count" {
  type    = number
  default = 1
}
