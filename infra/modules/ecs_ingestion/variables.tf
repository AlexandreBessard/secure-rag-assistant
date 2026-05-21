variable "project"             { type = string }
variable "environment"         { type = string }
variable "cluster_id"          { type = string }
variable "subnet_ids"          { type = list(string) }
variable "security_group_id"   { type = string }
variable "ingestion_image"     { type = string }
variable "db_host"             { type = string }
variable "db_name"             { type = string }
variable "aws_region"          { type = string }
variable "bedrock_region"      { type = string }
variable "sqs_queue_name"      { type = string }
variable "sqs_queue_arn"       { type = string }
variable "document_bucket_arn" { type = string }

variable "db_password" {
  type      = string
  sensitive = true
}

variable "desired_count" {
  type    = number
  default = 1
}
