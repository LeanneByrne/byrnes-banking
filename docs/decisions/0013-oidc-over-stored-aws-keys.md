# 0013 — GitHub Actions authenticates to AWS via OIDC, never stored access keys

**Status:** Accepted

## Context

The deploy workflow needs AWS credentials to push images to ECR, update the ECS service, and
sync the frontend to S3. The traditional approach is generating a long-lived IAM user access
key/secret pair and pasting it into a GitHub repository secret.

## Decision

An `aws_iam_openid_connect_provider` trusts GitHub's own OIDC token issuer
(`token.actions.githubusercontent.com`). A dedicated IAM role
(`aws_iam_role.github_actions_deploy`) can be assumed only by a token whose `sub` claim matches
`repo:<this-repo>:*` — i.e., only workflow runs from this specific repository. The workflow
(`.github/workflows/deploy.yml`) uses `aws-actions/configure-aws-credentials` to exchange
GitHub's short-lived token for temporary AWS credentials scoped to that one role, on every run.
No AWS access key or secret exists in GitHub's secret store at all.

## Why not a stored access key

A static IAM user access key, once created, works forever until someone manually rotates or
deletes it. If it ever leaks — accidentally logged, committed, or exfiltrated from a compromised
dependency in the CI environment — it remains valid for an attacker until that manual rotation
happens, which depends entirely on someone noticing. A token minted via OIDC is valid only for
the duration of one workflow run, is cryptographically tied to a specific repository (and,
narrower still, could be tied to a specific branch or environment), and there's no credential
sitting in a secret store for a compromised dependency or a misconfigured log statement to leak
in the first place. This is the same underlying principle as [ADR 0010](0010-frontend-token-storage.md)'s
choice to keep the frontend's JWT in memory rather than persistent storage — shrinking how long a
credential exists and where it can be found, not just how it's used once issued.

## Why the trust policy isn't scoped tighter than "this repository, any workflow/branch"

A tighter condition (a specific branch, a specific GitHub Environment with manual approval) is
possible and would be the right call for a team project with multiple contributors or an
environment that deploys automatically on merge. Here, the deploy workflow is already
`workflow_dispatch`-only (see `deploy.yml`) — nothing deploys without a human explicitly
triggering it regardless of which ref the trigger came from — so the marginal safety a tighter
`sub` condition would add is small relative to the added friction for a one-person project.

## Consequence

Rotation is a non-issue — there's no long-lived credential to rotate. The tradeoff is that this
setup is AWS- and GitHub-Actions-specific; moving CI to a different platform would mean
re-establishing an equivalent OIDC trust relationship with that platform's own token issuer
rather than reusing a portable credential, which is a reasonable cost for removing a standing
secret entirely.
