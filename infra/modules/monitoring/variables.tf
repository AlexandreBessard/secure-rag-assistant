variable "project"     { type = string }
variable "environment" { type = string }
variable "aws_region"  { type = string }

variable "bedrock_region" {
  description = "Region where Bedrock is invoked — that is where the metrics are published"
  type        = string
}

variable "model_id" {
  description = "Bedrock model ID used for inference (must match the ModelId dimension in CloudWatch)"
  type        = string
  default     = "eu.anthropic.claude-haiku-4-5-20251001-v1:0"
}
