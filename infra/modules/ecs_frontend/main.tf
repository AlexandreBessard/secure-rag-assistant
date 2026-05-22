locals {
  name = "${var.project}-${var.environment}"
}

# ── ECR ───────────────────────────────────────────────────────────────────────

resource "aws_ecr_repository" "frontend" {
  name                 = "${local.name}-frontend"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

# ── IAM ───────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "execution" {
  name = "${local.name}-frontend-execution"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ── Logs ──────────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "frontend" {
  name              = "/ecs/${local.name}-frontend"
  retention_in_days = 30
}

# ── Task Definition ───────────────────────────────────────────────────────────
# nginx serves the Angular SPA and reverse-proxies API calls to the backend.
# BACKEND_URL is injected at container start via docker-entrypoint.sh + envsubst.

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${local.name}-frontend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.execution.arn

  container_definitions = jsonencode([{
    name      = "frontend"
    image     = var.frontend_image
    essential = true

    portMappings = [{ containerPort = 80, protocol = "tcp" }]

    environment = [
      { name = "BACKEND_URL", value = var.backend_url },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.frontend.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "frontend"
      }
    }
  }])
}

# ── Service ───────────────────────────────────────────────────────────────────

resource "aws_ecs_service" "frontend" {
  name            = "${local.name}-frontend"
  cluster         = var.cluster_id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "frontend"
    container_port   = 80
  }

  depends_on = [aws_iam_role_policy_attachment.execution]
}
