# AWS deployment

Terraform for a minimal, cost-conscious deployment of ByrnesBanking: ECS Fargate (backend),
RDS Postgres, S3 + CloudFront (frontend), Secrets Manager, CloudWatch, and the IAM/OIDC setup
GitHub Actions needs to deploy without any stored AWS keys. See
[ADR 0011](../docs/decisions/0011-kafka-excluded-from-cloud.md),
[ADR 0012](../docs/decisions/0012-cost-conscious-cloud-infra.md), and
[ADR 0013](../docs/decisions/0013-oidc-over-stored-aws-keys.md) for the reasoning behind the
real tradeoffs this makes.

**This creates real, billable AWS resources.** Nothing here runs automatically — every step
below is something you run yourself, deliberately.

## Before anything else: the billing alert

Per the project roadmap, this step comes first, on its own, before any other resource exists:

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars   # fill in real values -- see below
terraform init
terraform apply -target=aws_budgets_budget.monthly
```

Confirm the email in `alert_email` actually receives the AWS Budgets subscription-confirmation
email before going further — the alert won't fire if that subscription wasn't confirmed.

## `terraform.tfvars`

Copy `terraform.tfvars.example` and fill in real values. **Never commit this file** (it's
gitignored) — it holds the DB password, JWT secret, and admin credentials in plaintext locally,
which is standard for a Terraform root module's local-only tfvars file, but still means treating
this file itself like a secret.

- `github_repository` — `"YourUsername/byrnes-banking"`, used to scope the OIDC trust policy
- `alert_email` — a real inbox you check
- `db_password`, `jwt_secret`, `admin_password` — generate real random values
  (`openssl rand -base64 32`), not the local dev placeholders

## Deploy everything else

```bash
terraform plan    # review what it's about to create
terraform apply
```

This takes several minutes (RDS in particular). When it finishes, note these outputs — you'll
need them in the next step:

```bash
terraform output
```

## One-time GitHub setup

The deploy workflow (`.github/workflows/deploy.yml`) reads its AWS resource identifiers from
repository **variables** (Settings → Secrets and variables → Actions → Variables), not secrets —
none of these values are sensitive, they're just identifiers:

| Repo variable | Value (from `terraform output`) |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | `github_actions_role_arn` |
| `AWS_REGION` | whatever you set `aws_region` to |
| `PROJECT_NAME` | `byrnes-banking` (or whatever you set `project_name` to) |
| `ECS_CLUSTER_NAME` | `ecs_cluster_name` |
| `ECS_SERVICE_NAME` | `ecs_service_name` |
| `FRONTEND_BUCKET_NAME` | `frontend_bucket_name` |
| `CLOUDFRONT_DISTRIBUTION_ID` | `cloudfront_distribution_id` |
| `BACKEND_API_URL` | `http://` + `alb_dns_name` |

## First deploy

The ECS service exists after `terraform apply`, but it's bootstrapped with a placeholder
`:latest` tag that was never actually pushed — expect 0/1 healthy tasks until you run the actual
deploy:

1. GitHub → Actions → "Deploy to AWS" → Run workflow (this is `workflow_dispatch` — it never
   runs on its own)
2. Once it finishes, the backend is live at `http://<alb_dns_name>` and the frontend at
   `https://<cloudfront_domain_name>` (both from `terraform output`)

## Tearing it down

```bash
cd terraform
terraform destroy
```

This is the one-command teardown the roadmap calls for. **Run this when you're not actively
demoing** — RDS and the ALB both bill hourly for existing, not for traffic.

## Cost awareness

Nothing here is free-tier-guaranteed forever. Rough always-on monthly cost if left running
continuously: RDS `db.t4g.micro` (~$12), ALB (~$16 + light usage), Fargate task at 0.25 vCPU/0.5GB
(~$9), S3 + CloudFront (cents, at demo traffic levels). `terraform destroy` between uses is the
actual cost control, not the sizing choices alone — the budget alert is a backstop for "I forgot
it was running," not a substitute for destroying it.
