resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-${var.environment}-db-subnet-group"
  subnet_ids = var.subnet_ids
}

resource "aws_db_instance" "postgres" {
  identifier        = "${var.project}-${var.environment}-postgres"
  engine            = "postgres"
  engine_version    = "16.14"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.security_group_id]

  # pgvector is pre-installed on RDS PostgreSQL 15+.
  # Run `CREATE EXTENSION IF NOT EXISTS vector;` once after the instance is ready.
  # The Lambda handler does this automatically on first invocation.

  backup_retention_period = 7
  skip_final_snapshot     = true  # POC — set false in production
  deletion_protection     = false # POC — set true in production

  lifecycle {
    ignore_changes = [password]
  }
}
