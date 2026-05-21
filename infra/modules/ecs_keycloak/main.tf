locals {
  name = "${var.project}-${var.environment}"
}

# ── ECR ───────────────────────────────────────────────────────────────────────

resource "aws_ecr_repository" "keycloak" {
  name                 = "${local.name}-keycloak"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

# ── IAM ───────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "execution" {
  name = "${local.name}-keycloak-execution"
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

resource "aws_cloudwatch_log_group" "keycloak" {
  name              = "/ecs/${local.name}-keycloak"
  retention_in_days = 30
}

# ── Task Definition ───────────────────────────────────────────────────────────
# Keycloak runs in start-dev mode. The realm is imported via --import-realm.
# The rag-realm.json must be baked into a custom Docker image derived from
# quay.io/keycloak/keycloak — see keycloak/Dockerfile in the project root.

resource "aws_ecs_task_definition" "keycloak" {
  family                   = "${local.name}-keycloak"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.execution.arn

  container_definitions = jsonencode([{
    name      = "keycloak"
    image     = var.keycloak_image
    essential = true

    portMappings = [{ containerPort = 8080, protocol = "tcp" }]

    command = ["start-dev", "--import-realm"]

    environment = [
      { name = "KC_BOOTSTRAP_ADMIN_USERNAME", value = "admin" },
      { name = "KC_BOOTSTRAP_ADMIN_PASSWORD", value = var.admin_password },
      { name = "KC_HTTP_ENABLED",             value = "true" },
      { name = "KC_HOSTNAME",                 value = var.alb_dns_name },
      { name = "KC_HOSTNAME_PORT",            value = "8180" },
      { name = "KC_HOSTNAME_STRICT",          value = "false" },
      { name = "KC_PROXY",                    value = "edge" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.keycloak.name
        "awslogs-region"        = "eu-west-1"
        "awslogs-stream-prefix" = "keycloak"
      }
    }
  }])
}

# ── Service ───────────────────────────────────────────────────────────────────

resource "aws_ecs_service" "keycloak" {
  name            = "${local.name}-keycloak"
  cluster         = var.cluster_id
  task_definition = aws_ecs_task_definition.keycloak.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "keycloak"
    container_port   = 8080
  }

  depends_on = [aws_iam_role_policy_attachment.execution]
}
