locals {
  name = "${var.project}-${var.environment}"
}

resource "aws_cloudwatch_dashboard" "llm_cost" {
  dashboard_name = "${local.name}-llm-cost"

  dashboard_body = templatefile("${path.module}/dashboard.tftpl", {
    model_id = var.model_id
    region   = var.bedrock_region
  })
}
