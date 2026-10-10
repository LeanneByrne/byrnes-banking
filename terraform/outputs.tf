output "alb_dns_name" {
  description = "The backend API's URL (http://, no custom domain)."
  value       = aws_lb.app.dns_name
}

output "frontend_url" {
  description = "The frontend's CloudFront URL (https://)."
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "ecr_repository_url" {
  description = "Where the deploy workflow pushes images."
  value       = aws_ecr_repository.app.repository_url
}

output "frontend_bucket_name" {
  description = "Where the deploy workflow syncs the built frontend."
  value       = aws_s3_bucket.frontend.id
}

output "cloudfront_distribution_id" {
  description = "Needed by the deploy workflow to invalidate the cache after a frontend deploy."
  value       = aws_cloudfront_distribution.frontend.id
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
}

output "github_actions_role_arn" {
  description = "Set this as the AWS_DEPLOY_ROLE_ARN repository variable for the deploy workflow."
  value       = aws_iam_role.github_actions_deploy.arn
}

output "rds_endpoint" {
  description = "For reference only -- the running app gets this via DB_URL, not manually."
  value       = aws_db_instance.main.endpoint
}
