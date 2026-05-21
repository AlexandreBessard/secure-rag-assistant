module "networking" {
  source      = "./modules/networking"
  project     = var.project
  environment = var.environment
}

module "s3" {
  source      = "./modules/s3"
  project     = var.project
  environment = var.environment
}

module "rds" {
  source            = "./modules/rds"
  project           = var.project
  environment       = var.environment
  subnet_ids        = module.networking.private_subnet_ids
  security_group_id = module.networking.rds_sg_id
  db_password       = var.db_password
}

module "sqs" {
  source              = "./modules/sqs"
  project             = var.project
  environment         = var.environment
  document_bucket_arn = module.s3.bucket_arn
  document_bucket_id  = module.s3.bucket_id
}

module "ecs_backend" {
  source              = "./modules/ecs_backend"
  project             = var.project
  environment         = var.environment
  subnet_ids          = module.networking.private_subnet_ids
  security_group_id   = module.networking.backend_sg_id
  target_group_arn    = module.networking.backend_tg_arn
  backend_image       = var.backend_image
  tools_image         = var.tools_image
  db_host             = module.rds.db_host
  db_name             = module.rds.db_name
  db_password         = var.db_password
  aws_region          = var.aws_region
  bedrock_region      = var.bedrock_region
  s3_bucket_name      = module.s3.bucket_id
  keycloak_issuer_uri = "http://${module.networking.alb_dns_name}:8180/realms/rag-assistant"
}

module "ecs_keycloak" {
  source            = "./modules/ecs_keycloak"
  project           = var.project
  environment       = var.environment
  cluster_id        = module.ecs_backend.ecs_cluster_id
  subnet_ids        = module.networking.public_subnet_ids
  security_group_id = module.networking.keycloak_sg_id
  target_group_arn  = module.networking.keycloak_tg_arn
  admin_password    = var.keycloak_admin_password
  alb_dns_name      = module.networking.alb_dns_name
  keycloak_image    = var.keycloak_image
}

module "ecs_ingestion" {
  source              = "./modules/ecs_ingestion"
  project             = var.project
  environment         = var.environment
  cluster_id          = module.ecs_backend.ecs_cluster_id
  subnet_ids          = module.networking.private_subnet_ids
  security_group_id   = module.networking.ingestion_sg_id
  ingestion_image     = var.ingestion_image
  db_host             = module.rds.db_host
  db_name             = module.rds.db_name
  db_password         = var.db_password
  aws_region          = var.aws_region
  bedrock_region      = var.bedrock_region
  sqs_queue_name      = module.sqs.queue_name
  sqs_queue_arn       = module.sqs.queue_arn
  document_bucket_arn = module.s3.bucket_arn
}
