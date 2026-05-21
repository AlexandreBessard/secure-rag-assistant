locals {
  name = "${var.project}-${var.environment}"
}

# ── ECR ───────────────────────────────────────────────────────────────────────

resource "aws_ecr_repository" "ingestion" {
  name                 = "${local.name}-ingestion"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }
}

# ── IAM ───────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "execution" {
  name = "${local.name}-ingestion-execution"
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

resource "aws_iam_role" "task" {
  name = "${local.name}-ingestion-task"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "task_s3" {
  name = "${local.name}-ingestion-s3"
  role = aws_iam_role.task.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:HeadObject"]
        Resource = "${var.document_bucket_arn}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = var.document_bucket_arn
      }
    ]
  })
}

resource "aws_iam_role_policy" "task_sqs" {
  name = "${local.name}-ingestion-sqs"
  role = aws_iam_role.task.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes",
        "sqs:ChangeMessageVisibility",
      ]
      Resource = var.sqs_queue_arn
    }]
  })
}

resource "aws_iam_role_policy" "task_bedrock" {
  name = "${local.name}-ingestion-bedrock"
  role = aws_iam_role.task.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["bedrock:InvokeModel"]
      Resource = "*"
    }]
  })
}

# ── Logs ──────────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "ingestion" {
  name              = "/ecs/${local.name}-ingestion"
  retention_in_days = 30
}

# ── Task Definition ───────────────────────────────────────────────────────────

resource "aws_ecs_task_definition" "ingestion" {
  family                   = "${local.name}-ingestion"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name      = "ingestion"
    image     = var.ingestion_image
    essential = true

    # No port mappings — the aws profile runs with web-application-type=none

    environment = [
      { name = "SPRING_PROFILES_ACTIVE",         value = "aws" },
      { name = "SPRING_DATASOURCE_URL",           value = "jdbc:postgresql://${var.db_host}:5432/${var.db_name}" },
      { name = "SPRING_DATASOURCE_USERNAME",      value = "raguser" },
      { name = "SPRING_DATASOURCE_PASSWORD",      value = var.db_password },
      { name = "AWS_REGION",                      value = var.aws_region },
      { name = "BEDROCK_REGION",                  value = var.bedrock_region },
      { name = "SQS_QUEUE_NAME",                  value = var.sqs_queue_name },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ingestion.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ingestion"
      }
    }
  }])
}

# ── Service ───────────────────────────────────────────────────────────────────

resource "aws_ecs_service" "ingestion" {
  name            = "${local.name}-ingestion"
  cluster         = var.cluster_id
  task_definition = aws_ecs_task_definition.ingestion.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  depends_on = [aws_iam_role_policy_attachment.execution]
}
