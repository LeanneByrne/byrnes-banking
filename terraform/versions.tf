terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Deliberately no remote backend (S3 + DynamoDB lock table) configured --
  # that's real infrastructure of its own, and a one-person demo project
  # doesn't have a second engineer who could conflict with local state.
  # Keep the state file (terraform.tfstate) somewhere safe; it is NOT
  # committed to git (see .gitignore).
}
