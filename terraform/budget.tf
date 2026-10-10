# Per ROADMAP.md: "billing alert created FIRST." In practice, with
# Terraform, that means applying just this one resource before the rest --
# see terraform/README.md's bootstrapping sequence. AWS Budgets rather than
# the older CloudWatch billing-metric-alarm approach, since Budgets doesn't
# require first enabling the legacy "Receive Billing Alerts" account
# preference by hand in the console.
resource "aws_budgets_budget" "monthly" {
  name         = "${var.project_name}-monthly"
  budget_type  = "COST"
  limit_amount = tostring(var.budget_limit_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.alert_email]
  }
}
