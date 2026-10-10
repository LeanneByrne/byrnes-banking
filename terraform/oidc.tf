# Lets GitHub Actions assume an AWS role using a short-lived token GitHub
# itself issues, instead of a long-lived access key pasted into a GitHub
# secret -- see ADR 0013. The thumbprint below is GitHub's well-known,
# long-standing OIDC token endpoint thumbprint, not a secret.
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

resource "aws_iam_role" "github_actions_deploy" {
  name = "${var.project_name}-github-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        # Scoped to this one repository -- any branch/workflow within it.
        # Tightening further to a specific ref is possible but adds
        # friction for a one-person project without materially changing
        # the risk (the deploy workflow is workflow_dispatch-only anyway,
        # see .github/workflows/deploy.yml, so nothing deploys without a
        # human explicitly triggering it).
        StringLike = {
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repository}:*"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_actions_deploy" {
  name = "deploy-permissions"
  role = aws_iam_role.github_actions_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "PushImages"
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
        ]
        Resource = "*"
      },
      {
        Sid    = "PushToThisRepoOnly"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:BatchGetImage",
        ]
        Resource = aws_ecr_repository.app.arn
      },
      {
        Sid    = "DeployToEcs"
        Effect = "Allow"
        Action = [
          "ecs:DescribeServices",
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition",
          "ecs:UpdateService",
        ]
        Resource = "*"
      },
      {
        Sid      = "PassExecutionRoleToEcs"
        Effect   = "Allow"
        Action   = "iam:PassRole"
        Resource = aws_iam_role.ecs_task_execution.arn
      },
      {
        Sid    = "DeployFrontend"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
        ]
        Resource = [
          aws_s3_bucket.frontend.arn,
          "${aws_s3_bucket.frontend.arn}/*",
        ]
      },
      {
        Sid      = "InvalidateCloudFrontCache"
        Effect   = "Allow"
        Action   = "cloudfront:CreateInvalidation"
        Resource = aws_cloudfront_distribution.frontend.arn
      },
    ]
  })
}
