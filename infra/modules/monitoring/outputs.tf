output "dashboard_url" {
  description = "Direct link to the CloudWatch LLM cost dashboard"
  value       = "https://${var.bedrock_region}.console.aws.amazon.com/cloudwatch/home?region=${var.bedrock_region}#dashboards:name=${aws_cloudwatch_dashboard.llm_cost.dashboard_name}"
}
