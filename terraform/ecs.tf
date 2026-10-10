resource "aws_ecs_cluster" "main" {
  name = var.project_name
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 14
}

# The execution role is what ECS itself uses to pull the image, write logs,
# and fetch secrets BEFORE the container starts -- distinct from a task
# role, which the app's own code would use to call AWS APIs at runtime.
# This app makes no AWS SDK calls, so there is no task role here at all.
resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.project_name}-ecs-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_task_execution_secrets" {
  name = "read-app-secrets"
  role = aws_iam_role.ecs_task_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = "secretsmanager:GetSecretValue"
      Resource = [
        aws_secretsmanager_secret.db_username.arn,
        aws_secretsmanager_secret.db_password.arn,
        aws_secretsmanager_secret.jwt_secret.arn,
        aws_secretsmanager_secret.admin_email.arn,
        aws_secretsmanager_secret.admin_password.arn,
      ]
    }]
  })
}

resource "aws_ecs_task_definition" "app" {
  family                   = var.project_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([{
    name  = "app"
    image = "${aws_ecr_repository.app.repository_url}:latest"

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "cloud" },
      { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.main.endpoint}/byrnes_banking" },
      { name = "FRONTEND_ORIGIN", value = "https://${aws_cloudfront_distribution.frontend.domain_name}" },
    ]

    secrets = [
      { name = "DB_USERNAME", valueFrom = aws_secretsmanager_secret.db_username.arn },
      { name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
      { name = "JWT_SECRET", valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
      { name = "ADMIN_EMAIL", valueFrom = aws_secretsmanager_secret.admin_email.arn },
      { name = "ADMIN_PASSWORD", valueFrom = aws_secretsmanager_secret.admin_password.arn },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "app"
      }
    }
  }])
}

resource "aws_lb" "app" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id
}

resource "aws_lb_target_group" "app" {
  name        = "${var.project_name}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  # /actuator/health/readiness, not the full /actuator/health -- the
  # readiness probe group was built specifically to never aggregate
  # third-party component health (Kafka, etc.), so it can't flap on
  # something that was never supposed to be reachable in this deployment.
  health_check {
    path                = "/actuator/health/readiness"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
  }
}

resource "aws_lb_listener" "app" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

resource "aws_ecs_service" "app" {
  name            = var.project_name
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true # no NAT gateway -- see network.tf
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "app"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.app]
}
