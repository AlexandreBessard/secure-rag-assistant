variable "project"           { type = string }
variable "environment"       { type = string }
variable "subnet_ids"        { type = list(string) }
variable "security_group_id" { type = string }

variable "db_name" {
  type    = string
  default = "ragdb"
}

variable "db_username" {
  type    = string
  default = "raguser"
}

variable "db_password" {
  type      = string
  sensitive = true
}
