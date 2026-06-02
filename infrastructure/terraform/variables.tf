variable "aws_region" {
  description = "Região AWS"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nome do projeto"
  type        = string
  default     = "feedback-platform"
}

variable "environment" {
  description = "Ambiente (dev, prod)"
  type        = string
  default     = "prod"
}

variable "db_name" {
  description = "Nome do banco de dados"
  type        = string
  default     = "feedbackdb"
}

variable "db_username" {
  description = "Usuário do banco de dados"
  type        = string
  default     = "feedbackuser"
}

variable "db_password" {
  description = "Senha do banco de dados"
  type        = string
  sensitive   = true
}

variable "admin_email" {
  description = "E-mail do administrador para notificações e relatórios"
  type        = string
}

variable "ses_sender_email" {
  description = "E-mail remetente verificado no SES"
  type        = string
}

variable "lambda_memory_mb" {
  description = "Memória das funções Lambda em MB"
  type        = number
  default     = 512
}

variable "lambda_timeout_seconds" {
  description = "Timeout das funções Lambda em segundos"
  type        = number
  default     = 30
}
