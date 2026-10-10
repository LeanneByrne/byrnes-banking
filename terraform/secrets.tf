# One secret per value rather than one JSON blob with several keys -- the
# ECS task definition's `secrets` block maps a container env var straight
# to a whole secret ARN with zero JSON-key parsing involved, which is
# simpler to get right than valueFrom's `arn:...:key::jsonkey` syntax.

resource "aws_secretsmanager_secret" "db_username" {
  name                    = "${var.project_name}/db-username"
  recovery_window_in_days = 0 # demo project; no need to delay deletion on destroy
}

resource "aws_secretsmanager_secret_version" "db_username" {
  secret_id     = aws_secretsmanager_secret.db_username.id
  secret_string = var.db_username
}

resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.project_name}/db-password"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${var.project_name}/jwt-secret"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

resource "aws_secretsmanager_secret" "admin_email" {
  name                    = "${var.project_name}/admin-email"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "admin_email" {
  secret_id     = aws_secretsmanager_secret.admin_email.id
  secret_string = var.admin_email
}

resource "aws_secretsmanager_secret" "admin_password" {
  name                    = "${var.project_name}/admin-password"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "admin_password" {
  secret_id     = aws_secretsmanager_secret.admin_password.id
  secret_string = var.admin_password
}
