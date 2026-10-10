variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Short name used to prefix/tag every resource."
  type        = string
  default     = "byrnes-banking"
}

variable "github_repository" {
  description = "GitHub \"owner/repo\" allowed to assume the deploy role via OIDC."
  type        = string
}

variable "alert_email" {
  description = "Email address that receives the AWS Budgets billing alert."
  type        = string
}

variable "budget_limit_usd" {
  description = "Monthly budget threshold in USD that triggers the billing alert."
  type        = number
  default     = 20
}

variable "db_username" {
  description = "RDS Postgres master username."
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "RDS Postgres master password."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Secret used to sign JWTs -- a long, random value, not the local dev placeholder."
  type        = string
  sensitive   = true
}

variable "admin_email" {
  description = "Seeds one ADMIN customer on first startup, same mechanism as local dev."
  type        = string
  sensitive   = true
}

variable "admin_password" {
  description = "Password for the seeded ADMIN customer."
  type        = string
  sensitive   = true
}
